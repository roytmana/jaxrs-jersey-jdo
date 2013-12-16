/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package com.peacetech.jaxrs.providers;

import com.peacetech.jaxrs.JDO;
import com.sun.net.httpserver.HttpContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.CloseableService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@javax.ws.rs.ext.Provider
public class PersistenceManagerBinder extends AbstractBinder {

  @Singleton
  private static class PersistenceManagerInjectionResolver implements InjectionResolver<JDO> {
    private static final Log LOG = LogFactory.getLog(PersistenceManagerInjectionResolver.class);
    @Inject private Provider<CloseableService> closeableServiceProvider;
    @Inject private Provider<HttpServletRequest> requestProvider;
    @Inject private Provider<ServletContext> servletContextProvider;
    @Inject private Provider<SecurityContext> securityContextProvider;
    @NotNull private final ServiceLocator locator;
    private final Map<Class<?>, PersistenceManagerPoolHolder> pmPools = new HashMap<Class<?>, PersistenceManagerPoolHolder>();
    private final AtomicLong _id = new AtomicLong(0);

    @Inject
    private PersistenceManagerInjectionResolver(@NotNull ServiceLocator locator) {
      this.locator = locator;
      LOG.debug("Creating PersistenceManagerInjectionResolver");
    }

    @Override
    public Object resolve(final Injectee injectee, final ServiceHandle<?> root) {
      long id = _id.getAndIncrement();
      final AnnotatedElement element = injectee.getParent();
      if (!injectee.getRequiredType().equals(PersistenceManager.class)) {
        LOG.error(element + " is not of the expected type " + injectee.getRequiredType());
        return null;
      }
      final JDO annotation = element.getAnnotation(JDO.class);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Instantiating " + element + "[id=" + id + "], " + servletContextProvider.get());
      }

      PersistenceManagerPoolHolder poolHolder;
      synchronized (pmPools) {
        poolHolder = pmPools.get((annotation.value()));
        if (poolHolder == null) {
          poolHolder = new PersistenceManagerPoolHolder(annotation);
          pmPools.put(annotation.value(), poolHolder);
        }
      }

      PersistenceManagerPoolPair pmPair = new PersistenceManagerPoolPair(poolHolder.getPool());
      closeableServiceProvider.get().add(pmPair);
      pmPair.getPersistenceManager().setUserObject(getUserObject(annotation));
      return pmPair.getPersistenceManager();
    }

    @Override
    public boolean isConstructorParameterIndicator() {
      return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
      return false;
    }

    private Object getUserObject(@NotNull JDO annotation) {
      Class<?> userObjectClass = annotation.userObject();
      Object userObject;
      if (Principal.class.equals(userObjectClass)) {
        userObject = requestProvider.get().getUserPrincipal();
      } else if (HttpServletRequest.class.equals(userObjectClass)) {
        userObject = requestProvider.get();
      } else if (ServletContext.class.equals(userObjectClass)) {
        userObject = servletContextProvider.get();
      } else if (SecurityContext.class.equals(userObjectClass) || Request.class.equals(userObjectClass) ||
                 HttpHeaders.class.equals(userObjectClass)) {
        userObject = locator.getService(userObjectClass);
      } else if (userObjectClass == null || Object.class.equals(userObjectClass)) {
        userObject = null;
      } else {
        if (userObjectClass.isInterface() || userObjectClass.isAnnotation()) {
          throw new IllegalArgumentException(
              "userObject specified in " + annotation + " must be one of the following classes" +
              " or a class with constructor taking single " + HttpContext.class.getName() + " parameter");
        }
        try {
          Constructor<?> constr = userObjectClass.getConstructor(HttpContext.class);
          userObject = constr.newInstance(requestProvider.get());
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException(
              "userObject specified in " + annotation + " must be one of the following classes" +
              " or a class with constructor taking single " + HttpContext.class.getName() + " parameter", e);
        } catch (Exception e) {
          throw new IllegalArgumentException("Attempt to instantiate PersistenceManager userObject specified in " +
                                             annotation + " failed", e);
        }
      }
      return userObject;
    }

  }

  @Override
  protected void configure() {
    System.out.println("Configuring PersistenceManagerBinder");
    bind(PersistenceManagerInjectionResolver.class).to(new TypeLiteral<InjectionResolver<JDO>>() {
    }).in(Singleton.class);
  }
}

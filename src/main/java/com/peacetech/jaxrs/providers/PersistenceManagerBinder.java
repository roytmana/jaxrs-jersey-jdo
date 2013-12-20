/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package com.peacetech.jaxrs.providers;

import com.peacetech.jaxrs.JDO;
import com.peacetech.jaxrs.Scope;
import com.peacetech.jdo.pmpool.PersistenceManagerPool;
import com.sun.net.httpserver.HttpContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.CloseableService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    @Inject private Provider<ContainerRequestContext> containerRequestContextProvider;
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
        LOG.error(element + " is not of the expected type " + injectee.getRequiredType() + requestProvider.get().getRequestURI());
        return null;
      }
      Class<? extends Annotation> scopeAnnotation = injectee.getInjecteeDescriptor().getScopeAnnotation();
      if (scopeAnnotation.equals(Singleton.class)) {
        LOG.error(element + " only support per request scope and not " + scopeAnnotation.getName() + requestProvider.get().getRequestURI());
        return null;
      }
      final JDO annotation = element.getAnnotation(JDO.class);

      if (annotation.scope() == Scope.SINGLETON || annotation.scope() == Scope.PER_THREAD) {
        LOG.error(element + " only support per request and per injection scopes and not " + annotation.scope() +
                  requestProvider.get().getRequestURI());
        return null;
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Instantiating " + element + "[id=" + id + "], " + requestProvider.get().getRequestURI());
      }

      final String requestPropertyKey = PersistenceManagerBinder.class.getName() + ':' + annotation.value().getName();
      if (annotation.scope() == Scope.PER_REQUEST) {
        ContainerRequestContext request = containerRequestContextProvider.get();
        PersistenceManagerPoolPair pmPair = (PersistenceManagerPoolPair)request.getProperty(requestPropertyKey);
        if (pmPair != null) {
          LOG.debug("Reusing Request Scoped PM [id=" + pmPair.getId() + "] for " + element + "[id=" + id + "], " +
                    requestProvider.get().getRequestURI());
          return  pmPair.getPersistenceManager();
        }
      }

      PersistenceManagerPoolHolder poolHolder;
      synchronized (pmPools) {
        poolHolder = pmPools.get((annotation.value()));
        if (poolHolder == null) {
          poolHolder = new PersistenceManagerPoolHolder(annotation);
          pmPools.put(annotation.value(), poolHolder);
        }
      }

      Object userObject = getUserObject(annotation);
      PersistenceManagerPoolPair pmPair = new PersistenceManagerPoolPair(id, poolHolder.getPool());
      closeableServiceProvider.get().add(pmPair);
      pmPair.getPersistenceManager().setUserObject(userObject);
      if (annotation.scope() == Scope.PER_REQUEST) {
        containerRequestContextProvider.get().setProperty(requestPropertyKey, pmPair);
      }
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
          Constructor<?> constr = userObjectClass.getConstructor(HttpServletRequest.class);
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

  private static class PersistenceManagerPoolPair implements Closeable {
    private final long id;
    @NotNull private final PersistenceManagerPool pool;
    @NotNull private final PersistenceManager persistenceManager;

    private PersistenceManagerPoolPair(long id, @NotNull PersistenceManagerPool pool, @NotNull PersistenceManager persistenceManager) {
      this.id = id;
      this.pool = pool;
      this.persistenceManager = persistenceManager;
    }

    public PersistenceManagerPoolPair(long id, @NotNull PersistenceManagerPool pool) {
      this.id = id;
      this.pool = pool;
      this.persistenceManager = pool.borrow();
    }

    @Override public void close() throws IOException {
      releasePersistenceManager();
    }

    public void releasePersistenceManager() {
      pool.release(persistenceManager);
    }

    @NotNull public PersistenceManagerPool getPool() {
      return pool;
    }

    @NotNull public PersistenceManager getPersistenceManager() {
      return persistenceManager;
    }

    public long getId() {
      return id;
    }

    @Override public String toString() {
      return "PersistenceManagerPoolPair{id=" + id + ", " + persistenceManager + ", " + pool + '}';
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PersistenceManagerPoolPair pair = (PersistenceManagerPoolPair)o;
      return persistenceManager.equals(pair.persistenceManager);
    }

    @Override public int hashCode() {
      return persistenceManager.hashCode();
    }
  }

  private static class PersistenceManagerPoolHolder {
    private final PersistenceManagerPool pool;
    private final @NotNull JDO annotation;

    private PersistenceManagerPoolHolder(@NotNull JDO annotation) {
      this.annotation = annotation;
      try {
        Method m = annotation.value().getMethod("pool");
        pool = (PersistenceManagerPool)m.invoke(null);
        if (pool == null) {
          throw new IllegalArgumentException("PersistenceManagerPool class defined in " + annotation.getClass().getName() +
                                             " annotation value parameter " + annotation.value().getName() +
                                             " has required pool() method but its invocation returned null value");
        }
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("PersistenceManagerPool class defined in " + annotation.getClass().getName() +
                                           " annotation value parameter " + annotation.value().getName() +
                                           " does not have required pool() method", e);
      } catch (InvocationTargetException e) {
        throw new IllegalArgumentException("PersistenceManagerPool class defined in " + annotation.getClass().getName() +
                                           " annotation value parameter " + annotation.value().getName() +
                                           " has required pool() method but its invocation thrown exception", e);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("PersistenceManagerPool class defined in " + annotation.getClass().getName() +
                                           " annotation value parameter " + annotation.value().getName() +
                                           " has required pool() method but its invocation thrown exception", e);
      }
    }

    @NotNull public PersistenceManagerPool getPool() {
      return pool;
    }

    @NotNull public JDO getAnnotation() {
      return annotation;
    }
  }
}

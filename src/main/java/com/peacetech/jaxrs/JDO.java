/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 1$
 * $Date: 2/25/2009 5:31:25 PM$
 * $NoKeywords$
 */
package com.peacetech.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests injection of {@link javax.jdo.PersistenceManager} into field ir parameter buy borrowing
 * instance of PersistenceManager from specified {@link com.peacetech.jdo.pmpool.PersistenceManagerPool}<br/>
 * Injected PersistenceManager is registered with http request and returned to the pool
 * at the end of request lifecycle automatically by {@link JAXRSJDOServletContainer}<br/>
 *
 * @see com.peacetech.jaxrs.providers.PersistenceManagerInjector
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JDO {
  /**
   * Any class having public static method {@code PersistenceManagerPool pool()} providing access to PersistenceManagerPool
   * for retrieval and disposal of PersistenceManagerPool instances
   *
   * @return Class providing access to PersistenceManagerPool
   */
  Class<?> value();

  /**
   * Class or Interface to set userObject into injected PersistenceManager.<br/>
   * {@link javax.jdo.PersistenceManager#setUserObject(Object)} will be called to set PersistenceManager's user object<br/>
   * Following values will cause injection:<br/>
   * {@link Object} -> no value (default)<br/>
   * {@link java.security.Principal} -> {@link javax.servlet.http.HttpServletRequest#getUserPrincipal()}<br/>
   * {@link javax.servlet.http.HttpServletRequest} -> {@link javax.servlet.http.HttpServletRequest}<br/>
   * {@link com.sun.jersey.api.core.HttpContext} -> {@link com.sun.jersey.api.core.HttpContext}<br/>
   * {@link javax.ws.rs.core.SecurityContext} -> {@link javax.ws.rs.core.SecurityContext}<br/>
   * {@link javax.ws.rs.core.Request} -> {@link javax.ws.rs.core.Request}<br/>
   * Any other class will be instantiated using constructor with single {@link com.sun.jersey.api.core.HttpContext} parameter.<br/>
   * Future version will support all JAX-RS mandated @Context(s)
   *
   * @return class of instance to be set to {@link javax.jdo.PersistenceManager#setUserObject(Object)}
   */
  Class<?> userObject() default Object.class;
}

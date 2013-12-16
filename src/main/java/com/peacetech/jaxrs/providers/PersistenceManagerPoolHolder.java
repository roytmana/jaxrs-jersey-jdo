/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 1$
 * $Date: 2/25/2009 5:31:26 PM$
 * $NoKeywords$
 */
package com.peacetech.jaxrs.providers;

import com.peacetech.jaxrs.JDO;
import com.peacetech.jdo.pmpool.PersistenceManagerPool;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class PersistenceManagerPoolHolder {
  private final PersistenceManagerPool pool;
  private final @NotNull JDO annotation;

  public PersistenceManagerPoolHolder(@NotNull JDO annotation) {
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

/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 1$
 * $Date: 2/25/2009 5:31:26 PM$
 * $NoKeywords$
 */
package com.peacetech.jaxrs.providers;

import com.peacetech.jdo.pmpool.PersistenceManagerPool;
import org.jetbrains.annotations.NotNull;

import javax.jdo.PersistenceManager;
import java.io.Closeable;
import java.io.IOException;

class PersistenceManagerPoolPair implements Closeable {
  @NotNull private final PersistenceManagerPool pool;
  @NotNull private final PersistenceManager persistenceManager;

  public PersistenceManagerPoolPair(@NotNull PersistenceManagerPool pool, @NotNull PersistenceManager persistenceManager) {
    this.pool = pool;
    this.persistenceManager = persistenceManager;
  }

  public PersistenceManagerPoolPair(@NotNull PersistenceManagerPool pool) {
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

  @Override public String toString() {
    return "PersistenceManagerPoolPair{" + persistenceManager + ", " + pool + '}';
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

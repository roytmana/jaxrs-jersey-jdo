/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package com.peacetech.jaxrs.providers;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class JDOFeature implements Feature {

  @Override public boolean configure(FeatureContext context) {
    context.register(new PersistenceManagerBinder());
    return true;
  }
}

/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 2$
 * $Date: 3/4/2009 11:43:17 PM$
 * $NoKeywords$
 */
package com.peacetech.jaxrs.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StringStreamingOutput implements StreamingOutput {
  private final String string;

  public StringStreamingOutput(String string) {
    this.string = string;
  }

  @Override public void write(OutputStream output) throws IOException, WebApplicationException {
    if (string != null) {
      output.write(string.getBytes());
    }
  }
}
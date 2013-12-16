/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 1$
 * $Date: 2/25/2009 5:31:26 PM$
 * $NoKeywords$
 */
package com.peacetech.jaxrs.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Convenience class for converting OutputStream in StreamingOutput to OutputStreamWriter
 * which is more suitable for writing character streams ensuring that OutputStreamWriter
 * will be flushed at the end of write method.
 * <p/>
 * Should be used as default implementation of StreamingOutput when character stream is desired
 */
public abstract class WriterOutput implements StreamingOutput {

  @Override public void write(OutputStream output) throws IOException, WebApplicationException {
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    OutputStreamWriter out = new OutputStreamWriter(output);
    try {
      write(out);
    } finally {
      out.flush();
    }
  }

  public abstract void write(OutputStreamWriter writer) throws IOException, WebApplicationException;
}

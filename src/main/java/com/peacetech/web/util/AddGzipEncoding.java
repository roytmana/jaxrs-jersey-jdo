package com.peacetech.web.util;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Copyright (c) 2007, Peace Technology, Inc.
 * $Author: Roytman, Alex$
 * $Revision: 2$
 * $Date: 6/8/2009 9:58:04 PM$
 * $NoKeywords$
 */
public class AddGzipEncoding implements Filter {
  @Override public void destroy() {
  }

  @Override public void doFilter(ServletRequest req, ServletResponse resp,
                                 FilterChain chain) throws ServletException, IOException {
    HttpServletRequest request = (HttpServletRequest)req;
    String url = request.getRequestURI();
    int p1 = url.lastIndexOf('.');
    if (p1 > 0) {
      int p2 = url.lastIndexOf('.', p1 - 1);
      if (p2 > 0) {
        String ext = url.substring(p2 + 1, p1).toLowerCase();
        if ("gz".equals(ext)) {
          HttpServletResponse response = (HttpServletResponse)resp;
          response.addHeader("Content-Encoding", "gzip");
        }
      }
    }
    chain.doFilter(req, resp);
  }

  @Override public void init(FilterConfig config) throws ServletException {

  }
}

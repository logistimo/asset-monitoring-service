/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import play.Logger;

/**
 * Utility to work with network connections, e.g. HTTP
 *
 * @author Arun
 */
public class HttpUtil {

  // Constants
  public static final String POST = "POST";
  public static final String GET = "GET";
  // Logger
  private static final Logger.ALogger xLogger = Logger.of(HttpUtil.class);

  // Do a HTTP get or post
  public static String connect(String method, String url, Map<String, String> params,
                               Map<String, String> requestProperties)
      throws MalformedURLException, IOException {
    xLogger.info("Entered httpConnect");
    String returnVal = "";
    try {
      // Get the query string
      String queryString = "";
      if (params != null) {
        Iterator<String> it = params.keySet().iterator();
        while (it.hasNext()) {
          String param = it.next();
          String value = params.get(param);
          if (value != null) {
            if (queryString.length() > 0) {
              queryString += "&";
            }
            queryString += param + "=" + URLEncoder.encode(value, "UTF-8");
          }
        }
      }
      ///else if ( url.indexOf( '?' ) > 0 ) {
      ///	queryString = url.substring( url.indexOf( '?' ) + 1, url.length() );
      ///}
      xLogger.info("HttpUtil.connect: URL = {0}, query string = {1}", url, queryString);
      // Open the URL connection
      URL urlObj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
      connection.setRequestMethod(method);
      connection.setConnectTimeout(10000); // 10 seconds max. in GAE (default is 5 seconds)
      // Set the properties, if present
      if (requestProperties != null) {
        Iterator<String> it = requestProperties.keySet().iterator();
        while (it.hasNext()) {
          String prop = it.next();
          String val = requestProperties.get(prop);
          if (val != null) {
            connection.setRequestProperty(prop, val);
          }
        }
      }
      // Get the output stream for writing param data
      if (!queryString.isEmpty()) {
        connection.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(queryString);
        out.close();
      }
      // Read returned value
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String decodedString = null;
      int i = 0;
      while ((decodedString = in.readLine()) != null) {
        if (i > 0) {
          returnVal += "\n";
        }
        returnVal += decodedString;
        ++i;
      }
      in.close();
    } catch (UnsupportedEncodingException e) {
      xLogger.warn("UnsupportedCodingException: {0}", e.getMessage());
    }
    xLogger.info("Exiting httpConnect");
    return returnVal;
  }

  // Get the URL base given a servlet
  public static String getUrlBase(HttpServletRequest request) {
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443)) {
      return request.getScheme() + "://" + request.getServerName();
    } else {
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
  }

  // Replace variables in a given string
  public static String replace(String str, Map<String, String> replaceMap) {
    Iterator<String> vars = replaceMap.keySet().iterator();
    while (vars.hasNext()) {
      String var = vars.next();
      str = str.replace(var, replaceMap.get(var));
    }
    return str;
  }
}


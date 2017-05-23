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

package com.logistimo.services;

import java.io.IOException;

import play.Logger;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Http;

/**
 * Created by kaniyarasu on 06/11/14.
 */
public class SMSService<T> extends ServiceImpl {
  private static final Logger.ALogger LOGGER = Logger.of(SMSService.class);

  /**
   * Call the SMS Service API, return response.
   */
  public String sendSMS(String requestURL) throws IOException {
    int MAX_TRIALS = 3;
    for (int index = 0; index < 3; index++) {
      try {
        //String hmac = hmac(Play.application().configuration().getString("application.secret"), data);
        WSRequestHolder req = WS.url(requestURL);
        WSResponse wsResponse = req.get().get(20000);
        if (wsResponse.getStatus() != Http.Status.OK) {
          throw new IOException(wsResponse.getStatusText());
        }
        LOGGER.info("Response: %s", wsResponse.getStatus());
        return wsResponse.getStatusText();
      } catch (IOException e) {
        LOGGER.warn("IOException on trial %s; will be trying %s more times: %s", index,
            (MAX_TRIALS - index), e.getMessage());
        if (index == MAX_TRIALS) {
          throw new IOException(
              "Error when connection to SMS Gateway to send message. Please try again later.");
        }
      }
    }
    return "";
  }

  /**
   * Call the SMS Service API, return response.
   */
  public String sendSMS(String requestURL, String requestBody) throws IOException {
    int MAX_TRIALS = 3;
    for (int index = 0; index < 3; index++) {
      try {
        //String hmac = hmac(Play.application().configuration().getString("application.secret"), data);
        WSRequestHolder req = WS.url(requestURL).setHeader("Content-Type", "application/json");
        WSResponse wsResponse = req.post(requestBody).get(20000);
        if (wsResponse.getStatus() != Http.Status.OK) {
          throw new IOException(wsResponse.getStatusText());
        }
        LOGGER.info("Response: {}", wsResponse.getStatus());
        return wsResponse.getStatusText();
      } catch (IOException e) {
        LOGGER.warn("IOException on url {}, body {}, trial {}; will be trying {} more times: {}",
            requestURL, requestBody, index, (MAX_TRIALS - index), e.getMessage());
        if (index == MAX_TRIALS) {
          throw new IOException(
              "Error when connection to SMS Gateway to send message. Please try again later.");
        }
      }
    }
    return "";
  }

  public String sendSMS(String requestUrl, T t) throws IOException {
    return sendSMS(requestUrl, Json.toJson(t).toString());
  }
}

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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kaniyarasu on 27/10/14.
 */
public interface LogistimoConstant {
  String VALIDATION_CONFIG_SMSGYPH_NOTFOUND = "validation.config.smsGyPh.not_found";

  //Global message variables
  String REQUEST_INVALID_CONTENT_TYPE = "request.invalid.content_type";
  String REQUEST_EMPTY_CONTENT_TYPE = "request.empty.content_type";

  String SERVER_ERROR_RESPONSE = "server.error.response";
  String SERVER_ERROR_PARTRESPONSE = "server.error.partresponse";

  //Devices
  String DEVICES_NOT_FOUND = "device.not_found";

  //Character Constants
  String SLASH = "/";
  String QUESTION = "?";
  String AMPERSAND = "&";
  String SPACE = " ";
  String PARAN_OPEN = "(";
  String PARAN_CLOSE = ")";
  String COMMA = ",";

  //SMS Service Constants
  String COUNTRY_CODE_PARAM = "%country%";
  String PHONE_PARAM = "%phone%";
  String VENDOR_ID_PARAM = "%vid%";
  String DEVICE_ID_PARAM = "%did%";
  String DEFAULT_COUNTRY_CODE = "IN";
  String SUPPORT_VENDOR_SEP = COMMA;

  //Asset relation types
  Integer CONTAINS = 1;
  Integer MONITORED_BY = 2;

  List<Integer> FRIDGE_MONITORING_POINTS = new ArrayList<Integer>() {{
    add(1);
    add(2);
    add(3);
    add(4);
  }};
  String EMPTY = "";
  String OK = "OK";
  String DEVICE_LOCK = "DEVICE";
}

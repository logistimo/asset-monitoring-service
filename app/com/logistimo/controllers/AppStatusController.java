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

package com.logistimo.controllers;

import java.io.File;

import play.Logger;
import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Result;

import static com.logistimo.controllers.BaseController.prepareResult;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SERVICE_UNAVAILABLE;

/**
 * @author Smriti on 6/30/16.
 */
public class AppStatusController {
  private final static Logger.ALogger aLogger = Logger.of(AppStatusController.class);

  @Transactional(readOnly = true)
  public static Result getAppStatus() {
    try {
      if (new File(Play.application().configuration().getString("status.path")).exists()) {
        return prepareResult(OK, null, (String) null);
      }
    } catch (Exception e) {
      aLogger.warn("Error in getting status of file: {0}", e);
    }
    return prepareResult(SERVICE_UNAVAILABLE, null, (String) null);
  }
}

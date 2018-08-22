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

import com.logistimo.models.user.request.AddApiConsumerRequest;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.UserService;
import com.logistimo.utils.LogistimoConstant;

import javax.persistence.NoResultException;

import play.Logger;
import play.Logger.ALogger;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

public class AdminController extends BaseController {
  private static final ALogger LOGGER = Logger.of(AdminController.class);
  private static final UserService userService = ServiceFactory.getService(UserService.class);


  @Transactional
  @With(AdminAuthentication.class)
  public static Result createUser(String callback) {
    AddApiConsumerRequest addApiConsumerRequest;

    try {
      addApiConsumerRequest =
          getValidatedObject(request().body().asJson(), AddApiConsumerRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Bad Request - Invalid JSON " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, e.getMessage());
    }

    try {
      try {
        userService.getUserAccount(addApiConsumerRequest.userName);
        LOGGER.warn("Bad Request - User already exists : " + addApiConsumerRequest.toString());
        return prepareResult(BAD_REQUEST, callback, "User already exists.");
      } catch (NoResultException e) {
        userService.addUser(addApiConsumerRequest);
        return prepareResult(CREATED, callback, "User account created.");
      }
    } catch (Exception e) {
      LOGGER.error("Error while creating user account:" + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }

  }

  @Transactional(readOnly = true)
  @With(AdminAuthentication.class)
  public static Result getUsers(String callback) {
    try {
      return prepareResult(OK, callback, Json.toJson(userService.getAllUsers()));
    } catch (NoResultException e) {
      LOGGER.warn("No users found - " + e.getMessage(),e);
      return prepareResult(NO_CONTENT, callback, "No users found.");
    }
  }
}

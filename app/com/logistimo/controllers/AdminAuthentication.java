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

import com.logistimo.utils.LogistimoUtils;

import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class AdminAuthentication extends Action.Simple {
  private final static String AUTH_TOKEN_HEADER = "Authorization";
  private static final ALogger LOGGER = Logger.of(AdminAuthentication.class);

  private static boolean isValidPassword(String actual, String supplied) {
    return actual.equals(supplied);
  }

  /**
   * Verify the authorization header with admin credentials
   */
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {
    String[] authTokenHeaderValues = ctx.request().headers().get(AUTH_TOKEN_HEADER);
    if (!ctx.request().headers().containsKey(AUTH_TOKEN_HEADER)) {
      authTokenHeaderValues = ctx.request().headers().get(AUTH_TOKEN_HEADER.toUpperCase());
    }

    Result unauthorized = Results.unauthorized("unauthorized");

    if (authTokenHeaderValues == null) {
      ctx.response().setHeader("WWW-Authenticate", "Basic realm='Secured'");
      return F.Promise.pure(unauthorized);
    }

    String[] credentials = LogistimoUtils.decodeHeader(authTokenHeaderValues[0]);
    if ((credentials != null) && (credentials.length == 2) && (credentials[0] != null)) {
      String adminPassword = Play.application().configuration().getString("admin.password");
      if (credentials[0].equals("admin") && isValidPassword(adminPassword, credentials[1])) {
        return delegate.call(ctx);
      }
    }
    ctx.response().setHeader("WWW-Authenticate", "Basic realm='Secured'");
    return F.Promise.pure(unauthorized);
  }
}

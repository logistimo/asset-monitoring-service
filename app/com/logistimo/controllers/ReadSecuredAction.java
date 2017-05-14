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

import com.logistimo.db.UserAccount;
import com.logistimo.models.common.BaseResponse;
import com.logistimo.models.user.response.UserAccountResponse;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.UserService;
import com.logistimo.utils.LogistimoUtils;
import org.apache.commons.codec.digest.DigestUtils;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.persistence.NoResultException;

public class ReadSecuredAction extends Action.Simple {
    public final static String AUTH_TOKEN_HEADER = "Authorization";
    private static final ALogger LOGGER = Logger.of(SecuredAction.class);

    private static boolean isValidPassword(String actual, String supplied) {
        String saltedSuppliedPassword = DigestUtils.md5Hex(supplied);
        return actual.equals(saltedSuppliedPassword);
    }

    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        String[] authTokenHeaderValues = ctx.request().headers().get(AUTH_TOKEN_HEADER);
        if(!ctx.request().headers().containsKey(AUTH_TOKEN_HEADER)){
            authTokenHeaderValues = ctx.request().headers().get(AUTH_TOKEN_HEADER.toUpperCase());
        }
        Result unauthorized = Results.unauthorized(Json.toJson(new BaseResponse("Access denied.")));

        if (authTokenHeaderValues == null) {
            ctx.response().setHeader("WWW-Authenticate", "Basic realm='Secured'");
            return F.Promise.pure(unauthorized);
        }

        String[] credentials = LogistimoUtils.decodeHeader(authTokenHeaderValues[0]);
        if ((credentials != null) && (credentials.length == 2) && (credentials[0] != null)) {
            try {
                UserAccountResponse userAccountResponse = ServiceFactory.getService(UserService.class)
                        .getUserAccount(credentials[0]);
                if (isValidPassword(userAccountResponse.password, credentials[1])) {
                    return delegate.call(ctx);
                }
            } catch (NoResultException e) {
                LOGGER.warn("Error while authenticating user", e.getMessage());
            }

        }
        ctx.response().setHeader("WWW-Authenticate", "Basic realm='Secured'");
        return F.Promise.pure(unauthorized);
    }

}

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

import com.fasterxml.jackson.databind.JsonNode;
import com.logistimo.exception.LogistimoException;
import com.logistimo.models.common.BaseResponse;
import com.logistimo.utils.LogistimoUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import static play.libs.Jsonp.jsonp;

/**
 * Created by kaniyarasu on 08/10/14.
 */
public class BaseController extends Controller {

  private static ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private static Validator validator = factory.getValidator();

  public static Result prepareResult(int statusCode, String callback, JsonNode jsonNode) {
    if (callback != null) {
      return status(statusCode, jsonp(callback, jsonNode));
    }
    return status(statusCode, jsonNode);
  }

  public static Result prepareResult(int statusCode, String callback, String message) {
    return prepareResult(statusCode, callback, Json.toJson(new BaseResponse(message)));
  }

  //Maps input json to given class instance, and validates the fields. Return the mapped object if no error.
  public static <T> T getValidatedObject(JsonNode jsonNode, Class<T> klass)
      throws LogistimoException {
    if (jsonNode == null) {
      throw new LogistimoException("JSON data is required.");
    }

    T obj = Json.fromJson(jsonNode, klass);
    LogistimoUtils.validateObject(obj);
    return obj;
  }

  public static String decodeParameter(String paramValue) {
    if (StringUtils.isNotEmpty(paramValue)) {
      try {
        paramValue = URLDecoder.decode(paramValue, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        //do nothing
      }
    }

    return paramValue;
  }
}

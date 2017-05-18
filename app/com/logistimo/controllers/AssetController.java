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

import com.logistimo.exception.LogistimoException;
import com.logistimo.models.asset.AssetRegistrationModel;
import com.logistimo.services.AssetService;
import com.logistimo.services.ServiceFactory;

import javax.persistence.NoResultException;

import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

/**
 * Created by kaniyarasu on 22/09/15.
 */
public class AssetController extends BaseController {
  private final static Logger.ALogger LOGGER = Logger.of(AssetController.class);
  private static final AssetService assetService = ServiceFactory.getService(AssetService.class);

  @Transactional
  @With(SecuredAction.class)
  public static Result createAsset(String callback) {
    AssetRegistrationModel assetRegistrationModel;
    try {
      assetRegistrationModel =
          getValidatedObject(request().body().asJson(), AssetRegistrationModel.class);
    } catch (LogistimoException e) {
      //TODO
      return prepareResult(BAD_REQUEST, callback, "Error parsing the request" + e.getMessage());
    }

    try {
      assetService.createOrUpdateAssets(assetRegistrationModel);
      return prepareResult(CREATED, callback, "Asset created successfully.");
    } catch (NoResultException e) {
      LOGGER.warn("Error while creating asset", e);
      return prepareResult(NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while creating asset", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAsset(String mancId, String assetId, String callback) {
    try {
      assetId = decodeParameter(assetId);
      return prepareResult(OK, callback, Json.toJson(assetService.getAsset(mancId, assetId)));
    } catch (NoResultException e) {
      LOGGER.warn("Asset not found : {}, {}", mancId, assetId);
      return prepareResult(NOT_FOUND, callback, "Asset not found.");
    } catch (Exception e) {
      LOGGER.error("Error while generating stats by device", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }
}

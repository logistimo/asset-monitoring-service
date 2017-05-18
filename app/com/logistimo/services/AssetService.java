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

import com.logistimo.db.Asset;
import com.logistimo.db.Device;
import com.logistimo.models.asset.AssetModel;
import com.logistimo.models.asset.AssetRegistrationModel;

import javax.persistence.NoResultException;

import play.Logger;

/**
 * Created by kaniyarasu on 22/09/15.
 */
public class AssetService extends ServiceImpl {
  private static final Logger.ALogger LOGGER = Logger.of(AssetService.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);

  public void createOrUpdateAssets(AssetRegistrationModel assetRegistrationModel) {
    if (assetRegistrationModel != null && assetRegistrationModel.data != null) {
      for (AssetModel assetModel : assetRegistrationModel.data) {
        createOrUpdateAsset(assetModel);
      }
    }
  }

  public void createOrUpdateAsset(AssetModel assetModel) {
    try {
      //Updating asset
      Asset asset = Asset.findAsset(assetModel.vId, assetModel.dId);
      asset.assetType = assetModel.at;
      asset.update();
      return;
    } catch (NoResultException e) {
      //do nothing
    }
  }

  public Asset getAsset(String mancId, String assetId) {
    return Asset.findAsset(mancId, assetId);
  }

  public Asset createAssetFromDevice(Device device, Integer assetType) {
    Asset asset = new Asset();
    asset.assetId = device.deviceId;
    asset.mancId = device.vendorId;
    asset.assetType = assetType;
    asset.save();
    return asset;
  }
}

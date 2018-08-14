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

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.logistimo.exception.LogistimoException;
import com.logistimo.healthcheck.MetricsUtil;
import com.logistimo.models.asset.AssetRegistrationRelationModel;
import com.logistimo.models.device.request.APNPushRequest;
import com.logistimo.models.device.request.AdminPushRequest;
import com.logistimo.models.device.request.ConfigurationPushRequest;
import com.logistimo.models.device.request.DeviceConfigurationRequest;
import com.logistimo.models.device.request.DeviceDeleteRequest;
import com.logistimo.models.device.request.DeviceReadyUpdateRequest;
import com.logistimo.models.device.request.DeviceRegisterRequest;
import com.logistimo.models.device.request.DeviceSMSStatusRequest;
import com.logistimo.models.device.request.TagRegisterRequest;
import com.logistimo.models.device.response.DeviceCreationResponse;
import com.logistimo.models.device.response.DeviceDeleteResponse;
import com.logistimo.models.device.response.DeviceReadyUpdateResponse;
import com.logistimo.services.DeviceService;
import com.logistimo.services.ServiceFactory;
import com.logistimo.utils.LogistimoConstant;

import javax.persistence.NoResultException;

import play.Logger;
import play.Logger.ALogger;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

public class DeviceController extends BaseController {
  private static final ALogger LOGGER = Logger.of(DeviceController.class);
  private static DeviceService deviceService = ServiceFactory.getService(DeviceService.class);
  private static final Meter
      meter = MetricsUtil.getMeter(DeviceController.class,"get.recentalarm.meter");
  private static final Timer
      timer = MetricsUtil.getTimer(DeviceController.class,"get.recentalarm.timer");

  @Transactional(readOnly = true)
  @With(ReadSecuredAction.class)
  public static Result getDeviceDetails(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback, Json.toJson(deviceService.getDevice(vendorId, deviceId)));
    } catch (NoResultException e) {
      LOGGER.warn(Messages.get("device.not_found") + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, Messages.get("device.not_found"));
    } catch (Exception e) {
      LOGGER.error("Error while finding device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result findDevice(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback, Json.toJson(deviceService.getDevice(vendorId, deviceId)));
    } catch (NoResultException e) {
      LOGGER.warn(Messages.get("device.not_found") + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, Messages.get("device.not_found"));
    } catch (Exception e) {
      LOGGER.error("Error while finding device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getDeviceRecentAlerts(String vendorId, String deviceId, String sid, int page,
                                             int size, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getDeviceRecentAlarms(vendorId, deviceId, sid, page, size)));
    } catch (NoResultException e) {
      LOGGER.warn(Messages.get("device.not_found") + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, Messages.get("device.not_found"));
    } catch (Exception e) {
      LOGGER.error("Error while finding device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result createOrUpdateDevice(String callback) {
    DeviceRegisterRequest deviceRegisterRequest;
    try {
      deviceRegisterRequest =
          getValidatedObject(request().body().asJson(), DeviceRegisterRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing device registration request data - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while parsing device registration request data");
    }

    try {
      DeviceCreationResponse
          deviceCreationResponse =
          deviceService.createOrUpdateDevice(deviceRegisterRequest);

      if (deviceCreationResponse.errs.size() > 0) {
        LOGGER.warn("Device creation - Partial content: " + deviceCreationResponse.toString());
        return prepareResult(PARTIAL_CONTENT, callback, Json.toJson(deviceCreationResponse));
      }
      return prepareResult(CREATED, callback, "Device created/updated successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while creating device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result deleteDevice(String callback) {
    DeviceDeleteRequest deviceDeleteRequest;
    try {
      deviceDeleteRequest =
          getValidatedObject(request().body().asJson(), DeviceDeleteRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing device delete request data", e);
      return prepareResult(BAD_REQUEST, callback, "Error parsing data - " + e.getMessage());
    }

    try {
      DeviceDeleteResponse deviceDeleteResponse = deviceService.deleteDevice(deviceDeleteRequest);

      if (deviceDeleteResponse.errs.size() == deviceDeleteRequest.getdIds().size()) {
        LOGGER.warn("No devices found: " + deviceDeleteResponse.toString());
        return prepareResult(NOT_FOUND, callback, Json.toJson(deviceDeleteResponse));
      } else if (deviceDeleteResponse.errs.size() > 0) {
        LOGGER.warn("Partial content: " + deviceDeleteResponse.toString());
        return prepareResult(PARTIAL_CONTENT, callback, Json.toJson(deviceDeleteResponse));
      }
      return prepareResult(OK, callback, "Devices deleted successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while deleting device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result createTag(String callback) {
    TagRegisterRequest tagRegisterRequest;
    try {
      tagRegisterRequest = getValidatedObject(request().body().asJson(), TagRegisterRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing tag registration request data", e);
      return prepareResult(BAD_REQUEST, callback, e.getMessage());
    }

    try {
      int numberOfDeviceUpdated = deviceService.createOrUpdateTags(tagRegisterRequest);
      return prepareResult(CREATED, callback,
          tagRegisterRequest.data.size() + " devices received, " + numberOfDeviceUpdated
              + " tagged");
    } catch (Exception e) {
      LOGGER.error("Error while creating tags - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getDevicesStatusByTagName(String tagName, String q, String assetType,
                                                 Integer workingStatus, Integer alarmType,
                                                 Integer alarmDuration, Integer awr,
                                                 int pageNumber, int pageSize, String callback) {
    meter.mark();
    Timer.Context context = timer.time();
    try {
      tagName = decodeParameter(tagName);
      return prepareResult(OK, callback, Json.toJson(deviceService
          .getDevicesByTag(tagName, q, assetType, workingStatus, alarmType, alarmDuration, awr,
              pageNumber, pageSize)));
    } catch (NoResultException e) {
      LOGGER.warn("Device not found - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Device not found.");
    } catch (Exception e) {
      LOGGER.error("Error while finding tagged device - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    } finally {
      context.stop();
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAbnormalDevicesByTagName(String tagName, int pageNumber, int pageSize,
                                                   String callback) {
    try {
      tagName = decodeParameter(tagName);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getAbnormalDevicesByTags(tagName, pageNumber, pageSize)));
    } catch (NoResultException e) {
      LOGGER.warn("Device not found - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Device not found.");
    } catch (Exception e) {
      LOGGER.error("Error while reading alarms - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAbnormalDeviceCountByTagName(String tagPattern, String sType,
                                                       String callback) {
    try {
      if (sType != null && sType.trim().equals("1")) {
        return prepareResult(OK, callback,
            Json.toJson(deviceService.searchDeviceStatusByTagPattern(tagPattern)));
      } else {
        return prepareResult(OK, callback,
            Json.toJson(deviceService.getTaggedDeviceCountResponse(tagPattern)));
      }
    } catch (NoResultException e) {
      LOGGER.warn("Device not found - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Device not found.");
    } catch (Exception e) {
      LOGGER.error("Error while reading alarms - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getChildTagSummary(String tagName, String callback) {
    try {
      tagName = decodeParameter(tagName);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.searchDeviceStatusByParentTag(tagName)));
    } catch (NoResultException e) {
      LOGGER.warn("Device not found - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Device not found.");
    } catch (Exception e) {
      LOGGER.error("Error while reading alarms - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result createDeviceConfig(String callback) {
    try {
      DeviceConfigurationRequest deviceConfigurationRequest;
      try {
        deviceConfigurationRequest =
            getValidatedObject(request().body().asJson(), DeviceConfigurationRequest.class);
      } catch (Exception e) {
        LOGGER.warn(e.getMessage());
        return prepareResult(BAD_REQUEST, callback, e.getMessage());
      }

      deviceService.addDeviceConfiguration(deviceConfigurationRequest);
      return prepareResult(CREATED, callback, "Device configuration posted successfully.");
    } catch (LogistimoException e) {
      LOGGER.warn("Error while posting device configuration - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while posting device configuration");
    } catch (NoResultException e) {
      LOGGER.warn("Error while posting device configuration, device not found - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while posting device configuration, device not found");
    } catch (Exception e) {
      LOGGER.error("Error while posting device configuration - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  public static Result getDeviceConfig(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getDeviceConfiguration(vendorId, deviceId, true)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while reading device configuration -  " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while reading device configuration");
    } catch (Exception e) {
      LOGGER.error("Error while reading device configuration - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getDeviceConfigForApps(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getDeviceConfiguration(vendorId, deviceId, false)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while reading device configuration -  " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while reading device configuration");
    } catch (Exception e) {
      LOGGER.error("Error while reading device configuration -  " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));

    }
  }

  @Transactional(readOnly = true)
  public static Result getDeviceConfigByTag(String vendorId, String tagName, String callback) {
    try {
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getDeviceConfigurationByTagName(tagName)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while reading device configuration -  " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while reading device configuration");
    } catch (Exception e) {
      LOGGER.error("Error while reading device configuration -  " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  public static Result createOrUpdateDeviceStatus(String callback) {
    DeviceReadyUpdateRequest deviceReadyUpdateRequest;
    try {
      deviceReadyUpdateRequest =
          getValidatedObject(request().body().asJson(), DeviceReadyUpdateRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing device ready request data - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while parsing device ready request data");
    }

    try {
      DeviceReadyUpdateResponse
          deviceReadyUpdateResponse =
          deviceService.createOrUpdateDeviceReady(deviceReadyUpdateRequest);

      if (deviceReadyUpdateResponse.errs.size() > 0) {
        LOGGER.warn(
            "Device Ready update - partial content: " + Json.toJson(deviceReadyUpdateResponse));
        return prepareResult(PARTIAL_CONTENT, callback, Json.toJson(deviceReadyUpdateResponse));
      }
      return prepareResult(CREATED, callback, "Device status updated successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while updating device ready status", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result pushConfigToDevice(String callback) {
    ConfigurationPushRequest configurationPushRequest;
    try {
      configurationPushRequest =
          getValidatedObject(request().body().asJson(), ConfigurationPushRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing config push request data - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while parsing config push request data");
    }

    try {
      deviceService.pushConfigToDevice(configurationPushRequest);
      return prepareResult(CREATED, callback, "Device configuration/URL pushed successfully.");
    } catch (NoResultException e) {
      LOGGER.warn("Error while pushing config - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while pushing config");
    } catch (LogistimoException e) {
      LOGGER.warn("Error while pushing config - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while pushing config");
    } catch (Exception e) {
      LOGGER.error("Error while pushing config - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result pushAPNSettings(String callback) {
    APNPushRequest apnPushRequest;
    try {
      apnPushRequest = getValidatedObject(request().body().asJson(), APNPushRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing apn push request data", e);
      return prepareResult(BAD_REQUEST, callback, e.getMessage());
    }

    try {
      deviceService.pushAPNSettingsToDevice(apnPushRequest);
      return prepareResult(CREATED, callback, "APN Settings pushed successfully.");
    } catch (LogistimoException e) {
      LOGGER.warn("Error while pushing apn settings - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while pushing apn settings");
    } catch (Exception e) {
      LOGGER.error("Error while pushing apn settings - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result pushAdminSettings(String callback) {
    AdminPushRequest adminPushRequest;
    try {
      adminPushRequest = getValidatedObject(request().body().asJson(), AdminPushRequest.class);
    } catch (LogistimoException e) {
      LOGGER.warn("Error while parsing admin settings - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback,"Error while parsing admin settings");
    } catch (Exception e) {
      LOGGER.warn("Error while pushing admin settings - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while pushing admin settings");
    }

    try {
      deviceService.pushAdminSettingsToDevice(adminPushRequest);
      return prepareResult(CREATED, callback, "Admin Settings pushed successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while pushing admin settings - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  public static Result updateConfigPushStatus(String callback) {
    DeviceSMSStatusRequest deviceSMSStatusRequest;
    try {
      deviceSMSStatusRequest =
          getValidatedObject(request().body().asJson(), DeviceSMSStatusRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing config push status request data - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, "Error while parsing config push status request data");
    }

    try {
      deviceService.updateConfigSentStatus(deviceSMSStatusRequest);
      return prepareResult(CREATED, callback, "Config sent status updated successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while parsing config push status request data - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  public static Result updateAdminPushStatus(String callback) {
    DeviceSMSStatusRequest deviceSMSStatusRequest;
    try {
      deviceSMSStatusRequest =
          getValidatedObject(request().body().asJson(), DeviceSMSStatusRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Error while parsing Admin push status request data - " + e.getMessage(), e);
      return prepareResult(BAD_REQUEST, callback, e.getMessage());
    }

    try {
      deviceService.updateAdminSentStatus(deviceSMSStatusRequest);
      return prepareResult(CREATED, callback, "Admin push status updated successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while pushing Admin push status request data - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAPNSettings(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getAPNSettings(vendorId, deviceId)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting APN Settings - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while getting APN Settings");
    } catch (Exception e) {
      LOGGER.error("Error while getting APN Settings - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAdminSettings(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getAdminSettings(vendorId, deviceId)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting Admin Settings - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while getting Admin Settings");
    } catch (Exception e) {
      LOGGER.error("Error while getting Admin Settings - " + e.getMessage(), e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result createAssetMapping(String callback) {
    AssetRegistrationRelationModel assetRegistrationRelationModel;
    try {
      assetRegistrationRelationModel =
          getValidatedObject(request().body().asJson(), AssetRegistrationRelationModel.class);
    } catch (LogistimoException e) {
      LOGGER.warn("{} while parsing asset mapping json: {}", e.getMessage(),
          request().body().asJson().toString(), e);
      return prepareResult(BAD_REQUEST, callback, "Error parsing the request" + e.getMessage());
    }

    try {
      deviceService.createOrUpdateAssetMapping(assetRegistrationRelationModel);
      return prepareResult(CREATED, callback, "Asset mapping created successfully.");
    } catch (NoResultException e) {
      LOGGER.warn("Error while creating asset mapping - " + e.getMessage(), e);
      return prepareResult(NOT_FOUND, callback, "Error while creating asset mapping");
    } catch (Exception e) {
      LOGGER.error("Error while creating asset mapping", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAssetRelation(String vendorId, String deviceId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getAssetRelation(vendorId, deviceId)));
    } catch (NoResultException e) {
      LOGGER
          .warn("Error while getting asset relation for the asset: {}, {}", vendorId, deviceId, e);
      return prepareResult(NOT_FOUND, callback, "Error while getting asset relation for the asset");
    } catch (Exception e) {
      LOGGER
          .error("Error while getting asset relation for the asset: {}, {}", vendorId, deviceId, e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getDevicePowerTransition(String vendorId, String deviceId, Integer from,
                                                Integer to, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(deviceService.getDevicePowerTransition(vendorId, deviceId, from, to)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting asset power transition for the asset: {}, {}", vendorId,
          deviceId, e);
      return prepareResult(NOT_FOUND, callback, "Error while getting asset power transition for the asset");
    } catch (Exception e) {
      LOGGER.error("Error while getting asset power transition for the asset: {}, {}", vendorId,
          deviceId, e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE));
    }
  }
}

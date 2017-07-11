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

import com.google.gson.Gson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistimo.db.AlarmLog;
import com.logistimo.db.AssetMapping;
import com.logistimo.db.AssetType;
import com.logistimo.db.AssetUser;
import com.logistimo.db.Device;
import com.logistimo.db.DeviceAPNSettings;
import com.logistimo.db.DeviceAdminSettings;
import com.logistimo.db.DeviceAdminSettingsPushStatus;
import com.logistimo.db.DeviceConfiguration;
import com.logistimo.db.DeviceConfigurationPushStatus;
import com.logistimo.db.DeviceMetaData;
import com.logistimo.db.DevicePowerTransition;
import com.logistimo.db.DeviceStatus;
import com.logistimo.db.DeviceStatusLog;
import com.logistimo.db.Tag;
import com.logistimo.db.TemperatureSensor;
import com.logistimo.exception.LogistimoException;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.alarm.response.AlarmResponse;
import com.logistimo.models.alarm.response.AlertResponse;
import com.logistimo.models.alarm.response.RecentAlertResponse;
import com.logistimo.models.asset.AssetMapModel;
import com.logistimo.models.asset.AssetRegistrationRelationModel;
import com.logistimo.models.asset.AssetRelationModel;
import com.logistimo.models.device.common.DeviceDetails;
import com.logistimo.models.device.common.DeviceEventPushModel;
import com.logistimo.models.device.common.DeviceRequestStatus;
import com.logistimo.models.device.common.TemperatureSensorRequest;
import com.logistimo.models.device.request.APNPushRequest;
import com.logistimo.models.device.request.AdminPushRequest;
import com.logistimo.models.device.request.ConfigurationPushRequest;
import com.logistimo.models.device.request.ConfigurationRequest;
import com.logistimo.models.device.request.DeviceConfigurationRequest;
import com.logistimo.models.device.request.DeviceDeleteRequest;
import com.logistimo.models.device.request.DeviceReadyRequest;
import com.logistimo.models.device.request.DeviceReadyUpdateRequest;
import com.logistimo.models.device.request.DeviceRegisterRequest;
import com.logistimo.models.device.request.DeviceRequest;
import com.logistimo.models.device.request.DeviceSMSStatusRequest;
import com.logistimo.models.device.request.SensorConfigurationRequest;
import com.logistimo.models.device.request.TagRegisterRequest;
import com.logistimo.models.device.response.AssetPowerTransitions;
import com.logistimo.models.device.response.DeviceConfigurationResponse;
import com.logistimo.models.device.response.DeviceCreationResponse;
import com.logistimo.models.device.response.DeviceDeleteResponse;
import com.logistimo.models.device.response.DeviceReadyResponse;
import com.logistimo.models.device.response.DeviceReadyUpdateResponse;
import com.logistimo.models.device.response.DeviceResponse;
import com.logistimo.models.device.response.DeviceStatusModel;
import com.logistimo.models.device.response.TaggedDeviceCountResponse;
import com.logistimo.models.device.response.TaggedDeviceResponse;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.models.task.TemperatureEventType;
import com.logistimo.models.temperature.response.AssetTemperatureResponse;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.DeviceMetaAppendix;
import com.logistimo.utils.HttpUtil;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;
import com.logistimo.utils.LogistimoValidationUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.persistence.NoResultException;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.Json;

@SuppressWarnings("ALL")
public class DeviceService extends ServiceImpl {
  private static final String
      DEVICE_NOT_FOUND =
      "Device %s from vendor %s not yet created in the temperature system.";
  private static final Logger.ALogger LOGGER = Logger.of(DeviceService.class);
  private static final SMSService smsService = ServiceFactory.getService(SMSService.class);
  private static final TemperatureService
      temperatureService =
      ServiceFactory.getService(TemperatureService.class);
  private static final AlarmLogService alarmLogService = ServiceFactory.getService(AlarmLogService.class);

  private static final AlarmService alarmService = ServiceFactory.getService(AlarmService.class);
  private static final String APN_PUSH_URL = "logistimo.apn_push.url";
  private static final String ADMIN_PUSH_URL = "logistimo.admin_push.url";
  private static final String CONFIG_PUSH_URL = "logistimo.config_push.url";
  private static final String CONFIG_PULL_URL = "logistimo.config_pull.url";
  private static final String SUPPORT_APN_PUSH_VENDORS = "logistimo.support.apn_push.vendors";
  private static final String SUPPORT_ADMIN_PUSH_VENDORS = "logistimo.support.admin_push.vendors";
  private static final String SUPPORT_CONFIG_PUSH_VENDORS = "logistimo.support.config_push.vendors";
  private static final String INVALID_DEVICE_PHONE_NUMBER = "invalid_device_phone_number";
  private static final String FEATURE_NOT_SUPPORTED = "feature_not_supported";

  private static final TaskService taskService = ServiceFactory.getService(TaskService.class);

  /**
   * Retrieves Device for given vendor and device id, maps it to DeviceResponse
   *
   * @return DeviceResponse
   */
  public DeviceResponse getDevice(String vendorId, String deviceId) {
    Device device = findDevice(vendorId, deviceId);
    List<DeviceStatus> deviceStatusList = null;
    List<DeviceMetaData> deviceMetaDatas = null;
    List<AssetUser> assetUsers = null;
    Map<Integer, AssetMapModel> assetMapModelMap = null;

    try {
      deviceStatusList = DeviceStatus.getDeviceStatus(device);
    } catch (NoResultException e) {
      LOGGER.warn("No configuration status found for the device: {}, {}", vendorId, deviceId);
    }

    try {
      deviceMetaDatas = getDeviceMetaDatas(device);
    } catch (NoResultException e) {
      LOGGER.warn("No device meta data found for the device: {}, {}", vendorId, deviceId);
    }

    try {
      assetUsers = AssetUser.getAssetUser(device);
    } catch (NoResultException e) {
      LOGGER.warn("No users found for the device: {}, {}", vendorId, deviceId);
    }

    try {
      assetMapModelMap = toAssetMapModels(AssetMapping.findAssetRelationByAsset(device));
    } catch (NoResultException e) {
      LOGGER.warn("No asset relation found for the device: {}, {}", vendorId, deviceId);
    }

    return toDeviceResponse(device, deviceStatusList, deviceMetaDatas, assetUsers,
        assetMapModelMap);
  }

  public RecentAlertResponse getDeviceRecentAlarms(String vendorId, String deviceId, String sid,
                                                   int page, int size) {
    Device device = findDevice(vendorId, deviceId, sid);
    Integer
        alarmType =
        device.assetType.assetType.equals(AssetType.MONITORED_ASSET) ? AlarmLog.TEMP_ALARM
            : AlarmLog.DEVICE_ALARM;
    List<AlertResponse> alertResponseList = toRecentAlertResponses(
        AlarmLog.getAlarmsForDevice(
            device,
            alarmType,
            LogistimoUtils.transformPageNumberToPosition(page, size),
            size
        )
    );
    long
        nPages =
        LogistimoUtils
            .availableNumberOfPages(size, AlarmLog.getAlarmCountForDevice(device, alarmType));
    return new RecentAlertResponse(alertResponseList, nPages);
  }

  /**
   * Retrieves Device for given vendor and device id
   *
   * @return Device
   * @throws javax.persistence.EntityNotFoundException if device not found
   */
  public Device findDevice(String vendorId, String deviceId) {
    try {
      return Device.findDevice(vendorId, deviceId);
    } catch (NoResultException e) {
      LOGGER
          .warn(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND) + ": {}, {}", vendorId, deviceId,
              e);
      throw new NoResultException(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND));
    }
  }

  public Device findDevice(String vendorId, String deviceId, String sensorId) {
    return findDevice(vendorId, (sensorId != null && !sensorId.isEmpty()) ? LogistimoUtils
        .generateVirtualDeviceId(deviceId, sensorId) : deviceId);
  }

  @SuppressWarnings("unchecked")
  public DeviceCreationResponse createOrUpdateDevice(DeviceRegisterRequest deviceRegisterRequest) {
    DeviceCreationResponse deviceCreationResponse = new DeviceCreationResponse();
    for (DeviceRequest deviceRequest : deviceRegisterRequest.devices) {
      try {
        if (deviceRequest.vId == null || deviceRequest.dId == null
            || deviceRequest.vId.length() > 50 || deviceRequest.dId.length() > 100
            || (deviceRequest.trId != null && deviceRequest.trId.length() > 100)) {
          deviceCreationResponse.errs.add(deviceRequest.toString());
        } else {
          Device device = null;
          if (!StringUtils.isEmpty(deviceRequest.ovId)) {
            //Update in vendor Id.
            device = Device.findDevice(deviceRequest.ovId, deviceRequest.dId);
            device.vendorId = deviceRequest.vId;
            device.hash();
          } else {
            device = Device.findDevice(deviceRequest.vId, deviceRequest.dId);
          }
          device.tags = new TreeSet<>();
          if (deviceRequest.tags != null) {
            populateTags(device, deviceRequest.tags);
          }
          if (deviceRequest.iUs != null) {
            device.imgUrls = StringUtils.join(deviceRequest.iUs, LogistimoConstant.COMMA);
          }
          if (deviceRequest.vNm != null) {
            device.vendorName = deviceRequest.vNm;
          }
          if (deviceRequest.ub != null) {
            device.updatedBy = deviceRequest.ub;
          }
          if (deviceRequest.getlId() != null) {
            device.locationId = deviceRequest.getlId();
          }

          if (deviceRequest.ons != null) {
            //Deleting asset users, if not found in ons
            try {
              List<AssetUser> assetUserList = AssetUser.getAssetUser(device, AssetUser.ASSET_OWNER);
              for (AssetUser assetUser : assetUserList) {
                if (!deviceRequest.ons.contains(assetUser.userName)) {
                  assetUser.delete();
                }
              }
            } catch (NoResultException e) {
              //do nothing
            }
            for (String owner : deviceRequest.ons) {
              AssetUser assetUser;
              try {
                AssetUser.getAssetUser(device, owner, AssetUser.ASSET_OWNER);
                continue;
              } catch (NoResultException e) {
                //do nothing
              }
              assetUser = new AssetUser();
              assetUser.device = device;
              assetUser.userName = owner;
              assetUser.userType = AssetUser.ASSET_OWNER;
              assetUser.save();
            }
          }

          if (deviceRequest.mts != null) {
            //Deleting asset users, if not found in mts
            try {
              List<AssetUser>
                  assetUserList =
                  AssetUser.getAssetUser(device, AssetUser.ASSET_MAINTAINER);
              for (AssetUser assetUser : assetUserList) {
                if (!deviceRequest.mts.contains(assetUser.userName)) {
                  assetUser.delete();
                }
              }
            } catch (NoResultException e) {
              //do nothing
            }
            for (String maintainer : deviceRequest.mts) {
              AssetUser assetUser;
              try {
                AssetUser.getAssetUser(device, maintainer, AssetUser.ASSET_MAINTAINER);
                continue;
              } catch (NoResultException e) {
                //do nothing
              }
              assetUser = new AssetUser();
              assetUser.device = device;
              assetUser.userName = maintainer;
              assetUser.userType = AssetUser.ASSET_MAINTAINER;
              assetUser.save();
            }
          }
          try {
            device.assetType = AssetType.getAssetType(deviceRequest.typ);
          } catch (NoResultException e) {
            //device.assetType = AssetType.getAssetType(1);
          }
          device.update();

          //Updating device working status, if any
          if (deviceRequest.ws != null) {
            DeviceStatus
                deviceStatus =
                getOrCreateDeviceStatus(device, null, null, AssetStatusConstants.WORKING_STATUS_KEY,
                    null);
            if (!deviceStatus.status.equals(deviceRequest.ws.st)) {
              int oldStatusUpdatedTime = deviceStatus.statusUpdatedTime;
              int previousStatus = deviceStatus.status;
              deviceStatus.status = deviceRequest.ws.st;
              deviceStatus.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
              deviceStatus.update();
              try {
                DeviceStatusLog
                    oldStatusLog =
                    DeviceStatusLog.getDeviceStatusForGivenStartTime(device.id,
                        AssetStatusConstants.WORKING_STATUS_KEY, oldStatusUpdatedTime);
                oldStatusLog.endTime = deviceStatus.statusUpdatedTime;
                oldStatusLog.nextStatus = deviceStatus.status;
                oldStatusLog.update();
              } catch (NoResultException e) {
                //do nothing
              }
              DeviceStatusLog statusLog = new DeviceStatusLog(device.id);
              statusLog.updatedBy = deviceRequest.ub;
              statusLog.status = deviceStatus.status;
              statusLog.statusUpdatedTime = deviceStatus.statusUpdatedTime;
              statusLog.startTime = deviceStatus.statusUpdatedTime;
              statusLog.previousStatus = previousStatus;
              statusLog.updatedOn = new Date();
              statusLog.save();
              postDeviceStatus(device, deviceStatus);
            }
          }
          //Updating device meta data
          if (deviceRequest.meta != null) {
            try {
              List<DeviceMetaData> deviceMetaDataList = getDeviceMetaDatas(device);
              Map<String, Object>
                  result =
                  new ObjectMapper().readValue(deviceRequest.meta.toString(), LinkedHashMap.class);
              if (result != null) {
                constructAndUpdateDeviceMetaDataFromMap(deviceMetaDataList,
                    LogistimoUtils.constructDeviceMetaDataFromJSON(null, result), device);
              }
            } catch (NoResultException ignored) {
              //do nothing
            } catch (Exception e) {
              LOGGER.error(
                  "{} while extracting device meta data from json {}, updating device {}, {} without meta data",
                  e.getMessage(), deviceRequest.meta, deviceRequest.vId, deviceRequest.dId, e);
            }
          }

          //Initializing asset status
          for (String assetStatusKey : device.assetType.assetType.equals(AssetType.MONITORED_ASSET)
              ? AssetStatusConstants.MONITORED_DEVICE_STATUS_KEYS
              : AssetStatusConstants.DEVICE_STATUS_KEYS) {
            if (device.assetType.assetType.equals(AssetType.MONITORED_ASSET)
                && (assetStatusKey.equals(AssetStatusConstants.ACTIVITY_STATUS_KEY)
                || assetStatusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY))) {
              if (deviceRequest.mps != null) {
                for (Integer monitoringPoint : deviceRequest.mps) {
                  getOrCreateDeviceStatus(device, monitoringPoint, null, assetStatusKey,
                      assetStatusKey.equals(AssetStatusConstants.ACTIVITY_STATUS_KEY)
                          ? AssetStatusConstants.ACTIVITY_STATUS_INACT
                          : AssetStatusConstants.STATUS_OK);
                }

                //Removing extra monitoring point data.
                List<DeviceStatus> deviceStatusList = DeviceStatus.getDeviceStatus(device);
                for (DeviceStatus deviceStatus : deviceStatusList) {
                  if (deviceStatus.statusKey.equals(assetStatusKey) && !deviceRequest.mps
                      .contains(deviceStatus.locationId)) {
                    deviceStatus.delete();
                  }
                }
              }
            } else if (device.assetType.assetType.equals(AssetType.MONITORING_ASSET)
                && AssetStatusConstants.ASSET_SENSOR_STATUS_KEYS.contains(assetStatusKey)
                && deviceRequest.sns != null && !deviceRequest.sns.isEmpty()) {
              for (TemperatureSensorRequest temperatureSensorRequest : deviceRequest.sns) {
                getOrCreateDeviceStatus(device, null, temperatureSensorRequest.getsId(),
                    assetStatusKey, null);
              }
            } else {
              getOrCreateDeviceStatus(device, null, null, assetStatusKey, null);
            }
          }

          //Updating temperature sensor details
          if (deviceRequest.sns != null) {
            createOrUpdateTemperatureSensors(device, deviceRequest);
          }
        }
        continue;
      } catch (NoResultException e) {
        LOGGER.info("Device not found, creating new.");
      }
      //Creating new device, if not found
      createDevice(deviceRequest);
    }

    return deviceCreationResponse;
  }

  private void postDeviceStatus(Device device, DeviceStatus deviceStatus) {
    DeviceEventPushModel.DeviceEvent deviceEvent = new DeviceEventPushModel.DeviceEvent();
    deviceEvent.vId = device.vendorId;
    deviceEvent.dId = device.deviceId;
    deviceEvent.st = deviceStatus.status;
    deviceEvent.time = deviceStatus.statusUpdatedTime;
    deviceEvent.type = DeviceEventPushModel.DEVICE_EVENT_STATUS_GROUP.get(deviceStatus.statusKey);
    if (deviceStatus.locationId != null) {
      deviceEvent.mpId = deviceStatus.locationId;
    }
    if (deviceStatus.sensorId != null) {
      deviceEvent.sId = deviceStatus.sensorId;
    }
    try {
      taskService.produceMessage(
          new TaskOptions(
              TaskType.BACKGROUND_TASK.getValue(),
              PushAlertService.class,
              LogistimoUtils.toJson(new DeviceEventPushModel(deviceEvent)),
              null
          )
      );
    } catch (ServiceException e) {
      LOGGER.error(
          "{} while scheduling task for posting device status update to Logistics service, {}",
          e.getMessage(), deviceEvent.toString(), e);
    }
  }

  public DeviceDeleteResponse deleteDevice(DeviceDeleteRequest deviceDeleteRequest) {
    DeviceDeleteResponse deviceDeleteResponse = new DeviceDeleteResponse();
    for (String deviceId : deviceDeleteRequest.getdIds()) {
      try {
        Device device = findDevice(deviceDeleteRequest.getvId(), deviceId);
        //Deleting related asset(Only contains, i.e., sensors)
        try {
          List<AssetMapping> assetMappingList = AssetMapping.findAssetRelationByAsset(device);
          for (AssetMapping assetMapping : assetMappingList) {
            assetMapping.relatedAsset.delete();
          }
        } catch (NoResultException ignored) {
          //do nothing
        }
        device.delete();
      } catch (NoResultException e) {
        LOGGER.warn(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND) + ": {}, {}",
            deviceDeleteRequest.getvId(), deviceId, e);
        deviceDeleteResponse.errs.add(deviceId);
      }
    }

    return deviceDeleteResponse;
  }

  public DeviceConfigurationResponse getDeviceConfiguration(String vendorId, String deviceId,
                                                            boolean isDevice) {
    Device device = findDevice(vendorId, deviceId);

    if (isDevice) {
      DeviceStatus deviceStatus = null;
      try {
        deviceStatus =
            getOrCreateDeviceStatus(device, null, null, AssetStatusConstants.CONFIG_STATUS_KEY,
                null);
        deviceStatus.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
        if(AssetStatusConstants.CONFIG_PULL_REQUEST_SENT != deviceStatus.status) {
          deviceStatus.statusUpdatedBy = null;
        }
        deviceStatus.status = AssetStatusConstants.CONFIG_STATUS_PULLED;
        deviceStatus.update();
      } catch (NoResultException e) {
        LOGGER.info("Configuration status not available for device: {}, {}", device.vendorId,
            device.deviceId);
      }

      if (deviceStatus == null) {
        deviceStatus = new DeviceStatus();
        deviceStatus.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
        deviceStatus.status = AssetStatusConstants.CONFIG_STATUS_PULLED;
        deviceStatus.device = device;
        deviceStatus.save();
      }
    }

    return getDeviceConfiguration(device);
  }

  public DeviceConfigurationResponse getDeviceConfiguration(Device device) {
    try {
      return toDeviceConfigurationResponse(DeviceConfiguration.getDeviceConfiguration(device));
    } catch (NoResultException e) {
      LOGGER.warn("Device Configuration not found for device, trying with tag.");
    }

    if (device.tags != null) {
      for (Tag tag : device.tags) {
        try {
          return toDeviceConfigurationResponse(DeviceConfiguration.getDeviceConfigByTag(tag));
        } catch (NoResultException e) {
          LOGGER.warn("Device configuration not found for tag, {}", tag.tagName);
        }
      }
    }

    throw new NoResultException("Device configuration not found.");
  }

  public DeviceConfigurationResponse getDeviceConfigurationByTagName(String tagName) {
    Tag tag = Tag.find(tagName);
    return toDeviceConfigurationResponse(DeviceConfiguration.getDeviceConfigByTag(tag));
  }

  public void addDeviceConfiguration(DeviceConfigurationRequest deviceConfigurationRequest)
      throws LogistimoException {
    Device device = null;
    Set<Tag> tags = new TreeSet<Tag>();

    //Validating request object
    validateDeviceConfigurationRequest(deviceConfigurationRequest);
    try {
      device = findDevice(deviceConfigurationRequest.vId, deviceConfigurationRequest.dId);
      if (deviceConfigurationRequest.tags != null) {
        tags = getTags(deviceConfigurationRequest.tags);
      }
    } catch (NoResultException e) {
      if (deviceConfigurationRequest.tags == null) {
        throw new NoResultException("Device and tag not found.");
      }
      tags = getTags(deviceConfigurationRequest.tags);
      if (tags.size() == 0) {
        throw new NoResultException("Device and tag not found.");
      }
    }

    DeviceConfiguration deviceConfiguration;
    if (device != null) {
      if (tags.size() == 0) {
        try {
          deviceConfiguration = DeviceConfiguration.getDeviceConfiguration(device);
          updateDeviceConfiguration(deviceConfiguration, deviceConfigurationRequest);
        } catch (NoResultException e1) {
          deviceConfiguration = toDeviceConfiguration(deviceConfigurationRequest, device, null);
          deviceConfiguration.save();
        }
      } else {
        updateDeviceConfigurationByTags(tags, deviceConfigurationRequest, device);
      }

      if (deviceConfigurationRequest.configuration != null) {
        int sInt = -1, pInt = -1;
        Double min = 2.0, max = 8.0, highWarnTemp = 8.0, lowWarnTemp = 2.0;
        Integer highAlarmDur = 600, lowAlarmDur = 60, highWarnDur = 60, lowWarnDur = 30;
        if (deviceConfigurationRequest.configuration.comm != null) {
          for (String key : DeviceMetaAppendix.INT_GROUP) {
            if (key.equals(DeviceMetaAppendix.INT_SINT)) {
              sInt = deviceConfigurationRequest.configuration.comm.samplingInt;
              createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.INT_SINT,
                  String.valueOf(sInt));
            } else if (key.equals(DeviceMetaAppendix.INT_PINT)) {
              pInt = deviceConfigurationRequest.configuration.comm.pushInt;
              createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.INT_PINT,
                  String.valueOf(pInt));
            }
          }
        }

        //Updating device locale meta information
        if (deviceConfigurationRequest.configuration.locale != null) {
          for (String key : DeviceMetaAppendix.LOCALE_GROUP) {
            if (key.equals(DeviceMetaAppendix.LOCALE_COUNTRYCODE)) {
              createOrUpdateDeviceMetaData(device, key,
                  deviceConfigurationRequest.configuration.locale.cn);
            } else if (key.equals(DeviceMetaAppendix.LOCALE_TIMEZONE)) {
              createOrUpdateDeviceMetaData(device, key,
                  String.valueOf(deviceConfigurationRequest.configuration.locale.tz));
            }
          }
        }

        //Updating device min and max temperature.
        if (deviceConfigurationRequest.configuration.highAlarm != null) {
          max = deviceConfigurationRequest.configuration.highAlarm.temp;
          createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.TMP_MAX, String.valueOf(max));

          for (String key : DeviceMetaAppendix.TEMP_ALARM_GROUP) {
            if (key.equals(DeviceMetaAppendix.ALARM_HIGH_TEMP)) {
              createOrUpdateDeviceMetaData(device, key, String.valueOf(max));
            }

            if (key.equals(DeviceMetaAppendix.ALARM_HIGH_DUR)) {
              highAlarmDur = deviceConfigurationRequest.configuration.highAlarm.dur;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(highAlarmDur));
            }
          }
        }

        if (deviceConfigurationRequest.configuration.lowAlarm != null) {
          min = deviceConfigurationRequest.configuration.lowAlarm.temp;
          createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.TMP_MIN, String.valueOf(min));

          for (String key : DeviceMetaAppendix.TEMP_ALARM_GROUP) {
            if (key.equals(DeviceMetaAppendix.ALARM_LOW_TEMP)) {
              createOrUpdateDeviceMetaData(device, key, String.valueOf(min));
            }

            if (key.equals(DeviceMetaAppendix.ALARM_LOW_DUR)) {
              lowAlarmDur = deviceConfigurationRequest.configuration.lowAlarm.dur;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(lowAlarmDur));
            }
          }
        }

        if (deviceConfigurationRequest.configuration.highWarn != null) {
          for (String key : DeviceMetaAppendix.TEMP_WARN_GROUP) {
            if (key.equals(DeviceMetaAppendix.WARN_HIGH_TEMP)) {
              highWarnTemp = deviceConfigurationRequest.configuration.highWarn.temp;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(highWarnTemp));
            }

            if (key.equals(DeviceMetaAppendix.WARN_HIGH_DUR)) {
              highWarnDur = deviceConfigurationRequest.configuration.highWarn.dur;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(highWarnDur));
            }
          }
        }

        if (deviceConfigurationRequest.configuration.lowWarn != null) {
          for (String key : DeviceMetaAppendix.TEMP_WARN_GROUP) {
            if (key.equals(DeviceMetaAppendix.WARN_LOW_TEMP)) {
              lowWarnTemp = deviceConfigurationRequest.configuration.lowWarn.temp;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(lowWarnTemp));
            }

            if (key.equals(DeviceMetaAppendix.WARN_LOW_DUR)) {
              lowWarnDur = deviceConfigurationRequest.configuration.lowWarn.dur;
              createOrUpdateDeviceMetaData(device, key, String.valueOf(lowWarnDur));
            }
          }
        }

        //Updating sensors sampling and push intervals
        try {
          List<AssetMapping>
              assetMappingList =
              AssetMapping.findAssetRelationByAssetAndType(device, LogistimoConstant.CONTAINS);
          for (AssetMapping assetMapping : assetMappingList) {
            Integer sensorSamplingInt = sInt, sensorPushInt = pInt, sHighAlarmDur = highAlarmDur,
                sLowAlarmDur =
                    lowAlarmDur,
                sHighWarnDur = highWarnDur, sLowWarnDur = lowWarnDur;
            Double sensorMax = max, sensorMin = min, sHighWarnTemp = highWarnTemp,
                sLowWarnTemp =
                    lowWarnTemp;
            if (deviceConfigurationRequest.configuration.sensors != null) {
              for (SensorConfigurationRequest sensor : deviceConfigurationRequest.configuration.sensors) {
                if (StringUtils.isNotEmpty(sensor.getsId())
                    && sensor.getsId()
                    .equals(LogistimoUtils.extractSensorId(assetMapping.relatedAsset.deviceId))) {
                  if (sensor.comm != null) {
                    if (sensor.comm.samplingInt != -1) {
                      sensorSamplingInt = sensor.comm.samplingInt;
                    }

                    if (sensor.comm.pushInt != -1) {
                      sensorPushInt = sensor.comm.pushInt;
                    }
                  }

                  if (sensor.highAlarm != null) {
                    sensorMax = sensor.highAlarm.temp;
                    sHighAlarmDur = sensor.highAlarm.dur;
                  }

                  if (sensor.lowAlarm != null) {
                    sensorMin = sensor.lowAlarm.temp;
                    sLowAlarmDur = sensor.lowAlarm.dur;
                  }

                  if (sensor.highWarn != null) {
                    sHighWarnTemp = sensor.highWarn.temp;
                    sHighWarnDur = sensor.highWarn.dur;
                  }

                  if (sensor.lowWarn != null) {
                    sLowWarnTemp = sensor.lowWarn.temp;
                    sLowWarnDur = sensor.lowWarn.dur;
                  }
                }
              }
            }
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset, DeviceMetaAppendix.INT_SINT,
                String.valueOf(sensorSamplingInt));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset, DeviceMetaAppendix.INT_PINT,
                String.valueOf(sensorPushInt));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset, DeviceMetaAppendix.TMP_MAX,
                String.valueOf(sensorMax));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset, DeviceMetaAppendix.TMP_MIN,
                String.valueOf(sensorMin));

            //Propagating alarm configuration to device
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.ALARM_HIGH_DUR, String.valueOf(sHighAlarmDur));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.ALARM_HIGH_TEMP, String.valueOf(sensorMax));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.ALARM_LOW_DUR, String.valueOf(sLowAlarmDur));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.ALARM_LOW_TEMP, String.valueOf(sensorMin));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.WARN_HIGH_DUR, String.valueOf(sHighWarnDur));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.WARN_HIGH_TEMP, String.valueOf(sHighWarnTemp));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset, DeviceMetaAppendix.WARN_LOW_DUR,
                String.valueOf(sLowWarnDur));
            createOrUpdateDeviceMetaData(assetMapping.relatedAsset,
                DeviceMetaAppendix.WARN_LOW_TEMP, String.valueOf(sLowWarnTemp));
          }
        } catch (NoResultException ignored) {
          //do nothing
        }
      }
    } else {
      updateDeviceConfigurationByTags(tags, deviceConfigurationRequest, device);
    }
  }

  public int createOrUpdateTags(TagRegisterRequest tagRegisterRequest) {
    int count = 0;
    for (DeviceRequest deviceRequest : tagRegisterRequest.data) {
      try {
        Device device = findDevice(deviceRequest.vId, deviceRequest.dId);
        device.tags = new TreeSet<Tag>();
        populateTags(device, deviceRequest.tags);
        device.update();
        count++;
      } catch (NoResultException e) {
        Logger.warn(DEVICE_NOT_FOUND, deviceRequest.dId, deviceRequest.vId);
      }

    }

    return count;
  }

  public List<Device> getDeviceTaggedWith(String tagName) {
    List<Device> deviceList = new ArrayList<Device>();
    try {
      for (Device device : Tag.find(tagName).device) {
        deviceList.add(device);
      }
    } catch (NoResultException e) {
      throw new NoResultException("Tag not found");
    }

    return deviceList;
  }

  public TaggedDeviceResponse getDevicesByTag(String tagName, String q, String assetType,
                                              Integer workingStatus, Integer alarmType,
                                              Integer alarmDuration, Integer awr, int pageNumber,
                                              int pageSize) {
    TaggedDeviceResponse taggedDeviceResponse = new TaggedDeviceResponse();
    List<Device>
        deviceList =
        getDeviceForTag(tagName, q, assetType, workingStatus, alarmType, alarmDuration, awr,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize);

    taggedDeviceResponse.setnDevices(deviceList.size());
    List<DeviceMetaData> deviceMetaDatas = null;
    List<AssetUser> assetUsers = null;
    List<DeviceStatus> deviceStatusList = null;
    Map<Integer, AssetMapModel> assetMapModelMap = null;

    for (Device device : deviceList) {
      try {
        deviceMetaDatas = getDeviceMetaDatas(device);
      } catch (NoResultException e) {
        LOGGER.warn("No device meta data found for the device: {}, {}", device.vendorId,
            device.deviceId);
      }

      if (StringUtils.isEmpty(q)) {
        try {
          deviceStatusList = DeviceStatus.getDeviceStatus(device);
        } catch (NoResultException e) {
          LOGGER.warn("No device status found for the device: {}, {}", device.vendorId,
              device.deviceId);
        }

        try {
          assetUsers = AssetUser.getAssetUser(device);
        } catch (NoResultException e) {
          LOGGER.warn("No users found for the device: {}, {}", device.vendorId, device.deviceId);
        }

        try {
          assetMapModelMap = toAssetMapModels(AssetMapping.findAssetRelationByAsset(device));
        } catch (NoResultException e) {
          LOGGER.warn("No asset relation found for the device: {}, {}", device.vendorId,
              device.deviceId);
        }
      }

      taggedDeviceResponse.data.add(
          toDeviceResponse(device, deviceStatusList, deviceMetaDatas, assetUsers,
              assetMapModelMap));
    }

    if (deviceList.size() > 0) {
      taggedDeviceResponse.setnAbnormalTemp(getAbnormalDeviceCount(deviceList));
      taggedDeviceResponse
          .setnAbnormalDevices(alarmService.getAbnormalAlarmCountForDevices(deviceList));
      taggedDeviceResponse.setnIActDevices(Device.getInactiveCountForDevices(deviceList));
    }

    return taggedDeviceResponse;
  }

  public TaggedDeviceResponse getAbnormalDevicesByTags(String tagName, int pageNumber,
                                                       int pageSize) {
    TaggedDeviceResponse taggedDeviceResponse = new TaggedDeviceResponse();
    List<Device>
        deviceList =
        getDeviceForTag(tagName, null, null, null, null, null, null,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize);
    List<DeviceMetaData> deviceMetaDatas = null;
    List<AssetUser> assetUsers = null;
    List<DeviceStatus> deviceStatusList = null;

    for (Device device : deviceList) {
      try {
        deviceStatusList = DeviceStatus.getDeviceStatus(device);
      } catch (NoResultException e) {
        LOGGER.warn("No device status found for the device: {}, {}", device.vendorId,
            device.deviceId);
      }

      try {
        deviceMetaDatas = getDeviceMetaDatas(device);
      } catch (NoResultException e) {
        LOGGER.warn("No device meta data found for the device: {}, {}", device.vendorId,
            device.deviceId);
      }

      try {
        assetUsers = AssetUser.getAssetUser(device);
      } catch (NoResultException e) {
        LOGGER.warn("No users found for the device: {}, {}", device.vendorId, device.deviceId);
      }

      boolean isAbnormal = false;
      DeviceResponse
          deviceResponse =
          toDeviceResponse(device, deviceStatusList, deviceMetaDatas, assetUsers, null);

      //Only abnormal device, needs to returned back
      if (!isAbnormal) {
        continue;
      }

      taggedDeviceResponse.data.add(deviceResponse);

    }

    taggedDeviceResponse.setnDevices(getAssetsCountForTag(tagName, AssetType.ILR));
    taggedDeviceResponse.setnAbnormalDevices(
        getTempMetricsByTag(tagName, AssetType.ILR, AssetStatusConstants.TEMP_STATUS_ALARM));
    taggedDeviceResponse.setnIActDevices(getInactiveAssetCountByTag(tagName, AssetType.ILR));

    return taggedDeviceResponse;
  }

  public TaggedDeviceCountResponse getTaggedDeviceCountResponse(String tagName) {
    TaggedDeviceCountResponse taggedDeviceCountResponse = new TaggedDeviceCountResponse();
    taggedDeviceCountResponse.setnDevices(getAssetsCountForTag(tagName, AssetType.ILR));
    taggedDeviceCountResponse.setnAbnormalDevices(
        getTempMetricsByTag(tagName, AssetType.ILR, AssetStatusConstants.TEMP_STATUS_ALARM));
    //taggedDeviceCountResponse.setnAbnormalDevices(alarmService.getAbnormalAlarmCountForDevices(deviceList));
    taggedDeviceCountResponse.setnIActDevices(getInactiveAssetCountByTag(tagName, AssetType.ILR));

    return taggedDeviceCountResponse;
  }

  public List<TaggedDeviceCountResponse> searchDeviceStatusByTagPattern(String tagPattern) {
    List<Tag> tags = Tag.findChildTags(tagPattern);
    List<TaggedDeviceCountResponse>
        taggedDeviceCountResponses =
        new ArrayList<TaggedDeviceCountResponse>(1);

    for (Tag tag : tags) {
      List<Device> deviceList = new ArrayList<>(1);
      deviceList.addAll(tag.device);

      TaggedDeviceCountResponse taggedDeviceCountResponse = new TaggedDeviceCountResponse();
      taggedDeviceCountResponse.tag = tag.tagName;
      taggedDeviceCountResponse.setnDevices(getAssetsCountForTag(tag.tagName, AssetType.ILR));
      taggedDeviceCountResponse.setnAbnormalDevices(
          getTempMetricsByTag(tag.tagName, AssetType.ILR, AssetStatusConstants.TEMP_STATUS_ALARM));
      taggedDeviceCountResponse
          .setnIActDevices(getInactiveAssetCountByTag(tag.tagName, AssetType.ILR));
      taggedDeviceCountResponses.add(taggedDeviceCountResponse);
    }

    return taggedDeviceCountResponses;
  }

  public List<TaggedDeviceCountResponse> searchDeviceStatusByParentTag(String tagName) {
    tagName = "^" + tagName + "\\.[^.]+$";
    return searchDeviceStatusByTagPattern(tagName);
  }

  public int getAbnormalDeviceCount(List<Device> deviceList) {
    if (deviceList != null && deviceList.size() > 0) {
      return Device.getAbnormalReadingCountForDevices(deviceList);
    } else {
      return 0;
    }
  }

  private DeviceResponse toDeviceResponse(Device device, List<DeviceStatus> deviceStatusList,
                                          List<DeviceMetaData> deviceMetaDatas,
                                          List<AssetUser> assetUsers,
                                          Map<Integer, AssetMapModel> assetMapModelMap) {
    DeviceResponse deviceResponse = new DeviceResponse();
    deviceResponse.setdId(device.deviceId);
    deviceResponse.setvId(device.vendorId);
    deviceResponse.trId = device.transmitterId;
    deviceResponse.setvNm(device.vendorName);
    deviceResponse.cb = device.createdBy;
    deviceResponse.ub = device.updatedBy;
    deviceResponse.co = device.createdOn;
    deviceResponse.uo = device.updatedOn;
    deviceResponse.setMpId(device.locationId);
    deviceResponse.typ = device.assetType.id;
    if (device.imgUrls != null) {
      deviceResponse.setiUs(Arrays.asList(device.imgUrls.split(LogistimoConstant.COMMA)));
    }

    for (Tag tag : device.tags) {
      deviceResponse.tags.add(tag.tagName);
    }

    if (device.statusUpdatedTime != null && device.statusUpdatedTime > 0) {
      deviceResponse.drdy = new DeviceReadyResponse(device.statusUpdatedTime);
    }

    try {
      deviceResponse.sns =
          toTemperatureSensorRequests(TemperatureSensor.getTemperatureSensor(device));
    } catch (NoResultException ignored) {
      //do nothing
    }

    //Constructing device meta data json
    Map<String, String> metaDataMap = null;
    if (deviceMetaDatas != null) {
      metaDataMap = new HashMap<>(deviceMetaDatas.size());
      for (DeviceMetaData deviceMetaData : deviceMetaDatas) {
        metaDataMap.put(deviceMetaData.key, deviceMetaData.value);
      }
      deviceResponse.meta =
          Json.parse(LogistimoUtils.constructDeviceMetaJsonFromMap(metaDataMap).toString());
    }

    if (deviceStatusList != null) {
      for (DeviceStatus deviceStatus : deviceStatusList) {
        if (deviceStatus.statusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY)) {
          AssetTemperatureResponse assetTemperatureResponse = new AssetTemperatureResponse();
          assetTemperatureResponse.st = deviceStatus.status;
          assetTemperatureResponse.setMpId(deviceStatus.locationId);
          assetTemperatureResponse.setaSt(deviceStatus.temperatureAbnormalStatus);
          assetTemperatureResponse.stut = deviceStatus.statusUpdatedTime;
          assetTemperatureResponse.time = deviceStatus.temperatureUpdatedTime;
          assetTemperatureResponse.tmp = deviceStatus.temperature;
          assetTemperatureResponse.setsId(deviceStatus.sensorId);
          if (metaDataMap != null) {
            if (metaDataMap.containsKey(DeviceMetaAppendix.TMP_MIN)) {
              assetTemperatureResponse.min =
                  Double.parseDouble(metaDataMap.get(DeviceMetaAppendix.TMP_MIN));
            }

            if (metaDataMap.containsKey(DeviceMetaAppendix.TMP_MAX)) {
              assetTemperatureResponse.max =
                  Double.parseDouble(metaDataMap.get(DeviceMetaAppendix.TMP_MAX));
            }
          }
          deviceResponse.tmp.add(assetTemperatureResponse);
        } else if (deviceStatus.statusKey.equals(AssetStatusConstants.CONFIG_STATUS_KEY)) {
          DeviceStatusModel deviceStatusModel = new DeviceStatusModel();
          deviceStatusModel.st = deviceStatus.status;
          deviceStatusModel.stut = deviceStatus.statusUpdatedTime;
          deviceStatusModel.stub = deviceStatus.statusUpdatedBy;
          deviceResponse.cfg = deviceStatusModel;
        } else if (deviceStatus.statusKey.equals(AssetStatusConstants.WORKING_STATUS_KEY)) {
          DeviceStatusModel deviceStatusModel = new DeviceStatusModel();
          deviceStatusModel.st = deviceStatus.status;
          deviceStatusModel.stut = deviceStatus.statusUpdatedTime;
          deviceResponse.ws = deviceStatusModel;
        } else if (AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS.contains(deviceStatus.statusKey)) {
          AlarmResponse alarmResponse = new AlarmResponse();
          alarmResponse.stat = deviceStatus.status;
          alarmResponse.time = deviceStatus.statusUpdatedTime;
          alarmResponse.setsId(deviceStatus.sensorId);
          alarmResponse.setMpId(deviceStatus.locationId);
          alarmResponse.typ = Integer.parseInt(deviceStatus.statusKey.split("_")[1]);
          deviceResponse.alrm.add(alarmResponse);
        }
      }
    }

    if (assetUsers != null) {
      for (AssetUser assetUser : assetUsers) {
        if (assetUser.userType.equals(AssetUser.ASSET_OWNER)) {
          deviceResponse.ons.add(assetUser.userName);
        }

        if (assetUser.userType.equals(AssetUser.ASSET_MAINTAINER)) {
          deviceResponse.mts.add(assetUser.userName);
        }
      }
    }

    deviceResponse.rel = assetMapModelMap;

    return deviceResponse;
  }

  @SuppressWarnings("unchecked")
  private void createDevice(DeviceRequest deviceRequest) {
    if (deviceRequest != null) {
      Device device = new Device();
      device.deviceId = deviceRequest.dId;
      device.vendorId = deviceRequest.vId;
      device.transmitterId = deviceRequest.trId;
      device.createdBy = deviceRequest.cb;
      device.updatedBy = deviceRequest.cb;
      device.vendorName = deviceRequest.vNm;
      device.locationId = deviceRequest.getlId();
      if (deviceRequest.iUs != null) {
        device.imgUrls = StringUtils.join(deviceRequest.iUs, LogistimoConstant.COMMA);
      }
      if (deviceRequest.tags != null) {
        populateTags(device, deviceRequest.tags);
      }
      try {
        device.assetType = AssetType.getAssetType(deviceRequest.typ);
      } catch (NoResultException e) {
        device.assetType = AssetType.getAssetType(1);
      }
      device.save();

      //Creating users
      if (deviceRequest.ons != null) {
        for (String owner : deviceRequest.ons) {
          AssetUser assetUser = new AssetUser();
          assetUser.device = device;
          assetUser.userName = owner;
          assetUser.userType = AssetUser.ASSET_OWNER;
          assetUser.save();
        }
      }
      if (deviceRequest.mts != null) {
        for (String maintainer : deviceRequest.mts) {
          AssetUser assetUser = new AssetUser();
          assetUser.device = device;
          assetUser.userName = maintainer;
          assetUser.userType = AssetUser.ASSET_MAINTAINER;
          assetUser.save();
        }
      }

      //Creating device meta data
      try {
        Map<String, Object>
            result =
            new ObjectMapper().readValue(deviceRequest.meta.toString(), LinkedHashMap.class);
        if (result != null) {
          constructAndUpdateDeviceMetaDataFromMap(null,
              LogistimoUtils.constructDeviceMetaDataFromJSON(null, result), device);
        }

        //Creating default meta data, currently only inactive push interval counts
        DeviceMetaData deviceMetaData = new DeviceMetaData();
        deviceMetaData.key = DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT;
        deviceMetaData.value =
            String.valueOf(DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT_DEFAULT_VALUE);
        deviceMetaData.device = device;
        deviceMetaData.save();

      } catch (NoResultException ignored) {
        //do nothing
      } catch (Exception e) {
        LOGGER.error(
            "{} while extracting device meta data from json {}, creating device {}, {} without meta data",
            e.getMessage(), deviceRequest.meta, deviceRequest.vId, deviceRequest.dId, e);
      }

      //Initializing asset status
      for (String assetStatusKey : device.assetType.assetType.equals(AssetType.MONITORED_ASSET)
          ? AssetStatusConstants.MONITORED_DEVICE_STATUS_KEYS
          : AssetStatusConstants.DEVICE_STATUS_KEYS) {
        if (device.assetType.assetType.equals(AssetType.MONITORED_ASSET)
            && AssetStatusConstants.ASSET_SENSOR_STATUS_KEYS.contains(assetStatusKey)) {
          for (Integer monitoringPoint : deviceRequest.mps) {
            DeviceStatus
                deviceStatus =
                getOrCreateDeviceStatus(device, monitoringPoint, null, assetStatusKey,
                    assetStatusKey.equals(AssetStatusConstants.ACTIVITY_STATUS_KEY)
                        ? AssetStatusConstants.ACTIVITY_STATUS_INACT
                        : AssetStatusConstants.STATUS_OK);

            if (assetStatusKey.equals(AssetStatusConstants.ACTIVITY_STATUS_KEY)) {
              postDeviceStatus(device, deviceStatus);
            }
          }
        } else if (device.assetType.assetType.equals(AssetType.MONITORING_ASSET)
            && AssetStatusConstants.ASSET_SENSOR_STATUS_KEYS.contains(assetStatusKey)
            && deviceRequest.sns != null && !deviceRequest.sns.isEmpty()) {
          for (TemperatureSensorRequest temperatureSensorRequest : deviceRequest.sns) {
            getOrCreateDeviceStatus(device, null, temperatureSensorRequest.getsId(), assetStatusKey,
                null);
          }
        } else {
          DeviceStatus
              deviceStatus =
              getOrCreateDeviceStatus(device, null, null, assetStatusKey, null);

          if (assetStatusKey.equals(AssetStatusConstants.WORKING_STATUS_KEY)) {
            postDeviceStatus(device, deviceStatus);
          }
        }
      }

      //Create new device status log
      DeviceStatus
          deviceStatus =
          getOrCreateDeviceStatus(device, null, null, AssetStatusConstants.WORKING_STATUS_KEY,
              null);
      createDeviceStatusLog(deviceStatus, deviceRequest.cb);

      //Creating temperature sensors and virtual devices.
      if (deviceRequest.sns != null && !deviceRequest.sns.isEmpty()) {
        createOrUpdateTemperatureSensors(device, deviceRequest);
      }
    }
  }

  private DeviceConfigurationResponse toDeviceConfigurationResponse(
      DeviceConfiguration deviceConfiguration) {
    DeviceConfigurationResponse deviceConfigurationResponse = new DeviceConfigurationResponse();
    deviceConfigurationResponse.data =
        new Gson().fromJson(deviceConfiguration.configuration, ConfigurationRequest.class);

    if (deviceConfigurationResponse.data != null && deviceConfigurationResponse.data.locale != null
        && deviceConfigurationResponse.data.locale.tznm != null
        && !deviceConfigurationResponse.data.locale.tznm.isEmpty()) {
      TimeZone timeZone = TimeZone.getTimeZone(deviceConfigurationResponse.data.locale.tznm);
      deviceConfigurationResponse.data.locale.tz =
          (double) (timeZone.getRawOffset() + timeZone.getDSTSavings()) / (60 * 60 * 1000);
    }
    return deviceConfigurationResponse;
  }

  private DeviceConfiguration toDeviceConfiguration(
      DeviceConfigurationRequest deviceConfigurationRequest, Device device, Tag tag) {
    DeviceConfiguration deviceConfiguration = new DeviceConfiguration();

    deviceConfiguration.configuration =
        Json.toJson(deviceConfigurationRequest.configuration).toString();
    deviceConfiguration.device = device;
    deviceConfiguration.tag = tag;

    return deviceConfiguration;
  }

  private void populateTags(Device device, List<String> tags) {
    for (String tagName : tags) {
      Tag tag = Tag.findOrCreateByName(tagName);
      device.tags.add(tag);
    }
  }

  private void updateDeviceConfiguration(DeviceConfiguration deviceConfiguration,
                                         DeviceConfigurationRequest deviceConfigurationRequest) {
    deviceConfiguration.configuration =
        Json.toJson(deviceConfigurationRequest.configuration).toString();
    deviceConfiguration.update();
  }

  private Set<Tag> getTags(Set<String> tagRequest) {
    Set<Tag> tags = new TreeSet<Tag>();
    for (String tagName : tagRequest) {
      try {
        Tag tag = Tag.find(tagName);
        tags.add(tag);
      } catch (NoResultException e) {
        LOGGER.warn("No tag found for name : " + tagName);
      }
    }

    return tags;
  }

  private void updateDeviceConfigurationByTags(Set<Tag> tags,
                                               DeviceConfigurationRequest deviceConfigurationRequest,
                                               Device device) {
    DeviceConfiguration deviceConfiguration;
    for (Tag tag : tags) {
      try {
        if (device != null) {
          deviceConfiguration = DeviceConfiguration.getDeviceConfigByDeviceTag(device, tag);
        } else {
          deviceConfiguration = DeviceConfiguration.getDeviceConfigByTag(tag);
        }
        updateDeviceConfiguration(deviceConfiguration, deviceConfigurationRequest);
      } catch (NoResultException e) {
        deviceConfiguration = toDeviceConfiguration(deviceConfigurationRequest, device, tag);
        deviceConfiguration.save();
      }
    }
  }

  private TaggedDeviceResponse getPaginatedTaggedDeviceResponse(
      TaggedDeviceResponse taggedDeviceResponse, int pageNumber, int pageSize) {
    pageSize = LogistimoUtils.transformPageSize(pageSize);
    //taggedDeviceResponse.setnPages(LogistimoUtils.availableNumberOfPages(pageSize, taggedDeviceResponse.data.size()));
    int offset = LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize);

    if (offset > taggedDeviceResponse.data.size()) {
      taggedDeviceResponse.data = new ArrayList<DeviceResponse>();
    } else if (offset + pageSize > taggedDeviceResponse.data.size()) {
      taggedDeviceResponse.data =
          taggedDeviceResponse.data.subList(offset, taggedDeviceResponse.data.size());
    } else {
      taggedDeviceResponse.data = taggedDeviceResponse.data.subList(offset, offset + pageSize);
    }

    return taggedDeviceResponse;
  }

  public void validateDeviceConfigurationRequest(
      DeviceConfigurationRequest deviceConfigurationRequest) throws LogistimoException {
    if (deviceConfigurationRequest.vId == null) {
      throw new LogistimoException("Vendor Id(vId) is required.");
    }

    if (deviceConfigurationRequest.dId == null) {
      throw new LogistimoException("Device Id(dId) is required.");
    }

    if (deviceConfigurationRequest.configuration == null) {
      throw new LogistimoException("Configuration(configuration) is required.");
    }

    if (deviceConfigurationRequest.configuration.comm.chnl < 0
        || deviceConfigurationRequest.configuration.comm.chnl > 2) {
      throw new LogistimoException("Communication channel(comm.chnl) is null/invalid.");
    }

    if (!LogistimoValidationUtils
        .validateURL(deviceConfigurationRequest.configuration.comm.tmpUrl)) {
      throw new LogistimoException("Temperature communication URL(comm.tmpUrl) is null/invalid.");
    }

    if (!LogistimoValidationUtils
        .validatePhoneNumber(deviceConfigurationRequest.configuration.comm.smsGyPh)) {
      throw new LogistimoException(
          Messages.get(LogistimoConstant.VALIDATION_CONFIG_SMSGYPH_NOTFOUND));
    }

    if (!LogistimoValidationUtils
        .validateDuration(deviceConfigurationRequest.configuration.comm.pushInt)) {
      throw new LogistimoException("Communication push interval(comm.pushInt) is null/invalid..");
    }

    if (!LogistimoValidationUtils
        .validateDuration(deviceConfigurationRequest.configuration.comm.samplingInt)) {
      throw new LogistimoException("Communication sampling interval(comm.samplingInt) invalid.");
    }

    if (deviceConfigurationRequest.configuration.highAlarm == null
        || !LogistimoValidationUtils
        .validateDuration(deviceConfigurationRequest.configuration.highAlarm.dur)
        || deviceConfigurationRequest.configuration.highAlarm.temp == -1000) {
      throw new LogistimoException("High alarm details invalid.");
    }

    if (deviceConfigurationRequest.configuration.comm.devAlrmsNotify
        && !LogistimoValidationUtils
        .validateURL(deviceConfigurationRequest.configuration.comm.alrmUrl)) {
      throw new LogistimoException("Alarm URL is required, when devAlrmsNotify is true");
    }

    if (deviceConfigurationRequest.configuration.comm.statsNotify
        && !LogistimoValidationUtils
        .validateURL(deviceConfigurationRequest.configuration.comm.statsUrl)) {
      throw new LogistimoException("Stats URL is required, when statsNotify is true");
    }
  }

  /**
   * Update the device ready status, if available otherwise creates new entry
   */
  public DeviceReadyUpdateResponse createOrUpdateDeviceReady(
      DeviceReadyUpdateRequest deviceReadyUpdateRequest) {
    DeviceReadyUpdateResponse deviceReadyUpdateResponse = new DeviceReadyUpdateResponse();

    for (DeviceReadyRequest deviceReadyRequest : deviceReadyUpdateRequest.data) {
      try {
        Device device = findDevice(deviceReadyUpdateRequest.vId, deviceReadyRequest.getdId());
        updateDeviceStatus(device, deviceReadyRequest);
        device.update();
      } catch (NoResultException e) {
        deviceReadyUpdateResponse.errs.add(deviceReadyRequest.getdId() + "(Device not found)");
      }
    }

    return deviceReadyUpdateResponse;
  }

  private Device updateDeviceStatus(Device device, DeviceReadyRequest deviceReadyRequest) {
    if (deviceReadyRequest.dev != null) {
      if (StringUtils.isNotEmpty(deviceReadyRequest.dev.mdl)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.DEV_MODEL_FROM_DEVICE,
            deviceReadyRequest.dev.mdl);
      }

      if (StringUtils.isNotEmpty(deviceReadyRequest.dev.getdVr())) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.DEV_DVR,
            deviceReadyRequest.dev.getdVr());
      }

      if (StringUtils.isNotEmpty(deviceReadyRequest.dev.getmVr())) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.DEV_MVR,
            deviceReadyRequest.dev.getmVr());
      }

      if (StringUtils.isNotEmpty(deviceReadyRequest.dev.imei)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.DEV_IMEI,
            deviceReadyRequest.dev.imei);
      }
    }

    if (deviceReadyRequest.sim != null) {
      if (StringUtils.isNotEmpty(deviceReadyRequest.sim.sid)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.GSM_SIM_SIMID,
            deviceReadyRequest.sim.sid);
      }

      if (StringUtils.isNotEmpty(deviceReadyRequest.sim.phn)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.GSM_SIM_PHN_NUMBER,
            deviceReadyRequest.sim.phn);
      }
    }

    if (deviceReadyRequest.altSim != null) {
      if (StringUtils.isNotEmpty(deviceReadyRequest.altSim.sid)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.GSM_ALTSIM_SIMID,
            deviceReadyRequest.altSim.sid);
      }

      if (StringUtils.isNotEmpty(deviceReadyRequest.altSim.phn)) {
        createOrUpdateDeviceMetaData(device, DeviceMetaAppendix.GSM_ALTSIM_PHN_NUMBER,
            deviceReadyRequest.altSim.phn);
      }
    }

    if (deviceReadyRequest.actSns != null) {
      updateTemperatureSensorStatus(device, deviceReadyRequest.actSns);
    } else {
      updateTemperatureSensorStatus(device, new ArrayList<String>(1));
    }

    device.statusUpdatedTime = System.currentTimeMillis() / 1000;

    return device;
  }

  public void pushConfigToDevice(ConfigurationPushRequest configurationPushRequest)
      throws LogistimoException, IOException {
    DeviceStatus deviceStatus = null;
    Integer status = AssetStatusConstants.CONFIG_STATUS_PUSHED;

    Device device;
    try {
      device = findDevice(configurationPushRequest.vId, configurationPushRequest.dId);
    } catch (NoResultException e) {
      LOGGER.warn("Error while pushing configuration, device not found: {}, {}",
          configurationPushRequest.vId,
          configurationPushRequest.dId, e);
      throw new NoResultException(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND));
    }

    String
        countryCode =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.LOCALE_COUNTRYCODE,
            LogistimoConstant.DEFAULT_COUNTRY_CODE);

    if (!Arrays.asList(Play.application().configuration().getString(SUPPORT_CONFIG_PUSH_VENDORS)
        .split(LogistimoConstant.SUPPORT_VENDOR_SEP)).contains(configurationPushRequest.vId)) {
      LOGGER.warn("Error while pushing configuration. Feature not supported for device: {}, {}",
          device.vendorId, device.deviceId);
      throw new LogistimoException(Messages.get(FEATURE_NOT_SUPPORTED));
    }

    String
        phoneNumber =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.GSM_SIM_PHN_NUMBER);
    if (phoneNumber == null) {
      LOGGER.warn("Error while pushing configuration. Invalid mobile number");
      throw new LogistimoException(Messages.get(INVALID_DEVICE_PHONE_NUMBER));
    }

    String
        requestUrl =
        Play.application().configuration().getString(CONFIG_PUSH_URL + "." + device.vendorId);
    if (configurationPushRequest.typ == 1 && configurationPushRequest.data == null) {
      ConfigurationRequest data = getDeviceConfiguration(device).data;
      configurationPushRequest.data = data;
      status = AssetStatusConstants.CONFIG_STATUS_PUSHED;
    } else if (configurationPushRequest.typ == 0 && (configurationPushRequest.url == null
        || configurationPushRequest.url.isEmpty())) {
      DeviceConfiguration deviceConfiguration = null;
      DeviceConfigurationResponse deviceConfigurationResponse = null;
      try {
        deviceConfiguration = DeviceConfiguration.getDeviceConfiguration(device);
        deviceConfigurationResponse = toDeviceConfigurationResponse(deviceConfiguration);
        status = AssetStatusConstants.CONFIG_PULL_REQUEST_SENT;
      } catch (NoResultException e) {
        //do nothing
      }

      if (deviceConfiguration != null
          && deviceConfigurationResponse != null
          && deviceConfigurationResponse.data != null
          && deviceConfigurationResponse.data.comm != null
          && deviceConfigurationResponse.data.comm.cfgUrl != null
          && !deviceConfigurationResponse.data.comm.cfgUrl.isEmpty()) {
        configurationPushRequest.url = deviceConfigurationResponse.data.comm.cfgUrl;
      } else {
        LOGGER.info(
            "Configuration/Configuration URL not defined for device: {} {}, picking system configuration pull URL.",
            device.deviceId, device.vendorId);
        configurationPushRequest.url =
            Play.application().configuration().getString(CONFIG_PULL_URL);
      }
      status = AssetStatusConstants.CONFIG_PULL_REQUEST_SENT;
    } else {
      throw new LogistimoException("Invalid push request, type should be either 1 or 0");
    }

    configurationPushRequest.dvs.add(
        new DeviceDetails(
            configurationPushRequest.dId,
            phoneNumber,
            countryCode));

    smsService.sendSMS(requestUrl, configurationPushRequest);

    updateDeviceStatus(configurationPushRequest.stub, status, device);

    saveDeviceConfigPushStatus(device);
  }

  private void saveDeviceConfigPushStatus(Device device) {
    DeviceConfigurationPushStatus
        deviceConfigurationPushStatus =
        new DeviceConfigurationPushStatus();
    deviceConfigurationPushStatus.device = device;
    deviceConfigurationPushStatus.status = DeviceRequestStatus.SMS_SENT;
    deviceConfigurationPushStatus.sent_time = new Date();
    deviceConfigurationPushStatus.save();
  }

  private void updateDeviceStatus(String statusUpdatedBy, Integer status,
                                  Device device) {
    DeviceStatus deviceStatus;
    deviceStatus = getOrCreateDeviceStatus(device, null, null,
        AssetStatusConstants.CONFIG_STATUS_KEY,
        null);
    deviceStatus.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
    deviceStatus.status = status;
    deviceStatus.statusUpdatedBy = statusUpdatedBy;
    deviceStatus.update();
  }

  public void pushAPNSettingsToDevice(APNPushRequest apnPushRequest) throws LogistimoException {
    Device device = findDevice(apnPushRequest.getvId(), apnPushRequest.getdId());

    String
        countryCode =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.LOCALE_COUNTRYCODE);
    if (countryCode == null) {
      countryCode = LogistimoConstant.DEFAULT_COUNTRY_CODE;
    }

    if (!Arrays.asList(Play.application().configuration().getString(SUPPORT_APN_PUSH_VENDORS)
        .split(LogistimoConstant.SUPPORT_VENDOR_SEP)).contains(apnPushRequest.getvId())) {
      LOGGER.warn("Error while pushing APN settings. Feature not supported for device: {}, {}",
          device.vendorId, device.deviceId);
      throw new LogistimoException(Messages.get(FEATURE_NOT_SUPPORTED));
    }

    String
        phoneNumber =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.GSM_SIM_PHN_NUMBER);
    if (phoneNumber != null) {
      try {
        String
            request_url =
            Play.application().configuration()
                .getString(APN_PUSH_URL + "." + apnPushRequest.getvId());
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(LogistimoConstant.COUNTRY_CODE_PARAM, countryCode);
        requestParams.put(LogistimoConstant.PHONE_PARAM, URLEncoder.encode(phoneNumber, "UTF-8"));
        request_url = HttpUtil.replace(request_url, requestParams);

        DeviceAPNSettings deviceAPNSettings = null;
        try {
          deviceAPNSettings = DeviceAPNSettings.findByDevice(device);
          toDeviceAPNSettings(deviceAPNSettings, apnPushRequest, device);
          deviceAPNSettings.update();
        } catch (NoResultException e) {
          LOGGER.warn("APN Settings not found, creating new for device: {}, {}", device.vendorId,
              device.deviceId);
        }

        if (deviceAPNSettings == null) {
          deviceAPNSettings = new DeviceAPNSettings();
          toDeviceAPNSettings(deviceAPNSettings, apnPushRequest, device);
          deviceAPNSettings.save();
        }

        smsService.sendSMS(request_url, apnPushRequest);
      } catch (IOException e) {
        LOGGER.error("Error while pushing APN settings for device: {}, {}", device.vendorId,
            device.deviceId, e);
        throw new LogistimoException(e.getMessage());
      }
    } else {
      LOGGER.error("Error while pushing APN settings. Invalid mobile number for device: {}, {}",
          device.vendorId, device.deviceId);
      throw new LogistimoException(Messages.get(INVALID_DEVICE_PHONE_NUMBER));
    }
  }

  public APNPushRequest getAPNSettings(Device device) {
    try {
      return toApnPushRequest(DeviceAPNSettings.findByDevice(device));
    } catch (NoResultException e) {
      LOGGER.error("Error while getting APN Settings, APN settings not found for device: {}, {}",
          device.vendorId, device.deviceId, e);
      throw new NoResultException(Messages.get(DEVICE_NOT_FOUND));
    }
  }

  public APNPushRequest getAPNSettings(String vendorId, String deviceId) {
    try {
      return getAPNSettings(findDevice(vendorId, deviceId));
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting APN Settings, APN Settings not found for device: {}, {}"
          , vendorId, deviceId, e);
      throw new NoResultException(Messages.get(DEVICE_NOT_FOUND));
    }
  }

  public AdminPushRequest getAdminSettings(String vendorId, String deviceId) {
    try {
      Device device = findDevice(vendorId, deviceId);
      try {
        DeviceAdminSettings deviceAdminSettings = DeviceAdminSettings.findByDevice(device);
        return toAdminPushRequest(deviceAdminSettings);
      } catch (NoResultException e) {
        LOGGER.warn("Error while getting Admin Settings, no admin settings for device: {}, {}",
            vendorId, deviceId, e);
        throw new NoResultException(Messages.get(DEVICE_NOT_FOUND));
      }
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting admin Settings, device: {} {} not found", vendorId, deviceId,
          e);
      throw new NoResultException(Messages.get(DEVICE_NOT_FOUND));
    }
  }

  public void pushAdminSettingsToDevice(AdminPushRequest adminPushRequest)
      throws LogistimoException {
    Device device = findDevice(adminPushRequest.getvId(), adminPushRequest.getdId());
    DeviceAdminSettingsPushStatus
        deviceAdminSettingsPushStatus =
        new DeviceAdminSettingsPushStatus();
    deviceAdminSettingsPushStatus.device = device;

    String
        countryCode =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.LOCALE_COUNTRYCODE);
    if (countryCode == null) {
      countryCode = LogistimoConstant.DEFAULT_COUNTRY_CODE;
    }

    if (!Arrays.asList(Play.application().configuration().getString(SUPPORT_ADMIN_PUSH_VENDORS)
        .split(LogistimoConstant.SUPPORT_VENDOR_SEP)).contains(adminPushRequest.getvId())) {
      LOGGER.warn("Error while pushing Admin settings. Feature not supported for device: {}, {}",
          device.vendorId, device.deviceId);
      throw new LogistimoException(Messages.get(FEATURE_NOT_SUPPORTED));
    }

    String
        phoneNumber =
        getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.GSM_SIM_PHN_NUMBER);
    if (phoneNumber != null) {
      try {
        String
            request_url =
            Play.application().configuration()
                .getString(ADMIN_PUSH_URL + "." + adminPushRequest.getvId());
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put(LogistimoConstant.COUNTRY_CODE_PARAM, countryCode);
        requestParams.put(LogistimoConstant.PHONE_PARAM, URLEncoder.encode(phoneNumber, "UTF-8"));
        request_url = HttpUtil.replace(request_url, requestParams);

        DeviceAdminSettings deviceAdminSettings = null;

        try {
          deviceAdminSettings = DeviceAdminSettings.findByDevice(device);
          toDeviceAdminSettings(deviceAdminSettings, adminPushRequest, device);
          deviceAdminSettings.update();
        } catch (NoResultException e) {
          LOGGER.info("Admin setting not found, creating new.");
        }

        if (deviceAdminSettings == null) {
          deviceAdminSettings = new DeviceAdminSettings();
          toDeviceAdminSettings(deviceAdminSettings, adminPushRequest, device);
          deviceAdminSettings.save();
        }

        smsService.sendSMS(request_url, adminPushRequest);

        deviceAdminSettingsPushStatus.status = DeviceRequestStatus.SMS_SENT;
        deviceAdminSettingsPushStatus.sent_time = new Date();
        deviceAdminSettingsPushStatus.save();
      } catch (IOException e) {
        LOGGER.error("Error while pushing Admin settings. For device: {}, {}",
            adminPushRequest.getvId(),
            adminPushRequest.getdId(), e);
        throw new LogistimoException(e.getMessage());
      }
    } else {
      LOGGER.error("Error while pushing Admin settings. Invalid mobile number For device: {}, {}"
          , adminPushRequest.getvId(), adminPushRequest.getdId());
      throw new LogistimoException(Messages.get(INVALID_DEVICE_PHONE_NUMBER));
    }
  }

  public void updateConfigSentStatus(DeviceSMSStatusRequest deviceSMSStatusRequest)
      throws LogistimoException {
    try {
      Device device = findDevice(deviceSMSStatusRequest.getvId(), deviceSMSStatusRequest.getdId());

      DeviceConfigurationPushStatus
          deviceConfigurationPushStatus =
          DeviceConfigurationPushStatus.findSentByDevice(device);
      deviceConfigurationPushStatus.acknowledged_time = new Date(deviceSMSStatusRequest.receivedOn);

      if (deviceSMSStatusRequest.isAcknowledged) {
        deviceConfigurationPushStatus.status = DeviceRequestStatus.SMS_ACK;
      } else {
        deviceConfigurationPushStatus.status = DeviceRequestStatus.SMS_F_ERROR;
        deviceConfigurationPushStatus.errorCode = deviceSMSStatusRequest.errCode;
        deviceConfigurationPushStatus.errorMessage = deviceSMSStatusRequest.errKeycode;
      }

      deviceConfigurationPushStatus.update();
    } catch (NoResultException e) {
      LOGGER.error("Error while updating config push status. For device: {}, {}"
          , deviceSMSStatusRequest.getvId(), deviceSMSStatusRequest.getdId(), e);
      throw new LogistimoException(e.getMessage());
    }
  }

  public void updateAdminSentStatus(DeviceSMSStatusRequest deviceSMSStatusRequest)
      throws LogistimoException {
    try {
      Device device = findDevice(deviceSMSStatusRequest.getvId(), deviceSMSStatusRequest.getdId());

      DeviceAdminSettingsPushStatus
          deviceAdminSettingsPushStatus =
          DeviceAdminSettingsPushStatus.findSentByDevice(device);
      deviceAdminSettingsPushStatus.acknowledged_time = new Date(deviceSMSStatusRequest.receivedOn);

      if (deviceSMSStatusRequest.isAcknowledged) {
        deviceAdminSettingsPushStatus.status = DeviceRequestStatus.SMS_ACK;
      } else {
        deviceAdminSettingsPushStatus.status = DeviceRequestStatus.SMS_F_ERROR;
        deviceAdminSettingsPushStatus.errorCode = deviceSMSStatusRequest.errCode;
        deviceAdminSettingsPushStatus.errorMessage = deviceSMSStatusRequest.errKeycode;
      }

      deviceAdminSettingsPushStatus.update();
    } catch (NoResultException e) {
      LOGGER.error("Error while Admin settings push status. For device: {}, {}"
          , deviceSMSStatusRequest.getvId(), deviceSMSStatusRequest.getdId(), e);
      throw new LogistimoException(e.getMessage());
    }
  }

  private void toDeviceAPNSettings(DeviceAPNSettings deviceAPNSettings,
                                   APNPushRequest apnPushRequest, Device device) {
    deviceAPNSettings.name = apnPushRequest.getApn().name;
    deviceAPNSettings.address = apnPushRequest.getApn().addr;
    deviceAPNSettings.device = device;
    if (apnPushRequest.altApn != null) {
      deviceAPNSettings.altName = apnPushRequest.altApn.name;
      deviceAPNSettings.altAddress = apnPushRequest.altApn.addr;
    }
    if (apnPushRequest.getUsr() != null) {
      deviceAPNSettings.username = apnPushRequest.getUsr().name;
      deviceAPNSettings.password = apnPushRequest.getUsr().pwd;
    }
  }

  private APNPushRequest toApnPushRequest(DeviceAPNSettings deviceAPNSettings) {
    APNPushRequest apnPushRequest = new APNPushRequest();

    String
        countryCode =
        getDeviceMetaDataValueAsString(deviceAPNSettings.device,
            DeviceMetaAppendix.LOCALE_COUNTRYCODE);
    String
        phoneNumber =
        getDeviceMetaDataValueAsString(deviceAPNSettings.device,
            DeviceMetaAppendix.GSM_SIM_PHN_NUMBER);

    apnPushRequest.cn = countryCode != null ? countryCode : LogistimoConstant.DEFAULT_COUNTRY_CODE;
    apnPushRequest.phone = phoneNumber;
    apnPushRequest.setvId(deviceAPNSettings.device.vendorId);
    apnPushRequest.setdId(deviceAPNSettings.device.deviceId);
    apnPushRequest.getApn().name = deviceAPNSettings.name;
    apnPushRequest.getApn().addr = deviceAPNSettings.address;
    apnPushRequest.getUsr().name = deviceAPNSettings.username;
    apnPushRequest.getUsr().pwd = deviceAPNSettings.password;
    apnPushRequest.altApn.name = deviceAPNSettings.altName;
    apnPushRequest.altApn.addr = deviceAPNSettings.altAddress;

    return apnPushRequest;
  }

  private void toDeviceAdminSettings(DeviceAdminSettings deviceAdminSettings,
                                     AdminPushRequest adminPushRequest, Device device) {
    deviceAdminSettings.device = device;
    deviceAdminSettings.phoneNumber = adminPushRequest.adm.phone;
    deviceAdminSettings.password = adminPushRequest.adm.pwd;
    deviceAdminSettings.senderId = adminPushRequest.adm.senderId;
  }

  private AdminPushRequest toAdminPushRequest(DeviceAdminSettings deviceAdminSettings) {
    AdminPushRequest adminPushRequest = new AdminPushRequest();

    adminPushRequest.setvId(deviceAdminSettings.device.vendorId);
    adminPushRequest.setdId(deviceAdminSettings.device.deviceId);
    adminPushRequest.adm.phone = deviceAdminSettings.phoneNumber;
    adminPushRequest.adm.pwd = deviceAdminSettings.password;
    adminPushRequest.adm.senderId = deviceAdminSettings.senderId;

    return adminPushRequest;
  }

  private List<AlertResponse> toRecentAlertResponses(List<AlarmLog> alarmLogs) {
    List<AlertResponse> alertResponseList = new ArrayList<AlertResponse>(1);
    for (AlarmLog alarmLog : alarmLogs) {
      AlertResponse alertResponse = new AlertResponse();
      alertResponse.typ = alarmLog.alarmType;
      alertResponse.time = alarmLog.startTime;
      if (alarmLog.alarmType == AlarmLog.TEMP_ALARM) {
        AssetTemperatureResponse assetTemperatureResponse = new AssetTemperatureResponse();
        assetTemperatureResponse.st = alarmLog.temperatureType;
        assetTemperatureResponse.setaSt(alarmLog.temperatureAbnormalType);
        assetTemperatureResponse.setMpId(alarmLog.monitoringPositionId);
        assetTemperatureResponse.tmp = alarmLog.temperature;
        assetTemperatureResponse.time = alarmLog.startTime;
        alertResponse.tmpalm = assetTemperatureResponse;
      }
      if (alarmLog.alarmType == AlarmLog.DEVICE_ALARM) {
        alertResponse.devalm =
            new AlarmResponse(alarmLog.deviceAlarmType,
                alarmLog.deviceAlarmStatus != null ? alarmLog.deviceAlarmStatus : -1,
                alarmLog.deviceFirmwareErrorCode, alarmLog.sensorId);
      }
      alertResponseList.add(alertResponse);
    }
    return alertResponseList;
  }

  public List<Device> getActiveSensorDevices() {
    return Device.getActiveSensorDevices();
  }

  public TemperatureSensor getTemperatureSensors(Device device, String sensorId) {
    return TemperatureSensor.getTemperatureSensor(device, sensorId);
  }

  public List<TemperatureSensor> getTemperatureSensors(Device device) {
    return TemperatureSensor.getTemperatureSensor(device);
  }

  private void createOrUpdateTemperatureSensors(Device device, DeviceRequest deviceRequest) {
    for (TemperatureSensorRequest temperatureSensorRequest : deviceRequest.sns) {
      TemperatureSensor temperatureSensor = null;
      try {
        temperatureSensor =
            TemperatureSensor.getTemperatureSensor(device, temperatureSensorRequest.getsId());
        temperatureSensor.code = temperatureSensorRequest.cd;
        temperatureSensor.update();
      } catch (NoResultException ignored) {
        //do nothing
      }

      //Create new sensor, if not found
      if (temperatureSensor == null) {
        temperatureSensor = new TemperatureSensor();
        temperatureSensor.sensorId = temperatureSensorRequest.getsId();
        temperatureSensor.code = temperatureSensorRequest.cd;
        temperatureSensor.device = device;
        temperatureSensor.save();
      }

      //Creating virtual temperature devices
      Device sensorDevice = null;
      try {
        sensorDevice =
            Device.findDevice(device.vendorId, LogistimoUtils
                .generateVirtualDeviceId(device.deviceId, temperatureSensorRequest.getsId()));

        //Propagating parent device model to sensors devices.
        createOrUpdateDeviceMetaData(sensorDevice, DeviceMetaAppendix.DEV_MODEL,
            getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.DEV_MODEL));
      } catch (NoResultException ignored) {
        //do nothing
      }

      if (sensorDevice == null) {
        sensorDevice = new Device();
        sensorDevice.vendorId = device.vendorId;
        sensorDevice.deviceId =
            LogistimoUtils
                .generateVirtualDeviceId(device.deviceId, temperatureSensorRequest.getsId());
        sensorDevice.createdBy = device.createdBy;
        sensorDevice.updatedBy = device.updatedBy;
        sensorDevice.assetType = AssetType.getAssetType(AssetType.TEMP_SENSOR);
        sensorDevice.save();

        //Propagating parent device model to sensors devices.
        createOrUpdateDeviceMetaData(sensorDevice, DeviceMetaAppendix.DEV_MODEL,
            getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.DEV_MODEL));

        //Initializing asset status
        for (String assetStatusKey : AssetStatusConstants.DEVICE_STATUS_KEYS) {
          getOrCreateDeviceStatus(sensorDevice, null, null, assetStatusKey, null);
        }

        //Creating asset mapping
        AssetMapModel assetMapModel = new AssetMapModel(LogistimoConstant.CONTAINS);
        createAssetMapping(assetMapModel, device, sensorDevice);
      }
    }
  }

  private List<TemperatureSensorRequest> toTemperatureSensorRequests(
      List<TemperatureSensor> temperatureSensors) {
    List<TemperatureSensorRequest> temperatureSensorRequests = new ArrayList<>(1);

    for (TemperatureSensor temperatureSensor : temperatureSensors) {
      temperatureSensorRequests.add(toTemperatureSensorRequest(temperatureSensor));
    }

    return temperatureSensorRequests;
  }

  private TemperatureSensorRequest toTemperatureSensorRequest(TemperatureSensor temperatureSensor) {
    TemperatureSensorRequest temperatureSensorRequest = new TemperatureSensorRequest();
    temperatureSensorRequest.setsId(temperatureSensor.sensorId);
    temperatureSensorRequest.cd = temperatureSensor.code;
    temperatureSensorRequest.isA = temperatureSensor.sensorStatus;
    return temperatureSensorRequest;
  }

  private void updateTemperatureSensorStatus(Device device, List<String> actSns) {
    try {
      List<TemperatureSensor> temperatureSensors = TemperatureSensor.getTemperatureSensor(device);
      for (TemperatureSensor temperatureSensor : temperatureSensors) {
        if (actSns.contains(temperatureSensor.sensorId)) {
          if (temperatureSensor.sensorStatus.equals(TemperatureSensor.STATUS_INACTIVE)) {
            temperatureSensor.sensorStatus = TemperatureSensor.STATUS_ACTIVE;
            temperatureSensor.update();
          }
        } else {
          if (temperatureSensor.sensorStatus.equals(TemperatureSensor.STATUS_ACTIVE)) {
            temperatureSensor.sensorStatus = TemperatureSensor.STATUS_INACTIVE;
            temperatureSensor.update();
          }
        }
      }
    } catch (NoResultException e) {
      LOGGER.warn("Sensor not found for the device: {}, {}. But received active sensor: {}",
          actSns.toArray());
    }
  }

  private List<DeviceMetaData> getDeviceMetaDatas(Device device) {
    return DeviceMetaData.getDeviceMetaDatas(device);
  }

  private DeviceMetaData getDeviceMetaByKey(Device device, String key) {
    return DeviceMetaData.getDeviceMetaDataByKey(device, key);
  }

  public String getDeviceMetaDataValueAsString(Device device, String key) {
    try {
      return getDeviceMetaByKey(device, key).value;
    } catch (NoResultException e) {
      return null;
    }
  }

  public String getDeviceMetaDataValueAsString(Device device, String key, String defaultValue) {
    try {
      return getDeviceMetaByKey(device, key).value;
    } catch (NoResultException e) {
      return defaultValue;
    }
  }

  public Map<String, String> getDeviceMetaDataValueAsString(Device device, List<String> keys) {
    Map<String, String> stringMap = new HashMap<>(keys.size());
    Map<String, DeviceMetaData> deviceMetaDataMap = getDeviceMetaDataMap(device, keys);
    for (DeviceMetaData deviceMetaData : deviceMetaDataMap.values()) {
      stringMap.put(deviceMetaData.key, deviceMetaData.value);
    }

    return stringMap;
  }

  private List<DeviceMetaData> getDeviceMetaDatas(Device device, List<String> keys) {
    return DeviceMetaData.getDeviceMetaDataByKeys(device, keys);
  }

  public Map<String, DeviceMetaData> getDeviceMetaDataMap(Device device, List<String> keys) {
    Map<String, DeviceMetaData> deviceMetaDataMap = null;
    try {
      List<DeviceMetaData> deviceMetaDataList = getDeviceMetaDatas(device, keys);
      deviceMetaDataMap = new HashMap<>(deviceMetaDataList.size());
      for (DeviceMetaData deviceMetaData : deviceMetaDataList) {
        deviceMetaDataMap.put(deviceMetaData.key, deviceMetaData);
      }
    } catch (NoResultException e) {
      //do nothing
    }
    return deviceMetaDataMap;
  }

  private void createOrUpdateDeviceMetaData(Device device, String key, String value) {
    DeviceMetaData deviceMetaData;
    try {
      //If meta found, updating the value
      deviceMetaData = getDeviceMetaByKey(device, key);
      deviceMetaData.value = value;
      deviceMetaData.update();
      return;
    } catch (NoResultException e) {
      //do nothing
    }
    //If meta data not found, creating new entry
    deviceMetaData = new DeviceMetaData(key, value, device);
    deviceMetaData.save();
  }

  private void constructAndUpdateDeviceMetaDataFromMap(List<DeviceMetaData> deviceMetaDataList,
                                                       Map<String, String> deviceMetaDataMap,
                                                       Device device) {
    if (deviceMetaDataList != null) {
      for (DeviceMetaData deviceMetaData : deviceMetaDataList) {
        if (deviceMetaDataMap.containsKey(deviceMetaData.key)) {
          deviceMetaData.value = deviceMetaDataMap.get(deviceMetaData.key);
          deviceMetaData.update();
          deviceMetaDataMap.remove(deviceMetaData.key);
        }
      }
    }

    if (deviceMetaDataMap.size() > 0) {
      for (String key : deviceMetaDataMap.keySet()) {
        DeviceMetaData deviceMetaData = new DeviceMetaData();
        deviceMetaData.key = key;
        deviceMetaData.value = deviceMetaDataMap.get(key);
        deviceMetaData.device = device;
        deviceMetaData.save();
      }
    }
  }

  public void createOrUpdateAssetMapping(
      AssetRegistrationRelationModel assetRegistrationRelationModel) {
    if (assetRegistrationRelationModel != null && assetRegistrationRelationModel.data != null) {
      for (AssetRelationModel assetRelationModel : assetRegistrationRelationModel.data) {
        try {
          Device device = findDevice(assetRelationModel.vId, assetRelationModel.dId);
          if (assetRelationModel.ras != null) {
            if (assetRelationModel.ras.size() == 0) {
              try {
                List<AssetMapping> assetMappingList = AssetMapping.findAssetRelationByAsset(device);
                Device monitoringDevice = null;
                for (AssetMapping assetMapping : assetMappingList) {
                  if (monitoringDevice == null) {
                    String
                        deviceId =
                        LogistimoUtils.extractDeviceId(assetMapping.relatedAsset.deviceId);
                    monitoringDevice = findDevice(assetMapping.relatedAsset.vendorId, deviceId);
                  }
                  DeviceStatus status = DeviceStatus.getDeviceStatus(device,
                      AssetStatusConstants.ACTIVITY_STATUS_KEY,
                      assetMapping.monitoringPositionId);
                  status.status = AssetStatusConstants.ACTIVITY_STATUS_INACT;
                  status.update();
                  postDeviceStatus(device, status);

                  DeviceStatus tempStatus = DeviceStatus.getDeviceStatus(device,
                      AssetStatusConstants.TEMP_STATUS_KEY,
                      assetMapping.monitoringPositionId);
                  tempStatus.status = AssetStatusConstants.TEMP_STATUS_NORMAL;
                  tempStatus.temperature = 0.0;
                  tempStatus.temperatureUpdatedTime = 0;
                  tempStatus.temperatureAbnormalStatus = 0;
                  tempStatus.update();
                  postDeviceStatus(device, tempStatus);

                  assetMapping.delete();
                }
                closePreviousAlarmLogs(device);
                closePreviousAlarmLogs(monitoringDevice);
                updateOverallDeviceStatus(device, AssetStatusConstants.TEMP_STATUS_KEY);

              } catch (NoResultException ignored) {
                //do nothing
              }
            } else {
              Device parentDevice = null;
              for (AssetMapModel assetMapModel : assetRelationModel.ras) {
                try {
                  boolean isSensor = StringUtils.isNotEmpty(assetMapModel.getsId());
                  String mappingAssetId = isSensor
                      ? LogistimoUtils
                      .generateVirtualDeviceId(assetMapModel.getdId(), assetMapModel.getsId())
                      : assetMapModel.getdId();
                  Device monitoringDevice = findDevice(assetMapModel.getvId(), mappingAssetId);
                  try {
                    AssetMapping
                        assetMapping =
                        AssetMapping.findAssetMappingByAssetAndMonitoringPosition(device,
                            assetMapModel.mpId);
                    assetMapping.relationType = assetMapModel.typ;
                    assetMapping.isPrimary = assetMapModel.isP;
                    assetMapping.relatedAsset = monitoringDevice;
                    assetMapping.update();
                    continue;
                  } catch (NoResultException ignored) {
                    //do nothing
                  }
                  //Creating new asset mapping if not exists.
                  createAssetMapping(assetMapModel, device, monitoringDevice);
                  DeviceStatus monitoringAssetStatus = null;
                  DeviceStatus monitoredAssetStatus = null;
                  DeviceStatus parentDeviceStatus = null;
                  int statusUpdateTime = (int)
                      (System.currentTimeMillis() / 1000);
                  for (String statusKey : AssetStatusConstants.ASSET_SENSOR_STATUS_KEYS) {
                    try {
                      monitoringAssetStatus =
                          DeviceStatus.getDeviceStatus(monitoringDevice, statusKey);
                      monitoredAssetStatus =
                          DeviceStatus.getDeviceStatus(device, statusKey, assetMapModel.mpId);
                      if (statusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY)) {
                        monitoringAssetStatus.status = AssetStatusConstants.STATUS_OK;
                        monitoringAssetStatus.temperatureAbnormalStatus =
                            AssetStatusConstants.STATUS_OK;
                        monitoringAssetStatus.statusUpdatedTime = statusUpdateTime;
                        monitoringAssetStatus.update();
                      } else {
                        monitoredAssetStatus.status =
                            monitoringAssetStatus.status;
                        monitoredAssetStatus.statusUpdatedTime =
                            monitoringAssetStatus.statusUpdatedTime;
                        monitoredAssetStatus.temperature =
                            monitoringAssetStatus.temperature;
                        monitoredAssetStatus.temperatureUpdatedTime =
                            monitoringAssetStatus.temperatureUpdatedTime;
                        monitoredAssetStatus.temperatureAbnormalStatus =
                            monitoringAssetStatus.temperatureAbnormalStatus;
                        monitoredAssetStatus.update();
                        postDeviceStatus(device, monitoredAssetStatus);
                      }
                    } catch (NoResultException e) {
                      LOGGER.warn("Device status not found: {}, {}", assetMapModel.getvId(),
                          assetMapModel.getdId());
                    }
                  }
                  for (String statusKey : AssetStatusConstants.DEVICE_STATUS_PROPAGATION_KEYS) {
                    try {
                      monitoringAssetStatus =
                          DeviceStatus.getDeviceStatus(monitoringDevice, statusKey);
                      monitoredAssetStatus =
                          DeviceStatus.getDeviceStatus(device, statusKey, assetMapModel.mpId);

                      monitoredAssetStatus.status = monitoringAssetStatus.status;
                      monitoredAssetStatus.statusUpdatedTime =
                          monitoringAssetStatus.statusUpdatedTime;
                      monitoredAssetStatus.update();

                      postDeviceStatus(device, monitoredAssetStatus);
                    } catch (NoResultException e) {
                      LOGGER.warn("Device status not found: {}, {}", assetMapModel.getvId(),
                          assetMapModel.getdId());
                    }
                  }

                  //Resetting the temperature status for parent device with sensor id
                  parentDevice = Device.findDevice(assetMapModel.getvId(), assetMapModel.getdId());
                  parentDeviceStatus =
                      DeviceStatus
                          .getDeviceStatus(parentDevice, AssetStatusConstants.TEMP_STATUS_KEY,
                              assetMapModel.getsId());
                  parentDeviceStatus.status = AssetStatusConstants.STATUS_OK;
                  parentDeviceStatus.temperatureAbnormalStatus = AssetStatusConstants.STATUS_OK;
                  parentDeviceStatus.statusUpdatedTime = statusUpdateTime;
                  parentDeviceStatus.update();
                  createAlarmLog(parentDeviceStatus,parentDevice);
                  //postDeviceStatus(parentDevice, parentDeviceStatus);
                } catch (NoResultException e) {
                  LOGGER.warn("Mapping device not found: {}, {}", assetMapModel.getvId(),
                      assetMapModel.getdId());
                }
              }
              updateOverallDeviceStatus(parentDevice, AssetStatusConstants.TEMP_STATUS_KEY);
            }
          }
        } catch (NoResultException e) {
          LOGGER.warn("Device not found while mapping device {}, {}", assetRelationModel.vId,
              assetRelationModel.dId);
        }
      }
    }
  }

  private void closePreviousAlarmLogs(Device device) {
    List<AlarmLog> alarmLogList;
    if (device.assetType.equals(AssetType.getAssetType(AssetType.MONITORED_ASSET))) {
      alarmLogList = AlarmLog.getOpenAlarmLogs(device);
      if (alarmLogList != null && alarmLogList.size() > 0) {
        for (AlarmLog alarmLog : alarmLogList) {
          alarmLog.endTime = (int) (System.currentTimeMillis() / 1000);
          alarmLog.update();
        }
      }
    }
  }

  private void createAssetMapping(AssetMapModel assetMapModel, Device device,
                                  Device mappingDevice) {
    AssetMapping assetMapping = new AssetMapping();
    assetMapping.asset = device;
    assetMapping.relatedAsset = mappingDevice;
    assetMapping.monitoringPositionId = assetMapModel.mpId;
    assetMapping.relationType = assetMapModel.typ;
    assetMapping.isPrimary = assetMapModel.isP;
    assetMapping.save();

    //Initializing asset status
    if (Objects.equals(assetMapModel.typ, LogistimoConstant.MONITORED_BY)) {
      for (String assetStatusKey : AssetStatusConstants.MONITORING_ASSET_STATUS_KEYS) {
        getOrCreateDeviceStatus(device, assetMapping.monitoringPositionId, null, assetStatusKey,
            null);
      }
    }
  }

  public Map<Integer, AssetMapModel> getAssetRelation(String vendorId, String deviceId) {
    Device device = findDevice(vendorId, deviceId);
    return toAssetMapModels(AssetMapping.findAssetRelationByAsset(device));
  }

  private Map<Integer, AssetMapModel> toAssetMapModels(List<AssetMapping> assetMappingList) {
    Map<Integer, AssetMapModel> assetRelationMap = new HashMap<>(assetMappingList.size());
    int index = 1;
    for (AssetMapping assetMapping : assetMappingList) {
      AssetMapModel assetMapModel = new AssetMapModel();
      List<DeviceMetaData> deviceMetaDatas = null;

      try {
        deviceMetaDatas = getDeviceMetaDatas(assetMapping.relatedAsset);
      } catch (NoResultException e) {
        LOGGER.warn("No device meta data found for the device: {}, {}",
            assetMapping.relatedAsset.vendorId, assetMapping.relatedAsset.deviceId);
      }

      assetMapModel.setdId(
          assetMapping.relatedAsset.assetType.id.equals(AssetType.TEMP_SENSOR) ? LogistimoUtils
              .extractDeviceId(assetMapping.relatedAsset.deviceId)
              : assetMapping.relatedAsset.deviceId);
      assetMapModel.setvId(assetMapping.relatedAsset.vendorId);
      assetMapModel.isP = assetMapping.isPrimary;
      assetMapModel.mpId = assetMapping.monitoringPositionId;
      assetMapModel.typ = assetMapping.relationType;
      assetMapModel.setsId(
          assetMapping.relatedAsset.assetType.id.equals(AssetType.TEMP_SENSOR) ? LogistimoUtils
              .extractSensorId(assetMapping.relatedAsset.deviceId) : null);
      Map<String, String> metaDataMap;
      if (deviceMetaDatas != null) {
        metaDataMap = new HashMap<>(deviceMetaDatas.size());
        for (DeviceMetaData deviceMetaData : deviceMetaDatas) {
          metaDataMap.put(deviceMetaData.key, deviceMetaData.value);
        }
        assetMapModel.meta =
            Json.parse(LogistimoUtils.constructDeviceMetaJsonFromMap(metaDataMap).toString());
      }
      assetRelationMap.put(
          assetMapping.monitoringPositionId != null ? assetMapping.monitoringPositionId : index,
          assetMapModel);
      index++;
    }
    return assetRelationMap;
  }

  public DeviceStatus getOrCreateDeviceStatus(Device device, Integer monitoringPositionId,
                                              String sensorId, String key, Integer status) {
    DeviceStatus deviceStatus = null;

    try {
      if (monitoringPositionId != null) {
        deviceStatus = DeviceStatus.getDeviceStatus(device, key, monitoringPositionId);
      } else if (sensorId != null) {
        deviceStatus = DeviceStatus.getDeviceStatus(device, key, sensorId);
      } else {
        deviceStatus = DeviceStatus.getDeviceStatus(device, key);
      }
      return deviceStatus;
    } catch (NoResultException e) {
      //do nothing
    }

    deviceStatus = new DeviceStatus();
    deviceStatus.device = device;
    deviceStatus.status = AssetStatusConstants.STATUS_OK;
    if (status != null) {
      deviceStatus.status = status;
    }
    deviceStatus.statusKey = key;
    deviceStatus.statusUpdatedTime = (int) (device.createdOn.getTime() / 1000);
    if (monitoringPositionId != null) {
      deviceStatus.locationId = monitoringPositionId;
    }

    if (sensorId != null) {
      deviceStatus.sensorId = sensorId;
    }
    deviceStatus.save();

    return deviceStatus;
  }

  private void createDeviceStatusLog(DeviceStatus deviceStatus, String updatedBy) {
    DeviceStatusLog statusLog = new DeviceStatusLog(deviceStatus.device.id);
    statusLog.status = deviceStatus.status;
    statusLog.startTime = deviceStatus.statusUpdatedTime;
    statusLog.statusUpdatedTime = deviceStatus.statusUpdatedTime;
    statusLog.updatedBy = updatedBy;
    statusLog.save();
  }

  public Integer getInactiveAssetCountByTag(String tagName, Integer assetType) {
    String innerQueryStr = "select count(1) from("
        + "select distinct device_id from device_status where status_key in('dsk_4') and status = 1 and device_id in("
        + "select id from devices where assetType_id = " + assetType + " and id in("
        + "select devices_id from devices_tags where tags_id in("
        + "select id from tags where tagname = '" + tagName + "'))))final";

    return new BigInteger(JPA.em().createNativeQuery(innerQueryStr).getSingleResult().toString())
        .intValue();
  }

  public Integer getTempMetricsByTag(String tagName, Integer assetType, Integer status) {
    List<String> statusKeyList = new ArrayList<String>() {{
      add(AssetStatusConstants.TEMP_STATUS_KEY);
    }};
    String innerQueryStr = "select count(1) from(" +
        "  select distinct device_id from device_status where status_key in :inclList and status =  "
        + status + " and device_id in(" +
        "    select distinct device_id from device_status where status_key = 'dsk_4' and device_id in("
        +
        "      select id from devices where assetType_id = " + assetType + " and id in(" +
        "        select devices_id from devices_tags where tags_id in(\n" +
        "          select id from tags where tagname = '" + tagName + "'" +
        "        )" +
        "      )" +
        "    ) group by device_id having max(status) = 0" +
        "  )" +
        ")final";

    return new BigInteger(
        JPA.em().createNativeQuery(innerQueryStr).setParameter("inclList", statusKeyList)
            .getSingleResult().toString()).intValue();
  }

  public Integer getAssetsCountForTag(String tagName, Integer assetType) {
    String
        innerQueryStr =
        "select count(1) from devices where assetType_id = " + assetType + " and id in("
            + "select devices_id from devices_tags where tags_id in("
            + "select id from tags where tagname = '" + tagName + "'))";

    return new BigInteger(JPA.em().createNativeQuery(innerQueryStr).getSingleResult().toString())
        .intValue();
  }

  public List<Device> getDeviceForTag(String tagName, String q, String assetType,
                                      Integer workingStatus, Integer alarmType,
                                      Integer alarmDuration, Integer awr,
                                      Integer startingOffset, Integer size) {
    String
        innerQueryStr =
        "select devices_id from devices_tags where tags_id in(select id from tags where tagname = '"
            + tagName + "')";

    if (workingStatus != null && workingStatus != -1) {
      innerQueryStr =
          "select device_id from device_status where status_key = '"
              + AssetStatusConstants.WORKING_STATUS_KEY
              + "' and status = " + workingStatus
              + " and device_id in(" + innerQueryStr + ")";
    }

    if (alarmType != null) {
      Integer alarmSince = (int) (System.currentTimeMillis() / 1000);
      if (alarmDuration != null && alarmDuration > 0) {
        alarmSince -= alarmDuration * 60;
      }
      if (alarmType == 1) {
        innerQueryStr =
            "select DISTINCT device_id from device_status where status_key = 'dsk_4' and device_id in("
                + innerQueryStr + ")"
                + "group by device_id having max(status) = 0";

        innerQueryStr =
            "select device_id from device_status where status_key = '"
                + AssetStatusConstants.TEMP_STATUS_KEY
                + "' and status = 3"
                + " and status_ut <= " + alarmSince
                + " and device_id in(" + innerQueryStr + ")";
      } else if (alarmType == 2) {
                /*innerQueryStr = "select DISTINCT device_id from device_status where status_key = 'dsk_4' and device_id in(" + innerQueryStr + ")"
                        + "group by device_id having max(status) = 0";*/
        innerQueryStr =
            "select DISTINCT device_id from device_status where status_key in ("
                + AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_CSV
                + ") and status > 0"
                + " and status_ut <= " + alarmSince
                + " and device_id in(" + innerQueryStr + ")";
      } else if (alarmType == 3) {
        innerQueryStr =
            "select DISTINCT device_id from device_status where status_key = '"
                + AssetStatusConstants.ACTIVITY_STATUS_KEY
                + "' and status > 0"
                + " and status_ut <= " + alarmSince
                + " and device_id in(" + innerQueryStr + ")";
      } else if (alarmType == 4) {
        innerQueryStr =
            "select DISTINCT device_id from device_status where status_key = 'dsk_4' and device_id in("
                + innerQueryStr + ")"
                + "group by device_id having max(status) = 0";

        Integer aty = null;
        if (assetType != null) {
          aty = Integer.parseInt(assetType.split(",")[0]);
        }
        if (Objects.equals(AssetType.getAssetType(aty).assetType, AssetType.MONITORING_ASSET)) {
          innerQueryStr =
              "select DISTINCT device_id from device_status where status_key in ("
                  + AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_CSV
                  + ")"
                  + " and device_id in(" + innerQueryStr
                  + ") group by device_id having max(status) = 0";
        } else {
          innerQueryStr =
              "select DISTINCT device_id from device_status where status_key = '"
                  + AssetStatusConstants.TEMP_STATUS_KEY + "'"
                  + " and device_id in(" + innerQueryStr
                  + ") group by device_id having max(status) < 3";
        }
      }
    }

    String queryStr = "select * from devices d where id in(" + innerQueryStr + ")";

    if (StringUtils.isNotEmpty(q)) {
      queryStr += " and deviceId like '" + q + "%'";
    }

    if (StringUtils.isNotEmpty(assetType)) {
      queryStr += " and assetType_id in (" + assetType + ")";
    }

    if (awr > 0) {
      if (AssetType.MONITORED_ASSET
          .equals(AssetType.getAssetType(Integer.parseInt(assetType.split(",")[0])).assetType)) {
        queryStr +=
            "AND " + (awr == 2 ? "NOT" : "")
                + " EXISTS(select 1 from asset_mapping where asset_id=d.id and relation_type=2 limit 1)";
      } else {
        queryStr +=
            "AND " + (awr == 2 ? "NOT" : "")
                + " EXISTS(select 1 from asset_mapping where relation_type = 2 and relatedAsset_id in (select relatedAsset_id from asset_mapping where asset_id=d.id and relation_type=1) limit 1)";
      }
    }

    return JPA.em().createNativeQuery(queryStr, Device.class)
        .setFirstResult(startingOffset)
        .setMaxResults(size)
        .getResultList();
  }

  public AssetPowerTransitions getDevicePowerTransition(String vendorId, String deviceId,
                                                        Integer from, Integer to) {
    return buildAssetPowerTransitions(
        DevicePowerTransition.getDevicePowerTransition(findDevice(vendorId, deviceId), from, to));
  }

  public AssetPowerTransitions buildAssetPowerTransitions(
      List<DevicePowerTransition> devicePowerTransitions) {
    AssetPowerTransitions assetPowerTransitions = new AssetPowerTransitions();
    for (DevicePowerTransition devicePowerTransition : devicePowerTransitions) {
      AssetPowerTransitions.AssetPowerTransition
          assetPowerTransition =
          new AssetPowerTransitions.AssetPowerTransition();
      assetPowerTransition.st = devicePowerTransition.state;
      assetPowerTransition.time = devicePowerTransition.time;
      assetPowerTransitions.data.add(assetPowerTransition);
    }
    return assetPowerTransitions;
  }

  /**
   * Propagates the status of all sensor id/monitoring position to device level
   */
  public void updateOverallDeviceStatus(Device device, String statusKey) {
    try {
      List<DeviceStatus>
          statuses =
          DeviceStatus.getDeviceStatuses(device, statusKey);
      DeviceStatus finalStatus = null;
      DeviceStatus currentOverallStatus = null;
      Integer latestStatusUpdateTime = null;
      for (DeviceStatus status : statuses) {
        if (status.sensorId == null && status.locationId == null) {
          currentOverallStatus = status;
        } else {
          if (finalStatus == null) {
            finalStatus = status;
            latestStatusUpdateTime = status.statusUpdatedTime;
          } else if (Objects.equals(statusKey, AssetStatusConstants.TEMP_STATUS_KEY) &&
              (status.status > finalStatus.status ||
                  Objects.equals(status.status, finalStatus.status)
                      && (status.temperatureAbnormalStatus > finalStatus.temperatureAbnormalStatus
                      || status.statusUpdatedTime > finalStatus.statusUpdatedTime))) {
            finalStatus = status;
          } else if (
              Objects.equals(statusKey, AssetStatusConstants.ACTIVITY_STATUS_KEY)
                  &&
                  (status.status < finalStatus.status ||
                      (Objects.equals(status.status, finalStatus.status)
                          && status.statusUpdatedTime > finalStatus.statusUpdatedTime))
              ) {
            finalStatus = status;
          }

          if(status.statusUpdatedTime > latestStatusUpdateTime){
            latestStatusUpdateTime = status.statusUpdatedTime;
          }
        }
      }

      if (finalStatus != null) {
        if (currentOverallStatus == null) {
          currentOverallStatus =
              getOrCreateDeviceStatus(device, null, null,
                  statusKey,
                  AssetStatusConstants.STATUS_OK);
        }
        if (!Objects.equals(currentOverallStatus.status, finalStatus.status) || !Objects
            .equals(currentOverallStatus.temperatureAbnormalStatus,
                finalStatus.temperatureAbnormalStatus)) {
          Integer previousStatusUpdatedTime = currentOverallStatus.statusUpdatedTime;
          Integer previousStatus = currentOverallStatus.status;
          currentOverallStatus.copyStatus(finalStatus, latestStatusUpdateTime);
          currentOverallStatus.update();

          createAlarmLog(currentOverallStatus, device);
        }
      }
    } catch (NoResultException e) {
      //do nothing
    }
  }

  /**
   * Creates the alarm log for the given device status and closes the older alarm log
   */
  public void createAlarmLog(DeviceStatus deviceStatus, Device device) {
    if (deviceStatus != null && device != null) {
      AlarmLog newAlarmLog;
      Integer
          alarmType =
          deviceStatus.statusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY) ? AlarmLog.TEMP_ALARM
              : AlarmLog.DEVICE_ALARM;
      Integer
          deviceAlarmType =
          deviceStatus.statusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY) ? null
              : getDeviceAlarmType(deviceStatus);

      try {
        AlarmLog oldAlarmLog;
        if (StringUtils.isNotEmpty(deviceStatus.sensorId)) {
          oldAlarmLog = alarmLogService.getOpenAlarmLogForDeviceAndSensorId(device,
              deviceStatus.sensorId, alarmType, deviceAlarmType);
        } else if (deviceStatus.locationId != null) {
          oldAlarmLog =
              alarmLogService.getOpenAlarmLogForDeviceAndMPId(device, deviceStatus.locationId,
                  alarmType, deviceAlarmType);
        }else{
          oldAlarmLog =
              alarmLogService.getOpenParentAlarmLog(device, alarmType, deviceAlarmType);
        }
        oldAlarmLog.endTime = deviceStatus.statusUpdatedTime;
        oldAlarmLog.update();
      } catch (NoResultException e) {
        //do nothing
      }

      if (deviceStatus.statusKey.equals(AssetStatusConstants.TEMP_STATUS_KEY)) {
        newAlarmLog = new AlarmLog(alarmType, deviceStatus.statusUpdatedTime);
        newAlarmLog.temperatureType =
            fetchAlarmTypeForDeviceStatus(deviceStatus.statusKey, deviceStatus.status);
        newAlarmLog.temperature = deviceStatus.temperature;
      } else {
        newAlarmLog = new AlarmLog(alarmType, deviceStatus.statusUpdatedTime);
        newAlarmLog.deviceAlarmStatus =
            fetchAlarmTypeForDeviceStatus(deviceStatus.statusKey, deviceStatus.status);
      }
      newAlarmLog.deviceAlarmType = deviceAlarmType;
      newAlarmLog.startTime = deviceStatus.statusUpdatedTime;
      newAlarmLog.device = device;
      newAlarmLog.sensorId = deviceStatus.sensorId;
      newAlarmLog.temperatureAbnormalType = deviceStatus.temperatureAbnormalStatus;
      newAlarmLog.monitoringPositionId = deviceStatus.locationId;
      newAlarmLog.save();
    }

  }

  private Integer getDeviceAlarmType(DeviceStatus deviceStatus) {
    Integer deviceAlarmType = null;
    switch (deviceStatus.statusKey) {
      case AssetStatusConstants.BATTERY_ALARM_STATUS_KEY:
        deviceAlarmType = AlarmService.BATTERY_ALARM;
        break;
      case AssetStatusConstants.ACTIVITY_STATUS_KEY:
        deviceAlarmType = AlarmService.DEVICE_NODATA_ALARM;
        break;
      case AssetStatusConstants.XSNS_ALARM_STATUS_KEY:
        deviceAlarmType = AlarmService.SENSOR_CONNECTION_ALARM;
        break;
    }
    return deviceAlarmType;
  }

  private Integer fetchAlarmTypeForDeviceStatus(String statusKey, Integer status) {
    if (StringUtils.isNotEmpty(statusKey) && status != null) {
      if (Objects.equals(statusKey, AssetStatusConstants.TEMP_STATUS_KEY)) {
        if (Objects.equals(status, AssetStatusConstants.TEMP_STATUS_EXCURSION)) {
          return TemperatureEventType.EXCURSION.getValue();
        } else if (Objects.equals(status, AssetStatusConstants.TEMP_STATUS_WARNING)) {
          return TemperatureEventType.WARNING.getValue();
        } else if (Objects.equals(status, AssetStatusConstants.TEMP_STATUS_ALARM)) {
          return TemperatureEventType.ALARM.getValue();
        } else if (Objects.equals(status, AssetStatusConstants.TEMP_STATUS_NORMAL)) {
          return TemperatureEventType.INCURSION.getValue();
        } else {
          return TemperatureEventType.RAW.getValue();
        }
      } else if (Objects.equals(statusKey, AssetStatusConstants.ACTIVITY_STATUS_KEY)) {
        if (Objects.equals(status, AssetStatusConstants.ACTIVITY_STATUS_INACT)) {
          return AssetStatusConstants.ACTIVITY_STATUS_INACT;
        } else {
          return AssetStatusConstants.ACTIVITY_STATUS_OK;
        }
      } else if (Objects.equals(statusKey, AssetStatusConstants.XSNS_ALARM_STATUS_KEY)) {
        if (Objects.equals(status, AssetStatusConstants.XSNS_ALARM_ALARM)) {
          return AssetStatusConstants.XSNS_ALARM_ALARM;
        } else if (Objects.equals(status, AssetStatusConstants.XSNS_ALARM_NORMAL)) {
          return AssetStatusConstants.XSNS_ALARM_NORMAL;
        }
      }
    }
    return null;
  }

  public void updateOverallActivityStatus(Set<Device> devices) {
    for (Device device : devices) {
      updateOverallDeviceStatus(device, AssetStatusConstants.ACTIVITY_STATUS_KEY);
    }
  }

}

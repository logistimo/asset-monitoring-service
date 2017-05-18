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

import com.logistimo.dao.DevicePowerTransitionDAO;
import com.logistimo.db.AlarmLog;
import com.logistimo.db.AssetMapping;
import com.logistimo.db.AssetType;
import com.logistimo.db.Device;
import com.logistimo.db.DeviceMetaData;
import com.logistimo.db.DevicePowerTransition;
import com.logistimo.db.DeviceStatus;
import com.logistimo.db.DeviceTemperatureRequest;
import com.logistimo.db.TemperatureReading;
import com.logistimo.exception.LogistimoException;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.alarm.request.AlarmLoggingRequest;
import com.logistimo.models.alarm.request.AlarmRequest;
import com.logistimo.models.alarm.request.DeviceAlarmRequest;
import com.logistimo.models.alarm.request.ExternalSensorAlarmsRequest;
import com.logistimo.models.alarm.request.GenericAlarmRequest;
import com.logistimo.models.device.common.DeviceEventPushModel;
import com.logistimo.models.device.request.DeviceReadingRequest;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.models.task.TemperatureEventType;
import com.logistimo.models.temperature.common.DeviceTemperatureRequestStatus;
import com.logistimo.models.temperature.request.TemperatureLoggingRequest;
import com.logistimo.models.temperature.request.TemperatureRequest;
import com.logistimo.models.temperature.response.TaggedDeviceReadingResponse;
import com.logistimo.models.temperature.response.TaggedTemperatureResponse;
import com.logistimo.models.temperature.response.TemperatureLoggingResponse;
import com.logistimo.models.temperature.response.TemperatureReadingResponse;
import com.logistimo.models.temperature.response.TemperatureResponse;
import com.logistimo.models.v1.request.Reading;
import com.logistimo.models.v1.request.ReadingRequest;
import com.logistimo.models.v1.request.Temperature;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.DeviceMetaAppendix;
import com.logistimo.utils.HttpUtil;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.NoResultException;

import play.Logger;
import play.Play;
import play.i18n.Messages;

@SuppressWarnings("unchecked")
public class TemperatureService extends ServiceImpl implements Executable {
  private static final Logger.ALogger LOGGER = Logger.of(TemperatureService.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);
  private static final AlarmService alarmService = ServiceFactory.getService(AlarmService.class);
  private static final SMSService smsService = ServiceFactory.getService(SMSService.class);
  private static final TaskService taskService = ServiceFactory.getService(TaskService.class);

  private static final String INVALID_DEVICE_PHONE_NUMBER = "invalid_device_phone_number";
  private static final String SMS_STATUS_REQUEST_URL = "logistimo.sent_status_request.url";
  private static final String
      SUPPORT_STATUS_REQUEST_VENDORS =
      "logistimo.support.status_request.vendors";
  private static final String FEATURE_NOT_SUPPORTED = "feature_not_supported";

  @Override
  public void process(String content, Map<String, Object> options) throws Exception {
    if (content != null) {
      Integer chSource = AssetStatusConstants.GPRS;
      if (options != null && options.get("source") != null) {
        chSource = (Integer) options.get("source");
      }
      TemperatureLoggingRequest
          temperatureLoggingRequest =
          LogistimoUtils.getValidatedObject(content, TemperatureLoggingRequest.class);
      TemperatureLoggingResponse
          temperatureLoggingResponse =
          logReadings(temperatureLoggingRequest, chSource);
      if (temperatureLoggingResponse.errs.size() == temperatureLoggingRequest.data.size()) {
        LOGGER.warn(
            "No device(s) found: {}, and input data: {}" + temperatureLoggingResponse.toString(),
            temperatureLoggingRequest.toString());
      } else if (temperatureLoggingResponse.errs.size() > 0
          || temperatureLoggingResponse.errTmps.size() > 0) {
        LOGGER.warn("Partial content: {}, data: {}", temperatureLoggingResponse.toString(),
            temperatureLoggingRequest.toString());
      } else {
        LOGGER.info("Temperature processing completed successfully.");
      }
    }
  }

  public TemperatureLoggingResponse logReadings(TemperatureLoggingRequest temperatureLoggingRequest,
                                                Integer chSource) throws ServiceException {
    TemperatureLoggingResponse temperatureLoggingResponse = new TemperatureLoggingResponse();
    for (DeviceReadingRequest deviceReadingRequest : temperatureLoggingRequest.data) {
      try {
        Device
            device =
            deviceService.findDevice(temperatureLoggingRequest.vId, deviceReadingRequest.dId);

        DevicePowerTransition devicePowerTransition = null;
        if (deviceReadingRequest.pwa != null) {
          devicePowerTransition =
              DevicePowerTransitionDAO.logPowerTransition(device, deviceReadingRequest.pwa);
        }

        List<String>
            errorReadings =
            logTemperatures(deviceReadingRequest.tmps, device, devicePowerTransition, chSource);
        if (errorReadings.size() > 0) {
          for (String errorReading : errorReadings) {
            temperatureLoggingResponse.errTmps.add(deviceReadingRequest.dId + ": " + errorReading);
          }
        }
      } catch (NoResultException e) {
        LOGGER.warn("Device not found while logging temperature {}", e.getMessage(), e);
        temperatureLoggingResponse.errs.add(deviceReadingRequest.dId);
      }
    }

    return temperatureLoggingResponse;
  }

  public TemperatureReadingResponse getPaginatedReadings(String vendorId, String deviceId
      , String sid, int pageNumber, int pageSize, int from, int to) {
    try {
      pageSize = LogistimoUtils.transformPageSize(pageSize);
      Device device = deviceService.findDevice(vendorId, deviceId, sid);
      if (from > 0 && to > 0) {
        int pageCount = TemperatureReading.getReadingsBetweenCount(device, from, to);
        return toTemperatureReadingResponse(
            getTemperatureReading(device, pageNumber, pageSize, from, to)
            , LogistimoUtils.availableNumberOfPages(pageSize, pageCount));
      } else {
        int pageCount = TemperatureReading.getReadingsCount(device);
        return toTemperatureReadingResponse(
            getTemperatureReading(device, pageNumber, pageSize, -1, -1)
            , LogistimoUtils.availableNumberOfPages(pageSize, pageCount));
      }
    } catch (NoResultException e) {
      throw new NoResultException(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND));
    }
  }

  public TemperatureReadingResponse getPaginatedReadingsV3(String vendorId, String deviceId
      , Integer mpId, int pageNumber, int pageSize, int from, int to) {
    try {
      pageSize = LogistimoUtils.transformPageSize(pageSize);
      Device device = deviceService.findDevice(vendorId, deviceId);

      TemperatureReadingResponse temperatureReadingResponse = new TemperatureReadingResponse();
      if (mpId == null) {
        List<AssetMapping> assetMappingList = AssetMapping.findAssetRelationByAsset(device);
        for (AssetMapping assetMapping : assetMappingList) {
          temperatureReadingResponse.data = toTemperatureResponseList(
              TemperatureReading.getReadingsBetweenByMA(device, assetMapping.monitoringPositionId,
                  LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize,
                  from, to)
          );
        }
      } else {
        temperatureReadingResponse.data = toTemperatureResponseList(
            TemperatureReading.getReadingsBetweenByMA(device, mpId,
                LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize, from,
                to)
        );
      }
      return temperatureReadingResponse;
    } catch (NoResultException e) {
      throw new NoResultException(Messages.get(LogistimoConstant.DEVICES_NOT_FOUND));
    }
  }

  @Deprecated
  public TemperatureLoggingRequest buildTempLoggingRequest(ReadingRequest readingRequest) {
    TemperatureLoggingRequest temperatureLoggingRequest = new TemperatureLoggingRequest();

    Map<String, Integer> typeMap = new TreeMap<String, Integer>() {{
      put("RAW", 0);
      put("INCURSION", 1);
      put("EXCURSION", 2);
    }};
    temperatureLoggingRequest.vId = readingRequest.vendorId;

    for (Reading reading : readingRequest.data) {
      DeviceReadingRequest deviceReadingRequest = new DeviceReadingRequest();

      deviceReadingRequest.dId = reading.deviceId;
      for (Temperature temperature : reading.temperatures) {
        TemperatureRequest temperatureRequest = new TemperatureRequest();

        temperatureRequest.tmp = temperature.temperature;
        temperatureRequest.time = (int) (temperature.timestamp / 1000);
        temperatureRequest.typ = typeMap.get(temperature.type.toUpperCase());

        deviceReadingRequest.tmps.add(temperatureRequest);
      }
      temperatureLoggingRequest.data.add(deviceReadingRequest);
    }

    return temperatureLoggingRequest;
  }

  public TaggedTemperatureResponse getPaginatedReadingsByTag(String tagName, int pageNumber,
                                                             int pageSize) {
    List<Device> deviceList = deviceService.getDeviceTaggedWith(tagName);

    pageSize = LogistimoUtils.transformPageSize(pageSize);

    if (deviceList == null || deviceList.size() == 0) {
      return new TaggedTemperatureResponse();
    }

    return toTaggedTemperatureResponse(
        TemperatureReading.getReadingsForDevices(deviceList,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize),
        LogistimoUtils.availableNumberOfPages(pageSize,
            TemperatureReading.getReadingsCountForDevices(deviceList)));
  }

  public TaggedTemperatureResponse toTaggedTemperatureResponse(
      List<TemperatureReading> temperatureReadingList, long pageCount) {

    if (temperatureReadingList != null) {
      TaggedTemperatureResponse taggedTemperatureResponse = new TaggedTemperatureResponse();
      for (TemperatureReading temperatureReading : temperatureReadingList) {
        TaggedDeviceReadingResponse taggedDeviceReadingResponse = new TaggedDeviceReadingResponse();
        taggedDeviceReadingResponse.deviceId = temperatureReading.device.deviceId;
        taggedDeviceReadingResponse.vendorId = temperatureReading.device.vendorId;

        TemperatureResponse temperatureResponse = new TemperatureResponse();
        temperatureResponse.time = temperatureReading.timeOfReading;
        temperatureResponse.typ = temperatureReading.type;
        temperatureResponse.tmp = temperatureReading.temperature;
        taggedDeviceReadingResponse.temperature = temperatureResponse;

        taggedTemperatureResponse.data.add(taggedDeviceReadingResponse);
      }
      taggedTemperatureResponse.setnPages(pageCount);
      return taggedTemperatureResponse;
    }
    return null;
  }

  private List<TemperatureReading> getTemperatureReading(Device device, int pageNumber,
                                                         int pageSize, int from, int to) {
    return TemperatureReading.getReadingsBetween(device,
        LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize, from, to);
  }

  private List<String> logTemperatures(List<TemperatureRequest> temperatureList,
                                       final Device rootDevice,
                                       DevicePowerTransition devicePowerTransition,
                                       Integer chSource) {
    List<String> errorReadings = new ArrayList<>(1);
    List<String> sensorStatusUpdated = new ArrayList<>(5);
    Device currentDevice = rootDevice;
    Map<String, Device> deviceMap = new HashMap<String, Device>(1) {{
      put(rootDevice.deviceId, rootDevice);
    }};

    Map<Device, TemperatureReading> mostRecentTemperatureReceived = new HashMap<>(1);
    Collections.sort(temperatureList);
    for (TemperatureRequest temperature : temperatureList) {
      try {
        if (StringUtils.isNotEmpty(temperature.sId)) {
          if (deviceMap.containsKey(temperature.sId)) {
            currentDevice = deviceMap.get(temperature.sId);
          } else {
            currentDevice =
                deviceService.findDevice(rootDevice.vendorId,
                    LogistimoUtils.generateVirtualDeviceId(rootDevice.deviceId, temperature.sId));
            deviceMap.put(temperature.sId, currentDevice);
          }
        } else {
          try {
            List<AssetMapping> assetMappingList = AssetMapping.findAssetRelationByAssetAndType(
                rootDevice, LogistimoConstant.CONTAINS);
            if (assetMappingList != null && assetMappingList.size() == 1) {
              currentDevice = assetMappingList.get(0).relatedAsset;
            } else {
              LOGGER.warn(
                  "Too many relation found for the device {}, {}. Skipping temperature data {}",
                  rootDevice.vendorId, rootDevice.deviceId, temperature.toString());
              continue;
            }
          } catch (NoResultException e) {
            LOGGER.warn(
                "Asset mapping not found for the device {}, {}. Skipping temperature data {}",
                rootDevice.vendorId, rootDevice.deviceId, temperature.toString());
            continue;
          }
        }

        TemperatureReading temperatureReading = saveTemperature(temperature, currentDevice,
            rootDevice, devicePowerTransition, chSource);
        if (temperatureReading != null) {
          mostRecentTemperatureReceived.put(currentDevice, temperatureReading);

          //Clearing sensor disconnected alarm if any
          if (!sensorStatusUpdated.contains(currentDevice.deviceId)) {
            try {
              String sensorId = LogistimoUtils.extractSensorId(currentDevice.deviceId);
              DeviceStatus deviceStatus = DeviceStatus.getDeviceStatus(rootDevice,
                  AssetStatusConstants.XSNS_ALARM_STATUS_KEY, sensorId);
              if (Objects.equals(deviceStatus.status, AssetStatusConstants.XSNS_ALARM_ALARM)
                  && temperatureReading.timeOfReading > deviceStatus.statusUpdatedTime) {
                try {
                  ExternalSensorAlarmsRequest
                      xSns =
                      new ExternalSensorAlarmsRequest(AssetStatusConstants.XSNS_ALARM_NORMAL,
                          temperatureReading.timeOfReading);
                  alarmService.generateAndPostAssetAlarm(rootDevice, sensorId,
                      AssetStatusConstants.XSNS_ALARM_TYPE, xSns);
                } catch (ServiceException e) {
                  LOGGER.warn("{} while generating sensor connected alarm for the device {}, {}",
                      e.getMessage(), rootDevice.vendorId, rootDevice.deviceId, e);
                }
                sensorStatusUpdated.add(currentDevice.deviceId);
              }
            } catch (NoResultException e) {
              //do nothing, means there is no sensor disconnected alarm for current device
            }
          }
        }
      } catch (NoResultException e) {
        LOGGER.warn(
            "Error while logging temperature for sensor, sensor({}) not found for the device: {}, {}",
            temperature.sId, currentDevice.vendorId, currentDevice.deviceId);
        errorReadings.add(temperature.toString());
      } catch (LogistimoException e) {
        LOGGER.warn("{}, while logging temperature for device: {}, {}", e.getMessage(),
            currentDevice.vendorId, currentDevice.deviceId, e);
        errorReadings.add(temperature.toString());
      }
    }

    //Updating virtual asset temperature status
    updateDeviceTemperatureStatus(mostRecentTemperatureReceived);

    //Updating virtual asset activity
    updateDeviceActivityStatus(mostRecentTemperatureReceived);

    //Updating related assets temperature status
    updateRelatedAssetsTemperatureStatus(mostRecentTemperatureReceived);

    return errorReadings;
  }

  private TemperatureReading saveTemperature(TemperatureRequest temperature, Device device,
                                             Device rootDevice,
                                             DevicePowerTransition devicePowerTransition,
                                             Integer source) throws LogistimoException {
    if (temperature.time < 0 || temperature.tmp == null) {
      throw new LogistimoException("Invalid temperature reading.");
    }

    TemperatureReading temperatureReading = null;
    try {
      temperatureReading =
          TemperatureReading.getReading(device, temperature.time, temperature.typ, temperature.tmp);
    } catch (NoResultException ignored) {
      //do nothing
    }

    if (temperatureReading == null) {
      updateFirstTemperatureTime(temperature, device);
      updateFirstTemperatureTime(temperature, rootDevice);

      temperatureReading = new TemperatureReading();
      temperatureReading.device = device;
      temperatureReading.temperature = temperature.tmp;
      temperatureReading.timeOfReading = temperature.time;
      temperatureReading.type = temperature.typ;
      temperatureReading.source = source;

      AssetMapping assetMapping = null;
      try {
        assetMapping =
            AssetMapping.findAssetMappingByRelatedAssetAndType(
                deviceService.findDevice(device.vendorId, device.deviceId),
                LogistimoConstant.MONITORED_BY);
      } catch (NoResultException ignored) {
        //do nothing
      }
      if (assetMapping != null) {
        updateFirstTemperatureTime(temperature, assetMapping.asset);
        temperatureReading.monitoringPositionId = assetMapping.monitoringPositionId;
        temperatureReading.monitoredAsset = assetMapping.asset;
      }

      //Power transition is available under root device
      DevicePowerTransition finalDevicePowerTransition = null;
      try {
        finalDevicePowerTransition =
            DevicePowerTransition.getRecentDevicePowerTransition(rootDevice, temperature.time);

        if (devicePowerTransition != null
            && devicePowerTransition.time > finalDevicePowerTransition.time) {
          finalDevicePowerTransition = devicePowerTransition;
        }
      } catch (NoResultException e) {
        if (devicePowerTransition != null) {
          finalDevicePowerTransition = devicePowerTransition;
        }
      }
      if (finalDevicePowerTransition != null) {
        temperatureReading.powerAvailability = finalDevicePowerTransition.state;
      }

      temperatureReading.save();
      return temperatureReading;
    }

    return null;
  }

  private void updateFirstTemperatureTime(TemperatureRequest temperature, Device device) {
    if (null == device.firstTempTime) {
      device.firstTempTime = temperature.time;
      device.update();
    }
  }

  private void updateDeviceTemperatureStatus(
      Map<Device, TemperatureReading> mrTemperatureReadings) {
    for (Device device : mrTemperatureReadings.keySet()) {
      TemperatureReading temperatureReading = mrTemperatureReadings.get(device);
      DeviceStatus deviceStatus = deviceService.getOrCreateDeviceStatus(device, null, null,
          AssetStatusConstants.TEMP_STATUS_KEY, null);

      if (temperatureReading.timeOfReading > deviceStatus.temperatureUpdatedTime) {
        Map<String, DeviceMetaData>
            metaDataMap =
            deviceService.getDeviceMetaDataMap(device, DeviceMetaAppendix.TMP_GROUP);
        double minTemp = Double.parseDouble(metaDataMap.get(DeviceMetaAppendix.TMP_MIN).value);
        double maxTemp = Double.parseDouble(metaDataMap.get(DeviceMetaAppendix.TMP_MAX).value);
        Integer temperatureAbnormalStatus = AssetStatusConstants.TEMP_ABNORMAL_STATUS_NORMAL;
        if (temperatureReading.temperature > maxTemp) {
          temperatureAbnormalStatus = AssetStatusConstants.TEMP_ABNORMAL_STATUS_HIGH;
        } else if (temperatureReading.temperature < minTemp) {
          temperatureAbnormalStatus = AssetStatusConstants.TEMP_ABNORMAL_STATUS_LOW;
        }

        if ((deviceStatus.status.equals(AssetStatusConstants.TEMP_STATUS_NORMAL)
            && temperatureAbnormalStatus > AssetStatusConstants.TEMP_ABNORMAL_STATUS_NORMAL)
            || (!Objects.equals(temperatureAbnormalStatus,
            AssetStatusConstants.TEMP_ABNORMAL_STATUS_NORMAL)
            && deviceStatus.status > AssetStatusConstants.TEMP_STATUS_NORMAL && !Objects.equals(
            temperatureAbnormalStatus, deviceStatus.temperatureAbnormalStatus))) {

          deviceStatus.status = AssetStatusConstants.TEMP_STATUS_EXCURSION;
          deviceStatus.statusUpdatedTime = temperatureReading.timeOfReading;
          deviceStatus.temperatureAbnormalStatus = temperatureAbnormalStatus;

          Map<String, Object> options = new HashMap<>(1);
          options.put(TemperatureEventService.DEVICE_ID, device.deviceId);
          options.put(TemperatureEventService.VENDOR_ID, device.vendorId);
          options.put(TemperatureEventService.EVENT_TYPE, TemperatureEventType.EXCURSION);
          options.put(TemperatureEventService.STATE_UPDATED_TIME,
              deviceStatus.statusUpdatedTime);
          try {
            taskService.produceMessage(
                new TaskOptions(
                    TaskType.BACKGROUND_TASK.getValue(),
                    TemperatureEventService.class,
                    null,
                    options,
                    3000L
                )
            );
          } catch (ServiceException e) {
            LOGGER.error(
                "{} while generating task for temperature event processing for the device {}, {}",
                e.getMessage(), device.vendorId, device.deviceId, e);
          }
        } else if (deviceStatus.status > AssetStatusConstants.TEMP_STATUS_NORMAL && Objects
            .equals(temperatureAbnormalStatus,
                AssetStatusConstants.TEMP_ABNORMAL_STATUS_NORMAL)) {

          deviceStatus.status = AssetStatusConstants.TEMP_STATUS_NORMAL;
          deviceStatus.statusUpdatedTime = temperatureReading.timeOfReading;
          deviceStatus.temperatureAbnormalStatus = AssetStatusConstants.TEMP_ABNORMAL_STATUS_NORMAL;

        }

        deviceStatus.temperature = temperatureReading.temperature;
        deviceStatus.temperatureUpdatedTime = temperatureReading.timeOfReading;
        deviceStatus.update();
      }
    }
  }

  /**
   * Update virtual device status
   */
  private void updateDeviceActivityStatus(Map<Device, TemperatureReading> mrTemperatureReadings) {
    long currentTime = (int) (System.currentTimeMillis() / 1000);

    for (Device device : mrTemperatureReadings.keySet()) {
      TemperatureReading temperatureReading = mrTemperatureReadings.get(device);
      DeviceStatus deviceStatus = deviceService.getOrCreateDeviceStatus(device, null, null,
          AssetStatusConstants.ACTIVITY_STATUS_KEY, null);

      if (temperatureReading.timeOfReading > deviceStatus.statusUpdatedTime &&
          deviceStatus.status.equals(AssetStatusConstants.ACTIVITY_STATUS_INACT)) {
        Map<String, String>
            deviceMetaDataMap =
            deviceService.getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.INT_GROUP);

        int pushInt = deviceMetaDataMap.get(DeviceMetaAppendix.INT_PINT) != null ?
            Integer.parseInt(deviceMetaDataMap.get(DeviceMetaAppendix.INT_PINT)) * 60 :
            3600;

        int
            iActCounts =
            deviceMetaDataMap.get(DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT) != null ?
                Integer.parseInt(
                    deviceMetaDataMap.get(DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT)) :
                DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT_DEFAULT_VALUE;

        if (temperatureReading.timeOfReading >= (currentTime - pushInt * iActCounts)) {
          deviceStatus.status = AssetStatusConstants.ACTIVITY_STATUS_OK;
          deviceStatus.statusUpdatedTime = temperatureReading.timeOfReading;
          deviceStatus.update();

          try {
            generateAndPostActivityAlarm(device, deviceStatus);
          } catch (ServiceException e) {
            LOGGER.error("{} while generating activity alarm for the device {}, {}", e.getMessage(),
                device.vendorId, device.deviceId, e);
          }
        }
      } else {
        //if status is in correct, then ensure related assets are also correct.
        DeviceEventPushModel deviceEventPushModel = new DeviceEventPushModel();
        Optional<Device>
            updatedDevice =
            alarmService.updateMonitoredAssetStatus(deviceStatus, device,
                AssetStatusConstants.ACTIVITY_ALARM_TYPE, deviceEventPushModel);
        if(updatedDevice.isPresent()){
          alarmService.updateAndPushEvent(deviceEventPushModel,
              Collections.singleton(updatedDevice.get()));
        }
      }
    }
  }

  private void generateAndPostActivityAlarm(Device device, DeviceStatus deviceStatus)
      throws ServiceException {
    AlarmLoggingRequest alarmLoggingRequest = new AlarmLoggingRequest(device.vendorId);
    GenericAlarmRequest
        genericAlarmRequest =
        new GenericAlarmRequest(deviceStatus.status, deviceStatus.statusUpdatedTime);
    DeviceAlarmRequest deviceAlarmRequest = new DeviceAlarmRequest(genericAlarmRequest);
    AlarmRequest alarmRequest = new AlarmRequest(device.deviceId, deviceAlarmRequest);

    if (device.assetType.id.equals(AssetType.TEMP_SENSOR)) {
      alarmRequest = new AlarmRequest(
          LogistimoUtils.extractDeviceId(device.deviceId),
          LogistimoUtils.extractSensorId(device.deviceId),
          deviceAlarmRequest
      );
    }
    alarmLoggingRequest.data.add(alarmRequest);
    alarmService.postDeviceAlarm(alarmLoggingRequest);
  }

  private void updateRelatedAssetsTemperatureStatus(
      Map<Device, TemperatureReading> mrTemperatureReadings) {
    Set<Device> devices = new HashSet<>(1);
    for (Device relatedAsset : mrTemperatureReadings.keySet()) {
      DeviceStatus monitoringAssetStatus =
          deviceService.getOrCreateDeviceStatus(relatedAsset, null, null,
              AssetStatusConstants.TEMP_STATUS_KEY, null);

      AssetMapping assetMapping;
      DeviceStatus monitoredAssetStatus;
      try {
        //Propagating the status to parent asset
        Optional<Device> optionalAsset = updateParentDeviceWithSensorStatus(relatedAsset);
        if (optionalAsset.isPresent()) {
          devices.add(optionalAsset.get());
        }

        //Propagating the status to monitored asset
        assetMapping =
            AssetMapping.findAssetMappingByRelatedAssetAndType(relatedAsset,
                LogistimoConstant.MONITORED_BY);
        monitoredAssetStatus =
            deviceService.getOrCreateDeviceStatus(assetMapping.asset,
                assetMapping.monitoringPositionId, null,
                AssetStatusConstants.TEMP_STATUS_KEY, null);

        if (!Objects.equals(monitoredAssetStatus.status, monitoringAssetStatus.status)
            ||
            !Objects.equals(monitoredAssetStatus.temperatureAbnormalStatus,
                monitoringAssetStatus.temperatureAbnormalStatus)) {
          updateMonitoredAssetStatus(relatedAsset, assetMapping,
              monitoringAssetStatus,
              monitoredAssetStatus);
        } else {
          monitoredAssetStatus.temperature = monitoringAssetStatus.temperature;
          monitoredAssetStatus.temperatureUpdatedTime =
              monitoringAssetStatus.temperatureUpdatedTime;
          monitoredAssetStatus.update();
        }

        devices.add(assetMapping.asset);
      } catch (NoResultException e) {
        //do nothing
      }
    }
    updateOverallTemperatureStatus(devices);
  }

  /**
   * Update the parent device temperature status with sensor id record.
   *
   * @param virtualDevice - temperatureLogger virtual device.
   * @return optional device object.
   */
  public Optional<Device> updateParentDeviceWithSensorStatus(Device virtualDevice) {
    AssetMapping assetMapping;
    try {
      assetMapping = updateParentDeviceWithSensorTemperatureStatus(virtualDevice);
      return Optional.of(assetMapping.asset);
    } catch (NoResultException e) {
      //do nothing
    }
    return Optional.empty();
  }

  /**
   *
   * @param virtualDevice
   * @return
   */
  public AssetMapping updateParentDeviceWithSensorTemperatureStatus(Device virtualDevice) {
    AssetMapping assetMapping;
    DeviceStatus virtualDeviceAssetStatus;
    assetMapping =
        AssetMapping.findAssetMappingByRelatedAssetAndType(virtualDevice,
            LogistimoConstant.CONTAINS);
    virtualDeviceAssetStatus =
        deviceService.getOrCreateDeviceStatus(assetMapping.relatedAsset, null, null,
            AssetStatusConstants.TEMP_STATUS_KEY, null);

    DeviceStatus
        mainDeviceAssetStatus =
        deviceService.getOrCreateDeviceStatus(assetMapping.asset, null,
            LogistimoUtils.extractSensorId(virtualDevice.deviceId),
            AssetStatusConstants.TEMP_STATUS_KEY, null);

    replicateDeviceStatus(assetMapping.asset, virtualDeviceAssetStatus, mainDeviceAssetStatus,
        true);
    return assetMapping;
  }

  private void replicateDeviceStatus(Device asset,
                                     DeviceStatus srcDeviceStatus,
                                     DeviceStatus destDeviceStatus, boolean replicateTemperature) {

    boolean isUpdated = false;
    if (replicateTemperature) {
      destDeviceStatus.temperature = srcDeviceStatus.temperature;
      destDeviceStatus.temperatureUpdatedTime =
          srcDeviceStatus.temperatureUpdatedTime;
    }

    if (!Objects
        .equals(destDeviceStatus.status, srcDeviceStatus.status) ||
        !Objects.equals(destDeviceStatus.temperatureAbnormalStatus,
            srcDeviceStatus.temperatureAbnormalStatus)) {
      Integer prevStatusTime = destDeviceStatus.statusUpdatedTime;
      Integer prevStatus = destDeviceStatus.status;

      destDeviceStatus.status = srcDeviceStatus.status;
      destDeviceStatus.statusUpdatedTime =
          srcDeviceStatus.statusUpdatedTime;
      if (replicateTemperature) {
        destDeviceStatus.temperatureAbnormalStatus =
            srcDeviceStatus.temperatureAbnormalStatus;
      }

      deviceService.createAlarmLog(destDeviceStatus, prevStatusTime, prevStatus, asset);
      isUpdated = true;
    }

    if (isUpdated || replicateTemperature) {
      destDeviceStatus.update();
    }
  }

  private void updateMonitoredAssetStatus(Device relatedAsset, AssetMapping assetMapping,
                                          DeviceStatus monitoringAssetStatus,
                                          DeviceStatus monitoredAssetStatus) {
    //Close old alarm log, if exists
    try {
      AlarmLog
          oldAlarmLog = AlarmLog.getAlarmLogForDeviceAndSensorId(assetMapping.asset,
          monitoredAssetStatus.statusUpdatedTime, monitoredAssetStatus.sensorId,
          AlarmLog.TEMP_ALARM, null);
      oldAlarmLog.endTime = monitoringAssetStatus.statusUpdatedTime;
      oldAlarmLog.update();
    } catch (NoResultException e) {
      //do nothing
    }

    monitoredAssetStatus.status = monitoringAssetStatus.status;
    monitoredAssetStatus.statusUpdatedTime = monitoringAssetStatus.statusUpdatedTime;
    monitoredAssetStatus.temperatureAbnormalStatus =
        monitoringAssetStatus.temperatureAbnormalStatus;
    monitoredAssetStatus.temperature = monitoringAssetStatus.temperature;
    monitoredAssetStatus.temperatureUpdatedTime = monitoringAssetStatus.temperatureUpdatedTime;
    monitoredAssetStatus.update();

    //Creating Alarm log for monitored asset
    AlarmLog alarmLog = new AlarmLog(AlarmLog.TEMP_ALARM, monitoredAssetStatus.statusUpdatedTime);
    alarmLog.temperature = monitoredAssetStatus.temperature;
    alarmLog.temperatureType =
        monitoredAssetStatus.status == 0 ? TemperatureEventType.INCURSION.getValue()
            : TemperatureEventType.EXCURSION.getValue();
    alarmLog.device = assetMapping.asset;
    alarmLog.monitoringPositionId = assetMapping.monitoringPositionId;
    alarmLog.temperatureAbnormalType = monitoredAssetStatus.temperatureAbnormalStatus;
    alarmLog.startTime = monitoredAssetStatus.statusUpdatedTime;
    alarmLog.updatedOn = new Date();
    alarmLog.save();

    //Generating temperature event and posting to Logistics service
    DeviceEventPushModel.DeviceEvent deviceEvent = new DeviceEventPushModel.DeviceEvent();
    deviceEvent.vId = assetMapping.asset.vendorId;
    deviceEvent.dId = assetMapping.asset.deviceId;
    deviceEvent.mpId = assetMapping.monitoringPositionId;
    deviceEvent.st = monitoredAssetStatus.status;
    deviceEvent.time = monitoredAssetStatus.statusUpdatedTime;
    deviceEvent.tmp = monitoredAssetStatus.temperature;
    deviceEvent.aSt = monitoredAssetStatus.temperatureAbnormalStatus;
    deviceEvent.type = DeviceEventPushModel.DEVICE_EVENT_TEMP;

    Map<String, String>
        assetMetaMap =
        deviceService.getDeviceMetaDataValueAsString(relatedAsset, DeviceMetaAppendix.TMP_GROUP);
    if (assetMetaMap != null) {
      if (assetMetaMap.containsKey(DeviceMetaAppendix.TMP_MIN)) {
        deviceEvent.attrs
            .put(DeviceEventPushModel.TMP_MIN, assetMetaMap.get(DeviceMetaAppendix.TMP_MIN));
      }

      if (assetMetaMap.containsKey(DeviceMetaAppendix.TMP_MAX)) {
        deviceEvent.attrs
            .put(DeviceEventPushModel.TMP_MAX, assetMetaMap.get(DeviceMetaAppendix.TMP_MAX));
      }
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
          "{} while scheduling task for posting temperature event to Logistics service, {}",
          e.getMessage(), deviceEvent.toString(), e);
    }
  }

  public void updateOverallTemperatureStatus(Set<Device> devices) {
    for (Device device : devices) {
      deviceService.updateOverallDeviceStatus(device, AssetStatusConstants.TEMP_STATUS_KEY);
    }
  }

  private TemperatureReadingResponse toTemperatureReadingResponse(
      List<TemperatureReading> temperatureReadingList, long pageCount) {
    if (temperatureReadingList != null) {
      TemperatureReadingResponse temperatureReadingResponse = new TemperatureReadingResponse();
      for (TemperatureReading temperatureReading : temperatureReadingList) {
        temperatureReadingResponse.data.add(toTemperatureResponse(temperatureReading));
      }
      temperatureReadingResponse.setnPages(pageCount);
      return temperatureReadingResponse;
    }
    return null;
  }

  public List<TemperatureResponse> toTemperatureResponseList(List<TemperatureReading> readingList) {
    List<TemperatureResponse> temperatureResponseList = new ArrayList<>(readingList.size());
    for (TemperatureReading temperatureReading : readingList) {
      temperatureResponseList.add(toTemperatureResponse(temperatureReading));
    }
    return temperatureResponseList;
  }

  public TemperatureResponse toTemperatureResponse(TemperatureReading temperatureReading) {
    TemperatureResponse temperatureResponse = new TemperatureResponse();
    temperatureResponse.time = temperatureReading.timeOfReading;
    temperatureResponse.typ = temperatureReading.type;
    temperatureResponse.tmp = temperatureReading.temperature;
    temperatureResponse.pwa = temperatureReading.powerAvailability;
    temperatureResponse.src = temperatureReading.source;
    return temperatureResponse;
  }

  public void getCurrentTemperature(String vendorId, String deviceId) throws LogistimoException {
    Device device = deviceService.findDevice(vendorId, deviceId);

    if (!Arrays.asList(Play.application().configuration()
        .getString(SUPPORT_STATUS_REQUEST_VENDORS).split(LogistimoConstant.SUPPORT_VENDOR_SEP))
        .contains(vendorId)) {
      LOGGER.warn("Error while requesting device status. Feature not supported in device: {}, {}",
          device.vendorId, device.deviceId);
      throw new LogistimoException(Messages.get(FEATURE_NOT_SUPPORTED));
    }

    String
        phoneNumber =
        deviceService.getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.GSM_SIM_PHN_NUMBER);
    if (StringUtils.isNotEmpty(phoneNumber)) {
      DeviceTemperatureRequest deviceTemperatureRequest = null;
      try {
        deviceTemperatureRequest = DeviceTemperatureRequest.getDeviceStatusByDevice(device);
        deviceTemperatureRequest.numberOfRequest += 1;
        deviceTemperatureRequest.update();
      } catch (NoResultException e) {
        LOGGER.info("Get current temperature - creating new request", e);
      }
      if (deviceTemperatureRequest == null) {
        deviceTemperatureRequest = new DeviceTemperatureRequest();
        deviceTemperatureRequest.device = device;
        deviceTemperatureRequest.numberOfRequest = 1;
        deviceTemperatureRequest.status = DeviceTemperatureRequestStatus.SMS_INIT;
        deviceTemperatureRequest.save();
      }

      try {
        String
            request_url =
            Play.application().configuration().getString(SMS_STATUS_REQUEST_URL + "." + vendorId);
        Map<String, String> requestParams = new HashMap<String, String>();
        String
            countryCode =
            deviceService
                .getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.LOCALE_COUNTRYCODE);
        if (countryCode == null) {
          countryCode = LogistimoConstant.DEFAULT_COUNTRY_CODE;
        }
        requestParams.put(LogistimoConstant.COUNTRY_CODE_PARAM, countryCode);
        requestParams.put(LogistimoConstant.PHONE_PARAM, URLEncoder.encode(phoneNumber, "UTF-8"));
        requestParams.put(LogistimoConstant.VENDOR_ID_PARAM, device.vendorId);
        requestParams.put(LogistimoConstant.DEVICE_ID_PARAM, device.deviceId);
        request_url = HttpUtil.replace(request_url, requestParams);
        smsService.sendSMS(request_url);
      } catch (IOException e) {
        LOGGER.error("Error while sent status request for device: {}, {}", vendorId, deviceId, e);
        throw new LogistimoException(e.getMessage());
      }
    } else {
      LOGGER.warn("Error while pushing Admin settings. Invalid mobile number for device: {}, {}",
          vendorId, deviceId);
      throw new LogistimoException(Messages.get(INVALID_DEVICE_PHONE_NUMBER));
    }
  }
}

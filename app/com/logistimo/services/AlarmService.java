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

import com.logistimo.db.AlarmLog;
import com.logistimo.db.AssetMapping;
import com.logistimo.db.AssetType;
import com.logistimo.db.Device;
import com.logistimo.db.DeviceAlarm;
import com.logistimo.db.DeviceStatus;
import com.logistimo.exception.LogistimoException;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.alarm.request.AlarmLoggingRequest;
import com.logistimo.models.alarm.request.AlarmRequest;
import com.logistimo.models.alarm.request.BatteryAlarmsRequest;
import com.logistimo.models.alarm.request.DeviceAlarmRequest;
import com.logistimo.models.alarm.request.DeviceConnectionAlarmsRequest;
import com.logistimo.models.alarm.request.DeviceErrorRequest;
import com.logistimo.models.alarm.request.ExternalSensorAlarmsRequest;
import com.logistimo.models.alarm.request.GenericAlarmRequest;
import com.logistimo.models.alarm.response.AlarmLoggingResponse;
import com.logistimo.models.alarm.response.AlarmResponse;
import com.logistimo.models.alarm.response.DeviceAlarmResponse;
import com.logistimo.models.common.ErrorResponse;
import com.logistimo.models.device.common.DeviceEventPushModel;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import play.Logger;
import play.i18n.Messages;

@SuppressWarnings("unchecked")
public class AlarmService extends ServiceImpl {
  public static final Integer DEVICE_CONNECTION_ALARM = 0;
  public static final Integer SENSOR_CONNECTION_ALARM = 1;
  public static final Integer BATTERY_ALARM = 2;
  public static final Integer FIRMWARE_ALARM = 3;
  public static final Integer DEVICE_NODATA_ALARM = 4;
  public static final Integer POWER_OUTAGE_ALARM = 5;
  private static final Logger.ALogger LOGGER = Logger.of(AlarmService.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);
  private static final AlarmLogService alarmLogService = ServiceFactory.getService(AlarmLogService.class);
  TaskService taskService = ServiceFactory.getService(TaskService.class);

  /**
   * Logs the alarm, updates the alarm if device found
   */
  public AlarmLoggingResponse postDeviceAlarm(AlarmLoggingRequest alarmLoggingRequest)
      throws ServiceException {
    AlarmLoggingResponse alarmLoggingResponse = new AlarmLoggingResponse();
    DeviceEventPushModel deviceEventPushModel = new DeviceEventPushModel();
    Set<Device> deviceSet = new HashSet<>(1);
    for (AlarmRequest alarmRequest : alarmLoggingRequest.data) {
      try {
        LogistimoUtils.validateObject(alarmRequest);
        Device device = deviceService.findDevice(alarmLoggingRequest.vId, alarmRequest.dId);
        List<DeviceAlarm> deviceAlarmList = toDeviceAlarms(alarmRequest, device);

        //Updating asset specific device alarms status
        for (DeviceAlarm deviceAlarm : deviceAlarmList) {
          deviceAlarm.save();

          //Currently skipping device firmware error
          if (deviceAlarm.type != 3) {
            DeviceStatus
                deviceStatus =
                updateDeviceStatus(device, deviceAlarm, deviceEventPushModel);

            Optional<Device>
                updatedOptional =
                updateMonitoredAssetStatus(deviceStatus, deviceAlarm.device,
                    deviceAlarm.type, deviceEventPushModel);
            if (updatedOptional.isPresent()) {
              deviceSet.add(updatedOptional.get());
            }

          }
          if (AssetType.TEMPERATURE_LOGGER.equals(device.assetType.id)) {
            deviceSet.add(device);
          }
        }

        for (AlarmLog alarmLog : toAlarmLog(alarmRequest, device)) {
          alarmLog.save();
        }
      } catch (LogistimoException e) {
        LOGGER.warn("Validation failed while posting alarm for device", e);
        alarmLoggingResponse.errs
            .add(new ErrorResponse(alarmRequest.dId, alarmRequest.sId, e.getMessage()));
      } catch (NoResultException e) {
        LOGGER.warn("Error while posting alarm for device", e);
        alarmLoggingResponse.errs.add(new ErrorResponse(alarmRequest.dId, alarmRequest.sId,
            Messages.get(LogistimoConstant.DEVICES_NOT_FOUND)));
      }
    }

    updateAndPushEvent(deviceEventPushModel, deviceSet);

    return alarmLoggingResponse;
  }

  /**
   * Propagates the device alarm to the device status
   */
  private DeviceStatus updateDeviceStatus(Device device, DeviceAlarm deviceAlarm,
                                          DeviceEventPushModel deviceEventPushModel) {
    DeviceStatus deviceStatus;

    if (deviceAlarm.sensorId != null) {
      deviceStatus =
          deviceService.getOrCreateDeviceStatus(device, null, deviceAlarm.sensorId,
              AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_MAP.get(deviceAlarm.type),
              null);
    } else {
      deviceStatus =
          deviceService.getOrCreateDeviceStatus(deviceAlarm.device, null, null,
              AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_MAP.get(deviceAlarm.type),
              null);
    }
    if (deviceStatus.status != deviceAlarm.status && (
        deviceStatus.statusUpdatedTime < deviceAlarm.time
            || deviceAlarm.type == AssetStatusConstants.ACTIVITY_ALARM_TYPE)) {
      deviceStatus.status = deviceAlarm.status;
      deviceStatus.statusUpdatedTime = deviceAlarm.time;
      deviceStatus.update();

      //Preparing notification to LS
      DeviceEventPushModel.DeviceEvent deviceEvent = new DeviceEventPushModel.DeviceEvent();
      deviceEvent.vId = deviceAlarm.device.vendorId;
      deviceEvent.dId = deviceAlarm.device.deviceId;
      deviceEvent.st = deviceStatus.status;
      deviceEvent.time = deviceStatus.statusUpdatedTime;
      deviceEvent.sId = deviceAlarm.sensorId;
      deviceEvent.type =
          DeviceEventPushModel.DEVICE_EVENT_ALARM_GROUP.get(deviceAlarm.type);
      deviceEventPushModel.data.add(deviceEvent);
    } else if (deviceStatus.statusUpdatedTime < deviceAlarm.time) {
      deviceStatus.statusUpdatedTime = deviceAlarm.time;
      deviceStatus.update();
    }
    return deviceStatus;
  }

  /**
   * Updates overall status of the asset and published the events.
   *
   * @param deviceEventPushModel - Device events to be pushed
   * @param deviceSet            - devices for which overall status should be updated
   */
  public void updateAndPushEvent(DeviceEventPushModel deviceEventPushModel, Set<Device> deviceSet) {
    try {
      deviceService.updateOverallActivityStatus(deviceSet);
    } catch (Exception e) {
      LOGGER.warn("Error while updating activity status for over all device", e);
    }

    if (!deviceEventPushModel.data.isEmpty()) {
      try {
        taskService.produceMessage(
            new TaskOptions(
                TaskType.BACKGROUND_TASK.getValue(),
                PushAlertService.class,
                LogistimoUtils.toJson(deviceEventPushModel),
                null
            )
        );
      } catch (ServiceException e) {
        LOGGER.error(
            "{} while scheduling task for posting temperature event to Logistics service, {}",
            e.getMessage(), deviceEventPushModel.toString(), e);
      }
    }
  }

  /**
   * Updates the monitored assets status for the sensor
   *
   * @param deviceEventPushModel - device events to be generated
   * @param deviceStatus         - device status of the virtual device
   * @param device               - device
   * @param type                 - Activity status type, see AssetStatusConstants
   */
  public Optional<Device> updateMonitoredAssetStatus(DeviceStatus deviceStatus, Device device,
                                                     int type,
                                                     DeviceEventPushModel deviceEventPushModel) {
    //Updating specific device alarm for related assets
    AssetMapping assetMapping = null;
    try {
      if (AssetStatusConstants.DEVICE_STATUS_PROPAGATION_TYPES.contains(type)) {
        assetMapping =
            AssetMapping.findMonitoredAssetMapping(device);
      } else {
        assetMapping =
            AssetMapping.findAssetMappingByRelatedAssetAndType(device,
                LogistimoConstant.MONITORED_BY);
      }
    } catch (NoResultException e) {
      //do nothing
    }

    if (assetMapping != null) {
      DeviceStatus relatedDeviceStatus;
      if (!AssetStatusConstants.DEVICE_STATUS_PROPAGATION_TYPES.contains(type)
          && assetMapping.monitoringPositionId != null) {
        relatedDeviceStatus =
            deviceService.getOrCreateDeviceStatus(assetMapping.asset,
                assetMapping.monitoringPositionId, null,
                AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_MAP.get(type),
                null);
      } else {
        relatedDeviceStatus =
            deviceService.getOrCreateDeviceStatus(assetMapping.asset, null, null,
                AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_MAP.get(type),
                null);
      }
      return propagateDeviceStatus(relatedDeviceStatus, deviceStatus, deviceEventPushModel,
          assetMapping, type);
    }
    return Optional.empty();
  }

  /**
   * Updates the parent assets status for the sensor
   */
  public Optional<Device> updateParentAssetStatus(DeviceStatus deviceStatus, Device device,
                                                  int type,
                                                  DeviceEventPushModel deviceEventPushModel) {
    //Updating specific device alarm for related assets
    AssetMapping assetMapping = null;
    try {
      assetMapping =
          AssetMapping.findAssetMappingByRelatedAssetAndType(device,
              LogistimoConstant.CONTAINS);
    } catch (NoResultException e) {
      //do nothing
    }
    if (assetMapping != null) {
      DeviceStatus relatedDeviceStatus =
          deviceService.getOrCreateDeviceStatus(assetMapping.asset,
              null, LogistimoUtils.extractSensorId(device.deviceId),
              AssetStatusConstants.DEVICE_ALARM_STATUS_KEYS_MAP.get(type),
              null);

      return propagateDeviceStatus(relatedDeviceStatus, deviceStatus, deviceEventPushModel,
          assetMapping, type);
    }
    return Optional.empty();
  }

  private Optional<Device> propagateDeviceStatus(DeviceStatus relatedDeviceStatus,
                                                 DeviceStatus deviceStatus,
                                                 DeviceEventPushModel deviceEventPushModel,
                                                 AssetMapping assetMapping, int type) {
    if (!Objects.equals(relatedDeviceStatus.status, deviceStatus.status)
        || !Objects.equals(deviceStatus.statusUpdatedTime,
        relatedDeviceStatus.statusUpdatedTime)) {
      relatedDeviceStatus.status = deviceStatus.status;
      relatedDeviceStatus.statusUpdatedTime = deviceStatus.statusUpdatedTime;
      relatedDeviceStatus.update();

      if (type == AssetStatusConstants.ACTIVITY_ALARM_TYPE) {
        DeviceEventPushModel.DeviceEvent
            deviceEvent =
            new DeviceEventPushModel.DeviceEvent();
        deviceEvent.vId = assetMapping.asset.vendorId;
        deviceEvent.dId = assetMapping.asset.deviceId;
        deviceEvent.mpId = assetMapping.monitoringPositionId;
        deviceEvent.sId =
            Objects.equals(assetMapping.asset.assetType.assetType, AssetType.TEMPERATURE_LOGGER)
                ? LogistimoUtils.extractSensorId(assetMapping.relatedAsset.deviceId) : null;
        deviceEvent.st = relatedDeviceStatus.status;
        deviceEvent.time = relatedDeviceStatus.statusUpdatedTime;
        deviceEvent.type =
            DeviceEventPushModel.DEVICE_EVENT_ALARM_GROUP.get(type);
        deviceEventPushModel.data.add(deviceEvent);
      }
      return Optional.of(assetMapping.asset);
    }else {
      try {
        deviceService.updateOverallActivityStatus(Collections.singleton(assetMapping.asset));
      } catch (Exception e) {
        LOGGER.warn("Error while updating activity status for over all device", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the alarm for given vendor and device
   *
   * @return DeviceAlarmResponse
   * @throws javax.persistence.EntityNotFoundException if device, alarm not found
   */
  public DeviceAlarmResponse getAlarm(String vendorId, String deviceId, String sid, int pageNumber,
                                      int pageSize) {
    DeviceAlarmResponse deviceAlarmResponse = new DeviceAlarmResponse();
    Device device = deviceService.findDevice(vendorId, deviceId, sid);
    pageSize = LogistimoUtils.transformPageSize(pageSize);
    int pageCount = DeviceAlarm.getDeviceAlarmCount(device);
    List<DeviceAlarm>
        deviceAlarmList =
        DeviceAlarm.getDeviceAlarms(device,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize);
    for (DeviceAlarm deviceAlarm : deviceAlarmList) {
      deviceAlarmResponse.data.add(toAlarmResponse(deviceAlarm));
    }
    deviceAlarmResponse.setnPages(LogistimoUtils.availableNumberOfPages(pageSize, pageCount));

    return deviceAlarmResponse;
  }

  public List<AlarmResponse> getRecentAlarmForDevice(Device device, int limit) {
    List<AlarmResponse> alarmResponseList = new ArrayList<>(1);
    for (DeviceAlarm deviceAlarm : DeviceAlarm.getRecentAlarmForDevice(device, limit)) {
      alarmResponseList.add(toAlarmResponse(deviceAlarm));
    }

    return alarmResponseList;
  }

  public List<DeviceAlarm> getRecentAlarmForDevices(List<Device> deviceList) {
    if (deviceList != null && deviceList.size() > 0) {
      return DeviceAlarm.getRecentAlarmForDevices(deviceList);
    } else {
      return null;
    }
  }

  public int getAbnormalAlarmCountForDevices(List<Device> deviceList) {
    if (deviceList != null && deviceList.size() > 0) {
      return DeviceAlarm.getAbnormalAlarmCountForDevices(deviceList);
    } else {
      return 0;
    }
  }

  /**
   * Maps the DeviceAlarmStatsRequest request object to DeviceAlarm DBO
   *
   * @return DeviceAlarm
   */
  private List<DeviceAlarm> toDeviceAlarms(AlarmRequest alarmRequest, Device device)
      throws LogistimoException {
    List<DeviceAlarm> deviceAlarms = new ArrayList<>();

    //For multi sensor devices
    List<AssetMapping> assetMappingList = null;
    if (StringUtils.isNotEmpty(alarmRequest.sId)) {
      try {
        assetMappingList = AssetMapping.findAssetRelationByAsset(device);
      } catch (NoResultException e) {
        //do nothing
      }
    }

    DeviceAlarm deviceAlarm = new DeviceAlarm();
    if (alarmRequest.dvc.batt != null) {
      deviceAlarm.status = alarmRequest.dvc.batt.stat;
      deviceAlarm.time = alarmRequest.dvc.batt.time;
      deviceAlarm.powerAvailability = alarmRequest.dvc.batt.avl;
      deviceAlarm.type = BATTERY_ALARM;
      deviceAlarm.device = device;
      deviceAlarms.add(deviceAlarm);

      if (assetMappingList != null) {
        for (AssetMapping assetMapping : assetMappingList) {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.batt.stat;
          deviceAlarm.time = alarmRequest.dvc.batt.time;
          deviceAlarm.powerAvailability = alarmRequest.dvc.batt.avl;
          deviceAlarm.type = BATTERY_ALARM;
          deviceAlarm.device = assetMapping.relatedAsset;
          deviceAlarms.add(deviceAlarm);
        }
      }
    }

    if (alarmRequest.dvc.dCon != null) {
      deviceAlarm = new DeviceAlarm();
      deviceAlarm.status = alarmRequest.dvc.dCon.stat;
      deviceAlarm.time = alarmRequest.dvc.dCon.time;
      deviceAlarm.type = DEVICE_CONNECTION_ALARM;
      deviceAlarm.device = device;
      deviceAlarms.add(deviceAlarm);

      if (assetMappingList != null) {
        for (AssetMapping assetMapping : assetMappingList) {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.dCon.stat;
          deviceAlarm.time = alarmRequest.dvc.dCon.time;
          deviceAlarm.type = DEVICE_CONNECTION_ALARM;
          deviceAlarm.device = assetMapping.relatedAsset;
          deviceAlarms.add(deviceAlarm);
        }
      }
    }

    if (alarmRequest.dvc.poa != null) {
      deviceAlarm = new DeviceAlarm();
      deviceAlarm.status = alarmRequest.dvc.poa.stat;
      deviceAlarm.time = alarmRequest.dvc.poa.time;
      deviceAlarm.type = POWER_OUTAGE_ALARM;
      deviceAlarm.device = device;
      deviceAlarms.add(deviceAlarm);

      if (assetMappingList != null) {
        for (AssetMapping assetMapping : assetMappingList) {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.poa.stat;
          deviceAlarm.time = alarmRequest.dvc.poa.time;
          deviceAlarm.type = POWER_OUTAGE_ALARM;
          deviceAlarm.device = assetMapping.relatedAsset;
          deviceAlarms.add(deviceAlarm);
        }
      }
    }

    if (alarmRequest.dvc.errs != null && alarmRequest.dvc.errs.size() > 0) {
      for (DeviceErrorRequest deviceErrorRequest : alarmRequest.dvc.errs) {
        deviceAlarm = new DeviceAlarm();
        deviceAlarm.errorCode = deviceErrorRequest.code;
        deviceAlarm.time = deviceErrorRequest.time;
        deviceAlarm.errorMessage = deviceErrorRequest.msg;
        deviceAlarm.type = FIRMWARE_ALARM;
        deviceAlarm.device = device;
        deviceAlarms.add(deviceAlarm);
      }
    }

    try {
      assetMappingList =
          AssetMapping.findAssetRelationByAssetAndType(device, LogistimoConstant.CONTAINS);
    } catch (NoResultException e) {
      LOGGER.warn("Asset mapping not found for the device {}, {}", device.vendorId, device.deviceId,
          alarmRequest.dvc.iAct.toString());
    }

    if (alarmRequest.dvc.iAct != null) {
      deviceAlarm = new DeviceAlarm();
      deviceAlarm.status = alarmRequest.dvc.iAct.stat;
      deviceAlarm.time = alarmRequest.dvc.iAct.time;
      deviceAlarm.type = DEVICE_NODATA_ALARM;
      deviceAlarm.device = device;
      if (StringUtils.isNotEmpty(alarmRequest.sId)) {
        deviceAlarm.sensorId = alarmRequest.sId;
      } else {
        if (assetMappingList != null && assetMappingList.size() == 1) {
          deviceAlarm.sensorId =
              LogistimoUtils.extractSensorId(assetMappingList.get(0).relatedAsset.deviceId);
        }
      }
      deviceAlarms.add(deviceAlarm);

      //For multi sensor devices
      if (StringUtils.isNotEmpty(alarmRequest.sId)) {
        try {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.iAct.stat;
          deviceAlarm.time = alarmRequest.dvc.iAct.time;
          deviceAlarm.type = DEVICE_NODATA_ALARM;
          deviceAlarm.device =
              deviceService.findDevice(device.vendorId,
                  LogistimoUtils.generateVirtualDeviceId(device.deviceId, alarmRequest.sId));
          deviceAlarms.add(deviceAlarm);
        } catch (NoResultException e) {
          throw new LogistimoException(
              "Sensor " + alarmRequest.sId + " not found for the device: " + device.deviceId + " "
                  + device.vendorId);
        }
      } else {
        if (assetMappingList != null && assetMappingList.size() == 1) {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.iAct.stat;
          deviceAlarm.time = alarmRequest.dvc.iAct.time;
          deviceAlarm.type = DEVICE_NODATA_ALARM;
          deviceAlarm.device = assetMappingList.get(0).relatedAsset;
          deviceAlarms.add(deviceAlarm);
        } else {
          LOGGER.warn(
              "Too many relation/no relation found for the device {}, {}. Skipping alarm data propagation to virtual device {}",
              device.vendorId, device.deviceId, alarmRequest.dvc.iAct.toString());
        }
      }
    }

    if (alarmRequest.dvc.xSns != null) {
      deviceAlarm = new DeviceAlarm();
      deviceAlarm.status = alarmRequest.dvc.xSns.stat;
      deviceAlarm.time = alarmRequest.dvc.xSns.time;
      deviceAlarm.type = SENSOR_CONNECTION_ALARM;
      deviceAlarm.device = device;
      if (StringUtils.isNotEmpty(alarmRequest.sId)) {
        deviceAlarm.sensorId = alarmRequest.sId;
      } else {
        if (assetMappingList != null && assetMappingList.size() == 1) {
          deviceAlarm.sensorId =
              LogistimoUtils.extractSensorId(assetMappingList.get(0).relatedAsset.deviceId);
        }
      }
      deviceAlarms.add(deviceAlarm);

      //For multi sensor devices
      if (StringUtils.isNotEmpty(alarmRequest.sId)) {
        try {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.xSns.stat;
          deviceAlarm.time = alarmRequest.dvc.xSns.time;
          deviceAlarm.type = SENSOR_CONNECTION_ALARM;
          deviceAlarm.device =
              deviceService.findDevice(device.vendorId,
                  LogistimoUtils.generateVirtualDeviceId(device.deviceId, alarmRequest.sId));
          deviceAlarms.add(deviceAlarm);
        } catch (NoResultException e) {
          throw new LogistimoException(
              "Sensor " + alarmRequest.sId + " not found for the device: " + device.deviceId + " "
                  + device.vendorId);
        }
      } else {
        if (assetMappingList != null && assetMappingList.size() == 1) {
          deviceAlarm = new DeviceAlarm();
          deviceAlarm.status = alarmRequest.dvc.xSns.stat;
          deviceAlarm.time = alarmRequest.dvc.xSns.time;
          deviceAlarm.type = SENSOR_CONNECTION_ALARM;
          deviceAlarm.device = assetMappingList.get(0).relatedAsset;
          deviceAlarms.add(deviceAlarm);
        } else {
          LOGGER.warn(
              "Too many relation/no relation found for the device {}, {}. Skipping alarm data propagation to virtual device {}",
              device.vendorId, device.deviceId, alarmRequest.dvc.xSns.toString());
        }
      }
    }

    deviceAlarms = deviceAlarms.stream()
        .filter(alarm -> LogistimoUtils.isValidTime(alarm.time))
        .collect(Collectors.toList());

    if (deviceAlarms.size() == 0) {
      throw new LogistimoException("No Alarm details found in request.");
    }
    return deviceAlarms;
  }

  /**
   * Maps the DeviceAlarmStatsRequest request object to AlarmLog DBO
   *
   * @return DeviceAlarm
   */
  private List<AlarmLog> toAlarmLog(AlarmRequest alarmRequest, Device device)
      throws LogistimoException {
    List<AlarmLog> alarmLogs = new ArrayList<>();

    AlarmLog alarmLog;
    if (alarmRequest.dvc.batt != null && LogistimoUtils.isValidTime(alarmRequest.dvc.batt.time)) {
      closePreviousAlarmLog(AlarmLog.DEVICE_ALARM, BATTERY_ALARM, alarmRequest.dvc.batt.time,
          alarmRequest.sId,
          device);
      alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, alarmRequest.dvc.batt.time);
      alarmLog.deviceAlarmStatus = alarmRequest.dvc.batt.stat;
      alarmLog.deviceAlarmType = BATTERY_ALARM;
      alarmLog.device = device;
      alarmLog.startTime = alarmRequest.dvc.batt.time;
      alarmLog.updatedOn = new Date();
      alarmLogs.add(alarmLog);
    }

    if (alarmRequest.dvc.dCon != null && LogistimoUtils.isValidTime(alarmRequest.dvc.dCon.time)) {
      closePreviousAlarmLog(AlarmLog.DEVICE_ALARM, DEVICE_CONNECTION_ALARM,
          alarmRequest.dvc.dCon.time, alarmRequest.sId, device);
      alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, alarmRequest.dvc.dCon.time);
      alarmLog.deviceAlarmStatus = alarmRequest.dvc.dCon.stat;
      alarmLog.deviceAlarmType = DEVICE_CONNECTION_ALARM;
      alarmLog.startTime = alarmRequest.dvc.dCon.time;
      alarmLog.updatedOn = new Date();
      alarmLog.device = device;
      alarmLogs.add(alarmLog);
    }

    if (alarmRequest.dvc.poa != null && LogistimoUtils.isValidTime(alarmRequest.dvc.poa.time)) {
      closePreviousAlarmLog(AlarmLog.DEVICE_ALARM, POWER_OUTAGE_ALARM, alarmRequest.dvc.poa.time,
          alarmRequest.sId, device);
      alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, alarmRequest.dvc.poa.time);
      alarmLog.deviceAlarmStatus = alarmRequest.dvc.poa.stat;
      alarmLog.deviceAlarmType = POWER_OUTAGE_ALARM;
      alarmLog.startTime = alarmRequest.dvc.poa.time;
      alarmLog.updatedOn = new Date();
      alarmLog.device = device;
      alarmLogs.add(alarmLog);
    }

    if (alarmRequest.dvc.xSns != null && LogistimoUtils.isValidTime(alarmRequest.dvc.xSns.time)) {
      closePreviousAlarmLog(AlarmLog.DEVICE_ALARM, SENSOR_CONNECTION_ALARM,
          alarmRequest.dvc.xSns.time, alarmRequest.sId, device);
      alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, alarmRequest.dvc.xSns.time);
      alarmLog.deviceAlarmStatus = alarmRequest.dvc.xSns.stat;
      alarmLog.deviceAlarmType = SENSOR_CONNECTION_ALARM;
      alarmLog.device = device;
      alarmLog.sensorId = alarmRequest.sId;
      alarmLog.startTime = alarmRequest.dvc.xSns.time;
      alarmLog.updatedOn = new Date();
      alarmLogs.add(alarmLog);
    }

    if (alarmRequest.dvc.errs != null && alarmRequest.dvc.errs.size() > 0) {
      for (DeviceErrorRequest deviceErrorRequest : alarmRequest.dvc.errs) {
        if(LogistimoUtils.isValidTime(deviceErrorRequest.time)) {
          alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, deviceErrorRequest.time);
          alarmLog.deviceFirmwareErrorCode = deviceErrorRequest.code;
          alarmLog.deviceAlarmType = FIRMWARE_ALARM;
          alarmLog.device = device;
          alarmLog.startTime = deviceErrorRequest.time;
          alarmLog.updatedOn = new Date();
          alarmLogs.add(alarmLog);
        }
      }
    }

    if (alarmRequest.dvc.iAct != null && LogistimoUtils.isValidTime(alarmRequest.dvc.iAct.time)) {
      closePreviousAlarmLog(AlarmLog.DEVICE_ALARM, DEVICE_NODATA_ALARM, alarmRequest.dvc.iAct.time,
          alarmRequest.sId, device);
      alarmLog = new AlarmLog(AlarmLog.DEVICE_ALARM, alarmRequest.dvc.iAct.time);
      alarmLog.deviceAlarmStatus = alarmRequest.dvc.iAct.stat;
      alarmLog.deviceAlarmType = DEVICE_NODATA_ALARM;
      alarmLog.device = device;
      alarmLog.sensorId = alarmRequest.sId;
      alarmLog.startTime = alarmRequest.dvc.iAct.time;
      alarmLog.updatedOn = new Date();
      alarmLogs.add(alarmLog);
    }

    return alarmLogs;
  }

  private void closePreviousAlarmLog(Integer alarmType, Integer deviceAlarmType, Integer endTime,
                                     String sensorId, Device device) {
    if (alarmType != null && endTime != null && deviceAlarmType != null && device != null) {
      AlarmLog alarmLog;
      try {
        if (StringUtils.isNotEmpty(sensorId)) {
          alarmLog = alarmLogService.getOpenAlarmLogForAlarmTypeBySensorId(device, alarmType,
              deviceAlarmType, sensorId);
        } else {
          alarmLog = alarmLogService.getOpenAlarmLogForAlarmType(device, alarmType,
              deviceAlarmType);
        }
        alarmLog.endTime = endTime;
        alarmLog.update();
      } catch (NoResultException e) {
        //do nothing
      }
    }
  }

  /**
   * Maps the DeviceAlarm and DeviceError DBO to DeviceAlarmStatsResponse
   *
   * @return DeviceAlarmStatsResponse
   */
  public AlarmResponse toAlarmResponse(DeviceAlarm deviceAlarm) {
    AlarmResponse alarmResponse = new AlarmResponse();
    alarmResponse.typ = deviceAlarm.type;
    alarmResponse.stat = deviceAlarm.status;
    alarmResponse.time = deviceAlarm.time;
    alarmResponse.code = deviceAlarm.errorCode;
    alarmResponse.msg = deviceAlarm.errorMessage;
    alarmResponse.avl = deviceAlarm.powerAvailability;
    return alarmResponse;
  }

  public void generateAndPostAssetAlarm(Device device, String sId, Integer alarmType,
                                        Object alarmObject) throws ServiceException {
    AlarmLoggingRequest alarmLoggingRequest = new AlarmLoggingRequest(device.vendorId);
    DeviceAlarmRequest deviceAlarmRequest = new DeviceAlarmRequest();
    switch (alarmType) {
      case AssetStatusConstants.POWER_OUTAGE_ALARM_TYPE:
        deviceAlarmRequest.poa = (GenericAlarmRequest) alarmObject;
        break;
      case AssetStatusConstants.XSNS_ALARM_TYPE:
        deviceAlarmRequest.xSns = (ExternalSensorAlarmsRequest) alarmObject;
        break;
      case AssetStatusConstants.ACTIVITY_ALARM_TYPE:
        deviceAlarmRequest.iAct = (GenericAlarmRequest) alarmObject;
        break;
      case AssetStatusConstants.BATTERY_ALARM_TYPE:
        deviceAlarmRequest.batt = (BatteryAlarmsRequest) alarmObject;
        break;
      case AssetStatusConstants.DEVICE_CONN_ALARM_TYPE:
        deviceAlarmRequest.dCon = (DeviceConnectionAlarmsRequest) alarmObject;
        break;
      default:
        throw new ServiceException(
            "Invalid alarmType " + alarmType + " while posting asset alarm for the device");
    }
    AlarmRequest alarmRequest = new AlarmRequest(device.deviceId, sId, deviceAlarmRequest);
    alarmLoggingRequest.data.add(alarmRequest);

    //posting
    postDeviceAlarm(alarmLoggingRequest);
  }

  public void updateRelatedAssetStatus(Device device, DeviceStatus deviceStatus) {
    //Updating parent status activity
    DeviceEventPushModel deviceEventPushModel = new DeviceEventPushModel();
    Optional<Device>
        updatedDevice =
        updateParentAssetStatus(deviceStatus, device,
            AssetStatusConstants.ACTIVITY_ALARM_TYPE, deviceEventPushModel);

    if (updatedDevice.isPresent()) {
      updateAndPushEvent(deviceEventPushModel,
          Collections.singleton(updatedDevice.get()));
    }

    //Updating monitored asset status
    deviceEventPushModel = new DeviceEventPushModel();
    updatedDevice =
        updateMonitoredAssetStatus(deviceStatus, device,
            AssetStatusConstants.ACTIVITY_ALARM_TYPE, deviceEventPushModel);

    if (updatedDevice.isPresent()) {
      updateAndPushEvent(deviceEventPushModel,
          Collections.singleton(updatedDevice.get()));
    }
  }
}

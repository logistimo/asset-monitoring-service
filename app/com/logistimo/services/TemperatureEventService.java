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
import com.logistimo.db.Device;
import com.logistimo.db.DeviceMetaData;
import com.logistimo.db.DeviceStatus;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.device.common.DeviceEventPushModel;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.models.task.TemperatureEventType;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.DeviceMetaAppendix;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;

import java.util.Date;
import java.util.Map;

import javax.persistence.NoResultException;

import play.Logger;
import play.db.jpa.JPA;

/**
 * Created by kaniyarasu on 11/08/15.
 */
@SuppressWarnings("unchecked")
public class TemperatureEventService extends ServiceImpl implements Executable {
  public static final String EVENT_TYPE = "event-type";
  public static final String DEVICE_ID = "device-id";
  public static final String VENDOR_ID = "vendor-id";
  public static final String STATE_UPDATED_TIME = "exc_time";
  private static final Logger.ALogger LOGGER = Logger.of(TemperatureEventService.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);
  private static final TaskService taskService = ServiceFactory.getService(TaskService.class);
  private static final TemperatureService
      temperatureService =
      ServiceFactory.getService(TemperatureService.class);
  private static final AlarmLogService alarmLogService = ServiceFactory.getService(AlarmLogService.class);

  @Override
  public void process(String object, Map<String, Object> options) throws Exception {
    if (options != null && options.get(EVENT_TYPE) != null
        && options.get(STATE_UPDATED_TIME) != null
        && options.get(VENDOR_ID) != null && options.get(DEVICE_ID) != null) {
      LockUtil.LockStatus
          status = null;
      try {
        status =
            LockUtil.lock("LOCK_" + options.get(VENDOR_ID) + "_" + LogistimoUtils.extractDeviceId(
                (String) options.get(DEVICE_ID)));
        if (!LockUtil.isLocked(status)) {
          throw new ServiceException("Failed to lock device "+"LOCK_" + options.get(VENDOR_ID) + "_" + LogistimoUtils.extractDeviceId(
              (String) options.get(DEVICE_ID)));
        }
        JPA.withTransaction(() -> {
          Device
              device =
              deviceService
                  .findDevice((String) options.get(VENDOR_ID), (String) options.get(DEVICE_ID));
          DeviceStatus
              deviceStatus =
              deviceService
                  .getOrCreateDeviceStatus(device, null, null, AssetStatusConstants.TEMP_STATUS_KEY,
                      null);
          if (deviceStatus.statusUpdatedTime.equals(options.get(STATE_UPDATED_TIME))) {
            if (options.get(EVENT_TYPE) == TemperatureEventType.EXCURSION) {
              Map<String, DeviceMetaData>
                  deviceMetaDataMap =
                  deviceService.getDeviceMetaDataMap(device, DeviceMetaAppendix.TEMP_WARN_GROUP);
              Long warningEventDelay = 0L;
              if (deviceStatus.temperatureAbnormalStatus == 2) {
                warningEventDelay = getExecutionDelayTime(deviceStatus.statusUpdatedTime
                    , LogistimoUtils
                    .getMetaDataValue(deviceMetaDataMap.get(DeviceMetaAppendix.WARN_HIGH_DUR)));
              } else if (deviceStatus.temperatureAbnormalStatus == 1) {
                warningEventDelay = getExecutionDelayTime(deviceStatus.statusUpdatedTime
                    , LogistimoUtils
                    .getMetaDataValue(deviceMetaDataMap.get(DeviceMetaAppendix.WARN_LOW_DUR)));
              }

              options.put(TemperatureEventService.EVENT_TYPE, TemperatureEventType.WARNING);
              taskService.produceMessage(
                  new TaskOptions(
                      TaskType.BACKGROUND_TASK.getValue(),
                      TemperatureEventService.class,
                      null,
                      options,
                      Math.max(warningEventDelay, 30000)
                  )
              );
            } else if (options.get(EVENT_TYPE) == TemperatureEventType.WARNING) {
              int
                  deviceExcursionTime =
                  updateAndPropagateDeviceStatus(device, deviceStatus,
                      Device.TEMP_WARNING);

              Map<String, DeviceMetaData>
                  deviceMetaDataMap =
                  deviceService.getDeviceMetaDataMap(device, DeviceMetaAppendix.TEMP_ALARM_GROUP);
              Long alarmEventDelay = 0L;
              if (deviceStatus.temperatureAbnormalStatus == 2) {
                alarmEventDelay = getExecutionDelayTime(deviceExcursionTime
                    , LogistimoUtils
                    .getMetaDataValue(deviceMetaDataMap.get(DeviceMetaAppendix.ALARM_HIGH_DUR)));
              } else if (deviceStatus.temperatureAbnormalStatus == 1) {
                alarmEventDelay = getExecutionDelayTime(deviceExcursionTime
                    , LogistimoUtils
                    .getMetaDataValue(deviceMetaDataMap.get(DeviceMetaAppendix.ALARM_LOW_DUR)));
              }

              options.put(TemperatureEventService.EVENT_TYPE, TemperatureEventType.ALARM);
              options
                  .put(TemperatureEventService.STATE_UPDATED_TIME, deviceStatus.statusUpdatedTime);
              taskService.produceMessage(
                  new TaskOptions(
                      TaskType.BACKGROUND_TASK.getValue(),
                      TemperatureEventService.class,
                      null,
                      options,
                      Math.max(alarmEventDelay, 30000)
                  )
              );
            } else if (options.get(EVENT_TYPE) == TemperatureEventType.ALARM) {
              updateAndPropagateDeviceStatus(device, deviceStatus,
                  Device.TEMP_ALARM);
            }
          } else {
            LOGGER.info("Device state changed while executing event {}", options.toString());
          }
        });
      } catch (NoResultException e) {
        LOGGER.warn("{} while scheduling temperature event for task options {}", e.getMessage(),
            options.toString(), e);
      } finally {
        if (LockUtil.shouldReleaseLock(status)){
          LockUtil.release(
              "LOCK_" + options.get(VENDOR_ID) + "_" + LogistimoUtils.extractDeviceId(
                  (String) options.get(DEVICE_ID)));
        }
      }
    }
  }

  private int updateAndPropagateDeviceStatus(Device device, DeviceStatus deviceStatus,
                                             int tempStatus)
      throws ServiceException {

    TemperatureEventType temperatureEventType = tempStatus == Device.TEMP_ALARM ?
        TemperatureEventType.ALARM : TemperatureEventType.WARNING;
    //Updating device state
    int deviceExcursionTime = deviceStatus.statusUpdatedTime;
    deviceStatus.status = tempStatus;
    deviceStatus.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
    deviceStatus.update();

    //Propagating the temperature state to MonitoredAsset
    updateMonitoredAssetStatus(device, temperatureEventType);
    try {
      temperatureService.updateParentDeviceWithSensorTemperatureStatus(device);
      AssetMapping
          assetMapping =
          AssetMapping.findAssetMappingByRelatedAssetAndType(device,
              LogistimoConstant.CONTAINS);
      deviceService.updateOverallDeviceStatus(assetMapping.asset,
          AssetStatusConstants.TEMP_STATUS_KEY);
    } catch (NoResultException ne) {
      //do nothing
    }

    createTemperatureEvent(device);
    return deviceExcursionTime;
  }

  private Long getExecutionDelayTime(Integer stateUpdatedTime, Integer duration) {
    if (stateUpdatedTime != null && duration != null) {
      Long eta = (stateUpdatedTime + duration * 60) * 1000L;
      if (eta > System.currentTimeMillis()) {
        return eta - System.currentTimeMillis();
      }
    }
    return 0L;
  }

  private void createTemperatureEvent(Device monitoringAsset) throws ServiceException {
    AssetMapping assetMapping;
    try {
      assetMapping =
          AssetMapping.findAssetMappingByRelatedAssetAndType(monitoringAsset,
              LogistimoConstant.MONITORED_BY);
    } catch (NoResultException e) {
      return;
    }

    DeviceStatus
        deviceStatus =
        deviceService
            .getOrCreateDeviceStatus(assetMapping.asset, assetMapping.monitoringPositionId, null,
                AssetStatusConstants.TEMP_STATUS_KEY, null);
    //Generating temperature event and posting to Logistics service
    DeviceEventPushModel.DeviceEvent deviceEvent = new DeviceEventPushModel.DeviceEvent();
    deviceEvent.vId = assetMapping.asset.vendorId;
    deviceEvent.dId = assetMapping.asset.deviceId;
    deviceEvent.mpId = assetMapping.monitoringPositionId;
    deviceEvent.st = deviceStatus.status;
    deviceEvent.time = deviceStatus.statusUpdatedTime;
    deviceEvent.tmp = deviceStatus.temperature;
    deviceEvent.aSt = deviceStatus.temperatureAbnormalStatus;
    deviceEvent.type = DeviceEventPushModel.DEVICE_EVENT_TEMP;

    Map<String, String>
        assetMetaMap =
        deviceService.getDeviceMetaDataValueAsString(monitoringAsset, DeviceMetaAppendix.TMP_GROUP);
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
      LOGGER
          .error("{} while scheduling task for posting temperature event to Logistics service, {}",
              e.getMessage(), deviceEvent.toString(), e);
    }
  }

  private void updateMonitoredAssetStatus(Device monitoringAsset,
                                          TemperatureEventType temperatureEventType) {
    AssetMapping assetMapping;
    try {
      assetMapping =
          AssetMapping.findAssetMappingByRelatedAssetAndType(monitoringAsset,
              LogistimoConstant.MONITORED_BY);
    } catch (NoResultException e) {
      return;
    }
    DeviceStatus
        deviceStatus =
        deviceService
            .getOrCreateDeviceStatus(assetMapping.asset, assetMapping.monitoringPositionId, null,
                AssetStatusConstants.TEMP_STATUS_KEY, null);
    DeviceStatus
        relatedAssetStatus =
        deviceService.getOrCreateDeviceStatus(assetMapping.relatedAsset, null, null,
            AssetStatusConstants.TEMP_STATUS_KEY, null);
    try {
      AlarmLog
          oldAlarmLog =
          alarmLogService.getOpenAlarmLogForDeviceAndMPId(assetMapping.asset,
              assetMapping.monitoringPositionId, AlarmLog.TEMP_ALARM, null);
      oldAlarmLog.endTime = relatedAssetStatus.statusUpdatedTime;
      oldAlarmLog.update();

    } catch (NoResultException e) {
      //do nothing
    }
    deviceStatus.temperature = relatedAssetStatus.temperature;
    deviceStatus.temperatureUpdatedTime = relatedAssetStatus.temperatureUpdatedTime;
    deviceStatus.status = relatedAssetStatus.status;
    deviceStatus.statusUpdatedTime = relatedAssetStatus.statusUpdatedTime;
    deviceStatus.temperatureAbnormalStatus = relatedAssetStatus.temperatureAbnormalStatus;
    deviceStatus.update();

    //Updating Alarm log for monitored asset
    AlarmLog alarmLog = new AlarmLog(AlarmLog.TEMP_ALARM, deviceStatus.statusUpdatedTime);
    alarmLog.temperature = deviceStatus.temperature;
    alarmLog.temperatureType = temperatureEventType.getValue();
    alarmLog.device = assetMapping.asset;
    alarmLog.monitoringPositionId = assetMapping.monitoringPositionId;
    alarmLog.temperatureAbnormalType = deviceStatus.temperatureAbnormalStatus;
    alarmLog.startTime = deviceStatus.statusUpdatedTime;
    alarmLog.updatedOn = new Date();
    alarmLog.save();

    deviceService.updateOverallDeviceStatus(assetMapping.asset,
        AssetStatusConstants.TEMP_STATUS_KEY);
  }
}

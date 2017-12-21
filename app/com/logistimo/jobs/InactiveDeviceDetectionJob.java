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

package com.logistimo.jobs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.logistimo.db.AssetType;
import com.logistimo.db.Device;
import com.logistimo.db.DeviceStatus;
import com.logistimo.exception.ServiceException;
import com.logistimo.healthcheck.MetricsUtil;
import com.logistimo.models.alarm.request.AlarmLoggingRequest;
import com.logistimo.models.alarm.request.AlarmRequest;
import com.logistimo.models.alarm.request.DeviceAlarmRequest;
import com.logistimo.models.alarm.request.GenericAlarmRequest;
import com.logistimo.services.AlarmService;
import com.logistimo.services.DeviceService;
import com.logistimo.services.ServiceFactory;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.DeviceMetaAppendix;
import com.logistimo.utils.LockUtil;
import com.logistimo.utils.LogistimoUtils;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.db.jpa.JPA;

/**
 * Created by kaniyarasu on 26/06/15.
 */
public class InactiveDeviceDetectionJob implements Job {

  private static final Logger.ALogger LOGGER = Logger.of(InactiveDeviceDetectionJob.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);
  private static final AlarmService alarmService = ServiceFactory.getService(AlarmService.class);

  private static final Meter
      inactMeter = MetricsUtil.getMeter(InactiveDeviceDetectionJob.class,"inactivedevice.job");
  private static final Timer
      inactTimer = MetricsUtil.getTimer(InactiveDeviceDetectionJob.class,"inactivedevice.job");


  private void doJob() {
    inactMeter.mark();
    Timer.Context context = inactTimer.time();
    try {
      final List<Device> deviceList = new ArrayList<>(1);
      JPA.withTransaction(() -> deviceList.addAll(deviceService.getActiveSensorDevices()));
      long currentTime = (int) (System.currentTimeMillis() / 1000);
      if (!deviceList.isEmpty()) {

        LOGGER.info(
            "Inside InactiveDeviceDetectionJob - doJob(), Initiating the job for the device list with size - {}",
            deviceList.size());
        for (Device device : deviceList) {
          LockUtil.LockStatus
              status = null;
          try {
            status =
                LockUtil.lock("LOCK_" + device.vendorId + "_" +
                    LogistimoUtils.extractDeviceId(device.deviceId));
            if (!LockUtil.isLocked(status)) {
              throw new ServiceException("Failed to lock device");
            }
            JPA.withTransaction(() -> {
              Map<String, String>
                  deviceMetaDataMap =
                  deviceService
                      .getDeviceMetaDataValueAsString(device, DeviceMetaAppendix.INT_GROUP);
              int pushInt = deviceMetaDataMap.get(DeviceMetaAppendix.INT_PINT) != null ?
                  Integer.parseInt(deviceMetaDataMap.get(DeviceMetaAppendix.INT_PINT)) * 60 : 3600;
              int iActCounts =
                  deviceMetaDataMap.get(DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT) != null
                      ? Integer.parseInt(
                      deviceMetaDataMap.get(DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT))
                      : DeviceMetaAppendix.INACTIVE_PUSH_INTERVAL_COUNT_DEFAULT_VALUE;

              DeviceStatus
                  temperatureStatus =
                  deviceService.getOrCreateDeviceStatus(device, null, null,
                      AssetStatusConstants.TEMP_STATUS_KEY, null);
              final DeviceStatus
                  activityStatus =
                  deviceService.getOrCreateDeviceStatus(device, null, null,
                      AssetStatusConstants.ACTIVITY_STATUS_KEY, null);

              if (temperatureStatus.temperatureUpdatedTime == null
                  || temperatureStatus.temperatureUpdatedTime == 0) {
                if (device.createdOn == null) {
                  activityStatus.status = AssetStatusConstants.ACTIVITY_STATUS_INACT;
                  activityStatus.statusUpdatedTime = (int) currentTime;
                } else {
                  Calendar calendar = GregorianCalendar.getInstance();
                  calendar.setTime(device.createdOn);
                  long createdTimeLong = (int) (calendar.getTimeInMillis() / 1000);
                  if (createdTimeLong < (currentTime - (iActCounts * pushInt))) {
                    activityStatus.status = AssetStatusConstants.ACTIVITY_STATUS_INACT;
                    activityStatus.statusUpdatedTime = ((int) createdTimeLong);
                  }
                }
              } else if (temperatureStatus.temperatureUpdatedTime < (currentTime - (iActCounts
                  * pushInt))) {
                activityStatus.status = AssetStatusConstants.ACTIVITY_STATUS_INACT;
                activityStatus.statusUpdatedTime = temperatureStatus.temperatureUpdatedTime;
              }

              //Generating device alarm
              if (activityStatus.status.equals(AssetStatusConstants.ACTIVITY_STATUS_INACT)) {

                activityStatus.update();
                final AlarmLoggingRequest
                    alarmLoggingRequest =
                    new AlarmLoggingRequest(device.vendorId);
                GenericAlarmRequest
                    genericAlarmRequest =
                    new GenericAlarmRequest(1, activityStatus.statusUpdatedTime);
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
            });
          } catch (Throwable e) {
            LOGGER.error("Error while running inactive device detection job for the device {}, {}",
                device.vendorId, device.deviceId, e);
          } finally {
            if (LockUtil.shouldReleaseLock(status)) {
              LockUtil
                  .release("LOCK_" + device.vendorId + "_" +
                      LogistimoUtils.extractDeviceId(device.deviceId));
            }
          }
        }

        LOGGER.info("Inside InactiveDeviceDetectionJob - doJob(), End of execution.");
      }
    } catch (Throwable e) {
      LOGGER.error("Error while running inactive device detection job", e);
    } finally {
      context.stop();
    }
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    doJob();
  }

}
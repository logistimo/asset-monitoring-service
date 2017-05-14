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

package com.logistimo.dao;

import com.logistimo.db.Device;
import com.logistimo.db.DevicePowerTransition;
import com.logistimo.db.DeviceStatus;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.alarm.request.BatteryAlarmsRequest;
import com.logistimo.models.alarm.request.GenericAlarmRequest;
import com.logistimo.models.device.request.DevicePowerAvailabilityRequest;
import com.logistimo.models.device.request.DeviceReadingRequest;
import com.logistimo.services.AlarmService;
import com.logistimo.services.DeviceService;
import com.logistimo.services.ServiceFactory;
import com.logistimo.utils.AssetStatusConstants;

import java.util.Objects;

import javax.persistence.NoResultException;

import play.Logger;

/**
 * Created by naveensnair on 20/03/17.
 */
public class DevicePowerTransitionDAO {

    private static final Logger.ALogger LOGGER = Logger.of(DevicePowerTransitionDAO.class);
    private static final AlarmService alarmService = ServiceFactory.getService(AlarmService.class);
    private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);

    /**
     * Creates a new device power transition, power outage alarm and clears any battery alarm
     * @param device
     * @param pwa
     * @return DevicePowerTransition object
     */
    public static DevicePowerTransition logPowerTransition(Device device,
                                                           DevicePowerAvailabilityRequest pwa) {
        DevicePowerTransition devicePowerTransition = null;
        try {
            devicePowerTransition = new DevicePowerTransition();
            devicePowerTransition.device = device;
            devicePowerTransition.state = pwa.stat;
            devicePowerTransition.time = pwa.time;
            devicePowerTransition.save();

            //Generating power outage alarm
            DeviceStatus powerDeviceStatus = deviceService.getOrCreateDeviceStatus(device, null, null, AssetStatusConstants.POWER_OUTAGE_STATUS_KEY, AssetStatusConstants.POWER_OUTAGE_STATUS_OK);
            if((pwa.stat == 0 && powerDeviceStatus.status.equals(AssetStatusConstants.POWER_OUTAGE_STATUS_OK)) ||
                    (pwa.stat == 1 && powerDeviceStatus.status.equals(AssetStatusConstants.POWER_OUTAGE_STATUS_NA))){
                try {
                    GenericAlarmRequest
                            genericAlarmRequest =
                            new GenericAlarmRequest(
                                    pwa.stat == 0 ? AssetStatusConstants.POWER_OUTAGE_STATUS_NA
                                            : AssetStatusConstants.POWER_OUTAGE_STATUS_OK, pwa.time);
                    alarmService
                            .generateAndPostAssetAlarm(device, null, AssetStatusConstants.POWER_OUTAGE_ALARM_TYPE,
                                    genericAlarmRequest);
                } catch (ServiceException e) {
                    LOGGER.warn("{} while generating power outage alarm for the device {}, {}", e.getMessage(),
                            device.vendorId, device.deviceId, e);
                }
            }

            //Clearing battery alarm if any
            if (devicePowerTransition.state == 1) {
                try {
                    DeviceStatus
                            deviceStatus =
                            DeviceStatus.getDeviceStatus(device, AssetStatusConstants.BATTERY_ALARM_STATUS_KEY);
                    if ((Objects.equals(deviceStatus.status,
                            AssetStatusConstants.BATTERY_ALARM_ALARM)
                            || Objects.equals(deviceStatus.status, AssetStatusConstants.BATTERY_ALARM_WARNING))
                            && devicePowerTransition.time > deviceStatus.statusUpdatedTime) {
                        try {
                            BatteryAlarmsRequest
                                    batteryAlarmsRequest =
                                    new BatteryAlarmsRequest(AssetStatusConstants.BATTERY_ALARM_NORMAL,
                                            devicePowerTransition.time);
                            alarmService
                                    .generateAndPostAssetAlarm(device, null, AssetStatusConstants.BATTERY_ALARM_TYPE,
                                            batteryAlarmsRequest);
                        } catch (ServiceException e) {
                            LOGGER.warn("{} while generating sensor connected alarm for the device {}, {}",
                                    e.getMessage(), device.vendorId, device.deviceId, e);
                        }
                    }
                } catch (NoResultException e) {
                    //do nothing, means there is sensor disconnected alarm for current device
                }
            }
        } catch (Exception e)
        {
            LOGGER.warn("Error while logging device power state for the device {}, {}", device.vendorId,
                    device.deviceId, e);
        }
        return devicePowerTransition;
    }
}
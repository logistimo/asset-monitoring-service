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

package com.logistimo.models.v1.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.logistimo.db.Device;
import com.logistimo.db.TemperatureStatistics;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceStats {
    @NotEmpty
    public String deviceId;

    @NotNull
    public Stats stats;

    public TemperatureStatistics toDeviceStatistics(Device d) {
        TemperatureStatistics ds = new TemperatureStatistics();
        ds.device = d;
        ds.dayOfComputation = stats.day;
        ds.numberOfAlerts = stats.numAlerts;
        ds.meanTemperature = stats.meanTemperature;
        if (stats.lowAlert != null) {
            ds.firstLowAlertTime = stats.lowAlert.triggerTime;
            ds.lowestTemperature = stats.lowAlert.lowestTemperature;
            ds.durationLow = stats.lowAlert.durationLow;
            ds.lowAlertAmbientTemperatureLow = stats.lowAlert.ambientTemperature;
            ds.numberOfLowAlerts = stats.lowAlert.numAlerts;
        }
        if (stats.highAlert != null) {
            ds.firstHighAlertTime = stats.highAlert.triggerTime;
            ds.highestTemperature = stats.highAlert.highestTemperature;
            ds.durationHigh = stats.highAlert.durationHigh;
            ds.highAlertAmbientTemperature = stats.highAlert.ambientTemperature;
            ds.numberOfHighAlerts = stats.highAlert.numAlerts;
        }

        if (stats.sensorConnection != null) {
            ds.firstSensorConnectionFailureTime = stats.sensorConnection.triggerTime;
            ds.sensorConnectionFailureDuration = stats.sensorConnection.durationFail;
        }

        if (stats.communication != null) {
            ds.numberOfSMSSent = stats.communication.numSMSSent;
            ds.numberOfInternetPushes = stats.communication.numInternetPushes;
            ds.numberInternetPushFailures = stats.communication.numInternetFailures;
        }
        return ds;
    }
}

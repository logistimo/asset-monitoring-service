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

package com.logistimo.models.stats.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyStats {
    @NotNull
    @Min(0)
    public Integer day;

    public Double tz;

    @NotNull
    public Double mean;

    @NotNull
    public Double min;

    @NotNull
    public Double max;

    @Valid
    public AlertStats high;

    @Valid
    public AlertStats low;

    @NotNull
    @Valid
    public CommunicationStats comm;

    @NotNull
    @Valid
    public BatteryStats batt;

    @Valid
    public List<DailyStatsDeviceError> errs;

    @Valid
    private int nExc;

    @Valid
    private ExternalSensorStats xSns;

    @Valid
    private DeviceConnectionStats dCon;

    public StorageStats str;

    public int getnExc() {
        return nExc;
    }

    public void setnExc(int nExc) {
        this.nExc = nExc;
    }

    public ExternalSensorStats getxSns() {
        return xSns;
    }

    public void setxSns(ExternalSensorStats xSns) {
        this.xSns = xSns;
    }

    public DeviceConnectionStats getdCon() {
        return dCon;
    }

    public void setdCon(DeviceConnectionStats dCon) {
        this.dCon = dCon;
    }

    @Override
    public String toString() {
        return "DailyStats{" +
                "day=" + day +
                ", mean=" + mean +
                ", min=" + min +
                ", max=" + max +
                ", high=" + high +
                ", low=" + low +
                ", batt=" + batt +
                ", comm=" + comm +
                ", errs=" + errs +
                ", nExc=" + nExc +
                ", xSns=" + xSns +
                ", dCon=" + dCon +
                '}';
    }
}

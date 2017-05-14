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

package com.logistimo.models.temperature.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by kaniyarasu on 19/11/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetTemperatureResponse {
    public Integer st;

    public Double tmp;

    //Abnormality status
    private Integer aSt;

    public Integer time;

    public Integer stut;

    //Asset monitoring location id, if any
    private Integer mpId = null;

    public Double min;

    public Double max;

    private String sId;

    public String getsId() {
        return sId;
    }

    public void setsId(String sId) {
        this.sId = sId;
    }

    public Integer getaSt() {
        return aSt;
    }

    public void setaSt(Integer aSt) {
        this.aSt = aSt;
    }

    public Integer getMpId() {
        return mpId;
    }

    public void setMpId(Integer mpId) {
        this.mpId = mpId;
    }
}

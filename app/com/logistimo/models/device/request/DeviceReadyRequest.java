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

package com.logistimo.models.device.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.logistimo.models.device.common.DeviceReadyModel;
import com.logistimo.models.device.common.DeviceReadySim;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

import javax.validation.Valid;

/**
 * Created by kaniyarasu on 31/10/14.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceReadyRequest {
  public String trId;

  @Valid
  public DeviceReadyModel dev;

  public DeviceReadySim sim;

  public DeviceReadySim altSim;
  public List<String> actSns;
  @NotEmpty
  private String dId;

  public String getdId() {
    return dId;
  }

  public void setdId(String dId) {
    this.dId = dId;
  }

  @Override
  public String toString() {
    return "DeviceReadyRequest{" +
        "trId='" + trId + '\'' +
        ", dev=" + dev +
        ", sim=" + sim +
        ", altSim=" + altSim +
        ", dId='" + dId + '\'' +
        '}';
  }
}

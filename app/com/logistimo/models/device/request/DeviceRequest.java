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
import com.fasterxml.jackson.databind.JsonNode;
import com.logistimo.models.device.common.TemperatureSensorRequest;
import com.logistimo.models.device.response.DeviceStatusModel;

import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotEmpty
  @Size(max = 100)
  public String dId;

  @NotEmpty
  @Size(max = 50)
  public String vId;

  public String ovId;

  @Size(max = 100)
  public String trId;

  public JsonNode meta;

  public Integer typ;

  //Image URLs
  public List<String> iUs;

  public String vNm;

  //Asset owners
  public List<String> ons;

  //Asset maintainers
  public List<String> mts;

  public List<String> tags;

  public List<TemperatureSensorRequest> sns;

  public List<Integer> mps;

  //Created by user
  public String cb;

  //Updated by user
  public String ub;

  public DeviceStatusModel ws;

  private String lId;

  public String getlId() {
    return lId;
  }

  public void setlId(String lId) {
    this.lId = lId;
  }
}

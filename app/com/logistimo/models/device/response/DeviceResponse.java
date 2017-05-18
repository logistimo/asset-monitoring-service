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

package com.logistimo.models.device.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.logistimo.models.alarm.response.AlarmResponse;
import com.logistimo.models.asset.AssetMapModel;
import com.logistimo.models.device.common.TemperatureSensorRequest;
import com.logistimo.models.temperature.response.AssetTemperatureResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse {

  public String trId;
  public Set<String> tags = new TreeSet<String>();
  public List<AssetTemperatureResponse> tmp;
  public List<AlarmResponse> alrm;
  public JsonNode meta;
  public String mdl;
  public DeviceStatusModel cfg;
  public DeviceStatusModel ws;
  public DeviceReadyResponse drdy;
  public List<TemperatureSensorRequest> sns;
  public List<String> ons;
  public List<String> mts;
  public Integer typ;
  public Date co;
  public Date uo;
  public String cb;
  public String ub;
  public Map<Integer, AssetMapModel> rel;
  private String dId;
  private String vId;
  private List<String> iUs;
  private String vNm;
  private String mpId;

  public DeviceResponse() {
    tmp = new ArrayList<>(1);
    ons = new ArrayList<>(1);
    mts = new ArrayList<>(1);
    alrm = new ArrayList<>(1);
  }

  public String getMpId() {
    return mpId;
  }

  public void setMpId(String mpId) {
    this.mpId = mpId;
  }

  public List<String> getiUs() {
    return iUs;
  }

  public void setiUs(List<String> iUs) {
    this.iUs = iUs;
  }

  public String getvNm() {
    return vNm;
  }

  public void setvNm(String vNm) {
    this.vNm = vNm;
  }

  public String getdId() {
    return dId;
  }

  public void setdId(String dId) {
    this.dId = dId;
  }

  public String getvId() {
    return vId;
  }

  public void setvId(String vId) {
    this.vId = vId;
  }
}

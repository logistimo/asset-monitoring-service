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

package com.logistimo.models.alarm.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AlarmResponse {
  public int typ = -1;

  public int stat = -1;

  public int time = 0;

  public String code = "-1";

  public String msg;

  public Integer avl;

  private String sId = null;

  private Integer mpId = null;

  public AlarmResponse() {
  }

  public AlarmResponse(int typ, int stat, String code, String sId) {
    this.typ = typ;
    this.stat = stat;
    this.code = code;
    this.sId = sId;
  }

  public AlarmResponse(int typ, int stat, int time, String code, String msg) {
    this.typ = typ;
    this.stat = stat;
    this.time = time;
    this.code = code;
    this.msg = msg;
  }

  public AlarmResponse(int typ, int stat, int time, String code, String msg, String sId) {
    this.typ = typ;
    this.stat = stat;
    this.time = time;
    this.code = code;
    this.msg = msg;
    this.sId = sId;
  }

  public String getsId() {
    return sId;
  }

  public void setsId(String sId) {
    this.sId = sId;
  }

  public Integer getMpId() {
    return mpId;
  }

  public void setMpId(Integer mpId) {
    this.mpId = mpId;
  }
}

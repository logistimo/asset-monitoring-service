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

package com.logistimo.models.alarm.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmRequest {
  @NotEmpty
  public String dId;

  public String trId;

  public String sId;

  @NotNull
  public DeviceAlarmRequest dvc;

  public AlarmRequest() {
  }

  public AlarmRequest(String dId, DeviceAlarmRequest dvc) {
    this.dId = dId;
    this.dvc = dvc;
  }

  public AlarmRequest(String dId, String sId, DeviceAlarmRequest dvc) {
    this.dId = dId;
    if (sId != null) {
      this.sId = sId;
    }
    this.dvc = dvc;
  }
}

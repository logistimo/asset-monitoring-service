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

package com.logistimo.models.temperature.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemperatureRequest implements Comparable<TemperatureRequest> {

  @Min(0)
  public Integer time = 0;

  @Min(0)
  public Integer typ = 0;

  public Double tmp;

  public String sId;

  public TemperatureRequest() {
  }

  public TemperatureRequest(Integer time, Integer typ, Double tmp) {
    this.time = time;
    this.typ = typ;
    this.tmp = tmp;
  }

  @Override
  public String toString() {
    return "TemperatureRequest{" +
        "time=" + time +
        ", typ=" + typ +
        ", tmp=" + tmp +
        ", sId='" + sId + '\'' +
        '}';
  }

  @Override
  public int compareTo(TemperatureRequest o) {
    if (o == null || o.tmp == null) {
      return 1;
    }

    return this.time - o.time;
  }
}

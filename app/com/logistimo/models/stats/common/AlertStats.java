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

import javax.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AlertStats {
  public int stat = -1;
  @Min(0)
  public Integer dur = 0;
  @Min(0)
  public Integer time = 0;
  public boolean cnf;
  public String cnfms;
  private int nAlrms = 0;
  private double aTmp;

  public double getaTmp() {
    return aTmp;
  }

  public void setaTmp(double aTmp) {
    this.aTmp = aTmp;
  }

  public int getnAlrms() {
    return nAlrms;
  }

  public void setnAlrms(int nAlrms) {
    this.nAlrms = nAlrms;
  }

  @Override
  public String toString() {
    return "AlertStats{" +
        "stat=" + stat +
        ", nAlrms=" + nAlrms +
        ", dur=" + dur +
        ", time=" + time +
        ", aTmp=" + aTmp +
        ", cnfms='" + cnfms + '\'' +
        '}';
  }
}

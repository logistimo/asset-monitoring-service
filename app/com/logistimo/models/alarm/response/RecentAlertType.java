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

/**
 * Created by kaniyarasu on 12/01/15.
 */
public enum RecentAlertType {
  HIGH_EXCURSION("High temperature excursion"),
  LOW_EXCURSION("Low temperature excursion"),
  BATTERY_WARNING("Battery warning"),
  BATTERY_ALARM("Battery alarm"),
  DEVICE_CONNECTION("Device connection error"),
  EXTERNAL_SENSOR("External sensor error"),
  FIRMWARE_ERROR("Firmware error");

  private final String value;

  RecentAlertType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

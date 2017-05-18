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

package com.logistimo.models.device.common;

import com.logistimo.utils.AssetStatusConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kaniyarasu on 01/12/15.
 */
public class DeviceEventPushModel {
  public static Integer DEVICE_EVENT_WORKING = 0;
  public static Integer DEVICE_EVENT_TEMP = 1;
  public static Integer DEVICE_EVENT_BATTERY = 2;
  public static Integer DEVICE_EVENT_ACTIVITY = 3;
  public static Integer DEVICE_EVENT_XSNS = 4;
  public static Integer DEVICE_EVENT_DCON = 5;
  public static Integer DEVICE_EVENT_POWER_OUTAGE = 6;
  public static Integer DEVICE_EVENT_WORKING_STATUS = 7;
  public static Map<Integer, Integer> DEVICE_EVENT_ALARM_GROUP = new HashMap<Integer, Integer>() {{
    put(AssetStatusConstants.DEVICE_CONN_ALARM_TYPE, DEVICE_EVENT_DCON);
    put(AssetStatusConstants.XSNS_ALARM_TYPE, DEVICE_EVENT_XSNS);
    put(AssetStatusConstants.BATTERY_ALARM_TYPE, DEVICE_EVENT_BATTERY);
    put(AssetStatusConstants.ACTIVITY_ALARM_TYPE, DEVICE_EVENT_ACTIVITY);
    put(AssetStatusConstants.POWER_OUTAGE_ALARM_TYPE, DEVICE_EVENT_POWER_OUTAGE);
  }};

  public static Map<String, Integer> DEVICE_EVENT_STATUS_GROUP = new HashMap<String, Integer>() {{
    put(AssetStatusConstants.ACTIVITY_STATUS_KEY, DEVICE_EVENT_ACTIVITY);
    put(AssetStatusConstants.WORKING_STATUS_KEY, DEVICE_EVENT_WORKING_STATUS);
    put(AssetStatusConstants.TEMP_STATUS_KEY, DEVICE_EVENT_TEMP);
    put(AssetStatusConstants.XSNS_ALARM_STATUS_KEY, DEVICE_EVENT_XSNS);
    put(AssetStatusConstants.BATTERY_ALARM_STATUS_KEY, DEVICE_EVENT_BATTERY);
    put(AssetStatusConstants.DEVICE_CONN_ALARM_STATUS_KEY, DEVICE_EVENT_DCON);
    put(AssetStatusConstants.POWER_OUTAGE_STATUS_KEY, DEVICE_EVENT_POWER_OUTAGE);

  }};

  public static String TMP_MIN = "min";
  public static String TMP_MAX = "max";

  public List<DeviceEvent> data;

  public DeviceEventPushModel() {
    data = new ArrayList<>(1);
  }

  public DeviceEventPushModel(final DeviceEvent deviceEvent) {
    data = new ArrayList<DeviceEvent>() {{
      add(deviceEvent);
    }};
  }

  @Override
  public String toString() {
    return "DeviceEventPushModel{" +
        "data=" + data +
        '}';
  }

  public static class DeviceEvent {
    public String vId;

    public String dId;

    public Integer st;

    public Integer type;

    public Integer time;

    public Integer aSt;

    public double tmp;

    public String sId;

    public Integer mpId;

    public Map<String, String> attrs = new HashMap<>(1);

    @Override
    public String toString() {
      return "DeviceEvent{" +
          "vId='" + vId + '\'' +
          ", dId='" + dId + '\'' +
          ", st=" + st +
          ", type=" + type +
          ", time=" + time +
          ", aSt=" + aSt +
          ", tmp=" + tmp +
          ", sId='" + sId + '\'' +
          ", mpId=" + mpId +
          ", attrs=" + attrs +
          '}';
    }
  }
}

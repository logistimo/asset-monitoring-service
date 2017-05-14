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

package com.logistimo.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kaniyarasu on 19/11/15.
 */
public interface AssetStatusConstants {
    Integer STATUS_OK = 0;
    //Temperature status constants
    String TEMP_STATUS_KEY = "tsk";
    Integer TEMP_STATUS_UNKNOWN = -1;
    Integer TEMP_STATUS_NORMAL = STATUS_OK;
    Integer TEMP_STATUS_EXCURSION = 1;
    Integer TEMP_STATUS_WARNING = 2;
    Integer TEMP_STATUS_ALARM = 3;

    //Temperature status abnormality, whether low, high
    Integer TEMP_ABNORMAL_STATUS_NORMAL = STATUS_OK;
    Integer TEMP_ABNORMAL_STATUS_LOW = 1;
    Integer TEMP_ABNORMAL_STATUS_HIGH = 2;

    //Device alarm status constants
    String DEVICE_ALARM_STATUS_KEY = "dsk";
    Integer DEVICE_ALARM_NORMAL = STATUS_OK;
    Integer DEVICE_ALARM_ABNORMAL = 1;

    //Battery alarm status constants
    String BATTERY_ALARM_STATUS_KEY = "dsk_2";
    Integer BATTERY_ALARM_NORMAL = STATUS_OK;
    Integer BATTERY_ALARM_WARNING = 1;
    Integer BATTERY_ALARM_ALARM = 2;
    Integer BATTERY_ALARM_CHARGING = 3;

    //Device connection status constants
    String DEVICE_CONN_ALARM_STATUS_KEY = "dsk_0";
    Integer DEVICE_CONN_ALARM_NORMAL = STATUS_OK;
    Integer DEVICE_CONN_ALARM_ALARM = 1;

    //External sensor connection status constants
    String XSNS_ALARM_STATUS_KEY = "dsk_1";
    Integer XSNS_ALARM_NORMAL = STATUS_OK;
    Integer XSNS_ALARM_ALARM = 1;

    //Activity status constants
    String ACTIVITY_STATUS_KEY = "dsk_4";
    Integer ACTIVITY_STATUS_OK = STATUS_OK;
    Integer ACTIVITY_STATUS_INACT = 1;

    //Power outage status constants
    String POWER_OUTAGE_STATUS_KEY = "dsk_5";
    Integer POWER_OUTAGE_STATUS_OK = STATUS_OK;
    Integer POWER_OUTAGE_STATUS_NA = 1;

    //Configuration status constants
    String CONFIG_STATUS_KEY = "csk";
    Integer CONFIG_STATUS_NOT_CONFIGURED = STATUS_OK;
    Integer CONFIG_STATUS_PULLED = 1;
    Integer CONFIG_STATUS_PUSHED = 2;

    //Working status constants
    String WORKING_STATUS_KEY = "wsk";
    Integer WORKING_STATUS_WORKING = 0;
    Integer WORKING_STATUS_NOTWORKING = 1;
    Integer WORKING_STATUS_REPAIR = 2;

    //List of all status key
    List<String> DEVICE_STATUS_KEYS = new ArrayList<String>(){{
        add(TEMP_STATUS_KEY);
        add(DEVICE_ALARM_STATUS_KEY);
        add(BATTERY_ALARM_STATUS_KEY);
        add(DEVICE_CONN_ALARM_STATUS_KEY);
        add(XSNS_ALARM_STATUS_KEY);
        add(ACTIVITY_STATUS_KEY);
        add(CONFIG_STATUS_KEY);
        add(WORKING_STATUS_KEY);
        add(POWER_OUTAGE_STATUS_KEY);
    }};

    //List of monitored status key
    List<String> MONITORING_ASSET_STATUS_KEYS = new ArrayList<String>(){{
        add(TEMP_STATUS_KEY);
        add(BATTERY_ALARM_STATUS_KEY);
        add(DEVICE_CONN_ALARM_STATUS_KEY);
        add(XSNS_ALARM_STATUS_KEY);
        add(ACTIVITY_STATUS_KEY);
        add(POWER_OUTAGE_STATUS_KEY);
    }};

    //List of monitored asset status key
    List<String> MONITORED_DEVICE_STATUS_KEYS = new ArrayList<String>(){{
        add(WORKING_STATUS_KEY);
        add(TEMP_STATUS_KEY);
        add(BATTERY_ALARM_STATUS_KEY);
        add(DEVICE_CONN_ALARM_STATUS_KEY);
        add(XSNS_ALARM_STATUS_KEY);
        add(ACTIVITY_STATUS_KEY);
        add(POWER_OUTAGE_STATUS_KEY);
    }};

    int BATTERY_ALARM_TYPE = 2;
    int DEVICE_CONN_ALARM_TYPE = 0;
    int XSNS_ALARM_TYPE = 1;
    int ACTIVITY_ALARM_TYPE = 4;
    int POWER_OUTAGE_ALARM_TYPE = 5;

    //List of device alarm status key
    List<String> DEVICE_ALARM_STATUS_KEYS = new ArrayList<String>(){{
        add(BATTERY_ALARM_STATUS_KEY);
        add(DEVICE_CONN_ALARM_STATUS_KEY);
        add(XSNS_ALARM_STATUS_KEY);
        add(ACTIVITY_STATUS_KEY);
        add(POWER_OUTAGE_STATUS_KEY);
    }};

    List<String> DEVICE_ALARM_STATUS_KEYS_STRING = new ArrayList<String>(){{
        add("'" + BATTERY_ALARM_STATUS_KEY + "'");
        add("'" + DEVICE_CONN_ALARM_STATUS_KEY + "'");
        add("'" + XSNS_ALARM_STATUS_KEY + "'");
        add("'" + ACTIVITY_STATUS_KEY + "'");
        add("'" + POWER_OUTAGE_STATUS_KEY + "'");
    }};

    List<String> ASSET_SENSOR_STATUS_KEYS = new ArrayList<String>(){{
        add(XSNS_ALARM_STATUS_KEY);
        add(ACTIVITY_STATUS_KEY);
        add(TEMP_STATUS_KEY);
    }};

    List<String> DEVICE_STATUS_PROPAGATION_KEYS = new ArrayList<String>(){{
        add(BATTERY_ALARM_STATUS_KEY);
        add(DEVICE_CONN_ALARM_STATUS_KEY);
        add(POWER_OUTAGE_STATUS_KEY);
    }};

    String DEVICE_ALARM_STATUS_KEYS_CSV = StringUtils.join(DEVICE_ALARM_STATUS_KEYS_STRING, LogistimoConstant.COMMA);

    //List of device alarm status key
    Map<Integer, String> DEVICE_ALARM_STATUS_KEYS_MAP = new HashMap<Integer, String>(){{
        put(BATTERY_ALARM_TYPE, BATTERY_ALARM_STATUS_KEY);
        put(DEVICE_CONN_ALARM_TYPE, DEVICE_CONN_ALARM_STATUS_KEY);
        put(XSNS_ALARM_TYPE, XSNS_ALARM_STATUS_KEY);
        put(ACTIVITY_ALARM_TYPE, ACTIVITY_STATUS_KEY);
        put(POWER_OUTAGE_ALARM_TYPE, POWER_OUTAGE_STATUS_KEY);
    }};
    Integer GPRS = 0;
    Integer SMS = 1;
}

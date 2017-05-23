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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kaniyarasu on 28/10/15.
 */
public interface DeviceMetaAppendix {
  //Locale meta data
  String LOCALE_COUNTRYCODE = "locale.cn";
  String LOCALE_TIMEZONE = "locale.tz";

  List<String> LOCALE_GROUP = new ArrayList<String>() {{
    add(LOCALE_COUNTRYCODE);
    add(LOCALE_TIMEZONE);
  }};

  //GSM meta data
  String GSM_SIM_PHN_NUMBER = "gsm.sim.phn";
  String GSM_SIM_SIMID = "gsm.sim.sid";
  String GSM_SIM_NETWORK_PROVIDER = "gsm.sim.np";
  String GSM_ALTSIM_PHN_NUMBER = "gsm.altSim.phn";
  String GSM_ALTSIM_SIMID = "gsm.altSim.sid";
  String GSM_ALTSIM_NETWORK_PROVIDER = "gsm.altSim.np";

  List<String> GSM_GROUP = new ArrayList<String>() {{
    add(GSM_ALTSIM_PHN_NUMBER);
    add(GSM_ALTSIM_SIMID);
    add(GSM_ALTSIM_NETWORK_PROVIDER);
    add(GSM_SIM_PHN_NUMBER);
    add(GSM_SIM_SIMID);
    add(GSM_SIM_NETWORK_PROVIDER);
  }};

  //Device details meta data
  String DEV_MODEL = "dev.mdl";
  String DEV_MODEL_FROM_DEVICE = "dev.mdlD";
  String DEV_DVR = "dev.dVr";
  String DEV_MVR = "dev.mVr";
  String DEV_IMEI = "dev.imei";

  List<String> DEV_GROUP = new ArrayList<String>() {{
    add(DEV_DVR);
    add(DEV_IMEI);
    add(DEV_MODEL);
    add(DEV_MVR);
  }};

  //Device temperature meta
  String TMP_MIN = "tmp.min";
  String TMP_MAX = "tmp.max";
  List<String> TMP_GROUP = new ArrayList<String>() {{
    add(TMP_MIN);
    add(TMP_MAX);
  }};

  //Device intervals meta
  String INT_SINT = "int.sint";
  String INT_PINT = "int.pint";
  String INACTIVE_PUSH_INTERVAL_COUNT = "int.cnt";
  Integer INACTIVE_PUSH_INTERVAL_COUNT_DEFAULT_VALUE = 3;
  List<String> INT_GROUP = new ArrayList<String>() {{
    add(INT_SINT);
    add(INT_PINT);
    add(INACTIVE_PUSH_INTERVAL_COUNT);
  }};

  //temperature alarm configuration
  String ALARM_HIGH_TEMP = "alarm.high.temp";
  String ALARM_LOW_TEMP = "alarm.low.temp";
  String ALARM_HIGH_DUR = "alarm.high.dur";
  String ALARM_LOW_DUR = "alarm.low.dur";
  String WARN_HIGH_TEMP = "warn.high.temp";
  String WARN_LOW_TEMP = "warn.low.temp";
  String WARN_HIGH_DUR = "warn.high.dur";
  String WARN_LOW_DUR = "warn.low.dur";

  List<String> TEMP_ALARM_GROUP = new ArrayList<String>() {{
    add(ALARM_HIGH_DUR);
    add(ALARM_HIGH_TEMP);
    add(ALARM_LOW_DUR);
    add(ALARM_LOW_TEMP);
  }};

  List<String> TEMP_WARN_GROUP = new ArrayList<String>() {{
    add(WARN_HIGH_DUR);
    add(WARN_HIGH_TEMP);
    add(WARN_LOW_DUR);
    add(WARN_LOW_TEMP);
  }};
}

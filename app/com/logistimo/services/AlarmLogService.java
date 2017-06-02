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

package com.logistimo.services;

import com.logistimo.db.AlarmLog;
import com.logistimo.db.Device;

import java.util.List;

import javax.persistence.NoResultException;

/**
 * Created by charan on 01/06/17.
 */
public class AlarmLogService extends ServiceImpl {


  public AlarmLog getOpenAlarmLogForDeviceAndSensorId(Device device, String sensorId,
                                                      Integer alarmType,
                                                      Integer deviceAlarmType) {
    return fixMultipleOpenAlarmLogs(
        AlarmLog.getAlarmLogForDeviceAndSensorId(device, sensorId, alarmType, deviceAlarmType));
  }

  public AlarmLog getOpenParentAlarmLog(Device device, Integer alarmType,
                                        Integer deviceAlarmType){
    return fixMultipleOpenAlarmLogs(AlarmLog.getParentAlarmLog(device, alarmType, deviceAlarmType));
  }

  public AlarmLog getOpenAlarmLogForAlarmTypeBySensorId(Device device, Integer alarmType,
                                                        Integer deviceAlarmType,
                                                        String sensorId) {
    return fixMultipleOpenAlarmLogs(AlarmLog.getOpenAlarmLogs(device, alarmType,
        deviceAlarmType, sensorId));
  }

  public AlarmLog getOpenAlarmLogForAlarmType(Device device, Integer alarmType,
                                              Integer deviceAlarmType) {
    return fixMultipleOpenAlarmLogs(AlarmLog.getOpenAlarmLogsForAlarmType(device, alarmType,
        deviceAlarmType));
  }

  public AlarmLog getOpenAlarmLogForDeviceAndMPId(Device device, Integer mpId, Integer alarmType,
                                                  Integer deviceAlarmType) {
    return fixMultipleOpenAlarmLogs(
        AlarmLog.getAlarmLogForDeviceAndMPId(device, mpId, alarmType, deviceAlarmType));
  }


  /**
   * Closes old events which are still open. Expects the logs to be in sorted order of start_time.
   * @param alarmLogs
   * @return final alarm log
   */
  protected AlarmLog fixMultipleOpenAlarmLogs(List<AlarmLog> alarmLogs) {
    if(alarmLogs == null || alarmLogs.isEmpty()){
      throw new NoResultException();
    }else if(alarmLogs.size()==1){
      return alarmLogs.get(0);
    }

    for(int i=0;i<alarmLogs.size()-1;i++){
      AlarmLog nextAlarmLog = AlarmLog.getNextAlarmLog(alarmLogs.get(i));
      alarmLogs.get(i).endTime = nextAlarmLog.startTime;
      alarmLogs.get(i).update();
    }
    return alarmLogs.get(alarmLogs.size()-1);
  }

}

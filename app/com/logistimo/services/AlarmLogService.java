package com.logistimo.services;

import com.logistimo.db.AlarmLog;
import com.logistimo.db.Device;

import java.util.List;

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
    if(alarmLogs.isEmpty()){
      return null;
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

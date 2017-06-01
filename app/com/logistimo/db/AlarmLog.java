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

package com.logistimo.db;

import com.logistimo.services.AlarmService;
import com.logistimo.utils.LogistimoConstant;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.JPA;

/**
 * Created by kaniyarasu on 25/08/15.
 */
@Entity
@Table(name = "alarm_log")
public class AlarmLog {
  public static final int TEMP_ALARM = 0;
  public static final int DEVICE_ALARM = 1;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "alarm_type")
  public Integer alarmType;

  @Column(name = "temperature")
  public Double temperature;

  @Column(name = "temperature_type")
  public Integer temperatureType;

  @Column(name = "temperature_abnormal_type")
  public Integer temperatureAbnormalType;

  @Column(name = "device_alarm_type")
  public Integer deviceAlarmType;

  @Column(name = "device_alarm_status")
  public Integer deviceAlarmStatus;

  @Column(name = "device_firmware_error_code")
  public String deviceFirmwareErrorCode;

  @Column(name = "monitoring_position_id")
  public Integer monitoringPositionId;

  @Column(name = "sensor_id")
  public String sensorId;

  @Column(name = "start_time")
  public Integer startTime;

  @Column(name = "end_time")
  public Integer endTime;

  @Column(name = "updated_on")
  public Date updatedOn;

  @ManyToOne
  public Device device;

  public AlarmLog() {
  }

  public AlarmLog(Integer alarmType, Integer startTime) {
    this.alarmType = alarmType;
    this.startTime = startTime;
  }

  public static List<AlarmLog> getAlarmsForDevice(Device device, int alarmType, int startingOffset,
                                                  int maxResult) {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT * FROM alarm_log WHERE device_id = ").append(device.id);
    sb.append(" AND alarm_type = ").append(alarmType);

    if (AlarmLog.DEVICE_ALARM == alarmType) {
      sb.append(" AND ((device_alarm_type IN(")
          .append(AlarmService.SENSOR_CONNECTION_ALARM)
          .append(LogistimoConstant.COMMA)
          .append(AlarmService.DEVICE_NODATA_ALARM)
          .append(") and sensor_id IS NOT NULL) OR device_alarm_type NOT IN(")
          .append(AlarmService.SENSOR_CONNECTION_ALARM)
          .append(LogistimoConstant.COMMA)
          .append(AlarmService.DEVICE_NODATA_ALARM)
          .append("))");
    } else {
      sb.append(" AND monitoring_position_id IS NOT NULL");
    }

    sb.append(" ORDER BY start_time desc");

    return JPA.em().createNativeQuery(sb.toString(), AlarmLog.class)
        .setFirstResult(startingOffset)
        .setMaxResults(maxResult)
        .getResultList();
  }

  public static List<AlarmLog> getAlarmLogForDeviceAndSensorId(Device device,
                                                               String sensorId, Integer alarmType,
                                                               Integer deviceAlarmType) {
    if (null == deviceAlarmType) {
      return JPA.em().createQuery(
          "from AlarmLog where device = ?1 and sensor_id = ?2 and end_time is NULL and "
              + "alarm_type = ?3 and device_alarm_type is NULL order by start_time",
          AlarmLog.class)
          .setParameter(1, device)
          .setParameter(2, sensorId)
          .setParameter(3, alarmType)
          .getResultList();
    }

    return JPA.em().createQuery(
        "from AlarmLog where device = ?1 and sensor_id = ?2 and end_time is NULL and "
            + "alarm_type = ?3 and device_alarm_type = ?4 order by start_time",
        AlarmLog.class)
        .setParameter(1, device)
        .setParameter(2, sensorId)
        .setParameter(3, alarmType)
        .setParameter(4, deviceAlarmType)
        .getResultList();
  }

  public static List<AlarmLog> getAlarmLogForDeviceAndMPId(Device device,
                                                           Integer mpId, Integer alarmType,
                                                               Integer deviceAlarmType) {
    if (null == deviceAlarmType) {
      return JPA.em().createQuery(
          "from AlarmLog where device = ?1 and monitoring_position_id = ?2 and end_time is NULL and "
              + "alarm_type = ?3 and device_alarm_type is NULL order by start_time",
          AlarmLog.class)
          .setParameter(1, device)
          .setParameter(2, mpId)
          .setParameter(3, alarmType)
          .getResultList();
    }

    return JPA.em().createQuery(
        "from AlarmLog where device = ?1 and monitoring_position_id = ?2 and end_time is NULL and "
            + "alarm_type = ?3 and device_alarm_type = ?4 order by start_time",
        AlarmLog.class)
        .setParameter(1, device)
        .setParameter(2, mpId)
        .setParameter(3, alarmType)
        .setParameter(4, deviceAlarmType)
        .getResultList();
  }

  public static List<AlarmLog> getOpenAlarmLogsForAlarmType(Device device, Integer alarmType,
                                                            Integer deviceAlarmType) {
    return JPA.em().createQuery(
        "from AlarmLog where device = ?1 and alarm_type = ?2 and device_alarm_type = ?3 and "
            + "end_time IS NULL and sensor_id is NULL and monitoring_position_id is NULL "
            + "order by start_time",
        AlarmLog.class)
        .setParameter(1, device)
        .setParameter(2, alarmType)
        .setParameter(3, deviceAlarmType)
        .getResultList();
  }

  public static List<AlarmLog> getOpenAlarmLogs(Device device) {
    return JPA.em()
        .createQuery("from AlarmLog where device = ?1 and end_time IS NULL",
            AlarmLog.class)
        .setParameter(1, device)
        .getResultList();
  }

  public static List<AlarmLog> getOpenAlarmLogs(Device device, Integer alarmType,
                                                Integer deviceAlarmType,
                                                String sensorId) {
    return JPA.em().createQuery(
        "from AlarmLog where device = ?1 and alarm_type = ?2 and device_alarm_type = ?3 "
            + "and sensor_id = ?4 and end_time IS NULL order by start_time",
        AlarmLog.class)
        .setParameter(1, device)
        .setParameter(2, alarmType)
        .setParameter(3, deviceAlarmType)
        .setParameter(4, sensorId)
        .getResultList();
  }

  public static List<AlarmLog> getParentAlarmLog(Device device, Integer alarmType,
                                                 Integer deviceAlarmType) {
    if (null == deviceAlarmType) {
      return JPA.em().createQuery(
          "from AlarmLog where device = ?1 and alarm_type = ?2 and device_alarm_type is NULL and "
              + " sensor_id IS NULL and monitoring_position_id IS NULL "
              + "and end_time is NULL order by start_time",
          AlarmLog.class)
          .setParameter(1, device)
          .setParameter(2, alarmType)
          .getResultList();
    }
    return JPA.em().createQuery(
        "from AlarmLog where device = ?1 and alarm_type = ?2 and device_alarm_type = ?3 and "
            + " sensor_id IS NULL and monitoring_position_id IS NULL and "
            + "end_time is NULL order by start_time",
        AlarmLog.class)
        .setParameter(1, device)
        .setParameter(2, alarmType)
        .setParameter(3, deviceAlarmType)
        .getResultList();
  }

  public static int getAlarmCountForDevice(Device device, int alarmType) {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT count(1) FROM alarm_log WHERE device_id = ").append(device.id);
    sb.append(" AND alarm_type = ").append(alarmType);

    if (AlarmLog.DEVICE_ALARM == alarmType) {
      sb.append(" AND ((device_alarm_type IN(")
          .append(AlarmService.SENSOR_CONNECTION_ALARM)
          .append(LogistimoConstant.COMMA)
          .append(AlarmService.DEVICE_NODATA_ALARM)
          .append(") and sensor_id IS NOT NULL) OR device_alarm_type NOT IN(")
          .append(AlarmService.SENSOR_CONNECTION_ALARM)
          .append(LogistimoConstant.COMMA)
          .append(AlarmService.DEVICE_NODATA_ALARM)
          .append("))");
    } else {
      sb.append(" AND monitoring_position_id IS NOT NULL");
    }

    return new BigInteger(JPA.em()
        .createNativeQuery(sb.toString())
        .getSingleResult().toString()).intValue();
  }

  public void save() {
    this.updatedOn = new Date();
    JPA.em().persist(this);
  }

  public void update() {
    this.updatedOn = new Date();
    JPA.em().merge(this);
  }

  public void delete() {
    JPA.em().remove(this);
  }

  public static AlarmLog getNextAlarmLog(AlarmLog alarmLog) {
    StringBuilder query = new StringBuilder("select * from alarm_log where device_id = ")
        .append(alarmLog.device.id);

    if(alarmLog.sensorId != null){
      query.append(" and sensor_id = '").append(alarmLog.sensorId).append("'");
    }else{
      query.append(" and sensor_id is NULL");
    }

    if(alarmLog.monitoringPositionId != null){
      query.append(" and monitoring_position_id = ").append(alarmLog.monitoringPositionId);
    }else {
      query.append(" and monitoring_position_id is NULL");
    }

    if(alarmLog.deviceAlarmType != null){
      query.append(" and device_alarm_type = ").append(alarmLog.deviceAlarmType);
    }else {
      query.append(" and device_alarm_type is NULL");
    }

    query.append(" and alarm_type = ").append(alarmLog.alarmType)
        .append(" and start_time >= ").append(alarmLog.startTime)
        .append(" and id <> ").append(alarmLog.id)
        .append(" order by start_time limit 1");

    return (AlarmLog) JPA.em()
        .createNativeQuery(query.toString(), AlarmLog.class)
        .getSingleResult();

  }
}

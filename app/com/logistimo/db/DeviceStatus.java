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

import com.logistimo.utils.AssetStatusConstants;

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
 * Created by kaniyarasu on 19/11/15.
 */
@Entity
@Table(name = "device_status")
public class DeviceStatus {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "status_key")
  public String statusKey;

  @Column(name = "status")
  public Integer status = AssetStatusConstants.STATUS_OK;

  @Column(name = "status_ut")
  public Integer statusUpdatedTime;

  @Column(name = "location_id")
  public Integer locationId;

  @Column(name = "sensor_id")
  public String sensorId;

  @Column(name = "temperature")
  public Double temperature = 0d;

  @Column(name = "temperature_ut")
  public Integer temperatureUpdatedTime = 0;

  @Column(name = "temperature_abnormal_status")
  public Integer temperatureAbnormalStatus = AssetStatusConstants.STATUS_OK;

  @ManyToOne
  public Device device;

  public static List<DeviceStatus> getDeviceStatus(Device device) {
    return JPA.em().createQuery(
        "from DeviceStatus where device = ?1 order by status_key, location_id, sensor_id",
        DeviceStatus.class)
        .setParameter(1, device)
        .getResultList();
  }

  public static List<DeviceStatus> getDeviceStatus(List<Device> deviceList) {
    return JPA.em().createQuery("from DeviceStatus where device in :inclList", DeviceStatus.class)
        .setParameter("inclList", deviceList)
        .getResultList();
  }

  public static DeviceStatus getDeviceStatus(Device device, String key) {
    return JPA.em().createQuery(
        "from DeviceStatus where device = ?1 and statusKey = ?2 and locationId IS NULL and sensorId IS NULL",
        DeviceStatus.class)
        .setParameter(1, device)
        .setParameter(2, key)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static List<DeviceStatus> getDeviceStatuses(Device device, String key) {
    return JPA.em().createQuery("from DeviceStatus where device = ?1 and statusKey = ?2",
        DeviceStatus.class)
        .setParameter(1, device)
        .setParameter(2, key)
        .getResultList();
  }

  public static DeviceStatus getDeviceStatus(Device device, String key, String sensorId) {
    return JPA.em()
        .createQuery("from DeviceStatus where device = ?1 and sensorId = ?3 and statusKey = ?2",
            DeviceStatus.class)
        .setParameter(1, device)
        .setParameter(2, key)
        .setParameter(3, sensorId)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static DeviceStatus getDeviceStatus(Device device, String key,
                                             Integer monitoringPositionId) {
    return JPA.em()
        .createQuery("from DeviceStatus where device = ?1 and locationId = ?2 and statusKey = ?3",
            DeviceStatus.class)
        .setParameter(1, device)
        .setParameter(2, monitoringPositionId)
        .setParameter(3, key)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static List<DeviceStatus> getDeviceStatus(List<Device> deviceList, String key) {
    return JPA.em().createQuery("from DeviceStatus where device in :inclList and statusKey = ?2",
        DeviceStatus.class)
        .setParameter("inclList", deviceList)
        .setParameter(2, key)
        .getResultList();
  }

  public void save() {
    JPA.em().persist(this);
  }

  public void update() {
    JPA.em().merge(this);
  }

  public void delete() {
    JPA.em().remove(this);
  }

  public void copyStatus(DeviceStatus deviceStatus) {
    this.status = deviceStatus.status;
    this.statusUpdatedTime = deviceStatus.statusUpdatedTime;
    this.temperature = deviceStatus.temperature;
    this.temperatureAbnormalStatus = deviceStatus.temperatureAbnormalStatus;
    this.temperatureUpdatedTime = deviceStatus.temperatureUpdatedTime;
  }
}

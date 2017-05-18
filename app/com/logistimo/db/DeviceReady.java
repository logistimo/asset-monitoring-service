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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.JPA;

@Entity
@Table(name = "device_ready")
public class DeviceReady {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "transmitter_id", nullable = false)
  public String transmitterId;

  @Column(name = "device_model")
  public String deviceModel;

  @Column(name = "device_sensor_firmware_version")
  public String deviceSensorFirmwareVersion;

  @Column(name = "device_gsm_firmware_version")
  public String deviceGsmFirmwareVersion;

  @Column(name = "device_imei")
  public String deviceImei;

  @Column(name = "sim_phone")
  public String simNumber;

  @Column(name = "sim_id")
  public String simId;

  @ManyToOne
  public Device device;

  public DeviceReady() {
  }

  public static DeviceReady getDeviceStatusByDevice(Device device) {
    return JPA.em()
        .createQuery("from DeviceReady where device = ?1", DeviceReady.class)
        .setParameter(1, device)
        .getSingleResult();
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

}

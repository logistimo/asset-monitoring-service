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

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

import javax.persistence.*;

import play.db.jpa.JPA;

/**
 * Created by naveensnair on 15/03/17.
 */
@Entity
@Table(name = "device_status_log")
public class DeviceStatusLog {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "device_id")
  public Long deviceId;

  @Column(name = "status_key")
  public String statusKey;

  @Column(name = "status")
  public Integer status = AssetStatusConstants.STATUS_OK;

  @Column(name = "status_ut")
  public Integer statusUpdatedTime;

  @Column(name = "start_time")
  public Integer startTime;

  @Column(name = "end_time")
  public Integer endTime;

  @Column(name = "updated_by")
  public String updatedBy;

  @Column(name = "previous_status")
  public Integer previousStatus;

  @Column(name = "next_status")
  public Integer nextStatus;

  @Column(name = "updated_on")
  public Date updatedOn;

  public DeviceStatusLog(){};
  public DeviceStatusLog(Long deviceId){
    this.deviceId = deviceId;
    this.statusKey = AssetStatusConstants.WORKING_STATUS_KEY;
  }

  public static DeviceStatusLog getDeviceStatusForGivenStartTime(Long deviceId, String statusKey, Integer eventStartTime) {
    if(deviceId != null && StringUtils.isNotEmpty(statusKey) && eventStartTime != null) {
      return JPA.em().createQuery("from DeviceStatusLog where device_id = ?1 and status_key = ?2 and start_time = ?3", DeviceStatusLog.class)
          .setParameter(1, deviceId)
          .setParameter(2, statusKey)
          .setParameter(3, eventStartTime)
          .getSingleResult();
    }
    return null;
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
}

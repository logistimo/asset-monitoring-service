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

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import play.db.jpa.JPA;

@Entity
@Table(name = "devices")
public class Device {
  public static int TEMP_NORMAL = 0;
  public static int TEMP_EXCURSION = 1;
  public static int TEMP_WARNING = 2;
  public static int TEMP_ALARM = 3;

  public static int DEVICE_NORMAL = 0;
  public static int DEVICE_ABNORMAL = 1;

  public static int WORKING = 0;
  public static int REPAIR = 1;
  public static int FAULTY = 2;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "deviceid", nullable = false)
  public String deviceId;

  @Column(name = "vendorid", nullable = false)
  public String vendorId;

  @Column(name = "transmitterId", nullable = false)
  public String transmitterId;

  @Column(name = "uniquehash", nullable = false, unique = true)
  public String uniqueHash;

  @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  @JoinTable(name = "devices_tags", joinColumns = {
      @JoinColumn(name = "devices_id")}, inverseJoinColumns = {@JoinColumn(name = "tags_id")})
  @Sort(type = SortType.NATURAL)
  public SortedSet<Tag> tags;

  @Column(name = "statusUpdatedTime")
  public Long statusUpdatedTime;

  @Column(name = "createdOn", updatable = false)
  public Date createdOn;

  @Column(name = "createdBy", updatable = false)
  public String createdBy;

  @Column(name = "updatedOn")
  public Date updatedOn;

  @Column(name = "updatedBy")
  public String updatedBy;

  @Column(name = "imgUrls")
  public String imgUrls;

  @Column(name = "vendorName")
  public String vendorName;

  @Column(name = "locationId")
  public String locationId;

  @Column(name = "firstTempTime")
  public Integer firstTempTime;

  @ManyToOne
  public AssetType assetType;

  public Device() {
    tags = new TreeSet<>();
  }

  public static Device findDevice(String vendorId, String deviceId) {
    return JPA.em()
        .createQuery("from Device where deviceId=?1 and vendorId=?2", Device.class)
        .setParameter(1, deviceId)
        .setParameter(2, vendorId)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static Device findById(long id) {
    return JPA.em().createQuery("from Device where id=?1", Device.class)
        .setParameter(1, id)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static List<Device> deviceTaggedWith(Tag tag) {
    return JPA.em().createQuery("from Device where tags = ?1", Device.class)
        .setParameter(1, tag)
        .getResultList();
  }

  public static int getAbnormalReadingCountForDevices(List<Device> deviceList) {
    return new BigInteger(JPA.em()
        .createNativeQuery(
            "select count(1) from devices where id in :inclList and temperatureState = ?1")
        .setParameter("inclList", deviceList)
        .setParameter(1, TEMP_ALARM)
        .getSingleResult().toString()).intValue();
  }

  public static List<Device> getInactiveDevices() {
    return JPA.em().createQuery("from Device where activityState = ?1", Device.class)
        .setParameter(1, Device.DEVICE_ABNORMAL)
        .getResultList();
  }

  //Only for migration
  public static List<Device> getExcursionDevices() {
    return JPA.em().createQuery("from Device where temperatureState = ?1", Device.class)
        .setParameter(1, Device.TEMP_EXCURSION)
        .getResultList();
  }

  @SuppressWarnings("unchecked")
  public static List<Device> getActiveSensorDevices() {
    return JPA.em().createNativeQuery("select * from devices where assetType_id = 4 and id in" +
        "(select device_id from device_status where status_key = 'dsk_4' and status = 0) order by vendorId",
        Device.class)
        .getResultList();
  }

  public static int getInactiveCountForDevices(List<Device> deviceList) {
    return new BigInteger(JPA.em()
        .createNativeQuery(
            "select count(1) from devices where id in :inclList and activityState = ?1")
        .setParameter("inclList", deviceList)
        .setParameter(1, Device.DEVICE_ABNORMAL)
        .getSingleResult().toString()).intValue();
  }

  @PrePersist
  @PreUpdate
  public void updateCreatedOn() {
    if (this.createdOn == null) {
      this.createdOn = new Date();
    }
    this.updatedOn = new Date();
  }

  public void hash() {
    this.uniqueHash = DigestUtils.md5Hex(deviceId + vendorId);
  }

  @Override
  public String toString() {
    return deviceId;
  }

  public void save() {
    //Creating unique hash for device, which is combination of vendor id and device id.
    hash();
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

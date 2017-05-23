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

import java.util.ArrayList;
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
 * Created by kaniyarasu on 23/09/15.
 */
@Entity
@Table(name = "device_power_transition")
public class DevicePowerTransition {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "state")
  public Integer state;

  @Column(name = "transition_time")
  public Integer time;

  @ManyToOne
  public Device device;

  public static List<DevicePowerTransition> getDevicePowerTransition(Device device, Integer from,
                                                                     Integer to) {
    List<DevicePowerTransition> devicePowerTransitionList = new ArrayList<>(1);

    //Getting transition within requested range
    devicePowerTransitionList.addAll(JPA.em()
        .createQuery(
            "from DevicePowerTransition where device = ?1 and time > ?2 and time <= ?3 order by time desc",
            DevicePowerTransition.class)
        .setParameter(1, device)
        .setParameter(2, from)
        .setParameter(3, to)
        .getResultList());

    //Getting older state for continuity
    DevicePowerTransition
        devicePowerTransition =
        JPA.em().createQuery(
            "from DevicePowerTransition where device = ?1 and time <= ?2 order by time desc",
            DevicePowerTransition.class)
            .setParameter(1, device)
            .setParameter(2, from)
            .setMaxResults(1)
            .getSingleResult();
    devicePowerTransition.time = from;
    devicePowerTransitionList.add(devicePowerTransition);

    return devicePowerTransitionList;
  }

  public static DevicePowerTransition getRecentDevicePowerTransition(Device device, Integer to) {
    return JPA.em().createQuery(
        "from DevicePowerTransition where device = ?1 and time <= ?2 order by time desc",
        DevicePowerTransition.class)
        .setParameter(1, device)
        .setParameter(2, to)
        .setMaxResults(1)
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

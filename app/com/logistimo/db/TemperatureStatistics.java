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
@Table(name = "temperature_statistics")
@Deprecated
public class TemperatureStatistics {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(nullable = false)
  public long dayOfComputation;
  public int numberOfAlerts;
  public Double meanTemperature;

  public int numberOfLowAlerts;
  public Double lowestTemperature;
  public int durationLow;
  public long firstLowAlertTime;
  public Double lowAlertAmbientTemperatureLow;

  public int numberOfHighAlerts;
  public Double highestTemperature;
  public int durationHigh;
  public long firstHighAlertTime;
  public Double highAlertAmbientTemperature;

  public int numberOfSMSSent;
  public int numberOfInternetPushes;
  public int numberInternetPushFailures;

  public long firstSensorConnectionFailureTime;
  public int sensorConnectionFailureDuration;

  @ManyToOne
  public Device device;

  public void save() {
    JPA.em().persist(this);
  }

}

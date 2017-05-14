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

import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.List;

@Entity
@Table(name = "daily_stats")
public class DailyStatsDO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "day", nullable = false)
    public int day;

    @Column(name = "timezone_offset")
    public double timezoneOffset;

    @Column(name = "number_of_excursions")
    public int numberOfExcursions;

    @Column(name = "mean_temperature", nullable = false)
    public double meanTemperature;

    @Column(name = "min_temperature", nullable = false)
    public double minTemperature;

    @Column(name = "max_temperature", nullable = false)
    public double maxTemperature;

    @Column(name = "high_alert_status", nullable = false)
    public int highAlertStatus;

    @Column(name = "high_alert_nalarms")
    public int highAlertAlarms;

    @Column(name = "high_alert_time", nullable = false)
    public int highAlertTime;

    @Column(name = "high_alert_duration", nullable = false)
    public int highAlertDuration;

    @Column(name = "high_alert_ambient_temperature", nullable = false)
    public double highAlertAmbientTemperature;

    @Column(name = "high_alert_cnfms")
    public String highAlertCnfms;

    @Column(name = "high_alert_cnf")
    public boolean highAlertCnf;

    @Column(name = "low_alert_status", nullable = false)
    public int lowAlertStatus;

    @Column(name = "low_alert_nalarms")
    public int lowAlertAlarms;

    @Column(name = "low_alert_time", nullable = false)
    public int lowAlertTime;

    @Column(name = "low_alert_duration", nullable = false)
    public int lowAlertDuration;

    @Column(name = "low_alert_ambient_temperature", nullable = false)
    public double lowAlertAmbientTemperature;

    @Column(name = "low_alert_cnfms")
    public String lowAlertCnfms;

    @Column(name = "low_alert_cnf")
    public boolean lowAlertCnf;

    @Column(name = "external_sensor_status", nullable = false)
    public int externalSensorStatus;

    @Column(name = "external_sensor_nalarms")
    public int externalSensorAlarms;

    @Column(name = "external_sensor_duration", nullable = false)
    public int externalSensorDuration;

    @Column(name = "external_sensor_time", nullable = false)
    public int externalSensorTime;

    @Column(name = "device_connection_status", nullable = false)
    public int deviceConnectionStatus;

    @Column(name = "device_connection_alarms")
    public int deviceConnectionAlarms;

    @Column(name = "device_connection_duration", nullable = false)
    public int deviceConnectionDuration;

    @Column(name = "device_connection_time", nullable = false)
    public int deviceConnectionTime;

    @Column(name = "battery_status", nullable = false)
    public int batteryStatus;

    @Column(name = "battery_nalarms")
    public int batteryAlarms;

    @Column(name = "battery_time", nullable = false)
    public int batteryTime;

    @Column(name = "battery_actual_volt", nullable = false)
    public double batteryActualVolt;

    @Column(name = "battery_low_volt")
    public double batteryLowVolt;

    @Column(name = "battery_high_volt")
    public double batteryHighVolt;

    @Column(name = "battery_charging_time")
    public int batteryChargingTime;

    @Column(name = "battery_warning_dur")
    public int batteryWarningDuration;

    @Column(name = "battery_alarm_dur")
    public int batteryAlarmDuration;

    @Column(name = "power_available_time")
    public int powerAvailableTime;

    @Column(name = "number_of_sms_sent", nullable = false)
    public int numberOfSmsSent;

    @Column(name = "number_of_internet_pushes", nullable = false)
    public int numberOfInternetPushes;

    @Column(name = "number_of_internet_failures", nullable = false)
    public int numberOfInternetFailures;

    @Column(name = "available_disk_space")
    public Double availableDiskSpace;

    @Column(name = "number_of_temperature_cached")
    public Integer numberOfTempCached;

    @Column(name = "number_of_dvc_cached")
    public Integer numberOfDVCCached;

    @ManyToOne
    public Device device;

    public static List<DailyStatsDO> getDailyStats(Device device, int offset, int limit, int from, int to) {
        if (from > 0 && to > 0) {
            return JPA.em()
                    .createQuery("from DailyStatsDO where device = ?1 and day >= ?2 and day < ?3 order by day desc", DailyStatsDO.class)
                    .setParameter(1, device)
                    .setParameter(2, from)
                    .setParameter(3, to)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
        }
        return JPA.em()
                .createQuery("from DailyStatsDO where device = ?1 order by day desc", DailyStatsDO.class)
                .setParameter(1, device)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public static int getDailyStatsCount(Device device, int from, int to) {
        if (from > 0 && to > 0) {
            return new BigInteger(JPA.em()
                    .createNativeQuery("select  count(1) from daily_stats where device_id = ?1 and day > ?2 and day < ?3")
                    .setParameter(1, device)
                    .setParameter(2, from)
                    .setParameter(3, to)
                    .getSingleResult().toString()).intValue();
        }
        return new BigInteger(JPA.em()
                .createNativeQuery("select  count(1) from daily_stats where device_id = ?1")
                .setParameter(1, device)
                .getSingleResult().toString()).intValue();
    }

    public static DailyStatsDO getDailyStatsByDay(Device device, int day) {
        return JPA.em()
                .createQuery("from DailyStatsDO where device = ? and day = ?", DailyStatsDO.class)
                .setParameter(1, device)
                .setParameter(2, day)
                .getSingleResult();
    }

    public void save() {
        JPA.em().persist(this);
    }
}

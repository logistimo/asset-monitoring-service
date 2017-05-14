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
@Table(name = "temperature_readings")
public class TemperatureReading {
    public static Integer NORMAL = 0;
    public static Integer INCURSION = 1;
    public static Integer EXCURSION = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "timeofreading", nullable = false)
    public int timeOfReading;

    @Column(name = "type", nullable = false)
    public int type;

    @Column(name = "temperature", nullable = false)
    public double temperature;

    @ManyToOne
    public Device monitoredAsset;

    @Column(name = "location_id")
    public Integer monitoringPositionId;

    @ManyToOne
    public Device device;

    @Column(name = "power_availability")
    public Integer powerAvailability;

    @Column(name = "source")
    public Integer source;

    public static TemperatureReading getReading(Device device, int timeOfReading, int type, double temperature){
        return JPA.em().createQuery("from TemperatureReading  where device = ?1 and timeOfReading = ?2 and type = ?3 and temperature = ?4", TemperatureReading.class)
                .setParameter(1, device)
                .setParameter(2, timeOfReading)
                .setParameter(3, type)
                .setParameter(4, temperature)
                .setMaxResults(1)
                .getSingleResult();
    }

    public static List<TemperatureReading> getReadingsBetween(Device device, int startingOffset, int pageSize,
                                                              int from, int to) {

        if (from > 0 && to > 0) {
            return JPA.em()
                    .createQuery("from TemperatureReading where device = ?1 and timeOfReading >= ?2 and timeOfReading < ?3 order by timeOfReading desc", TemperatureReading.class)
                    .setParameter(1, device)
                    .setParameter(2, from)
                    .setParameter(3, to)
                    .setFirstResult(startingOffset)
                    .setMaxResults(pageSize)
                    .getResultList();
        } else {
            return JPA.em()
                    .createQuery("from TemperatureReading where device = ?1 order by timeOfReading desc", TemperatureReading.class)
                    .setParameter(1, device)
                    .setFirstResult(startingOffset)
                    .setMaxResults(pageSize)
                    .getResultList();
        }

    }

    public static List<TemperatureReading> getReadingsBetweenByMA(Device device, Integer mpId, int startingOffset, int pageSize,
                                                              int from, int to) {

        if (from > 0 && to > 0) {
            return JPA.em()
                    .createQuery("from TemperatureReading where monitoredAsset = ?1 and monitoringPositionId = ?2 and timeOfReading >= ?3 and timeOfReading < ?4 order by timeOfReading desc", TemperatureReading.class)
                    .setParameter(1, device)
                    .setParameter(2, mpId)
                    .setParameter(3, from)
                    .setParameter(4, to)
                    .setFirstResult(startingOffset)
                    .setMaxResults(pageSize)
                    .getResultList();
        } else {
            return JPA.em()
                    .createQuery("from TemperatureReading where monitoredAsset = ?1 and monitoringPositionId = ?2 order by timeOfReading desc", TemperatureReading.class)
                    .setParameter(1, device)
                    .setParameter(2, mpId)
                    .setFirstResult(startingOffset)
                    .setMaxResults(pageSize)
                    .getResultList();
        }

    }

    public static List<TemperatureReading> getReadingsForDevices(List<Device> deviceList, int pageNumber, int pageSize) {
        if (pageNumber == -1) {
            return JPA.em()
                    .createQuery("from TemperatureReading where device in :inclList order by timeOfReading desc", TemperatureReading.class)
                    .setParameter("inclList", deviceList)
                    .getResultList();
        }

        return JPA.em()
                .createQuery("from TemperatureReading where device in :inclList order by timeOfReading desc", TemperatureReading.class)
                .setParameter("inclList", deviceList)
                .setFirstResult(pageNumber)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public static int getReadingsCount(Device device) {
        return new BigInteger(JPA.em()
                .createNativeQuery("select count(1) from temperature_readings where device_id = ?1")
                .setParameter(1, device.id)
                .getSingleResult().toString()).intValue();
    }

    public static int getReadingsCountForDevices(List<Device> deviceList) {
        return new BigInteger(JPA.em()
                .createQuery("select count(*) from TemperatureReading where device in :inclList")
                .setParameter("inclList", deviceList)
                .getSingleResult().toString()).intValue();
    }

    public static int getReadingsBetweenCount(Device device, int from, int to) {
        return new BigInteger(JPA.em()
                .createNativeQuery("select count(1) from temperature_readings where device_id = ?1 and timeofreading >= ?2 and timeofreading < ?3")
                .setParameter(1, device.id)
                .setParameter(2, from)
                .setParameter(3, to)
                .getSingleResult().toString()).intValue();
    }

    public static TemperatureReading getRecentReadingForDevice(Device device) {
        return (TemperatureReading) JPA.em().createNativeQuery("select * from temperature_readings where device_id = ?1 order by timeofreading desc", TemperatureReading.class)
                .setParameter(1, device)
                .setMaxResults(1)
                .getSingleResult();
    }

    public static List<TemperatureReading> getRecentReadingForDevices(List<Device> deviceList) {
        return JPA.em()
                .createNativeQuery("SELECT tr.* FROM temperature_readings tr JOIN (SELECT device_id, MAX(timeofreading) AS latestDateTime FROM temperature_readings where device_id in :inclList GROUP BY device_id) grouptr ON tr.device_id = grouptr.device_id AND tr.timeofreading = grouptr.latestDateTime order by tr.device_id", TemperatureReading.class)
                .setParameter("inclList", deviceList)
                .getResultList();
    }

    public void save() {
        JPA.em().persist(this);
    }

    public static List<TemperatureReading> getAbnormalReadingsForDevices(Device device) {
        return JPA.em()
                .createQuery(" from TemperatureReading where device = ?1 and type = 2 order by timeOfReading desc", TemperatureReading.class)
                .setParameter(1, device)
                .getResultList();
    }

    public static List<TemperatureReading> getInactiveDevicesRecentReadings(List<Device> deviceList, Integer timeSinceInactive, int startIndex, int size) {
        return JPA.em().createNativeQuery("select * from(select distinct on (imm.device_id) * from (select COALESCE(tr.id, row_number() over()) as id, d.id as device_id, COALESCE(tr.type, 0) as type, COALESCE(tr.temperature,0) as temperature, COALESCE(tr.timeofreading,0) as timeofreading from devices d left outer join temperature_readings tr on (d.id = tr.device_id) where d.id in (:inclList))imm order by imm.device_id, imm.timeofreading desc)final where final.timeofreading <= ?1 or final.timeofreading is NULL", TemperatureReading.class)
                .setParameter("inclList", deviceList)
                .setParameter(1, timeSinceInactive)
                .setFirstResult(startIndex)
                .setMaxResults(size)
                .getResultList();
    }

    public static int getInactiveDevicesCount(List<Device> deviceList, Integer timeSinceInactive) {
        return new BigInteger(JPA.em().createNativeQuery("select count(1) from(select distinct on (imm.device_id) * from (select COALESCE(tr.id, row_number() over()) as id, d.id as device_id, COALESCE(tr.type, 0) as type, COALESCE(tr.temperature,0) as temperature, COALESCE(tr.timeofreading,0) as timeofreading from devices d left outer join temperature_readings tr on (d.id = tr.device_id) where d.id in (:inclList))imm order by imm.device_id, imm.timeofreading desc)final where final.timeofreading <= ?1 or final.timeofreading is NULL")
                .setParameter("inclList", deviceList)
                .setParameter(1, timeSinceInactive)
                .getSingleResult().toString()).intValue();
    }
}

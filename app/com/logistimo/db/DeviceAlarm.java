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
@Table(name = "device_alarms")
public class DeviceAlarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "type")
    public int type;

    @Column(name = "status")
    public int status;

    @Column(name = "time")
    public int time;

    @Column(name = "error_code")
    public String errorCode;

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "power_availability")
    public Integer powerAvailability;

    @ManyToOne
    public Device device;

    @Column(name = "sensor_id")
    public String sensorId;

    public DeviceAlarm() {
        errorCode = "";
        status = -1;
    }

    public static List<DeviceAlarm> getDeviceAlarms(Device device, int offset, int limit) {
        return JPA.em()
                .createQuery("from DeviceAlarm where device = ?1 order by time desc", DeviceAlarm.class)
                .setParameter(1, device)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public static int getDeviceAlarmCount(Device device) {
        return new BigInteger(JPA.em()
                .createNativeQuery("select count(1) from device_alarms where device_id = ?1")
                .setParameter(1, device)
                .getSingleResult().toString()).intValue();
    }

    public static List<DeviceAlarm> getAbnormal(List<Device> device) {
        return JPA.em()
                .createQuery("from DeviceAlarm where device in :inclList", DeviceAlarm.class)
                .setParameter("inclList", device)
                .getResultList();
    }

    public static List<DeviceAlarm> getRecentAlarmForDevice(Device device, int limit) {
        if(limit > 0){
            return JPA.em()
                    .createNativeQuery("select * from device_alarms where device_id = ?1 order by type, device_id, time desc", DeviceAlarm.class)
                    .setParameter(1, device)
                    .setMaxResults(limit)
                    .getResultList();
        }
        return JPA.em()
                .createNativeQuery("select * from device_alarms where device_id = ?1 order by type, device_id, time desc", DeviceAlarm.class)
                .setParameter(1, device)
                .getResultList();
    }


    public static List<DeviceAlarm> getRecentAlarmForDevices(List<Device> deviceList) {
        return JPA.em()
                .createNativeQuery("select da.* from device_alarms da JOIN (select device_id, type, max(time) as latestDateTime from device_alarms where device_id in :inclList and type != 3 group by device_id, type) groupda on da.device_id = groupda.device_id and da.type = groupda.type and da.time = groupda.latestDateTime order by da.device_id", DeviceAlarm.class)
                .setParameter("inclList", deviceList)
                .getResultList();
    }

    public static int getAbnormalAlarmCountForDevices(List<Device> deviceList) {
        return new BigInteger(JPA.em()
                .createNativeQuery("select count(1) from (select distinct da.device_id from device_alarms da JOIN (select device_id, type, max(time) as latestDateTime from device_alarms where device_id in :inclList and type != 3 group by device_id, type) groupda on da.device_id = groupda.device_id and da.type = groupda.type and da.time = groupda.latestDateTime where status > 0) final")
                .setParameter("inclList", deviceList)
                .getSingleResult().toString()).intValue();
    }

    public void save() {
        JPA.em().persist(this);
    }
}

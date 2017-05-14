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
import java.util.List;

/**
 * Created by kaniyarasu on 27/10/15.
 */
@Entity
@Table(name = "device_meta_data")
public class DeviceMetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "ky")
    public String key;

    @Column(name = "value")
    public String value;

    @ManyToOne
    public Device device;

    public DeviceMetaData() {
    }

    public DeviceMetaData(String key, Device device) {
        this.key = key;
        this.device = device;
    }

    public DeviceMetaData(String key, String value, Device device) {
        this.key = key;
        this.value = value;
        this.device = device;
    }

    public static List<DeviceMetaData> getDeviceMetaDatas(Device device){
        return JPA.em().createQuery("from DeviceMetaData where device = ?1", DeviceMetaData.class)
                .setParameter(1, device)
                .getResultList();
    }

    public static DeviceMetaData getDeviceMetaDataByKey(Device device, String key){
        return JPA.em().createQuery("from DeviceMetaData where device = ?1 and key = ?2", DeviceMetaData.class)
                .setParameter(1, device)
                .setParameter(2, key)
                .getSingleResult();
    }

    public static List<DeviceMetaData> getDeviceMetaDataByKeys(Device device, List<String> keys){
        return JPA.em().createQuery("from DeviceMetaData where device = ?1 and key in :inclList", DeviceMetaData.class)
                .setParameter(1, device)
                .setParameter("inclList", keys)
                .getResultList();
    }

    public void save() {
        if(this.key != null && this.value != null){
            JPA.em().persist(this);
        }
    }

    public void update() {
        if(this.key != null && this.value != null) {
            JPA.em().merge(this);
        }
    }

    public void delete() {
        JPA.em().remove(this);
    }
}

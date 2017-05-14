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

import com.logistimo.models.temperature.common.DeviceTemperatureRequestStatus;
import play.db.jpa.JPA;

import javax.persistence.*;

@Entity
@Table(name = "device_temperature_request")
public class DeviceTemperatureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "number_of_request", nullable = false)
    public int numberOfRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public DeviceTemperatureRequestStatus status;

    @ManyToOne
    public Device device;

    public DeviceTemperatureRequest() {
    }

    public static DeviceTemperatureRequest getDeviceStatusByDevice(Device device) {
        return JPA.em()
                .createQuery("from DeviceTemperatureRequest where device = ?1", DeviceTemperatureRequest.class)
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

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

import com.logistimo.models.device.common.DeviceRequestStatus;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by kaniyarasu on 20/11/14.
 */
@Entity
@Table(name = "device_config_push_status")
public class DeviceConfigurationPushStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public DeviceRequestStatus status;

    @Column(name = "sent_time")
    public Date sent_time;

    @Column(name = "acknowledged_time")
    public Date acknowledged_time;

    @Column(name = "error_code")
    public String errorCode;

    @Column(name = "error_message")
    public String errorMessage;

    @ManyToOne
    public Device device;

    public static DeviceConfigurationPushStatus findSentByDevice(Device device){
        return JPA.em().createQuery("from DeviceConfigurationPushStatus where device = ?1 and status = ?2 order by sent_time desc", DeviceConfigurationPushStatus.class)
                .setParameter(1, device)
                .setParameter(2, DeviceRequestStatus.SMS_SENT)
                .setMaxResults(1)
                .getSingleResult();
    }

    public void save() {
        JPA.em().persist(this);
    }

    public void update() {
        JPA.em().merge(this);
    }
}

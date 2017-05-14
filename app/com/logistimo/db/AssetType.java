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

/**
 * Created by kaniyarasu on 28/10/15.
 */
@Entity
@Table(name = "asset_type")
public class AssetType {
    public static Integer TEMPERATURE_LOGGER = 1;
    public static Integer TEMP_SENSOR = 4;
    public static Integer ILR = 2;
    public static Integer DEEP_FREEZER = 2;

    public static Integer MONITORED_ASSET = 2;
    public static Integer MONITORING_ASSET = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Integer id;

    @Column(name = "asset_name")
    public String assetName;

    @Column(name = "is_temp_sensitive")
    public Boolean isTempSensitive;

    @Column(name = "is_gsm_enabled")
    public Boolean isGSMEnabled;

    @Column(name = "type")
    public Integer assetType;

    public static AssetType getAssetType(Integer id){
        return JPA.em().createQuery("from AssetType where id = ?1", AssetType.class)
                .setParameter(1, id)
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

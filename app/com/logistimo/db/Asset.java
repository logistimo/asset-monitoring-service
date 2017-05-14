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

import com.logistimo.models.asset.AssetType;
import org.apache.commons.codec.digest.DigestUtils;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by kaniyarasu on 22/09/15.
 */
@Entity
@Table(name = "asset")
public class Asset {
    public static int TEMP_NORMAL = 0;
    public static int TEMP_EXCURSION = 1;
    public static int TEMP_WARNING = 2;
    public static int TEMP_ALARM = 3;

    public static int ASSET_NORMAL = 0;
    public static int ASSET_ABNORMAL = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "uniquehash", nullable = false, unique = true)
    public String uniqueHash;

    @Column(name = "asset_id", nullable = false)
    public String assetId;

    @Column(name = "manc_id", nullable = false)
    public String mancId;

    @Column(name = "asset_type", nullable = false)
    public Integer assetType;

    //Only for temperature monitored assets
    @Column(name = "temperature_state")
    public Integer temperatureState = TEMP_NORMAL;

    //Only for temperature monitored assets
    @Column(name = "temperature_state_updated_time")
    public Integer temperatureStateUpdatedTime;

    @Column(name = "status")
    public Integer status = ASSET_NORMAL;

    @Column(name = "status_updated_time")
    public Integer statusUpdatedTime;

    @Column(name = "created_on", updatable = false)
    public Date createdOn;

    @Column(name = "updated_on")
    public Date updatedOn;

    @PrePersist
    @PreUpdate
    public void updateDevice(){
        //Inserting createdOn time only once, i.e., first time
        if(this.createdOn == null){
            this.createdOn = new Date();
        }
        this.updatedOn = new Date();

        if(this.temperatureStateUpdatedTime == null){
            this.temperatureStateUpdatedTime = (int) (System.currentTimeMillis() / 1000);
        }

        if(this.statusUpdatedTime == null){
            this.statusUpdatedTime = (int) (System.currentTimeMillis() / 1000);
        }
    }

    public static Asset findAsset(String mancId, String assetId) {
        return JPA.em()
                .createQuery("from Asset where mancId=?1 and assetId=?2", Asset.class)
                .setParameter(1, mancId)
                .setParameter(2, assetId)
                .setMaxResults(1)
                .getSingleResult();
    }

    public void save() {
        //Creating unique hash value for device as unique key.
        hash();
        JPA.em().persist(this);
    }

    public void update() {
        JPA.em().merge(this);
    }

    public void delete() {
        JPA.em().remove(this);
    }

    public void hash() {
        this.uniqueHash = DigestUtils.md5Hex(mancId + assetId);
    }
}

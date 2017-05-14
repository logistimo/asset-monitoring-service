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
 * Created by kaniyarasu on 28/10/15.
 */
@Entity
@Table(name = "asset_user")
public class AssetUser {
    public static Integer ASSET_OWNER = 0;
    public static Integer ASSET_MAINTAINER = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "user_name")
    public String userName;

    @Column(name = "user_type")
    public Integer userType;

    @ManyToOne
    public Device device;

    public static List<AssetUser> getAssetUser(Device device){
        return JPA.em().createQuery("from AssetUser where device = ?1", AssetUser.class)
                .setParameter(1, device)
                .getResultList();
    }

    public static AssetUser getAssetUser(Device device, String userName, Integer assetUserType){
        return JPA.em().createQuery("from AssetUser where device = ?1 and userName = ?2 and userType = ?3", AssetUser.class)
                .setParameter(1, device)
                .setParameter(2, userName)
                .setParameter(3, assetUserType)
                .getSingleResult();
    }

    public static List<AssetUser> getAssetUser(Device device, Integer userType){
        return JPA.em().createQuery("from AssetUser where device = ?1 and userType = ?2", AssetUser.class)
                .setParameter(1, device)
                .setParameter(2, userType)
                .getResultList();
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

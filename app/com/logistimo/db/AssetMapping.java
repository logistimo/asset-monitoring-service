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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.JPA;

/**
 * Created by kaniyarasu on 22/09/15.
 */
@Entity
@Table(name = "asset_mapping")
public class AssetMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "location_id")
  public Integer monitoringPositionId;

  @Column(name = "relation_type")
  public Integer relationType;

  @Column(name = "is_primary")
  public Integer isPrimary;

  @ManyToOne
  public Device asset;

  @ManyToOne
  public Device relatedAsset;

  public static AssetMapping findAssetMapping(Device asset, Device relatedAsset) {
    return JPA.em()
        .createQuery("from AssetMapping where asset=?1 and relatedAsset=?2", AssetMapping.class)
        .setParameter(1, asset)
        .setParameter(2, relatedAsset)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static List<AssetMapping> findAssetMappingByRelatedAsset(Device relatedAsset) {
    return JPA.em()
        .createQuery("from AssetMapping where relatedAsset=?1", AssetMapping.class)
        .setParameter(1, relatedAsset)
        .getResultList();
  }

  public static AssetMapping findAssetMappingByRelatedAssetAndType(Device relatedAsset,
                                                                   Integer type) {
    return JPA.em()
        .createQuery("from AssetMapping where relatedAsset=?1 and relationType = ?2",
            AssetMapping.class)
        .setParameter(1, relatedAsset)
        .setParameter(2, type)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static AssetMapping findAssetMappingByAssetAndMonitoringPosition(Device asset,
                                                                          Integer mpId) {
    return JPA.em()
        .createQuery("from AssetMapping where asset=?1 and monitoringPositionId = ?2",
            AssetMapping.class)
        .setParameter(1, asset)
        .setParameter(2, mpId)
        .setMaxResults(1)
        .getSingleResult();
  }

  public static List<AssetMapping> findAssetRelationByAsset(Device asset) {
    return JPA.em()
        .createQuery("from AssetMapping where asset=?1", AssetMapping.class)
        .setParameter(1, asset)
        .getResultList();
  }

  public static List<AssetMapping> findAssetRelationByAssetAndType(Device asset, Integer type) {
    return JPA.em()
        .createQuery("from AssetMapping where asset=?1 and relationType =?2", AssetMapping.class)
        .setParameter(1, asset)
        .setParameter(2, type)
        .getResultList();
  }

  public static AssetMapping findMonitoredAssetMapping(Device monitoringAsset) {
    return (AssetMapping) JPA.em()
        .createNativeQuery("select * from asset_mapping where relatedAsset_id in (select "
                + "relatedAsset_id from asset_mapping where asset_id = "+monitoringAsset.id+" and "
                + "relation_type = 1) and relation_type = 2",
            AssetMapping.class)
        .setMaxResults(1)
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

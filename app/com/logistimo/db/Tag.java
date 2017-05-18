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
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NoResultException;
import javax.persistence.Table;

import play.db.jpa.JPA;

@Entity
@Table(name = "tags")
public class Tag implements Comparable<Tag> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  public Long id;

  @Column(name = "tagname", nullable = false)
  public String tagName;

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
  public Set<Device> device;

  public Tag() {

  }

  public Tag(String name) {
    this.tagName = name;
  }

  public static Tag findOrCreateByName(String tagName) {
    Tag tag;
    try {
      tag = find(tagName);
    } catch (NoResultException e) {
      tag = new Tag(tagName);
      tag.save();
    }

    return tag;
  }

  public static Tag find(String tagName) {
    return JPA.em()
        .createQuery("from Tag where tagName=?1", Tag.class)
        .setParameter(1, tagName)
        .setMaxResults(1)
        .getSingleResult();
  }

  @SuppressWarnings("unchecked")
  public static List<Tag> findChildTags(String tagName) {
    tagName = tagName.trim();
    return (List<Tag>) JPA.em()
        .createNativeQuery("select * from tags where tagname REGEXP ?1", Tag.class)
        .setParameter(1, tagName)
        .getResultList();
  }

  public static List<Tag> findByTagName(String tagName) {
    return JPA.em()
        .createQuery("from Tag where tagName = ?1", Tag.class)
        .setParameter(1, tagName)
        .getResultList();
  }

  public static int getNumber(String tagName) {
    return JPA.em()
        .createQuery("from Tag where tagName = ?1", Tag.class)
        .setParameter(1, tagName)
        .getResultList().size();
  }

  @Override
  public int compareTo(Tag o) {
    return tagName.compareTo(o.tagName);
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
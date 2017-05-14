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

@Entity
@Table(name = "daily_aggregations")
public class DailyAggregation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(nullable = false)
    public int year;
    @Column(nullable = false)
    public int month;
    @Column(nullable = false)
    public int day;
    @Column(nullable = false)
    public long durationOutOfRange;

    @ManyToOne
    public Device device;

    public static DailyAggregation findbyYearAndMonthAndDayAndDevice(int year, int month, int day, Device device) {
        return JPA.em()
                .createQuery("from DailyAggregation where year = ?1 and month = ?2 and day = ?3 and device = ?4",
                        DailyAggregation.class)
                .setParameter(1, year)
                .setParameter(2, month)
                .setParameter(3, day)
                .setParameter(4, device)
                .getSingleResult();
    }

    public void save() {
        JPA.em().persist(this);
    }

    public void update() {
        JPA.em().merge(this);
    }
}

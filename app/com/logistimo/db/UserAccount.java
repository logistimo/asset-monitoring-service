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

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    public static final Integer USERTYPE_RW = 1;
    public static final Integer USERTYPE_R = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    public Long id;

    @Column(name = "username", unique = true, nullable = false)
    public String userName;

    @Column(name = "password", nullable = false)
    public String password;

    @Column(name = "organizationname", nullable = false)
    public String organizationName;

    @Column(name = "type", nullable = false)
    public Integer userType;

    public static List<UserAccount> findAll() {
        return JPA.em()
                .createQuery("from UserAccount", UserAccount.class)
                .getResultList();
    }

    public static UserAccount getUser(String userName) {
        return (UserAccount) JPA.em()
                .createQuery("from UserAccount where username = ?1")
                .setParameter(1, userName)
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

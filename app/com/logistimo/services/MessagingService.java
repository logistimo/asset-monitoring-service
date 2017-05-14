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

package com.logistimo.services;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import akka.camel.CamelMessage;
import com.logistimo.task.AssetAlarmsMessageProducer;
import org.apache.activemq.camel.component.ActiveMQComponent;
import play.Play;

import java.util.HashMap;
import java.util.Map;

public class MessagingService extends ServiceImpl {

    private static final String ACTOR_NAME = "messaging-service-actor";
    private static final String COMPONENT_NAME = "activemq";
    private static final String ACTIVEMQ_URL = "activemq.url";
    private static final String ASSET_ALARMS = "ASSET-ALARMS";

    private Map<String, ActorRef> producers;
    private ActorSystem system;

    public MessagingService() {

        system = ActorSystem.create(ACTOR_NAME);
        Camel camel = CamelExtension.get(system);
        camel.context().addComponent(COMPONENT_NAME, ActiveMQComponent.activeMQComponent(Play.application().configuration().getString(ACTIVEMQ_URL)));

        producers = new HashMap<>();
        producers.put(ASSET_ALARMS, system.actorOf(Props.create(AssetAlarmsMessageProducer.class)));
    }

    public void produceMessage(final String producerId, final String jsonString) {
        producers.get(producerId).tell(new CamelMessage(jsonString, new HashMap()), null);
    }
}


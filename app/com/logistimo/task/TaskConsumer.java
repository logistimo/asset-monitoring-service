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

package com.logistimo.task;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.TaskService;
import play.Logger;
import play.db.jpa.JPA;

/**
 * Created by kaniyarasu on 12/08/15.
 */
public class TaskConsumer extends UntypedConsumerActor {
    private static final TaskService taskService = ServiceFactory.getService(TaskService.class);
    private static final Logger.ALogger LOGGER = Logger.of(TaskConsumer.class);
    private static final int MAX_NUMBER_OF_RETRY = 5;

    //Defaulting to task, if there is new queue should be overridden in sub class
    @Override
    public String getEndpointUri() {
        return "activemq:queue:tms-task";
    }

    @Override
    public void onReceive(Object message) {
        if (message != null && message instanceof CamelMessage) {
            final TaskOptions taskOptions = ((CamelMessage) message).getBodyAs(TaskOptions.class, getCamelContext());
            if(taskOptions != null){
                try {
                    JPA.withTransaction(new play.libs.F.Function0<Void>() {
                        public Void apply() throws Throwable {
                            taskService.consumeMessage(taskOptions);
                            return null;
                        }
                    });
                } catch (Throwable throwable) {
                    LOGGER.warn("Error while executing task {}", taskOptions, throwable);
                    try {
                        JPA.withTransaction(new play.libs.F.Function0<Void>() {
                            public Void apply() throws Throwable {
                                retryTask(taskOptions);
                                return null;
                            }
                        });
                    } catch (Throwable throwable1) {
                        LOGGER.warn("Error while retrying task {}", taskOptions, throwable);
                    }
                }
            }else {
                LOGGER.warn("Error while logging temperature, invalid task options received from queue");
            }
        } else{
            LOGGER.warn("Error while logging temperature, invalid message received from queue");
        }
    }

    private void retryTask(TaskOptions taskOptions){
        try {
            taskOptions.incrNumberOfRetry();
            //Retrying for maximum of five times
            if (taskOptions.getNumberOfRetry() <= MAX_NUMBER_OF_RETRY) { //Use constant
                LOGGER.info("Retrying task {} for {}", taskOptions.toString(), taskOptions.getNumberOfRetry());
                taskOptions.setDelayInMillis(getRetryETAMillis(taskOptions.getNumberOfRetry()));
                taskService.produceMessage(taskOptions, null);
            } else {
                LOGGER.error("Reached the maximum retry, stopping the further execution for task: {}", taskOptions.toString());
            }
        } catch (ServiceException e) {
            LOGGER.error("{} while retrying task {}", e.getMessage(), taskOptions.toString(), e);
        }

    }

    private long getRetryETAMillis(int numberOfRetry){
        final int RETRY_F_MILLISECONDS = 5000;
        long factorial = 1;
        for(int index = 1; index <= numberOfRetry; index++){
            factorial *= index;
        }
        return factorial * RETRY_F_MILLISECONDS;
    }
}

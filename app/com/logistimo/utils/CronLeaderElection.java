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

package com.logistimo.utils;

import com.logistimo.jobs.InactiveDeviceDetectionJob;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.TaskService;
import org.apache.zookeeper.KeeperException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.libs.F;

import java.io.IOException;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by charan on 24/11/14.
 */
public class CronLeaderElection extends ZooElectableClient {

    private final static Logger.ALogger LOGGER = Logger.of(CronLeaderElection.class);

    public static Scheduler scheduler;

    private static CronLeaderElection _instance;
    private boolean shutDown = false;

    protected CronLeaderElection() throws InterruptedException, IOException, KeeperException {
        super(Play.application().configuration().getString("cron.zoo.path", "/amsCronLeader"));
    }

    @Override
    public void performRole() {
        if(getCachedIsLeader()) {
            LOGGER.info("Elected as cron leader");
            try {
                SchedulerFactory schedulerFactory = new StdSchedulerFactory();
                scheduler = schedulerFactory.getScheduler();
                scheduler.start();

                /*JobDetail job = newJob(DailyAggregationJob.class)
                        .withIdentity("DailyAgg", "DailyAggGroup")
                        .build();

                CronTrigger trigger = newTrigger()
                        .withIdentity("DailyAggTrigger", "DailyAggGroup")
                        .withSchedule(cronSchedule("0 0 0 * * ?"))
                        .build();

                scheduler.scheduleJob(job, trigger);*/

                JobDetail job = newJob(InactiveDeviceDetectionJob.class)
                        .withIdentity("InactiveDetection", "DeviceAlarmGroup")
                        .build();

                CronTrigger trigger = newTrigger()
                        .withIdentity("InactiveDetectionTrigger", "DeviceAlarmGroup")
                        .withSchedule(cronSchedule("0 0 * * * ?"))
                        .build();

                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }

            try {
                final TaskService taskService = ServiceFactory.getService(TaskService.class);
                taskService.init();
                try {
                    JPA.withTransaction(new F.Function0<Void>() {
                        public Void apply() throws Throwable {
                            taskService.rescheduleUnprocessedTasks();
                            return null;
                        }
                    });
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }catch(Exception e) {
                LOGGER.error("Error while initiating task service", e);
            }
        }
    }

    @Override
    public void onZooKeeperDisconnected() {
        if (scheduler != null) {
            LOGGER.info("Shutting down Cron scheduler");
            try {
                scheduler.shutdown();
                scheduler = null;
            } catch (SchedulerException e) {
                LOGGER.warn("Failed to close scheduler on shutdown", e);
            }
        }
    }

    @Override
    public void onZooKeeperSessionClosed() {
        if(!shutDown) {
            LOGGER.info("Session closed.. Re init connections");

            if (scheduler != null) {
                LOGGER.info("Shutting down Cron scheduler");
                try {
                    scheduler.shutdown();
                    scheduler = null;
                } catch (SchedulerException e) {
                    LOGGER.warn("Failed to close scheduler on shutdown", e);
                }
            }

            if (hZooKeeper != null) {
                try {
                    hZooKeeper.close();
                } catch (InterruptedException e) {
                    LOGGER.warn("Exception while closing session {0}", e.getMessage());
                }
            }

            try {
                init();
            } catch (Exception e) {
                LOGGER.error("Fatal error while initializing zoo keeper connections ", e);
            }
        }
    }

    public static void start() {
        if (_instance == null) {
            synchronized (CronLeaderElection.class) {
                if (_instance == null) {
                    try {
                        _instance = new CronLeaderElection();
                    } catch (Exception e) {
                        LOGGER.error("Failed to start Zoo Keeper Leader Election for Cron scheduler {0}", e.getLocalizedMessage(), e);
                    }
                }
            }
        }

    }

    public static void stop() {
        if(null != _instance) {
            _instance.shutDown = true;
            if (scheduler != null) {
                LOGGER.info("Shutting down Cron scheduler");
                try {
                    scheduler.shutdown();
                    scheduler = null;
                } catch (SchedulerException e) {
                    LOGGER.warn("Failed to close scheduler on shutdown", e);
                }
            }
            if (null != _instance.hZooKeeper) {
                try {
                    LOGGER.info("Shutting down Zookeeper client");
                    _instance.hZooKeeper.close();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close zookeeper on shutdown", e);
                }
            }
            _instance = null;
        }

    }
}

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

import com.logistimo.db.Task;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.task.BackgroundTaskConsumer;
import com.logistimo.task.BackgroundTaskProducer;
import com.logistimo.task.DataLoggerTaskConsumer;
import com.logistimo.task.DataLoggerTaskProducer;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import akka.camel.CamelMessage;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.Json;
import scala.concurrent.duration.Duration;

/**
 * Created by kaniyarasu on 11/08/15.
 */
@SuppressWarnings("unchecked")
public class TaskService extends ServiceImpl {

  private static final Logger.ALogger LOGGER = Logger.of(TaskService.class);
  private Map<Integer, ActorRef> producers, consumers;
  public ActorSystem system;

  public TaskService() {
    producers = new HashMap<>();
    consumers = new HashMap<>();

    initActorSystem("background-task-actor", TaskType.BACKGROUND_TASK, BackgroundTaskProducer.class, BackgroundTaskConsumer.class);
    initActorSystem("data-logger-task-actor", TaskType.DATA_LOGGER_TASK, DataLoggerTaskProducer.class, DataLoggerTaskConsumer.class);

  }

  private void initActorSystem(String actor, TaskType taskType, Class<?> producerClass, Class<?> consumerClass) {
    system = ActorSystem.create(actor);
    Camel camel = CamelExtension.get(system);
    String brokerURL = Play.application().configuration().getString("activemq.url");
    if (StringUtils.isNotEmpty(brokerURL)) {
      camel.context().addComponent("activemq",
          ActiveMQComponent
              .activeMQComponent(brokerURL));
    }
    producers.put(taskType.getValue(),
        system.actorOf(Props.create(producerClass)));
    consumers.put(taskType.getValue(),
        system.actorOf(Props.create(consumerClass)));
  }

  public void init() {

  }

  public void produceMessage(final TaskOptions taskOptions, Task task) throws ServiceException {
    if (taskOptions != null
        && taskOptions.getClazz() != null) {
      if (producers.get(taskOptions.getType()) != null) {
        //Generating activemq scheduling properties
        //generateHeaders(taskOptions);
        if (taskOptions.getDelayInMillis() > 0) {
          //Persisting task options for recovery
          if (task == null) {
            task = new Task(Json.toJson(taskOptions).toString());
            final Task persistableFinalTask = task;
            boolean isEMOpen = false;
            try{
              if(JPA.em().isOpen()){
                task.save();
              }else{
                isEMOpen = true;
                JPA.withTransaction(persistableFinalTask::save);
              }
            }catch(Throwable e){
              if(!isEMOpen) {
                JPA.withTransaction(persistableFinalTask::save);
              }
            }
          }
          final Task finalTask = task;
          Akka.system().scheduler().scheduleOnce(
              Duration.create(taskOptions.getDelayInMillis(), TimeUnit.MILLISECONDS),
              () -> {
                producers.get(taskOptions.getType()).tell(
                    new CamelMessage(taskOptions, taskOptions.getHeaders()),
                    null
                );
                deleteTask(finalTask);
              },
              Akka.system().dispatcher()
          );
        } else {
          producers.get(taskOptions.getType()).tell(
              new CamelMessage(taskOptions, taskOptions.getHeaders()),
              null
          );
          if(task != null){
            deleteTask(task);
          }
        }
      } else {
        throw new ServiceException("Invalid task type: " + taskOptions.getType());
      }
    } else {
      throw new ServiceException("Invalid task options: " + taskOptions);
    }
  }

  public void deleteTask(final Task finalTask) {
    try {
      JPA.withTransaction(() -> {
        Task.findTaskById(finalTask.id).delete();
        return null;
      });
    } catch (Throwable throwable) {
      LOGGER.warn("Error while deleting task, {}", finalTask, throwable);
    }
  }

  public void consumeMessage(TaskOptions taskOptions) throws Exception {
    if (taskOptions != null && taskOptions.getClazz() != null) {
      Object object = ServiceFactory.getService(taskOptions.getClazz());
      if (object != null && object instanceof Executable) {
        ((Executable) object).process(taskOptions.getContent(), taskOptions.getOptions());
      } else {
        LOGGER.warn("Unable to execute the task, invalid service class: {0}",
            taskOptions.getClazz().getName());
      }
    } else {
      LOGGER.warn("Unable to execute the task, invalid task options");
    }
  }

    /*private void generateHeaders(TaskOptions taskOptions){
        if(taskOptions != null && taskOptions.getDelayInMillis() > 0){
            taskOptions.getHeaders().put(ScheduledMessage.AMQ_SCHEDULED_DELAY, taskOptions.getDelayInMillis());
        }
    }*/

  public void rescheduleUnprocessedTasks() {
    //Rescheduling pending task from Task entity
    try {
      List<Task> taskList = Task.getUnprocessedTasks();
      if (taskList != null && !taskList.isEmpty()) {
        for (Task task : taskList) {
          TaskOptions taskOptions = null;
          try {
            taskOptions = Json.fromJson(Json.parse(task.taskOptions), TaskOptions.class);
            if (taskOptions != null) {
              Long etaMillis = taskOptions.getDelayInMillis() + taskOptions.getInitiatedTime();
              taskOptions.setDelayInMillis(etaMillis - System.currentTimeMillis());
              produceMessage(taskOptions, task);
            }
          } catch (Exception e) {
            LOGGER.warn("{} while rescheduling task {}", e.getMessage(),
                taskOptions != null ? taskOptions.toString() : null, e);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("{} while rescheduling task", e.getMessage(), e);
    }
  }

  public void produceMessage(TaskOptions taskOptions) throws ServiceException {
    produceMessage(taskOptions, null);
  }
}

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

package com.logistimo.migrator;

import com.logistimo.controllers.BaseController;
import com.logistimo.controllers.SecuredAction;
import com.logistimo.db.DeviceStatus;
import com.logistimo.exception.ServiceException;
import com.logistimo.models.device.common.DeviceEventPushModel;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.services.PushAlertService;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.TaskService;
import com.logistimo.utils.LogistimoUtils;

import java.util.List;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.With;

/**
 * @author Mohan Raja
 */
public class DeviceWorkingStatusMigrator extends BaseController {

  private static final Logger.ALogger LOGGER = Logger.of(DeviceWorkingStatusMigrator.class);
  private static final TaskService taskService = ServiceFactory.getService(TaskService.class);
  private static final int MAX_RESULT = 500;

  @Transactional
  @With(SecuredAction.class)
  public static Result migrate() {
    int si = 0;
    while (true) {
      List<DeviceStatus>
          ds =
          JPA.em()
              .createQuery("from DeviceStatus where statusKey = 'wsk' and device.assetType.id != 4",
                  DeviceStatus.class)
              .setFirstResult(si)
              .setMaxResults(MAX_RESULT)
              .getResultList();
      for (DeviceStatus d : ds) {
        DeviceEventPushModel.DeviceEvent deviceEvent = new DeviceEventPushModel.DeviceEvent();
        deviceEvent.vId = d.device.vendorId;
        deviceEvent.dId = d.device.deviceId;
        deviceEvent.st = d.status;
        deviceEvent.time = d.statusUpdatedTime;
        deviceEvent.type = DeviceEventPushModel.DEVICE_EVENT_WORKING_STATUS;
        try {
          taskService.produceMessage(
              new TaskOptions<>(
                  TaskType.BACKGROUND_TASK.getValue(),
                  PushAlertService.class,
                  LogistimoUtils.toJson(new DeviceEventPushModel(deviceEvent)),
                  null
              )
          );
        } catch (ServiceException e) {
          LOGGER.error(
              "{} while scheduling task for posting working status update to Logistics service, {}",
              e.getMessage(), deviceEvent.toString(), e);
        }
      }
      if (ds.size() < MAX_RESULT) {
        break;
      } else {
        si += MAX_RESULT;
      }
    }
    return prepareResult(OK, null, "Migration completed");
  }
}

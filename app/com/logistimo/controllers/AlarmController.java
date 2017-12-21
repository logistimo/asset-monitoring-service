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

package com.logistimo.controllers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.logistimo.healthcheck.MetricsUtil;
import com.logistimo.models.alarm.request.AlarmLoggingRequest;
import com.logistimo.models.alarm.response.AlarmLoggingResponse;
import com.logistimo.services.AlarmService;
import com.logistimo.services.ServiceFactory;

import javax.persistence.NoResultException;

import play.Logger;
import play.Logger.ALogger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

public class AlarmController extends BaseController {
  private static final ALogger LOGGER = Logger.of(AlarmController.class);
  private static final AlarmService alarmService = ServiceFactory.getService(AlarmService.class);
  private static final Meter
      meter = MetricsUtil.getMeter(AlarmController.class,"post.alarm");
  private static final Timer
      timer = MetricsUtil.getTimer(AlarmController.class,"post.alarm");

  @Transactional
  public static Result createAlarm(String callback) {

    meter.mark();
    Timer.Context context = timer.time();
    AlarmLoggingRequest alarmLoggingRequest;
    try {
      alarmLoggingRequest = getValidatedObject(request().body().asJson()
          , AlarmLoggingRequest.class);
    } catch (Exception e) {
      LOGGER.warn("Validation failed while posting alarm", e);
      return prepareResult(BAD_REQUEST, callback,
          "Error while parsing request data - " + e.getMessage());
    }
    try {
      AlarmLoggingResponse alarmLoggingResponse = alarmService.postDeviceAlarm(alarmLoggingRequest);

      if (alarmLoggingResponse.errs.size() == alarmLoggingRequest.data.size()) {
        LOGGER.warn("No device found - " + alarmLoggingResponse.toString());
        return prepareResult(NOT_FOUND, callback, Json.toJson(alarmLoggingResponse));
      } else if (alarmLoggingResponse.errs.size() > 0) {
        LOGGER.warn("Partial content - " + alarmLoggingResponse.toString());
        return prepareResult(PARTIAL_CONTENT, callback, Json.toJson(alarmLoggingResponse));
      }
      return prepareResult(CREATED, callback, "Alarm posted successfully.");
    } catch (Exception e) {
      LOGGER.error("Error while logging alarms", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
    } finally {
      context.stop();
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getAlarm(String vendorId, String deviceId, String sid, int pageNumber,
                                int pageSize, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      return prepareResult(OK, callback,
          Json.toJson(alarmService.getAlarm(vendorId, deviceId, sid, pageNumber, pageSize)));
    } catch (NoResultException e) {
      LOGGER.warn("Error while reading alarms", e);
      return prepareResult(NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while reading alarms", e);
      return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }
}

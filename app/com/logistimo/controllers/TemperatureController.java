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
import com.logistimo.exception.LogistimoException;
import com.logistimo.healthcheck.MetricsUtil;
import com.logistimo.models.common.BaseResponse;
import com.logistimo.models.task.TaskOptions;
import com.logistimo.models.task.TaskType;
import com.logistimo.models.temperature.request.TemperatureLoggingRequest;
import com.logistimo.models.temperature.response.TemperatureLoggingResponse;
import com.logistimo.models.temperature.response.TemperatureReadingResponse;
import com.logistimo.models.temperature.response.TemperatureResponse;
import com.logistimo.models.v1.request.ReadingRequest;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.TaskService;
import com.logistimo.services.TemperatureService;
import com.logistimo.utils.AssetStatusConstants;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.NoResultException;

import play.Logger;
import play.Logger.ALogger;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

public class TemperatureController extends BaseController {

  private static final ALogger LOGGER = Logger.of(TemperatureController.class);
  private static final TemperatureService
      temperatureService =
      ServiceFactory.getService(TemperatureService.class);
  private static final TaskService taskService = ServiceFactory.getService(TaskService.class);
  private static final String MSG_STATUS_REQUEST_SENT = "status_sms_sent";

  private static final Meter
      meter = MetricsUtil.getMeter(TemperatureController.class,"post.temp.meter");
  private static final Timer
      timer = MetricsUtil.getTimer(TemperatureController.class,"post.temp.timer");

  /**
   * @return
   */
  @SuppressWarnings("unchecked")
  @Transactional
  public static Result logReadings(Boolean debug, String callback) {
    TemperatureLoggingRequest temperatureLoggingRequest;
    Integer chSource = AssetStatusConstants.GPRS;
    Map<String, Object> source = null;
    meter.mark();
    Timer.Context context = timer.time();
    try {
      temperatureLoggingRequest =
          getValidatedObject(request().body().asJson(), TemperatureLoggingRequest.class);
      // todo: assuming we have only SMS other than GPRS. Need to know the value of Request-Source.
      if (request().getHeader("Request-Source") != null
          || request().getHeader("Request-source") != null) {
        chSource = AssetStatusConstants.SMS;
        if (!debug) {
          source = new HashMap<>(1);
          source.put("source", chSource);
        }
      }
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      return prepareResult(Http.Status.BAD_REQUEST, callback, e.getMessage());
    }

    try {
      if (StringUtils.isNotBlank(temperatureLoggingRequest.vId)
          && temperatureLoggingRequest.data != null
          && temperatureLoggingRequest.data.size() > 0) {
        if (debug) {
          TemperatureLoggingResponse temperatureLoggingResponse =
              temperatureService.logReadings(temperatureLoggingRequest, chSource);

          if (temperatureLoggingResponse.errs.size() == temperatureLoggingRequest.data.size()) {
            LOGGER.warn("No device found: " + temperatureLoggingRequest.toString());
            return prepareResult(Http.Status.NOT_FOUND, callback,
                temperatureLoggingRequest.toString());
          } else if (temperatureLoggingResponse.errs.size() > 0
              || temperatureLoggingResponse.errTmps.size() > 0) {
            LOGGER.warn("Partial content: " + temperatureLoggingResponse.toString());
            return prepareResult(Http.Status.PARTIAL_CONTENT, callback,
                Json.toJson(temperatureLoggingResponse));
          }
        } else {
          taskService.produceMessage(
              new TaskOptions(
                  TaskType.DATA_LOGGER_TASK.getValue(),
                  TemperatureService.class,
                  LogistimoUtils.toJson(temperatureLoggingRequest),
                  source
              )
          );
        }
        return prepareResult(Http.Status.CREATED, callback,
            "Temperature reading posted successfully.");
      } else {
        LOGGER.warn("Invalid Temperature Reading.");
        return prepareResult(Http.Status.BAD_REQUEST, callback, "Invalid Temperature Reading.");
      }
    } catch (Exception e) {
      LOGGER.error("Error while logging temperature readings", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    } finally {
      context.stop();
    }
  }

  @Deprecated
  @Transactional
  public static Result createReadingsV1(String callback) {
    ReadingRequest readingRequest;
    try {
      readingRequest = getValidatedObject(request().body().asJson()
          , ReadingRequest.class);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      return prepareResult(Http.Status.BAD_REQUEST, callback, e.getMessage());
    }
    Integer chSource = AssetStatusConstants.GPRS;
    if (request().getHeader("Request-Source") != null
        || request().getHeader("Request-source") != null) {
      chSource = AssetStatusConstants.SMS;
    }

    try {
      if (StringUtils.isNotBlank(readingRequest.vendorId)
          && readingRequest.data != null
          && readingRequest.data.size() > 0) {
        TemperatureLoggingRequest
            temperatureLoggingRequest =
            temperatureService.buildTempLoggingRequest(readingRequest);
        TemperatureLoggingResponse temperatureLoggingResponse =
            temperatureService.logReadings(temperatureLoggingRequest, chSource);

        if (temperatureLoggingResponse.errs.size() == temperatureLoggingRequest.data.size()) {
          LOGGER.warn("No device found: " + temperatureLoggingRequest.toString());
          return prepareResult(Http.Status.NOT_FOUND, callback,
              temperatureLoggingRequest.toString());
        } else if (temperatureLoggingResponse.errs.size() > 0
            || temperatureLoggingResponse.errTmps.size() > 0) {
          LOGGER.warn("Partial content: " + temperatureLoggingResponse.toString());
          return prepareResult(Http.Status.PARTIAL_CONTENT, callback,
              Json.toJson(temperatureLoggingResponse));
        } else {
          return prepareResult(Http.Status.CREATED, callback,
              "Temperature reading posted successfully.");
        }
      } else {
        LOGGER.warn("Invalid Temperature Reading: " + readingRequest.toString());
        return prepareResult(Http.Status.BAD_REQUEST, callback,
            "Invalid Temperature Reading : " + readingRequest.toString());
      }
    } catch (Exception e) {
      LOGGER.error("Error while logging temperature readings", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadings(String vendorId, String deviceId, String sid,
                                            int pageNumber, int pageSize, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadings(
              vendorId, deviceId, sid, pageNumber, pageSize, -1, -1);
      return prepareResult(Http.Status.OK, callback, Json.toJson(temperatureReadingResponse));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsV3(String vendorId, String deviceId, Integer mpId,
                                              int pageNumber, int pageSize, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadingsV3(
              vendorId, deviceId, mpId, pageNumber, pageSize, -1, -1);
      return prepareResult(Http.Status.OK, callback, Json.toJson(temperatureReadingResponse));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsBetween(String vendorId, String deviceId, String sid,
                                                   int from, int to, int pageNumber, int pageSize,
                                                   String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadings(
              vendorId, deviceId, sid, pageNumber, pageSize, from, to);
      return prepareResult(Http.Status.OK, callback, Json.toJson(temperatureReadingResponse));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId, e);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsBetweenV3(String vendorId, String deviceId, Integer mpId,
                                                     int from, int to, int pageNumber, int pageSize,
                                                     String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadingsV3(
              vendorId, deviceId, mpId, pageNumber, pageSize, from, to);
      return prepareResult(Http.Status.OK, callback, Json.toJson(temperatureReadingResponse));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId, e);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Deprecated
  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsV1(String vendorId, String deviceId, String sid,
                                              int pageNumber, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadings(
              vendorId, deviceId, sid, pageNumber, -1, -1, -1);
      return prepareResult(Http.Status.OK, callback,
          Json.toJson(toTemperatureReadingResponseV1(temperatureReadingResponse)));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId, e);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Deprecated
  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsBetweenV1(String vendorId, String deviceId, String sid,
                                                     int from, int to, int pageNumber,
                                                     String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      TemperatureReadingResponse
          temperatureReadingResponse =
          temperatureService.getPaginatedReadings(
              vendorId, deviceId, sid, pageNumber, -1, from, to);
      return prepareResult(Http.Status.OK, callback,
          Json.toJson(toTemperatureReadingResponseV1(temperatureReadingResponse)));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for device %s and vendor %s", deviceId, vendorId);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  @With(SecuredAction.class)
  public static Result getPaginatedReadingsByTag(String tagName, int pageNumber, int pageSize,
                                                 String callback) {
    try {
      return prepareResult(Http.Status.OK, callback,
          Json.toJson(temperatureService.getPaginatedReadingsByTag(
              tagName, pageNumber, pageSize)));
    } catch (NoResultException e) {
      LOGGER.warn("No Temperature Reading for tagName %s", tagName, e);
      return prepareResult(Http.Status.NOT_FOUND, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while retrieving temperatures", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Transactional
  @With(SecuredAction.class)
  public static Result getDeviceCurrentTemperature(String vendorId, String deviceId,
                                                   String sensorId, String callback) {
    try {
      deviceId = decodeParameter(deviceId);
      temperatureService.getCurrentTemperature(vendorId, deviceId);
      return prepareResult(Http.Status.OK, callback,
          Json.toJson(new BaseResponse(Messages.get(MSG_STATUS_REQUEST_SENT))));
    } catch (NoResultException e) {
      LOGGER.warn("Error while getting device current temperature" + Messages
          .get(LogistimoConstant.DEVICES_NOT_FOUND), e);
      return prepareResult(Http.Status.NOT_FOUND, callback,
          Messages.get(LogistimoConstant.DEVICES_NOT_FOUND));
    } catch (LogistimoException e) {
      LOGGER.warn("Error while getting device current temperature", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error while getting device current temperature", e);
      return prepareResult(Http.Status.INTERNAL_SERVER_ERROR, callback, e.getMessage());
    }
  }

  @Deprecated
  private static com.logistimo.models.v1.response.TemperatureReadingResponse toTemperatureReadingResponseV1(
      TemperatureReadingResponse temperatureReadingResponse) {
    Map<Integer, String> typeMap = new TreeMap<Integer, String>() {{
      put(0, "RAW");
      put(1, "INCURSION");
      put(2, "EXCURSION");
    }};

    com.logistimo.models.v1.response.TemperatureReadingResponse
        temperatureReadingResponseV1 =
        new com.logistimo.models.v1.response.TemperatureReadingResponse();

    temperatureReadingResponseV1.numberOfPages = temperatureReadingResponse.getnPages();
    for (TemperatureResponse temperatureResponse : temperatureReadingResponse.data) {
      com.logistimo.models.v1.response.TemperatureResponse
          temperatureResponseV1 =
          new com.logistimo.models.v1.response.TemperatureResponse();

      temperatureResponseV1.temperature = temperatureResponse.tmp;
      temperatureResponseV1.timeOfReading = temperatureResponse.time;
      temperatureResponseV1.type = typeMap.get(temperatureResponse.typ);

      temperatureReadingResponseV1.data.add(temperatureResponseV1);
    }

    return temperatureReadingResponseV1;
  }
}

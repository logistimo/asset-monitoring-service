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

import com.logistimo.models.stats.request.DailyStatsLoggingRequest;
import com.logistimo.models.stats.response.StatsLoggingResponse;
import com.logistimo.models.v1.request.StatsRequest;
import com.logistimo.services.ServiceFactory;
import com.logistimo.services.StatsService;
import play.Logger;
import play.Logger.ALogger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;

import javax.persistence.NoResultException;

public class StatsController extends BaseController {
    private static final ALogger LOGGER = Logger.of(StatsController.class);
    private static final StatsService statsService = ServiceFactory.getService(StatsService.class);

    @Transactional
    public static Result createStats(String callback) {
        DailyStatsLoggingRequest dailyStatsLoggingRequest;
        try {
            dailyStatsLoggingRequest = getValidatedObject(request().body().asJson()
                    , DailyStatsLoggingRequest.class);
        } catch (Exception e) {
            LOGGER.warn("Error while parsing the stats request", e);
            return prepareResult(BAD_REQUEST, callback, "Error parsing the request" + e.getMessage());
        }

        try {
            StatsLoggingResponse statsLoggingResponse = statsService.postDailyStats(dailyStatsLoggingRequest);
            if (statsLoggingResponse.errs.size() == dailyStatsLoggingRequest.data.size()){
                LOGGER.warn("Device(s) not found: " + Json.toJson(statsLoggingResponse).toString());
                return prepareResult(NOT_FOUND, callback, Json.toJson(statsLoggingResponse));
            } else if (statsLoggingResponse.errs.size() > 0) {
                LOGGER.warn("Partial content: " + statsLoggingResponse.toString());
                return prepareResult(PARTIAL_CONTENT, callback, Json.toJson(statsLoggingResponse));
            }
            return prepareResult(CREATED, callback, "Daily stats posted successfully.");
        } catch (NoResultException e) {
            LOGGER.warn("Error while logging stats", e);
            return prepareResult(NOT_FOUND, callback, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error while logging daily stats", e);
            return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
        }
    }

    @Deprecated
    @Transactional
    public static Result createStatsV1(String callback) {
        StatsRequest statsRequest;
        try {
            statsRequest = Json.fromJson(request().body().asJson(), StatsRequest.class);
        } catch (Exception e) {
            LOGGER.warn("Error while parsing the stats request", e);
            return prepareResult(BAD_REQUEST, callback, "Error parsing the request" + e.getMessage());
        }

        try {
            if (statsRequest == null) {
                return prepareResult(BAD_REQUEST, callback, "Bad Reqeust.");
            }

            statsService.postDailyStatsV1(statsRequest);
            return prepareResult(OK, callback, "Daily Stats posted successfully.");
        } catch (NoResultException e) {
            LOGGER.warn(e.getMessage());
            return prepareResult(NOT_FOUND, callback, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error while logging daily stats - v1", e);
            return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @With(SecuredAction.class)
    public static Result getStats(String vendorId, String deviceId, String sid, int pageNumber, int pageSize, String callback) {
        try {
            deviceId = decodeParameter(deviceId);
            return prepareResult(OK, callback, Json.toJson(statsService.getStatsByDevice(vendorId, deviceId, sid, pageNumber, pageSize)));
        } catch (Exception e) {
            LOGGER.error("Error while generating stats by device", e);
            return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @With(SecuredAction.class)
    public static Result getStatsByRange(String vendorId, String deviceId, String sid, int from, int to, int pageNumber, int pageSize, String callback) {
        try {
            deviceId = decodeParameter(deviceId);
            return prepareResult(OK, callback, Json.toJson(statsService.getStatsByDeviceAndRange(vendorId, deviceId, sid, from, to, pageNumber, pageSize)));
        } catch (Exception e) {
            LOGGER.error("Error while generating stats by tag", e);
            return prepareResult(INTERNAL_SERVER_ERROR, callback, e.getMessage());
        }
    }
}

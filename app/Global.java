
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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.logistimo.controllers.BaseController;
import com.logistimo.healthcheck.MetricsUtil;
import com.logistimo.utils.CronLeaderElection;
import com.logistimo.utils.LogistimoConstant;

import java.lang.reflect.Method;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.i18n.Messages;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public class Global extends GlobalSettings {
    private static final Logger.ALogger LOGGER = Logger.of(Global.class);

    public static MetricRegistry metrics;

    public static JmxReporter reporter;

    @Override
    public void onStart(Application app) {
        super.onStart(app);
        //Starting cron leader election.
        CronLeaderElection.start();
        //configuring metrics
        configureMetrics();
    }

    @Override
    public void onStop(Application app) {
        super.onStop(app);
        //Stopping cron leader election.
        CronLeaderElection.stop();
        //closing JmxReporter
        if (null != reporter) {
            reporter.close();
        }
    }

    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader request, String error) {
        logMeterMetrics(request);
        return F.Promise.pure(BaseController.prepareResult(Http.Status.BAD_REQUEST, request.getQueryString("callback"), error));
    }

    @Override
    public Action onRequest(Http.Request request, Method actionMethod) {
        String CONTENT_TYPE = "Content-Type";
        String REQUEST_SOURCE = "Request-Source";

        if (!request.headers().containsKey(CONTENT_TYPE))
            CONTENT_TYPE = "Content-type";

        if (!request.headers().containsKey(CONTENT_TYPE))
            CONTENT_TYPE = "content-type";

        if (!request.headers().containsKey(CONTENT_TYPE))
            CONTENT_TYPE = CONTENT_TYPE.toUpperCase();

        if (!request.headers().containsKey(REQUEST_SOURCE))
            REQUEST_SOURCE = "Request-source";

        if (!request.headers().containsKey(REQUEST_SOURCE))
            REQUEST_SOURCE = REQUEST_SOURCE.toUpperCase();

        if (request.method().equals("POST")) {
            if (request.headers().get(CONTENT_TYPE) != null && request.headers().get(CONTENT_TYPE).length > 0) {
                if (!("application/json; charset=utf-8".equals(request.headers().get(CONTENT_TYPE)[0]) || "application/json".equals(request.headers().get(CONTENT_TYPE)[0]) || "text/json".equals(request.headers().get(CONTENT_TYPE)[0]))) {
                    LOGGER.warn(Messages.get(LogistimoConstant.REQUEST_INVALID_CONTENT_TYPE));
                    throw new UnsupportedOperationException(Messages.get(LogistimoConstant.REQUEST_INVALID_CONTENT_TYPE)
                            + request.headers().get(CONTENT_TYPE)[0]);
                }
            } else {
                LOGGER.warn(Messages.get(LogistimoConstant.REQUEST_EMPTY_CONTENT_TYPE));
                throw new UnsupportedOperationException(Messages.get(LogistimoConstant.REQUEST_EMPTY_CONTENT_TYPE) + "***" + Json.toJson(request.headers()));
            }

            String requestSource = request.headers().get(REQUEST_SOURCE) != null && request.headers().get(REQUEST_SOURCE).length > 0
                    ? request.headers().get(REQUEST_SOURCE)[0] : "GPRS";
            String requestData = request.body() != null ? request.body().asJson().toString() : null;
            LOGGER.info("src: " + requestSource + ",data: " + requestData);
        }

        logMeterMetrics(request);
        return super.onRequest(request, actionMethod);
    }

    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        LOGGER.error(Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE)+" "+t.getMessage(),t);
        logMeterMetrics(request);
        return F.Promise.pure(BaseController.prepareResult(Http.Status.INTERNAL_SERVER_ERROR, request.getQueryString("callback"), Messages.get(LogistimoConstant.SERVER_ERROR_RESPONSE)));
    }

    private void configureMetrics () {
        metrics = MetricsUtil.getRegistry();
        reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
    }

    private void logMeterMetrics (Http.Request request) {
        logMeterMetrics(request.path(),null);
    }
    private void logMeterMetrics (Http.RequestHeader requestHeader) {
        logMeterMetrics(requestHeader.path(),"error.");
    }

    private void logMeterMetrics (String path, String prefix) {
        Meter meter = MetricsUtil.getMeter(Http.Request.class,prefix != null ?prefix+path : path);
        meter.mark();
    }


}
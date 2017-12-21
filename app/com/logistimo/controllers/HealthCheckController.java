package com.logistimo.controllers;

import com.logistimo.models.common.BaseResponse;
import com.logistimo.services.RedisCacheService;
import com.logistimo.services.ServiceFactory;

import play.db.jpa.JPA;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by kumargaurav on 21/12/17.
 */
public class HealthCheckController extends Controller {

  private static final Integer SUCCESS = 200;
  private static final Integer ERROR = 500;

  public static Result healthCheck () {
    try {
      checkDB();
      checkCache();
      return status(SUCCESS, Json.toJson(new BaseResponse("AMS service up and running!")));
    } catch (Throwable e) {
      return status(ERROR, Json.toJson(new BaseResponse("Issue with AMS service, Please check!")));
    }
  }

  private static void checkDB () throws Throwable {
    JPA.withTransaction(() -> JPA.em().createNativeQuery("SELECT 1").getMaxResults());
  }

  private static void checkCache () {
    RedisCacheService cache = ServiceFactory.getService(RedisCacheService.class);
    cache.ping();
  }
}

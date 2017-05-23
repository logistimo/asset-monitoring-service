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

import com.logistimo.db.DailyStatsDO;
import com.logistimo.db.Device;
import com.logistimo.db.TemperatureStatistics;
import com.logistimo.exception.LogistimoException;
import com.logistimo.models.common.ErrorResponse;
import com.logistimo.models.stats.common.AlertStats;
import com.logistimo.models.stats.common.BatteryStats;
import com.logistimo.models.stats.common.CommunicationStats;
import com.logistimo.models.stats.common.DailyStats;
import com.logistimo.models.stats.common.DailyStatsDeviceError;
import com.logistimo.models.stats.common.DeviceConnectionStats;
import com.logistimo.models.stats.common.ExternalSensorStats;
import com.logistimo.models.stats.common.StorageStats;
import com.logistimo.models.stats.request.DailyStatsLoggingRequest;
import com.logistimo.models.stats.request.DeviceStatsRequest;
import com.logistimo.models.stats.response.DeviceStatsResponse;
import com.logistimo.models.stats.response.StatsLoggingResponse;
import com.logistimo.models.stats.response.StatsResponse;
import com.logistimo.models.v1.request.DeviceStats;
import com.logistimo.models.v1.request.StatsRequest;
import com.logistimo.utils.LogistimoConstant;
import com.logistimo.utils.LogistimoUtils;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import play.Logger;
import play.i18n.Messages;

public class StatsService extends ServiceImpl {
  private static final Logger.ALogger LOGGER = Logger.of(StatsService.class);
  private static final DeviceService deviceService = ServiceFactory.getService(DeviceService.class);

  public StatsLoggingResponse postDailyStats(DailyStatsLoggingRequest dailyStatsLoggingRequest) {
    StatsLoggingResponse statsLoggingResponse = new StatsLoggingResponse();
    for (DeviceStatsRequest deviceStatsRequest : dailyStatsLoggingRequest.data) {
      try {
        LogistimoUtils.validateObject(deviceStatsRequest);
        Device
            device =
            deviceService.findDevice(dailyStatsLoggingRequest.vId, deviceStatsRequest.dId);
        if (device != null) {
          DailyStatsDO dailyStatsDO = toDailyStats(deviceStatsRequest.stats);
          dailyStatsDO.device = device;

          //If sId exist, then received stats is for specific sensor
          if (deviceStatsRequest.sId != null && !deviceStatsRequest.sId.isEmpty()) {
            dailyStatsDO.device = deviceService.findDevice(
                device.vendorId,
                LogistimoUtils.generateVirtualDeviceId(
                    device.deviceId,
                    deviceStatsRequest.sId
                )
            );
          }
          dailyStatsDO.save();

          //Saving Daily Stats - Device errors
          if (deviceStatsRequest.stats.errs != null) {
            for (DailyStatsDeviceError dailyStatsDeviceErrorRequest : deviceStatsRequest.stats.errs) {
              com.logistimo.db.DailyStatsDeviceError
                  dailyStatsDeviceError =
                  toDailyStatsDeviceError(dailyStatsDeviceErrorRequest);
              dailyStatsDeviceError.daily_stats = dailyStatsDO;
              dailyStatsDeviceError.save();
            }
          }
        }
      } catch (NoResultException e) {
        LOGGER.warn("{} while logging daily stats for device: {}, {}", e.getMessage(),
            dailyStatsLoggingRequest.vId, deviceStatsRequest.dId, e);
        statsLoggingResponse.errs.add(
            new ErrorResponse(
                deviceStatsRequest.dId,
                deviceStatsRequest.sId,
                Messages.get(LogistimoConstant.DEVICES_NOT_FOUND)));
      } catch (LogistimoException e) {
        LOGGER.warn("{} while logging daily stats for device: {}, {}", e.getMessage(),
            dailyStatsLoggingRequest.vId, deviceStatsRequest.dId, e);
        statsLoggingResponse.errs.add(
            new ErrorResponse(
                deviceStatsRequest.dId,
                deviceStatsRequest.sId,
                e.getMessage()));
      }
    }

    return statsLoggingResponse;
  }

  @Deprecated
  public int postDailyStatsV1(StatsRequest statsRequest) {
    int count = 0;
    for (DeviceStats stats : statsRequest.data) {
      Device device = deviceService.findDevice(statsRequest.vendorId, stats.deviceId);

      if (device != null) {
        TemperatureStatistics temperatureStatistics = stats.toDeviceStatistics(device);
        temperatureStatistics.save();
        count++;
      }
    }

    return count;
  }
/*
    public TaggedStatsResponse getStatsByTags(String tagName){
        DeviceService deviceService = ServiceFactory.getDeviceService();
        TaggedStatsResponse taggedStatsResponse = new TaggedStatsResponse();
        taggedStatsResponse.tag = tagName;

        try{
            TaggedAbnormalDeviceResponse taggedAbnormalDeviceResponse = deviceService.getAbnormalDeviceResponseByTag(tagName);
            taggedStatsResponse.numDevices = taggedAbnormalDeviceResponse.numberOfDevices;
            taggedStatsResponse.numAbnormalTemps = taggedAbnormalDeviceResponse.abnormalTemperatures.size();
            taggedStatsResponse.numAlarmDevices = taggedAbnormalDeviceResponse.alarmDevices.size();
        }catch (NoResultException e){
            taggedStatsResponse.err.code = 0;
            taggedStatsResponse.err.message = Messages.get("tag.not_found");
        }catch (Exception e){
            taggedStatsResponse.err.code = 1;
            taggedStatsResponse.err.message = e.getMessage();
        }

        return taggedStatsResponse;
    }*/

  public StatsResponse getStatsByDevice(String vendorId, String deviceId, String sid,
                                        int pageNumber, int pageSize) {
    Device device = deviceService.findDevice(vendorId, deviceId, sid);

    pageSize = LogistimoUtils.transformPageSize(pageSize);

    List<DailyStatsDO>
        dailyStatsDOList =
        DailyStatsDO.getDailyStats(device,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize, -1, -1);
    StatsResponse statsResponse = new StatsResponse();

    for (DailyStatsDO dailyStatsDO : dailyStatsDOList) {
      statsResponse.data.add(toDeviceStatsResponse(dailyStatsDO,
          com.logistimo.db.DailyStatsDeviceError.getDailyStatsDeviceErrors(dailyStatsDO)));
    }

    statsResponse.setnPages(LogistimoUtils
        .availableNumberOfPages(pageSize, DailyStatsDO.getDailyStatsCount(device, -1, -1)));
    return statsResponse;
  }

  public StatsResponse getStatsByDeviceAndRange(String vendorId, String deviceId, String sid,
                                                int from, int to, int pageNumber, int pageSize) {
    Device device = deviceService.findDevice(vendorId, deviceId, sid);

    pageSize = LogistimoUtils.transformPageSize(pageSize);

    List<DailyStatsDO>
        dailyStatsDOList =
        DailyStatsDO.getDailyStats(device,
            LogistimoUtils.transformPageNumberToPosition(pageNumber, pageSize), pageSize, from, to);
    StatsResponse statsResponse = new StatsResponse();

    for (DailyStatsDO dailyStatsDO : dailyStatsDOList) {
      statsResponse.data.add(toDeviceStatsResponse(dailyStatsDO,
          com.logistimo.db.DailyStatsDeviceError.getDailyStatsDeviceErrors(dailyStatsDO)));
    }

    statsResponse.setnPages(LogistimoUtils
        .availableNumberOfPages(pageSize, DailyStatsDO.getDailyStatsCount(device, from, to)));
    return statsResponse;
  }

  private DailyStatsDO toDailyStats(DailyStats dailyStatsRequest) throws LogistimoException {
    DailyStatsDO dailyStatsDO = new DailyStatsDO();

    if (dailyStatsRequest.day > 0) {
      dailyStatsDO.day = dailyStatsRequest.day;
    } else {
      throw new LogistimoException("Day(day) is required.");
    }

    if (dailyStatsRequest.batt != null) {
      dailyStatsDO.batteryStatus = dailyStatsRequest.batt.stat;
      dailyStatsDO.batteryAlarms = dailyStatsRequest.batt.getnAlrms();
      dailyStatsDO.batteryTime = dailyStatsRequest.batt.time;
      dailyStatsDO.batteryActualVolt = dailyStatsRequest.batt.actv;
      dailyStatsDO.batteryLowVolt = dailyStatsRequest.batt.lowv;
      dailyStatsDO.batteryHighVolt = dailyStatsRequest.batt.highv;
      dailyStatsDO.batteryChargingTime = dailyStatsRequest.batt.chgt;
      dailyStatsDO.batteryWarningDuration = dailyStatsRequest.batt.wdur;
      dailyStatsDO.batteryAlarmDuration = dailyStatsRequest.batt.adur;
      dailyStatsDO.powerAvailableTime = dailyStatsRequest.batt.pwrt;
    } else {
      throw new LogistimoException("Battery stats(batt) is required");
    }

    if (dailyStatsRequest.getdCon() != null) {
      dailyStatsDO.deviceConnectionStatus = dailyStatsRequest.getdCon().stat;
      dailyStatsDO.deviceConnectionAlarms = dailyStatsRequest.getdCon().getnAlrms();
      dailyStatsDO.deviceConnectionTime = dailyStatsRequest.getdCon().time;
      dailyStatsDO.deviceConnectionDuration = dailyStatsRequest.getdCon().dur;
    }

    if (dailyStatsRequest.getxSns() != null) {
      dailyStatsDO.externalSensorStatus = dailyStatsRequest.getxSns().stat;
      dailyStatsDO.externalSensorAlarms = dailyStatsRequest.getxSns().getnAlrms();
      dailyStatsDO.externalSensorTime = dailyStatsRequest.getxSns().time;
      dailyStatsDO.externalSensorDuration = dailyStatsRequest.getxSns().dur;
    }

    if (dailyStatsRequest.mean != null) {
      dailyStatsDO.meanTemperature = dailyStatsRequest.mean;
    } else {
      throw new LogistimoException("Mean temperature(mean) is required.");
    }

    if (dailyStatsRequest.min != null) {
      dailyStatsDO.minTemperature = dailyStatsRequest.min;
    } else {
      throw new LogistimoException("Min temperature(min) is required.");
    }

    if (dailyStatsRequest.max != null) {
      dailyStatsDO.maxTemperature = dailyStatsRequest.max;
    } else {
      throw new LogistimoException("Max temperature(max) is required.");
    }

    dailyStatsDO.numberOfExcursions = dailyStatsRequest.getnExc();
    if (dailyStatsRequest.tz != null) {
      dailyStatsDO.timezoneOffset = dailyStatsRequest.tz;
    }

    if (dailyStatsRequest.high != null) {
      dailyStatsDO.highAlertStatus = dailyStatsRequest.high.stat;
      dailyStatsDO.highAlertAlarms = dailyStatsRequest.high.getnAlrms();
      dailyStatsDO.highAlertTime = dailyStatsRequest.high.time;
      dailyStatsDO.highAlertDuration = dailyStatsRequest.high.dur;
      dailyStatsDO.highAlertAmbientTemperature = dailyStatsRequest.high.getaTmp();
      dailyStatsDO.highAlertCnfms = dailyStatsRequest.high.cnfms;
      dailyStatsDO.highAlertCnf = dailyStatsRequest.high.cnf;
    }

    if (dailyStatsRequest.low != null) {
      dailyStatsDO.lowAlertStatus = dailyStatsRequest.low.stat;
      dailyStatsDO.lowAlertAlarms = dailyStatsRequest.low.getnAlrms();
      dailyStatsDO.lowAlertTime = dailyStatsRequest.low.time;
      dailyStatsDO.lowAlertDuration = dailyStatsRequest.low.dur;
      dailyStatsDO.lowAlertAmbientTemperature = dailyStatsRequest.low.getaTmp();
      dailyStatsDO.lowAlertCnfms = dailyStatsRequest.low.cnfms;
      dailyStatsDO.lowAlertCnf = dailyStatsRequest.low.cnf;
    }

    if (dailyStatsRequest.comm != null) {
      dailyStatsDO.numberOfSmsSent = dailyStatsRequest.comm.getnSMS();
      dailyStatsDO.numberOfInternetPushes = dailyStatsRequest.comm.getnPsh();
      dailyStatsDO.numberOfInternetFailures = dailyStatsRequest.comm.getnErr();
    } else {
      throw new LogistimoException("Communication stats(comm) is required");
    }

    if (dailyStatsRequest.str != null) {
      dailyStatsDO.availableDiskSpace = dailyStatsRequest.str.dsk;
      dailyStatsDO.numberOfTempCached = dailyStatsRequest.str.ntmp;
      dailyStatsDO.numberOfDVCCached = dailyStatsRequest.str.ndvc;
    }

    return dailyStatsDO;
  }

  private DeviceStatsResponse toDeviceStatsResponse(DailyStatsDO dailyStatsDO,
                                                    List<com.logistimo.db.DailyStatsDeviceError> dailyStatsDeviceErrorList) {

    DeviceStatsResponse deviceStatsResponse = new DeviceStatsResponse();

    deviceStatsResponse.trId = dailyStatsDO.device.transmitterId;
    deviceStatsResponse.stats.day = dailyStatsDO.day;
    deviceStatsResponse.stats.tz = dailyStatsDO.timezoneOffset;
    deviceStatsResponse.stats.setnExc(dailyStatsDO.numberOfExcursions);
    deviceStatsResponse.stats.mean = dailyStatsDO.meanTemperature;
    deviceStatsResponse.stats.min = dailyStatsDO.minTemperature;
    deviceStatsResponse.stats.max = dailyStatsDO.maxTemperature;

    deviceStatsResponse.stats.low = new AlertStats();
    deviceStatsResponse.stats.low.stat = dailyStatsDO.lowAlertStatus;
    deviceStatsResponse.stats.low.setnAlrms(dailyStatsDO.lowAlertAlarms);
    deviceStatsResponse.stats.low.dur = dailyStatsDO.lowAlertDuration;
    deviceStatsResponse.stats.low.time = dailyStatsDO.lowAlertTime;
    deviceStatsResponse.stats.low.setaTmp(dailyStatsDO.lowAlertAmbientTemperature);
    deviceStatsResponse.stats.low.cnfms = dailyStatsDO.lowAlertCnfms;
    deviceStatsResponse.stats.low.cnf = dailyStatsDO.lowAlertCnf;

    deviceStatsResponse.stats.high = new AlertStats();
    deviceStatsResponse.stats.high.stat = dailyStatsDO.highAlertStatus;
    deviceStatsResponse.stats.high.setnAlrms(dailyStatsDO.highAlertAlarms);
    deviceStatsResponse.stats.high.dur = dailyStatsDO.highAlertDuration;
    deviceStatsResponse.stats.high.time = dailyStatsDO.highAlertTime;
    deviceStatsResponse.stats.high.setaTmp(dailyStatsDO.highAlertAmbientTemperature);
    deviceStatsResponse.stats.high.cnfms = dailyStatsDO.highAlertCnfms;
    deviceStatsResponse.stats.high.cnf = dailyStatsDO.highAlertCnf;

    deviceStatsResponse.stats.setxSns(new ExternalSensorStats());
    deviceStatsResponse.stats.getxSns().dur = dailyStatsDO.externalSensorDuration;
    deviceStatsResponse.stats.getxSns().stat = dailyStatsDO.externalSensorStatus;
    deviceStatsResponse.stats.getxSns().setnAlrms(dailyStatsDO.externalSensorAlarms);
    deviceStatsResponse.stats.getxSns().time = dailyStatsDO.externalSensorTime;

    deviceStatsResponse.stats.setdCon(new DeviceConnectionStats());
    deviceStatsResponse.stats.getdCon().dur = dailyStatsDO.deviceConnectionDuration;
    deviceStatsResponse.stats.getdCon().stat = dailyStatsDO.deviceConnectionStatus;
    deviceStatsResponse.stats.getdCon().setnAlrms(dailyStatsDO.deviceConnectionAlarms);
    deviceStatsResponse.stats.getdCon().time = dailyStatsDO.deviceConnectionTime;

    deviceStatsResponse.stats.batt = new BatteryStats();
    deviceStatsResponse.stats.batt.stat = dailyStatsDO.batteryStatus;
    deviceStatsResponse.stats.batt.setnAlrms(dailyStatsDO.batteryAlarms);
    deviceStatsResponse.stats.batt.time = dailyStatsDO.batteryTime;
    deviceStatsResponse.stats.batt.actv = dailyStatsDO.batteryActualVolt;
    deviceStatsResponse.stats.batt.lowv = dailyStatsDO.batteryLowVolt;
    deviceStatsResponse.stats.batt.highv = dailyStatsDO.batteryHighVolt;
    deviceStatsResponse.stats.batt.chgt = dailyStatsDO.batteryChargingTime;
    deviceStatsResponse.stats.batt.wdur = dailyStatsDO.batteryWarningDuration;
    deviceStatsResponse.stats.batt.adur = dailyStatsDO.batteryAlarmDuration;
    deviceStatsResponse.stats.batt.pwrt = dailyStatsDO.powerAvailableTime;

    deviceStatsResponse.stats.comm = new CommunicationStats();
    deviceStatsResponse.stats.comm.setnSMS(dailyStatsDO.numberOfSmsSent);
    deviceStatsResponse.stats.comm.setnPsh(dailyStatsDO.numberOfInternetPushes);
    deviceStatsResponse.stats.comm.setnErr(dailyStatsDO.numberOfInternetFailures);

    deviceStatsResponse.stats.str = new StorageStats();
    deviceStatsResponse.stats.str.dsk = dailyStatsDO.availableDiskSpace;
    deviceStatsResponse.stats.str.ntmp = dailyStatsDO.numberOfTempCached;
    deviceStatsResponse.stats.str.ndvc = dailyStatsDO.numberOfDVCCached;

    deviceStatsResponse.stats.errs = new ArrayList<>();
    for (com.logistimo.db.DailyStatsDeviceError dailyStatsDeviceError : dailyStatsDeviceErrorList) {
      DailyStatsDeviceError dailyStatsDeviceErrorRequest = new DailyStatsDeviceError();
      dailyStatsDeviceErrorRequest.cnt = dailyStatsDeviceError.count;
      dailyStatsDeviceErrorRequest.code = dailyStatsDeviceError.errorCode;
      dailyStatsDeviceErrorRequest.time = dailyStatsDeviceError.time;

      deviceStatsResponse.stats.errs.add(dailyStatsDeviceErrorRequest);
    }

    return deviceStatsResponse;
  }

  private com.logistimo.db.DailyStatsDeviceError toDailyStatsDeviceError(
      DailyStatsDeviceError dailyStatsDeviceErrorRequest) {
    com.logistimo.db.DailyStatsDeviceError
        dailyStatsDeviceError =
        new com.logistimo.db.DailyStatsDeviceError();

    dailyStatsDeviceError.errorCode = dailyStatsDeviceErrorRequest.code;
    dailyStatsDeviceError.time = dailyStatsDeviceErrorRequest.time;
    dailyStatsDeviceError.count = dailyStatsDeviceErrorRequest.cnt;

    return dailyStatsDeviceError;
  }
}

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

package com.logistimo.jobs;

import com.logistimo.db.DailyAggregation;
import com.logistimo.db.Device;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import play.Logger;
import play.db.jpa.JPA;

public class DailyAggregationJob implements Job {
  private static final Logger.ALogger LOGGER = Logger.of(DailyAggregationJob.class);

  private final String
      SQL =
      "select ex.device_id, abs(COALESCE((durationex - durationinc), (durationex-?1))) as duration"
          +
          " FROM (" +
          " select device_id, sum(timeofreading) as durationex" +
          " from temperature_readings" +
          " where type = 2" +
          " and timeofreading < ?2 and timeofreading > ?3" +
          " group by device_id order by device_id) AS ex" +
          " LEFT OUTER JOIN (" +
          " select device_id, sum(timeofreading) as durationinc" +
          " from temperature_readings" +
          " where type = 1" +
          " and timeofreading < ?4 and timeofreading > ?5" +
          " group by device_id order by device_id) AS inc" +
          " ON ex.device_id = inc.device_id" +
          " order by device_id";

  private void doJob() {
    LOGGER.info("Duration Aggregator job ...");
    Calendar cal = Calendar.getInstance();
    long today = cal.getTimeInMillis();
    cal.add(Calendar.DATE, -1);
    long yesterday = cal.getTimeInMillis() / 1000;
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH);
    int day = cal.get(Calendar.DAY_OF_MONTH);

    Query q = JPA.em().createNativeQuery(SQL);
    q.setParameter(1, yesterday);
    q.setParameter(2, today);
    q.setParameter(3, yesterday);
    q.setParameter(4, today);
    q.setParameter(5, yesterday);
    List<Object[]> result = q.getResultList();
    if (result != null && result.size() > 0) {
      for (Object[] o : result) {
        Long deviceId = new BigInteger(o[0].toString()).longValue();
        long duration = new BigInteger(o[1].toString()).longValue();
        Device d = Device.findById(deviceId);
        try {
          DailyAggregation
              agg =
              DailyAggregation.findbyYearAndMonthAndDayAndDevice(year, month, day, d);
          agg.durationOutOfRange += duration;
          agg.update();
        } catch (NoResultException e) {
          DailyAggregation agg = new DailyAggregation();
          agg.year = year;
          agg.month = month;
          agg.day = day;
          agg.device = d;
          agg.durationOutOfRange = duration;

          agg.save();
        }
      }
      LOGGER.info(result.size() + " aggregations done...");
    } else {
      LOGGER.info("Nothing to aggregate....");
    }
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      JPA.withTransaction(new play.libs.F.Function0<Void>() {
        public Void apply() throws Throwable {
          doJob();
          return null;
        }
      });
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }
}

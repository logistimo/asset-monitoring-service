package com.logistimo.healthcheck;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Created by kumargaurav on 21/12/17.
 */
public class MetricsUtil {

  private static final MetricRegistry _metrics = new MetricRegistry();

  public static MetricRegistry getRegistry() {
    return _metrics;
  }

  public static Timer getTimer(Class clazz, String key) {
    return _metrics.timer(MetricRegistry.name(clazz, key));
  }

  public static Meter getMeter(Class clazz, String key) {
    Meter
        meter =
        _metrics.getMeters() != null ? _metrics.getMeters().get(MetricRegistry.name(clazz, key))
            : null;
    if (meter == null) {
      return _metrics.meter(MetricRegistry.name(clazz, key));
    }
    return meter;
  }
}
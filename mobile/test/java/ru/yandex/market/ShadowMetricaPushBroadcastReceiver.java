package ru.yandex.market;

import com.yandex.metrica.push.core.notification.MetricaPushBroadcastReceiver;

import org.robolectric.annotation.Implements;

/**
 * MetricaPushBroadcastReceiver throws RuntimeException in constructor - this break all robolectric-tests
 */
@Implements(MetricaPushBroadcastReceiver.class)
public class ShadowMetricaPushBroadcastReceiver {

}

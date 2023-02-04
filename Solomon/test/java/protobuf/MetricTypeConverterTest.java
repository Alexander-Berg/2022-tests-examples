package ru.yandex.solomon.model.protobuf;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


/**
 * @author Sergey Polovko
 */
public class MetricTypeConverterTest {

    @Test
    public void toProto() {
        for (ru.yandex.monlib.metrics.MetricType type : ru.yandex.monlib.metrics.MetricType.values()) {
            ru.yandex.solomon.model.protobuf.MetricType typeProto = MetricTypeConverter.toProto(type);
            if (type == MetricType.UNKNOWN) {
                assertEquals("invalid conversion of " + type, ru.yandex.solomon.model.protobuf.MetricType.METRIC_TYPE_UNSPECIFIED, typeProto);
            } else {
                assertNotEquals("invalid conversion of " + type, ru.yandex.solomon.model.protobuf.MetricType.METRIC_TYPE_UNSPECIFIED, typeProto);
                assertEquals("invalid name for " + type, type.name(), typeProto.name());
            }

            // must never converted into UNRECOGNIZED
            assertNotEquals("invalid conversion of " + type, ru.yandex.solomon.model.protobuf.MetricType.UNRECOGNIZED, typeProto);
        }
    }

    @Test
    public void fromProto() {
        ImmutableSet<ru.yandex.solomon.model.protobuf.MetricType> ignoredTypes = ImmutableSet.of(
            ru.yandex.solomon.model.protobuf.MetricType.METRIC_TYPE_UNSPECIFIED,
            ru.yandex.solomon.model.protobuf.MetricType.UNRECOGNIZED);

        for (ru.yandex.solomon.model.protobuf.MetricType typeProto : ru.yandex.solomon.model.protobuf.MetricType.values()) {
            ru.yandex.monlib.metrics.MetricType type = MetricTypeConverter.fromProto(typeProto);
            if (ignoredTypes.contains(typeProto)) {
                assertEquals("invalid conversion of " + typeProto, ru.yandex.monlib.metrics.MetricType.UNKNOWN, type);
            } else {
                assertNotEquals("invalid conversion of " + typeProto, ru.yandex.monlib.metrics.MetricType.UNKNOWN, type);
                assertEquals("invalid name for " + typeProto, typeProto.name(), type.name());
            }
        }
    }
}

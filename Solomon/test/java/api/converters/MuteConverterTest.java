package ru.yandex.solomon.alert.api.converters;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;
import ru.yandex.solomon.alert.mute.domain.MuteType;
import ru.yandex.solomon.alert.rule.AlertMuteStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.mute.domain.MuteTestSupport.randomMute;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class MuteConverterTest {
    private ThreadLocalRandom random;
    private MuteConverter converter;

    @Parameterized.Parameter
    public MuteType type;

    @Parameterized.Parameters(name = "{0}")
    public static MuteType[] configs() {
        return MuteType.values();
    }

    @Before
    public void setUp() throws Exception {
        random = ThreadLocalRandom.current();
        converter = MuteConverter.INSTANCE;
    }

    @Test
    public void toProtoAndBack() {
        Mute expected = randomMute(random, type);
        Mute converted =
                converter.protoToMute(converter.muteToProto(expected, MuteStatus.ACTIVE));
        compare(expected, converted);
    }

    private void compare(Object expected, Object converted) {
        boolean areEquals = EqualsBuilder.reflectionEquals(expected, converted);
        assertThat("Original: " + expected + ", \n                         Converted: " + converted, areEquals, equalTo(true));
    }

    @Test
    public void alertMuteStatusRoundTrip() {
        for (var status : AlertMuteStatus.MuteStatusCode.values()) {
            var proto = MuteConverter.alertMuteStatusCodeToProto(status);
            var statusBack = MuteConverter.protoToAlertMuteStatusCode(proto);
            assertSame(status, statusBack);
        }
    }

    @Test
    public void muteStatusRoundTrip() {
        for (var status : MuteStatus.values()) {
            var proto = MuteConverter.muteStatusCodeToProto(status);
            var statusBack = MuteConverter.protoToMuteStatusCode(proto);
            assertSame(status, statusBack);
        }
    }
}

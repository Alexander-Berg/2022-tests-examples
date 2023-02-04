package ru.yandex.qe.bus.features.log;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.qe.spring.profiles.Profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

/**
 * Established by terry
 * on 04.02.14.
 */
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath*:spring/bus.xml", "classpath:spring/qe-plugin-spring.xml"})
@ExtendWith(SpringExtension.class)
public class SecureSessionCookiesTest {

    @Inject
    private LogFeature logFeature;

    @Test
    public void check_replace_in_the_middle() {
        logFeature.setTraceLevel(true);

        final Message mockedMessage = Mockito.mock(Message.class);
        final Map<String, List<String>> headers = Maps.newHashMap();
        headers.put("Cookie", Collections.singletonList("yandexuid=9857935251387366724; csrftoken=61d4d4a1c1c8514785736d30e06a891f; _sandbox_session=BAh7CUkiD3Nlc3Npb25faWQGOgZFRkkiJTdiYTRkMTQ1YWM3ZmNiMmNmMTA5MWMwYTdjMjVhM2JjBjsAVEkiEF9jc3JmX3Rva2VuBjsARkkiMU1IYUxKd3JNcVo5QWpaZmMzYS8wTTV2d20xTUhBL25vZzlXYVIxYWJRcmc9BjsARkkiFGN1cnJlbnRfdXNlcl9pZAY7AEZJIgp0ZXJyeQY7AFRJIhdjdXJyZW50X3VzZXJfbGV2ZWwGOwBGOgphZG1pbg%3D%3D--2ad3c7fcb2384b43487a0c8f2462db5aa2e59046; ys=udn.dGVycnk=; yp=1706798035.udn.dGVycnk=; yandex_login=terry; sessionid2=2:1391438035.-285.5.1120000000000194.8:1391438035440:1297613783:56.1.1.1.0.51462.6066.caa50dea3f46f7619f0d747757d031bd; my=YzYBAQA=; Session_id=2:1391438035.-285.5.1120000000000194.8:1391438035440:1297613783:56.0.1.1.0.51462.8442.bd89dde5a627b20825bdb26daa8eb413; qb-theme=dark"));
        when(mockedMessage.containsKey(Message.PROTOCOL_HEADERS)).thenReturn(true);
        when(mockedMessage.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

        final StringBuilder builder = logFeature.preConstructLogLine(mockedMessage, "req-id");
        assertThat(builder.toString(), equalTo("headers: {Cookie=[yandexuid=***; csrftoken=***; _sandbox_session=***; ys=***; yp=***; yandex_login=***; sessionid2=***; my=***; Session_id=***; qb-theme=***]}; "));
    }

    @Test
    public void check_replace_at_the_end() {
        logFeature.setTraceLevel(true);

        final Message mockedMessage = Mockito.mock(Message.class);
        final Map<String, List<String>> headers = Maps.newHashMap();
        headers.put("Cookie", Collections.singletonList("yandexuid=9857935251387366724; csrftoken=61d4d4a1c1c8514785736d30e06a891f; _sandbox_session=BAh7CUkiD3Nlc3Npb25faWQGOgZFRkkiJTdiYTRkMTQ1YWM3ZmNiMmNmMTA5MWMwYTdjMjVhM2JjBjsAVEkiEF9jc3JmX3Rva2VuBjsARkkiMU1IYUxKd3JNcVo5QWpaZmMzYS8wTTV2d20xTUhBL25vZzlXYVIxYWJRcmc9BjsARkkiFGN1cnJlbnRfdXNlcl9pZAY7AEZJIgp0ZXJyeQY7AFRJIhdjdXJyZW50X3VzZXJfbGV2ZWwGOwBGOgphZG1pbg%3D%3D--2ad3c7fcb2384b43487a0c8f2462db5aa2e59046; ys=udn.dGVycnk=; yp=1706798035.udn.dGVycnk=; yandex_login=terry; sessionid2=2:1391438035.-285.5.1120000000000194.8:1391438035440:1297613783:56.1.1.1.0.51462.6066.caa50dea3f46f7619f0d747757d031bd; my=YzYBAQA=; Session_id=2:1391438035.-285.5.1120000000000194.8:1391438035440:1297613783:56.0.1.1.0.51462.8442.bd89dde5a627b20825bdb26daa8eb413"));
        when(mockedMessage.containsKey(Message.PROTOCOL_HEADERS)).thenReturn(true);
        when(mockedMessage.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

        final StringBuilder builder = logFeature.preConstructLogLine(mockedMessage, "req-id");
        assertThat(builder.toString(), equalTo("headers: {Cookie=[yandexuid=***; csrftoken=***; _sandbox_session=***; ys=***; yp=***; yandex_login=***; sessionid2=***; my=***; Session_id=***]}; "));
    }
}

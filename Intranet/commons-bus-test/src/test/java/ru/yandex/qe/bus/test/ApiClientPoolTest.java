package ru.yandex.qe.bus.test;

import java.util.Map;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.qe.bus.api.ApiService;
import ru.yandex.qe.bus.factories.client.BusClientSet;
import ru.yandex.qe.spring.profiles.Profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Established by terry
 * on 30.01.14.
 */
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath*:spring/qe-plugin-spring.xml", "classpath:spring/bus-clients.xml"})
public class ApiClientPoolTest extends AbstractBusJUnit5SpringContextTests {

    @Resource(name = "apiClientsPool")
    private BusClientSet<ApiService> pool;


    @Test
    public void api_invocation_works() {
        final Map<String, ApiService> address2Client = pool.getAddress2Client();
        assertThat(address2Client.size(), equalTo(1));

        final ApiService serverService = getMockServerService(ApiService.class);
        doReturn("login").when(serverService).getLogin(anyString());

        for (ApiService apiService : address2Client.values()) {
            assertThat(apiService.getLogin("login"), equalTo("login"));
        }

        verify(serverService, times(1)).getLogin("login");
        verifyNoMoreInteractions(serverService);
    }
}

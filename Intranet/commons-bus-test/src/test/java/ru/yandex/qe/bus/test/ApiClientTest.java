package ru.yandex.qe.bus.test;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.qe.bus.MediaTypeConstants;
import ru.yandex.qe.bus.api.ApiService;
import ru.yandex.qe.spring.profiles.Profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath*:spring/qe-plugin-spring.xml", "classpath:spring/bus-client.xml"})
public class ApiClientTest extends AbstractBusJUnit5SpringContextTests {

    @Inject @Named("apiClientService")
    private ApiService apiService;

    @Test
    public void api_invocation_works() {
        assertNotNull(applicationContext);
        final ApiService serverService = getMockServerService(ApiService.class);
        doReturn("login").when(serverService).getLogin(anyString());

        assertThat(apiService.getLogin("login"), equalTo("login"));

        createWebClient(apiService).
                accept(MediaTypeConstants.APPLICATION_JSON_WITH_UTF_TYPE).path("/test/x/login").get();

        verify(serverService, times(2)).getLogin("login");
        verifyNoMoreInteractions(serverService);
    }

}

package ru.auto.tests.redirect.desktopreviews;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import okhttp3.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.redirect.RedirectClientModule;
import ru.auto.tests.redirect.categories.DesktopToDesktop;
import ru.auto.tests.redirect.steps.RedirectSteps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.redirect.constant.Features.REDIRECT;
import static ru.auto.tests.redirect.constant.Owners.TIMONDL;

@DisplayName("Редиректы desktop -> desktop")
@Feature(REDIRECT)
@RunWith(Parameterized.class)
@GuiceModules(RedirectClientModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DesktopToDesktopRedirectTest {

    private static final String RESOURCE = "desktopreviews/desktop_to_desktop.csv";
    private static final String SEPARATOR = " ";

    @Inject
    private RedirectSteps redirectSteps;

    @Parameterized.Parameter
    public String urlBeforeRedirect;

    @Parameterized.Parameter(1)
    public String urlAfterRedirect;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static List getParameters() {
        InputStream dataStream = DesktopToDesktopRedirectTest.class.getClassLoader().getResourceAsStream(RESOURCE);
        Stream<String> lines = new BufferedReader(new InputStreamReader(dataStream, UTF_8)).lines();

        return lines.map(s -> s.split(SEPARATOR)).collect(Collectors.toList());
    }

    @Test
    @Owner(TIMONDL)
    @Category({DesktopToDesktop.class})
    @DisplayName("Проверка редиректа")
    public void shouldSeeRedirect() throws IOException {
        Response response = redirectSteps.makeDesktopRequest(urlBeforeRedirect);

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.request().url().toString(), equalTo(urlAfterRedirect));
    }
}

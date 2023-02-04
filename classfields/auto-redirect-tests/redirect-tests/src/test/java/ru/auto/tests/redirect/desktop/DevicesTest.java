package ru.auto.tests.redirect.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import okhttp3.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.auto.tests.redirect.RedirectClientModule;
import ru.auto.tests.redirect.categories.Blocker;
import ru.auto.tests.redirect.categories.DesktopToDesktop;
import ru.auto.tests.redirect.steps.RedirectSteps;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.redirect.constant.Features.REDIRECT;
import static ru.auto.tests.redirect.constant.Owners.DSVICHIHIN;

@DisplayName("Проверка редиректов для разных девайсов")
@Feature(REDIRECT)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RedirectClientModule.class)
public class DevicesTest {

    @Inject
    private RedirectSteps redirectSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Blocker.class, DesktopToDesktop.class})
    @DisplayName("Редирект для Ipad")
    public void shouldRedirectForIpad() throws IOException {
        Response response = redirectSteps.makeDesktopRequest("https://auto.ru/", "",
                "Mozilla/5.0 (iPad; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");

        assertThat(response.request().url().toString(), equalTo("https://auto.ru/?cookiesync=true"));
    }
}
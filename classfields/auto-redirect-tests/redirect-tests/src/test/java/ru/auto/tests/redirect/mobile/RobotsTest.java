package ru.auto.tests.redirect.mobile;

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
import ru.auto.tests.redirect.categories.MobileToMobile;
import ru.auto.tests.redirect.steps.RedirectSteps;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.redirect.constant.Features.REDIRECT;
import static ru.auto.tests.redirect.constant.Owners.DSVICHIHIN;

@DisplayName("Проверка редиректов для роботов")
@Feature(REDIRECT)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RedirectClientModule.class)
public class RobotsTest {

    @Inject
    private RedirectSteps redirectSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Blocker.class, MobileToMobile.class})
    @DisplayName("Редирект для роботов")
    public void shouldRedirectForRobots() throws IOException {
        Response response = redirectSteps.makeMobileRequest("https://m.auto.ru/",
                "gids=213;",
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");

        assertThat(response.request().url().toString(), equalTo("https://m.auto.ru/"));
    }
}
package ru.yandex.webmaster.tests;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Link("https://st.yandex-team.ru/VERTISTEST-2054")
@DisplayName("Добавление фидов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DefaultModule.class)
public class WebmasterTest {

    private static final String AQUA_LOGIN_URL = "http://aqua.yandex-team.ru/auth.html?host=https://" +
            "passport.yandex.ru/passport&mode=auth&login=%s&passwd=%s";

    private List<String> feedsAdded = newArrayList();
    private List<String> feedsToAdd;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private WebDriverManager webDriverManager;

    @Inject
    private WebMasterConfig config;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Before
    public void before() {
        webDriverManager.getDriver().get(format(AQUA_LOGIN_URL, config.wmLogin(), config.wmPassword()));
        webDriverManager.getDriver().get(config.wmUrl());
    }

    @Test
    @DisplayName("Загружаем фиды")
    public void shouldAddFeeds() {
        webDriverManager.getDriver().manage().window().fullscreen();
        fillFeedsAdded();
        detectNewFeeds();
        onWmPage().popupCloseCross().clickIf(isDisplayed());
        onWmPage().popupCloseCross().should(not(isDisplayed()));
        feedsToAdd.forEach(f -> {
            onWmPage().addButton().waitUntil(isDisplayed(), 20).click();
            onWmPage().region().click();
            onWmPage().russiaRegionCheckbox().click();
            onWmPage().region().click();
            onWmPage().russiaRegionCheckbox().waitUntil(not(isDisplayed()), 5);
            onWmPage().inputFeeds().sendKeys(f);
            onWmPage().doneButton().waitUntil(isEnabled()).click();
            onWmPage().doneButton().waitUntil(not(isDisplayed()), 15);
        });
    }

    private void fillFeedsAdded() {
        onWmPage().addButton().waitUntil(isDisplayed(), 20);
        int size = onWmPage().rows().waitUntil(hasSize(greaterThan(0))).size();
        for (int i = 0; i < size; i++) {
            feedsAdded.add(onWmPage().rows().get(i).urlName().getText());
        }
    }

    private void detectNewFeeds() {
        String content = "";
        try {
            HttpURLConnection connection = ((HttpURLConnection) new URL(config.feedsUrl()).openConnection());
            content = IOUtils.toString(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        feedsToAdd = newArrayList(content.split("\\n"));
        feedsToAdd.removeAll(feedsAdded);
    }

    private WebmasterPage onWmPage() {
        return webDriverSteps.on(WebmasterPage.class);
    }
}

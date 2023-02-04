package ru.yandex.general.feed;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.FeedSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Notifications.FEED_SENT_FOR_PROCESSING;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.page.FeedPage.FAILED;
import static ru.yandex.general.page.FeedPage.SEND;
import static ru.yandex.general.step.FeedSteps.YML_FEED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("YML фид")
@DisplayName("Добавляем YML фид")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class YmlFeedTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FeedSteps feedSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(MY).path(FEED).open();
        feedSteps.addFeed(YML_FEED);
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());
        feedSteps.waitUntilFeedProcessed();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем YML фид с ошибками - статус «Отклонён» после обработки и 2 ошибки")
    public void shouldSeeYmlFeedWithErrorsFailedStatus() {
        feedSteps.onFeedPage().feedStatus().should(hasText(FAILED));
        feedSteps.onFeedPage().errorsTable().get(0).should(hasText(containsString("Добавить адрес")));
        feedSteps.onFeedPage().errorsTable().get(1).should(hasText(containsString("Добавить номер телефона")));
    }

}

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

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Button.ARIA_DISABLED;
import static ru.yandex.general.element.Button.FALSE;
import static ru.yandex.general.element.Button.TRUE;
import static ru.yandex.general.page.FeedPage.FEED_URL_INPUT;
import static ru.yandex.general.page.FeedPage.SEND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Проверка базовых контролов")
@DisplayName("Проверка базовых контролов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedControlsTest {

    private static final String FEED_URL = "https://my-files.su/Save/nspznb/general_test_feed.xml";

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
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» задизейблена изначально")
    public void shouldSeeDisabledSendButton() {
        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» активируется при добавлении фида файлом")
    public void shouldSeeActiveSendButtonAfterAddFileFeed() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());

        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, FALSE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» активируется при добавлении фида по ссылке")
    public void shouldSeeActiveSendButtonAfterAddUrlFeed() {
        feedSteps.onFeedPage().input(FEED_URL_INPUT).sendKeys(FEED_URL);

        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, FALSE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Инпут для ссылки на фид пропадает при добавлении фида файлом")
    public void shouldSeeNoFeedLinkInputAfterAddFileFeed() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());

        feedSteps.onFeedPage().input(FEED_URL_INPUT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем фид добавленный файлом по кнопке «Удалить» внутри аплоадера, форма очищается")
    public void shouldSeeDeleteFileFeedFromUploader() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().deleteInUploader().click();

        feedSteps.onFeedPage().spanLink("Выберите файл").should(isDisplayed());
        feedSteps.onFeedPage().input(FEED_URL_INPUT).should(isDisplayed());
        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

}

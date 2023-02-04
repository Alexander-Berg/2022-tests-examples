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
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Notifications.FEED_DELETED;
import static ru.yandex.general.consts.Notifications.FEED_SAVED;
import static ru.yandex.general.consts.Notifications.FEED_SENT_FOR_PROCESSING;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Button.ARIA_DISABLED;
import static ru.yandex.general.element.Button.FALSE;
import static ru.yandex.general.element.Button.TRUE;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.FeedPage.DELETE;
import static ru.yandex.general.page.FeedPage.EDIT;
import static ru.yandex.general.page.FeedPage.FEED_URL_INPUT;
import static ru.yandex.general.page.FeedPage.FILE;
import static ru.yandex.general.page.FeedPage.PROCESSING;
import static ru.yandex.general.page.FeedPage.SEND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Тесты с добавлением фида")
@DisplayName("Тесты с добавлением фида")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedUploadTest {

    private static final String FEED_URL = "https://my-files.su/Save/nspznb/general_test_feed.xml";
    private static final String S3_DIRECTORY_FEED_URL = "https://vertis-feeds.s3.yandex.net/general_one_shot/";

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фид файлом")
    public void shouldSeeUploadFileFeed() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());

        feedSteps.onFeedPage().feedStatus().should(hasText(PROCESSING));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фид по ссылке")
    public void shouldSeeUploadUrlFeed() {
        feedSteps.onFeedPage().input(FEED_URL_INPUT).sendKeys(FEED_URL);
        feedSteps.onFeedPage().button(SEND).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());

        feedSteps.onFeedPage().feedStatus().should(hasText(PROCESSING));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем добавленный файлом фид на фид по ссылке")
    public void shouldSeeEditFileUploadedFeedToUrlFeed() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(EDIT).click();
        feedSteps.onFeedPage().modal().input(FEED_URL_INPUT).sendKeys(FEED_URL);
        feedSteps.onFeedPage().modal().button(SEND).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().modal().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SAVED).waitUntil(isDisplayed());

        feedSteps.onFeedPage().feedStatus().should(hasText(PROCESSING));
        feedSteps.onFeedPage().link(FILE).should(hasAttribute(HREF, FEED_URL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем добавленный по ссылке фид на фид файлом")
    public void shouldSeeEditUrlUploadedFeedToFileFeed() {
        feedSteps.onFeedPage().input(FEED_URL_INPUT).sendKeys(FEED_URL);
        feedSteps.onFeedPage().button(SEND).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(EDIT).click();
        feedSteps.addFeedExampleInModal();
        feedSteps.onFeedPage().modal().button(SEND).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().modal().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SAVED).waitUntil(isDisplayed());

        feedSteps.onFeedPage().feedStatus().should(hasText(PROCESSING));
        feedSteps.onFeedPage().link(FILE).should(hasAttribute(HREF, containsString(S3_DIRECTORY_FEED_URL)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем фид")
    public void shouldSeeDeleteFeed() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(DELETE).click();
        feedSteps.onFeedPage().modal().button(DELETE).click();
        feedSteps.onFeedPage().notificationWithText(FEED_DELETED).waitUntil(isDisplayed());

        feedSteps.onFeedPage().spanLink("Выберите файл").should(isDisplayed());
        feedSteps.onFeedPage().input(FEED_URL_INPUT).should(isDisplayed());
        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, TRUE));
        feedSteps.onFeedPage().feedStatus().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отменяем удаление фида")
    public void shouldSeeDeleteFeedCancel() {
        feedSteps.addFeedExample();
        feedSteps.onFeedPage().iconSuccess().waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(SEND).click();
        feedSteps.onFeedPage().notificationWithText(FEED_SENT_FOR_PROCESSING).waitUntil(isDisplayed());
        feedSteps.onFeedPage().button(DELETE).click();
        feedSteps.onFeedPage().modal().button("Отмена").click();

        feedSteps.onFeedPage().feedStatus().should(hasText(PROCESSING));
        feedSteps.onFeedPage().modal().should(not(isDisplayed()));
    }

}

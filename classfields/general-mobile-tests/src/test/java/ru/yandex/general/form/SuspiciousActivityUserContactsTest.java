package ru.yandex.general.form;

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
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.DRAFT_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SUSPICIOUS_ACTIVITY;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.element.SuspiciousActivityContacts.SUSPICIOUS_ACTIVITY_TEXT;
import static ru.yandex.general.mobile.element.SuspiciousActivityContacts.SUSPICIOUS_ACTIVITY_TITLE;
import static ru.yandex.general.mock.MockCurrentDraft.SUSPICIOUS_ACTIVITY_CONTACTS;
import static ru.yandex.general.mock.MockCurrentDraft.mockCurrentDraft;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(DRAFT_FEATURE)
@Feature(SUSPICIOUS_ACTIVITY)
@DisplayName("Блок «Подозрительная активность» в разделе контактов на черновике")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SuspiciousActivityUserContactsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentDraft(mockCurrentDraft(SUSPICIOUS_ACTIVITY_CONTACTS).build())
                .setCategoriesTemplate()
                .setCategoryTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();

        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подозрительная активность в контактах. Тайтл.")
    public void shouldSeeSuspiciousActivityContactsTitle() {
        offerAddSteps.onFormPage().suspiciousActivityContacts().title().should(hasText(SUSPICIOUS_ACTIVITY_TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подозрительная активность в контактах. Текст.")
    public void shouldSeeSuspiciousActivityContactsText() {
        offerAddSteps.onFormPage().suspiciousActivityContacts().text().should(hasText(SUSPICIOUS_ACTIVITY_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подозрительная активность в контактах. Ссылка в кнопке.")
    public void shouldSeeSuspiciousActivityContactsLinkInButton() {
        offerAddSteps.onFormPage().suspiciousActivityContacts().knowMore().should(
                hasAttribute(HREF, urlSteps.getPassportUrl() + "passport?mode=userapprove"));
    }

}

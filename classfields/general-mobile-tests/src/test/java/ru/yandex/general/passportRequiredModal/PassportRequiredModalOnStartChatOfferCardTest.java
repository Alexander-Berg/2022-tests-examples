package ru.yandex.general.passportRequiredModal;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.PASSPORT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEXANDERREX;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.page.OfferCardPage.WRITE;
import static ru.yandex.general.mock.MockCard.ANY;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.CHAT;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PASSPORT_FEATURE)
@DisplayName("Отображение модалки паспортного логина при открытии чата на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class PassportRequiredModalOnStartChatOfferCardTest {

    private static final String ID = "12345";

    private MockResponse mockResponse = mockResponse().setCategoriesTemplate().setRegionsTemplate();
    private MockCard mockCard = mockCard(BASIC_CARD).setIsOwner(false);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEXANDERREX)
    @DisplayName("Модалка паспортного логина при открытии чата (Только «Написать»)")
    public void shouldSeePassportModalFromWriteOnChatContacts() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(CHAT).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().button(WRITE).click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

    @Test
    @Owner(ALEXANDERREX)
    @DisplayName("Модалка паспортного логина при открытии чата (Оба способа связи)")
    public void shouldSeePassportModalFromWriteOnAnyContacts() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().button(WRITE).click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }
}

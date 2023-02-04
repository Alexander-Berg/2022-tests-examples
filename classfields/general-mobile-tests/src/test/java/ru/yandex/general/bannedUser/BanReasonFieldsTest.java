package ru.yandex.general.bannedUser;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.BANNED_USER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.FRAUD;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.SPAM;
import static ru.yandex.general.mobile.element.BanReason.FIRST;
import static ru.yandex.general.mobile.element.BanReason.SECOND;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(BANNED_USER_FEATURE)
@DisplayName("Аллерт забаненного юзера в ЛК, проверка полей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class BanReasonFieldsTest {

    private static final String SUPPORT_LINK = "https://yandex.ru/chat#/user/2d001e07-0165-9004-5972-3c2857a2ac80";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл в аллерте бана юзера, 1 причина бана")
    public void shouldSeeBanMessageTitle1Reason() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().title().should(hasText("Личный кабинет заблокирован по 1 причине"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл в аллерте бана юзера, 2 причины бана")
    public void shouldSeeBanMessageTitle2Reason() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD, SPAM).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().title().should(hasText("Личный кабинет заблокирован по 2 причинам"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл причины бана юзера, 1 причина бана")
    public void shouldSeeReasonTitle() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().banReasons().get(FIRST).title()
                .should(hasText(FRAUD.getTitle()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст причины бана юзера, 1 причина бана")
    public void shouldSeeReasonDescription() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().banReasons().get(FIRST).description()
                .should(hasText(FRAUD.getTextHtml()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл второй причины бана юзера, 2 причины бана")
    public void shouldSeeSecondReasonTitle() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD, SPAM).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().banReasons().get(SECOND).title()
                .should(hasText(SPAM.getTitle()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст второй причины бана юзера, 2 причины бана")
    public void shouldSeeSecondReasonDescriptions() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD, SPAM).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().banReasons().get(SECOND).description()
                .should(hasText(SPAM.getTextHtml()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на чат с саппортом")
    public void shouldSeeBanMessageChatWithSupportLink() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().banMessage().chatWithSupport().link().should(hasAttribute(HREF, SUPPORT_LINK));
    }

}

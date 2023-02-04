package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_PHOTO;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.Sidebar.CHAT_WITH_SUPPORT;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение контролов")
@DisplayName("Чат с поддержкой на забаненном оффере")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardBannedChatWithSupportTest {

    private static final String ID = "12345";
    private static final String SUPPORT_LINK = "https://yandex.ru/chat#/user/2d001e07-0165-9004-5972-3c2857a2ac80";

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

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameters(name = "{index}. Оффер забаненный «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"По 1 причине", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(WRONG_OFFER_CATEGORY).setBannedOfferAvaliableActions()},
                {"По 2 причинам", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(WRONG_OFFER_CATEGORY, WRONG_PHOTO).setBannedOfferAvaliableActions()},
                {"По 1 причине без возможности редактирования", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM).setBannedNoEditOfferAvaliableActions()}

        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(true).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в кнопке «Чат с поддержкой на забаненном оффере»")
    public void shouldSeeChatWithSupportLink() {
        basePageSteps.onOfferCardPage().sidebar().link(CHAT_WITH_SUPPORT).should(hasAttribute(HREF, SUPPORT_LINK));
    }

}

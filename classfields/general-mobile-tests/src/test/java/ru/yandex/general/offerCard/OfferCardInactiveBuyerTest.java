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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.OfferCardMessage.SOLD_TEXT_BUYER;
import static ru.yandex.general.element.OfferCardMessage.SOLD_TITLE_BUYER;
import static ru.yandex.general.mobile.page.OfferCardPage.FULL_DESCRIPTION;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.MESSAGE_DEFAULT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Неактивный оффер для покупателя")
@DisplayName("Отображение сообщения и бейджа неактивного оффера, с разными причинами неактивности")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardInactiveBuyerTest {

    private static final String ID = "12345";

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
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Неактивный оффер по причине «Не удалось дозвониться» для продавца", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setCantCallInactiveReason()},
                {"Неактивный оффер для продавца причина «Продал на Яндексе»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_ON_YANDEX.getMockValue())},
                {"Неактивный оффер для продавца причина «Продал в другом месте»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_SOMEWHERE.getMockValue())},
                {"Неактивный оффер для продавца причина «Передумал продавать»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(RETHINK.getMockValue())},
                {"Неактивный оффер для продавца причина «Другая причина»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(OTHER.getMockValue())}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard.setIsOwner(false).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение сообщения неактивного оффера, с разными причинами неактивности")
    public void shouldSeeInactiveOfferMessage() {
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message(MESSAGE_DEFAULT).title().should(hasText(SOLD_TITLE_BUYER));
        basePageSteps.onOfferCardPage().message(MESSAGE_DEFAULT).text().should(hasText(SOLD_TEXT_BUYER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается часть контролов на свернутой неактивной карточке")
    public void shouldSeeInactiveOfferControlsBeforeMaximization() {
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().description().should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().firstAttribute().should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().address().should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().sellerInfo().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разворачиваем неактивную карточку")
    public void shouldSeeInactiveOfferMaximization() {
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().button(FULL_DESCRIPTION).click();

        basePageSteps.onOfferCardPage().description().should(isDisplayed());
        basePageSteps.onOfferCardPage().firstAttribute().should(isDisplayed());
        basePageSteps.onOfferCardPage().address().should(isDisplayed());
        basePageSteps.onOfferCardPage().sellerInfo().should(isDisplayed());
        basePageSteps.onOfferCardPage().button(FULL_DESCRIPTION).should(not(isDisplayed()));
    }

}

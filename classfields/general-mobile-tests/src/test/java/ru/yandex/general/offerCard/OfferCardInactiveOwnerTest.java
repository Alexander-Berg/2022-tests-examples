package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TEXT;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TITLE;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.OfferCardMessage.ENDED_TEXT;
import static ru.yandex.general.element.OfferCardMessage.ENDED_TITLE;
import static ru.yandex.general.element.OfferCardMessage.SOLD_TEXT_OWNER;
import static ru.yandex.general.element.OfferCardMessage.SOLD_TITLE_OWNER;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.EDIT;
import static ru.yandex.general.page.OfferCardPage.MESSAGE_DEFAULT;
import static ru.yandex.general.page.OfferCardPage.MESSAGE_ERROR;
import static ru.yandex.general.page.OfferCardPage.MESSAGE_EXPIRED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Неактивный оффер для продавца")
@DisplayName("Отображение сообщения неактивного оффера, с разными причинами неактивности")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardInactiveOwnerTest {

    private static final String ID = "12345";
    private static final String EDIT_FORM_LINK = "/form/86050276474376192/";

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameter(2)
    public String messageTitle;

    @Parameterized.Parameter(3)
    public String messageText;

    @Parameterized.Parameter(4)
    public String messageType;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Неактивный оффер по причине «Не удалось дозвониться» для продавца", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setCantCallInactiveReason().setEditFormLink(EDIT_FORM_LINK),
                        CANT_CALL_REASON_TITLE, CANT_CALL_REASON_TEXT, MESSAGE_ERROR},
                {"Неактивный оффер для продавца причина «Продал на Яндексе»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_ON_YANDEX.getMockValue())
                        .setEditFormLink(EDIT_FORM_LINK),
                        SOLD_TITLE_OWNER, SOLD_TEXT_OWNER, MESSAGE_DEFAULT},
                {"Неактивный оффер для продавца причина «Продал в другом месте»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_SOMEWHERE.getMockValue())
                        .setEditFormLink(EDIT_FORM_LINK),
                        SOLD_TITLE_OWNER, SOLD_TEXT_OWNER, MESSAGE_DEFAULT},
                {"Неактивный оффер для продавца причина «Передумал продавать»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(RETHINK.getMockValue())
                        .setEditFormLink(EDIT_FORM_LINK),
                        ENDED_TITLE, ENDED_TEXT, MESSAGE_EXPIRED},
                {"Неактивный оффер для продавца причина «Другая причина»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(OTHER.getMockValue())
                        .setEditFormLink(EDIT_FORM_LINK),
                        ENDED_TITLE, ENDED_TEXT, MESSAGE_EXPIRED}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение сообщения неактивного оффера, с разными причинами неактивности")
    public void shouldSeeInactiveOfferMessage() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard.setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message(messageType).title().should(hasText(messageTitle));
        basePageSteps.onOfferCardPage().message(messageType).text().should(hasText(messageText));
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Редактировать» неактивного оффера, с разными причинами неактивности")
    public void shouldSeeEditButton() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard.setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().link(EDIT).should(
                hasAttribute(HREF, urlSteps.testing().path(EDIT_FORM_LINK).toString()));
    }

}

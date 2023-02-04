package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_PHOTO;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.OfferCardMessage.COLLAPSE;
import static ru.yandex.general.element.OfferCardMessage.SHOW_MORE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Забаненный оффер с несколькими причинами")
@DisplayName("Забаненный оффер с несколькими причинами")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardBannedReasonsMessagesTest {

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

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается одна причина бана на карточке, добавлено 2, свернутое состояние")
    public void shouldSeeOneBanReasonDisplayedWith2BansCollapsed() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message().banReasons().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается тайтл первой причины бана на карточке, добавлено 2, свернутое состояние")
    public void shouldSeeOneBanReasonDisplayedTitleWith2BansCollapsed() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message().banReasons().get(0).title().should(hasText(SPAM.getTitle()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается дескрипшн первой причины бана на карточке, добавлено 2, свернутое состояние")
    public void shouldSeeOneBanReasonDisplayedDescriptionWith2BansCollapsed() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message().banReasons().get(0).description()
                .should(hasText(SPAM.getReasonNoLinks()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разворачиваем список из двух причин бана")
    public void shouldSee2BanReasonsExpanded() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();

        basePageSteps.onOfferCardPage().message().banReasons().should(hasSize(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается тайтл первой причины бана на карточке, добавлено 2, развернутое состояние")
    public void shouldSee2BanReasonsExpandedFirstReasonTitle() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();

        basePageSteps.onOfferCardPage().message().banReasons().get(0).title().should(hasText(SPAM.getTitle()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается дескрипшн первой причины бана на карточке, добавлено 2, развернутое состояние")
    public void shouldSee2BanReasonsExpandedFirstReasonDescription() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();

        basePageSteps.onOfferCardPage().message().banReasons().get(0).description()
                .should(hasText(SPAM.getReasonNoLinks()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается тайтл второй причины бана на карточке, добавлено 2, развернутое состояние")
    public void shouldSee2BanReasonsExpandedSecondReasonTitle() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();

        basePageSteps.onOfferCardPage().message().banReasons().get(1).title().should(hasText(WRONG_OFFER_CATEGORY.getTitle()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается дескрипшн второй причины бана на карточке, добавлено 2, развернутое состояние")
    public void shouldSee2BanReasonsExpandedSecondReasonDescription() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();

        basePageSteps.onOfferCardPage().message().banReasons().get(1).description()
                .should(hasText(WRONG_OFFER_CATEGORY.getReasonNoLinks()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сворачиваем список из двух причин бана")
    public void shouldSee2BanReasonsCollapsed() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().message().spanLink(format("%s 1", SHOW_MORE)).click();
        basePageSteps.onOfferCardPage().message().banReasons().waitUntil(hasSize(2));
        basePageSteps.onOfferCardPage().message().spanLink(COLLAPSE).click();

        basePageSteps.onOfferCardPage().message().banReasons().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавлено 3 причины бана, отображается «Показать ещё 2»")
    public void shouldSee3More() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY, WRONG_PHOTO).setIsOwner(true).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().message().actionButton().should(hasText(format("%s 2", SHOW_MORE)));
    }

}

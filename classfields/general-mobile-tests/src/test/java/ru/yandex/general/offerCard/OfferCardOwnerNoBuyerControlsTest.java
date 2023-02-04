package ru.yandex.general.offerCard;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.page.OfferCardPage.CALL;
import static ru.yandex.general.mobile.page.OfferCardPage.WRITE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение контролов")
@DisplayName("Не отображаются контролы покупателя у продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardOwnerNoBuyerControlsTest {

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

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setCard(
                mockCard(BASIC_CARD).setIsOwner(true).setStatisticsGraph(7).setVas()
                        .setPreferContactWay("Any").build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();

        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка добавления в избранное на карточке продавца")
    public void shouldNotSeeAddToFavoriteOwnerCard() {
        urlSteps.open();
        basePageSteps.onOfferCardPage().description().hover();

        basePageSteps.onOfferCardPage().addToFavorite().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка шаринга на карточке для продавца")
    public void shouldNotSeeShareOwnerCard() {
        urlSteps.open();
        basePageSteps.onOfferCardPage().description().hover();

        basePageSteps.onOfferCardPage().shareButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Пожаловаться» на карточке для продавца")
    public void shouldNotSeeComplainOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().spanLink(COMPLAIN).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Показать телефон» на карточке для продавца")
    public void shouldNotSeePhoneShowOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().link(CALL).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Написать» на карточке для продавца")
    public void shouldNotSeeChatButtonOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().button(WRITE).should(not(isDisplayed()));
    }

}

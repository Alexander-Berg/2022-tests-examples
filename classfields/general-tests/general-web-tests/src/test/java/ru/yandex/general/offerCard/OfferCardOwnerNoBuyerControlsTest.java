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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение контролов")
@DisplayName("Не отображаются контролы покупателя у продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
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
    @DisplayName("Не отображается блок заметки на карточке продавца")
    public void shouldNotSeeNoticeOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().notice().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка добавления в избранное на карточке продавца")
    public void shouldNotSeeAddToFavoriteOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().addToFavorite().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка шаринга на карточке для продавца")
    public void shouldNotSeeShareOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().shareButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Пожаловаться» на карточке для продавца")
    public void shouldNotSeeComplainOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().button(COMPLAIN).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Показать телефон» на карточке для продавца")
    public void shouldNotSeePhoneShowOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().showPhone().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Написать» на карточке для продавца")
    public void shouldNotSeeChatButtonOwnerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().startChat().should(not(isDisplayed()));
    }

}

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
import static ru.yandex.general.element.MyOfferSnippet.RAISE_UP;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.EDIT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение контролов")
@DisplayName("Не отображаются контролы продавца у покупателя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardBuyerNoOwnerControlsTest {

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
                mockCard(BASIC_CARD).setIsOwner(false).setVas()
                        .setPreferContactWay("Any").build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();

        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается «Поднять в топ» на карточке покупателя")
    public void shouldNotSeeRaiseUpBuyerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().button(RAISE_UP).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается «Редактировать» на карточке покупателя")
    public void shouldNotSeeEditButtonBuyerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().link(EDIT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается контекстное меню на карточке покупателя")
    public void shouldNotSeeMoreButtonBuyerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().more().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кол-во дней до снятия с выдачи на карточке покупателя")
    public void shouldNotSeeDaysUntilExpireBuyerCard() {
        urlSteps.open();

        basePageSteps.onOfferCardPage().daysUntilExpire().should(not(isDisplayed()));
    }

}

package ru.yandex.realty.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.LAND;
import static ru.yandex.realty.consts.OfferAdd.RENT;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;

/**
 * Created by vicdev on 01.06.17.
 */

@DisplayName("Форма добавления объявления. Типы и категории")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void openManagementAddPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
    }

    @Test
    @DisplayName("Кнопка «Участок должна» быть недоступна, если выбрали «Сдать»")
    @Category({Regression.class, Production.class})
    @Owner(VICDEV)
    public void shouldNotRentLotAfterClickRent() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(RENT);
        offerAddSteps.onOfferAddPage().offerType().button(LAND).should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Кнопка «Сдать» должна быть недоступна, если выбрали «Участок»")
    @Category({Regression.class, Production.class})
    @Owner(VICDEV)
    public void shouldNotRentLotAfterClickLot() {
        offerAddSteps.onOfferAddPage().offerType().selectButton(LAND);
        offerAddSteps.onOfferAddPage().dealType().button(RENT).should(not(isDisplayed()));
    }
}

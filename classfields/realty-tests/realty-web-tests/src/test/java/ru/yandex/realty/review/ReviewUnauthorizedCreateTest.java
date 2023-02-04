package ru.yandex.realty.review;

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
import org.openqa.selenium.Dimension;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;

/**
 * Created by kopitsa on 08.08.17.
 */

@DisplayName("Создание отзыва на новостройки для незалогина")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ReviewUnauthorizedCreateTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openNewBuildingSitePage() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        basePageSteps.getDriver().manage().window().setSize(new Dimension(1400, 1800));
        urlSteps.testing().newbuildingSiteMock().open();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Видим попап авторизации")
    public void shouldSeeDomikPopup() {
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().inputField());
        basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().inputField().click();
        basePageSteps.onNewBuildingSitePage().domikPopup().should(isDisplayed());
    }
}

package ru.yandex.realty.newbuilding.flats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.DELIVERY_DATE;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.HOUSE_ID;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Фильтры квартир новостройки.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DeliveryDateFinishedFilterTest {

    public static final String HOUSE_ID_VALUE = "1946964";
    public static final String HOUSE_NAME = "3\u00a0квартал\u00a02022\u00a0•\u00a0Корпус 10";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим корпус зачекан")
    public void shouldSeeHouseIdButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(HOUSE_ID, HOUSE_ID_VALUE).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().filters());
        basePageSteps.onNewBuildingCardPage().filters().button(HOUSE_NAME).should(isDisplayed());
    }
}

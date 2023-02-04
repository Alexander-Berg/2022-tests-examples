package ru.yandex.realty.specproject.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Спецпроект. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DeliveryDateUrlSpecProjectTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим срок сдачи по урлу")
    public void shouldSeeDeliveryButtonFromUrl() {
        urlSteps.testing().path(SAMOLET).queryParam("deliveryDate", "3_2023").open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().deliveryDateButton().should(hasText("До 3 квартала 2023"));
    }
}

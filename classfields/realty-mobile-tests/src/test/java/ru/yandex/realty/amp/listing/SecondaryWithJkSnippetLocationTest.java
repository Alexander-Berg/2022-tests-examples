package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Поиск в ЖК.")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SecondaryWithJkSnippetLocationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @DisplayName("Сниппет жк выше чем сортировки.")
    @Owner(KANTEMIROV)
    public void shouldSeeSortLocation() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path("/zhk-tarmo-549393/").open();
        int snippetLocation = basePageSteps.onAmpSaleAdsPage().ampSiteSnippet().getCoordinates().onPage().getY();
        int sortLocation = basePageSteps.onAmpSaleAdsPage().ampSortSelect().getCoordinates().onPage().getY();
        assertThat(snippetLocation).as("Проверяем что Y-координата сниппета меньше чем элемента сортировки")
                .isLessThan(sortLocation);

    }
}

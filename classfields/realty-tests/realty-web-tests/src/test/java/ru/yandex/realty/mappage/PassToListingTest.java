package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Общее")
@Feature(MAP)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PassToListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String dealType;

    @Parameterized.Parameter(2)
    public String realtyType;

    @Parameterized.Parameters(name = "Переход с {0} на листинг")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", KUPIT, KVARTIRA},
                {"Аренда дома", SNYAT, DOM},
                {"Новостройки", KUPIT, NOVOSTROJKA},
                {"Коттеджные поселки", KUPIT, KOTTEDZHNYE_POSELKI},
                {"Снять Коммерческая", SNYAT, COMMERCIAL},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход с карты на листинг")
    public void shouldSeePassOnListing() {
        urlSteps.testing().path(MOSKVA_I_MO).path(dealType).path(realtyType).path(KARTA).open();
        basePageSteps.onMapPage().toListingButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(dealType).path(realtyType).shouldNotDiffWithWebDriverUrl();
    }
}

package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;

@DisplayName("Карточка оффера. Шорткаты")
@Feature(OFFER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShortcutNewBuildingSpecificClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferPageSteps offerPageSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String shortcut;

    @Parameterized.Parameter(1)
    public String fragment;

    @Parameterized.Parameters(name = "{index} Карточка новостройки плик по шорткату - {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {"Панорама", "infrastructure"},
                {"Ход строительства", "progress"},
//                {"Отделка", "decoration"},
        });
    }

    @Before
    public void before() {
        basePageSteps.resize(1920, 2400);
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path("chistoe-nebo-46459/").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KANTEMIROV)
    public void shouldSeeScrollAndSelectNbSpecific() {
        offerPageSteps.clickOnElementShouldScroll(() -> offerPageSteps.findShortcut(shortcut));
        urlSteps.fragment(fragment).shouldNotDiffWithWebDriverUrl();
    }
}

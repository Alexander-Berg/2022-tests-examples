package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Canonical")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CanonicalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mock;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameter(2)
    public String canonical;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"desktop/OfferCarsUsedUser", "/cars/used/sale/land_rover/discovery/1076842087-f1e84/", "https://test.avto.ru/cars/used/sale/land_rover/discovery/1076842087-f1e84/"},
                {"desktop/OfferCarsNewDealerKiaOptima", "/cars/new/group/kia/optima/21342125/0/1076842087-f1e84/", "https://test.avto.ru/cars/new/group/kia/optima/21342125/0/1076842087-f1e84/"},
                {"desktop/OfferCarsNewDealer", "/cars/new/group/kia/optima/21342125/21342344/1076842087-f1e84/", "https://test.avto.ru/cars/new/group/kia/optima/21342125/21342344/1076842087-f1e84/"},

                {"desktop/OfferTrucksUsedUserLcv", "/lcv/used/sale/volkswagen/crafter_/1076842087-f1e84/", "https://test.avto.ru/lcv/used/sale/volkswagen/crafter_/1076842087-f1e84/"},
                {"desktop/OfferTrucksUsedUserArtic", "/artic/used/sale/daf/xf95/1076842087-f1e84/", "https://test.avto.ru/artic/used/sale/daf/xf95/1076842087-f1e84/"},

                {"desktop/OfferMotoUsedUser", "/motorcycle/used/sale/harley_davidson/dyna_super_glide/1076842087-f1e84/", "https://test.avto.ru/motorcycle/used/sale/harley_davidson/dyna_super_glide/1076842087-f1e84/"},
                {"desktop/OfferMotoUsedUserScooter", "/scooters/used/sale/vento/corsa/1076842087-f1e84/", "https://test.avto.ru/scooters/used/sale/vento/corsa/1076842087-f1e84/"},
                {"desktop/OfferMotoUsedUserAtv", "/atv/used/sale/yacota/150/1076842087-f1e84/", "https://test.avto.ru/atv/used/sale/yacota/150/1076842087-f1e84/"},
                {"desktop/OfferMotoUsedUserSnowmobile", "/snowmobile/used/sale/brp/ski_doo_expedition_se_1200/1076842087-f1e84/", "https://test.avto.ru/snowmobile/used/sale/brp/ski_doo_expedition_se_1200/1076842087-f1e84/"}

        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                mock).post();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка canonical")
    public void shouldSeeCanonical() {
        urlSteps.testing().path(url).open();
        basePageSteps.onCardPage().canonical().should(hasAttribute("href", canonical));
    }
}

package ru.auto.tests.desktop.dealersdistributor;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALERS_DISTRIBUTOR;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Подбор дилера - сниппет")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetTest {

    private static final String MARK = "toyota";
    private static final String MODEL = "corolla";

    @Rule
    @Inject
    public MockRule mockRule;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/DealersDistributorTest/SearchCarsNewToyotaCorolla",
                "desktop/SearchCarsBreadcrumbsToyotaCorolla",
                "desktop/Salon",
                "desktop/OfferCarsPhones",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(DEALERS_DISTRIBUTOR).path(CARS).path(NEW).path(MARK).path(MODEL).path("/").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickDealer() {
        basePageSteps.onDealerDisturbutorPage().getDealer(0).should(hasText("Авилон Mercedes-Benz Воздвиженка\n" +
                "Официальный дилер\nСеть АВИЛОН\nМосква, ул. Воздвиженка, д. 12\n+7 916 039-84-27\n⎘")).click();
        urlSteps.testing().path(DILER).path(CARS).path(NEW).path(CARS_OFFICIAL_DEALER).path("/")
                .shouldNotSeeDiff();
    }
}
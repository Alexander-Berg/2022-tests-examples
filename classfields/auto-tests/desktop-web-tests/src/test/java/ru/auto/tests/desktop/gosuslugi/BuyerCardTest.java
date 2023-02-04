package ru.auto.tests.desktop.gosuslugi;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.GOSUSLUGI;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - Госуслуги")
@Feature(GOSUSLUGI)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class BuyerCardTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String SHOW = "{\"cars\":{\"card\":{\"advantages\":{\"show\":{\"gosuslugi_linked\":{}}}}}}";
    private static final String SHOW_POPUP = "{\"cars\":{\"card\":{\"advantages\":{\"show-popup\":{\"gosuslugi_linked\":{}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public SeleniumMockSteps browserMockSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserGosuslugi"),
                stub("desktop/SessionUnauth")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Преимущество «Госуслуги» для покупателя")
    public void shouldSeeBenefitsGosuslugi() {
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(SHOW)));

        basePageSteps.onCardPage().benefits().benefit("Госуслуги").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Аккаунт на Госуслугах\nПродавец " +
                "указал свой подтвержденный аккаунт Госуслуг. Мы больше доверяем таким пользователям, так как они " +
                "подтвердили регистрацию на Госуслугах с предъявлением документов."));

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(SHOW_POPUP)));
    }
}

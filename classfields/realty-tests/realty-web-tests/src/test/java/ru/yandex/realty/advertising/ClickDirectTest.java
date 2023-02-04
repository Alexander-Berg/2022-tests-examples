package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * Created by vicdev on 28.06.17.
 */
@DisplayName("Проверяем, что при клике делаем запросы в биллинг")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class ClickDirectTest {

    private static final String GATE_STAT_PHONE = "/gate/stat/phone";
    private static final String BILLING_DUMP = "billingDump";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private BasePageSteps user;

    @Before
    public void before() {
        proxy.clearHar();
        proxy.proxyServerManager.getServer().enableHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().newbuildingSiteMock().open();
    }

    @Test
    @Owner(VICDEV)
    @DisplayName("Клик по кнопке «Показать телефоны» на платном оффере")
    @Category({Regression.class, Smoke.class, Testing.class})
    public void shouldSeeBillingDumpFromNewBuilding() {
        user.onNewBuildingSitePage().apartmentCategory().waitUntil(hasSize(greaterThan(0))).get(FIRST).openBlock()
                .click();
        urlSteps.fromUri(user.onNewBuildingSitePage().offerLinks().waitUntil(hasSize(greaterThan(0))).get(FIRST)
                .getAttribute("href")).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        user.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        shouldSeeBillingDump();
    }

    @Test
    @Owner(VICDEV)
    @DisplayName("Клик по кнопке «Показать телефоны» на платной новостройке")
    @Category({Regression.class, Smoke.class, Testing.class})
    @Issue("REALTY-12947")
    public void shouldSeeBillingDumpFromOffer() {
        proxy.clearHarUntilThereAreNoHarEntries();
        user.onNewBuildingSitePage().siteCardAbout().showPhoneClick();
        shouldSeeBillingDump();
    }

    @Step("Проверяем, что уходит запрос")
    private void shouldSeeBillingDump() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(10, SECONDS).ignoreExceptions()
                .untilAsserted(() -> assertThat(
                        proxy.proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                                .filter(e -> e.getRequest().getUrl().contains(GATE_STAT_PHONE))
                                .filter(e -> e.getRequest().getPostData().getText().contains(BILLING_DUMP))
                                .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK))
                                .collect(Collectors.toList()).size()).isGreaterThan(0));
    }
}

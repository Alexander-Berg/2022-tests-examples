package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Dimension;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_COUNT;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_RBTCOUNT;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Проверяем, что на странице оффера есть счетчики")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Issue("VERTISTEST-635")
public class CountersOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private ProxySteps proxy;

    @Before
    public void before() {
        proxy.clearHar();
        apiSteps.createVos2Account(account, OWNER);
        String offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        user.getDriver().manage().window().setSize(new Dimension(1600, 1800));
        urlSteps.testing().path(String.format("/offer/%s/", offerId)).open();
        user.onMainPage().rtbAds().forEach(RealtyElement::hover);
    }

    @Test
    @Category({Regression.class, Smoke.class, Testing.class})
    @Owner(VICDEV)
    public void shouldSeeOfferCounters() {
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_COUNT), greaterThanOrEqualTo(2));
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_RBTCOUNT), greaterThanOrEqualTo(1));
    }
}

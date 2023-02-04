package ru.yandex.realty.eventslog.lk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.element.management.OfferServicesPanel.ADD_SERVICE;
import static ru.yandex.realty.element.management.OfferServicesPanel.PROMOTION;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.GoalsSteps.EVENT_GATE_STAT_RENEWAL;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Событие «UPDATE_RENEWAL»")
@Feature(RealtyFeatures.EVENTS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class AutoContinueEventTest {

    private static final String PATH_TO_EVENTS_AUTO_RENEW_CANCEL_JSON = "events/autoRenewCancel.json";
    private static final String[] JSONPATHS_TO_IGNORE = {"params[0][1].offer_id",
            "params[0][1].offer_ids[0]"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void openManagementPage() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).withSearcherWait().create().getId();
        managementSteps.setWindowSize(1400, 1600);
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.refreshUntil(() -> managementSteps.onManagementNewPage().offer(FIRST).servicesPanel(),
                isDisplayed());
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION).button(ADD_SERVICE)
                .click();
        walletSteps.payAndWaitSuccess();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Отменяем автопродление видим событие «UPDATE_RENEWAL»")
    public void shouldSeeCancelAutoRenewEvent() {
        managementSteps.onManagementNewPage().offer(FIRST).servicesPanel().service(PROMOTION).autoRenew().click();
        goalsSteps.urlMatcher(containsString(EVENT_GATE_STAT_RENEWAL))
                .withEventParams(PATH_TO_EVENTS_AUTO_RENEW_CANCEL_JSON)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }
}
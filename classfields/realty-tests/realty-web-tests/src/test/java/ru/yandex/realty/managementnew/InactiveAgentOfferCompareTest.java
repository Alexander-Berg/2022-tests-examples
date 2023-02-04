package ru.yandex.realty.managementnew;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.page.ManagementNewPage.REMOVE_FROM_PUBLICATION;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Агентский оффер")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class InactiveAgentOfferCompareTest {


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        compareSteps.resize(1920,5000);
        managementSteps.moveCursor(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        managementSteps.onManagementNewPage().agencyOffer(FIRST).button(REMOVE_FROM_PUBLICATION)
                .waitUntil(isDisplayed()).click();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем на «Снять с публикации», сравниваем попапы «Причина снятия объявления»")
    public void shouldSeePublishControlPopup() {
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().publishControlPopup().content());
        urlSteps.setProductionHost().open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        managementSteps.onManagementNewPage().agencyOffer(FIRST).button(REMOVE_FROM_PUBLICATION)
                .waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().publishControlPopup().content());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}

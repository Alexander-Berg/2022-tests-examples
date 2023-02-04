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
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.page.ManagementNewPage.CANCEL_DELETE;
import static ru.yandex.realty.page.ManagementNewPage.DELETE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Удаление оффера со страницы личного кабинета")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeleteOfferTest {

    private String offerId;

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

    @Before
    public void createOffer() {
        apiSteps.createVos2Account(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account).create().getId();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Удаляем оффер")
    public void shouldDeleteOffer() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(DELETE).click();
        apiSteps.waitOfferStatus(account.getId(), offerId, "ERROR");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Не удаляем блок с оффером до рефреша")
    public void shouldNotDeleteOfferWithoutRefresh() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(DELETE).click();
        managementSteps.onManagementNewPage().offer(FIRST).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Не удаляем блок с оффером после рефреша")
    public void shouldDeleteOfferBlockAfterRefresh() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(DELETE).click();
        managementSteps.refresh();
        managementSteps.onManagementNewPage().offersList().should(hasSize(0));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Удаляем оффер")
    public void shouldNotDeleteOfferAfterCancel() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(DELETE).click();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(CANCEL_DELETE).click();
        apiSteps.waitOfferStatus(account.getId(), offerId, "OK");
    }
}

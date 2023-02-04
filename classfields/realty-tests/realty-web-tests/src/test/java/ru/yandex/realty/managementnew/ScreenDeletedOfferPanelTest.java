package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.page.ManagementNewPage.CANCEL_DELETE;
import static ru.yandex.realty.page.ManagementNewPage.DELETE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Попап подтверждения удаления оффера со страницы личного кабинета")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ScreenDeletedOfferPanelTest {

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

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Inject
    private SearcherAdaptor searcherAdaptor;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Скрин удаленного оффера")
    public void shouldDeleteOffer() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).create();
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        Screenshot testingScreenshot = getScreenshot();

        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(CANCEL_DELETE).click();
        searcherAdaptor.waitOffer(offerBuildingSteps.getId());
        vos2Adaptor.waitActivateOffer(account.getId(), offerBuildingSteps.getId());

        urlSteps.production().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        Screenshot productionScreenshot = getScreenshot();

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Step("Нажимаем на кнопку удалить оффер и делаем скриншот")
    private Screenshot getScreenshot() {
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().button(DELETE).click();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel()
                .button(CANCEL_DELETE).waitUntil(isDisplayed());
        /*Двигаем курсор потому что «Отменить удаление» подсвечивается*/
        offerAddSteps.moveCursorToElement(managementSteps.onManagementNewPage().offer(FIRST).offerInfo());
        return compareSteps.takeScreenshot(managementSteps.onManagementNewPage().offer(FIRST).controlPanel());
    }
}

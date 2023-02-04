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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.page.ManagementNewPage.EDIT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Агентский оффер")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class EditAgentOfferTest {

    private static final String NEW_PRICE = "9000000";
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
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        offerId = offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create().getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().agencyOffer(FIRST));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем на «Изменить», проверяем что перешли на страницу редактирования")
    public void shouldSeeEditPage() {
        managementSteps.onManagementNewPage().agencyOffer(FIRST).link(EDIT).waitUntil(isDisplayed()).click();
        managementSteps.switchToTab(1);
        urlSteps.path("edit").path(offerId).path("/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вводим новую цену, проверяем что изменилась")
    public void shouldSeeEditedPrice() {
        managementSteps.moveCursor(managementSteps.onManagementNewPage().agencyOffer(FIRST).price());

        managementSteps.onManagementNewPage().agencyOffer(FIRST).price().editButton().should(isDisplayed()).click();
        managementSteps.onManagementNewPage().agencyOffer(FIRST).price().clearSign().click();
        managementSteps.onManagementNewPage().agencyOffer(FIRST).price().input().sendKeys(NEW_PRICE);
        managementSteps.onManagementNewPage().agencyOffer(FIRST).price().submitButton().click();
        managementSteps.onManagementNewPage().agencyOffer(FIRST).price().value()
                .waitUntil(hasText(not(isEmptyOrNullString())));

        assertThat(managementSteps.onManagementNewPage().agencyOffer(FIRST).price().value().getText()
                .replace(" ", "").replace("\u20BD", "")).isEqualTo(NEW_PRICE);
    }
}

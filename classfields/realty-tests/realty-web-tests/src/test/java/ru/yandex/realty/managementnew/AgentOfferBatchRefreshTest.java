package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.WithOffers;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModuleExp;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.time.LocalDateTime.now;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.page.ManagementNewPage.RENEW;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

@DisplayName("Агентский оффер")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleExp.class)
public class AgentOfferBatchRefreshTest {

    private static final int OFFERS_COUNT = 2;
    private static final int UPDATE_DAY = -364;

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

    @Before
    public void before() {
        managementSteps.setWindowSize(1200, 1600);
        apiSteps.createVos2Account(account, AGENT);
    }

    @Ignore("пока убрали функционал")
    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Обновляем офферы")
    @WithOffers(count = OFFERS_COUNT, createDay = UPDATE_DAY, updateDay = UPDATE_DAY, accountType = "agent")
    public void shouldRefreshOfferBatch() {
        String nearlyExpiredDate = reformatOfferCreateDate(now().plusDays(UPDATE_DAY));

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.onManagementNewPage().headerAgentOffers().selectAllChecbox().should(isDisplayed()).click();
        managementSteps.onManagementNewPage().offersControlPanel().button("Ещё").click();
        managementSteps.onManagementNewPage().offersControlPanel().button(RENEW).click();
        apiSteps.waitOfferUpdateTimeChange(account.getId(), 0, nearlyExpiredDate);
        apiSteps.waitOfferUpdateTimeChange(account.getId(), 1, nearlyExpiredDate);
    }
}

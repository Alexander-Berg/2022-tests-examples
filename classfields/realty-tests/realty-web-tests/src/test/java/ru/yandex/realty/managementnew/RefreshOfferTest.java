package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.realty.module.RealtyWebModuleExp;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.time.LocalDateTime.now;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

@DisplayName("Обновление оффера со страницы личного кабинета")
@Feature(MANAGEMENT)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleExp.class)
public class RefreshOfferTest {

    private static final int OFFER_DATE = -364;

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

    @Ignore("пока убрали функционал")
    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Обновляем оффер")
    @WithOffers(count = 1, createDay = OFFER_DATE, updateDay = OFFER_DATE)
    public void shouldRefreshOffer() {
        String nearlyExpiredDate = reformatOfferCreateDate(now().plusDays(OFFER_DATE));

        apiSteps.login(account);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        managementSteps.setWindowSize(1200, 1600);
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().prolongateOffer().click();
        apiSteps.waitOfferUpdateTimeChange(account.getId(), 0, nearlyExpiredDate);
    }
}

package ru.yandex.realty.managementnew.egrn.www.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.offercard.EgrnBlock.BUY_FULL_REPORT;
import static ru.yandex.realty.page.BasePage.REPORTS;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@Tag(JURICS)
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AdAgencyOfferCardEgrnTest {

    private static final String MESSAGE_IN_A_BOTTLE = "К сожалению, для вашего аккаунта покупка отчета пока невозможна";
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
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.login("zagent5555", "Qwerty123!@");
        apiSteps.createVos2AccountWithoutLogin(account, AccountType.OWNER);
        offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим попап о невозможности купить отчет под рекламным агентством")
    public void shouldSeeRejectPopupForRa() {
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        basePageSteps.onOfferCardPage().modalVisible().should(hasText(containsString(MESSAGE_IN_A_BOTTLE)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Не видим «Отчеты» в попапе юзера")
    public void shouldSeeUserPopupReportLine() {
        urlSteps.testing().open();
        basePageSteps.moveCursor(basePageSteps.onManagementNewPage().headerMain().userAccount());
        basePageSteps.onManagementNewPage().userNewPopup().link(REPORTS).should(not(exists()));
    }
}

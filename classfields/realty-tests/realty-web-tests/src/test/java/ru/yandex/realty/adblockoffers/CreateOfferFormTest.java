package ru.yandex.realty.adblockoffers;

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
import ru.yandex.realty.categories.Adblock;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModuleWithAdBlock;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.ADBLOCK;
import static ru.yandex.realty.element.offers.PublishBlock.ITEM_IS_SELECTED;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasStatus;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.step.OfferAddSteps.MOSCOW_LOCATION;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kantemirov
 */
@DisplayName("Форма добавления объявления с AdBlock'ом.")
@Feature(ADBLOCK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithAdBlock.class)
public class CreateOfferFormTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Adblock.class, Testing.class})
    @DisplayName("Создаем объявление под AdBlock'ом через форму.")
    public void shouldSeeOfferCreateWithAdBlock() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.switchToTab(0);
        urlSteps.setMoscowCookie();
        offerAddSteps.fillRequiredFieldsForSellFlat(MOSCOW_LOCATION);
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishBlock().paySelector(PROMOTION)
                .clickWhile(hasClass(containsString(ITEM_IS_SELECTED)));
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        apiSteps.waitFirstOffer(account, hasStatus("active"));

        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offersList().should(hasSize(1));
    }
}
package ru.yandex.realty.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by vicdev on 08.06.17.
 */

@DisplayName("Форма добавления объявления. Определение места")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class GeoLocationFieldTest {

    private static final String EMPTY_LOCATION = "";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Before
    public void before() {
        urlSteps.setSpbCookie();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(VICDEV)
    public void shouldClearField() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.onOfferAddPage().locationControls().suggest().click();
        offerAddSteps.onOfferAddPage().locationControls().suggest().clear();
        offerAddSteps.onOfferAddPage().locationControls().suggest().sendKeys(Utils.getRandomString());
        offerAddSteps.onOfferAddPage().locationControls().iconClear().click();
        offerAddSteps.onOfferAddPage().locationControls().suggest()
                .should(hasAttribute("value", EMPTY_LOCATION));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверяем, что адрес на бэке и на форматировании одинаковый")
    public void shouldSeeGeolocationInVos() {
        api.createVos2Account(account, OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasAddress(DEFAULT_LOCATION);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().locationControls().suggest().should(hasValue(DEFAULT_LOCATION));
    }
}

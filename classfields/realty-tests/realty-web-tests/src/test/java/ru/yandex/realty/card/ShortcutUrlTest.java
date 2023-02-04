package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.element.Shortcuts.INFRASTRUCTURE;
import static ru.yandex.realty.element.Shortcuts.PROFITABILITY;
import static ru.yandex.realty.element.Shortcuts.RENT_PRICE;
import static ru.yandex.realty.element.Shortcuts.SELL_PRICE;
import static ru.yandex.realty.element.Shortcuts.TRANSPORT_ACCESSIBILITY;
import static ru.yandex.realty.element.Shortcuts.YA_DRIVE;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Карточка оффера. Шорткаты")
@Feature(OFFER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShortcutUrlTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private OfferPageSteps offerPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String layer;

    @Parameterized.Parameter(1)
    public String mapShortcut;

    @Parameterized.Parameters(name = "{index} Переход по ссылке с «layer={0}» в карточке оффера . Кнопка зачекана")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {"infrastructure", INFRASTRUCTURE},
                {"transport", TRANSPORT_ACCESSIBILITY},
//                {"price-sell", SELL_PRICE},
                {"price-rent", RENT_PRICE},
                {"profitability", PROFITABILITY},
                {"carsharing", YA_DRIVE},
        });
    }

    @Before
    public void before() {
        apiSteps.createVos2AccountWithoutLogin(account, OWNER);
        String offerId =
                offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).withSearcherWait().create().getId();
        urlSteps.testing().path(OFFER).path(offerId).queryParam("layer", layer).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KANTEMIROV)
    public void shouldSeeSelectedButton() {
        offerPageSteps.onOfferCardPage().mapShortcut(mapShortcut).should(isChecked());
    }
}

package ru.yandex.realty.offers.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getRandomShortLong;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.DRAFT;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;

/**
 * Created by ivanvan on 01.08.17.
 */
@DisplayName("Заполняем форму оффера, не залогинившись. После чего логинимся.")
@Feature(OFFERS)
@Story(DRAFT)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class DraftNoAuthTest {

    private static final String SAVE_QUERY = "/gate/add/save_draft";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private ProxySteps proxy;

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    @Description("Проверяем, что черновик сохраняется")
    public void shouldSeeSaveDraft() {
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        proxy.proxyServerManager.getServer().clearBlacklist();

        long price = getRandomShortLong();
        long floor = getRandomShortLong();
        long area = getRandomShortLong();
        long totalFloor = floor + 1;

        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);

        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(FIRST, valueOf(floor));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputInItem(SECOND, valueOf(totalFloor));

        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(valueOf(area));
        offerAddSteps.onOfferAddPage().priceField().priceInput().sendKeys(valueOf(price));
        offerAddSteps.waitSaveOnBackend();

        proxy.shouldSeeRequestInLog(containsString(SAVE_QUERY), greaterThanOrEqualTo(1));

        api.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.refresh();
        offerAddSteps.onOfferAddPage().priceField().priceInput()
                .should("Цена должна сохранится", hasValue(valueOf(price)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(FIRST)
                .should("Этажи должны сохранятся", hasValue(valueOf(floor)));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND)
                .should("Этажность дома должна сохраняться", hasValue(valueOf(totalFloor)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input()
                .should("Площадь квартиры должна сохранятся", hasValue(valueOf(area)));
    }
}

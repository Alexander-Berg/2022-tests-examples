package ru.yandex.realty.offers.validate;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.VALIDATE;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;

/**
 * Created by ivanvan on 15.08.17.
 */
@DisplayName("Форма добаления. Проверка необходимый для заполнения полей для «продать-квартиру»")
@Feature(OFFERS)
@Story(VALIDATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NecessaryFieldForFlatTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openManagementPage() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
    }

    @Test
    @DisplayName("Поле цены выдаёт алерт")
    @Owner(IVANVAN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void comparePriceFieldAlertWithProduction() {
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage()
                .priceField().formInner());

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();

        Screenshot productionScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage()
                .priceField().formInner());

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @DisplayName("Поле контактной информации выдаёт алерт")
    @Owner(IVANVAN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void compareContactFieldAlertWithProduction() {
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage()
                .contactInfo().formInner());

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();

        Screenshot productionScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage()
                .contactInfo().formInner());

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

}

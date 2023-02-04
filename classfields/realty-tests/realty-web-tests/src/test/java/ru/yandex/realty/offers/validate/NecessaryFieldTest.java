package ru.yandex.realty.offers.validate;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.HOUSE;
import static ru.yandex.realty.consts.OfferAdd.LAND;
import static ru.yandex.realty.consts.OfferAdd.LAND_AREA;
import static ru.yandex.realty.consts.OfferAdd.ROOM;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.VALIDATE;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;

/**
 * Created by ivanvan on 16.08.17.
 */
@DisplayName("Форма добаления. Проверка необходимых для заполнения полей для «продать-дом»/«продать-участок»/«продать-комнату»")
@Feature(OFFERS)
@Story(VALIDATE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NecessaryFieldTest {

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


    @Parameterized.Parameter
    public String fieldName;

    @Parameterized.Parameter(1)
    public String fieldArea;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {HOUSE, AREA},
                {LAND, LAND_AREA},
                {ROOM, AREA},
        });
    }

    @Before
    public void before() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(fieldName);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();
    }

    @Test
    @Owner(IVANVAN)
    @DisplayName("Валидация полей «продать-дом»/«продать-участок»/«продать-комнату»")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void compareNecessaryFieldsWithProduction() {
        Screenshot testingScreenshot = compareSteps
                .getElementScreenshot(offerAddSteps.onOfferAddPage().featureField(fieldArea));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(fieldName);
        offerAddSteps.publish();

        Screenshot productionScreenshot = compareSteps
                .getElementScreenshot(offerAddSteps.onOfferAddPage().featureField(fieldArea));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}

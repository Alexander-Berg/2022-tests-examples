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
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.VALIDATE;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;


@DisplayName("Проверка необходимый для заполнения полей для «продать-квартиру». Площадь/Этаж")
@Feature(OFFERS)
@Story(VALIDATE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NecessaryFieldAreaFlatTest {

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

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String elementName;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {AREA},
                {FLOOR}
        });
    }


    @Before
    public void openManagementPage() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Поля должны верно отображаться")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void compareNecessaryFieldsForFlatWithProduction() {
        Screenshot testingScreenshot = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage()
                .featureField(elementName));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().locationControls().iconClear().clickIf(exists());
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.publish();

        Screenshot productionScreenshot = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage()
                .featureField(elementName));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

}

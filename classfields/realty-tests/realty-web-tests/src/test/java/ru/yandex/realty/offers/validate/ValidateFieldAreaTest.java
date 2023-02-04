package ru.yandex.realty.offers.validate;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.NUMBER_OF_ROOMS;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.VALIDATE;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;

/**
 * Created by ivanvan on 17.08.17.
 */
@DisplayName("Форма добавления объявления. Проверяем, что нельзя заполнять поля неверно")
@Feature(OFFERS)
@Story(VALIDATE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ValidateFieldAreaTest {

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
    public Integer numberOfRoom;

    @Parameterized.Parameters
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {2},
                {3},
                {4},
                {5},
                {6},
        });
    }


    @Before
    public void openManagement() {
        api.createVos2Account(account, AccountType.OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        compareSteps.resize(1920, 10000);
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Должен появляться алерт")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void compareValidateAreaAlertWithProduction() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);

        int roomArea = Utils.getRandomShortInt();
        int totalArea = roomArea * numberOfRoom;
        int invalidTotalArea = totalArea - 1;

        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_ROOMS).should(isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_ROOMS).checkButton(String.valueOf(numberOfRoom));
        fillRoomsAreaAndTotalArea(roomArea, numberOfRoom, invalidTotalArea);

        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().featureField(AREA));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);

        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_ROOMS).should(isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_ROOMS).checkButton(String.valueOf(numberOfRoom));
        fillRoomsAreaAndTotalArea(roomArea, numberOfRoom, invalidTotalArea);
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();

        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().featureField(AREA));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Заполняем площадь комнат и этажность")
    private void fillRoomsAreaAndTotalArea(int roomArea, int numberOfRoom, int totalArea) {
        offerAddSteps.onOfferAddPage().featureField(AREA).inputList()
                .should(hasSize(greaterThanOrEqualTo(numberOfRoom)));
        offerAddSteps.onOfferAddPage().featureField(AREA).inputList().stream().skip(2)
                .forEach(s -> s.input().sendKeys(String.valueOf(roomArea)));
        offerAddSteps.onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(totalArea));
    }
}

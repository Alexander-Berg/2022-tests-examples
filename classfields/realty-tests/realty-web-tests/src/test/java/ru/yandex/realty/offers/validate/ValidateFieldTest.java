package ru.yandex.realty.offers.validate;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.VALIDATE;
import static ru.yandex.realty.element.offers.FeatureField.SECOND;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_LOCATION;

/**
 * Created by ivanvan on 17.08.17.
 */
@DisplayName("Форма добавления объявления. Проверяем, что нельзя заполнять поля неверно")
@Feature(OFFERS)
@Story(VALIDATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ValidateFieldTest {

    private static final String ERROR_TEXT = "Не удалось";
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

    @Before
    public void openManagement() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        basePageSteps.resize(1600, 7000);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
    }

    @Test
    @DisplayName("Появляется алерт в поле геолокации")
    @Owner(IVANVAN)
    public void compareValidateLocationWithProduction() {
        moveCursorByOffset(offerAddSteps.onOfferAddPage().map(), -200, 0);
        Screenshot testing = compareSteps.takeScreenshot(
                offerAddSteps.onOfferAddPage().addressError().waitUntil(hasText(containsString(ERROR_TEXT))));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        moveCursorByOffset(offerAddSteps.onOfferAddPage().map(), -200, 0);
        Screenshot production = compareSteps.takeScreenshot(
                offerAddSteps.onOfferAddPage().addressError().waitUntil(hasText(containsString(ERROR_TEXT))));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Появляется алерт в поле «Этаж»")
    @Owner(IVANVAN)
    public void compareFloorAlertWithProduction() {
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().locationControls().suggest()
                .waitUntil("Должен быть адрес", hasValue(DEFAULT_LOCATION), 30);

        int totalFloor = Utils.getRandomShortInt();
        fillFloorFieldAndPublish(totalFloor);

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().featureField(FLOOR));

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.selectGeoLocation(DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().locationControls().suggest()
                .waitUntil("Должен быть адрес", hasValue(DEFAULT_LOCATION), 30);
        fillFloorFieldAndPublish(totalFloor);

        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().featureField(FLOOR));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Заполняем поле «Этаж» и нажимаем кнопку «Опубликовать»")
    private void fillFloorFieldAndPublish(int totalFloor) {
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input().hover();
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputList().get(FIRST).clearSign().clickIf(isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input().sendKeys(String.valueOf(totalFloor + 1));
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND).hover();
        offerAddSteps.onOfferAddPage().featureField(FLOOR).inputList().get(SECOND).clearSign().clickIf(isDisplayed());
        offerAddSteps.onOfferAddPage().featureField(FLOOR).input(SECOND).sendKeys(String.valueOf(totalFloor));
        offerAddSteps.publish();
    }

    @Step("Переходим к {element} и кликаем смещаясь от центра на {x}, {y}")
    private void moveCursorByOffset(WebElement element, int x, int y) {
        (new Actions(basePageSteps.getDriver())).moveToElement(element, x, y).click().build().perform();
    }
}

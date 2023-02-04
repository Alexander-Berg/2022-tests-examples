package ru.auto.tests.mobile.garage;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CURRENT;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Regions.HIMKI;
import static ru.auto.tests.desktop.element.garage.CardForm.REGION_FIELD;
import static ru.auto.tests.desktop.mobile.component.WithButton.FIND;
import static ru.auto.tests.desktop.mobile.element.garage.CarSelector.ADD_DREAM_CAR;
import static ru.auto.tests.desktop.mobile.element.garage.CarSelector.ADD_EX_CAR;
import static ru.auto.tests.desktop.mobile.page.GarageAddPage.LICENCE_PLATE_OR_VIN;
import static ru.auto.tests.desktop.mobile.page.GarageCardPage.CHANGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Гараж")
@Story("Cтраница добавления своего автомобиля")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageAddCurrentTest {

    private static final String VIN = "WVWZZZ16ZBM121912";
    private static final String BAD_VIN = "WVWZZZ16ZBM121913";
    private static final String VIN_CARD_ID = "/1146321503/";
    private static final String LICENSE_PLATE = "Y019PY197";
    private static final String LICENSE_PLATE_CARD_ID = "/1373191029/";
    private static final String DEFAULT_CITY = "Москва";
    private static final String ADD_TO_GARAGE = "Добавить в гараж";
    private static final String MAIN_REGION = "Москва и Московская область";

    private static final String MY_CAR_BLOCK_TEXT_ADD_MOBILE = "Госномер или VIN\nОтменить\nVolkswagen Jetta\nГод" +
            "\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет\nСерый\nРегион учёта\nМосква\nVIN\nWVWZZZ16*BM*****2" +
            "\nПроверьте регион и поменяйте его, если не соответствует действительности\nДобавить в гараж\nНайти";
    private static final String MY_CAR_BLOCK_TEXT_ADD_MOBILE_EXISTS = "Госномер или VIN\nОтменить\nVolkswagen Jetta" +
            "\nГод\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет\nКрасный\nРегион учёта\nМосква\nVIN\nWVWZZZ16ZBM121912" +
            "\nПроверьте регион и поменяйте его, если не соответствует действительности\nПерейти в гараж\nНайти";
    private static final String MY_CAR_BLOCK_TEXT_ADD_MOBILE_EDIT_CITY = "Госномер или VIN\nОтменить\nVolkswagen Jetta" +
            "\nГод" + "\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет\nСерый\nРегион учёта\nХимки\nVIN\nWVWZZZ16*BM*****2" +
            "\nПроверьте регион и поменяйте его, если не соответствует действительности\nДобавить в гараж\nНайти";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest")).create();

        urlSteps.testing().path(GARAGE).path(ADD).path(CURRENT).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onGarageAddPage().content().should(hasText("Госномер или VIN\nОтменить\nВся информация о вашем " +
                "автомобиле под крышей одного Гаража.\nУзнайте о бесплатной замене деталей, скидках " +
                "и рекомендациях по обслуживанию\nНайти"));
        basePageSteps.onGarageAddPage().button("Найти").should(not(isEnabled()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Отменить»")
    public void shouldClickCancelButton() {
        basePageSteps.onGarageAddPage().cancelButton().click();
        urlSteps.testing().path(GARAGE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление автомобиля по VIN")
    public void shouldAddVin() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoVin"),
                stub("desktop/GarageUserCardIdentifierVin"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin")).update();

        basePageSteps.onGarageAddPage().input(LICENCE_PLATE_OR_VIN, VIN);
        basePageSteps.onGarageAddPage().button(FIND).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().content().should(hasText(MY_CAR_BLOCK_TEXT_ADD_MOBILE));
        basePageSteps.onGarageAddPage().button(ADD_TO_GARAGE).click();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().carSelector().should(hasText(format("Volkswagen Jetta\n%s\n%s\n%s",
                VIN, ADD_DREAM_CAR, ADD_EX_CAR)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление автомобиля по госномеру")
    public void shouldAddLicensePlate() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoLicensePlate"),
                stub("desktop/GarageUserCardIdentifierLicensePlate"),
                stub("desktop/GarageUserCardsLicensePlatePost"),
                stub("desktop/GarageUserCardLicensePlate")).update();

        basePageSteps.onGarageAddPage().input(LICENCE_PLATE_OR_VIN, LICENSE_PLATE);
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onGarageAddPage().button(FIND).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().content().should(hasText(MY_CAR_BLOCK_TEXT_ADD_MOBILE));
        basePageSteps.onGarageAddPage().button(ADD_TO_GARAGE).click();
        urlSteps.testing().path(GARAGE).path(LICENSE_PLATE_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().carSelector().should(hasText(format("Volkswagen Jetta\n%s\n%s\n%s",
                LICENSE_PLATE, ADD_DREAM_CAR, ADD_EX_CAR)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление уже добавленного автомобиля по VIN")
    public void shouldAddAlreadyAddedVin() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoVinAlreadyAdded"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin")).update();

        basePageSteps.onGarageAddPage().input(LICENCE_PLATE_OR_VIN, VIN);
        basePageSteps.onGarageAddPage().button(FIND).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().content().should(hasText(MY_CAR_BLOCK_TEXT_ADD_MOBILE_EXISTS));
        basePageSteps.onGarageAddPage().button("Перейти в гараж").click();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().carSelector().should(hasText(format("Volkswagen Jetta\n%s\n%s\n%s",
                VIN, ADD_DREAM_CAR, ADD_EX_CAR)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Добавление автомобиля по госномеру с изменением региона")
    public void shouldAddLicensePlateEditRegion() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoLicensePlate"),
                stub("desktop/GarageUserCardIdentifierLicensePlate"),
                stub("desktop/GarageUserCardsLicensePlatePost"),
                stub("desktop/GarageUserCardLicensePlateHimki"),
                stub("desktop/GeoSuggest")).update();

        basePageSteps.onGarageAddPage().input(LICENCE_PLATE_OR_VIN, LICENSE_PLATE);
        basePageSteps.onGarageAddPage().button(FIND).waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();

        basePageSteps.onGarageAddPage().button(DEFAULT_CITY).click();
        basePageSteps.onGarageAddPage().input("Населённый пункт", HIMKI);
        basePageSteps.onGarageAddPage().select(format("%s%s", HIMKI, MAIN_REGION)).click();
        basePageSteps.onGarageAddPage().content().should(hasText(MY_CAR_BLOCK_TEXT_ADD_MOBILE_EDIT_CITY));
        basePageSteps.onGarageAddPage().button(ADD_TO_GARAGE).click();

        urlSteps.testing().path(GARAGE).path(LICENSE_PLATE_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().button(CHANGE).click();

        basePageSteps.onGarageCardPage().form().block(format("%s%s", REGION_FIELD, HIMKI)).should(isDisplayed());
    }
}

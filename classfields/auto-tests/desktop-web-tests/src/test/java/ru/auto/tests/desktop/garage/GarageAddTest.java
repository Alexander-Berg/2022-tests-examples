package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Regions.HIMKI;
import static ru.auto.tests.desktop.element.garage.CardForm.CARD_REGION_PLACEHOLDER;
import static ru.auto.tests.desktop.element.garage.CardForm.REGION_FIELD;
import static ru.auto.tests.desktop.element.garage.DreamCarBlock.DREAM_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.EX_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.MY_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.MY_CAR_BLOCK_TEXT_ADD;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.MY_CAR_BLOCK_TEXT_ADD_WITH_EDIT_REGION;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.MY_CAR_BLOCK_TEXT_PASS;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.PUT_INTO_GARAGE_BUTTON;
import static ru.auto.tests.desktop.mobile.page.GarageAddPage.LICENCE_PLATE_OR_VIN;
import static ru.auto.tests.desktop.mobile.page.HistoryPage.QUESTION_BUTTON_TEXT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.page.GarageAddPage.ADD_INPUT_PLACEHOLDER;
import static ru.auto.tests.desktop.page.GarageAddPage.ADD_TO_GARAGE;
import static ru.auto.tests.desktop.page.GarageAddPage.PUT_INTO_GARAGE;
import static ru.auto.tests.desktop.page.GarageCardPage.ALL_PARAMETERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;



@DisplayName("Гараж")
@Story("Страница добавления автомобиля/машины мечты в гараж")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageAddTest {

    private static final String MARK = "Volkswagen";
    private static final String MODEL = "Jetta";
    private static final String VIN = "WVWZZZ16ZBM121912";
    private static final String VIN_CARD_ID = "/1146321503/";
    private static final String LICENSE_PLATE = "Y019PY197";
    private static final String LICENSE_PLATE_CARD_ID = "/1373191029/";
    private static final String MAIN_REGION = "Москва и Московская область";
    private static final String POST_BUTTON_TEXT = "Поставить";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest")).create();

        urlSteps.testing().path(GARAGE).path(ADD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onGarageAddPage().h1().should(hasText(PUT_INTO_GARAGE_BUTTON));
        basePageSteps.onGarageAddPage().myCarBlock().should(hasText(format("%s\n%s", MY_CAR_BLOCK_TEXT, POST_BUTTON_TEXT)));
        basePageSteps.onGarageAddPage().dreamCarBlock().should(hasText(DREAM_CAR_BLOCK_TEXT));
        basePageSteps.onGarageAddPage().myExCarBlock().should(hasText(format("%s\n%s", EX_CAR_BLOCK_TEXT, POST_BUTTON_TEXT)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «?»")
    public void shouldClickQuestionButton() {
        basePageSteps.onGarageAddPage().myCarBlock().questionButton().waitUntil(isDisplayed()).click();
        basePageSteps.onGarageAddPage().popup().waitUntil(hasText(QUESTION_BUTTON_TEXT));
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

        basePageSteps.onGarageAddPage().myCarBlock().input(LICENCE_PLATE_OR_VIN, VIN);
        basePageSteps.onGarageAddPage().myCarBlock().button(PUT_INTO_GARAGE).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().myCarBlock().should(hasText(MY_CAR_BLOCK_TEXT_ADD));
        basePageSteps.onGarageAddPage().myCarBlock().button(ADD_TO_GARAGE).click();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().h1().should(hasText(format("%s %s", MARK, MODEL)));
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

        basePageSteps.onGarageAddPage().myCarBlock().input(LICENCE_PLATE_OR_VIN, LICENSE_PLATE);
        basePageSteps.onGarageAddPage().myCarBlock().button(PUT_INTO_GARAGE).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().myCarBlock().should(hasText(MY_CAR_BLOCK_TEXT_ADD));
        basePageSteps.onGarageAddPage().myCarBlock().button(ADD_TO_GARAGE).click();
        urlSteps.testing().path(GARAGE).path(LICENSE_PLATE_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().h1().should(hasText(format("%s %s", MARK, MODEL)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление уже добавленного автомобиля по VIN")
    public void shouldAddAlreadyAddedVin() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoVinAlreadyAdded"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin")).update();

        basePageSteps.onGarageAddPage().myCarBlock().input(LICENCE_PLATE_OR_VIN, VIN);
        basePageSteps.onGarageAddPage().myCarBlock().button(PUT_INTO_GARAGE).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().myCarBlock().should(hasText(MY_CAR_BLOCK_TEXT_PASS));
        basePageSteps.onGarageAddPage().myCarBlock().button("Перейти в гараж").click();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().h1().should(hasText(format("%s %s", MARK, MODEL)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Добавление автомобиля по госномеру с изменением региона")
    public void shouldAddLicensePlateWithEditRegion() {
        mockRule.setStubs(stub("desktop/GarageUserVehicleInfoLicensePlate"),
                stub("desktop/GarageUserCardIdentifierLicensePlate"),
                stub("desktop/GarageUserCardsLicensePlatePost"),
                stub("desktop/GarageUserCardLicensePlateHimki"),
                stub("desktop/GeoSuggest")).update();

        basePageSteps.onGarageAddPage().myCarBlock().input(ADD_INPUT_PLACEHOLDER, LICENSE_PLATE);
        basePageSteps.onGarageAddPage().myCarBlock().button(PUT_INTO_GARAGE).click();
        urlSteps.shouldNotSeeDiff();

        basePageSteps.onGarageAddPage().myCarBlock().button("Москва").click();
        basePageSteps.onGarageAddPage().myCarBlock().popup().input(CARD_REGION_PLACEHOLDER, HIMKI);
        basePageSteps.onGarageAddPage().myCarBlock().popup().geoSuggest().region(format("%s%s", HIMKI,
                MAIN_REGION)).waitUntil(isDisplayed()).click();
        basePageSteps.onGarageAddPage().myCarBlock().waitUntil(hasText(MY_CAR_BLOCK_TEXT_ADD_WITH_EDIT_REGION));

        basePageSteps.onGarageAddPage().myCarBlock().button(ADD_TO_GARAGE).click();
        urlSteps.testing().path(GARAGE).path(LICENSE_PLATE_CARD_ID).shouldNotSeeDiff();

        basePageSteps.onGarageCardPage().button(ALL_PARAMETERS).click();
        basePageSteps.onGarageCardPage().form().waitUntil(isDisplayed());

        basePageSteps.onGarageCardPage().form().block(format("%s%s", REGION_FIELD, HIMKI)).should(isDisplayed());
    }
}

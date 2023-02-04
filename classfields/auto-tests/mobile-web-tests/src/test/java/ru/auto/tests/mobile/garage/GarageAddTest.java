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
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CURRENT;
import static ru.auto.tests.desktop.consts.Pages.DREAMCAR;
import static ru.auto.tests.desktop.consts.Pages.EXCAR;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.QueryParams.VIN_OR_LICENSE_PLATE;
import static ru.auto.tests.desktop.element.garage.DreamCarBlock.DREAM_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.EX_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.MY_CAR_BLOCK_TEXT;
import static ru.auto.tests.desktop.element.garage.MyCarBlock.PUT_INTO_GARAGE_BUTTON;
import static ru.auto.tests.desktop.mobile.element.garage.DreamCarBlock.CHANGE_DREAM_CAR;
import static ru.auto.tests.desktop.mobile.page.GarageAddPage.LICENCE_PLATE_OR_VIN;
import static ru.auto.tests.desktop.mobile.page.GarageAddPage.PUT_VIN;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Гараж")
@Feature(AutoruFeatures.GARAGE)
@Story("Страница добавления автомобиля/машины мечты в гараж")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageAddTest {

    private static final String VIN = "WVWZZZ16ZBM121912";
    private static final String BAD_VIN = "WVWZZZ16ZBM12191";

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
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest")
        ).create();

        urlSteps.testing().path(GARAGE).path(ADD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onGarageAddPage().h1().should(hasText(PUT_INTO_GARAGE_BUTTON));
        basePageSteps.onGarageAddPage().myCarBlock().should(hasText(MY_CAR_BLOCK_TEXT));
        basePageSteps.onGarageAddPage().dreamCarBlock().should(hasText(DREAM_CAR_BLOCK_TEXT));
        basePageSteps.onGarageAddPage().myExCarBlock().should(hasText(EX_CAR_BLOCK_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Поиск автомобиля по VIN/Госномеру")
    public void shouldClickSearchCarByVinButton() {
        basePageSteps.onGarageAddPage().myCarBlock().input(LICENCE_PLATE_OR_VIN, VIN);
        basePageSteps.onGarageAddPage().myCarBlock().searchButton().click();

        urlSteps.testing().path(GARAGE).path(ADD).path(CURRENT).addParam(VIN_OR_LICENSE_PLATE, VIN)
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Поиск автомобиля по несуществующему VIN/Госномеру")
    public void shouldClickSearchCarByErrorVinButton() {
        basePageSteps.onGarageAddPage().myCarBlock().input(LICENCE_PLATE_OR_VIN, BAD_VIN);
        basePageSteps.onGarageAddPage().myCarBlock().searchButton().click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onGarageAddPage().myCarBlock().errorText().should(hasText("Введите правильный VIN/госномер."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Кнопка добавления машины мечты")
    public void shouldClickDreamCarButton() {
        basePageSteps.onGarageAddPage().dreamCarBlock().button(CHANGE_DREAM_CAR).click();

        urlSteps.testing().path(GARAGE).path(ADD).path(DREAMCAR).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поиск бывшего автомобиля по VIN")
    public void shouldSearchExCarByVinButton() {
        basePageSteps.onGarageAddPage().myExCarBlock().input(PUT_VIN, VIN);
        basePageSteps.onGarageAddPage().myExCarBlock().searchButton().click();

        urlSteps.testing().path(GARAGE).path(ADD).path(EXCAR).addParam(QueryParams.VIN, VIN)
                .shouldNotSeeDiff();
    }

}

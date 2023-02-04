package ru.auto.tests.forms.dealer;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONTACTS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Дилер - редактирование реквизитов")
@Feature(FORMS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RequisitesTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salePath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "/cars/used/sale/"},
                {MOTO, "/motorcycle/new/sale/"},
                {TRUCKS, "/lcv/new/sale/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "forms/UserOffersCarsRequisites",
                "forms/OfferCars",
                "forms/UserOffersTrucksRequisites",
                "forms/OfferTrucks",
                "forms/UserOffersMotoRequisites",
                "forms/OfferMoto").post();

        urlSteps.testing().path(category).path(CONTACTS).path(EDIT).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение формы")
    @Category({Regression.class, Screenshooter.class})
    public void shouldSeeForm() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsRequisitesPage().content());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsRequisitesPage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сохранение реквизитов")
    public void shouldSaveRequisites() {
        mockRule.with("forms/UserOffersCarsRequisitesPut",
                "forms/UserOffersTrucksRequisitesPut",
                "forms/UserOffersMotoRequisitesPut").update();

        String address = "Россия, Москва, Московская кольцевая автомобильная дорога";
        formsSteps.onFormsRequisitesPage()
                .input("Город, улица, номер дома или бокса", address);
        formsSteps.onFormsRequisitesPage()
                .input("Город, улица, номер дома или бокса")
                .waitUntil(hasAttribute("value", address));
        formsSteps.onFormsRequisitesPage().geoSuggest().region(address).waitUntil(isDisplayed())
                .click();
        formsSteps.onFormsRequisitesPage().input("Укажите имя менеджера", "MINOR EXPERT");
        formsSteps.onFormsRequisitesPage().input("phone-0", "+74951261706");
        formsSteps.onFormsRequisitesPage().input("from-0", "12");
        formsSteps.onFormsRequisitesPage().input("to-0", "19");
        formsSteps.onFormsRequisitesPage().button("Сохранить").click();
        urlSteps.testing().path(salePath).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс реквизитов")
    public void shouldResetRequisites() {
        mockRule.with("forms/UserOffersCarsRequisitesEmptyPut",
                "forms/UserOffersTrucksRequisitesEmptyPut",
                "forms/UserOffersMotoRequisitesEmptyPut").update();

        formsSteps.onFormsRequisitesPage().button("Сбросить все реквизиты").click();
        urlSteps.testing().path(salePath).path(SALE_ID).shouldNotSeeDiff();
    }
}

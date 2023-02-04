package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.page.FormPage.VESCHI;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Форма подачи оффера. Скриншот шагов незалогином")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AddOfferStepsUnauthorizedScreenshotTest {

    private static final String TELEFON = "Телефон";
    private static final String MULTISELECT_ATTRIBUTE = "Тип питания";
    private static final String SELECT_ATTRIBUTE = "Производитель";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок пресетов")
    public void shouldSeeFormPresetsScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блока пресетов, ховер на пресете")
    public void shouldSeeFormPresetsHoverScreenshot() {
        offerAddSteps.onFormPage().section(VESCHI).hover();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().section(VESCHI).hover();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Этапа ввода тайтла и фото")
    public void shouldSeeFormTitleStepScreenshot() {
        offerAddSteps.onFormPage().section(VESCHI).click();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Саджест категории")
    public void shouldSeeFormCategoryStepScreenshot() {
        offerAddSteps.withName(TELEFON).fillToCategoryStep();
        compareSteps.resize(1920, 3000);
        offerAddSteps.scrollToTop();
        offerAddSteps.onFormPage().h1().click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().h1().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Второй уточняющий саджест категории")
    public void shouldSeeFormCategorySecondSuggestStepScreenshot() {
        offerAddSteps.withName(TELEFON).fillToCategoryStep();
        compareSteps.resize(1920, 3000);
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.scrollToTop();
        offerAddSteps.onFormPage().h1().click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.scrollToTop();
        offerAddSteps.onFormPage().h1().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап выбора категории")
    public void shouldSeeFormCategoryPopupScreenshot() {
        offerAddSteps.withName(TELEFON).fillToCategoryStep();
        compareSteps.resize(1920, 3000);
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().modal().content());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().modal().content());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Этапа выбора категории с выбранной категорией")
    public void shouldSeeFormCategoryFilledStepScreenshot() {
        offerAddSteps.fillToDescriptionStep();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        compareSteps.resize(1920, offerAddSteps.getMaxPageHeight());

        offerAddSteps.refresh();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.scrollToTop();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Этап атрибутов незалогином, атрибуты развернуты")
    public void shouldSeeFormAttributesOpenStepUnauthorizedScreenshot() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.wait500MS();
        compareSteps.resize(1920, 3000);
        offerAddSteps.refresh();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().spoilerOpen().click();
        offerAddSteps.scrollToTop();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().spoilerOpen().click();
        offerAddSteps.scrollToTop();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот выпадающего мультиселект атрибута")
    public void shouldSeeMultiselectAttributeScreenshot() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        compareSteps.resize(1920, 3000);
        offerAddSteps.onFormPage().spoilerOpen().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MULTISELECT_ATTRIBUTE).input().click();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().spoilerOpen().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MULTISELECT_ATTRIBUTE).input().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот выпадающего селект атрибута")
    public void shouldSeeSelectAttributeScreenshot() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        compareSteps.resize(1920, 3000);
        offerAddSteps.onFormPage().spoilerOpen().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(SELECT_ATTRIBUTE).input().click();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().spoilerOpen().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(SELECT_ATTRIBUTE).input().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

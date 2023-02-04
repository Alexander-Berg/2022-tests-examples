package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики»")
@DisplayName("Скриншот селект атрибутов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AddFormControlsScreenshotTest {

    private static final String TIP_PITANIYA = "Тип питания";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String MANUFACTURER = "Производитель";

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
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        offerAddSteps.resize(375, 2000);
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот экрана атрибутов селекта")
    public void shouldSeeAttributeSelectPopup() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот экрана атрибутов мультиселекта")
    public void shouldSeeAttributeMultiselectPopup() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот атрибутов селекта, значение выбрано")
    public void shouldSeeAttributeSelectChoosedPopup() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().menuItem("Apple").waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот атрибутов мультиселекта, значение выбрано")
    public void shouldSeeAttributeMultiselectChoosedPopup() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().menuItem(FROM_AKKUM).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().closeFloatPopup().click();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

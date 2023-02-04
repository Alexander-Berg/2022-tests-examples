package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.CONTACTS;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.page.FormPage.NAZVANIE;
import static ru.yandex.general.page.FormPage.VESCHI;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Форма подачи оффера. Скриншот блока контактов с разными выбранными контактами")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddOfferContactsStepScreenshotTest {

    private static final String FOTOAPPARAT = "Фотоаппарат";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String contactType;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Только сообщения"},
                {"Только звонки"},
                {"Звонки и сообщения"}
        });
    }

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Форма подачи оффера. Скриншот блока контактов с разными выбранными контактами")
    public void shouldSeeContactsStepScreenshot() {
        offerAddSteps.fillToDescriptionStep();
        offerAddSteps.onFormPage().contactType(contactType).waitUntil(isDisplayed()).click();
        offerAddSteps.scrollToTop();
        compareSteps.resize(1920, 4000);

        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onFormPage().field(CONTACTS));

        urlSteps.setProductionHost().open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.onFormPage().h1().click();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onFormPage().field(CONTACTS));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

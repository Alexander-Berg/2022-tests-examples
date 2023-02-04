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
import ru.yandex.general.rules.MockRule;
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
import static ru.yandex.general.mock.MockCurrentDraft.FINAL_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.mockCurrentDraft;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(ADD_FORM_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Форма подачи оффера. Скриншот заполенного черновика")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddOfferScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String theme;

    @Parameterized.Parameters(name = "{index}. Тема «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {LIGHT_THEME},
                {DARK_THEME}
        });
    }

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        compareSteps.resize(1920, 4800);
        mockRule.graphqlStub(mockResponse()
                .setCurrentDraft(mockCurrentDraft(FINAL_SCREEN).build())
                .setCategoriesTemplate()
                .setCategoryTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();
        offerAddSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Форма подачи оффера. Скриншот заполенного черновика, светлая/темная темы")
    public void shouldSeeFormFieldsScreenshot() {
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                offerAddSteps.onFormPage().pageRoot(),
                offerAddSteps.onFormPage().videoFrame(),
                offerAddSteps.onFormPage().map());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                offerAddSteps.onFormPage().pageRoot(),
                offerAddSteps.onFormPage().videoFrame(),
                offerAddSteps.onFormPage().map());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

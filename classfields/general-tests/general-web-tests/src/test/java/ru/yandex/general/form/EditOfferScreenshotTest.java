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
import static ru.yandex.general.consts.GeneralFeatures.EDIT_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(EDIT_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот формы редактирования")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditOfferScreenshotTest {

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
        passportSteps.accountWithOffersLogin();
        offerAddSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот формы редактирования, светлая/темная темы")
    public void shouldSeeEditOfferScreenshot() {
        compareSteps.resize(1920, 3200);
        offerAddSteps.onMyOffersPage().snippetFirst().spanLink(EDIT).click();
        String offerId = urlSteps.getOfferId();
        offerAddSteps.onFormPage().h1().click();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                offerAddSteps.onFormPage().pageMain(),
                offerAddSteps.onFormPage().videoFrame(),
                offerAddSteps.onFormPage().map());

        urlSteps.testing().path(FORM).path(offerId).setProductionHost().open();
        offerAddSteps.onFormPage().h1().click();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                offerAddSteps.onFormPage().pageMain(),
                offerAddSteps.onFormPage().videoFrame(),
                offerAddSteps.onFormPage().map());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

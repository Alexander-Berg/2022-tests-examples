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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.CONTINUE;
import static ru.yandex.general.mock.MockCurrentDraft.CONTACTS_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.mockCurrentDraft;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(ADD_FORM_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот раздела «Контакты» на форме подачи оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AddOfferContactsScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        mockRule.graphqlStub(mockResponse()
                .setCurrentUserExample()
                .setCurrentDraft(mockCurrentDraft(CONTACTS_SCREEN).build())
                .setCategoriesTemplate()
                .setCategoryTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.testing().path(FORM).open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот раздела «Контакты» на форме подачи оффера")
    public void shouldSeeFormScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        urlSteps.setProductionHost().open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

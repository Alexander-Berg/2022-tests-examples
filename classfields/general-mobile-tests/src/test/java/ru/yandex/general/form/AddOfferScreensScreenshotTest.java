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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.CONTINUE;
import static ru.yandex.general.mock.MockCurrentDraft.ATTRIBUTES_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.ATTRIBUTES_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.CATEGORY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.CATEGORY_SELECTED_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.CONDITION_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.CONDITION_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.DESCRIPTION_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.DESCRIPTION_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.FINAL_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.MEDIA_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.MEDIA_SCREEN_WITH_PHOTO;
import static ru.yandex.general.mock.MockCurrentDraft.PLACE_OF_DEAL_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.PLACE_OF_DEAL_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.PRICE_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.PRICE_FREE_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.PRICE_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.TITLE_EMPTY_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.TITLE_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.mockCurrentDraft;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(ADD_FORM_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты формы подачи оффера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddOfferScreensScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String currentDraft;

    @Parameterized.Parameters(name = "Скриншот формы подачи оффера. Раздел «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Название не указано", TITLE_EMPTY_SCREEN},
                {"Название указано", TITLE_SCREEN},
                {"Фото и видео. Без фото", MEDIA_SCREEN},
                {"Фото и видео. Добавлена фотография", MEDIA_SCREEN_WITH_PHOTO},
                {"Категории не выбраны", CATEGORY_SCREEN},
                {"Категории. выбрана категория", CATEGORY_SELECTED_SCREEN},
                {"Описание не указано", DESCRIPTION_EMPTY_SCREEN},
                {"Описание указано", DESCRIPTION_SCREEN},
                {"Состояние не указано", CONDITION_EMPTY_SCREEN},
                {"Состояние указано", CONDITION_SCREEN},
                {"Атрибуты не выбраны", ATTRIBUTES_EMPTY_SCREEN},
                {"Атрибуты выбраны", ATTRIBUTES_SCREEN},
                {"Цена не указана", PRICE_EMPTY_SCREEN},
                {"Цена указана", PRICE_SCREEN},
                {"Цена даром", PRICE_FREE_SCREEN},
                {"Место сделки не указано", PLACE_OF_DEAL_EMPTY_SCREEN},
                {"Место сделки 3 адреса", PLACE_OF_DEAL_SCREEN},
                {"Финальный этап", FINAL_SCREEN}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentDraft(mockCurrentDraft(currentDraft).build())
                .setCategoriesTemplate()
                .setCategoryTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.testing().path(FORM);

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот формы подачи оффера")
    public void shouldSeeFormScreenshot() {
        urlSteps.open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        urlSteps.setProductionHost().open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот формы подачи оффера, темная тема")
    public void shouldSeeFormDarkThemeScreenshot() {
        basePageSteps.setDarkThemeCookie();
        urlSteps.open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        urlSteps.setProductionHost().open();
        basePageSteps.onFormPage().button(CONTINUE).click();
        basePageSteps.onFormPage().screenTitle().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFormPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}

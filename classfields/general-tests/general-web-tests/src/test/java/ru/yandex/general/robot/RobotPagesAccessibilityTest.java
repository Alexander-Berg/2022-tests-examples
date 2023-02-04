package ru.yandex.general.robot;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.ROBOT_ACCESSIBILITY_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;

@Epic(ROBOT_ACCESSIBILITY_FEATURE)
@DisplayName("Страницы сервиса доступны роботу, есть H1")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RobotPagesAccessibilityTest {

    private static final String H1_LOCATOR = "body h1";

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String h1;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Главная", "", "Объявления в России"},
                {"Листинг родительской категории с городом", "/moskva/elektronika/", "Электроника в Москве"},
                {"Листинг конечной категории", "/komputernaya-tehnika/noutbuki/", "Ноутбуки в России"},
                {"Поиск", "/moskva/tag/noutbuk/", "Объявления по запросу «ноутбук» в Москве"},
                {"Форма", "/form/", FORM_PAGE_H1},
                {"Форма", "/add/dacha-sad-i-ogorod/", "Разместить объявление о продажеДача, сад и огород"},
                {"Профиль продавца", "profile" + SELLER_PATH, "Vasily Pupkin"}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Страницы сервиса доступны роботу, есть H1")
    public void shouldSeeH1ByRobot() {
        jSoupSteps.testing().path(path).setDesktopRobotUserAgent().get();
        String actualTitle = jSoupSteps.select(H1_LOCATOR).text();

        assertThat("H1 на странице соответствует", actualTitle, equalTo(h1));
    }

}

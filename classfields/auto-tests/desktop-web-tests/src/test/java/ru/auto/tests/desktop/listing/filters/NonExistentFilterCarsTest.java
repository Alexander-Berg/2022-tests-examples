package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг")
@Story("Несуществующие модификации")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class NonExistentFilterCarsTest {

    private static final String MARK = "Tesla";
    private static final String ENGINE_BENZIN = "/engine-benzin/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Выбор/сброс автомобиля в фильтрах")
    public void shouldNotSee404() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ENGINE_BENZIN).open();
        basePageSteps.onListingPage().filter().selectItem("Марка", MARK);
        basePageSteps.onListingPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().emptyResultButton().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().resetButton().click();
        basePageSteps.onListingPage().filter().submitButton().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("404 по прямой ссылке")
    public void shouldSee404() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(ALL).path(ENGINE_BENZIN).open();
        basePageSteps.onListingPage().title()
                .should(hasAttribute("textContent", "Ошибка 404! Страница не найдена. - AUTO.RU"));
    }
}
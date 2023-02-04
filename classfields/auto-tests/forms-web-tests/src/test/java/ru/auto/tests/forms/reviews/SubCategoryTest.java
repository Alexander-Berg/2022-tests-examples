package ru.auto.tests.forms.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - переключатель подкатегорий")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SubCategoryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Обновление списка марок после выбора подкатегории")
    public void shouldRefreshMarksList() {
        urlSteps.testing().path(MOTO).path(REVIEWS).path(ADD).open();
        basePageSteps.onFormsPage().unfoldedBlock("Категория").radioButton("Мотоциклы").click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onFormsPage().unfoldedBlock("Укажите марку").button("Все марки")
                .should(isDisplayed()).click();
        basePageSteps.onFormsPage().unfoldedBlock("Укажите марку").radioButton("Aermacchi")
                .should(isDisplayed());
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onFormsPage().unfoldedBlock("Категория").radioButton("Снегоходы").click();
        basePageSteps.onFormsPage().unfoldedBlock("Укажите марку").radioButton("Arctic Cat")
                .should(isDisplayed());
    }
}
package ru.auto.tests.desktop.prevnext;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Переключение на предыдущее/следующее объявление на карточке")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PrevNextSpaTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Тип транспорта")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(MOSKVA).path(category).path(USED).open();
        basePageSteps.onListingPage().getSale(0).nameLink().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().footer().hover();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение на следующее/предыдущее объявление")
    public void shouldNavigateOnCard() {
        String title = basePageSteps.onCardPage().cardHeader().firstLine().getText();
        String sellerComment = basePageSteps.onCardPage().sellerComment().getText();

        basePageSteps.onCardPage().stickyBar().next().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().footer().hover();
        basePageSteps.onCardPage().cardHeader().firstLine().should(not(hasText(title)));
        basePageSteps.onCardPage().sellerComment().should(not(hasText(sellerComment)));
    }
}

package ru.auto.tests.desktopreviews.header;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;

@DisplayName("Шапка - кнопка «Продать»")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderSellButtonTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "/cars/used/"},
                {CARS, "/cars/used/"},
                {MOTO, MOTO},
                {TRUCKS, TRUCKS}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Продать»")
    public void shouldClickSellButton() {
        urlSteps.testing().path(REVIEWS).path(startUrl).open();
        basePageSteps.onReviewsMainPage().header().button("Разместить бесплатно").click();
        urlSteps.testing().path(category).path(ADD).shouldNotSeeDiff();
    }
}

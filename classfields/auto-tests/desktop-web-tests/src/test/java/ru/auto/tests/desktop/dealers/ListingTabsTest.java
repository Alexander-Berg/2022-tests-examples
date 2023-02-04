package ru.auto.tests.desktop.dealers;

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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;

@Feature(DEALERS)
@DisplayName("Дилеры - вкладки")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameter(1)
    public String tabTitle;

    @Parameterized.Parameter(2)
    public String tabUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {USED, "Объявления", "https://%s/moskva/cars/used/"},
                {USED, "Дилеры", "https://%s/moskva/dilery/cars/used/"},
                {USED, "Каталог", "https://%s/catalog/cars/"},
                {USED, "Отзывы", "https://%s/reviews/"},
                {USED, "Видео", "https://%s/video/"},
                {NEW, "Объявления", "https://%s/moskva/cars/new/"},
                {NEW, "Дилеры", "https://%s/moskva/dilery/cars/new/"},
                {NEW, "Каталог", "https://%s/catalog/cars/"},
                {NEW, "Отзывы", "https://%s/reviews/"},
                {NEW, "Видео", "https://%s/video/"},
                {ALL, "Объявления", "https://%s/moskva/cars/all/"},
                {ALL, "Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {ALL, "Каталог", "https://%s/catalog/cars/"},
                {ALL, "Отзывы", "https://%s/reviews/"},
                {ALL, "Видео", "https://%s/video/"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Вкладки")
    public void shouldClickSubHeaderUrl() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(section).open();
        user.onDealerListingPage().subHeader().button(tabTitle).click();
        urlSteps.fromUri(format(tabUrl, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
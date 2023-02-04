package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotEquals;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;

@DisplayName("Обновление листинга")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReloadTest {

    private List<String> salesBeforeReload;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String outputType;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "list"},
                {CARS, "table"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        salesBeforeReload = basePageSteps.onListingPage().salesList().stream()
                .map(element -> element.nameLink().getAttribute("href")).collect(toList());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листинг должен обновляться после смены сортировки")
    public void shouldReloadListingAfterSort() {
        basePageSteps.onListingPage().sortBar().selectItem("Сортировка", DATE_DESC.getName());
        checkListingReloaded();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листинг должен обновляться после смены периода")
    public void shouldReloadListingAfterPeriod() {
        basePageSteps.onListingPage().sortBar().selectItem("Время размещения", "За сутки");
        checkListingReloaded();
    }

    @Step("Проверяем, что листинг обновился")
    private void checkListingReloaded() {
        await().pollInterval(1, SECONDS).atMost(10, SECONDS).untilAsserted(() -> {
            List<String> salesAfterReload = basePageSteps.onListingPage().salesList().stream()
                    .map(element -> element.nameLink().getAttribute("href")).collect(toList());
            assertNotEquals("Листинг не обновился", salesBeforeReload, salesAfterReload);
        });
    }
}
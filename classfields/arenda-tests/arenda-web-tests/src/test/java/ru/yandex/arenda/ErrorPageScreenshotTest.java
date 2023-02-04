package ru.yandex.arenda;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Скриншоты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class ErrorPageScreenshotTest {

    public static final String TEXT_404 = "Нет такой страницы";
    public static final String TEXT_500 = "Произошла ошибка";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private PassportSteps passportSteps;

    @Test
    @DisplayName("Скриншот 404")
    public void shouldSee404() {
        compareSteps.resizeDesktop();
        urlSteps.testing().path("/wrongpath/").open();
        mainSteps.onBasePage().h1().waitUntil(hasText(TEXT_404));
        Screenshot testing = compareSteps.takeScreenshot(mainSteps.onBasePage().root());
        urlSteps.setProductionHost().open();
        mainSteps.onBasePage().h1().waitUntil(hasText(TEXT_404));
        Screenshot production = compareSteps.takeScreenshot(mainSteps.onBasePage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Скриншот 500")
    public void shouldSee500() {
        passportSteps.adminLogin();
        compareSteps.resizeDesktop();
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam("disable-api", "get").open();
        mainSteps.onBasePage().h1().waitUntil(hasText(TEXT_500));
        Screenshot testing = getIgnoredScreenshot();
        urlSteps.setProductionHost().open();
        mainSteps.onBasePage().h1().waitUntil(hasText(TEXT_500));
        Screenshot production = getIgnoredScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private Screenshot getIgnoredScreenshot() {
        return compareSteps.takeScreenshotWithIgnore(mainSteps.onBasePage().main500(),
                compareSteps.getCoordsFor(mainSteps.onBasePage().requestId()),
                compareSteps.getCoordsFor(mainSteps.onBasePage().qrCode())
        );
    }
}

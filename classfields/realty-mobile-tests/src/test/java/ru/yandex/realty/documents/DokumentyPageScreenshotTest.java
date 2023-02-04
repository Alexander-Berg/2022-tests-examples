package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;

@DisplayName("Страница документов")
@Feature(MOBILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DokumentyPageScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(375, 5000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Аренда»")
    public void shouldSeeDocsRentPage() {
        urlSteps.testing().path(DOKUMENTY).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Купля-продажа»")
    public void shouldSeeDocsBuyPage() {
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Договора»")
    public void shouldSeeDocsPage() {
        urlSteps.testing().path(DOKUMENTY).path("dogovor-kupli-prodazhi-nedvizhimogo-imushchestva/").open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}

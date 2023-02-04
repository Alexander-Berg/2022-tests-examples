package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница документов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocPageScreenshotTest {

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
        compareSteps.resize(1920, 3000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Аренда»")
    public void shouldSeeDocsRentPage() {
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDocumentsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}

package ru.yandex.realty.pageScreenshoot;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.SCROOGE;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;

@DisplayName("Документы. Скриншоты")
@Feature(MOBILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DocumentsPageScreenshotTest {
    public static final String ONLY_CONTENT = "only-content";
    public static final String TRUE = "true";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Mobile.class, Screenshooter.class})
    @Owner(SCROOGE)
    public void shouldSeeDocumentPageWebViewContent() {
        urlSteps.testing().path(DOKUMENTY).queryParam(ONLY_CONTENT, TRUE).open();
        compareSteps.resize(1280, 5000);
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(user.onDocumentsPage()
                .pageContent().waitUntil(isDisplayed()));

        urlSteps.production().path(DOKUMENTY).queryParam(ONLY_CONTENT, TRUE).open();
        compareSteps.resize(1280, 5000);
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(user.onDocumentsPage()
                .pageContent().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Mobile.class, Screenshooter.class})
    @Owner(SCROOGE)
    public void shouldSeeDocumentPageContent() {
        urlSteps.testing().path(DOKUMENTY).open();
        compareSteps.resize(1280, 5000);
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(user.onDocumentsPage()
                .pageContent().waitUntil(isDisplayed()));

        urlSteps.production().path(DOKUMENTY).open();
        compareSteps.resize(1280, 5000);
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(user.onDocumentsPage()
                .pageContent().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}

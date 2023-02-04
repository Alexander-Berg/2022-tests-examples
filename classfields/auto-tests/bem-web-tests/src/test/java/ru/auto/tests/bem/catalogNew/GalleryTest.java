package ru.auto.tests.bem.catalogNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CATALOG_NEW;
import static ru.auto.tests.desktop.consts.AutoruFeatures.GALLERY;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели - Галерея")
@Epic(CATALOG_NEW)
@Feature(GALLERY)
public class GalleryTest {

    private static final String GENERATION = "21745628";
    private static final String MODIFICATION = "21746366";
    private static final Integer MIN_THUMBS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение блока галереи")
    public void shouldSeeGallery() {
        screenshotSteps.setWindowSizeForScreenshot();

        catalogPageSteps.onCatalogNewPage().gallery().getThumb(0).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogNewPage().gallery());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogNewPage().gallery().getThumb(0).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogNewPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Переключение фотографий по ховеру")
    public void shouldSwitchPhotosByHover() {
        screenshotSteps.setWindowSizeForScreenshot();

        catalogPageSteps.onCatalogNewPage().gallery().thumbList().should(hasSize(greaterThanOrEqualTo(MIN_THUMBS_CNT)))
                .get(2).should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogNewPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        catalogPageSteps.onCatalogNewPage().gallery().thumbList().should(hasSize(greaterThanOrEqualTo(MIN_THUMBS_CNT)))
                .get(2).should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogNewPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход в старую карточку модели по клику на галерею")
    public void shouldClickToGallery() {
        catalogPageSteps.onCatalogNewPage().gallery().getThumb(0).click();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION).path(MODIFICATION).path(SLASH).shouldNotSeeDiff();
        catalogPageSteps.onCatalogPage().gallery().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поделяшки")
    public void shouldClickShareIcon() {
        catalogPageSteps.onCatalogNewPage().gallery().shareButton().click();
        catalogPageSteps.onCatalogNewPage().gallery().shareDropdown().vk().waitUntil(isDisplayed()).click();

        urlSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith("https://oauth.vk.com/authorize"));
    }

}

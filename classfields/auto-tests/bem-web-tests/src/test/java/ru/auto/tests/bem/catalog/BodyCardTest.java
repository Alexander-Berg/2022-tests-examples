package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.EQUIPMENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка кузова")
@Feature(AutoruFeatures.CATALOG)
public class BodyCardTest {

    private static final String MARK = "Audi";
    private static final String MODEL = "A5";
    private static final String GENERATION = "20795592";
    private static final String BODY = "20795627";
    private static final Integer MIN_THUMBS_CNT = 6;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION).path(BODY).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        screenshotSteps.setWindowSizeForScreenshot();

        catalogPageSteps.onCatalogPage().gallery().getThumb(0).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogPage().gallery().getThumb(0).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Просмотр панорамы")
    public void shouldSeePanorama() {
        catalogPageSteps.setNarrowWindowSize(3000);

        catalogPageSteps.onCatalogPage().gallery().panorama().should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        catalogPageSteps.onCatalogPage().gallery().panorama().should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Переключение фото по ховеру")
    public void shouldSwitchPhotosByHover() {
        screenshotSteps.setWindowSizeForScreenshot();

        catalogPageSteps.onCatalogPage().gallery().thumbList().should(hasSize(greaterThanOrEqualTo(MIN_THUMBS_CNT)))
                .get(2).should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        catalogPageSteps.onCatalogPage().gallery().thumbList().should(hasSize(greaterThanOrEqualTo(MIN_THUMBS_CNT)))
                .get(2).should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход в полноэкранный режим по клику на фото")
    public void shouldOpenAndCloseFullscreenMode() {
        catalogPageSteps.onCatalogPage().gallery().thumbList().should(hasSize(greaterThanOrEqualTo(MIN_THUMBS_CNT)))
                .get(0).should(isDisplayed()).click();
        catalogPageSteps.onCardPage().fullScreenGallery().waitUntil(isDisplayed());
        catalogPageSteps.onCardPage().fullScreenGallery().currentImage()
                .should(hasAttribute("src", getGalleryActiveImageUrl()));
        catalogPageSteps.onCardPage().fullScreenGallery().closeButton().click();
        catalogPageSteps.onCardPage().fullScreenGallery().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Описание")
    public void shouldSeeDescription() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().bodyAbout());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().bodyAbout());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Комплектации")
    public void shouldSeeComplectations() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().bodyComplectations());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().bodyComplectations());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по комплектации")
    @Category({Regression.class})
    public void shouldClickComplectationUrl() {
        catalogPageSteps.onCatalogPage().bodyComplectation().waitUntil(isDisplayed()).click();
        urlSteps.path(EQUIPMENT).path("/20795627_20861937_20795681/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение похожих")
    @Category({Regression.class})
    public void shouldSeeRelatedModels() {
        catalogPageSteps.onCatalogPage().horizontalRelated().should(isDisplayed());
        catalogPageSteps.onCatalogPage().horizontalRelated().itemsList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по похожей модели")
    @Category({Regression.class})
    public void shouldClickRelatedModel() {
        String firstItemTitle = catalogPageSteps.onCatalogPage().horizontalRelated().getItem(0).title().getText()
                .replaceAll("\\s.+", "");
        catalogPageSteps.onCatalogPage().horizontalRelated().getItem(0).click();
        catalogPageSteps.onCatalogPage().title()
                .should(hasAttribute("textContent", containsString(firstItemTitle)));
    }

    private String getGalleryActiveImageUrl() {
        return format("https:%s", getMatchedString(catalogPageSteps.onCatalogPage().gallery().activePhoto()
                .getAttribute("style"), "url\\((.+)\\)").replaceAll("\"", ""));
    }

    private String getMatchedString(String str, String regexp) {
        Matcher m = Pattern.compile(regexp).matcher(str);
        m.find();
        return m.group(1);
    }
}

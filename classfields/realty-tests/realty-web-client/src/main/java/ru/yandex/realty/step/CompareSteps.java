package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.comparison.PointsMarkupPolicy;
import ru.yandex.qatools.ashot.coordinates.Coords;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.cropper.indent.IndentCropper;
import ru.yandex.realty.config.RealtyWebConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.awt.Color.MAGENTA;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * eroshenkoam
 * 24.01.17
 */
public class CompareSteps {

    @Inject
    private WebDriverManager wm;

    @Inject
    private RealtyWebConfig config;

    @Attachment(value = "{name}", type = "image/png")
    private byte[] attachImage(String name, BufferedImage image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        String format = "png";
        try {
            ImageIO.write(image, format, stream);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Не добавлять @Step, пока плагин для allure2 не научиться с ними работать
    public void screenshotsShouldBeTheSame(Screenshot testing, Screenshot production) {
        screenshotsShouldBeTheSame(testing, production, config.getDiffSize());
    }

    //Не добавлять @Step, пока плагин для allure2 не научиться с ними работать
    public void screenshotsShouldBeTheSame(Screenshot testing, Screenshot production, int diffSize) {
        Allure.getLifecycle().updateTestCase(testResult ->
                testResult.getLabels().add(new Label().setName("testType").setValue("screenshotDiff")));

        ImageDiff diff = new ImageDiffer().withDiffMarkupPolicy(new PointsMarkupPolicy().withDiffColor(MAGENTA))
                .makeDiff(production, testing);

        attachImage("diff", diff.getMarkedImage());
        attachImage("actual", testing.getImage());
        attachImage("expected", production.getImage());

        assertThat("разница в пикселях между скриншотом тестинга и продакшена", diff.getDiffSize(),
                lessThanOrEqualTo(diffSize));
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshot(WebElement element) {
        resize(1920, 3000);
        /* Ресайз делается из-за бага в Ashot: если до элемента надо прокручивать страницу, то делается скрин не элемента,
         а всей страницы. Потому если страница большая, на ней виден элемент без прокрутки, то бага нет */
        return takeScreenshot(element);
    }

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        wm.getDriver().manage().window().setSize(new Dimension(x, y));
    }

    @Step("Делаем скриншот {element}")
    public Screenshot takeScreenshot(WebElement element) {
        waitSomething(3, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), element);
    }

    @Step
    public Screenshot expScreen(WebElement element) {
        (new Actions(wm.getDriver())).moveToElement(element).build().perform();
        return new AShot().coordsProvider(new WebDriverCoordsProvider()).takeScreenshot(wm.getDriver(), element);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreAreas(WebElement elementToScreenshoot, Set<AtlasWebElement> areas) {
        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredAreas(areas.stream().map(element ->
                new Coords(element.getCoordinates().onPage().getX(),
                        element.getCoordinates().onPage().getY(),
                        element.getSize().getWidth(),
                        element.getSize().getHeight())).collect(toSet()))
                .takeScreenshot(wm.getDriver(), elementToScreenshoot);
    }

    @Step("Делаем скриншот {element}, игнорим {coords}")
    public Screenshot takeScreenshotWithIgnore(WebElement element, Coords coords) {
        waitSomething(3, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .addIgnoredArea(coords)
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), element);
    }

    @Step("Делаем скриншот {elements}")
    public Screenshot takePackScreenshots(Collection<WebElement> elements) {
        waitSomething(3, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), elements);
    }

    //Не добавлять @Step, пока плагин для allure2 не научиться с ними работать
    public void compareScreenshots(UrlSteps url, AtlasWebElement element) {
        Screenshot testingScreenshot = getElementScreenshot(element.waitUntil(isDisplayed()));
        url.setProductionHost().open();
        Screenshot productionScreenshot = getElementScreenshot(element.waitUntil(isDisplayed()));
        screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}

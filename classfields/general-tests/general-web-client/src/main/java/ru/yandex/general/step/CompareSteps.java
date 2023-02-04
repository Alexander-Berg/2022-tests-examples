package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.comparison.PointsMarkupPolicy;
import ru.yandex.qatools.ashot.coordinates.Coords;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.cropper.indent.IndentCropper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.awt.Color.MAGENTA;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public class CompareSteps {

    @Inject
    private WebDriverManager wm;

    @Inject
    private GeneralWebConfig config;

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

    public void screenshotsShouldBeTheSame(Screenshot testing, Screenshot production) {
        Allure.getLifecycle().updateTestCase(testResult ->
                testResult.getLabels().add(new Label().setName("testType").setValue("screenshotDiff")));

        ImageDiff diff = new ImageDiffer().withDiffMarkupPolicy(new PointsMarkupPolicy().withDiffColor(MAGENTA))
                .makeDiff(production, testing);

        attachImage("diff", diff.getMarkedImage());
        attachImage("actual", testing.getImage());
        attachImage("expected", production.getImage());

        assertThat("разница в пикселях между скриншотом тестинга и продакшена", diff.getDiffSize(),
                lessThan(config.getDiffSize()));
    }

    @Step("Делаем скриншот «{element}»")
    public Screenshot takeScreenshot(WebElement element) {
        waitSomething(2, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), element);
    }

    public Screenshot getElementScreenshotIgnoreAreasWithBorders(WebElement elementToScreenshoot, VertisElement... elementsIgnore) {
        return getElementScreenshotIgnoreAreasWithScrollOffset(elementToScreenshoot, 0, elementsIgnore);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreAreas(WebElement elementToScreenshoot, VertisElement... elementsIgnore) {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        Set<Coords> ignoredAreas = new HashSet<>();

        for (VertisElement element : elementsIgnore) {
            try {
                ignoredAreas.add(new Coords(element.getLocation().getX(),
                        element.getLocation().getY(),
                        element.getSize().getWidth(),
                        element.getSize().getHeight()));
            } catch (NoSuchElementException e) {
            }
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredAreas(ignoredAreas)
                .takeScreenshot(wm.getDriver(), elementToScreenshoot);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreAreasWithScrollOffset(WebElement elementToScreenshoot, int scrollOffset, VertisElement... elementsIgnore) {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        Set<Coords> ignoredAreas = new HashSet<>();

        for (VertisElement element : elementsIgnore) {
            try {
                ignoredAreas.add(new Coords(element.getLocation().getX(),
                        element.getLocation().getY() - scrollOffset,
                        element.getSize().getWidth() + 5,
                        element.getSize().getHeight() + 35));
            } catch (NoSuchElementException e) {
            }
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredAreas(ignoredAreas)
                .takeScreenshot(wm.getDriver(), elementToScreenshoot);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreElements(WebElement elementToScreenshoot, Set<String> selectors) {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredElements(selectors.stream()
                .map(selector -> By.xpath(selector)).collect(toSet()))
                .takeScreenshot(wm.getDriver(), elementToScreenshoot);
    }

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        wm.getDriver().manage().window().setSize(new Dimension(x, y));
    }

}

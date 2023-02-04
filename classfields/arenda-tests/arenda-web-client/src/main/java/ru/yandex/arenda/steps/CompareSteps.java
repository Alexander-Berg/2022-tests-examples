package ru.yandex.arenda.steps;

import com.google.inject.Inject;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.arenda.config.ArendaWebConfig;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.coordinates.Coords;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.cropper.indent.IndentCropper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public class CompareSteps {

    @Inject
    private WebDriverManager wm;

    @Inject
    private ArendaWebConfig config;

    @Step("Ресайз «{x} x {y}»")
    public void resize(int x, int y) {
        wm.getDriver().manage().window().setSize(new Dimension(x, y));
    }

    @Step("Ресайз для десктопа")
    public void resizeDesktop() {
        resize(1920, 3000);
    }

    @Attachment(value = "{0}", type = "image/png")
    public byte[] attachImage(String name, BufferedImage image) {
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
        Allure.getLifecycle().updateTestCase(testResult ->
                testResult.getLabels().add(new Label().setName("testType").setValue("screenshotDiff")));

        ImageDiff diff = new ImageDiffer()
                .makeDiff(testing, production);

        attachImage("diff", diff.getMarkedImage());
        attachImage("actual", testing.getImage());
        attachImage("expected", production.getImage());

        assertThat("разница в пикселях между скриншотом тестинга и продакшена", diff.getDiffSize(),
                lessThan(config.getDiffSize()));
    }

    public Screenshot getScreenshotFromFile(InputStream img) {
        try {
            return new Screenshot(ImageIO.read(img));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Делаем скриншот {element}")
    public Screenshot takeScreenshot(WebElement element) {
        waitSomething(3, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), element);
    }

    @Step("Делаем скриншот {element}, игнорим {ignore}")
    public Screenshot takeScreenshotWithIgnore(WebElement element, WebElement ignore) {
        waitSomething(3, TimeUnit.SECONDS);
        return new AShot().imageCropper(new IndentCropper(-1))
                .addIgnoredArea(new WebDriverCoordsProvider().ofElement(wm.getDriver(), ignore))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(wm.getDriver(), element);
    }

    @Step("Делаем скриншот {element}, игнорим {coords}")
    public Screenshot takeScreenshotWithIgnore(WebElement element, Coords... coords) {
        waitSomething(3, TimeUnit.SECONDS);
        AShot aShot = new AShot().imageCropper(new IndentCropper(-1))
                .coordsProvider(new WebDriverCoordsProvider());
        for (Coords coord : coords) {
            aShot.addIgnoredArea(coord);
        }
        return aShot.takeScreenshot(wm.getDriver(), element);
    }

    @Step("Игнорируем координаты для {element}")
    public Coords getCoordsFor(WebElement element) {
        return new Coords(
                element.getLocation().getX() - 5,
                element.getLocation().getY() - 5,
                element.getSize().getWidth() + 20,
                element.getSize().getHeight() + 50);
    }
}

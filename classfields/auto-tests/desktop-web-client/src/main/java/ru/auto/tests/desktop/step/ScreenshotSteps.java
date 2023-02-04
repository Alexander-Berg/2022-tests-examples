package ru.auto.tests.desktop.step;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.comparison.ImageDiff;
import pazone.ashot.comparison.ImageDiffer;
import pazone.ashot.comparison.ImageMarkupPolicy;
import pazone.ashot.coordinates.Coords;
import pazone.ashot.coordinates.WebDriverCoordsProvider;
import pazone.ashot.cropper.indent.IndentCropper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_MAX_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_MAX_PAGE_MOBILE;
import static ru.auto.tests.desktop.utils.Utils.getResourceAsImage;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class ScreenshotSteps extends WebDriverSteps {

    @Setter
    public int acceptableDiff = 5;

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
                testResult.getLabels().add(new Label().withName("testType").withValue("screenshotDiff")));

        ImageDiff diff = new ImageDiffer()
                .withDiffMarkupPolicy(new ImageMarkupPolicy().withDiffColor(Color.GREEN))
                .makeDiff(testing, production);

        attachImage("diff", diff.getMarkedImage());
        attachImage("actual", testing.getImage());
        attachImage("expected", production.getImage());

        assertThat("разница в пикселях между скриншотом тестинга и продакшена", diff.getDiffSize(),
                lessThan(acceptableDiff));
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementsScreenshotWithoutResize(WebElement... element) {
        return new AShot().coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(getDriver(), asList(element));
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotWithoutResize(WebElement element) {
        return new AShot().coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(getDriver(), element);
    }

    @Step("Делаем скриншот элемента с обрезкой")
    public Screenshot getElementScreenshotWithCutting(WebElement... element) {
        return new AShot().coordsProvider(new WebDriverCoordsProvider()).imageCropper(new IndentCropper(-1))
                .takeScreenshot(getDriver(), asList(element));
    }

    @Step("Делаем скриншот элемента с обрезкой")
    public Screenshot getElementScreenshotWithCutting(int crop, WebElement... element) {
        return new AShot().coordsProvider(new WebDriverCoordsProvider()).imageCropper(new IndentCropper(-crop))
                .takeScreenshot(getDriver(), asList(element));
    }

    @Step("Делаем скриншот элемента с таймаутом")
    public Screenshot getElementScreenshotWithWaiting(VertisElement... elements) {
        for (VertisElement element : elements) {
            element.waitUntil(isDisplayed());
        }

        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return getElementScreenshotWithCutting(elements);
    }

    @Step("Делаем скриншот элемента с таймаутом")
    public Screenshot getElementScreenshotWithWaiting(int crop, VertisElement... elements) {
        for (VertisElement element : elements) {
            element.waitUntil(isDisplayed());
        }

        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return getElementScreenshotWithCutting(crop, elements);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotWithCuttingIgnoreAreas(WebElement elementToScreenshoot,
                                                                 Set<VertisElement> areas) {
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).imageCropper(new IndentCropper(-1))
                .ignoredAreas(areas.stream().map(element ->
                        new Coords(element.getLocation().getX(),
                                element.getLocation().getY(),
                                element.getSize().getWidth(),
                                element.getSize().getHeight())).collect(toSet()))
                .takeScreenshot(getDriver(), elementToScreenshoot);
    }

    public Screenshot getElementScreenshot(WebElement element) {
        setWindowSizeForScreenshot();
        /* Ресайз делается из-за бага в Ashot: если до элемента надо прокручивать страницу, то делается скрин не элемента,
         а всей страницы. Потому если страница большая, на ней виден элемент без прокрутки, то бага нет */
        return getElementScreenshotWithoutResize(element);
    }

    public void setWindowSizeForScreenshot() {
        String maxHeight = ((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollHeight").toString();
        setWindowSize(WIDTH_MAX_PAGE, Integer.parseInt(maxHeight));
    }

    public void setWindowSizeForMobileScreenshot() {
        String maxHeight = ((JavascriptExecutor) getDriver())
                .executeScript("return document.documentElement.scrollHeight").toString();
        setWindowSize(WIDTH_MAX_PAGE_MOBILE, Integer.parseInt(maxHeight));
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreAreas(WebElement elementToScreenshoot, Set<VertisElement> areas) {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredAreas(areas.stream().map(element ->
                        new Coords(element.getLocation().getX(),
                                element.getLocation().getY(),
                                element.getSize().getWidth(),
                                element.getSize().getHeight())).collect(toSet()))
                .takeScreenshot(getDriver(), elementToScreenshoot);
    }

    @Step("Делаем скриншот элемента")
    public Screenshot getElementScreenshotIgnoreElements(WebElement elementToScreenshoot, Set<String> selectors) {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return new AShot().coordsProvider(new WebDriverCoordsProvider()).ignoredElements(selectors.stream()
                        .map(By::xpath).collect(toSet()))
                .takeScreenshot(getDriver(), elementToScreenshoot);
    }

    private enum AttachTitle {
        IMAGE_IGNORED,
        IMAGE_DIFF_MARKED,
        IMAGE_DIFF_FRAGMENT,
        IMAGE_EXPECTED,
        IMAGE_ACTUAL
    }

    @Step("Получаем скриншот по xpath: {xpath}")
    public Screenshot getScreenshotByXpath(String xpath) {
        return new AShot().takeScreenshot(getDriver(), getDriver().findElement(By.xpath(xpath)));
    }

    @Step("Проверяем разницу скриншотов")
    public void checkDiff(Screenshot actual, Screenshot expected, List<String> ignoreList, int maxDiff)
            throws IOException {
        ImageDiffer imageDiffer = new ImageDiffer();

        ImageDiff diff = imageDiffer.makeDiff(actual, expected);

        if (diff.getDiffSize() > maxDiff) {

            boolean ignored = false;
            for (String ignoreImgPath : ignoreList) {
                ImageDiff ignoreDiff = imageDiffer.makeDiff(diff.getTransparentMarkedImage(),
                        getResourceAsImage(ignoreImgPath));

                if (ignoreDiff.getDiffSize() == 0) {
                    ignored = true;
                    break;
                }
            }

            if (ignored) {
                attachImage(AttachTitle.IMAGE_IGNORED.toString(), diff.getMarkedImage());
                return;
            }

            attachImage(AttachTitle.IMAGE_DIFF_MARKED.toString(), diff.getMarkedImage());
            attachImage(AttachTitle.IMAGE_DIFF_FRAGMENT.toString(), diff.getTransparentMarkedImage());
            attachImage(AttachTitle.IMAGE_EXPECTED.toString(), expected.getImage());
            attachImage(AttachTitle.IMAGE_ACTUAL.toString(), actual.getImage());

            assertThat("Разница в пикселях", diff.getDiffSize(), lessThanOrEqualTo(maxDiff));
        }
    }

    @Deprecated
    public void writeImage(String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(format("%s_screenshot_%s.png", name,
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))));
    }
}

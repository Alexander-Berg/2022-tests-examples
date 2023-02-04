package ru.auto.tests.commons.webdriver;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.openqa.selenium.chrome.ChromeOptions.CAPABILITY;
import static org.openqa.selenium.remote.CapabilityType.ACCEPT_INSECURE_CERTS;
import static org.openqa.selenium.remote.CapabilityType.ACCEPT_SSL_CERTS;
import static org.openqa.selenium.remote.CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION;
import static org.openqa.selenium.remote.CapabilityType.PAGE_LOAD_STRATEGY;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 *         Default implementation for WebDriverManager.
 */
@Log4j
public class DefaultWebDriverManager implements WebDriverManager {

    private static final String GRID_USERNAME = "vertistest";
    private static final String GRID_PASSWORD = "ebeb5544e20cc41da95de1a9096068da";

    @Inject
    private WebDriverConfig config;

    private final DesiredCapabilities capabilities = new DesiredCapabilities();
    private final ChromeOptions chromeOptions = new ChromeOptions();

    private WebDriver driver;
    private String sessionId;

    @Override
    public void startDriver() {
        initCapabilities();
        this.driver = initDriver();
    }

    @Override
    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    @Attachment(value = "{attachName}", type = "text/html")
    @ru.yandex.qatools.allure.annotations.Attachment(value = "{0}", type = "text/html")
    private String logHtml(String attachName, String html) {
        return html;
    }

    @Attachment(value = "{description}", type = "image/png")
    @ru.yandex.qatools.allure.annotations.Attachment(value = "{0}")
    private byte[] saveScreenShot(String description) {
        return (byte[]) getScreenShotFile(OutputType.BYTES);
    }

    private Object getScreenShotFile(OutputType type) {
        return ((TakesScreenshot) getDriver()).getScreenshotAs(type);
    }

    @Override
    @Step("Тушим веб драйвер")
    @ru.yandex.qatools.allure.annotations.Step("Тушим веб драйвер")
    public void stopDriver() {
        if (driver != null) {
            try {
                String url = driver.getCurrentUrl();
                logHtml("Ссылка", format("<a target=\"_blank\" href=\"%s\">%s</a>", url, url));
                logHtml("Активные окна браузера", format("<pre>%s</pre>", driver.getWindowHandles().toString()));
                logHtml("HTML код страницы", driver.getPageSource());
                logHtml("Куки", format("<pre>%s</pre>", driver.manage().getCookies().toString()));
                logHtml("Session id", sessionId);
                driver.manage().window().setSize(new Dimension(1920, 8000));
                saveScreenShot("Снимок браузера");
                driver.close();
                driver.quit();
                if (config.videoEnabled()) {
                    attachTestVideo("Видео выполнения теста", sessionId);
                }
            } catch (Exception e) {
                logHtml(e.getClass().getName(),
                        format("<pre>\nMessage:\n%s\nStackTrace:\n%s\n</pre>",
                                e.getMessage(),
                                e.fillInStackTrace()
                        )
                );
            }
        }
    }

    @Override
    public WebDriver getDriver() {
        if (driver == null) {
            throw new IllegalStateException("WebDriver не был проинициализирован");
        }
        return driver;
    }

    @Override
    public void updateCapabilities(Consumer<DesiredCapabilities> consumer) {
        consumer.accept(getCapabilities());
    }

    @Override
    public void updateChromeOptions(Consumer<ChromeOptions> consumer) {
        consumer.accept(getChromeOptions());
    }

    @Attachment(value = "{attachName}", type = "text/html",fileExtension = ".html")
    private String attachTestVideo(String attachName, String sessionId) {
        String host;
        if (Objects.nonNull(config.getRemoteUrl())) {
            host = String.format("http://%s:%s", config.getRemoteUrl().getHost(), config.getRemoteUrl().getPort());
        } else {
            host = format("https://%s:%s@selenium.yandex-team.ru",
                    config.getRemoteUsername(), config.getRemotePassword());
        }
        String videoUrl = format("%s/video/%s", host, sessionId);

        return "<video width=\"90%\" height=\"90%\" controls>\n" +
            "  <source src=\"" + videoUrl + "\" type=\"video/mp4\">\n" +
            "</video>";
    }

    @Step("Стартуем браузер")
    @ru.yandex.qatools.allure.annotations.Step("Стартуем браузер")
    private WebDriver initDriver() {
        log.info("Стартует бразуер " + getCapabilities().getBrowserName());
        String username = config.getRemoteUsername();
        String password = config.getRemotePassword();

        WebDriver driver;

        if (Objects.nonNull(config.getRemoteUrl())) {
            log.info("Host: " + config.getRemoteUrl());
            driver = await().atMost(config.getBrowserStartTimeout(), TimeUnit.SECONDS).alias("Ждём старта браузера")
                    .until(() -> new RemoteWebDriver(config.getRemoteUrl(), getCapabilities()), notNullValue());
            ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
        } else {
            driver = config.isLocal()
                    ? new ChromeDriver(getCapabilities())
                    : new RemoteWebDriver(config.getRemoteUrl(), getCapabilities());
        }

        sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        log.info("Session id = " + sessionId);

        driver.manage().timeouts().setScriptTimeout(90, TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(90, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);

        return driver;
    }


    private void initCapabilities() {
        String browser = config.getBrowserName();
        String userAgent = config.getUserAgent();
        String emulation = config.getEmulation();
        capabilities.setBrowserName(browser);
        capabilities.setVersion(config.getBrowserVersion());
        capabilities.setPlatform(Platform.ANY);
        capabilities.setCapability(ENSURING_CLEAN_SESSION, true);
        capabilities.setCapability(PAGE_LOAD_STRATEGY, "normal");
        capabilities.setCapability("enableVideo", config.videoEnabled());
        capabilities.setCapability("enableLog", config.logsEnabled());

        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.ALL);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        switch (browser) {
            case "firefox":
                if (userAgent != null) {
                    FirefoxProfile profile = new FirefoxProfile();
                    profile.setPreference("general.useragent.override", userAgent);
                    capabilities.setCapability(FirefoxDriver.Capability.PROFILE, profile);
                }
                break;
            case "chrome":
                if (userAgent != null) {
                    chromeOptions.addArguments("user-agent=" + userAgent);
                }
                if (config.isLocal()) {
                    chromeOptions.setCapability(ACCEPT_SSL_CERTS, true);
                    chromeOptions.setCapability(ACCEPT_INSECURE_CERTS, true);
                    chromeOptions.addArguments("--allow-insecure-localhost", "--ignore-certificate-errors");
                }
                if (emulation != null) {
                    Map<String, String> mobileEmulation = new HashMap<>();
                    mobileEmulation.put("deviceName", emulation);
                    chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
                }
                chromeOptions.setCapability(ACCEPT_SSL_CERTS, true);
                chromeOptions.setCapability(ACCEPT_INSECURE_CERTS, true);
//                chromeOptions.addArguments("disable-gpu");
//                chromeOptions.addArguments("--window-size=1920,1080");
//                chromeOptions.addArguments("--allow-insecure-localhost");
                capabilities.setCapability(CAPABILITY, chromeOptions);
                break;
            default:
                throw new IllegalStateException("Браузер " + browser + " не поддерживается");

        }
    }

    protected DesiredCapabilities getCapabilities() {
        return capabilities;
    }

    protected ChromeOptions getChromeOptions() {
        return chromeOptions;
    }

}

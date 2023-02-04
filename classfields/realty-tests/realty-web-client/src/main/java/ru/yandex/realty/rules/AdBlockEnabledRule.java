package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.junit.rules.ExternalResource;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import java.io.File;

/**
 * @author kantemirov
 */
public class AdBlockEnabledRule extends ExternalResource {

    private static final String ADBLOCK_PLUS_EXTENSION_CHROME = "adblock.crx";

    @Inject
    private WebDriverManager driverManager;

    @Override
    protected void before() {
        loadAdblockExtension(ADBLOCK_PLUS_EXTENSION_CHROME);
    }

    @Step("Устанавливаем расширение {extension}")
    private void loadAdblockExtension(String extension) {
        ClassLoader classLoader = getClass().getClassLoader();
        driverManager.updateChromeOptions(o -> o.addExtensions(new File(classLoader.getResource(extension).getFile())));
    }
}

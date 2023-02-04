package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.junit.rules.ExternalResource;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.realty.config.RealtyWebConfig;

import java.io.File;

/**
 * Created by vicdev on 03.04.18.
 * VERTISTEST-735
 */
public class AdBlockRule extends ExternalResource {

    private static final String ADBLOCK_PLUS_EXTENSION_CHROME = "adblockpluschrome-3.0.2.1997.crx";

    @Inject
    private WebDriverManager driverManager;

    @Inject
    private RealtyWebConfig config;

    @Override
    protected void before() {
        if (config.getExtensionAdBlockEnable()) {
            loadAdblockExtension(ADBLOCK_PLUS_EXTENSION_CHROME);
        }
    }

    @Step("Устанавливаем расширение {extension}")
    private void loadAdblockExtension(String extension) {
        ClassLoader classLoader = getClass().getClassLoader();
        driverManager.updateChromeOptions(o -> o.addExtensions(new File(classLoader.getResource(extension).getFile())));
    }
}

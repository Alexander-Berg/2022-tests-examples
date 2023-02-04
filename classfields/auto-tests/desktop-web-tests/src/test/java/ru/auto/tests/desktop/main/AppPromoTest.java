package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - промо приложения")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AppPromoTest {

    private static final String APP_URL = "https://app.adjust.com/";
    private static final String APP_GALLERY_URL = "https://appgallery8.huawei.com/#/app/C101134405";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение промо")
    public void shouldSeePromo() {
        basePageSteps.onMainPage().appPromo().should(hasText("Авто.ру —\nприложение для покупки и продажи автомобилей\n" +
                "более сотни тысяч объявлений\nо продаже автомобилей\nAppStore\nGoogle Play\nAppGallery"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Google Play")
    public void shouldClickGooglePlayUrl() {
        basePageSteps.onMainPage().appPromo().googlePlayButton().should(isDisplayed())
                .should(hasAttribute("href", startsWith(APP_URL))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на AppStore")
    public void shouldClickAppStoreUrl() {
        basePageSteps.onMainPage().appPromo().appStoreButton().should(isDisplayed())
                .should(hasAttribute("href", startsWith(APP_URL))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на AppGallery")
    public void shouldClickAppGalleryUrl() {
        basePageSteps.onMainPage().appPromo().appGalleryButton().should(isDisplayed())
                .should(hasAttribute("href", startsWith(APP_GALLERY_URL))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на QR-код")
    public void shouldClickQrCodeUrl() {
        basePageSteps.onMainPage().appPromo().qrCodeButton().click();
        basePageSteps.onMainPage().qrCodePopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().qrCodePopup().img().waitUntil(isDisplayed());
    }
}
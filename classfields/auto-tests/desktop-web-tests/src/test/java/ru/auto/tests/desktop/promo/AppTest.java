package ru.auto.tests.desktop.promo;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.APP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - Приложения")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AppTest {

    private static final String GOOGLE_PLAY_URL = "https://app.adjust.com/eb04l75?campaign=app_page&adgroup=gpbutton";
    private static final String APPSTORE_URL = "https://app.adjust.com/m1nelw7?campaign=app_page&adgroup=asbutton";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(APP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onAppPromoPage().content().should(hasText("Авто.ру —\nприложение для покупки и продажи " +
                "автомобилей\nболее сотни тысяч объявлений\nо продаже автомобилей\nAppStore\nGoogle Play\nAppGallery\n" +
                "Ищите машины по самым разным параметрам\nНемец или японец, хетчбек или внедорожник, красный или белый\n" +
                "Смотрите подробную информацию об автомобиле\nФотографии, характеристики и описание комплектации\n" +
                "Бесплатный отчёт по VIN с полной проверкой\nПроверенные автомобили отмечены зеленым значком\n" +
                "Скачайте приложение Авто.ру\nAppStore\nGoogle Play\nAppGallery"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Google Play")
    public void shouldClickGooglePlayUrl() {
        basePageSteps.onAppPromoPage().googlePlayUrl().should(isDisplayed())
                .should(hasAttribute("href", GOOGLE_PLAY_URL));
        basePageSteps.onAppPromoPage().googlePlayUrl().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на AppStore")
    public void shouldClickAppStoreUrl() {
        basePageSteps.onAppPromoPage().appStoreUrl().should(isDisplayed())
                .should(hasAttribute("href", startsWith(APPSTORE_URL)));
        basePageSteps.onAppPromoPage().appStoreUrl().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
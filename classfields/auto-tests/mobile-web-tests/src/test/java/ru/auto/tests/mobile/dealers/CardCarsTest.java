package ru.auto.tests.mobile.dealers;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Карточка дилера - инфо")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/Salon",
                "desktop/SalonPhones",
                "mobile/SearchCarsAllDealerId").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onDealerCardPage().info().should(hasText("1 / 15\nВсе дилерыСеть АВИЛОНАвилон Mercedes-Benz " +
                "Воздвиженка\nАвилон Mercedes-Benz Воздвиженка\nАвтомобили в наличии\nОфициальный дилер Mercedes-Benz\n" +
                "ПРОВЕРЕННЫЙ ДИЛЕР\nежедн. 9:00-21:40\nм. Арбатская (Филевская), Арбатская (Арбатско-Покровская)\n" +
                "Москва, ул. Воздвиженка, д. 12"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Телефоны")
    public void shouldSeePhones() {
        basePageSteps.onDealerCardPage().showPhoneButton().should(isDisplayed()).click();
        basePageSteps.onDealerCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 495 266-44-41\nс 09:00 до 22:00\n+7 495 266-44-42\nс 10:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа официального дилера")
    public void shouldSeeOfficialDealerPopup() {
        basePageSteps.onDealerCardPage().info().officialDealerStatus().click();
        basePageSteps.onDealerCardPage().popup().waitUntil(isDisplayed()).should(hasText("Официальный дилер\nСтатус " +
                "«Официальный дилер» используется в значении, указанном в Условиях оказания услуг на сервисе Auto.ru, " +
                "размещенных по ссылке: https://yandex.ru/legal/autoru_cars_dogovor/"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в поп-апе официального дилера")
    public void shouldClickOfficialDealerPopupUrl() {
        basePageSteps.onDealerCardPage().info().officialDealerStatus().click();
        basePageSteps.onDealerCardPage().popup().button().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri("https://yandex.ru/legal/autoru_cars_dogovor/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Карта")
    public void shouldSeeMap() {
        basePageSteps.onDealerCardPage().info().yaMapsControls().waitUntil(isDisplayed());
    }
}

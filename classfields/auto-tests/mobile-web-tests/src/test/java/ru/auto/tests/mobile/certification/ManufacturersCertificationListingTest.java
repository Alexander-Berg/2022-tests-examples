package ru.auto.tests.mobile.certification;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CERTIFICATION;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сертификация - сертификация производителей в листинге")
@Feature(CERTIFICATION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ManufacturersCertificationListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsCertificateManufacturer").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("search_tag", "certificate_manufacturer").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап с описанием программы сертификации производителя")
    public void shouldSeeManufacturerCertificationPopup() {
        basePageSteps.onListingPage().getSale(0).manufacturerCertIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed())
                .should(hasText("Автомобиль сертифицирован\nLand Rover Approved\nПробег не более 150 000 км и не более " +
                        "7 лет с даты первой покупки\n«Защита от поломок» до 2 лет или 60 000 км пробега после покупки, " +
                        "либо заводская гарантия до 2 лет\n«Помощь на дорогах» на срок до 24 месяцев\nПроверены по 165 " +
                        "пунктам\nПолностью подготовлены исходя из результатов диагностики\nПроверенная сервисная " +
                        "история\nО программе\nLand Rover Approved — это программа продаж сертифицированных автомобилей " +
                        "Land Rover с пробегом, которые предлагаются клиентам на самых выгодных условиях. К участию " +
                        "в программе допускаются только тщательно проверенные автомобили с пробегом до 150 000 км " +
                        "и возрастом до 7 лет, доставленные на территорию РФ официальным импортером.\n\nКаждый " +
                        "автомобиль прошел внимательный отбор, обязательную техническую проверку по 165 пунктам и полную " +
                        "предпродажную подготовку с устранением всех выявленных недостатков квалифицированными " +
                        "механиками с использованием оригинальных запчастей. История каждого из них прозрачна. " +
                        "Их надежность доказана на деле!\nПодробнее о программе\nРеклама"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().getSale(0).manufacturerCertIcon().hover().click();
        basePageSteps.onListingPage().popup().button()
                .should(hasAttribute("href",
                        "http://www.landrover.ru/approved-used/index.html/?utm_source=manuf_cert")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}

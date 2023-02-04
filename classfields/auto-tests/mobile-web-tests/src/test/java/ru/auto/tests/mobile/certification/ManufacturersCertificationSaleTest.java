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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сертификация - сертификация производителей на карточке объявления")
@Feature(CERTIFICATION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ManufacturersCertificationSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/OfferCarsUsedDealer").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока сертификации производителя")
    public void shouldSeeCertificate() {
        basePageSteps.onCardPage().manufacturerCert().should(hasText("Пробег не более 120 000 км и не более 5 лет " +
                "с даты первой покупки\n«Защита от поломок» до 2 лет или 60 000 км пробега после покупки, " +
                "либо заводская гарантия до 2 лет\n«Помощь на дорогах» на срок до 24 месяцев\nПроверены по 165 пунктам\n" +
                "Полностью подготовлены исходя из результатов диагностики\nПроверенная сервисная история"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап с описанием программы сертификации производителя")
    public void shouldSeeManufacturerCertificationPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().manufacturerCertLogo());
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Автомобиль сертифицирован\nLand Rover Approved\nПробег не более 120 000 км и не более " +
                        "5 лет с даты первой покупки\n«Защита от поломок» до 2 лет или 60 000 км пробега после покупки, " +
                        "либо заводская гарантия до 2 лет\n«Помощь на дорогах» на срок до 24 месяцев\nПроверены по 165 " +
                        "пунктам\nПолностью подготовлены исходя из результатов диагностики\nПроверенная сервисная " +
                        "история\nО программе\nLand Rover Approved - это программа продаж сертифицированных автомобилей " +
                        "Land Rover с пробегом, которые предлагаются клиентам на самых выгодных условиях. К участию " +
                        "в программе допускаются только тщательно проверенные автомобили с пробегом до 120 000 км " +
                        "и возрастом до 5 лет, доставленные на территорию РФ официальным импортером.\n\nКаждый " +
                        "автомобиль прошел внимательный отбор, обязательную техническую проверку по 165 пунктам и полную " +
                        "предпродажную подготовку с устранением всех выявленных недостатков квалифицированными " +
                        "механиками с использованием оригинальных запчастей. История каждого из них прозрачна. " +
                        "Их надежность доказана на деле!\nПодробнее о программе\nРеклама"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка в поп-апе с описанием программы сертификации производителя")
    public void shouldClickManufacturerCertificationPopupUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().manufacturerCertLogo());
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().popup().button().waitUntil(isDisplayed())
                .should(hasAttribute("href",
                        "http://www.landrover.ru/approved-used/index.html/?utm_source=manuf_cert"));
        basePageSteps.onCardPage().popup().button().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}

package ru.auto.tests.mobile.vin;

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

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Купленный отчёт под владельцем")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VinSalePaidOwnerTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawPaid").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать бесплатный отчёт»")
    public void shouldClickShowFreeReportButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .buttonContains("Показать бесплатный отчёт"));
        basePageSteps.onCardPage().vinReport().waitUntil(hasText("Отчёт о проверке по VIN\nОбновлён 18 сентября 2019\n" +
                "Оценка\n87\nДанные из ПТС\nОбновлено 26 ноября 2019\nМарка и модель\nMitsubishi Lancer Evolution\n" +
                "ПТС\n*****\nVIN\nJA3AW86V28U049773\nГосномер\nО010АУ186\nГод выпуска\n2008 г.\nМощность двигателя\n" +
                "150 л.с.\nв объявлении 295 л.с.\nЦвет\nЧерный\nДанные ПТС проверены\n4 владельца по ПТС\n" +
                "Обновлено 26 ноября 2019\nФизическое лицо\n18 августа 2012 — 26 апреля 2014 (1 год 9 месяцев)\n" +
                "Физическое лицо\n26 апреля 2014 — 10 декабря 2016 (2 года 8 месяцев)\nФизическое лицо\n" +
                "10 декабря 2016 — 30 марта 2017 (4 месяца)\nФизическое лицо\n30 марта 2017 — 30 марта 2017 (1 день)\n" +
                "Количество владельцев совпадает с указанным в объявлении\nНаличие ограничений\n" +
                "Обновлено 11 октября 2020\nОграничения на регистрационные действия не обнаружены\nПо данным ГИБДД\n" +
                "Обнаружено 1 ДТП\nОбновлено 18 сентября 2019\nСтолкновение\n" +
                "16 ноября 2016, Ханты-Мансийский Автономный Округ\n1\nПереднее правое крыло\nЦарапина/Скол\n2\n" +
                "Передний бампер\nЦарапина/Скол\n2\n1\n87\nРейтинг похожих автомобилей 75-92 из 100\n" +
                "Оценка помогает сравнить состояние похожих автомобилей и основана на ключевых пунктах отчёта.\n" +
                "Скрыть\nПолный отчёт\nИсчерпывающая информация об автомобиле. Отчёт виден только тем, кто оплатил доступ\n" +
                "2 записи в истории пробегов\n3 записи в истории эксплуатации\nОценка ПроАвто\nЕщё 1 размещение на Авто.ру\n" +
                "Отзывные кампании не найдены\nРасчёт транспортного налога\nПосмотреть полный отчёт"
        ));
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Посмотреть полный отчёт»")
    public void shouldClickShowFullReportButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Посмотреть полный отчёт"));
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Скролл к отчёту")
    public void shouldScrollToReport() {
        urlSteps.addParam("action", "showVinReport").open();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к отчёту", basePageSteps.getPageYOffset() > 0);
    }
}

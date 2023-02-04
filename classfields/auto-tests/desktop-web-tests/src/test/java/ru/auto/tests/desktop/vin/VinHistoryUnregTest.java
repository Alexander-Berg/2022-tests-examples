package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Urls.FORMS_YANDEX_PARTNERSHIP;
import static ru.auto.tests.desktop.mobile.page.HistoryPage.QUESTION_BUTTON_TEXT;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Покупка отчёта под незарегом")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryUnregTest {

    private static final String VIN = "4S2CK58D924333406";
    private static final String LICENSE_PLATE = "Y151BB178";
    private static final String EXAMPLE_TEXT = "Пример отчёта\nHD\nСодержание отчёта\nДанные из ПТС\nНайдены характеристики\nНаличие ограничений\nЕсть записи\nНахождение в розыске\nЕсть записи\nДанные о залоге\nНет записей\nВладельцы по ПТС\n1\n3 владельца\nРабота в такси\n1 запись\n8 HD фотографий\nРабота в каршеринге\n1 запись\nДоговоры лизинга\nНет записей\nАукцион битых автомобилей\n1 запись\nУчастие в ДТП\n2 ДТП\nРасчёт стоимости ремонта\n1 запись\nСтраховые выплаты\n2 записи\nДанные о техосмотрах\n3 записи\nФотографии автолюбителей\n1 фотография\nОпции по VIN\nЕсть записи\nСертификация производителем\n1 запись\nПрограммы производителей\n1 запись\nОтзывные кампании\n1 запись\nТаможня\n1 запись\nСтраховые полисы\n2 записи\nОценка от партнёров\n1 запись\nШтрафы\nНет неоплаченных\nРазмещения на Авто.ру\n2 объявления\nФотографии из отзыва владельца\nИстория пробегов\n7 записей\nИстория эксплуатации\n22 записи\nОценка стоимости\n~ 430 627 ₽\nЦена в зависимости от возраста\nДешевеет на 5% в год\nТранспортный налог\n2 550 ₽ в год\nСреднее время продажи\n27 дней\nДанные из ПТС\nОбновлено 14 июля\nМарка и модель\nNissan Almera\nVIN\nZ0NZWE00054341234\nГод выпуска\n2015 г.\nОбъём двигателя\n1.6 л.\nМощность двигателя\n102 л.с.\nЦвет\nСерый\nДанные ПТС проверены\nНаличие ограничений\nОбновлено 14 июля\nАвтомобиль может иметь ограничения на регистрационные действия\nПо данным ГИБДД\nНахождение в розыске\nОбновлено 14 июля\nАвтомобиль может находиться в розыске\nПо данным ГИБДД\nНахождение в залоге\nОбновлено 15 июля\nСведения о нахождении в залоге не обнаружены\nПо данным Реестра уведомлений о залоге движимого имущества и Национального бюро кредитных историй\n3 владельца по ПТС\nОбновлено 14 июля\nЮридическое лицо\n26 декабря 2015 — 15 мая 2016 (5 месяцев)\nФизическое лицо\n15 мая 2016 — 10 марта 2017 (10 месяцев)\nБез регистрации\n10 марта 2017 — 10 марта 2017 (1 день)\nФизическое лицо\n10 марта 2017 — настоящее время (5 лет 5 месяцев)\nРабота в такси\n30 декабря 2015 — 31 октября 2020\nНомер разрешения\n0074997\nСтатус\nАктивно\nКомпания-перевозчик\nМОСТАКСИ-24\nТип номера\nОбычный\nГород регистрации\nМосква\nИспользовался в каршеринге\nОбновлено 15 июля\n26 декабря 2015 — 15 мая 2016\nООО МЭЙДЖОР ПРОФИ\nПочему о прошлом в каршеринге важно знать? Всё дело в износе — скорее всего, он будет гораздо больше, чем у автомобиля из частных рук.\nДоговоры лизинга\nАвтомобиль не обнаружен в договорах лизинга\nЛизинг — долгосрочная аренда. Автомобиль мог подвергаться сильному износу, стоит обратить внимание на его состояние\nПродавался на аукционах битых автомобилей\nОбновлено 15 июля\nАукцион Migtorg, SPB\n18 апреля 2019\nПроверили в базе крупнейшего онлайн-аукциона битых автомобилей с тотальным ущербом.\nОбнаружено 2 ДТП с 2015 года\nОбновлено 14 июля\nНаезд на стоящее ТС\n2 августа 2016, Московская Область\nСтолкновение\n10 ноября 2018, Москва\n1\nЗадняя правая дверь\nЦарапина/Скол\nлегкие повреждения задней правой двери\n2\nЗаднее правое крыло\nЦарапина/Скол\nлегкие повреждения заднего правого крыла или колеса\n3\nЗадний бампер\nЦарапина/Скол\nлегкие повреждения правой части заднего бампера\n4\nЗадний бампер\nЦарапина/Скол\nлегкие повреждения левой части заднего бампера\n3\n2\n1\n1 расчёт стоимости ремонта\nОбновлено 14 июля\n25 000 — 50 000 ₽\n10 августа 2016\nСтраховые компании оценивают стоимость ремонта при наступлении страхового случая. В каждой компании могут по-разному рассчитать стоимость. В отчёте будут предоставлены все случаи оценки. Это не означает, что автомобиль ремонтировали\nСтраховые выплаты\nОбновлено 21 апреля 2019\nСтраховая компания\nЛИБЕРТИ СТРАХОВАНИЕ\nТип страховки\nКАСКО\nМесяц выплаты\nНеизвестно\nСумма выплат\n7 970 ₽\nСтраховая компания\nЛИБЕРТИ СТРАХОВАНИЕ\nТип страховки\nКАСКО\nМесяц выплаты\nНеизвестно\nСумма выплат\n23 135 ₽\nПосле повреждений страховые компании компенсируют ущерб. Выплата не гарантирует, что машину ремонтировали\nСертификация производителем\nАвтомобиль прошёл серьёзную проверку производителя, получил сертификат и будет обслуживаться по специальным условиям.\nПрограммы производителей\nОбновлено 15 июля\nПродленная гарантия от Nissan\nExtendedWarranty\n27 декабря 2018 — 1 января 3000\nПрограммы дают особые преимущества при обслуживании автомобиля\nТехосмотры\nОбновлено 15 июля\n10 июля 2017\n8 июля 2019\n21 июня 2020\nОтзывные кампании\nОбновлено 15 июля\nПроблема газогенератора подушки безопасности\n26 декабря 2016\nИногда производители отзывают свои автомобили из-за некачественных деталей или сборки. Замена всегда происходит бесплатно и независимо от гарантии. В Росстандарте собрана полная база данных всех отзывных кампаний с 2014 года.\nФотографии автолюбителей\nОбновлено 15 июля\n30 июня 2021\nФотографии от автолюбителей по всему миру\nОпции по VIN\nОпции, установленные на этом автомобиле. Информация по VIN\nТаможня\nОбновлено 15 июля\nВвоз автомобиля\nДата\n1 июня 2013\nСтрана ввоза\nРоссия\nСтрана вывоза\nБеларусь\nИсточник\nФедеральная таможенная служба\nПо данным таможенных деклараций с 2017 года\nСтраховые полисы\nОбновлено 21 апреля 2019\n28 июля 2017 — 29 июля 2018\nТип страховки\nКАСКО\nСтраховая компания\nЛИБЕРТИ СТРАХОВАНИЕ\nПродавец\nUremont\n16 ноября 2021\nРегион\nМосковская обл\nТип страховки\nОСАГО\nСерия и номер\nХХХ 0204757525\nСтатус\nДействует\nСтраховая компания\nПАО \"Группа Ренессанс Страхование\"\nПо данным РСА и партнёров ПроАвто\nЭкспертная оценка\nОбновлено 15 июля\n20 октября 2019\nПробег\n210 123 км\nРегион\nМосква и МО\nИсточник\nCarPrice\nДанные по оценке автомобиля от партнёров Авто.ру\nШтрафы\nНет неоплаченных\nНевозможно проверить данные о штрафах, для автомобиля не найден номер СТС\nИщем штрафы по номеру СТС с 2015 года\n2 объявления на Авто.ру\nОбновлено 15 июля\nДата\n18 апреля 2019\nПробег\n112 000 км\nСостояние\nНе требует ремонта\nПродавец\nЧастное лицо\nHD\nСмотреть объявление\nДата\n13 июня 2019\nПробег\n110 000 км\nСостояние\nНе требует ремонта\nПродавец\nЧастное лицо\nHD\nСмотреть объявление\nФотографии из отзыва владельца\nОбновлено 15 июля\n5,0\nОтличная машина.\n20 апреля 2021\nОтличное авто! Хорошая динамика, удобный салон для длительных поездок с детьми, третий ряд не тесный, проекция на лобовое стекло, отличная опция, удобно пользоваться. Ну и напичкана по полной полезными функциями, подогрев и вентиляция сидений, можно настроить что бы включалось автоматически при разной температуре. Электро привод багажника, автоматические фары, вкруг светодиодные, включая птф и многое другое. Отлично ведёт себя на дороге. В грязь не приходилось заезжать, но думаю и там она не подвела бы.\nЧитать отзыв\nИстория пробегов\nОбновлено 15 июля\nПробег, тыс. км\n300\n200\n100\n210 123\n2015\n2017\n2018\n2019\n1\n2\n3\nИстория эксплуатации\nОбновлено 15 июля 2022\nВыпуск автомобиля\nГод выпуска\n2015\nВладелец 1\nЮридическое лицо, 26 декабря 2015 — 15 мая 2016 (5 месяцев)\nМосква\n26 декабря 2015 — 15 мая 2016\nИспользовался в каршеринге\nООО МЭЙДЖОР ПРОФИ\nПочему о прошлом в каршеринге важно знать? Всё дело в износе — скорее всего, он будет гораздо больше, чем у автомобиля из частных рук.\n30 декабря 2015 — 31 октября 2020\nИспользовался в такси\nНомер разрешения\n0074997\nСтатус\nАктивно\nКомпания-перевозчик\nМОСТАКСИ-24\nТип номера\nОбычный\nГород регистрации\nМосква\n23 января 2016\nВизит в автосервис\nАвтосервис\nNissan\nОписание\nУстановка доп. оборудования\n1 февраля 2016\nВизит в автосервис\nАвтосервис\nNissan\nОписание\nУстановка доп. оборудования\nВладелец 2\nФизическое лицо, 15 мая 2016 — 10 марта 2017 (10 месяцев)\nМосковская Область\n2 августа 2016, Московская Область\nНаезд на стоящее ТС\n10 августа 2016\nРасчёт стоимости ремонта\nПробег\n44 852 км\nОбщая стоимость\n25 000 — 50 000 ₽\nТотальный ущерб\nНет\nТип страховки\nОСАГО\nИсточник\nAudatex\nСмотреть расчёт\n26 декабря 2016\nОтзывная кампания: Проблема газогенератора подушки безопасности\nПодробнее\nВладелец 3\nФизическое лицо, 10 марта 2017 — настоящее время (5 лет 5 месяцев)\nМосква\n17 марта 2017\nВизит в автосервис\nПробег\n49 345 км\nАвтосервис\nСТО Фильтр\nВыполненные работы\nКомпьютерная диагностика (снятие кодов сканером)\nСнятие-установка защиты двигателя (2 сложность)\nКупленные детали\nМасло трансмиссионное синтетическое Ravenol CVT Fluid, разл 1 литр.\nЛампа Philips 12-10 Вт. T10.5x38 в виде предохранителя L=38 мм SV8.5 салонная\n10 июля 2017\nТехосмотр\nПробег\n66 000 км\nНомер диагностической карты\n053770441706759\nДействителен до\n10 июля 2019\n28 июля 2017,\nПолис КАСКО\nДаты\n28 июля 2017 — 29 июля 2018\nСтраховая компания\nЛИБЕРТИ СТРАХОВАНИЕ\nПродавец\nUremont\n10 декабря 2017\nВизит в автосервис\nПробег\n60 250 км\nАвтосервис\nЕвроАвто\nВыполненные работы\nДиагностика ходовой АКЦИЯ бесплатный осмотр при ремонте на подъемнике\nПроверка уровня масла в двигателе\nПроверка состояния шин давления шин и глубины протектора\nФильтр воздушный - Замена\nЕщё 14\nКупленные детали\nПрокладка пробки масляного поддона\n10 ноября 2018, Москва\nСтолкновение\nСхема повреждений\n18 апреля 2019\nРазмещение на Авто.ру\nПробег\n112 000 км\nПродавец\nЧастное лицо\nСмотреть объявление\n18 апреля 2019, SPB\nПродавался на аукционе битых автомобилей Migtorg\n13 июня 2019\nРазмещение на Авто.ру\nПробег\n110 000 км\nПродавец\nЧастное лицо\nСмотреть объявление\n8 июля 2019\nТехосмотр\nНомер диагностической карты\n053560031902346\nДействителен до\n9 июля 2020\n20 октября 2019\nОценка стоимости\nПробег\n210 123 км\nРегион\nМосква и МО\nИсточник\nCarPrice\n21 июня 2020\nТехосмотр\nНомер диагностической карты\n078120022002988\nДействителен до\n22 июня\n20 апреля 2021\nФотографии из отзыва владельца\nЧитать отзыв\n30 июня 2021\nФотографии автомобиля\n16 ноября 2021, Московская обл\nПолис ОСАГО\nДаты\n16 ноября 2021\nРегион\nМосковская обл\nСерия и номер\nХХХ 0204757525\nСтатус\nДействует\nСтраховая компания\nПАО \"Группа Ренессанс Страхование\"\nОценка стоимости\n~ 430 627 ₽\nСредняя цена\nот 300 000 ₽\n55 предложений\nот 549 999 ₽\n54 предложения\nот 799 998 ₽\n8 предложений\nПотеря стоимости\nОбновлено 15 июля\n-5% в год\nЭта модель теряет в среднем 5% в год\n685 тыс\n654 тыс\n-4%\n581 тыс\n-11%\n549 тыс\n-5%\n547 тыс\n523 тыс\n-4%\n4 года\n5 лет\n6 лет\n7 лет\n8 лет\n9 лет\nТранспортный налог\n2 550 ₽ в год\nПо данным на 2022 год в Москве.\nВремя продажи\n~ 27 дней\nС опциями продвижения";

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
        mockRule.newMock().with("desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кноке «?» в верхнем блоке")
    public void shouldClickQuestionButton() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().questionButton().click();
        basePageSteps.onHistoryPage().popup().waitUntil(hasText(QUESTION_BUTTON_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Пример отчёта» в верхнем блоке")
    public void shouldClickExampleReportUrlInTopBlock() {
        urlSteps.testing().path(HISTORY).open();

        mockRule.with("desktop/CarfaxReportRawExample").update();

        basePageSteps.onHistoryPage().topBlock().button("Пример отчёта").click();
        basePageSteps.onHistoryPage().vinReportExample().should(hasText(EXAMPLE_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кноке «Войти» в сайдбаре")
    public void shouldClickEnterButton() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.scrollDown(500);

        String returnUrl = urlSteps.getCurrentUrl();
        basePageSteps.onHistoryPage().sidebar().button("Войти").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s?action=scroll-to-reports", returnUrl))).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Пример отчёта» в сайдбаре")
    public void shouldClickExampleReportSidebarUrl() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.scrollDown(800);

        mockRule.with("desktop/CarfaxReportRawExample").update();

        basePageSteps.onHistoryPage().sidebar().button("Пример отчёта").click();
        basePageSteps.onHistoryPage().vinReportExample().should(hasText(EXAMPLE_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Сотрудничество» в сайдбаре")
    public void shouldClickCollaborationUrl() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.scrollDown(500);

        basePageSteps.onHistoryPage().sidebar().button("Сотрудничество").click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(FORMS_YANDEX_PARTNERSHIP).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «5 отчётов» в сайдбаре")
    public void shouldClick5ReportsButtonInSidebar() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.scrollDown(500);

        basePageSteps.onHistoryPage().sidebar().button("5 отчётов за 599\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «10 отчётов» в сайдбаре")
    public void shouldClick10ReportsButtonInSidebar() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.scrollDown(500);

        basePageSteps.onHistoryPage().sidebar().button("10 отчётов за 990\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Сотрудничество» в промо")
    public void shouldClickCollaborationUrlInPromo() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().vinReportPromo().button("Сотрудничество").click();
        urlSteps.fromUri(FORMS_YANDEX_PARTNERSHIP).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Пример отчёта» в промо")
    public void shouldClickExampleReportUrlInPromo() {
        urlSteps.testing().path(HISTORY).open();

        mockRule.with("desktop/CarfaxReportRawExample").update();

        basePageSteps.onHistoryPage().vinReportPromo().button("Пример отчёта").click();
        basePageSteps.onHistoryPage().vinReportExample().should(hasText(EXAMPLE_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Купить отчёт»")
    public void shouldClickBuyReportButton() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().purchase().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Купить пакет из 10 отчётов»")
    public void shouldClickBuyReportPackageButton() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().purchase().button("Купить пакет за 990\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск отчёта по VIN и покупка")
    public void shouldSearchByVinAndBuy() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.path(VIN).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().sidebar().input("Госномер или VIN")
                .waitUntil(hasAttribute("value", VIN));
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск отчёта по госномеру и покупка")
    public void shouldSearchByLicensePlateAndBuy() {
        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", LICENSE_PLATE);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие отчёта по VIN и покупка")
    public void shouldOpenByVinAndBuy() {
        urlSteps.testing().path(HISTORY).path(VIN).open();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие отчёта по госномеру и покупка")
    public void shouldOpenByLicensePlateAndBuy() {
        urlSteps.testing().path(HISTORY).path(LICENSE_PLATE).open();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().switchToAuthPopupFrame();
        basePageSteps.onHistoryPage().authPopupFrame().title().should(hasText("Вход на сайт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по VIN. Нет такого автомобиля")
    public void shouldOpenHistoryByVinNoSuchCar() {
        mockRule.with("desktop/CarfaxReportRawVinNotFound").update();

        urlSteps.testing().path(HISTORY).open();
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", "WP0ZZZ97ZEL221611");
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().error().waitUntil(hasText("Нет такого автомобиля\nНе нашли историю по VIN. " +
                "Убедитесь, что он написан без ошибок. Если опечаток нет – попробуйте найти историю по госномеру."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по госномеру. Нет такого автомобиля")
    public void shouldOpenHistoryByNumberNoSuchCar() {
        mockRule.with("desktop/CarfaxReportRawLicensePlateNotFound").update();

        urlSteps.testing().path(HISTORY).open();
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", "K000OC777");
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().error().waitUntil(hasText("Нет такого автомобиля\nНе нашли историю по госномеру. " +
                "Убедитесь, что он написан без ошибок. Если опечаток нет – попробуйте найти историю по VIN."));
    }
}

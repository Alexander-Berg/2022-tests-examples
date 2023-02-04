package ru.auto.tests.desktop.photoHD;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.GALLERY;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SALES)
@Feature(GALLERY)
@Story("HD фото")
@DisplayName("Объявление - галерея - HD фото")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class OfferBuyHdPhotoTests {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String BUY_REPORT_TEXT = "Оригиналы фотографий будут доступны после покупки отчёта\nОтчёт о проверке по VIN\nXTA**************\nХарактеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\nHD фотографии\nЕще 16 пунктов проверки\nКупить отчёт от 99 ₽\nПоказать бесплатный отчёт";
    private static final String FREE_REPORT_TEXT = "Оригиналы фотографий будут доступны после покупки отчёта\nОтчёт о проверке по VIN\nXTA**************\nДанные из ПТС\nОбновлено 11 октября 2020\nМарка и модель LADA (ВАЗ) 2112\nVIN XTA**************\nГод выпуска 2006 г.\nОбъём двигателя 1,6 л\nМощность двигателя 89 л.с.\nЦвет Светло-серебристый бежевый металлик\nДанные ПТС проверены\n3 владельца по ПТС\nОбновлено 11 октября 2020\nФизическое лицо\n25 апреля 2006 — 11 сентября 2010 (4 года 5 месяцев)\nФизическое лицо\n11 сентября 2010 — 18 ноября 2017 (7 лет 3 месяца)\nФизическое лицо\n18 ноября 2017 — 18 ноября 2017 (1 день)\nВ объявлении указан 1 владелец, в ПТС 3\nВаш комментарий\nКомментировать\nНаличие ограничений\nОбновлено 11 октября 2020\nИнформация о юридических ограничениях появится чуть позже\nПо данным ГИБДД\nНахождение в розыске\nОбновлено 11 октября 2020\nИнформация о розыске появится чуть позже\nПо данным ГИБДД\nПолный отчёт\nИсчерпывающая информация об автомобиле\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nПоиск данных о залоге\nПоиск оценок стоимости ремонта\nЕщё 1 размещение на Авто.ру\nПроверка на работу в такси\nHD фотографии\nПроверка на работу в каршеринге\nАукционы битых автомобилей\nПроверка отзывных кампаний\nРасчёт транспортного налога\nСертификация производителем\nРасчёт среднего времени продажи\nЦена в зависимости от возраста\nХарактеристики\nОтзывы и рейтинг\nКупить отчёт от 99 ₽\nСкрыть подробности";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/OfferCarsUsedUserHd"),
                stub("desktop/CarfaxOfferCarsRawNotPaid")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().gallery().click();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Покупка отчета при нажатии на просмотр оригинала фото")
    public void shouldBuyReportHdPhotoButton() {
        mockRule.setStubs(
                stub("desktop/BillingAutoruPaymentInitVinHD"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesSale"),
                stub("desktop/BillingAutoruPaymentProcessVinHistoryHd"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxOfferCarsRawPaidDecrementQuota")
        ).update();

        basePageSteps.onCardPage().fullScreenGallery().badgeOpenOrig().click();
        basePageSteps.onCardPage().fullScreenGallery().activePopup().waitUntil(isDisplayed())
                .should(hasText(BUY_REPORT_TEXT));
        basePageSteps.onCardPage().fullScreenGallery().activePopup().button("Купить отчёт от 99\u00A0₽")
                .waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();

        urlSteps.testing().path(HISTORY).path(SALE_ID).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр бесплатного отчета при нажатии на просмотр оригинала фото")
    public void shouldSeeBadgePreviewGallery() {
        basePageSteps.onCardPage().fullScreenGallery().badgeOpenOrig().click();
        basePageSteps.onCardPage().fullScreenGallery().activePopup().button("Показать бесплатный отчёт")
                .waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().activePopup().waitUntil(isDisplayed())
                .should(hasText(FREE_REPORT_TEXT));
        basePageSteps.onCardPage().fullScreenGallery().activePopup().button("Купить отчёт от 99\u00A0₽")
                .waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
    }

}

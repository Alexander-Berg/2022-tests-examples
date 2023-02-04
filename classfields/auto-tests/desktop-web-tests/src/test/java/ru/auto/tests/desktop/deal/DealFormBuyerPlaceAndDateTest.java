package ru.auto.tests.desktop.deal;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Дата и место сделки»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealFormBuyerPlaceAndDateTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";
    private final static String URL_TEMPLATE = "https://%s/download-deal-agreement/%s/";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop/GeoSuggest",
                "desktop-lk/SafeDealDealGetWithBuyerBankDetailsWithMoney",
                "desktop/SafeDealDealUpdatePlaceAndDate").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заполняем блок с датой и местом встречи под покупателем")
    public void shouldFillDocumentsBlockByBuyer() {
        String tomorrow = LocalDate.parse(LocalDate.now().toString()).plusDays(1)
                .format(DateTimeFormatter.ofPattern("dd.MM.uuuu"));

        basePageSteps.onDealPage().section("Дата и место сделки").input("Дата сделки", tomorrow);
        basePageSteps.onDealPage().section("Дата и место сделки").input("Город", "химки");
        basePageSteps.onDealPage().geoSuggest().getItem(0).click();

        mockRule.overwriteStub(3, "desktop-lk/SafeDealDealGetWithBuyerPlaceAndDate");

        basePageSteps.onDealPage().section("Дата и место сделки").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().section("Документы для\u00a0печати").button("Посмотреть")
                .should(hasAttribute("href", format(URL_TEMPLATE, urlSteps.getConfig().getBaseDomain(), DEAL_ID)));
        basePageSteps.onDealPage().section("Документы для\u00a0печати").should(hasText("Документы для печати\n" +
                "Проверьте все персональные данные, даты, сумму. Если ошибок нет, распечатайте 3 экземпляра " +
                "договора.\nРаспечатайте договор в 3-х экземплярах и возьмите на встречу с продавцом\nДоговор " +
                "купли-продажи\nПредзаполненный договор\nПосмотреть\nПодробнее о личной встрече"));
    }
}
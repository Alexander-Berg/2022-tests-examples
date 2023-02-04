package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Notifications.ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Notifications.VIN_REPORT_SUCCESSFULLY_DOWNLOADED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.RCARD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@Story("Купленные отчёты")
@DisplayName("Купленные отчёты")
@GuiceModules(MobileTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryBoughtReportsTest {

    private static final int PAGE_SIZE = 10;

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxBoughtReportsRaw",
                "desktop/CarfaxBoughtReportsRawPage2",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отчёта")
    public void shouldSeeReport() {
        basePageSteps.onHistoryPage().boughtReportsList().should(hasSize(PAGE_SIZE));
        basePageSteps.onHistoryPage().getBoughtReport(0).should(hasText("Отчёт может дополняться. " +
                "Опрошено 26 из 30 источников. Найдено 12 записей.\nBMW 5 серии\nWBAJC51020G468520\nОценка\n" +
                "Дата проверки\n16 апреля 2021\nГод выпуска\n2017 г.\nДвигатель\n2,0 л / 190 л.с.\n" +
                "Цвет\nЧерный\nСмотреть отчёт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка отчётов")
    public void shouldLoadMoreReport() {
        basePageSteps.onHistoryPage().getBoughtReport(9).hover();
        basePageSteps.scrollDown(300);
        basePageSteps.onHistoryPage().boughtReportsList().should(hasSize(PAGE_SIZE * 2));
        basePageSteps.onHistoryPage().getBoughtReport(10).should(hasText("Porsche Cayenne\nWP1ZZZ92ZBLA85365\n" +
                "Оценка\n81\nДата проверки\n16 апреля 2021\nГод выпуска\n2010 г.\nДвигатель\n4,8 л / 500 л.с.\n" +
                "Цвет\nКоричневый\nОтчёт\nОбъявление"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Скролл к отчётам")
    public void shouldScrollToReports() {
        urlSteps.addParam("action", "scroll-to-reports").open();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к отчётам", basePageSteps.getPageYOffset() > 0);
        basePageSteps.onHistoryPage().boughtReportsList().should(hasSize(PAGE_SIZE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отчёту")
    public void shouldClickReport() {
        basePageSteps.onHistoryPage().getBoughtReport(0).click();
        urlSteps.path("/WBAJC51020G468520/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Смотреть отчёт»")
    public void shouldClickShowReportButton() {
        basePageSteps.onHistoryPage().getBoughtReport(0).button("Смотреть отчёт").click();
        urlSteps.switchToNextTab();
        urlSteps.path("/WBAJC51020G468520/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Отчёт»")
    public void shouldClickReportButton() {
        basePageSteps.onHistoryPage().getBoughtReport(3).button("Отчёт").click();
        urlSteps.switchToNextTab();
        urlSteps.path("/WF0HXXWPDH8G50198/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Объявление»")
    public void shouldClickSaleButton() {
        basePageSteps.onHistoryPage().getBoughtReport(3).button("Объявление").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(RCARD).path("/1103089829-7ab7b236/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Скачать»")
    public void shouldClickDownloadButton() {
        basePageSteps.onHistoryPage().getBoughtReport(0).downloadButton().click();

        basePageSteps.onHistoryPage().notifier(VIN_REPORT_SUCCESSFULLY_DOWNLOADED).should(isDisplayed());
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем отчёт в избранное")
    public void shouldClickAddToFavoriteButton() {
        mockRule.overwriteStub(1, "desktop/CarfaxBoughtReportsRawIsFavoriteFalse");
        basePageSteps.refresh();

        mockRule.with("desktop/UserFavoritesCarsPost").update();
        basePageSteps.onHistoryPage().getBoughtReport(0).favoriteButton().click();

        basePageSteps.onHistoryPage().notifier(ADDED_TO_FAV).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем отчет из избранного")
    public void shouldClickDeleteFromFavoriteButton() {
        mockRule.overwriteStub(1, "desktop/CarfaxBoughtReportsRawIsFavoriteTrue");
        basePageSteps.refresh();

        mockRule.with("desktop/UserFavoritesCarsDelete").update();
        basePageSteps.onHistoryPage().getBoughtReport(0).favoriteButton().click();

        basePageSteps.onHistoryPage().notifier(DELETED_FROM_FAV).should(isDisplayed());
    }

}

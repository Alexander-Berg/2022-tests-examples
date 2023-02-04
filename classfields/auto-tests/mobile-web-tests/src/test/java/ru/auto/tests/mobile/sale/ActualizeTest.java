package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import java.sql.Timestamp;
import java.time.LocalDate;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Карточка объявления - актуализация")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActualizeTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserOffersCarsActualize");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим кнопку «Актуально» в течение первых трёх суток")
    public void shouldSeeActualizeButton() {
        openLessThanThreeDaysOldSale();

        basePageSteps.onCardPage().floatingActualizeButton().button("Актуально").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Объявление актуально\nПодтвердите актуальность своего объявления. " +
                        "Делать это можно каждый день — тогда ваше объявление при поиске всегда будет находиться " +
                        "выше неактуализированных и его увидят больше покупателей."));
        basePageSteps.onCardPage().floatingActualizeButton().waitUntil(hasText("Актуально"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим кнопку «Да, продаю» после трёх дней")
    public void shouldSeeYesButton() {
        openMoreThanThreeDaysOldSale();

        basePageSteps.onCardPage().floatingActualizeButton().button("Да, продаю").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Объявление актуально\nПодтвердите актуальность своего объявления. " +
                        "Делать это можно каждый день — тогда ваше объявление при поиске всегда будет находиться " +
                        "выше неактуализированных и его увидят больше покупателей."));
        basePageSteps.onCardPage().floatingActualizeButton().waitUntil(hasText("Актуально"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа актуальности")
    public void shouldSeeActualizePopup() {
        openMoreThanThreeDaysOldSale();

        basePageSteps.onCardPage().floatingActualizeButton().helpIcon().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Актуальность объявлений\nПодтвердите актуальность своего объявления. Делать это можно " +
                        "каждый день — тогда ваше объявление при поиске всегда будет находиться выше неактуализированных " +
                        "и его увидят больше покупателей."));
    }

    @Step("Открываем объявление младше трёх дней»")
    public void openLessThanThreeDaysOldSale() {
        long lessThanThreeDaysAgo = Timestamp.valueOf(LocalDate.parse(LocalDate.now().toString()).minusDays(2)
                .atStartOfDay()).getTime();
        mockRule.with(mockRule.setActualizeDate("desktop/OfferCarsUsedUserOwner", Long.toString(lessThanThreeDaysAgo)))
                .post();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Step("Открываем объявление старше трёх дней»")
    public void openMoreThanThreeDaysOldSale() {
        long moreThanThreeDaysAgo = Timestamp.valueOf(LocalDate.parse(LocalDate.now().toString()).minusDays(4).atStartOfDay())
                .getTime();
        mockRule.with(mockRule.setActualizeDate("desktop/OfferCarsUsedUserOwner", Long.toString(moreThanThreeDaysAgo)))
                .post();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }
}

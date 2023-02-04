package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.DesktopStatic;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDate;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Карточка объявления - актуальность объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActualizeTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String IS_ACTUAL = "Актуально";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserOffersCarsActualize");

        cookieSteps.setCookieForBaseDomain("calls-promote-closed", "true");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим зелёную кнопку «Актуально» в течение первых трёх суток")
    public void shouldSeeActualGreenButton() {
        openLessThanThreeDaysOldSale();

        basePageSteps.onCardPage().cardHeader().partiallyActualButton().should(hasText(IS_ACTUAL));
        basePageSteps.onCardPage().cardHeader().partiallyActualButton().click();
        basePageSteps.onCardPage().cardHeader().fullyActualButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим синюю кнопку «Да, продаю» после трёх дней")
    public void shouldSeeYesActualButton() {
        openMoreThanThreeDaysOldSale();

        basePageSteps.onCardPage().cardHeader().yesActualButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().fullyActualButton().waitUntil(isDisplayed()).waitUntil(hasText(IS_ACTUAL));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим поп-ап при наведении на кнопку «Актуально»")
    public void shouldSeePopup() {
        openLessThanThreeDaysOldSale();

        basePageSteps.onCardPage().cardHeader().partiallyActualButton().should(isDisplayed()).hover();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed())
                .should(hasText(DesktopStatic.CARD_DROPDOWN_DESC_IS_ACTUAL));
    }

    @Step("Открываем объявление младше трёх дней»")
    public void openLessThanThreeDaysOldSale() {
        long lessThanThreeDaysAgo = Timestamp.valueOf(LocalDate.parse(LocalDate.now().toString()).minusDays(2).atStartOfDay())
                .getTime();
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
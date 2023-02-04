package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;

@DisplayName("Гараж")
@Story("Отчет")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageCardBoughtVinReportTest {

    private static final String VIN_CARD_ID = "/1146321503/";
    private static final String VIN = "WVWZZZ16ZBM121912";

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
                "desktop/ReferenceCatalogCarsSuggestVolkswagenJetta",
                "desktop/GarageUserCardsVinPost",
                "desktop/GarageUserCardVinPaidReport",
                "desktop/CarfaxReportRawVinPaidWVWZZZ16ZBM121912").post();

        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Посмотреть полный отчёт»")
    public void shouldClickSeeFullReportButton() {
        basePageSteps.onGarageCardPage().vinReport().button("Посмотреть полный отчёт").click();
        urlSteps.testing().path(HISTORY).path(VIN).path("/").shouldNotSeeDiff();
    }
}
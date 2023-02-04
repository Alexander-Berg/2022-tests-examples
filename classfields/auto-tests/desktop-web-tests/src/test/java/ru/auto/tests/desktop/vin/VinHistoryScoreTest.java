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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Блок «Полезная история?» на странице истории авто")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryScoreTest {

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
        mockRule.newMock().with("desktop/SessionAuthUser").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Выставление оценки отчёту по VIN")
    public void shouldSetScore() {
        mockRule.with("desktop/CarfaxReportRawVinPaid",
                "desktop/CarfaxScore").update();

        urlSteps.testing().path(HISTORY).path("/4S2CK58D924333406/").open();
        basePageSteps.onHistoryPage().vinReport().score().button("Нет").click();
        basePageSteps.onHistoryPage().vinReport().score().input("Расскажите почему", "1234567890");
        basePageSteps.onHistoryPage().button("Отправить").click();
        basePageSteps.onHistoryPage().vinReport().score().should(hasText("Спасибо, что нашли минутку!"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Не должно быть блока оценки, если пользователь уже оценивал отчёт")
    public void shouldNotSeeScore() {
        mockRule.with("desktop/CarfaxReportRawAlreadyScored").update();

        urlSteps.testing().path(HISTORY).path("/5J2CK58D924333410/").open();
        basePageSteps.onHistoryPage().vinReport().score().should(not(isDisplayed()));
    }
}
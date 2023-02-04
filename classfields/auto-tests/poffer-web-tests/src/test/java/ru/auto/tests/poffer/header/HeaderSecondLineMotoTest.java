package ru.auto.tests.poffer.header;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Шапка - мото")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderSecondLineMotoTest {

    private static final String TITLE = "Снегоходы";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("poffer/UserDraftCarsEmpty")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылкам в меню «Мото»")
    public void shouldClickMotoMenuUrls() {
        basePageSteps.onPofferPage().header().line2().button("Мото").hover();
        basePageSteps.onPofferPage().header().line2().dropdown().button(TITLE).click();

        urlSteps.testing().path(MOSKVA).path(SNOWMOBILE).path(ALL).shouldNotSeeDiff();
    }
}
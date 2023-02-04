package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Редактирование объявления. Мото и КомТС")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditMotoAndTrucksClickTest {

    private static final String OFFER_ID_PATH = "/1076842087-f1e84/";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salesMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOTO, "desktop-lk/UserOffersMotoActive"},
                {TRUCKS, "desktop-lk/UserOffersTrucksActive"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                salesMock).post();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(category).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в иконку редактирования оффера")
    public void shouldClickEditOfferIcon() {
        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().editIcon().should(isDisplayed()).click();

        basePageSteps.switchToNextTab();
        urlSteps.testing().path(category).path(EDIT).path(OFFER_ID_PATH).shouldNotSeeDiff();
    }
}

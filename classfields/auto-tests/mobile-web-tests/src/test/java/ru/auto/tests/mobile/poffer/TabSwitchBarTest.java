package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.ADD_OFFER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.page.PofferPage.KOMTRANS;
import static ru.auto.tests.desktop.mobile.page.PofferPage.MOTO;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Переключатель типов транспорта в мобильной форме размещения объявлений")
@Epic(BETA_POFFER)
@Feature(ADD_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TabSwitchBarTest {

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

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private PofferSteps pofferSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String tabName;

    @Parameterized.Parameter(2)
    public String mockDraft;

    @Parameterized.Parameter(3)
    public String switchTabName;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {Pages.TRUCKS, KOMTRANS, "mobile/poffer/UserDraftTrucks", MOTO},
                {Pages.MOTO, MOTO, "mobile/poffer/UserDraftMoto", KOMTRANS}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/ReferenceCatalogCarsSuggest"),
                stub("mobile/poffer/UserDraftCars"),
                stub(mockDraft)
        ).create();
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
        basePageSteps.onPofferPage().addOfferNavigateModal().closeIcon().click();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переключение на на тип транспорта, для которого есть драфт")
    public void shouldSeeTabSwitch() {
        basePageSteps.onPofferPage().radioButton(tabName).waitUntil(isDisplayed()).click();
        basePageSteps.onPofferPage().markBlock().should(not(isDisplayed()));
        basePageSteps.onPofferPage().banner(tabName.toLowerCase()).waitUntil(isDisplayed());
        urlSteps.desktopURI().path(category).path(USED).path(ADD).shouldNotSeeDiff();

        basePageSteps.onPofferPage().radioButton(switchTabName).should(isDisplayed());
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение переключателя типов транспорта после очистки поля с маркой")
    public void shouldSeeTabSWitchBarAfterClearForm() {
        basePageSteps.onPofferPage().radioButton(tabName).waitUntil(isDisplayed());
        pofferSteps.selectMark();
        basePageSteps.onPofferPage().radioButton(tabName).should(not(isDisplayed()));
        basePageSteps.onPofferPage().markBlock().clearInput("Марка");

        basePageSteps.onPofferPage().radioButton(tabName).should(isDisplayed());
    }
}

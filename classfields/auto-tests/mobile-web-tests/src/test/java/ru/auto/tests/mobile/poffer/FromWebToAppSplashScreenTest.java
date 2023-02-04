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
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.QueryParams.ACTION;
import static ru.auto.tests.desktop.mobile.element.FromWebToAppSplash.SPLASH_EDIT_OFFER_TEXT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сплэш-скрин «Отредактировать в приложении»")
@Epic(BETA_POFFER)
@Feature("Сплэш-скрин «Отредактировать в приложении»")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FromWebToAppSplashScreenTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String lkMock;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {"desktop/UserOffersMotoActive", MOTO},
                {"desktop/UserOffersTrucksActive", TRUCKS},
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub(lkMock)
        ).create();

        urlSteps.testing().path(MY).path(path).open();
        basePageSteps.onLkPage().getSale(0).button("Редактировать").click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сплэш-скрин, при нажатии на «Редактировать» с Комтранс/Мото оффера в ЛК")
    public void shouldSeeSplash() {
        basePageSteps.onLkPage().fromWebToAppSlash().waitUntil(isDisplayed()).should(hasText(SPLASH_EDIT_OFFER_TEXT));
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam(ACTION, QueryParams.EDIT).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем сплэш-скрин")
    public void shouldCloseSplash() {
        basePageSteps.onLkPage().fromWebToAppSlash().closeButton().waitUntil(isDisplayed()).click();

        basePageSteps.onLkPage().fromWebToAppSlash().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка в кнопке «Отредактировать в приложении» на сплэш-скрине")
    public void shouldSeeEditInAppLink() {
        basePageSteps.onLkPage().fromWebToAppSlash().button().waitUntil(isDisplayed()).should(hasAttribute(
                "href", allOf(
                        containsString(format("https://sb76.adj.st/my%s", path)),
                        containsString("adjust_campaign=touch_edit_ad_splash"),
                        containsString("adjust_adgroup=applogo_phone_white"),
                        containsString("adjust_creative=direct"),
                        containsString(format("adjust_deeplink=%s", encode(format("autoru://app/my%s", path))))
                )
        ));
    }

}

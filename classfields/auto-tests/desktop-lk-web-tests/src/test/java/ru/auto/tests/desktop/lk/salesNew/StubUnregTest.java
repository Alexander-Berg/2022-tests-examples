package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.QueryParams.R;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Заглушка под незарегом")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StubUnregTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL, "/beta/cars/used/"},
                {CARS, "/beta/cars/used/"},
                {MOTO, MOTO},
                {TRUCKS, TRUCKS}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        urlSteps.testing().path(MY).path(startUrl).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onLkSalesNewPage().stub().should(hasText("Продать — легко\n" +
                "Добавьте объявление, и его увидят тысячи потенциальных покупателей.\nВойдите, чтобы получить доступ к " +
                "своим объявлениям.\nРазместить бесплатно\nВойти"
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Добавить объявление»")
    public void shouldClickSellButton() {
        basePageSteps.onLkSalesNewPage().stub().button("Разместить бесплатно").click();

        urlSteps.testing().path(category).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Войти»")
    public void shouldClickAuthButton() {
        String currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onLkSalesNewPage().stub().button("Войти").click();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam(R, encode(currentUrl)).shouldNotSeeDiff();
    }

}

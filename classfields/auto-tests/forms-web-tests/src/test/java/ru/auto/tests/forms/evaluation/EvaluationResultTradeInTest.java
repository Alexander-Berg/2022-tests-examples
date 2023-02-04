package ru.auto.tests.forms.evaluation;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EVALUATION;
import static ru.auto.tests.desktop.consts.Regions.SPB_IP;
import static ru.auto.tests.desktop.consts.Regions.SPB_OBL_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Оценка авто - результат оценки, трейд-ин")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class EvaluationResultTradeInTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() throws IOException {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "forms/ReferenceCatalogCarsSuggest",
                "forms/TradeInIsAvailable",
                "forms/SearchPriceFrom360000",
                "forms/SearchPriceFrom460000",
                "forms/SearchCars",
                "forms/SearchCarsHistogram",
                "forms/StatsPredict",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        formsSteps.createEvaluationCarsForm();
        cookieSteps.setCookieForBaseDomain(GIDS, SPB_OBL_GEO_ID);
        urlSteps.testing().path(CARS).path(EVALUATION)
                .addXRealIP(SPB_IP).open();
        formsSteps.fillForm(formsSteps.getEvaluateReason().getBlock());
        formsSteps.submitForm();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Отправить заявку»")
    public void shouldClickTradeInButton() {
        formsSteps.onFormsEvaluationPage().evaluationResult().tradeInForm().should(hasText("Оставьте заявку в трейд-ин\n" +
                "Имя\nТелефон\nОтправить заявку\nЯ даю согласие ООО «Яндекс.Вертикали» на обработку данных в целях " +
                "оформления заявки и осуществления обратной связи по вопросам ее заполнения."));
        formsSteps.onFormsEvaluationPage().evaluationResult().tradeInForm().button("Отправить заявку").click();
        formsSteps.onFormsEvaluationPage().evaluationResult().tradeInForm()
                .waitUntil(hasText("Заявка на трейд-ин отправлена\nОжидайте звонка сегодня или завтра. " +
                        "Дилеры получат заявку и предложат варианты покупки. Выберите подходящий вам."));
    }
}
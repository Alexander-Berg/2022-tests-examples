package ru.auto.tests.desktop.callback;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CALLBACK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - поп-ап заказа обратного звонка")
@Feature(CALLBACK)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallbackDisplayTest {

    private static final String SALE_ID = "1076842087-f1e84";

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

    //@Parameter("Тип ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedDealer"},
                {CARS, "desktop/OfferCarsNewDealer"},
                {TRUCK, "desktop/OfferTrucksUsedDealer"},
                {MOTORCYCLE, "desktop/OfferMotoUsedDealer"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().contacts().callBackButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeCallbackPopup() {
        basePageSteps.onCardPage().callbackPopup().waitUntil(isDisplayed())
                .should(hasText("Обратный звонок\nНомер телефона\nПодтвердить номер\n" +
                        "Отправляя заявку, я соглашаюсь с условиями\nпользовательского соглашения\nЗакрыть"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение пользовательского соглашения")
    public void shouldSeeUserAgreement() {
        basePageSteps.onCardPage().callbackPopup().button("пользовательского\u00a0соглашения")
                .waitUntil(isDisplayed()).hover();
        basePageSteps.onCardPage().userAgreementPopup().waitUntil(isDisplayed()).should(hasText("Я даю свое согласие " +
                "на передачу в ООО «Яндекс.Вертикали» (ОГРН: 515746192742) и оператору связи ООО «ФАСТКОМ» " +
                "(ОГРН: 1117746465440), а также лицу, указанному в качестве продавца в объявлении о продаже " +
                "транспортного средства, моей персональной информации, указанной в настоящей форме, содержащей, " +
                "в том числе, мои персональные данные, и согласен с тем, что мои персональные данные будут " +
                "обрабатываться указанными лицами в соответствии " +
                "с Федеральным законом «О персональных данных» в целях связи со мной по вопросам купли-продажи " +
                "транспортного средства.\nРазговор может быть записан в целях оценки качества услуг."));
    }
}
package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Комментарий продавца на карточке объявления")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SellerCommentTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
    public String saleMock;

    @Parameterized.Parameter(2)
    public String comment;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "Комментарий продавца\nАвто в отличном состоянии" +
                        "Доводчики, камера 360Панорама, вентиляция\n<script>alert(5)</script> Автомобиль\nАвтомобиль\n" +
                        "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                        "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                        "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                        "Автомобиль\nАвтомобиль\nПоказать полностью"},

                {TRUCK, "desktop/OfferTrucksUsedUser", "Комментарий продавца\nКомментарий продавца\n<script>alert(5)</script> Грузовик\nГрузовик\n" +
                        "Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\n" +
                        "Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик" +
                        "\nГрузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                        "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                        "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                        "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик\nПоказать полностью"},

                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "Комментарий продавца\n<script>alert(5)</script> Мото\nМото\nМото\nМото\nМото\nМото\nМото\n" +
                        "Мото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото\nМото Мото Мото " +
                        "Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото " +
                        "Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото " +
                        "Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото " +
                        "Мото Мото Мото Мото Мото Мото Мото Мото Мото Мото\nПоказать полностью"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(KOPITSA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeComment() {
        basePageSteps.onCardPage().sellerComment().should(isDisplayed()).should(hasText(comment));
    }
}
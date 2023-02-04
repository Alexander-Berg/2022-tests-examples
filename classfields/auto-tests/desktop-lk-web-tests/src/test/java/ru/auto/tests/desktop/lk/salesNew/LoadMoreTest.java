package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Подгрузка объявлений")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LoadMoreTest {

    private static final String CAR_TEXT = "Добавьте панораму\nAston Martin DB11 AMR I, 2019\n777 км • Задний • Бензин • 5.2 AT (639 л.с.) • Купе • X1F4208ME60009602\n777 777 ₽\nДа, продаю\nСвернуть\n×20 просмотров\nАктивно ещё\n×5 просмотров\nЭкспресс-продажа • 6 дней";
    private static final String MOTO_TEXT = "ЗИЛ 4331, 1994\n47 558 км • Дизель • 8.7 MT • Фургон\n110 000 ₽\nДа, продаю\nСвернуть\n×20 просмотров\nТурбо-продажа • 3 дня\n";
    private static final String TRUCK_TEXT = "BMW K 1600 GTL, 2014\n36 000 км • Кардан • Инжектор • 1 600 (163 л.с.) • WB1060204EZ440802\n1 150 000 ₽\nИстёк срок размещения\nРазместить за 249 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salesMock1;

    @Parameterized.Parameter(2)
    public String salesMock2;

    @Parameterized.Parameter(3)
    public String userOfferCountPath;

    @Parameterized.Parameter(4)
    public String text;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop-lk/UserOffersCarsPage1", "desktop-lk/UserOffersCarsPage2",
                        USER_OFFERS_CARS_COUNT, CAR_TEXT},
                {TRUCKS, "desktop-lk/UserOffersTrucksPage1", "desktop-lk/UserOffersTrucksPage2",
                        USER_OFFERS_TRUCKS_COUNT, MOTO_TEXT},
                {MOTO, "desktop-lk/UserOffersMotoPage1", "desktop-lk/UserOffersMotoPage2",
                        USER_OFFERS_MOTO_COUNT, TRUCK_TEXT}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub(salesMock2),
                stub(salesMock1),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody())
        ).create();

        basePageSteps.setWideWindowSize(5000);
        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Подгрузка объявления")
    public void shouldSeeLoadedSale() {
        int saleListSize = 10;

        basePageSteps.onLkSalesNewPage().button("Показать ещё").click();

        basePageSteps.onLkSalesNewPage().salesList().waitUntil(hasSize(saleListSize + 1));
        basePageSteps.onLkSalesNewPage().getSale(saleListSize).should(hasText(startsWith(text)));
    }

}

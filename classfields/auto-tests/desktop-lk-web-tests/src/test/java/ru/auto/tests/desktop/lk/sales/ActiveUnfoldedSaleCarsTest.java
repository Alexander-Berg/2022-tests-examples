package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DKP;
import static ru.auto.tests.desktop.consts.Pages.DOCS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PRINT;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное развёрнутое объявление")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActiveUnfoldedSaleCarsTest {

    private final static String SALE_ID = "1076842087";
    private final static String SALE_ID_HASH = "1076842087-f1e84";

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
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                sessionAuthUserStub(),
                stub("desktop/User"),
                stub("desktop-lk/UserOffersCarsActive"),
                stub("desktop-lk/UserOffersCarsId"),
                stub("desktop-lk/UserOffersCarsStats")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст блока VAS")
    public void shouldSeeVasText() {
        String vasPattern = "×20\\nзвонков и просмотров\\n297 ₽\\n×5\\nзвонков и просмотров\\n197 ₽\\n" +
                "Опции на выбор\\nТурбо-продажа\\nВаше предложение увидит максимум посетителей — это увеличит шансы " +
                "на быструю и выгодную продажу\\. Объявление будет выделено цветом, поднято в топ, размещено в " +
                "специальном блоке на главной странице, на странице марки и в выдаче объявлений\\.\\n" +
                "Пакет опций действует 3 дня\\.\\nВключены: Выделение цветом, Спецпредложение, Поднятие в ТОП\\n" +
                "Скидка на опции\\n40%\\nБез скидки\\n495 ₽\\nСо скидкой\\n297 ₽\\nПредложение действует\\n" +
                "\\d{2}:\\d{2}:\\d{2}\\nПрименить пакет \\\"Турбо-продажа\\\" за 297 ₽";

        basePageSteps.onLkSalesPage().getSale(0).vas().should(hasText(matchesPattern(vasPattern)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст поп-апа с описанием услуги «Выделение цветом»")
    public void shouldSeeColorPopupText() {
        String colorPopup = "Выделение цветом\nОтличная возможность выделить своё предложение среди других — в " +
                "результатах поиска оно будет привлекать больше внимания.\nДействует 3 дня.\n2\n" +
                "Увеличивает количество просмотров в 2 раза\nПодключить за 67 ₽";

        basePageSteps.onLkSalesPage().getSale(0).colorIcon().hover();

        basePageSteps.onLkSalesPage().activePopup().should(hasText(colorPopup));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст поп-апа с описанием услуги «Поднятие в поиске»")
    public void shouldSeeFreshPopupText() {
        String freshPopup = "Поднятие в поиске\nСамый недорогой способ продвижения, который позволит вам в любой " +
                "момент оказаться наверху списка объявлений, отсортированного по актуальности или по дате. Это " +
                "поможет быстрее найти покупателя — ведь предложения в начале списка просматривают гораздо чаще.\n" +
                "Действует 1 день.\n3\nУвеличивает количество просмотров в 3 раза";

        basePageSteps.onLkSalesPage().getSale(0).freshButton().hover();

        basePageSteps.onLkSalesPage().activePopup().should(hasText(freshPopup));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку объявления»")
    public void shouldClickSaleTitle() {
        basePageSteps.onLkSalesPage().getSale(0).title().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/vaz/2121/").path(SALE_ID_HASH).path("/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkSalesPage().getSale(0).button("Редактировать").should(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(SALE_ID_HASH).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Договор»")
    public void shouldClickDocButton() {
        basePageSteps.onLkSalesPage().getSale(0).button("Договор купли-продажи").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(DOCS).path(DKP).addParam(QueryParams.SALE_ID, SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Лист продажи»")
    public void shouldClickPrintButton() {
        basePageSteps.onLkSalesPage().getSale(0).button("Лист продажи").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MY).path(CARS).path(PRINT).path(SALE_ID_HASH).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по столбику на графике")
    public void shouldClickChartItem() {
        basePageSteps.onLkSalesPage().getSale(0).chart().getItem(1).hover(); //ховер, чтоб убрать тултип
        basePageSteps.onLkSalesPage().getSale(0).chart().getItem(0).hover();

        basePageSteps.onLkSalesPage().activePopup().waitUntil(hasText("3 звонка\n0 добавили в избранное\n50 просмотров"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить панораму»")
    public void shouldClickAddPanoramaButton() {
        basePageSteps.onLkSalesPage().getSale(0).addPanoramaButton().click();

        basePageSteps.onLkSalesPage().popup().waitUntil(isDisplayed()).should(hasText("360°\nС панорамой — " +
                "больше звонков до 2,5 раз\nСнять панораму можно в приложении Авто.ру на любом смартфоне. " +
                "Отсканируйте QR-код, чтобы скачать приложение."));
    }

}

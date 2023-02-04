package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Активное б/у объявление частника под владельцем")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActiveSaleUsedUserOwnerCommercialTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferTrucksUsedUserOwner",
                "desktop/ReferenceCatalogMotoDictionariesV1Equipment").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение объявления")
    public void shouldSeeSale() {
        basePageSteps.onCardPage().contacts().should(hasText("Частное лицо\nМоскваЩелковская\n+7 222 222-22-22\n" +
                "Почему не мой номер"));

        basePageSteps.onCardPage().cardVas().should(hasText("Турбо-продажа\n697 ₽\nЭкспресс-продажа\n397 ₽\n" +
                "Поднятие в поиске\n297 ₽\nПАКЕТ ОПЦИЙ\nТурбо-продажа\nx20 просмотров\nВаше предложение увидит " +
                "максимум посетителей — это увеличит шансы на быструю и выгодную продажу. " +
                "Объявление будет выделено цветом, поднято в топ, размещено в специальном блоке на главной странице, " +
                "на странице марки и в выдаче объявлений.\nПодключить за 697 ₽\nВместо 1 161 ₽\n" +
                "Включены: Выделение цветом, Спецпредложение, Поднятие в ТОП"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onCardPage().cardOwnerPanel().button("Редактировать").click();
        urlSteps.testing().path(TRUCKS).path(EDIT).path(SALE_ID).shouldNotSeeDiff();
    }
}
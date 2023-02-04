package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление частника под владельцем")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveSaleUsedUserOwnerTrucksTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferTrucksUsedUserOwner",
                "desktop/ReferenceCatalogTrucksDictionariesV1Equipment").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение даты")
    public void shouldSeeDate() {
        basePageSteps.onCardPage().dateAndStats().should(hasText("15 сохранили\nдо снятия"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение галереи")
    public void shouldSeeGallery() {
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(isDisplayed())
                .should(hasAttribute("src", "https://avatars.mds.yandex.net/get-autoru-vos/6408148/" +
                        "405be7e3fedf13c29905ee6d5b37d1c8/456x342"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Характеристики\nгод выпуска\n2005\n" +
                "Пробег\n270 000 км\nКузов\nбортовой грузовик\nЦвет\nбелый\nДвигатель\n2.5 л / 145 л.с. / Бензин\n" +
                "Г/подъёмность\n1.5 т\nКоробка\nмеханическая\nРуль\nЛевый\nСостояние\nНе требует ремонта\n" +
                "Владельцы\n2 владельца\nПТС\nОригинал\nВладение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\n" +
                "Таможня\nРастаможен\nОбмен\nРассмотрю варианты")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация\nБезопасность\n1\nУправление\n1\n" +
                "Защита от угона\n2\nЭкстерьер\n1\nКомфорт\n3\nОбзор\n3"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не должно быть кнопки «Позвонить»")
    public void shouldNotSeeCallButton() {
        basePageSteps.onCardPage().floatingContacts().callButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не должно быть кнопки «Позвонить» в галерее")
    public void shouldNotSeeGalleryCallButton() {
        basePageSteps.onCardPage().gallery().getItem(0).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Редактировать"));
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam("action", "edit").shouldNotSeeDiff();
    }
}

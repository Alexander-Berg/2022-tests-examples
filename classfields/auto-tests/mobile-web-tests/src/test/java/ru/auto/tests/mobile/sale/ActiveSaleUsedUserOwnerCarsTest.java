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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление частника под владельцем")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ActiveSaleUsedUserOwnerCarsTest {

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
                "desktop/OfferCarsUsedUserOwner",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
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
        basePageSteps.onCardPage().gallery().getItem(0).panoramaPromo()
                .should(hasText("Добавьте панораму —\nполучите ×2,5 звонков"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Характеристики\nгод выпуска\n2008\n" +
                "Пробег\n210 000 км\nКузов\nвнедорожник 5 дв.\nЦвет\nсеребристый\nДвигатель\n" +
                "2.7 л / 190 л.с. / Дизель\nКомплектация\nHSE\nКоробка\nавтоматическая\nПривод\nполный\nРуль\nЛевый\n" +
                "Состояние\nНе требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\nВладение" +
                "\n\\d+ (год|года|лет)( и \\d+ (месяц|месяца|месяцев))?\n" +
                "Таможня\nРастаможен\nVIN\nSALLAAA148A485103\nГосномер\nА900ВН777\nВсе характеристики")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация HSE\nОбзор\n9\nЭлементы экстерьера\n3\n" +
                "Защита от угона\n4\nМультимедиа\n7\nСалон\n12\nКомфорт\n12\nБезопасность\n7\nПрочее\n3"));
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

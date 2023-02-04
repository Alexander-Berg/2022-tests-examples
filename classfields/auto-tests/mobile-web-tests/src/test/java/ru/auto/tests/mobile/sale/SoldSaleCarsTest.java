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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятое с продажи объявление легковых")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SoldSaleCarsTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserInactive",
                "desktop/OfferCarsSpecials",
                "desktop/OfferCarsRelated").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Плашка «уже продан»")
    public void shouldSeeSoldMessage() {
        basePageSteps.onCardPage().statusSold().waitUntil(isDisplayed())
                .should(hasText("Этот автомобиль уже продан\n "));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение спецпредложений")
    public void shouldSeeSpecials() {
        basePageSteps.onCardPage().damages().hover();
        basePageSteps.onCardPage().specials().title().waitUntil(hasText("Спецпредложения"));
        basePageSteps.onCardPage().specials().getSpecial(0).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение похожих")
    public void shouldSeeRelated() {
        basePageSteps.onCardPage().damages().hover();
        basePageSteps.onCardPage().related().title().waitUntil(hasText("Похожие объявления"));
        basePageSteps.onCardPage().related().getRelated(0).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение учебника")
    public void shouldSeeTextbook() {
        basePageSteps.onCardPage().footer().hover();
        basePageSteps.onCardPage().textbook().waitUntil(isDisplayed());
        basePageSteps.onCardPage().textbook().articlesList().subList(0, 4)
                .forEach(article -> article.should(isDisplayed()));
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
}

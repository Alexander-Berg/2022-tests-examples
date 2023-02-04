package ru.auto.tests.desktop.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Карточка объявления легковых - промо отзывов после снятия с продажи")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsSalePromoTest {

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
                "desktop/UserOffersCarsHide").post();

        urlSteps.testing().path(CARS).path(USED).path(Pages.SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение промо")
    public void shouldSeePromo() {
        deactivateSale();
        basePageSteps.onCardPage().reviewsPromo().waitUntil(isDisplayed())
                .should(hasText("Оставьте отзыв о вашем автомобиле\nПомогите другим людям сделать правильный выбор\n" +
                        "Оставить отзыв\nНе оставлять"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Оставить отзыв»")
    public void shouldClickSaveButton() {
        deactivateSale();

        basePageSteps.onCardPage().reviewsPromo().saveButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().reviewsPromo().waitUntil(not(isDisplayed()));
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(anyOf(startsWith(urlSteps.testing().path(CARS).path(REVIEWS).path(EDIT).toString()),
                startsWith(urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).path(SALE_ID)
                        .addParam("rvw_campaign", "dsktp_offer_promo").toString())));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие промо")
    public void shouldClosePromo() {
        deactivateSale();

        String saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.onCardPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().reviewsPromo().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotDiffWith(saleUrl);
    }

    @Step("Снимаем объявление с продажи")
    public void deactivateSale() {
        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().button("Снять с продажи").waitUntil(isEnabled()).click();
    }
}
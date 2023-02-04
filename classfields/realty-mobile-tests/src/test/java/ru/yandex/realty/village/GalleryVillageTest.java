package ru.yandex.realty.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALLBACK;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class GalleryVillageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(380, 950);
        urlSteps.testing().villageCardMobile().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея закрывается")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesClosedGallery() {
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().waitUntil(isDisplayed());
        basePageSteps.onVillageCardPage().gallery().closeCrossHeader().click();
        basePageSteps.onVillageCardPage().gallery().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим кнопку «Позвонить»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesCallButton() {
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL).waitUntil(isDisplayed());
        basePageSteps.onVillageCardPage().gallery().link(CALL).should(not(isDisabled()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим попап «Перезвоните мне»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesCallBackPopup() {
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().button(CALLBACK).click();
        basePageSteps.onVillageCardPage().callbackPopup().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап «Перезвоните мне» остаемся в галерее")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesReturnToGallery() {
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().button(CALLBACK).click();
        basePageSteps.onVillageCardPage().callbackPopup().closeCross().click();
        basePageSteps.onVillageCardPage().gallery().button(CALLBACK).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап «Перезвоните мне» остаемся в галерее")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesPromoHint() {
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().promoHint().click();
        basePageSteps.onVillageCardPage().popupVisible()
                .should(allOf(hasText(containsString("ГК «Астерра»")), hasText(containsString("Реклама"))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот галереи")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesGalleryScreenshot() {
        compareSteps.resize(390, 800);
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().gallery());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().gallery());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}

package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
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
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALLBACK;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class GalleryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    private static final String PHOTO = "фото";

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().navbarShortcut(PHOTO));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея закрывается")
    public void shouldSeeClosedGallery() {
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onNewBuildingCardPage().gallery().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingCardPage().gallery().closeCrossHeader().click();
        basePageSteps.onNewBuildingCardPage().gallery().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим кнопку «Позвонить»")
    public void shouldSeeCallButton() {
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL).waitUntil(isDisplayed());
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL).should(not(isDisabled()));
    }

    @Test
    @Ignore("Убрали кнопку обратного звонка. Тесты надо будет перевести на новый MOCK с нерабочим временем.")
    @Owner(KANTEMIROV)
    @DisplayName("Видим попап «Перезвоните мне»")
    public void shouldSeeCallBackPopup() {
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onNewBuildingCardPage().gallery().button(CALLBACK).click();
        basePageSteps.onNewBuildingCardPage().callbackPopup().should(isDisplayed());
    }

    @Test
    @Ignore("Убрали кнопку обратного звонка. Тесты надо будет перевести на новый MOCK с нерабочим временем.")
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап «Перезвоните мне» остаемся в галерее")
    public void shouldSeeReturnToGallery() {
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onNewBuildingCardPage().gallery().button(CALLBACK).click();
        basePageSteps.onNewBuildingCardPage().callbackPopup().closeCross().click();
        basePageSteps.onNewBuildingCardPage().gallery().button(CALLBACK).should(isDisplayed());
    }

    @Test
    @Ignore("Убрали кнопку обратного звонка. Тесты надо будет перевести на новый MOCK с нерабочим временем.")
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап «Перезвоните мне» остаемся в галерее")
    public void shouldSeePromoHint() {
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onNewBuildingCardPage().gallery().promoHint().click();
        basePageSteps.onNewBuildingCardPage().popupVisible()
                .should(allOf(hasText(containsString("Группа Компаний ПИК")), hasText(containsString("Реклама"))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот галереи")
    public void shouldSeeGalleryScreenshot() {
        compareSteps.resize(390, 800);
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().gallery());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().navbarShortcut(PHOTO));
        basePageSteps.onNewBuildingCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().gallery());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}

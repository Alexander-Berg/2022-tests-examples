package ru.auto.tests.mobile.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Заметки на карточке объявления")
@Feature(NOTES)
@RunWith(value = GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class NotesSaleUnregTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String NOTE = "Note text";
    private String saleUrl;

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/UserNotesCarsPut",
                "desktop/UserFavoritesCarsPost").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
        scrollToNote();
        basePageSteps.onCardPage().cardActions().noteButton().should(isDisplayed()).hover().click();

        mockRule.with("desktop/SessionAuthUser").update();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Issue("AUTORUFRONT-21218")
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление заметки")
    public void shouldAddNote() {
        basePageSteps.onAuthPage().input("Номер телефона").sendKeys("9111111111");
        basePageSteps.onAuthPage().input("Код из смс", "1234");
        urlSteps.shouldNotDiffWith(saleUrl);
        basePageSteps.onCardPage().cardActions().notePopup().input().waitUntil(isDisplayed()).sendKeys(NOTE);
        basePageSteps.onCardPage().cardActions().notePopup().button("Сохранить").should(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявления с заметками автоматически добавляются в «Избранные объявления», " +
                        "чтобы вы могли легко их найти"));
        basePageSteps.onCardPage().note().should(hasText(NOTE));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed());
    }

    @Step("Скроллим к заметке")
    private void scrollToNote() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().cardActions().noteButton(), 0, 0);
        basePageSteps.scrollDown(basePageSteps.onCardPage().cardActions().noteButton().getSize().getHeight() * 3);
    }
}

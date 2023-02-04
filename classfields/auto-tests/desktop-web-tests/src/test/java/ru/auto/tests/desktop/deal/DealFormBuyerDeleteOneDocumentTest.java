package ru.auto.tests.desktop.deal;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка. Форма. Блок с личной встречей и подтверждением сделки")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealFormBuyerDeleteOneDocumentTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/SafeDealDealGetWithBuyerDealDocuments",
                "desktop/SafeDealDealUpdateBuyerDeleteDocument").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один документ под покупателем")
    public void shouldDeleteOneDocumentByBuyer() {
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").dkpSecondPagePhoto()
                .photo().should(isDisplayed());
        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithBuyerDealOneDocument");

        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").dkpSecondPagePhoto()
                .button("Удалить фото").click();

        basePageSteps.onDealPage().notifier().should(hasText("Фотографии удалены"));
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").dkpSecondPagePhoto()
                .photo().should(not(isDisplayed()));
    }
}
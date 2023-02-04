package ru.auto.tests.desktop.lk.c2bauction;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.NEW;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplication.c2bAuctionApplication;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplicationsList.userApplicationsResponse;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_APPLICATION_LIST;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Деактивация заявки аукциона")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.C2B_AUCTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DeactivateAuctionTest {

    private static final String ID = getRandomId();

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
                stub().withGetDeepEquals(C2B_AUCTION_APPLICATION_LIST).
                        withResponseBody(
                                userApplicationsResponse().setApplications(
                                        c2bAuctionApplication().setId(ID).setStatus(NEW)
                                ).build()),
                stub().withPostDeepEquals(format("/1.0/c2b-auction/application/%s/close", ID)).withStatusSuccessResponse(),
                sessionAuthUserStub(),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(C2B_AUCTION).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отзываем заявку")
    public void shouldCancelApplication() {
        basePageSteps.onLkSalesPage().getApplication(0).button("Отозвать заявку").click();
        basePageSteps.onLkSalesPage().popup().isDisplayed();
        basePageSteps.onLkSalesPage().popup().button("Отозвать").click();

        basePageSteps.onLkSalesPage().getApplication(0).auctionResult().should(hasText("Сделка\nне состоялась"));
    }
}

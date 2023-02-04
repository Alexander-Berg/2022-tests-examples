package ru.auto.tests.desktop.lk.c2bauction;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.AUCTION;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.DEAL;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.FINISHED;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.NEW;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplication.c2bAuctionApplication;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplicationsList.userApplicationsResponse;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_APPLICATION_LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Активное объявление аукциона")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.C2B_AUCTION)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopTestsModule.class)
public class ActiveAuctionTest {

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

    @Parameterized.Parameter
    public StatusName statusName;

    @Parameterized.Parameter(1)
    public String statusValue;

    @Parameterized.Parameter(2)
    public String statusText;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {NEW, "Назначаем и проводим встречу", "Skoda Octavia III (A7) Рестайлинг, 2020\nОсмотр\nНазначаем и проводим встречу," +
                        " чтобы финально оценить машину\nОтозвать заявку"},
                {AUCTION, "Уточняем у дилеров итоговую цену", "Skoda Octavia III (A7) Рестайлинг, 2020\nСбор предложений\nУточняем у дилеров итоговую цену и выбираем" +
                        " лучшую\nОстались вопросы? С 9 до 18 в будни +7 495 792-65-69\nОтозвать заявку"},
                {DEAL, "Выбрали лучшее предложение!", "Skoda Octavia III (A7) Рестайлинг, 2020\nИтоговая цена\nВыбрали лучшее предложение! Скоро позвоним и " +
                        "расскажем о нём\nОстались вопросы? С 9 до 18 в будни +7 495 792-65-69\nОтозвать заявку"},
                {FINISHED, "Сделка состоялась", "Skoda Octavia III (A7) Рестайлинг, 2020\nСделка\nсостоялась"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(C2B_AUCTION_APPLICATION_LIST).
                        withResponseBody(
                                userApplicationsResponse().setApplications(
                                        c2bAuctionApplication().setStatus(statusName)
                                ).build()),
                sessionAuthUserStub(),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(C2B_AUCTION).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Аукцион в статусе {{statusValue}}")
    public void shouldSeeApplicationStatus() {
        basePageSteps.onLkSalesPage().getApplication(0).should(hasText(statusText));
    }
}

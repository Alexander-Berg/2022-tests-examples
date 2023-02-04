package ru.auto.tests.mobile.c2bauction;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отображение попапов аукциона")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.C2B_AUCTION)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(MobileEmulationTestsModule.class)

public class AuctionPopup {

    public static final String PREPARE = "Как подготовиться к осмотру?\nЧем лучше вы подготовитесь, тем дороже сможете продать свой автомобиль\nПомыть автомобиль\nЧистую машину проще оценить\nВзять документы\nВам пригодятся паспорт, ПТС, СТС\nНайти ключи\nВозьмите с собой второй комплект ключей\nПодготовить резину\nКомплект сезонной резины увеличит стоимость выкупа машины";
    public static final String WORK = "Продаёте автомобиль?\nАвто.ру возьмёт хлопоты на себя\nЭто бесплатно и не обязывает вас продавать машину, если цена не устроит\nОнлайн-оценка за 3 часа\nЗа это время узнаем, сколько дилеры готовы заплатить\nЭкономия времени\nНесколько звонков и одна встреча — всё, что нужно для сделки\nЮридическая чистота\nСделка с партнером Авто.ру. Деньги — наличными или на карту\nКак это работает\nВыкуп проходит за 5 простых шагов. Обычно это занимает от 1 до 3 дней\n1\nОнлайн-оценка\nПолучаете предварительные оценки автомобиля от дилеров\n2\nОсмотр машины\nСпециалист Авто.ру приезжает в удобное для вас время и место\n3\nСбор предложений\nДилеры предлагают финальную цену по результатам осмотра\n4\nИтоговая цена\nВыбираем для вас лучшее предложение\n5\nВыкуп авто\nПолучаете деньги и сдаёте машину в салоне дилера\nОстались вопросы?\n+7 495 792-65-69\nПока заявка в работе, пользователи Авто.ру не увидят объявление о продаже этой машины\n«Выкуп» является маркетинговым названием сервиса по информационному обеспечению участия в Сборе ценовых предложений, предоставляемого в соответствии с правилами.";

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
    public String buttonName;

    @Parameterized.Parameter(1)
    public String popupValue;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                        {"Как подготовиться к\u00A0осмотру?", PREPARE},
                        {"Как работает «Авто.ру Выкуп»?", WORK}
                }
        );
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(C2B_AUCTION_APPLICATION_LIST).
                        withResponseBody(
                                userApplicationsResponse().setApplications(
                                        c2bAuctionApplication().setStatus(NEW)
                                ).build()),
                sessionAuthUserStub(),
                stub("desktop/User")
        ).create();


        urlSteps.testing().path(MY).path(C2B_AUCTION).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение попапа")
    public void shouldSeeApplicationStatus() {
        basePageSteps.onLkPage().getApplication(0).button(buttonName).click();

        basePageSteps.onLkPage().popup().should(isDisplayed()).should(hasText(popupValue));
    }

}

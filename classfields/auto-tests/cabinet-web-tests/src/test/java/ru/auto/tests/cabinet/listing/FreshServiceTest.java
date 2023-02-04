package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_APPLIED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.FRESH_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Услуга «Поднять»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FreshServiceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS},
                {TRUCKS},
                {MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsProductsFreshPost"),
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersTrucksProductsFreshPost"),
                stub("cabinet/UserOffersMotoUsed"),
                stub("cabinet/UserOffersMotoProductsFreshPost")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "active").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Нотификация с подробным описанием услуги")
    public void shouldSeeRaiseNotificationPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().hover();

        steps.onCabinetOffersPage().popup().should(hasText("Поднятие в поиске\n" +
                "Данная услуга не просто поднимает ваше объявление в поиске, но еще и обновляет дату " +
                "публикации и количество просмотров объявления. Таким образом, она отлично работает даже в " +
                "случае самых застоявшихся автомобилей.\nРазовое поднятие за 350 ₽\nАвтоприменение\n" +
                "Пн\nВт\nСр\nЧт\nПт\nСб\nВс\n--:--\nСохранить расписание"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поднятие оффера в поиске")
    public void shouldSeeRaisedOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh()
                .should(hasClass(containsString(FRESH_ACTIVE)));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап «Ваше объявление уже поднято»")
    public void shouldSeeRaisedPopupWithWarning() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText(SERVICE_APPLIED));

        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().click();
        steps.onCabinetOffersPage().popup().should(hasText("Обратите внимание\nУслугу повторно можно будет " +
                "применить через 10 минут\nЗакрыть"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Покупка поднятия по кнопке в попапе")
    public void shouldApplyFreshServiceFromPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().hover();
        steps.onCabinetOffersPage().popup().buttonContains("Разовое поднятие").click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh()
                .should(hasClass(containsString(FRESH_ACTIVE)));
    }
}

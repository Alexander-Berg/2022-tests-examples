package ru.auto.tests.cabinet.listing;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Отправка сообщения о скидке")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FavoriteDiscountMessageTest {

    private static final String VALIDITY_PERIOD = "2 часа";
    private static final String PASSPHRASE = "QA";
    private static final String SUCCESSFUL_SENT = "Сообщения успешно отправлены";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/InciteGetFavoriteMessageEmpty")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
        steps.hideElement(steps.onCabinetOffersPage().panoramaPromo());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Попап про скидку добавившим в избранное")
    public void shouldSeeFavoriteDiscountPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().hover();

        steps.onCabinetOffersPage().popup().should(hasText("Предложения пользователям, добавившим объявления " +
                "в избранное\nОтправьте сообщение пользователям, которые добавили Ваше объявление в избранное. " +
                "Эта услуга позволит разослать специальные предложения в чат пользователям, которые показали " +
                "свой интерес к автомобилю, и привлечет их внимание.\nОтправить 1 предложение"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Шторка с отправкой сообщения добавившим в избранное")
    public void shouldOpenFavoriteDiscountMessageCurtain() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();

        steps.onCabinetOffersPage().favoriteDiscountsCurtain().should(hasText("Предложение для 1 пользователя\n" +
                "Скидка, %\nСкидка, ₽\nПодарок\nРазмер скидки, %\nСрок действия\n\nКодовое слово\n" +
                "Сообщение, которое получат покупатели\nLand Rover Range Rover\n490 000 ₽  500 000 ₽\n" +
                "Позвоните по объявлению в течение 48 часов, чтобы получить скидку 2%. " +
                "Предложение действует, пока не найдётся первый покупатель.\nОтправить сообщение"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Шторка с массовой отправкой сообщения добавившим в избранное")
    public void shouldOpenFavoriteDiscountMessageCurtainByGroupButton() {
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();
        steps.onCabinetOffersPage().groupServiceButtons().favorites().click();

        steps.onCabinetOffersPage().favoriteDiscountsCurtain().should(hasText("Предложение для 1 пользователя\n" +
                "Скидка, %\nСкидка, ₽\nПодарок\nРазмер скидки, %\nСрок действия\n\nКодовое слово\n" +
                "Сообщение, которое получат покупатели\nLand Rover Range Rover\n490 000 ₽  500 000 ₽\n" +
                "Позвоните по объявлению в течение 48 часов, чтобы получить скидку 2%. " +
                "Предложение действует, пока не найдётся первый покупатель.\nОтправить сообщение"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отправляем сообщение о скидке в процентах")
    public void shouldSendDiscountMessagePercent() {
        mockRule.setStubs(stub("cabinet/InciteSendFavoriteMessageDiscountPercent")).update();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Скидка, %").click();
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().selectItem("Срок действия", VALIDITY_PERIOD);
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Размер скидки, %"), "10");
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Кодовое слово"), PASSPHRASE);
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().messageExample().waitUntil(hasText("Land Rover Range Rover\n" +
                "450 000 ₽  500 000 ₽\nПозвоните по объявлению в течение 2 часов, чтобы получить скидку 10%. " +
                "Скажите менеджеру кодовое слово «QA». Предложение действует, пока не найдётся первый покупатель."));
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Отправить сообщение").click();

        steps.onCabinetOffersPage().notifier().should(hasText(SUCCESSFUL_SENT));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().counter().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отправляем сообщение о скидке в рублях")
    public void shouldSendDiscountMessageRubles() {
        mockRule.setStubs(stub("cabinet/InciteSendFavoriteMessageDiscountRubles")).update();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Скидка, ₽").click();
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().selectItem("Срок действия", VALIDITY_PERIOD);
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Размер скидки, ₽"), "100000");
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Кодовое слово"), PASSPHRASE);
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().messageExample().waitUntil(hasText("Land Rover Range Rover\n" +
                "400 000 ₽  500 000 ₽\nПозвоните по объявлению в течение 2 часов, чтобы получить скидку 100000 ₽. " +
                "Скажите менеджеру кодовое слово «QA». Предложение действует, пока не найдётся первый покупатель."));
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Отправить сообщение").click();

        steps.onCabinetOffersPage().notifier().should(hasText(SUCCESSFUL_SENT));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().counter().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отправляем сообщение о подарке")
    public void shouldSendDiscountMessageGift() {
        mockRule.setStubs(stub("cabinet/InciteSendFavoriteMessageGift")).update();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Подарок").click();
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Текст сообщения 0/40 символов"), "аааавтомобиль");
        steps.input(steps.onCabinetOffersPage().favoriteDiscountsCurtain().input("Кодовое слово"), PASSPHRASE);
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().selectItem("Срок действия", VALIDITY_PERIOD);
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().messageExample().waitUntil(hasText("Land Rover Range Rover " +
                "+ подарок\n500 000 ₽  \nПозвоните по объявлению в течение 2 часов, чтобы получить в подарок " +
                "аааавтомобиль. Скажите менеджеру кодовое слово «QA». Предложение действует, пока не найдётся " +
                "первый покупатель."));
        steps.onCabinetOffersPage().favoriteDiscountsCurtain().button("Отправить сообщение").click();

        steps.onCabinetOffersPage().notifier().should(hasText(SUCCESSFUL_SENT));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().counter().should(not(isDisplayed()));
    }
}

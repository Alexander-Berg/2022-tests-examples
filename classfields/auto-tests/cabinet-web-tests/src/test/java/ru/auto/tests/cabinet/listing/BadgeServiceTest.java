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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.STICKER_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Услуга «Стикеры»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class BadgeServiceTest {

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
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsProductsBadgePost"),
                stub("cabinet/UserOffersCarsProductsOneBadgePost"),
                stub("cabinet/UserOffersCarsProductsBadgeDelete")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active")
                .open();

    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап «Стикеры быстрой продажи»")
    public void shouldSeeQuickStickersNotificationPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().hover();

        steps.onCabinetOffersPage().activePopup().should(isDisplayed()).should(hasText("Стикеры быстрой продажи" +
                "\nНебольшие цветные блоки, которые отображаются в карточке при поиске. Содержат надпись, " +
                "которая позволяет привлечь к машине больше внимания. Стикеры выделят ваш автомобиль среди " +
                "конкурентов и помогут продать его быстрее.\nЦена одного стикера 20 ₽ в сутки. Выберите не " +
                "больше 3:\n(осталось 3)\nПарктроник\nКоврики в подарок\nWebasto\nЕщё 11\nСвой текст (до 25 символов)\n" +
                "Сохранить"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап «Стикеры быстрой продажи» с выбранными стикерами")
    public void shouldSeeQuickStickersPopupWithChosenStickers() {
        steps.addStickers(0, "Парктроник", "Коврики в подарок");
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().hover();

        steps.onCabinetOffersPage().activePopup().should(isDisplayed()).should(hasText("Стикеры быстрой продажи\n" +
                "Небольшие цветные блоки, которые отображаются в карточке при поиске. Содержат надпись, " +
                "которая позволяет привлечь к машине больше внимания. Стикеры выделят ваш автомобиль среди " +
                "конкурентов и помогут продать его быстрее.\nЦена одного стикера 20 ₽ в сутки. Выберите не " +
                "больше 3:\n(осталось 1)\nПарктроник\nКоврики в подарок\nWebasto\nЕщё 11\n" +
                "Свой текст (до 25 символов)\n" +
                "Сохранить\nОтключить все стикеры"));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Стикеры» после сохранения стикера")
    public void shouldSeeActiveStickersButton() {
        steps.addStickers(0, "Парктроник", "Коврики в подарок");

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers()
                .should(hasClass(containsString(STICKER_ACTIVE)));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().should(hasText("2"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Должны задизейблить инпут своего стикера когда выбрано 3")
    public void shouldDisableOwnStickerInputWhenSelectedTheeStickers() {
        steps.selectStickers(0, "Парктроник", "Коврики в подарок", "Webasto");

        steps.onCabinetOffersPage().quickSaleStickers().input("Свой текст (до 25 символов)").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Удаляем один стикер")
    public void shouldRemoveOneSticker() {
        steps.addStickers(0, "Парктроник", "Коврики в подарок");
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().waitUntil(hasText("2"));

        steps.addStickers(0, "Парктроник");

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers()
                .should(hasClass(containsString(STICKER_ACTIVE)));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().should(hasText("1"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Удаляем все стикеры")
    public void shouldRemoveAllStickers() {
        steps.addStickers(0, "Парктроник", "Коврики в подарок");
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().waitUntil(hasText("2"));

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().hover();
        steps.onCabinetOffersPage().quickSaleStickers().button("Отключить все стикеры").click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers()
                .should(not(hasClass(containsString(STICKER_ACTIVE))));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Создание собственного стикера")
    public void shouldCreateOwnStickers() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsCustomBadgePost")).update();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().hover();
        steps.onCabinetOffersPage().quickSaleStickers().waitUntil(isDisplayed());
        steps.onCabinetOffersPage().quickSaleStickers().sticker("Ещё 11").click();

        steps.onCabinetOffersPage().quickSaleStickers().input("Свой текст (до 25 символов)", "my_sticker");

        steps.onCabinetOffersPage().quickSaleStickers().applyOwnSticker().waitUntil(isEnabled());
        steps.onCabinetOffersPage().quickSaleStickers().applyOwnSticker().click();

        steps.onCabinetOffersPage().quickSaleStickers().sticker("my_sticker").waitUntil(isDisplayed());

        steps.onCabinetOffersPage().quickSaleStickers().apply().click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Услуга применена"));
        steps.onCabinetOffersPage().quickSaleStickers().waitUntil(not(isDisplayed()));

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers()
                .should(hasClass(containsString(STICKER_ACTIVE)));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().counter().should(hasText("1"));
    }
}

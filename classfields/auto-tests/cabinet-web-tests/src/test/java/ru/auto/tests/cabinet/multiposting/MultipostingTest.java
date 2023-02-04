package ru.auto.tests.cabinet.multiposting;

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

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_APPLIED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Мультипостинг")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class MultipostingTest {

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
                stub("cabinet/ClientsGetMultipostingEnabled"),
                stub("cabinet/DealerInfoMultipostingEnabled"),
                stub("cabinet/UserOffersCarsMarkModelsUsed"),
                stub("cabinet/UserOffersCarsUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsCountUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsCountUsedMultipostingActiveInactive"),
                stub("cabinet/UserOffersCarsCountUsedMultipostingInactive")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Сниппет объявления")
    public void shouldSeeOffersSnippet() {
        steps.onCabinetOffersPage().snippet(0).should(hasText("Добавить фотографии\nМожно забронировать\n" +
                "83% похожих объявлений выше\nДоставка в города\nLADA (ВАЗ) Niva Off-road I, 2020\n200 000 км\n" +
                "1.7 MT (80 л.с.) 4WD\nVIN не указан\nПоказать всё\n2 000 ₽\nНа складе3д.\nАвто.ру\nВ продаже3д.\n" +
                "Ниже 83% похожих\nАвито\nВ продажед.\nДром\nБыло в продажед.\nПодробнее\nПодключить"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение статистики")
    public void shouldSeeStats() {
        steps.onCabinetOffersPage().snippet(0).button("Подробнее").click();

        steps.onCabinetOffersPage().snippet(0).should(hasText("Добавить фотографии\nМожно забронировать\n" +
                "83% похожих объявлений выше\nДоставка в города\nLADA (ВАЗ) Niva Off-road I, 2020\n200 000 км\n" +
                "1.7 MT (80 л.с.) 4WD\nVIN не указан\nПоказать всё\n2 000 ₽\nНа складе3д.\nАвто.ру\nВ продаже3д.\n" +
                "Ниже 83% похожих\nАвито\nВ продажед.\nДром\nБыло в продажед.\nПодробнее\nНе получилось загрузить " +
                "данные. Проверьте интернет и попробуйте ещё раз\nПовторить загрузку\n19 просмотров\nВсе\nАвто.ру\n" +
                "Авито\n43 просмотра контактов\n20\n23\n26\n29\n1 нояб.\n4\n7\n10\n13\n16\nЗвонки в" +
                " зависимости от цены," +
                " ₽\nАнализируйте поступившие звонки. Мы автоматически выделим целевые и уникальные звонки.\n" +
                "Подключить колтрекинг\nРасходы\nВсе расходы\nРазмещение\nУслуги\n0 ₽\n0 ₽\nПодключить"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по сниппету")
    public void shouldClickSnippet() {
        steps.onCabinetOffersPage().snippet(0).title().click();
        urlSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("/vaz/niva/1101567979-eb813af5/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Авто.ру по кнопке «Подключить Авто.ру»")
    public void shouldEnableAutoRu() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingAutoru")).update();

        steps.onCabinetOffersPage().snippet(3).multipostingAutoruButton().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Авто.ру"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авто.ру").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Авто.ру по кнопке «Подключить»")
    public void shouldEnableAutoRuViaColumnButton() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingAutoru")).update();

        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авто.ру").toggle().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Авто.ру"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авто.ру").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Авито по кнопке «Подключить Авито»")
    public void shouldEnableAvito() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingAvito")).update();

        steps.onCabinetOffersPage().snippet(3).multipostingAvitoButton().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Авито"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авито").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Авито по кнопке «Подключить»")
    public void shouldEnableAvitoViaColumnButton() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingAvito")).update();

        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авито").toggle().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Авито"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Авито").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Дром по кнопке «Подключить Дром»")
    public void shouldEnableDrom() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingDrom")).update();

        steps.onCabinetOffersPage().snippet(3).multipostingDromButton().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Дром"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Дром").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение Дром по кнопке «Подключить»")
    public void shouldEnableDromViaColumnButton() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsMultipostingDrom")).update();

        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Дром").toggle().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление размещено на Дром"));
        steps.onCabinetOffersPage().snippet(3).classifiedColumn("Дром").toggle()
                .should(hasClass(containsString("Toggle_checked")));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по кнопке «Услуги»")
    public void shouldPhotoLeadOnOffersPage() {
        steps.onCabinetOffersPage().snippet(2).multipostingDromServicesButton()
                .should(hasAttribute("href", "https://auto.drom.ru/all/?my")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Попап услуги Авито - x2 просмотров")
    public void shouldSeeAvitoX2ViewsPopup() {
        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x2").hover();

        steps.onCabinetOffersPage().avitoPopup().should(isDisplayed())
                .should(hasText("До x2 Просмотров\n1 день\n7 дней"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Попап услуги Авито - x5 просмотров")
    public void shouldSeeAvitoX5ViewsPopup() {
        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x5").hover();

        steps.onCabinetOffersPage().avitoPopup().should(isDisplayed())
                .should(hasText("До x5 Просмотров\n1 день\n7 дней"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Попап услуги Авито - x10 просмотров")
    public void shouldSeeAvitoX10ViewsPopup() {
        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x10").hover();

        steps.onCabinetOffersPage().avitoPopup().should(isDisplayed())
                .should(hasText("До x10 Просмотров\n1 день\n7 дней"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Попап услуги Авито - XL")
    public void shouldSeeAvitoXLPopup() {
        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("xl").hover();

        steps.onCabinetOffersPage().avitoPopup().should(isDisplayed())
                .should(hasText("XL-объявление\nXL-объявления показываются в поиске с кнопкой «Показать телефон» " +
                        "(в приложении и в мобильной версии — «Позвонить»). В поиске также видно начало описания " +
                        "объявления и некоторые характеристики.\nза 7 дней за объявление"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Попап услуги Авито - выделение цветом")
    public void shouldSeeAvitoHighlightPopup() {
        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("highlight").hover();

        steps.onCabinetOffersPage().avitoPopup().should(isDisplayed())
                .should(hasText("Выделение цветом\nДелает объявление заметнее в поиске. Цена выделяется " +
                        "цветом, поэтому объявление привлекает внимание. В мобильной версии цветом выделяется " +
                        "название объявления.\nза 7 дней за объявление"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x2 просмотров на 1 день")
    public void shouldEnableAvitoX2Views1day() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX2_1")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x2").hover();
        steps.onCabinetOffersPage().avitoPopup().button("1 день").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x2").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x2 просмотров на 7 дней")
    public void shouldEnableAvitoX2Views7days() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX2_7")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x2").hover();
        steps.onCabinetOffersPage().avitoPopup().button("7 дней").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x2").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x5 просмотров на 1 день")
    public void shouldEnableAvitoX5Views1day() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX5_1")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x5").hover();
        steps.onCabinetOffersPage().avitoPopup().button("1 день").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x5").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x5 просмотров на 7 дней")
    public void shouldEnableAvitoX5Views7days() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX5_7")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x5").hover();
        steps.onCabinetOffersPage().avitoPopup().button("7 дней").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x5").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x10 просмотров на 1 день")
    public void shouldEnableAvitoX10Views1day() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX10_1")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x10").hover();
        steps.onCabinetOffersPage().avitoPopup().button("1 день").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x10").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - x10 просмотров на 7 дней")
    public void shouldEnableAvitoX10Views7days() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoX10_7")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("x10").hover();
        steps.onCabinetOffersPage().avitoPopup().button("7 дней").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("x10").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - XL")
    public void shouldEnableAvitoXL() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoXL")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("xl").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("xl").should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение услуги Авито - выделение цветом")
    public void shouldEnableAvitoHighlight() {
        mockRule.setStubs(stub("cabinet/UserOffersCarsProductsAvitoHighlight")).update();

        steps.onCabinetOffersPage().snippet(0).avitoServiceButton("highligh").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText(SERVICE_APPLIED));
        steps.onCabinetOffersPage().snippet(0).avitoActiveServiceButton("highlight").should(isDisplayed());
    }
}

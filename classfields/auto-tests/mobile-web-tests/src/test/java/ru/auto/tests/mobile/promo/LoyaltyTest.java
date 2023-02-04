package ru.auto.tests.mobile.promo;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LOYALTY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Urls.FORMS_YANDEX_FEEDBACK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Промо - проверенный дилер")
@Feature(AutoruFeatures.PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class LoyaltyTest {

    private static final String PHONE = "tel:+74957555577";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(PROMO).path(LOYALTY).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoPage().content().should(hasText("Проверенный дилер\nКупите машину у проверенного дилера\n" +
                "Мы хотим изменить рынок автомобилей с пробегом, чтобы уменьшить число проблем при покупке. " +
                "Поэтому объявления некоторых дилерских центров теперь помечаются специальным знаком. " +
                "Он означает, что мы лично побывали в этом центре и всё проверили.\n" +
                "Что мы проверяем в дилерском центре?\nЧто адрес и контакты центра корректные\nЧто все машины " +
                "из объявлений на Авто.ру и правда в наличии\nЧто все машины именно такие, как сказано в объявлениях\n" +
                "Какие параметры машин мы сверяем с объявлениями?\nМодификацию\nПробег\nЧисло владельцев по ПТС\n" +
                "Состояние кузова и салона\nНомер VIN\nЦену\nЦвет кузова и салона\nЗнают ли сотрудники центра, " +
                "когда мы приедем?\nНет. Кроме того, мы даже во время проверки не всегда сообщаем им, что мы из Авто.ру. " +
                "Это для чистоты эксперимента.\nКаких дилеров мы проверяем?\nКоторые работают в Москве " +
                "и Санкт-Петербурге*\nКоторые не менее полугода продают машины на Авто.ру\nКоторые занимаются только " +
                "подержанными автомобилями\nК которым нет претензий от модераторов Авто.ру\n*Во всех остальных регионах " +
                "пока проводится только проверка службой модерации.\nЧто делать, если «проверенный» дилер меня " +
                "обманул?\nПожалуйста, позвоните по номеру +7 (495) 755-55-77 или напишите нам. Если обман подтвердится, " +
                "мы отберём у дилера статус проверенного."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Телефон должен быть ссылкой")
    public void shouldSeePhone() {
        basePageSteps.onPromoPage().loyaltyPhone().should(hasAttribute("href", PHONE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «напишите нам»")
    public void shouldClickHelpUrl() {
        basePageSteps.onPromoPage().loyaltyHelpUrl().click();
        urlSteps.testing().fromUri(FORMS_YANDEX_FEEDBACK).shouldNotSeeDiff();
    }
}

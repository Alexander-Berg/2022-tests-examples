package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Объявление - комплектация")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ComplectationCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String complectationMinimized = "Комплектация\nОбзор\n•\nКсеноновые/" +
            "Биксеноновые фары\n•\nЭлектрообогрев зоны стеклоочистителей\n•\nЭлектрообогрев лобового стекла\n•\n" +
            "Электрообогрев боковых зеркал\n•\nПротивотуманные фары\n•\nАвтоматический корректор фар\n•\n" +
            "Омыватель фар\n•\nДатчик дождя\nСалон\n•\nКожа (Материал салона)\n•\n" +
            "Регулировка сиденья водителя по высоте\n•\nРегулировка передних сидений по высоте\n•\n" +
            "Электрорегулировка сиденья водителя\n•\nЭлектрорегулировка передних сидений\n•\n" +
            "Подогрев передних сидений\n•\nПодогрев задних сидений\n•\nОтделка кожей рулевого колеса\nВсе опции";
    private static final String complectationMaximized = "Комплектация\nОбзор\n•\nКсеноновые/Биксеноновые фары\n•\n" +
            "Электрообогрев зоны стеклоочистителей\n•\n" +
            "Электрообогрев лобового стекла\n•\nЭлектрообогрев боковых зеркал\n•\nПротивотуманные фары\n•\n" +
            "Автоматический корректор фар\n•\nОмыватель фар\n•\nДатчик дождя\n•\nДатчик света\n" +
            "Элементы экстерьера\n•\nЛегкосплавные диски\n•\nДиски 20\nЗащита от угона\n•\n" +
            "Сигнализация\n•\nСигнализация с обратной связью\n•\nЦентральный замок\n•\n" +
            "Иммобилайзер\nМультимедиа\n•\nАудиоподготовка\n•\nАудиосистема\n•\nAUX\n•\n" +
            "Bluetooth\n•\nUSB\n•\nГолосовое управление\n•\nРозетка 12V\nСалон\n•\n" +
            "Кожа (Материал салона)\n•\nРегулировка сиденья водителя по высоте\n•\n" +
            "Регулировка передних сидений по высоте\n•\nЭлектрорегулировка сиденья водителя\n•\n" +
            "Электрорегулировка передних сидений\n•\nПодогрев передних сидений\n•\n" +
            "Подогрев задних сидений\n•\nОтделка кожей рулевого колеса\n•\nОтделка кожей рычага КПП\n•\n" +
            "Складывающееся заднее сиденье\n•\nТонированные стекла\n•\nНакладки на пороги\n" +
            "Комфорт\n•\nЭлектростеклоподъёмники передние\n•\nЭлектростеклоподъёмники задние\n•\n" +
            "Климат-контроль 1-зонный\n•\nУсилитель руля\n•\nРегулировка руля по высоте\n•\n" +
            "Круиз-контроль\n•\nПарктроник передний\n•\nПарктроник задний\n•\nБортовой компьютер\n•\n" +
            "Программируемый предпусковой отопитель\n•\nЭлектропривод зеркал\n•\n" +
            "Электроскладывание зеркал\n•\nПрикуриватель и пепельница\nБезопасность\n•\n" +
            "Подушка безопасности водителя\n•\nПодушка безопасности пассажира\n•\n" +
            "Подушки безопасности боковые\n•\nАнтипробуксовочная система (ASR)\n•\n" +
            "Антиблокировочная система (ABS)\n•\nСистема стабилизации (ESP)\n•\n" +
            "Блокировка замков задних дверей\nПрочее\n•\nПневмоподвеска\n•\nФаркоп\n•\n" +
            "Защита картера\nСкрыть";


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
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferCarsUsedUser",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText(complectationMinimized));
        basePageSteps.onCardPage().complectation().button("Все опции").click();
        basePageSteps.onCardPage().complectation().should(hasText(complectationMaximized));
        basePageSteps.onCardPage().complectation().button("Скрыть").click();
        basePageSteps.onCardPage().complectation().should(hasText(complectationMinimized));
    }
}
package ru.auto.tests.poffer.user;

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
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaComplectationBlock.SHOW_OPTIONS;
import static ru.auto.tests.desktop.element.poffer.beta.BetaOptionsCurtain.SEARCH;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.REFERENCE_CATALOG_CARS_SUGGEST_PATH;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - комплектация")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class ComplectationOptionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/ReferenceCatalogCarsParseOptions"),
                stub("desktop/ReferenceCatalogCarsAllOptions"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody()),

                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS_SUGGEST_PATH)
                        .withRequestQuery(getQuery())
                        .withResponseBody("poffer/beta/ReferenceCatalogCarsSuggestKiaOptima2017Response")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем опции когда нет комплектаций")
    public void shouldSelectEquipmentWhenEmptyComplectations() {
        mockRule.overwriteStub(4,
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS_SUGGEST_PATH)
                        .withRequestQuery(getQuery())
                        .withResponseBody("poffer/beta/ReferenceCatalogCarsSuggestKiaOptima2017WithoutComplectationResponse"));
        urlSteps.refresh();

        pofferSteps.onBetaPofferPage().equipmentBlock().button("USB").click();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody("drafts/user_equipment_usb.json")
        ));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должна открыться шторка с опциями")
    public void shouldOpenOptionsCurtain() {
        pofferSteps.onBetaPofferPage().complectationBlock().button(SHOW_OPTIONS).click();

        pofferSteps.onBetaPofferPage().optionCurtain().should(isDisplayed()).should(hasText("Комплектации\n" +
                "Luxe\nPrestige\nGT Line\nLuxe FCC 2017\nLuxe RED Line\nLuxe 2018 FWC\nДругая\nОпции\nПоиск\n" +
                "Обзор\nФары\nЭлектрообогрев\nДневные ходовые огни\nПротивотуманные фары\nАвтоматический " +
                "корректор фар\nОмыватель фар\nЭлементы экстерьера\nТип дисков\nРазмер дисков\nРейлинги " +
                "на крыше\nЗащита от угона\nСигнализация\nЦентральный замок\nИммобилайзер\nДатчик " +
                "проникновения в салон (датчик объема)\nМультимедиа\nАудиосистема\nМультимедиа система " +
                "с ЖК-экраном\nAUX\nBluetooth\nUSB\nНавигационная система\nРозетка 12V\nСалон\nКоличество " +
                "мест\nМатериал салона\nРегулировка сидений по высоте\nЭлектрорегулировка сидений\nСиденья " +
                "с поддержкой поясницы\nПодогрев сидений\nОбогрев рулевого колеса\nОтделка кожей рулевого " +
                "колеса\nОтделка кожей рычага КПП\nЛюк\nПанорамная крыша / лобовое стекло\nПередний центральный " +
                "подлокотник\nТретий ряд сидений\nТонированные стекла\nКомфорт\nЭлектростеклоподъёмники\n" +
                "Кондиционер\nУсилитель руля\nРегулировка руля\nКруиз-контроль\nСистема помощи при парковке\n" +
                "Бортовой компьютер\nСистема доступа без ключа\nЭлектропривод зеркал\nЭлектроскладывание зеркал\n" +
                "Мультифункциональное рулевое колесо\nСистема выбора режима движения\nБезопасность\nПодушки " +
                "безопасности\nСистема крепления Isofix\nВспомогательные системы\nАнтиблокировочная система (ABS)\n" +
                "Система стабилизации (ESP)\nПрочее\nЗапасное колесо\nФаркоп\nЗащита картера\nПоказать все опции\n" +
                "Сохранить"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны открыться все опции в шторке")
    public void shouldSeeAllOptionsInCurtain() {
        pofferSteps.onBetaPofferPage().complectationBlock().button(SHOW_OPTIONS).click();
        pofferSteps.onBetaPofferPage().optionCurtain().button("Показать все опции").click();

        pofferSteps.onBetaPofferPage().optionCurtain().should(hasText("Комплектации\nLuxe\nPrestige\nGT Line\n" +
                "Luxe FCC 2017\nLuxe RED Line\nLuxe 2018 FWC\nДругая\nОпции\nПоиск\nОбзор\nФары\nЭлектрообогрев\n" +
                "Дневные ходовые огни\nПротивотуманные фары\nАвтоматический корректор фар\nОмыватель фар\n" +
                "Система адаптивного освещения\nСистема управления дальним светом\nДатчик дождя\nДатчик света\n" +
                "Элементы экстерьера\nТип дисков\nРазмер дисков\nДвухцветная окраска кузова\nАэрография\n" +
                "Декоративные молдинги\nРейлинги на крыше\nЗащита от угона\nСигнализация\nЦентральный замок\n" +
                "Иммобилайзер\nДатчик проникновения в салон (датчик объема)\nМультимедиа\nАудиосистема\n" +
                "Мультимедиа система с ЖК-экраном\nAUX\nBluetooth\nUSB\nМультимедиа система для задних пассажиров\n" +
                "Навигационная система\nГолосовое управление\nAndroid Auto\nCarPlay\nЯндекс.Авто\nБеспроводная " +
                "зарядка для смартфона\nРозетка 12V\nРозетка 220V\nСалон\nКоличество мест\nМатериал салона\nЦвет " +
                "салона\nРегулировка сидений по высоте\nЭлектрорегулировка сидений\nСиденья с поддержкой поясницы\n" +
                "Подогрев сидений\nВентиляция сидений\nПамять положения сидений\nСпортивные передние сиденья\n" +
                "Сиденья с массажем\nОбогрев рулевого колеса\nОтделка кожей рулевого колеса\nОтделка кожей рычага " +
                "КПП\nЛюк\nПанорамная крыша / лобовое стекло\nОтделка потолка чёрного цвета\nПередний центральный " +
                "подлокотник\nТретий задний подголовник\nТретий ряд сидений\nСкладывающееся заднее сиденье\n" +
                "Функция складывания спинки сиденья пассажира\nСкладной столик на спинках передних сидений\n" +
                "Тонированные стекла\nСолнцезащитные шторки в задних дверях\nСолнцезащитная шторка на заднем " +
                "стекле\nДекоративная подсветка салона\nДекоративные накладки на педали\nНакладки на пороги\n" +
                "Комфорт\nЭлектростеклоподъёмники\nКондиционер\nУсилитель руля\nРегулировка руля\nКруиз-контроль\n" +
                "Система помощи при парковке\nКамера\nБортовой компьютер\nЭлектронная приборная панель\nПроекционный " +
                "дисплей\nСистема доступа без ключа\nЗапуск двигателя с кнопки\nСистема «старт-стоп»\nДистанционный " +
                "запуск двигателя\nПрограммируемый предпусковой отопитель\nЭлектропривод крышки багажника\nОткрытие " +
                "багажника без помощи рук\nЭлектропривод зеркал\nЭлектроскладывание зеркал\nМультифункциональное " +
                "рулевое колесо\nПодрулевые лепестки переключения передач\nОхлаждаемый перчаточный ящик\nРегулируемый " +
                "педальный узел\nДоводчик дверей\nПрикуриватель и пепельница\nСистема выбора режима движения\n" +
                "Безопасность\nПодушки безопасности\nСистема крепления Isofix\nВспомогательные системы\n" +
                "Антиблокировочная система (ABS)\nСистема стабилизации (ESP)\nДатчик давления в шинах\nБлокировка " +
                "замков задних дверей\nЭРА-ГЛОНАСС\nБронированный кузов\nПрочее\nПодвеска\nЗапасное колесо\nФаркоп\n" +
                "Защита картера\nПоказать только популярные\nСохранить"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("")
    public void shouldSearchOptions() {
        pofferSteps.onBetaPofferPage().complectationBlock().button(SHOW_OPTIONS).click();
        pofferSteps.onBetaPofferPage().optionCurtain().input(SEARCH, "USB");

        pofferSteps.onBetaPofferPage().optionCurtain().optionsList().should(hasSize(1));
        pofferSteps.onBetaPofferPage().optionCurtain().should(hasText("Комплектации\nLuxe\nPrestige\nGT Line\n" +
                "Luxe FCC 2017\nLuxe RED Line\nLuxe 2018 FWC\nДругая\nОпции\nМультимедиа\nUSB\n" +
                "Показать все опции\nСохранить"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем шторку с опциями")
    public void shouldCloseOptionsCurtain() {
        pofferSteps.onBetaPofferPage().complectationBlock().button(SHOW_OPTIONS).click();
        pofferSteps.onBetaPofferPage().optionCurtain().closeIcon().click();

        pofferSteps.onBetaPofferPage().optionCurtain().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбор комплектации")
    public void shouldSelectComplectation() {
        pofferSteps.selectComplectation();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody("drafts/user_complectation_luxe.json")
        ));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбор опций кастомно")
    public void shouldSelectCustomOption() {
        pofferSteps.onBetaPofferPage().complectationBlock().button(SHOW_OPTIONS).click();
        pofferSteps.selectOptions();
        waitSomething(3, TimeUnit.SECONDS);

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody("drafts/user_complectation_custom_select.json")
        ));
    }

    private Query getQuery() {
        return query()
                .setBodyType("SEDAN")
                .setEngineType("GASOLINE")
                .setGearType("FORWARD_CONTROL")
                .setMark("KIA")
                .setModel("OPTIMA")
                .setRid("225")
                .setSuperGenId("20526471")
                .setTechParamId("20757746")
                .setTransmission("AUTOMATIC")
                .setYear("2017");
    }
}

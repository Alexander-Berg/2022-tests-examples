package ru.auto.tests.desktop.group;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка - комплектации")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupComplectationsTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
        basePageSteps.onGroupPage().tab("Комплектации").click();
        basePageSteps.onGroupPage().complectations().radioButton("Сравнение").click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравнение комплектаций")
    public void shouldCompareComplectations() {
        basePageSteps.onGroupPage().complectations().waitUntil(hasText("Список\nСравнение\nПоказать только отличия\n" +
                "Лига Европы\nот 68 опций\nот 1 466 920 ₽\nClassic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\n" +
                "от 1 169 400 ₽\nEdition Plus\nот 55 опций\nот 1 319 900 ₽\nКомфорт\nКруиз-контроль\n" +
                "Мультифункциональное рулевое колесо\nЭлектропривод зеркал\nБортовой компьютер\nКлимат-контроль " +
                "1-зонный\nЭлектроскладывание зеркал\nКамера передняя\nКамера задняя\nАктивный усилитель руля\n" +
                "Подрулевые лепестки переключения передач\nЭлектростеклоподъёмники задние\nПарктроник передний\n" +
                "Парктроник задний\nЭлектростеклоподъёмники передние\nСистема доступа без ключа\nЗапуск двигателя с " +
                "кнопки\nРегулировка руля по вылету\nРегулировка руля по высоте\nКамера 360°\nУсилитель руля\n" +
                "Кондиционер\nСистема автоматической парковки\nОбзор\nЭлектрообогрев боковых зеркал\nАвтоматический " +
                "корректор фар\nСистема адаптивного освещения\nЭлектрообогрев зоны стеклоочистителей\nСветодиодные " +
                "фары\nДатчик света\nПротивотуманные фары\nДатчик дождя\nСалон\nДекоративная подсветка салона\nКожа " +
                "(Материал салона)\nПамять сиденья водителя\nВентиляция передних сидений\nСкладывающееся заднее сиденье\n" +
                "Отделка кожей рулевого колеса\nПередний центральный подлокотник\nПодогрев задних сидений\nОбогрев " +
                "рулевого колеса\nЭлектрорегулировка передних сидений\nОтделка кожей рычага КПП\nПодогрев передних " +
                "сидений\nТретий задний подголовник\nТкань (Материал салона)\nПамять передних сидений\nНакладки на " +
                "пороги\nДекоративные накладки на педали\nЛюк\nСолнцезащитные шторки в задних дверях\nПанорамная " +
                "крыша / лобовое стекло\nМультимедиа\nНавигационная система\nAndroid Auto\nUSB\nАудиоподготовка\n" +
                "CarPlay\nАудиосистема Hi-Fi\nAUX\nАудиосистема\nBluetooth\nРозетка 12V\nЭлементы экстерьера\nДиски 17\n" +
                "Металлик\nот 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\nДиски 16\nДиски 18\nОбвес кузова\n" +
                "Защита от угона\nЦентральный замок\nИммобилайзер\nБезопасность\nПодушка безопасности пассажира\n" +
                "Система помощи при торможении (BAS, EBD)\nЭРА-ГЛОНАСС\nПодушки безопасности боковые\nАнтиблокировочная " +
                "система (ABS)\nАнтипробуксовочная система (ASR)\nСистема стабилизации (ESP)\nПодушка безопасности для " +
                "защиты коленей водителя\nПодушка безопасности водителя\nКрепление детского кресла (задний ряд) ISOFIX\n" +
                "Система помощи при старте в гору (HSA)\nПодушки безопасности оконные (шторки)\nСистема контроля слепых " +
                "зон\nДатчик давления в шинах\nПрочее\nПолноразмерное запасное колесо"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по чекбоксу «Показать только отличия»")
    public void shouldClickShowOnlyDiffCheckbox() {
        basePageSteps.onGroupPage().complectations().checkbox("Показать только отличия").click();
        basePageSteps.onGroupPage().complectations().waitUntil(hasText("Список\nСравнение\nПоказать только отличия\nЛига " +
                "Европы\nот 68 опций\nот 1 466 920 ₽\nClassic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\n" +
                "от 1 169 400 ₽\nEdition Plus\nот 55 опций\nот 1 319 900 ₽\nКомфорт\nКлимат-контроль 1-зонный\nКамера " +
                "передняя\nКамера задняя\nАктивный усилитель руля\nПодрулевые лепестки переключения передач\nПарктроник " +
                "передний\nПарктроник задний\nСистема доступа без ключа\nЗапуск двигателя с кнопки\nКамера 360°\n" +
                "Усилитель руля\nКондиционер\nОбзор\nАвтоматический корректор фар\nСистема адаптивного освещения\n" +
                "Светодиодные фары\nПротивотуманные фары\nДатчик дождя\nСалон\nДекоративная подсветка салона\nКожа " +
                "(Материал салона)\nПамять сиденья водителя\nВентиляция передних сидений\nОтделка кожей рулевого " +
                "колеса\nПодогрев задних сидений\nОбогрев рулевого колеса\nЭлектрорегулировка передних сидений\nОтделка " +
                "кожей рычага КПП\nТкань (Материал салона)\nПамять передних сидений\nМультимедиа\nНавигационная " +
                "система\nAndroid Auto\nCarPlay\nАудиосистема Hi-Fi\nРозетка 12V\nЭлементы экстерьера\nДиски 17\nДиски " +
                "16\nДиски 18\nБезопасность\nСистема помощи при торможении (BAS, EBD)\nАнтипробуксовочная система (ASR)\n" +
                "Подушка безопасности для защиты коленей водителя\nСистема контроля слепых зон"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Скролл списка комплектаций")
    public void shouldScrollComplectations() {
        basePageSteps.onGroupPage().complectations().compare().nextButton().hover().click();
        basePageSteps.onGroupPage().complectations().compare().getComplectation(0)
                .waitUntil(hasText("Classic\nот 38 опций\nнет предложений"));
        basePageSteps.onGroupPage().complectations().waitUntil(hasText("Список\nСравнение\nПоказать только отличия\n" +
                "Classic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\nот 1 169 400 ₽\nEdition Plus\nот 55 опций\n" +
                "от 1 319 900 ₽\nGT\nот 68 опций\nот 1 779 900 ₽\nКомфорт\nКруиз-контроль\nМультифункциональное рулевое " +
                "колесо\nЭлектропривод зеркал\nБортовой компьютер\nКлимат-контроль 1-зонный\nЭлектроскладывание зеркал\n" +
                "Камера передняя\nКамера задняя\nАктивный усилитель руля\nПодрулевые лепестки переключения передач\n" +
                "Электростеклоподъёмники задние\nПарктроник передний\nПарктроник задний\nЭлектростеклоподъёмники " +
                "передние\nСистема доступа без ключа\nЗапуск двигателя с кнопки\nРегулировка руля по вылету\n" +
                "Регулировка руля по высоте\nКамера 360°\nУсилитель руля\nКондиционер\nСистема автоматической парковки\n" +
                "Обзор\nЭлектрообогрев боковых зеркал\nАвтоматический корректор фар\nСистема адаптивного освещения\n" +
                "Электрообогрев зоны стеклоочистителей\nСветодиодные фары\nДатчик света\nПротивотуманные фары\nДатчик " +
                "дождя\nСалон\nДекоративная подсветка салона\nКожа (Материал салона)\nПамять сиденья водителя\n" +
                "Вентиляция передних сидений\nСкладывающееся заднее сиденье\nОтделка кожей рулевого колеса\nПередний " +
                "центральный подлокотник\nПодогрев задних сидений\nОбогрев рулевого колеса\nЭлектрорегулировка передних " +
                "сидений\nОтделка кожей рычага КПП\nПодогрев передних сидений\nТретий задний подголовник\nТкань " +
                "(Материал салона)\nПамять передних сидений\nНакладки на пороги\nДекоративные накладки на педали\nЛюк\n" +
                "Солнцезащитные шторки в задних дверях\nПанорамная крыша / лобовое стекло\nМультимедиа\nНавигационная " +
                "система\nAndroid Auto\nUSB\nАудиоподготовка\nCarPlay\nАудиосистема Hi-Fi\nAUX\nАудиосистема\nBluetooth\n" +
                "Розетка 12V\nЭлементы экстерьера\nДиски 17\nМеталлик\nот 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\n" +
                "от 15 000 ₽\nДиски 16\nДиски 18\nОбвес кузова\nЗащита от угона\nЦентральный замок\nИммобилайзер\n" +
                "Безопасность\nПодушка безопасности пассажира\nСистема помощи при торможении (BAS, EBD)\nЭРА-ГЛОНАСС\n" +
                "Подушки безопасности боковые\nАнтиблокировочная система (ABS)\nАнтипробуксовочная система (ASR)\n" +
                "Система стабилизации (ESP)\nПодушка безопасности для защиты коленей водителя\nПодушка безопасности " +
                "водителя\nКрепление детского кресла (задний ряд) ISOFIX\nСистема помощи при старте в гору (HSA)\n" +
                "Подушки безопасности оконные (шторки)\nСистема контроля слепых зон\nДатчик давления в шинах\nПрочее\n" +
                "Полноразмерное запасное колесо"));

        basePageSteps.onGroupPage().complectations().compare().prevButton().hover().click();
        basePageSteps.onGroupPage().complectations().compare().getComplectation(0)
                .waitUntil(hasText("Лига Европы\nот 68 опций\nот 1 466 920 \u20BD"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Скрытие/раскрытие группы опций")
    public void shouldFoldAndUnfoldOptionsGroup() {
        basePageSteps.onGroupPage().complectations().compare().optionsGroup("Комфорт").click();
        basePageSteps.onGroupPage().complectations().waitUntil(hasText("Список\nСравнение\nПоказать только отличия\n" +
                "Лига Европы\nот 68 опций\nот 1 466 920 ₽\nClassic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\n" +
                "от 1 169 400 ₽\nEdition Plus\nот 55 опций\nот 1 319 900 ₽\nКомфорт\nОбзор\nЭлектрообогрев боковых " +
                "зеркал\nАвтоматический корректор фар\nСистема адаптивного освещения\nЭлектрообогрев зоны " +
                "стеклоочистителей\nСветодиодные фары\nДатчик света\nПротивотуманные фары\nДатчик дождя\nСалон\n" +
                "Декоративная подсветка салона\nКожа (Материал салона)\nПамять сиденья водителя\nВентиляция передних " +
                "сидений\nСкладывающееся заднее сиденье\nОтделка кожей рулевого колеса\nПередний центральный " +
                "подлокотник\nПодогрев задних сидений\nОбогрев рулевого колеса\nЭлектрорегулировка передних сидений\n" +
                "Отделка кожей рычага КПП\nПодогрев передних сидений\nТретий задний подголовник\nТкань " +
                "(Материал салона)\nПамять передних сидений\nНакладки на пороги\nДекоративные накладки на педали\n" +
                "Люк\nСолнцезащитные шторки в задних дверях\nПанорамная крыша / лобовое стекло\nМультимедиа\n" +
                "Навигационная система\nAndroid Auto\nUSB\nАудиоподготовка\nCarPlay\nАудиосистема Hi-Fi\nAUX\n" +
                "Аудиосистема\nBluetooth\nРозетка 12V\nЭлементы экстерьера\nДиски 17\nМеталлик\nот 15 000 ₽\n" +
                "от 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\nДиски 16\nДиски 18\nОбвес кузова\nЗащита от угона\nЦентральный " +
                "замок\nИммобилайзер\nБезопасность\nПодушка безопасности пассажира\nСистема помощи при торможении " +
                "(BAS, EBD)\nЭРА-ГЛОНАСС\nПодушки безопасности боковые\nАнтиблокировочная система (ABS)\n" +
                "Антипробуксовочная система (ASR)\nСистема стабилизации (ESP)\nПодушка безопасности для защиты коленей " +
                "водителя\nПодушка безопасности водителя\nКрепление детского кресла (задний ряд) ISOFIX\nСистема помощи " +
                "при старте в гору (HSA)\nПодушки безопасности оконные (шторки)\nСистема контроля слепых зон\nДатчик " +
                "давления в шинах\nПрочее\nПолноразмерное запасное колесо"));

        basePageSteps.onGroupPage().complectations().compare().optionsGroup("Прочее").click();
        basePageSteps.onGroupPage().complectations().waitUntil(hasText("Список\nСравнение\nПоказать только отличия\n" +
                "Лига Европы\nот 68 опций\nот 1 466 920 ₽\nClassic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\n" +
                "от 1 169 400 ₽\nEdition Plus\nот 55 опций\nот 1 319 900 ₽\nКомфорт\nОбзор\nЭлектрообогрев боковых " +
                "зеркал\nАвтоматический корректор фар\nСистема адаптивного освещения\nЭлектрообогрев зоны " +
                "стеклоочистителей\nСветодиодные фары\nДатчик света\nПротивотуманные фары\nДатчик дождя\nСалон\n" +
                "Декоративная подсветка салона\nКожа (Материал салона)\nПамять сиденья водителя\nВентиляция передних " +
                "сидений\nСкладывающееся заднее сиденье\nОтделка кожей рулевого колеса\nПередний центральный " +
                "подлокотник\nПодогрев задних сидений\nОбогрев рулевого колеса\nЭлектрорегулировка передних сидений\n" +
                "Отделка кожей рычага КПП\nПодогрев передних сидений\nТретий задний подголовник\nТкань (Материал салона)\n" +
                "Память передних сидений\nНакладки на пороги\nДекоративные накладки на педали\nЛюк\nСолнцезащитные " +
                "шторки в задних дверях\nПанорамная крыша / лобовое стекло\nМультимедиа\nНавигационная система\nAndroid " +
                "Auto\nUSB\nАудиоподготовка\nCarPlay\nАудиосистема Hi-Fi\nAUX\nАудиосистема\nBluetooth\nРозетка 12V\n" +
                "Элементы экстерьера\nДиски 17\nМеталлик\nот 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\nот 15 000 ₽\nДиски 16\n" +
                "Диски 18\nОбвес кузова\nЗащита от угона\nЦентральный замок\nИммобилайзер\nБезопасность\nПодушка " +
                "безопасности пассажира\nСистема помощи при торможении (BAS, EBD)\nЭРА-ГЛОНАСС\nПодушки безопасности " +
                "боковые\nАнтиблокировочная система (ABS)\nАнтипробуксовочная система (ASR)\nСистема стабилизации (ESP)\n" +
                "Подушка безопасности для защиты коленей водителя\nПодушка безопасности водителя\nКрепление детского " +
                "кресла (задний ряд) ISOFIX\nСистема помощи при старте в гору (HSA)\nПодушки безопасности оконные " +
                "(шторки)\nСистема контроля слепых зон\nДатчик давления в шинах\nПрочее"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Плавающий блок со списком комплектаций")
    public void shouldSeeFloatingComplectations() {
        basePageSteps.scrollDown(600);
        basePageSteps.onGroupPage().complectations().compare().floatingComplectations().waitUntil(isDisplayed());
    }
}
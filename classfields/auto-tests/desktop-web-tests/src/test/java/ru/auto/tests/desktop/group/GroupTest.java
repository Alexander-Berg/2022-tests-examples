package ru.auto.tests.desktop.group;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matchers;
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

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.OPTIONS;
import static ru.auto.tests.desktop.consts.Pages.TECH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupTest {

    private static final String OFFER_ID = "/21342050-21342121/";
    private static final int PAGE_SIZE = 10;
    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String GENERATION = "21342050";
    private static final String CONFIGURATION = "21342121";
    private static final String TECH_PARAM = "21342125";

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
                "desktop/SearchCarsGroupContextGroupPage2",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/SearchCarsEquipmentFiltersKiaOptima",
                "desktop/OfferCarsPhones",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(OFFER_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение заголовка группы")
    public void shouldSeeGroupHeader() {
        basePageSteps.onGroupPage().groupHeader().should(hasText("Kia Optima IV Рестайлинг\nНовый\nСедан\n" +
                "от 1 169 400 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение предложений")
    public void shouldSeeGroupOffers() {
        basePageSteps.onGroupPage().groupOffers().title().should(hasText("62 предложения от 1 169 400 до 2 069 900 ₽"));
        basePageSteps.onGroupPage().groupOffersList().should(Matchers.hasSize(10));
        basePageSteps.onGroupPage().getOffer(0).should(hasText("Prestige\nВ наличии\n2019\n2.0 л / 150 л.с. / Бензин\n" +
                "Автомат\nПередний\nСиний\n58 базовых опций\n1 837 210 ₽\nО скидках и акциях узнавайте по телефону\n" +
                "Подробнее о предложении\nПротивотуманные фары\nСистема адаптивного освещения\nДатчик дождя\n+55 опций\n" +
                "АвтоГЕРМЕС KIA Рябиновая\nМоскваРябиновая улица, 43Б\nКупить в трейд-ин\nПоказать контакты"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickShowMoreOffersButton() {
        basePageSteps.onGroupPage().groupOffersList().should(hasSize(PAGE_SIZE));
        basePageSteps.onGroupPage().pager().button("Показать ещё").click();
        basePageSteps.onGroupPage().groupOffersList().waitUntil(hasSize(PAGE_SIZE * 2));
        basePageSteps.onGroupPage().pager().currentPage().waitUntil(hasText("2"));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбор комплектации во вкладке «Комплектации»")
    public void shouldSelectComplectation() {
        basePageSteps.onGroupPage().tab("Комплектации").click();
        basePageSteps.onGroupPage().complectation("Edition Plus").click();
        basePageSteps.onGroupPage().selectedComplectation().should(hasText("Edition Plus"));
        basePageSteps.onGroupPage().complectations().options().should(hasText("Комфорт18 опций\nКамера 360°\n" +
                "Камера задняя\nКруиз-контроль\nПарктроник задний\nБортовой компьютер\nПарктроник передний\n" +
                "Электропривод зеркал\nАктивный усилитель руля\nКлимат-контроль 1-зонный\nЭлектроскладывание зеркал\n" +
                "Система доступа без ключа\nЗапуск двигателя с кнопки\nРегулировка руля по вылету\n" +
                "Регулировка руля по высоте\nЭлектростеклоподъёмники задние\nЭлектростеклоподъёмники передние\n" +
                "Мультифункциональное рулевое колесо\nПодрулевые лепестки переключения передач\nОбзор7 опций\n" +
                "Датчик света\nДатчик дождя\nСветодиодные фары\nПротивотуманные фары\nЭлектрообогрев боковых зеркал\n" +
                "Система адаптивного освещения\nЭлектрообогрев зоны стеклоочистителей\nСалон10 опций\n" +
                "Кожа (Материал салона)\nПамять передних сидений\nОбогрев рулевого колеса\nОтделка кожей рычага КПП\n" +
                "Подогрев передних сидений\nТретий задний подголовник\nСкладывающееся заднее сиденье\nОтделка кожей " +
                "рулевого колеса\nПередний центральный подлокотник\nЭлектрорегулировка передних сидений\n" +
                "Мультимедиа6 опций\nUSB\nAUX\nBluetooth\nАудиосистема\nАудиоподготовка\nНавигационная система\n" +
                "Элементы экстерьера2 опции\nДиски 18\nМогут быть установлены дополнительно\nМеталлик\n" +
                "Защита от угона2 опции\nИммобилайзер\nЦентральный замок\nБезопасность10 опций\nЭРА-ГЛОНАСС\n" +
                "Датчик давления в шинах\nСистема стабилизации (ESP)\nПодушки безопасности боковые\n" +
                "Подушка безопасности водителя\nПодушка безопасности пассажира\nАнтиблокировочная система (ABS)\n" +
                "Подушки безопасности оконные (шторки)\nСистема помощи при старте в гору (HSA)\nКрепление детского " +
                "кресла (задний ряд) ISOFIX\nПрочее1 опция\nПолноразмерное запасное колесо"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Характеристики»")
    public void shouldClickSpecificationsTab() {
        basePageSteps.onGroupPage().tab("Характеристики").click();
        urlSteps.path(TECH).addParam("catalog_filter", format("mark=%s,model=%s,generation=%s,configuration=%s,tech_param=%s",
                MARK.toUpperCase(), MODEL.toUpperCase(), GENERATION, CONFIGURATION, TECH_PARAM)).shouldNotSeeDiff();
        basePageSteps.onGroupPage().specifications().should(hasText("Объем\n2.0 л\nМощность\n150 л.с.\n" +
                "Коробка\nАвтомат\nТип двигателя\nБензин\nТопливо\nАИ-95\nПривод\nпередний\nРазгон\n10.7 с\n" +
                "Расход\n7.8 л\nКолёсная база\n2805 мм\nШирина передней колеи\n1594 мм\nШирина задней колеи\n1595 мм\n" +
                "Размер колёс\n215/60/R16, 215/55/R17\nВысота\n1485 мм\nКлиренс\n155 мм\nДлина\n4855 мм\n" +
                "Ширина\n1860 мм\nОбщая информация\nСтрана марки\nЮжная Корея\nКласс автомобиля\nD\n" +
                "Количество дверей\n4\nКоличество мест\n5\nРасположение руля\nЛевый\nРазмеры\nДлина\n4855 мм\n" +
                "Ширина\n1860 мм\nВысота\n1485 мм\nКолёсная база\n2805 мм\nКлиренс\n155 мм\n" +
                "Ширина передней колеи\n1594 мм\nШирина задней колеи\n1595 мм\nРазмер колёс\n215/60/R16, 215/55/R17\n" +
                "Подвеска и тормоза\nТип передней подвески\nнезависимая, пружинная\n" +
                "Тип задней подвески\nнезависимая, пружинная\nПередние тормоза\nдисковые вентилируемые\n" +
                "Задние тормоза\nдисковые\nЭксплуатационные показатели\nМаксимальная скорость\n202 км/ч\n" +
                "Разгон до 100 км/ч\n10.7 с\nРасход топлива город/трасса/смешанный\n11.2/5.8/7.8 л\n" +
                "Марка топлива\nАИ-95\nЭкологический класс\nEuro 5\nВыбросы CO2\n182 г/км\nОбъём и масса\n" +
                "Объем багажника мин/макс\n510 л\nОбъём топливного бака\n70 л\nСнаряженная масса\n1545 кг\n" +
                "Полная масса\n2020 кг\nТрансмиссия\nКоробка передач\nАвтомат\nКоличество передач\n6\n" +
                "Тип привода\nпередний\nДвигатель\nТип двигателя\nБензин\nРасположение двигателя\n" +
                "переднее, поперечное\nОбъем двигателя\n1999 см³\nТип наддува\nнет\n" +
                "Максимальная мощность\n150/110 л.с./кВт при 6500 об/мин\nМаксимальный крутящий момент\n" +
                "196 Н*м при 4800 об/мин\nРасположение цилиндров\nрядное\nКоличество цилиндров\n4\n" +
                "Число клапанов на цилиндр\n4\nСистема питания двигателя\nраспределенный впрыск (многоточечный)\n" +
                "Степень сжатия\n10.3\nДиаметр цилиндра и ход поршня\n81.0x97.0 мм"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Комплектации»")
    public void shouldClickComplectationsTab() {
        basePageSteps.onGroupPage().tab("Комплектации").click();
        urlSteps.path(OPTIONS).shouldNotSeeDiff();
        basePageSteps.onGroupPage().complectations().should(hasText("Список\nСравнение\nЛига Европы\nот 68 опций\n" +
                "от 1 534 900 ₽\nClassic\nот 38 опций\nнет предложений\nComfort\nот 44 опций\nот 1 169 400 ₽\nEdition " +
                "Plus\nот 55 опций\nот 1 319 900 ₽\nКомфорт19 опций\nКамера 360°\nКамера задняя\nКруиз-контроль\n" +
                "Камера передняя\nПарктроник задний\nБортовой компьютер\nПарктроник передний\nЭлектропривод зеркал\n" +
                "Активный усилитель руля\nКлимат-контроль 1-зонный\nЭлектроскладывание зеркал\nСистема доступа без " +
                "ключа\nЗапуск двигателя с кнопки\nРегулировка руля по вылету\nРегулировка руля по высоте\n" +
                "Электростеклоподъёмники задние\nЭлектростеклоподъёмники передние\nМультифункциональное рулевое " +
                "колесо\nПодрулевые лепестки переключения передач\nОбзор8 опций\nДатчик света\nДатчик дождя\n" +
                "Светодиодные фары\nПротивотуманные фары\nАвтоматический корректор фар\nЭлектрообогрев боковых " +
                "зеркал\nСистема адаптивного освещения\nЭлектрообогрев зоны стеклоочистителей\nСалон13 опций\nКожа " +
                "(Материал салона)\nПамять сиденья водителя\nПодогрев задних сидений\nОбогрев рулевого колеса\n" +
                "Отделка кожей рычага КПП\nПодогрев передних сидений\nТретий задний подголовник\nВентиляция передних " +
                "сидений\nДекоративная подсветка салона\nСкладывающееся заднее сиденье\nОтделка кожей рулевого " +
                "колеса\nПередний центральный подлокотник\nЭлектрорегулировка передних сидений\nМультимедиа10 опций\n" +
                "USB\nAUX\nCarPlay\nBluetooth\nРозетка 12V\nAndroid Auto\nАудиосистема\nАудиоподготовка\n" +
                "Аудиосистема Hi-Fi\nНавигационная система\nЭлементы экстерьера2 опции\nДиски 17\nМогут быть " +
                "установлены дополнительно\nМеталлик\nЗащита от угона2 опции\nИммобилайзер\nЦентральный замок\n" +
                "Безопасность14 опций\nЭРА-ГЛОНАСС\nДатчик давления в шинах\nСистема стабилизации (ESP)\nСистема " +
                "контроля слепых зон\nПодушки безопасности боковые\nПодушка безопасности водителя\nПодушка " +
                "безопасности пассажира\nАнтиблокировочная система (ABS)\nАнтипробуксовочная система (ASR)\nПодушки " +
                "безопасности оконные (шторки)\nСистема помощи при старте в гору (HSA)\nСистема помощи при торможении " +
                "(BAS, EBD)\nКрепление детского кресла (задний ряд) ISOFIX\nПодушка безопасности для защиты коленей " +
                "водителя\nПрочее1 опция\nПолноразмерное запасное колесо"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на сравнение комплектаций")
    public void shouldClickComplectationsCompareUrl() {
        basePageSteps.onGroupPage().groupOffers().filters().select("Комплектация и опции").click();
        basePageSteps.onGroupPage().groupComplectationsPopup().button("Сравнение комплектаций").click();
        basePageSteps.onGroupPage().groupComplectationsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().complectations().radioButtonSelected("Сравнение").waitUntil(isDisplayed());
        basePageSteps.onGroupPage().complectations().compare().waitUntil(isDisplayed());
    }
}
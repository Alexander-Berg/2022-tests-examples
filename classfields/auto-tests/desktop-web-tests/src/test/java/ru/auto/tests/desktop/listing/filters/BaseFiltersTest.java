package ru.auto.tests.desktop.listing.filters;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.AUTOLOADER;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

//import io.qameta.allure.Parameter;

@DisplayName("Базовые фильтры поиска - отображение")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    //@Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String filterText;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Все\nНовые\nС пробегом\nВ кредит\nСохранить поиск\nМарка\nМодель\nПоколение\nКузов\nКоробка\nДвигатель\nПривод\nОбъем от, л\nдо\nГод от\nдо\nПробег от, км\nдо\nЦена от, ₽\nдо\nВсе параметры\n"},
                {CARS, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nКузов\nКоробка\nДвигатель\nПривод\nОбъем от, л\nдо\nГод от\nдо\nМощность от, л.с.\nдо\nЦена от, ₽\nдо\nВсе параметры\n"},
                {CARS, USED, "Все\nНовые\nС пробегом\nВ кредит\nСохранить поиск\nМарка\nМодель\nПоколение\nКузов\nКоробка\nДвигатель\nПривод\nОбъем от, л\nдо\nГод от\nдо\nПробег от, км\nдо\nЦена от, ₽\nдо\nВсе параметры\n"},

                {MOTORCYCLE, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип мотоцикла\nГод от\nдо\nЦена от, ₽\nдо\nОбъем от, см³\nдо\nВсе параметры\n"},
                {MOTORCYCLE, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип мотоцикла\nГод от\nдо\nЦена от, ₽\nдо\nОбъем от, см³\nдо\nВсе параметры\n"},
                {MOTORCYCLE, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип мотоцикла\nГод от\nдо\nЦена от, ₽\nдо\nОбъем от, см³\nдо\nВсе параметры\n"},

                {SCOOTERS, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЧисло тактов\nЦена от, ₽\nдо\nОбъем от, см³\nдо\nДвигатель\nПробег от, км\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {SCOOTERS, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЧисло тактов\nЦена от, ₽\nдо\nОбъем от, см³\nдо\nДвигатель\nПробег от, км\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},

                {ATV, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип вездехода\nГод от\nдо\nЦена от, ₽\nдо\nПробег от, км\nдо\nВсе параметры\n"},
                {ATV, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип вездехода\nГод от\nдо\nЦена от, ₽\nдо\nВсе параметры\n"},
                {ATV, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип вездехода\nГод от\nдо\nЦена от, ₽\nдо\nПробег от, км\nдо\nВсе параметры\n"},

                {SNOWMOBILE, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип снегохода\nГод от\nдо\nЦена от, ₽\nдо\nПробег от, км\nдо\nВсе параметры\n"},
                {SNOWMOBILE, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип снегохода\nГод от\nдо\nЦена от, ₽\nдо\nВсе параметры\n"},
                {SNOWMOBILE, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nТип снегохода\nГод от\nдо\nЦена от, ₽\nдо\nПробег от, км\nдо\nВсе параметры\n"},

                {LCV, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 1 т.\n1-1,5 т.\nот 1,5 т.\nЦена от, ₽\nдо\nТип кузова\nПривод\nКоробка\nДвигатель\nГод от\nдо\nЧисло мест от\nдо\nВсе параметры\n"},
                {LCV, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 1 т.\n1-1,5 т.\nот 1,5 т.\nЦена от, ₽\nдо\nТип кузова\nПривод\nКоробка\nДвигатель\nГод от\nдо\nЧисло мест от\nдо\nВсе параметры\n"},
                {LCV, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 1 т.\n1-1,5 т.\nот 1,5 т.\nЦена от, ₽\nдо\nТип кузова\nПривод\nКоробка\nДвигатель\nГод от\nдо\nЧисло мест от\nдо\nВсе параметры\n"},

                {TRUCK, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nЦена от, ₽\nдо\nТип кузова\nТип кабины\nДвигатель\nКоробка\nГод от\nдо\nКолёсн. ф-ла\nШасси\nВсе параметры\n"},
                {TRUCK, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nЦена от, ₽\nдо\nТип кузова\nТип кабины\nДвигатель\nКоробка\nГод от\nдо\nКолёсн. ф-ла\nШасси\nВсе параметры\n"},
                {TRUCK, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nЦена от, ₽\nдо\nТип кузова\nТип кабины\nДвигатель\nКоробка\nГод от\nдо\nКолёсн. ф-ла\nШасси\nВсе параметры\n"},

                {ARTIC, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nКолёсн. ф-ла\nТип кабины\nДвигатель\nКоробка\nВысота седельного устройства\nПодвеска кабины\nВсе параметры\n"},
                {ARTIC, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nКолёсн. ф-ла\nТип кабины\nДвигатель\nКоробка\nВысота седельного устройства\nПодвеска кабины\nВсе параметры\n"},
                {ARTIC, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nКолёсн. ф-ла\nТип кабины\nДвигатель\nКоробка\nВысота седельного устройства\nПодвеска кабины\nВсе параметры\n"},

                {BUS, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип автобуса\nЧисло мест от\nВсе параметры\n"},
                {BUS, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип автобуса\nЧисло мест от\nВсе параметры\n"},
                {BUS, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип автобуса\nЧисло мест от\nВсе параметры\n"},

                {TRAILER, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип подвески\nТормоза\nТип прицепа\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nКол-во осей от\nдо\nВсе параметры\n"},
                {TRAILER, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип подвески\nТормоза\nТип прицепа\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nКол-во осей от\nдо\nВсе параметры\n"},
                {TRAILER, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nТип подвески\nТормоза\nТип прицепа\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nКол-во осей от\nдо\nВсе параметры\n"},

                {AGRICULTURAL, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {AGRICULTURAL, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {AGRICULTURAL, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},

                {CONSTRUCTION, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nВсе параметры\n"},
                {CONSTRUCTION, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nВсе параметры\n"},
                {CONSTRUCTION, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип техники\nВсе параметры\n"},

                {AUTOLOADER, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип автопогрузчика\nПодъем от, м\nдо\nВсе параметры\n"},
                {AUTOLOADER, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип автопогрузчика\nПодъем от, м\nдо\nВсе параметры\n"},
                {AUTOLOADER, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип автопогрузчика\nПодъем от, м\nдо\nВсе параметры\n"},

                {CRANE, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nПробег от, км\nдо\nОбъем от, л\nдо\nВсе параметры\n"},
                {CRANE, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nОбъем от, л\nдо\nВсе параметры\n"},
                {CRANE, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nдо 3,5 т.\n3,5-12 т.\nот 12 т.\nПробег от, км\nдо\nОбъем от, л\nдо\nВсе параметры\n"},

                {DREDGE, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип экскаватора\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {DREDGE, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип экскаватора\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {DREDGE, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип экскаватора\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},

                {BULLDOZERS, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип\nТяговый класс\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {BULLDOZERS, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип\nТяговый класс\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},
                {BULLDOZERS, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМоточасы от\nдо\nТип\nТяговый класс\nОбъем от, л\nдо\nМощность от, л.с.\nдо\nВсе параметры\n"},

                {MUNICIPAL, ALL, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМощность от, л.с.\nдо\nТип техники\nДвигатель\nВсе параметры\n"},
                {MUNICIPAL, NEW, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМощность от, л.с.\nдо\nТип техники\nДвигатель\nВсе параметры\n"},
                {MUNICIPAL, USED, "Все\nНовые\nС пробегом\nСохранить поиск\nМарка\nМодель\nГод от\nдо\nЦена от, ₽\nдо\nМощность от, л.с.\nдо\nТип техники\nДвигатель\nВсе параметры\n"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение фильтров")
    public void shouldSeeFilters() {
        basePageSteps.onListingPage().filter()
                .should(hasText(matchesPattern(format("%s((Показать .+ предложени[е,я,й])|(Ничего не найдено))",
                        filterText))));
    }
}
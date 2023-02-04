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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Regions.ANY_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Групповая карточка (электро)")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupElectroTest {

    private static final String PATH = "/audi/e_tron/21447469-21447519/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain("gids", ANY_GEO_ID);

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по вкладке «Характеристики»")
    public void shouldClickSpecificationsTab() {
        basePageSteps.onGroupPage().tab("Характеристики").click();
        basePageSteps.onGroupPage().specifications().should(hasText("Мощность\n408 л.с.\nКоробка\nАвтомат\n" +
                "Тип двигателя\nЭлектро\nПривод\nполный\nРазгон\n5.7 с\nЗапас хода на электричестве\n436 км\n" +
                "Колёсная база\n2928 мм\nШирина передней колеи\n1655 мм\nШирина задней колеи\n1652 мм\n" +
                "Размер колёс\n255/55/R19, 255/50/R20, 265/45/R21\nВысота\n1629 мм\nКлиренс\n175 мм\nДлина\n4901 мм\n" +
                "Ширина\n1935 мм\nОбщая информация\nСтрана марки\nГермания\nКласс автомобиля\nJ\nКоличество дверей\n5\n" +
                "Количество мест\n5\nРасположение руля\nЛевый\nРазмеры\nДлина\n4901 мм\nШирина\n1935 мм\n" +
                "Высота\n1629 мм\nКолёсная база\n2928 мм\nКлиренс\n175 мм\nШирина передней колеи\n1655 мм\n" +
                "Ширина задней колеи\n1652 мм\nРазмер колёс\n255/55/R19, 255/50/R20, 265/45/R21\nПодвеска и тормоза\n" +
                "Тип передней подвески\nнезависимая, пневмоэлемент\nТип задней подвески\nнезависимая, пневмоэлемент\n" +
                "Передние тормоза\nдисковые вентилируемые\nЗадние тормоза\nдисковые вентилируемые\n" +
                "Эксплуатационные показатели\nМаксимальная скорость\n200 км/ч\nРазгон до 100 км/ч\n5.7 с\n" +
                "Объём и масса\nОбъем багажника мин/макс\n660 л\nСнаряженная масса\n2555 кг\nПолная масса\n3130 кг\n" +
                "Трансмиссия\nКоробка передач\nАвтомат\nКоличество передач\n1\nТип привода\nполный\nДвигатель\n" +
                "Тип двигателя\nЭлектро\nМаксимальная мощность\n408/300 л.с./кВт при об/мин\n" +
                "Максимальный крутящий момент\n664 Н*м при об/мин\nАккумуляторная батарея\n" +
                "Запас хода на электричестве\n436 км\nВремя зарядки\n46.0 ч\nЕмкость батареи\n95.0 кВт⋅ч"));
    }
}
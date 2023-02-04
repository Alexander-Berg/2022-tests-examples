package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка кузова - характеристики")
@Feature(AutoruFeatures.CATALOG)
public class BodySpecificationsElectroTest {

    private static final String MARK = "tesla";
    private static final String MODEL = "model_x";
    private static final String GENERATION = "20697853";
    private static final String BODY = "20697906";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY)
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Характеристики")
    @Category({Regression.class})
    public void shouldSeeSpecifications() {
        basePageSteps.onCatalogPage().complectationDescription().column("2").should(hasText("Запас хода\n" +
                "489 км\nМощность\n518 л.с.\nКоробка\nавтомат\nТип двигателя\nэлектро\nПривод\nполный\nРазгон\n5 с"));
        basePageSteps.onCatalogPage().complectationDescription().column("5").should(hasText("Общая информация\n" +
                "Страна марки\nСША\nКласс автомобиля\nJ\nКоличество дверей\n5\nКоличество мест\n7\nРазмеры, мм\n" +
                "Длина\n5004\nШирина\n2083\nВысота\n1626\nКолёсная база\n3061\nКлиренс\n180\nОбъём и масса\nОбъем " +
                "багажника мин/макс, л\n-\nСнаряженная масса, кг\n2389\nПолная масса, кг\n3020\nТрансмиссия\nКоробка " +
                "передач\nавтомат\nТип привода\nполный\nПодвеска и тормоза\nТип передней подвески\nнезависимая, " +
                "пружинная\nТип задней подвески\nнезависимая, пружинная\nПередние тормоза\nдисковые вентилируемые\n" +
                "Задние тормоза\nдисковые вентилируемые"));
        basePageSteps.onCatalogPage().complectationDescription().column("6").should(hasText("Эксплуатационные " +
                "показатели\nМаксимальная скорость, км/ч\n250\nРазгон до 100 км/ч, с\n5\nДвигатель\nТип двигателя\n" +
                "электро\nМаксимальная мощность, л.с./кВт при об/мин\n518 / 381\nМаксимальный крутящий момент, Н*м " +
                "при об/мин\n930\nКоличество цилиндров\n0\nДиаметр цилиндра и ход поршня, мм\n-\nАккумуляторная " +
                "батарея\nЗапас хода на электричестве, км\n489\nЕмкость батареи, кВт⋅ч\n90\nВремя зарядки, ч\n27.1"));
    }
}
package ru.auto.tests.desktop.footer;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Regions.ANY_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Футер")
@Feature(FOOTER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FooterTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение футера в Москве")
    public void shouldSeeFooterInMoscow() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().footer().should(hasText("Разместить рекламу\nДилерам\nПомощь\nСтань частью команды\n" +
                "О проекте\nАналитика Авто.ру\nСаша Котов\nМосква и Московская область\nМосква\nСанкт-Петербург\n" +
                "Владимир\nВолгоград\nВоронеж\nЕкатеринбург\nИваново\nКазань\nКалуга\nКострома\nКраснодар\nКрасноярск\n" +
                "Нижний Новгород\nНовосибирск\nОмск\nПермь\nРостов-на-Дону\nСамара\nСаратов\nТверь\nТула\nУфа\n" +
                "Челябинск\nЯрославль\nАвто.ру Москва — один из самых посещаемых автомобильных сайтов в российском " +
                "интернетеМы предлагаем большой выбор легковых автомобилей, грузового и коммерческого транспорта, " +
                "мототехники, спецтехники и многих других видов транспортных средств\n" +
                "© 1996–2022 ООО «Яндекс.Вертикали»\nПользовательское соглашение"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение футера в регионе «Любой»")
    public void shouldSeeFooterInAnyRegion() {
        urlSteps.testing().addParam("geo_id", ANY_GEO_ID).open();
        basePageSteps.onBasePage().footer().should(hasText("Разместить рекламу\nДилерам\nПомощь\nСтань частью команды\n" +
                "О проекте\nАналитика Авто.ру\nСаша Котов\nМосква\nСанкт-Петербург\nВладимир\nВолгоград\nВоронеж\n" +
                "Екатеринбург\nИваново\nКазань\nКалуга\nКострома\nКраснодар\nКрасноярск\nНижний Новгород\nНовосибирск\n" +
                "Омск\nПермь\nРостов-на-Дону\nСамара\nСаратов\nТверь\nТула\nУфа\nЧелябинск\nЯрославль\nАлтайский край\n" +
                "Амурская область\nАрхангельская область\nАстраханская область\nБелгородская область\n" +
                "Брянская область\nВладимирская область\nВолгоградская область\nВологодская область\n" +
                "Воронежская область\nЕврейская автономная область\nЗабайкальский край\nИвановская область\n" +
                "Иркутская область\nКабардино-Балкарская Республика\nКалининградская область\nКалужская область\n" +
                "Камчатский край\nКарачаево-Черкесская Республика\nКемеровская область\nКировская область\n" +
                "Костромская область\nКраснодарский край\nКрасноярский край\nКурганская область\nКурская область\n" +
                "Липецкая область\nМагаданская область\nМосква и Московская область\nМурманская область\n" +
                "Ненецкий автономный округ\nНижегородская область\nНовгородская область\nНовосибирская область\n" +
                "Омская область\nОренбургская область\nОрловская область\nПензенская область\nПермский край\n" +
                "Приморский край\nПсковская область\nРеспублика Адыгея\nРеспублика Алтай\nРеспублика Башкортостан\n" +
                "Республика Бурятия\nРеспублика Дагестан\nРеспублика Ингушетия\nРеспублика Калмыкия\n" +
                "Республика Карелия\nРеспублика Коми\nРеспублика Крым\nРеспублика Марий Эл\nРеспублика Мордовия\n" +
                "Республика Саха (Якутия)\nРеспублика Северная Осетия — Алания\nРеспублика Татарстан\n" +
                "Республика Тыва\nРеспублика Хакасия\nРостовская область\nРязанская область\nСамарская область\n" +
                "Санкт-Петербург и Ленинградская область\nСаратовская область\nСахалинская область\n" +
                "Свердловская область\nСмоленская область\nСтавропольский край\nТамбовская область\nТверская область\n" +
                "Томская область\nТульская область\nТюменская область\nУдмуртская Республика\nУльяновская область\n" +
                "Хабаровский край\nХанты-Мансийский автономный округ - Югра\nЧелябинская область\n" +
                "Чеченская Республика\nЧувашская Республика\nЧукотский автономный округ\n" +
                "Ямало-Ненецкий автономный округ\nЯрославская область\nАвто.ру — один из самых посещаемых автомобильных " +
                "сайтов в российском интернетеМы предлагаем большой выбор легковых автомобилей, " +
                "грузового и коммерческого транспорта, мототехники, спецтехники и многих других видов " +
                "транспортных средств\n© 1996–2022 ООО «Яндекс.Вертикали»\nПользовательское соглашение"));
    }
}

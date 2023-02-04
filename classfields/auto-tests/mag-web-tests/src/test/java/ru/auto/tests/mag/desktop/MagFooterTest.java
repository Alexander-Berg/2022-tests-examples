package ru.auto.tests.mag.desktop;

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
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер в журнале")
@Feature(FOOTER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagFooterTest {

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
        urlSteps.subdomain(SUBDOMAIN_MAG).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение футера")
    public void shouldSeeFooter() {
        basePageSteps.onMagPage().footer().waitUntil(isDisplayed()).should(hasText("Разместить рекламу\nДилерам\n" +
                "Помощь\nСтань частью команды\nО проекте\nАналитика Авто.ру\nСаша Котов\nМосква и Московская область\nМосква\n" +
                "Санкт-Петербург\nВладимир\nВолгоград\nВоронеж\nЕкатеринбург\nИваново\nКазань\nКалуга\nКострома\n" +
                "Краснодар\nКрасноярск\nНижний Новгород\nНовосибирск\nОмск\nПермь\nРостов-на-Дону\nСамара\nСаратов\n" +
                "Тверь\nТула\nУфа\nЧелябинск\nЯрославль\nАвто.ру Москва — один из самых посещаемых автомобильных сайтов " +
                "в российском интернетеМы предлагаем большой выбор легковых автомобилей, грузового и коммерческого " +
                "транспорта, мототехники, спецтехники и многих других видов транспортных средств\n" +
                "© 1996–2022 ООО «Яндекс.Вертикали»\nПользовательское соглашение"));
    }
}

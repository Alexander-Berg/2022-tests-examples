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
import ru.auto.tests.desktop.categories.Screenshooter;
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

@DisplayName("Групповая карточка - предложение")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupOfferTest {

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
                "desktop/OfferCarsPhones",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать контакты»")
    public void shouldClickShowContactsButton() {
        basePageSteps.onGroupPage().getOffer(0).showContactsButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("АвтоГЕРМЕС KIA Рябиновая\n" +
                "Официальный дилер Kia\nПодписаться\n+7 916 039-84-27\nРоман\nc 10:00 до 23:00\n+7 916 039-84-28\n" +
                "Дмитрий\nc 12:00 до 20:00\nМоскваРябиновая улица, 43Б\nKia Optima IV Рестайлинг • 1 837 210 ₽\n" +
                "2.0 л / 150 л.с. / Бензин\nавтомат\nседан\nпередний\nPrestige\n56 опций\nЗаметка об этом автомобиле " +
                "(её увидите только вы)"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onGroupPage().getOffer(0), 0, 100);
        basePageSteps.onGroupPage().getOffer(0).price().hover();
        basePageSteps.onGroupPage().getOffer(0).pricePopup().waitUntil(isDisplayed())
                .should(hasText("1 837 210 \u20BD\nО скидках и акциях узнавайте по телефону\n29 744 $\n26 820 €"));
    }
}
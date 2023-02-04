package ru.auto.tests.desktop.gallery;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@Feature(SALES)
@DisplayName("Объявление - контакты в галерее")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GalleryContactsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;


    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String state;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, USED}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUserWithAllowedSafeDeal",
                "desktop/OfferCarsPhones").post();

        basePageSteps.setWideWindowSize(3000);
        urlSteps.testing().path(CARS).path(state).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().gallery().currentImage().should(isDisplayed()).click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов в галерее")
    public void shouldSeeContactsInGallery() {
        basePageSteps.onCardPage().fullScreenGallery().contacts().should(hasText("700 000 ₽\n" +
                "Land Rover Discovery III, 2008 г, 210 000 км\nАвто в отличном состоянии\nДоводчики, камера 360\n" +
                "Панорама, вентиляция\nПоказать телефон\n+7 ●●● ●●● ●● ●●\nНаписать"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().fullScreenGallery().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("Федор\nЧастное лицо\n" +
                "+7 916 039-84-27\nc 10:00 до 23:00\n+7 916 039-84-28\nc 12:00 до 20:00\nПредложите Безопасную сделку\n" +
                "Мы пересмотрели сделку купли-продажи автомобиля, разложили её по полочкам и перенесли в онлайн для " +
                "вашего удобства и безопасности. Подробнее\nНомер защищён от спама\n" +
                "SMS и сообщения в мессенджерах доставлены не будут, звоните.\nТолько частникам\nВладелец просит " +
                "автосалоны и перепродавцов машин не беспокоить.\nВнимание\nПриобретая ТС, никогда не отправляйте " +
                "предоплату.Подробнее\nМоскваметро Марьино\nLand Rover Discovery III • 700 000 ₽\n2.7 л / 190 л.с. / " +
                "Дизель\nавтомат\nвнедорожник 5 дв.\nполный\nсеребристый\nЗаметка об этом автомобиле " +
                "(её увидите только вы)"));
    }
}

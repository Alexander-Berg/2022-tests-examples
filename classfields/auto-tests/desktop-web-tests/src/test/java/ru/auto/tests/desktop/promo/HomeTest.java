package ru.auto.tests.desktop.promo;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HOME;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - об Авто.ру")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HomeTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(HOME).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoPage().content().should(hasText("Захотел — купил,\nзахотел — продал\nАвто.ру помогает " +
                "купить или продать машину.\nЭто не просто сайт, где каждый может найти или разместить объявление " +
                "(хотя, конечно, каждый может), задача Авто.ру — оградить человека от неприятностей," +
                " связанных с куплей-продажей автомобиля.\nМы считаем, что удобством интерфейса или количеством " +
                "предложений уже никого не удивишь. Это как кондиционер или электрические стеклоподъемники — должно " +
                "быть в базовой комплектации. Поэтому Авто.ру заботится о душевном спокойствии пользователей " +
                "и работает для того, чтобы сделка была в радость.\nУ нас можно разместить полноценное объявление " +
                "с помощью смартфона, госномера на снимках закрашиваются автоматически, юридическая чистота машин " +
                "проверяется в системе «Автокод», услуга «Умный номер» защищает от спама и звонков перекупщиков, " +
                "есть стандартная, проверенная юристами форма договора купли-продажи. И мы ещё не закончили.\n" +
                "Мы хотим, чтобы автомобильный рынок стал более безопасным и комфортным для обычного человека. " +
                "Авто.ру, к которому мы стремимся, это место, куда можно прийти без особого опыта и быть уверенным, " +
                "что купишь желаемую машину в хорошем состоянии или продашь свою по хорошей цене. С удовольствием.\n" +
                "Адрес и телефон\n115035, Россия, г. Москва, ул. Садовническая, д. 82, стр. 2.\n" +
                "Телефоны 8 800 234-28-86 и 8 495 755-55-77.\nКоммерческий отдел\nО продвижении объявлений для " +
                "дилеров и размещении рекламы читайте на auto.ru/dealer.\nПресс-служба\nПишите на pr@auto.ru, " +
                "чтобы подписаться на наши пресс-релизы или задать вопрос от СМИ или блога.\nДилерам\n" +
                "Размещение и продвижение\nДополнительные сервисы\nКонференции Авто.ру\n3D панорамы машин\nПрофессиональным " +
                "продавцам\nАналитика в Журнале →\nМедийная реклама\nО проекте\nЛоготип Авто.ру\nДоговор →"));
        basePageSteps.onPromoPage().header().should(isDisplayed());
        basePageSteps.onPromoPage().footer().should(isDisplayed());
        basePageSteps.onPromoPage().sidebar().should(isDisplayed());
    }
}
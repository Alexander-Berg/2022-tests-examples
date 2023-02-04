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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.DESIGN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - дизайн")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DesignTest {

    private static final String STATIC_URL = "https://auto.ru/static/img/design/";

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

        urlSteps.testing().path(DESIGN).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoPage().content().should(hasText("Правила использования логотипа Авто.ру\nНастоящие " +
                "правила (далее — Правила) регулируют некоммерческое использование логотипа ООО «Авто.ру» " +
                "(далее — Авто.ру).\nЛюбой желающий может разместить логотип Авто.ру у себя на сайте или " +
                "в мобильном приложении при соблюдении Правил. Для использования логотипа иным образом нужно " +
                "получить письменное разрешение Авто.ру, обратившись по адресу pr@auto.ru.\nЛоготип можно " +
                "размещать только с прямой ссылкой на главную страницу Авто.ру — auto.ru.\nНельзя искажать внешний вид, " +
                "пропорции, цветовую гамму логотипа или его частей. Запрещается использовать логотип или какую-либо " +
                "его часть как элемент другого логотипа, товарного знака, фирменного наименования, слогана и пр. " +
                "Допустимо использование текстовой части логотипа (без знака автомобиля).\nНельзя размещать логотип " +
                "таким образом, при котором может возникнуть предположение о сотрудничестве Авто.ру с неким физическим " +
                "или юридическим лицом либо о причастности Авто.ру к предлагаемым товарам, услугам, мероприятиям.\n" +
                "Логотип Авто.ру запрещается публиковать на сайтах и в мобильных приложениях, не соответствующих " +
                "требованиям законодательства, в том числе содержащих:\nнедостоверную информацию об Авто.ру и его " +
                "услугах,\nматериалы, размещение которых нарушает интеллектуальные права, порочит честь, достоинство, " +
                "деловую репутацию или не соответствуют принципам Авто.ру.\nАвто.ру оставляет за собой право вносить " +
                "изменения в Правила. Новые и изменённые положения будут обязательными для всех лиц, использующих " +
                "логотип на основании Правил.\nВ случае использования логотипа способами, не предусмотренными " +
                "в Правилах, Авто.ру оставляет за собой право обратиться за защитой своих законных интересов " +
                "в правоохранительные и судебные органы.\nЛоготип Auto.ru является зарегистрированным товарным " +
                "знаком, свидетельство № 377132. Запрещается любое использование товарного знака без письменного " +
                "разрешения администрации ООО «Авто.ру»\nЛоготип для скачивания\nВеб-версия логотипа\n(png, svg)\n" +
                "Версия для печати\n(tiff, pdf, eps, ai)\nЦвет Авто.ру\nЗапрещено использовать логотип в любых " +
                "оттенках красного, отличных от указанных. Если логотип размещается на черном или цветном фоне, " +
                "то необходимо использовать его в белом цвете.\nR 219 G 55 B 39\nRAL 3028\nPantone 179C\n" +
                "C8 M93 Y100 K0\n#DB3727\nДилерам\nРазмещение и продвижение\nДополнительные сервисы\nКонференции Авто.ру\n" +
                "3D панорамы машин\nПрофессиональным продавцам\nАналитика в Журнале →\nМедийная реклама\n" +
                "О проекте\nЛоготип Авто.ру\nДоговор →"));
        basePageSteps.onPromoPage().header().should(isDisplayed());
        basePageSteps.onPromoPage().footer().should(isDisplayed());
        basePageSteps.onPromoPage().sidebar().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Веб-версия логотипа")
    public void shouldSeeLogoUrl() {
        basePageSteps.onPromoPage().buttonContains("Веб-версия логотипа")
                .should(hasAttribute("href", format("%s%s", STATIC_URL, "logo-for-web.zip")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Версия для печати")
    public void shouldSeePrintUrl() {
        basePageSteps.onPromoPage().buttonContains("Версия для печати")
                .should(hasAttribute("href", format("%s%s", STATIC_URL, "logo-for-print.zip")));
    }
}
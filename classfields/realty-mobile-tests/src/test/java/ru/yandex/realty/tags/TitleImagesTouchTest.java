package ru.yandex.realty.tags;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.config.RealtyTagConfig;
import ru.yandex.realty.module.RealtySeoModule;
import ru.yandex.realty.step.JSoupSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@DisplayName("Проверка «title» у картинок на разных страницах")
@Link("https://st.yandex-team.ru/VERTISTEST-2077")
@RunWith(Parameterized.class)
@GuiceModules(RealtySeoModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TitleImagesTouchTest {

    private String host;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps = new JSoupSteps();

    @Inject
    public RealtyTagConfig config;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String locator;

    @Parameterized.Parameter(3)
    public String titleExpected;

    @Parameterized.Parameters(name = " {index} Проверяем «title» у картинок на {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Листинг офферов",
                        "/sankt-peterburg/kupit/kvartira/odnokomnatnaya/do-6000000/",
                        ".SwipeGallery__thumb-img",
                        "Купить 1-комнатную квартиру до 6 млн рублей в Санкт-Петербурге - Яндекс Недвижимость"},
                {"Карточка ЖК",
                        "/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka/valo-395435/",
                        ".SwipeGallery__thumb-img",
                        "Апарт-отель VALO - Яндекс Недвижимость"},
                {"Листинг ЖК",
                        "/moskva/kupit/novostrojka/",
                        "img[class*=SiteSnippetGallery__image]",
                        "Купить квартиру в новостройке в Москве - Яндекс Недвижимость"},
                {"Карточка застройщика",
                        "/sankt-peterburg_i_leningradskaya_oblast/zastroyschik/gruppa-ehtalon-lenspecsmu-1914/",
                        ".SwipeGallery__thumb-img",
                        "Застройщик Группа «Эталон» (ЛенСпецСМУ) в Санкт-Петербурге и ЛО - Яндекс Недвижимость"},
                {"Главная",
                        "/moskva/",
                        ".MinifiedSerpItem__image",
                        "Недвижимость в Москве - Яндекс Недвижимость"},
                {"Листинг КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/",
                        ".SwipeGallery__thumb-img",
                        "Коттеджные посёлки в Москве и МО - Яндекс Недвижимость"},
                {"Карточка КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/krona-1797180/",
                        ".SwipeGallery__thumb-img",
                        "Коттеджный посёлок «Крона» - Яндекс Недвижимость"}
        });
    }

    @Before
    public void before() {
        host = config.getTestingURI().toString();
        host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeTitleInImg() {
        jSoupSteps.connectTo(host + path).mobileHeader().get();
        String titleActual = jSoupSteps.select(locator).first().attr("title");
        assertThat(titleActual).isEqualTo(titleExpected);
    }
}

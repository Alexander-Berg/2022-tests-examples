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

@DisplayName("Проверка «alt» у картинок на разных страницах")
@Link("https://st.yandex-team.ru/VERTISTEST-2077")
@RunWith(Parameterized.class)
@GuiceModules(RealtySeoModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AltImagesTouchTest {

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
    public String altExpected;

    @Parameterized.Parameters(name = " {index} Проверяем «alt» у картинок на {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Листинг офферов",
                        "/sankt-peterburg/kupit/kvartira/odnokomnatnaya/do-6000000/",
                        ".SwipeGallery__thumb-img",
                        "Купить 1-комнатную квартиру до 6 млн рублей в Санкт-Петербурге - изображение 1"},
                {"Карточка ЖК",
                        "/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka/valo-395435/",
                        ".SwipeGallery__thumb-img",
                        "Апарт-отель VALO - изображение 1"},
                {"Листинг ЖК",
                        "/moskva/kupit/novostrojka/",
                        "img[class*=SiteSnippetGallery__image]",
                        "Купить квартиру в новостройке в Москве - изображение 1"},
                {"Карточка застройщика",
                        "/sankt-peterburg_i_leningradskaya_oblast/zastroyschik/gruppa-ehtalon-lenspecsmu-1914/",
                        ".SwipeGallery__thumb-img",
                        "Застройщик Группа «Эталон» (ЛенСпецСМУ) в Санкт-Петербурге и ЛО - изображение 1"},
                {"Главная",
                        "/moskva/",
                        ".MinifiedSerpItem__image",
                        "Недвижимость в Москве - изображение 1"},
                {"Листинг КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/",
                        ".SwipeGallery__thumb-img",
                        "Коттеджные посёлки в Москве и МО - изображение 1"},
                {"Карточка КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/krona-1797180/",
                        ".SwipeGallery__thumb-img",
                        "Коттеджный посёлок «Крона» - изображение 1"}
        });
    }

    @Before
    public void before() {
        host = config.getTestingURI().toString();
        host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeAltInImg() {
        jSoupSteps.connectTo(host + path).mobileHeader().get();
        String altActual = jSoupSteps.select(locator).first().attr("alt");
        assertThat(altActual).isEqualTo(altExpected);
    }
}

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
public class AltImagesTest {

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
                        ".Gallery__activeImg",
                        "Купить 1-комнатную квартиру до 6 млн рублей в Санкт-Петербурге - изображение 1"},
                {"Карточка ЖК",
                        "/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka/valo-395435/",
                        ".GallerySlideHidden",
                        "Апарт-отель VALO - изображение 1"},
                {"Листинг ЖК",
                        "/moskva/kupit/novostrojka/",
                        ".Gallery__activeImg",
                        "Купить квартиру в новостройке в Москве - изображение 1"},
                {"Карточка застройщика",
                        "/sankt-peterburg_i_leningradskaya_oblast/zastroyschik/setl-group-2247/",
                        ".Gallery__activeImg",
                        "Застройщик Setl Group в Санкт-Петербурге и ЛО - изображение 1"},
                {"Главная",
                        "/moskva/",
                        ".Gallery__activeImg",
                        "где альт???? должен ли быть???"},
                {"Листинг КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/",
                        ".Gallery__item img",
                        "Коттеджные посёлки в Москве и МО - изображение 1"},
                {"Карточка КП",
                        "/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/krona-1797180/",
                        ".GallerySlideHidden",
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
        jSoupSteps.connectTo(host + path).get();
        String altActual = jSoupSteps.select(locator).first().attr("alt");
        assertThat(altActual).isEqualTo(altExpected);
    }
}

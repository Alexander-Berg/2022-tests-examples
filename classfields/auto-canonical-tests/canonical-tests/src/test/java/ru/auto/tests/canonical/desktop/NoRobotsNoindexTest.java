package ru.auto.tests.canonical.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import okhttp3.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.canonical.CanonicalClientModule;
import ru.auto.tests.canonical.categories.Desktop;
import ru.auto.tests.canonical.categories.Mobile;
import ru.auto.tests.canonical.steps.CanonicalSteps;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;

import java.io.IOException;
import java.util.Collection;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.canonical.constant.Features.ROBOTS_NOINDEX;
import static ru.auto.tests.canonical.constant.Owners.ALEKS_IVANOV;

@Feature(ROBOTS_NOINDEX)
@DisplayName("Проверяем отсутствие meta тэга с robots noindex")
@RunWith(Parameterized.class)
@GuiceModules(CanonicalClientModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NoRobotsNoindexTest {

    @Inject
    private CanonicalSteps canonicalSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"https://auto.ru/"},
                {"https://auto.ru/moskva/"},

                {"https://auto.ru/cars/all/"},
                {"https://auto.ru/moskva/cars/all/body-sedan/"},
                {"https://auto.ru/cars/used/"},
                {"https://auto.ru/cars/new/group/bmw/3er/21398591-21398651/"},
                {"https://auto.ru/cars/new/group/bmw/3er/21398591-21398651/?from=single_group_snippet_listing"},
                {"https://auto.ru/cars/new/group/mercedes/gle_klasse_amg/21754087/21754100/1105739733-71daf756/"},
                {"https://auto.ru/rossiya/cars/renault/logan/used/"},
                {"https://auto.ru/moskva/cars/hyundai/tucson/21343951/21344007/all/"},
                {"https://auto.ru/moskva/cars/audi/a2/2010-year/all/"},

                {"https://auto.ru/lcv/all/"},
                {"https://auto.ru/dredge/caterpillar/used/"},
                {"https://auto.ru/moskva/motorcycle/bmw/f_650_gs/all/"},

                {"https://auto.ru/catalog/cars/"},
                {"https://auto.ru/catalog/cars/mazda/6/20435551/20492272/specifications/20492272__20492351/"},
                {"https://auto.ru/catalog/cars/mazda/6/20435551/20435559/equipment/20435559_20435717_20435560/"},

                {"https://auto.ru/dealer/"},
                {"https://auto.ru/moskva/dilery/cars/all/"},
                {"https://auto.ru/diler/cars/all/rolf_severo_zapad_avtomobili_s_probegom_moskva/"},

                {"https://auto.ru/reviews/"},
                {"https://auto.ru/reviews/trucks/"},
                {"https://auto.ru/reviews/moto/"},
                {"https://auto.ru/review/moto/atv/armada/atv_110/3766172241007211287/"},
                {"https://auto.ru/review/trucks/bus/ford/transit/1643591388826256509/"},
                {"https://auto.ru/review/cars/audi/q3/7341708/4030403/"},

                {"https://auto.ru/video/"},
                {"https://auto.ru/video/cars/audi/q3/7341708/"},

                {"https://auto.ru/history/"},
                {"https://auto.ru/promo/finance-october-promo/"},
                {"https://auto.ru/home/"},
                {"https://auto.ru/promo/osago/"},
                {"https://auto.ru/garage/"},
                {"https://auto.ru/compare-cars/hyundai-solaris-vs-kia-rio/"}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Desktop.class})
    @DisplayName("Проверяем отсутствие meta тэга с robots noindex")
    public void shouldNotSeeRobotsNoindex() throws IOException {
        Response response = canonicalSteps.makeDesktopRequest(url, "");

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.headers("X-Autoru-App-Id").get(0), startsWith("af-desktop"));
        canonicalSteps.notContainsRobotsNoindex(response.body().string());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Mobile.class})
    @DisplayName("Проверяем отсутствие meta тэга с robots noindex")
    public void shouldNotSeeRobotsNoindexMobile() throws IOException {
        Response response = canonicalSteps.makeMobileRequest(url, "");

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.headers("X-Autoru-App-Id").get(0), startsWith("af-mobile"));
        canonicalSteps.notContainsRobotsNoindex(response.body().string());
    }

}


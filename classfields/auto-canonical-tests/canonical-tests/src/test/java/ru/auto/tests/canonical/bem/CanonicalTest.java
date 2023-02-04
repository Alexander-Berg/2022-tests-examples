package ru.auto.tests.canonical.bem;

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
import ru.auto.tests.canonical.categories.Bem;
import ru.auto.tests.canonical.categories.Mobile;
import ru.auto.tests.canonical.steps.CanonicalSteps;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;

import java.io.IOException;
import java.util.Collection;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.canonical.constant.Features.CANONICAL;
import static ru.auto.tests.canonical.constant.Owners.DSVICHIHIN;
import static ru.auto.tests.canonical.constant.Owners.TIMONDL;

@DisplayName("Canonical")
@Feature(CANONICAL)
@RunWith(Parameterized.class)
@GuiceModules(CanonicalClientModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CanonicalTest {

    @Inject
    private CanonicalSteps canonicalSteps;

    @Parameterized.Parameter
    public String gids;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameter(2)
    public String canonical;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "https://auto.ru/catalog/cars/", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/audi/", "https://auto.ru/catalog/cars/audi/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/", "https://auto.ru/catalog/cars/audi/a4/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/2305334/", "https://auto.ru/catalog/cars/audi/a4/2305334/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/specifications/", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/specifications/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/specifications/3480062__3480065/", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/specifications/3480062__3480065/"},
                {"", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/equipment/", "https://auto.ru/catalog/cars/audi/a4/2305334/3480062/equipment/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT&seats=4_5", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT&seats=4_5&acceleration_from=7", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT&seats=4_5&acceleration_from=7&acceleration_to=8", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT&seats=4_5&acceleration_from=7&acceleration_to=8&power_from=150", "https://auto.ru/catalog/cars/"},
                {"", "https://auto.ru/catalog/cars/all/?autoru_body_type=SEDAN&autoru_body_type=HATCHBACK_3_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&gear_type=ALL_WHEEL_DRIVE&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&transmission_full=AUTO_ROBOT&transmission_full=AUTO_VARIATOR&transmission_full=AUTO&transmission_full=MECHANICAL&year_from=2004&year_to=2007&steering_wheel=LEFT&seats=4_5&acceleration_from=7&acceleration_to=8&power_from=150&power_to=200", "https://auto.ru/catalog/cars/"},
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Bem.class})
    @DisplayName("Проверка canonical")
    public void shouldSeeCanonical() throws IOException {
        Response response = canonicalSteps.makeDesktopRequest(url, format("gids=%s", gids));

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.headers("X-Autoru-App-Id").get(0), startsWith("af-desktop-bem="));
        canonicalSteps.checkCanonical(response.body().string(), canonical);
    }

    @Test
    @Owner(TIMONDL)
    @Category({Mobile.class})
    @DisplayName("Проверка canonical")
    public void shouldSeeCanonicalMobile() throws IOException {
        Response response = canonicalSteps.makeMobileRequest(url, format("gids=%s", gids));

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.headers("X-Autoru-App-Id").get(0), startsWith("af-mobile="));
        canonicalSteps.checkCanonical(response.body().string(), canonical);
    }
}

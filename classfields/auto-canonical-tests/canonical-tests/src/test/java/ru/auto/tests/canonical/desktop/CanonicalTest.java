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

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.canonical.constant.Features.CANONICAL;
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
                // Главная
                {"", "https://auto.ru/", "https://auto.ru/"},
                {"", "https://auto.ru/moskva/", "https://auto.ru/moskva/"},
                {"", "https://auto.ru/voronezh/", "https://auto.ru/voronezh/"},
                {"", "https://auto.ru/voronezhskaya_oblast/", "https://auto.ru/voronezhskaya_oblast/"},
                {"", "https://auto.ru/rossiya/", "https://auto.ru/"},
                //{"", "https://auto.ru/lcv/", "https://auto.ru/"},
                {"213", "https://auto.ru/", "https://auto.ru/"},
                {"225", "https://auto.ru/", "https://auto.ru/"},
                {"255", "https://auto.ru/", "https://auto.ru/"},
                {"213", "https://auto.ru/moskva/", "https://auto.ru/moskva/"},

                // Дилерский листинг
                {"", "https://auto.ru/voronezh/dilery/cars/all/?dealer_org_type=1", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?sort_offers=cr_date-DESC&dealer_org_type=1&mark-model-nameplate=AUDI", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?sort_offers=cr_date-DESC&dealer_org_type=1&mark-model-nameplate=AUDI%23A4", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?sort_offers=cr_date-DESC&dealer_org_type=1&mark-model-nameplate=AUDI%23A4%23%2320637504", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?sort_offers=cr_date-DESC&autoru_body_type=SEDAN&dealer_org_type=1&mark-model-nameplate=AUDI%23A4%23%2320637504", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?autoru_body_type=SEDAN&color=FAFBFB&displacement_from=400&displacement_to=9000&engine_type=GASOLINE&gear_type=REAR_DRIVE&is_clear=true&km_age_from=10&km_age_to=1000000&mark-model-nameplate=AUDI%23A4%23%2320637504&power_from=1&power_to=5555&price_from=25&price_to=30000025&sort_offers=cr_date-DESC&transmission_full=AUTO_AUTOMATIC&video=true&warranty_status=1&year_from=2010&year_to=2017&dealer_org_type=1&dealer_net_id=20156383", "https://auto.ru/voronezh/dilery/cars/all/"},
                {"", "https://auto.ru/voronezh/dilery/cars/all/?sort_offers=cr_date-DESC&autoru_body_type=SEDAN&change-index=1&color=FAFBFB&dealer_org_type=1&displacement_from=400&displacement_to=9000&engine_type=GASOLINE&gear_type=REAR_DRIVE&is_clear=true&km_age_from=10&km_age_to=1000000&mark-model-nameplate=AUDI%23A4%23%2320637504&mark-model-nameplate=BMW%233ER%23%2320548423&power_from=1&power_to=5555&price_from=25&price_to=30000025&transmission_full=AUTO_AUTOMATIC&video=true&warranty_status=1&year_from=2010&year_to=2017&dealer_net_id=20156383", "https://auto.ru/voronezh/dilery/cars/all/"},

                // Дилерская карточка
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/?from=dealer-listing-list", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/audi/?from=dealer-listing-list", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/audi/a4/?from=dealer-listing-list", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/audi/a4/20637504/?from=dealer-listing-list", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/audi/a4/20637504/?sort_offers=cr_date-DESC&autoru_body_type=SEDAN&from=dealer-listing-list", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/audi/a4/20637504/?sort_offers=cr_date-DESC&autoru_body_type=SEDAN&color=CACECB&displacement_from=400&displacement_to=9000&engine_type=GASOLINE&exchange_status=1&from=dealer-listing-list&gear_type=REAR_DRIVE&is_clear=true&km_age_from=10&km_age_to=1000000&owners_count=1&owning_time=12_36&power_from=1&power_to=5555&price_from=25&price_to=10000000&pts_status=1&search_tag=certificate&steering_wheel=LEFT&transmission_full=AUTO_AUTOMATIC&video=true&warranty_status=1&year_from=2012&year_to=2017", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},
                {"", "https://auto.ru/diler-oficialniy/cars/all/major_hyundai_strogino_moskva/?sort_offers=cr_date-DESC&autoru_body_type=SEDAN&color=CACECB&displacement_from=400&displacement_to=9000&engine_type=GASOLINE&exchange_status=1&from=dealer-listing-list&gear_type=REAR_DRIVE&is_clear=true&km_age_from=10&km_age_to=1000000&mark-model-nameplate=AUDI%23A4%23%2320637504&mark-model-nameplate=BMW%233ER&owners_count=1&owning_time=12_36&power_from=1&power_to=5555&price_from=25&price_to=10000000&pts_status=1&search_tag=certificate&steering_wheel=LEFT&transmission_full=AUTO_AUTOMATIC&video=true&warranty_status=1&year_from=2012&year_to=2017", "https://auto.ru/diler/cars/all/major_hyundai_strogino_moskva/"},

                //Листинг CARS
                {"", "https://auto.ru/moskva/cars/new/", "https://auto.ru/moskva/cars/new/"},
                {"", "https://auto.ru/moskva/cars/audi/new/", "https://auto.ru/moskva/cars/audi/new/"},
                {"", "https://auto.ru/moskva/cars/audi/new/?sort_offers=cr_date-DESC&top_days=off&currency=RUR&output_type=list&image=true&beaten=1&customs_state=1&geo_id=193&page_num_offers=1", "https://auto.ru/voronezh/cars/audi/new/"},
                {"", "https://auto.ru/moskva/cars/kia/rio/new/", "https://auto.ru/moskva/cars/kia/rio/new/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/new/?year_from=2000&year_to=2001", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/q5/new/?year_from=2000&year_to=2021&do_not_redirect=true", "https://auto.ru/moskva/cars/audi/q5/new/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/new/?price_from=25&price_to=30", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/new/?body_type_group=LIFTBACK&body_type_group=COUPE", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/2305334/new/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/2305334/new/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/2305334/new/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/2305334/new/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/2305334/new/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/cars/new/group/mercedes/c_klasse/21291732-21291804/", "https://auto.ru/cars/new/group/mercedes/c_klasse/21291732-21291804/"},
                {"", "https://auto.ru/cars/new/group/mercedes/c_klasse/21333380/21333455/1095997190-2872b1d9/", "https://auto.ru/cars/mercedes/c_klasse/new/"},
                {"", "https://auto.ru/cars/new/group/ford/focus/7307060/0/1029379693-dca791/", "https://auto.ru/cars/ford/focus/new/"},
                {"", "https://auto.ru/moskva/cars/vaz/2101/new/", "https://auto.ru/cars/vaz/2101/used/"},

                {"", "https://auto.ru/moskva/cars/used/", "https://auto.ru/moskva/cars/used/"},
                {"", "https://auto.ru/moskva/cars/vaz/2101/used/", "https://auto.ru/moskva/cars/vaz/2101/used/"},
                {"", "https://auto.ru/cars/used/sale/audi/a4/1045817426-2b24/", "https://auto.ru/cars/audi/a4/used/"},
                {"", "https://auto.ru/moskva/cars/vaz/kalina/9389448/used/do-600000/", "https://auto.ru/moskva/cars/vaz/kalina/9389448/used/do-600000/"},

                {"", "https://auto.ru/moskva/cars/all/", "https://auto.ru/moskva/cars/all/"},
                {"", "https://auto.ru/moskva/cars/audi/all/", "https://auto.ru/moskva/cars/audi/all/"},
                {"", "https://auto.ru/moskva/cars/vendor-foreign/all/", "https://auto.ru/moskva/cars/vendor-foreign/all/"},
                {"", "https://auto.ru/moskva/cars/audi/a4/all/", "https://auto.ru/moskva/cars/audi/a4/all/"},
                {"", "https://auto.ru/moskva/cars/vaz/2101/all/", "https://auto.ru/moskva/cars/vaz/2101/used/"},
                {"", "https://auto.ru/samara/cars/daewoo/all/do-200000/", "https://auto.ru/samara/cars/daewoo/used/"},
                {"", "https://auto.ru/moskva/cars/kia/k5/22462291/all/body-sedan/", "https://auto.ru/moskva/cars/kia/k5/22462291/all/body-sedan/"},
                {"", "https://auto.ru/rostov-na-donu/cars/kia/k5/2017-year/all/", "https://auto.ru/rostov-na-donu/cars/kia/k5/all/"}, // из пустого листинга сбрасывается год
                {"", "https://auto.ru/moskva/cars/kia/k5/22462291/22462326/all/", "https://auto.ru/moskva/cars/kia/k5/22462291/all/body-sedan/"}, // урл с конфигурацией
                {"", "https://auto.ru/rossiya/cars/all/", "https://auto.ru/cars/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/all/", "https://auto.ru/cars/audi/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/all/", "https://auto.ru/cars/audi/a4/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/", "https://auto.ru/cars/audi/a4/2305334/used/"}, // a4 2305334 - снято с производста больше 3х лет
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=FASTBACK&autoru_body_type=SEDAN&autoru_body_type=SEDAN_HARDTOP", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&transmission_full=AUTO_VARIATOR&year_from=2012&year_to=2020", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&transmission_full=AUTO_VARIATOR&year_from=2012&year_to=2020", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&transmission_full=AUTO_VARIATOR&year_from=2012&year_to=2020", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&transmission_full=AUTO_VARIATOR&year_from=2012&year_to=2020", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_VARIATOR&year_from=2012&year_to=2020", "https://auto.ru/cars/audi/a4/2305334/used/body-sedan/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/", "https://auto.ru/cars/audi/q5/22408434/all/"}, // q5 22408434 - выпуск не старше 3х лет
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=FASTBACK&autoru_body_type=SEDAN&autoru_body_type=SEDAN_HARDTOP", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=ALLROAD_5_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=ALLROAD_5_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=ALLROAD_5_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=ALLROAD_5_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/?autoru_body_type=ALLROAD_5_DOORS&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007", "https://auto.ru/cars/audi/q5/22408434/all/"},
                {"", "https://auto.ru/voronezh/cars/all/", "https://auto.ru/voronezh/cars/all/"},
                {"", "https://auto.ru/rossiya/cars/audi/q5/22408434/all/body-allroad_5_doors/", "https://auto.ru/cars/audi/q5/22408434/all/body-allroad_5_doors/"}, // кузов не должен обрезаться
                {"", "https://auto.ru/voronezh/cars/audi/all/", "https://auto.ru/voronezh/cars/audi/all/"},
                {"", "https://auto.ru/voronezh/cars/audi/a4/all/", "https://auto.ru/voronezh/cars/audi/a4/all/"},
                {"", "https://auto.ru/voronezh/cars/audi/a4/2305334/all/", "https://auto.ru/voronezh/cars/audi/a4/2305334/used/"},
                {"", "https://auto.ru/voronezh/cars/audi/a4/2305334/all/?autoru_body_type=SEDAN&displacement_from=2000&displacement_to=2000&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&km_age_from=2000&km_age_to=1000000&price_from=25&price_to=30000025&transmission_full=AUTO_VARIATOR&year_from=2004&year_to=2007&geo_radius=500", "https://auto.ru/voronezh/cars/audi/a4/2305334/used/"},

                //Листинг ТС
                {"", "https://auto.ru/moskva/motorcycle/all/", "https://auto.ru/moskva/motorcycle/all/"},
                {"", "https://auto.ru/moskva/motorcycle/honda/all/", "https://auto.ru/moskva/motorcycle/honda/all/"},
                {"", "https://auto.ru/moskva/truck/all/", "https://auto.ru/moskva/truck/all/"},
                {"", "https://auto.ru/moskva/truck/ford/all/", "https://auto.ru/moskva/truck/ford/all/"},
                {"", "https://auto.ru/lcv/used/sale/mercedes/sprinter/6612204-6f27/", "https://auto.ru/lcv/mercedes/sprinter/used/"},
                {"", "https://auto.ru/motorcycle/used/sale/bmw/r_nine_t/2963772-bf48f574/", "https://auto.ru/motorcycle/bmw/r_nine_t/used/"},
                {"", "https://auto.ru/artic/used/sale/maz/5432/15579540-3ee397e9/", "https://auto.ru/artic/maz/5432/used/"},
                {"", "https://auto.ru/atv/used/sale/armada/atv_110/2960492-6ebba51c/", "https://auto.ru/atv/armada/atv_110/used/"},
                {"", "https://auto.ru/scooters/used/sale/irbis/nirvana/2399332-3d50/", "https://auto.ru/scooters/irbis/nirvana/used/"},
                {"", "https://auto.ru/snowmobile/used/sale/brp/lynx_yeti_pro_army/2398878-b04950/", "https://auto.ru/snowmobile/brp/lynx_yeti_pro_army/used/"},
                {"", "https://auto.ru/voronezh/bus/all/", "https://auto.ru/voronezh/bus/all/"},
                {"", "https://auto.ru/voronezh/bus/mercedes/all/", "https://auto.ru/voronezh/bus/mercedes/all/"},
                {"", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/"},
                {"", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/?bus_type=TOURIST", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/"},
                {"", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/?bus_type=TOURIST&dealer_org_type=4&displacement_from=400&displacement_to=9000&engine_type=DIESEL&exchange_status=1&haggle=POSSIBLE&in_stock=IN_STOCK&km_age_from=10&km_age_to=1000000&power_from=1&power_to=5555&price_from=25&price_to=30000025&seats_from=5&steering_wheel=LEFT&transmission_full=MECHANICAL&truck_color=FFFFFF&wheel_drive=4x2&year_from=2000&year_to=2017", "https://auto.ru/voronezh/bus/mercedes/sprinter_bus/all/"},
                {"", "https://auto.ru/voronezh/bus/all/?bus_type=TOURIST&dealer_org_type=4&displacement_from=400&displacement_to=9000&engine_type=DIESEL&exchange_status=1&haggle=POSSIBLE&in_stock=IN_STOCK&km_age_from=10&km_age_to=1000000&mark-model-nameplate=MERCEDES%23SPRINTER_BUS&mark-model-nameplate=PAZ%233205&power_from=1&power_to=5555&price_from=25&price_to=30000025&seats_from=5&steering_wheel=LEFT&transmission_full=MECHANICAL&truck_color=FFFFFF&wheel_drive=4x2&year_from=2000&year_to=2017", "https://auto.ru/voronezh/bus/all/"},

        });
    }

    @Test
    @Owner(TIMONDL)
    @Category({Desktop.class})
    @DisplayName("Проверка canonical")
    public void shouldSeeCanonical() throws IOException {
        Response response = canonicalSteps.makeDesktopRequest(url, format("gids=%s", gids));

        assertThat(response.code(), equalTo(HTTP_OK));
        assertThat(response.headers("X-Autoru-App-Id").get(0), startsWith("af-desktop="));
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

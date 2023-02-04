package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiSearchInstance.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /device/deeplink-parse")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeeplinkParseTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Диплинк")
    @Parameterized.Parameter(0)
    public String uri;

    @Parameter("Категория")
    @Parameterized.Parameter(1)
    public CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"https://test.avto.ru/cars/all/", CARS},
                {"https://m.test.avto.ru/cars/all/", CARS},

                {"https://test.avto.ru/mototsikly/all/", MOTO},
                {"https://test.avto.ru/skutery/all/", MOTO},
                {"https://test.avto.ru/motovezdehody/all/", MOTO},
                {"https://test.avto.ru/snegohody/all/", MOTO},
                {"https://m.test.avto.ru/scooters/all/", MOTO},
                {"https://test.avto.ru/motorcycle/all/", MOTO},
                {"https://test.avto.ru/snowmobile/all/", MOTO},

                {"https://test.avto.ru/legkie-gruzoviki/all/", TRUCKS},
                {"https://test.avto.ru/trucks/all/", TRUCKS},
                {"https://test.avto.ru/artic/all/", TRUCKS},
                {"https://test.avto.ru/bus/all/", TRUCKS},
                {"https://test.avto.ru/drags/all/", TRUCKS},
                {"https://m.test.avto.ru/light_trucks/all/", TRUCKS},
                {"https://m.test.avto.ru/trucks/all/", TRUCKS},
                {"https://test.avto.ru/lcv/all/", TRUCKS},
                {"https://test.avto.ru/truck/all/", TRUCKS},
                {"https://test.avto.ru/trailer/all/", TRUCKS},
                {"https://test.avto.ru/agricultural/all/", TRUCKS},
                {"https://test.avto.ru/construction/all/", TRUCKS},
                {"https://test.avto.ru/autoloader/all/", TRUCKS},
                {"https://test.avto.ru/crane/all/", TRUCKS},
                {"https://test.avto.ru/dredge/all/", TRUCKS},
                {"https://test.avto.ru/bulldozers/all/", TRUCKS},
                {"https://test.avto.ru/crane_hydraulics/all/", TRUCKS},
                {"https://test.avto.ru/municipal/all/", TRUCKS}}
        );
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeCategory() {
        CategoryEnum response = api.device().parseDeeplink().linkQuery(uri).typeQuery("search").reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSearchData().getCategory();

        assertThat(response).isEqualTo(category);
    }
}

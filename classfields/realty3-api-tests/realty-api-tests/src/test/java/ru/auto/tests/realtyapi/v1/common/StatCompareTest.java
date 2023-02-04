package ru.auto.tests.realtyapi.v1.common;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("POST /stat.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StatCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("События")
    @Parameterized.Parameter(0)
    public String events;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        String resourceName = "testdata/stat.txt";
        InputStream dataStream = ErrorsDescriptionCompareTest.class.getClassLoader().getResourceAsStream(resourceName);
        return (Collection<Object[]>) IOUtils.readLines(dataStream, UTF_8).stream()
                .map(request -> new Object[]{request})
                .collect(Collectors.toList());
    }

    @Test
    public void shouldNoDiffWithProduction() {
        api.common().statRoute().reqSpec(s -> s.setContentType(JSON).setBody(events)).reqSpec(authSpec())
                .execute(validatedWith(shouldBeOK()));
    }
}

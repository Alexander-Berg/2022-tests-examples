package ru.yandex.realty.seo.redirect;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import com.opencsv.CSVReader;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.config.RealtyTagConfig;
import ru.yandex.realty.module.RealtySeoModule;
import ru.yandex.realty.step.SeoTestSteps;

import java.io.FileReader;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("Тесты редирект. Тач")
@RunWith(Parameterized.class)
@GuiceModules(RealtySeoModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TouchRedirectTest {

    @Inject
    public RealtyTagConfig config;

    @Inject
    private SeoTestSteps seoTestSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String redirect;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<String[]> testParameters() throws Exception {
        CSVReader reader = new CSVReader(new FileReader("target/test-classes/redirect.csv"));
        return reader.readAll();
    }

    @Test
    @DisplayName("Тач")
    public void shouldSeeTouchRedirect() {
        String realUrl = seoTestSteps.getNetworkMobileResponseUrl(config.getTestingURI() + path);
        assertThat(realUrl, equalTo(config.getTestingURI().toString() + redirect));
    }
}

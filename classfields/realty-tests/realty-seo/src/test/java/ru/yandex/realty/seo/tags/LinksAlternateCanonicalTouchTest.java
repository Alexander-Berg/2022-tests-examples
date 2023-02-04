package ru.yandex.realty.seo.tags;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import com.opencsv.CSVReader;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.config.RealtyTagConfig;
import ru.yandex.realty.module.RealtySeoModule;
import ru.yandex.realty.step.JSoupSteps;

import java.io.FileReader;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@DisplayName("Canonical/alternate touch")
@Link("https://st.yandex-team.ru/VERTISTEST-1866")
@RunWith(Parameterized.class)
@GuiceModules(RealtySeoModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LinksAlternateCanonicalTouchTest {

    private static final String HREF = "href";
    private static final String CANONICAL_LOCATOR = "link[rel='canonical']";
    private static final String ALTERNATE_LOCATOR = "link[rel='alternate']";
    private static final String H1_LOCATOR = "body h1";
    private static final String PAGE_404 = "Страница не найдена";
    private static final String PAGE_500 = "Произошла ошибка";

    private String host;
    private String mHost;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps = new JSoupSteps();

    @Inject
    public RealtyTagConfig config;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String canonicalPath;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<String[]> testParameters() throws Exception {
        CSVReader reader = new CSVReader(new FileReader("target/test-classes/alternate_canonical_touch.csv"));
        List<String[]> list = reader.readAll();
        return list.subList(1, list.size());
    }

    @Before
    public void before() {
        host = config.getTestingURI().toString();
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        mHost = host.replace("realty.", "m.realty.");
    }

    @Test
    @DisplayName("Смотрим на «href» на таче")
    @Owner(KANTEMIROV)
    public void shouldSeeTouchTags() {
        jSoupSteps.connectTo(mHost + path).get();
        String canonicalExpected = host + canonicalPath;
        String prodH1 = jSoupSteps.select(H1_LOCATOR).text();
        assertThat(prodH1).doesNotContain(PAGE_500).doesNotContain(PAGE_404);
        String canonicalActual = jSoupSteps.select(CANONICAL_LOCATOR).attr(HREF);
        Elements alternateActual = jSoupSteps.select(ALTERNATE_LOCATOR);
        assertThat(alternateActual).isEmpty();
        assertThat(canonicalActual).isEqualTo(canonicalExpected);
    }
}

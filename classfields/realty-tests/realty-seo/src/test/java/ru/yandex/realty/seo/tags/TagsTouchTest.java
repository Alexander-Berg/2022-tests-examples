package ru.yandex.realty.seo.tags;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.yandex.realty.step.SeoTestSteps;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.Files.readAllLines;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.step.JSoupSteps.CONTENT;
import static ru.yandex.realty.step.JSoupSteps.DESCRIPTION_LOCATOR;
import static ru.yandex.realty.step.JSoupSteps.H1_LOCATOR;
import static ru.yandex.realty.step.JSoupSteps.PAGE_404;
import static ru.yandex.realty.step.JSoupSteps.PAGE_500;
import static ru.yandex.realty.step.JSoupSteps.TITLE_LOCATOR;
import static ru.yandex.realty.step.SeoTestSteps.PATTERN_1;
import static ru.yandex.realty.step.SeoTestSteps.PATTERN_2;

@DisplayName("Проверка тегов. Тач")
@RunWith(Parameterized.class)
@GuiceModules(RealtySeoModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TagsTouchTest {

    String testHost;
    String prodHost;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps = new JSoupSteps();

    @Inject
    private SeoTestSteps seoTestSteps;

    @Inject
    public RealtyTagConfig config;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static List<String> testParameters() throws IOException {
        return readAllLines(Paths.get("target/test-classes/urls.txt"));
    }

    @Before
    public void before() {
        testHost = seoTestSteps.replaceHostToMobile(config.getTestingURI());
        prodHost = seoTestSteps.replaceHostToMobile(config.getProductionURI());
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeTags() {
        jSoupSteps.connectTo(prodHost + "/" + config.getRegion() + url).get();

        String prodTitle = jSoupSteps.select(TITLE_LOCATOR).text();
        String prodDescription = jSoupSteps.select(DESCRIPTION_LOCATOR).attr(CONTENT);
        String prodH1 = jSoupSteps.select(H1_LOCATOR).text();

        jSoupSteps.connectTo(testHost + "/" + config.getRegion() + url).get();
        String testTitle = jSoupSteps.select(TITLE_LOCATOR).text();
        String testDescription = jSoupSteps.select(DESCRIPTION_LOCATOR).attr(CONTENT);
        String testH1 = jSoupSteps.select(H1_LOCATOR).text();

        assertThat(prodH1).doesNotContain(PAGE_500).doesNotContain(PAGE_404);

        patternAssert(testTitle, prodTitle, "Title");
        patternAssert(testDescription, prodDescription, "Description");
        patternAssert(testH1, prodH1, "h1");
    }

    private void patternAssert(String test, String prod, String as) {
        prod = seoTestSteps.parametrizePattern(PATTERN_1, 0, prod);
        prod = seoTestSteps.parametrizePattern(PATTERN_2, 0, prod);
        assertThat(test)
                .overridingErrorMessage(String.format("Should «%s» in testing:\n«%s»\n contains pattern:\n«%s»", as, test, prod))
                .containsPattern(Pattern.compile(prod));
    }
}

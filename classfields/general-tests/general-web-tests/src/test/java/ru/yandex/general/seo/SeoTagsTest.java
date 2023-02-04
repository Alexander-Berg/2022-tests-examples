package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.H1_TITLE_DESCRIPTION;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;

@Epic(SEO_FEATURE)
@Feature(H1_TITLE_DESCRIPTION)
@DisplayName("Сео тесты на тэги - тайтл, описание, H1")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoTagsTest {

    private static final String H1_LOCATOR = "body h1";
    private static final String TITLE_LOCATOR = "head title";
    private static final String DESCRIPTION_LOCATOR = "head meta[name='description']";

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameter(2)
    public String title;

    @Parameterized.Parameter(3)
    public String description;

    @Parameterized.Parameter(4)
    public String h1;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testData() throws IOException {
        return Files.lines(Paths.get("src/test/resources/seo.csv")).map(line -> line.split(";"))
                .collect(Collectors.toList());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сео тайтл")
    public void shouldSeeTitle() {
        jSoupSteps.testing().uri(url).setDesktopUserAgent().get();
        String actualTitle = jSoupSteps.select(TITLE_LOCATOR).text();

        assertThat(actualTitle).matches(title);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сео описание")
    public void shouldSeeSeoDescription() {
        jSoupSteps.testing().uri(url).setDesktopUserAgent().get();
        String actualDescription = jSoupSteps.select(DESCRIPTION_LOCATOR).attr("content");

        assertThat(actualDescription).matches(description);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("H1")
    public void shouldSeeH1() {
        jSoupSteps.testing().uri(url).setDesktopUserAgent().get();
        String actualH1 = jSoupSteps.select(H1_LOCATOR).text();

        assertThat(actualH1).matches(h1);
    }

}

package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
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

import static org.hamcrest.Matchers.is;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.step.JSoupSteps.CANONICAL_LOCATOR;

@Epic(SEO_FEATURE)
@Feature("Canonical на страницах сервиса")
@DisplayName("Сео тесты на canonical на страницах сервиса")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoCanonicalTest {

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameter(2)
    public String canonicalUrl;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testData() throws IOException {
        return Files.lines(Paths.get("src/test/resources/seoCanonical.csv")).map(line -> line.split(";"))
                .collect(Collectors.toList());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем canonical")
    public void shouldSeeCanonical() {
        jSoupSteps.testing().uri(url).setDesktopUserAgent().get();
        String actualCanonical = jSoupSteps.select(CANONICAL_LOCATOR).attr(HREF);

        Assert.assertThat("Canonical на странице соответствует", actualCanonical,
                is(jSoupSteps.testing().uri(canonicalUrl).toString()));
    }

}

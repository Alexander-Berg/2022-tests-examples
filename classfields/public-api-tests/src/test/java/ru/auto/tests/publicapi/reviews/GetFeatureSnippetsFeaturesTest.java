package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/featureSnippet/{category}/snippet")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetFeatureSnippetsFeaturesTest {
    private static final String DEFAULT_MARK = "VAZ";
    private static final String DEFAULT_MODEL = "2107";
    private static final String DEFAULT_GENERATION = "2307270";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Название хатактеристики")
    @Parameterized.Parameter(0)
    public String feature;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultFeaturesEnums());
    }

    private static Object[] defaultFeaturesEnums() {
        return new Object[]{
                "dynamics",
                "assembly_quality",
                "comfort",
                "design",
                "dynamics",
                "fuel",
                "gear",
                "maintenance_cost",
                "multimedia",
                "noise",
                "passability",
                "reliability",
                "safety",
                "space",
                "steering",
                "suspension",
                "trunk",
                "visibility"
        };
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldGetAllFeatures() {
        api.reviews().featureSnippet().subjectPath(AUTO).categoryPath(CARS).markQuery(DEFAULT_MARK)
                .modelQuery(DEFAULT_MODEL).superGenQuery(DEFAULT_GENERATION).featureQuery(feature)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
    }
}

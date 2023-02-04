package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.VertisPassportSocialProviderAuthUriResult;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.enums.Platform.ANDROID;
import static ru.auto.tests.publicapi.enums.Platform.DESKTOP;
import static ru.auto.tests.publicapi.enums.Platform.FRONTEND_MOBILE;
import static ru.auto.tests.publicapi.enums.Platform.IOS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.yandex.qatools.matchers.uri.SameAsURIMatcher.sameAsURI;

@DisplayName("GET /auth/login-social/auth-uri/{provider}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LoginSocialProviderPlatformTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Платформа")
    @Parameterized.Parameter
    public ru.auto.tests.publicapi.enums.Platform platform;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(platforms());
    }


    private static Object[] platforms() {
        return new ru.auto.tests.publicapi.enums.Platform[]{
                ANDROID,
                IOS,
                DESKTOP,
                FRONTEND_MOBILE
        };
    }

    @Test
    public void shouldSeeUri() {
        String sessionId = adaptor.session().getSession().getId();

        Function<ApiClient, VertisPassportSocialProviderAuthUriResult> request = apiClient -> apiClient.auth().socialProviderAuthUri().providerPath(LoginSocialProviderTest.VK_PROVIDER)
                .platformQuery(platform.name()).reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(URI.create(request.apply(api).getUri()), sameAsURI(URI.create(request.apply(prodApi).getUri()))
                .filteredWith(param("state")));
    }
}

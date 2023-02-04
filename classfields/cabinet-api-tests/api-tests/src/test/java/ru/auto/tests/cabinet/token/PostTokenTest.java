package ru.auto.tests.cabinet.token;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.model.TokenView;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /token")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostTokenTest {

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetToken() {
        TokenView response = api.token().generate().reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getToken()).isNotEmpty();
    }
}
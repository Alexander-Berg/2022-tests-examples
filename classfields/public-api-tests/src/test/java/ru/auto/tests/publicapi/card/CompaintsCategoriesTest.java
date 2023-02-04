package ru.auto.tests.publicapi.card;

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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiComplaintRequest;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.ANOTHER;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_PHOTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /offer/{category}/{offerID}/complaints")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CompaintsCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }


    @Test
    public void shouldSetComplaintWithText() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.offerCard().createComplaintOfferCard().categoryPath(category).offerIDPath(offerId).reqSpec(defaultSpec())
                .body(new AutoApiComplaintRequest().reason(ANOTHER).text(Utils.getRandomString())).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSetComplaintForAllCategories() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.offerCard().createComplaintOfferCard().categoryPath(category).offerIDPath(offerId).reqSpec(defaultSpec())
                .body(new AutoApiComplaintRequest().reason(WRONG_PHOTO)).execute(validatedWith(shouldBeSuccess()));
    }
}

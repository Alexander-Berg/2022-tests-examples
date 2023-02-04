package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiPriceAttribute;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 25.07.18
 */


@DisplayName("POST /user/offers/{category}/{offerID}/price")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PriceCategoriesTest {
    private static final String CURRENCY_RUR = "RUR";
    private static final int LOWER_LIMIT_PRICE = 1499;
    private static final int UPPER_LIMIT_PRICE = 1000000050;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldChangePrice() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        int validPrice = getRandomValidPrice();

        api.userOffers().price().categoryPath(category).offerIDPath(offerId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .body(new AutoApiPriceAttribute().price(validPrice).currency(CURRENCY_RUR)).execute(validatedWith(shouldBeSuccess()));

        Double price = api.userOffers().getMyOffer().categoryPath(category).offerIDPath(offerId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess())).getOffer().getPriceInfo().getDprice();

        assertThat(price).isEqualTo(validPrice);
    }

    private int getRandomValidPrice() {
        return new RandomUtils().nextInt(LOWER_LIMIT_PRICE + 1, UPPER_LIMIT_PRICE - 50);
    }
}

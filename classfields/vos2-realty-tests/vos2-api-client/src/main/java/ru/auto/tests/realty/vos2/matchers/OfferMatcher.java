package ru.auto.tests.realty.vos2.matchers;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.http.HttpStatus;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.objects.OfferInfo;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.matchers.OfferInfoMatchers.hasCode;

/**
 * @author kurau (Yuri Kalinin)
 */
@Accessors(chain = true)
public class OfferMatcher extends TypeSafeDiagnosingMatcher<ApiClient> {

    @Setter
    private Matcher<OfferInfo> matcher;
    @Setter
    private int code = HttpStatus.SC_OK;
    @Setter
    private String userId;
    @Setter
    private String offerId;

    @Override
    protected boolean matchesSafely(ApiClient vos2, Description mismatchDescription) {
        OfferInfo offerInfo = vos2.offer().getOfferRoute().userIDPath(userId).offerIDPath(offerId)
                .execute(validatedWith(shouldBeCode(code)))
                .as(OfferInfo.class, GSON);
        matcher.describeMismatch(offerInfo, mismatchDescription);
        return matcher.matches(offerInfo);
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(matcher);
    }

    public static OfferMatcher offerShould(Matcher<OfferInfo> offerInfoMatcher) {
        return new OfferMatcher().setMatcher(offerInfoMatcher);
    }

    public static OfferMatcher shouldNotSeeOffer() {
        return new OfferMatcher().setMatcher(hasCode("ERROR")).setCode(HttpStatus.SC_NOT_FOUND);
    }
}

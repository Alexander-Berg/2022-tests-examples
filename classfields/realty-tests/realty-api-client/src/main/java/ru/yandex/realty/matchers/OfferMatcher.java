package ru.yandex.realty.matchers;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasCode;

/**
 * @author kurau (Yuri Kalinin)
 */
@Accessors(chain = true)
public class OfferMatcher extends TypeSafeDiagnosingMatcher<ApiVos2> {

    @Setter
    private Matcher<OfferInfo> matcher;
    @Setter
    private int code = SC_OK;
    @Setter
    private String userId;
    @Setter
    private String offerId;

    @Override
    protected boolean matchesSafely(ApiVos2 vos2, Description mismatchDescription) {
        OfferInfo offerInfo = vos2.offer().userID().withUserID(userId).offerID().withOfferID(offerId)
                .get(validatedWith(shouldBeCode(code)))
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
        return new OfferMatcher().setMatcher(hasCode("ERROR")).setCode(SC_NOT_FOUND);
    }
}

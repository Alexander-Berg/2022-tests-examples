package ru.yandex.realty.matchers;

import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.test.api.realty.offer.userid.offerid.responses.Offer;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.response.ResponseSpecifications.shouldBeStatusOk;

/**
 * Created by kopitsa on 07.07.17.
 */
@Log
@Accessors(chain = true)
public class TransactionStatusMatcher extends TypeSafeMatcher<ApiVos2> {

    private Offer status;
    @Setter
    private Matcher matcher;
    @Setter
    private String userId;
    @Setter
    private String offerId;

    @Override
    protected boolean matchesSafely(ApiVos2 vos2) {
        status = vos2.offer().userID().withUserID(userId).offerID().withOfferID(offerId)
                .get(validatedWith(shouldBeStatusOk()))
                .as(OfferInfo.class, GSON)
                .getOffer();
        log.info(String.format("Raising status is «%s»", status));
        return (status.getRaising() != null) && matcher.matches(status.getRaising().getStatus());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("wait for offer status").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(ApiVos2 api, Description mismatchDescription) {
        mismatchDescription.appendText("offer status was ").appendValue(status);
    }

    public static TransactionStatusMatcher transactionIsActive(String userId, String offerId) {
        return new TransactionStatusMatcher().setUserId(userId).setOfferId(offerId)
                .setMatcher(equalTo("active"));
    }
}

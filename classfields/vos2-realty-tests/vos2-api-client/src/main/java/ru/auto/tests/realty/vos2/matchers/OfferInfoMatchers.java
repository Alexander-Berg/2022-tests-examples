package ru.auto.tests.realty.vos2.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.auto.tests.realty.vos2.objects.OfferInfo;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author kurau (Yuri Kalinin)
 */
public class OfferInfoMatchers {

    public static Matcher<OfferInfo> hasCode(String code) {
        return new FeatureMatcher<OfferInfo, String>(equalTo(code), "code should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                return offerInfo.getStatus();
            }
        };
    }

    public static Matcher<OfferInfo> hasStatus(String status) {
        return new FeatureMatcher<OfferInfo, String>(equalTo(status), "status should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                try {
                    return offerInfo.getOffer().getStatus();
                } catch (Exception e) {
                    return "can't get offer status";
                }
            }
        };
    }

    public static Matcher<OfferInfo> hasPromotion() {
        return new FeatureMatcher<OfferInfo, String>(equalTo("active"), "promotion status should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                try {
                    return offerInfo.getOffer().getPromotion().getStatus();
                } catch (Exception e) {
                    return "can't get offer status";
                }
            }
        };
    }
}

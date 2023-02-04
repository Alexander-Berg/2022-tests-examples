package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;
import ru.auto.test.api.realty.offer.userid.offerid.responses.Photo;

import java.util.List;

import static java.util.stream.Collectors.toList;
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

    public static Matcher<OfferInfo> hasPremium() {
        return new FeatureMatcher<OfferInfo, String>(equalTo("active"), "premium status should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                try {
                    return offerInfo.getOffer().getPremium().getStatus();
                } catch (Exception e) {
                    return "can't get offer status";
                }
            }
        };
    }

    public static Matcher<OfferInfo> hasRaising() {
        return new FeatureMatcher<OfferInfo, String>(equalTo("active"), "raising status should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                try {
                    return offerInfo.getOffer().getRaising().getStatus();
                } catch (Exception e) {
                    return "can't get offer status";
                }
            }
        };
    }

    public static Matcher<OfferInfo> hasTurboSale() {
        return new FeatureMatcher<OfferInfo, String>(equalTo("active"), "turbosale status should be", "actual") {
            @Override
            protected String featureValueOf(OfferInfo offerInfo) {
                try {
                    return offerInfo.getOffer().getTurboSale().getStatus();
                } catch (Exception e) {
                    return "can't get offer status";
                }
            }
        };
    }

    public static Matcher<OfferInfo> hasPhoto(Matcher matcher) {
        return new TypeSafeMatcher<OfferInfo>() {

            private List<String> urls;

            @Override
            public void describeTo(Description description) {
                description.appendText("Список фотографий оффера ").appendDescriptionOf(matcher);
            }

            @Override
            protected boolean matchesSafely(OfferInfo offerInfo) {
                urls = offerInfo.getPhoto().stream().map(Photo::getUrl).collect(toList());
                return matcher.matches(urls);
            }

            @Override
            protected void describeMismatchSafely(OfferInfo item, Description mismatchDescription) {
                mismatchDescription.appendText("список фотографий был ")
                        .appendValueList("«", ",", "»", urls);
            }
        };
    }
}

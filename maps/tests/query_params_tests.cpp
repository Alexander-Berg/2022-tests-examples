#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/auth/include/user_info.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/http/include/url.h>

#include <maps/libs/introspection/include/comparison.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::http {

using maps::introspection::operator==;

} // namespace maps::http

namespace maps::auth::tests {

Y_UNIT_TEST_SUITE(query_params) {

Y_UNIT_TEST(test_params_serialization)
{
    constexpr std::string_view baseUrl = "http://hostname/";

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestBoundPhones().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?getphones=bound"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestAllPhones().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?getphones=all"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestAllEmails().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?getemails=all"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams()
            .requestAllPhones({PhoneAttributeType::Number, PhoneAttributeType::Created})
            .updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?getphones=all&phone_attributes=1%2C2"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams()
            .requestAllEmails({EmailAttributeType::Address, EmailAttributeType::Created})
            .updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?getemails=all&email_attributes=1%2C2"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestUserTicket().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?get_user_ticket=yes"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestDefaultEmailsInAddressList().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?emails=getdefault"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestYandexEmailsInAddressList().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?emails=getyandex"));
    }

    {
        http::URL url(baseUrl);
        BlackboxQueryParams().requestAllEmailsInAddressList().updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?emails=getall"));
    }

    {
        http::URL url(baseUrl);
        const std::string address{"sample@yandex.ru"};
        BlackboxQueryParams().requestTestEmailBelonging(address).updateBlackboxUrlQueryParams(url);
        EXPECT_EQ(url, http::URL("http://hostname/?emails=testone&addrtotest=sample%40yandex.ru"));
    }
}

}

} // namespace maps::auth::tests

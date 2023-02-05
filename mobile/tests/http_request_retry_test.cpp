#include <yandex/maps/navikit/http_request_retry.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit::test {
using namespace ::yandex::maps::runtime::network;

struct Fixture {
    HttpResponse okResponse{HttpStatus::OK, HttpHeaders(), "body"};
    HttpResponse fatalError{HttpStatus::BAD_REQUEST, HttpHeaders(), "body"};
    HttpResponse retriableError{HttpStatus::BAD_GATEWAY, HttpHeaders(), "body"};
};

BOOST_AUTO_TEST_SUITE(HttpRequestRetryTest)

BOOST_FIXTURE_TEST_CASE(testOkResponse, Fixture)
{
    int callCount = 0;
    auto actual = httpRequest([&] {
        ++callCount;
        return okResponse;
    });
    BOOST_REQUIRE(okResponse.status == actual.status);
    BOOST_REQUIRE(callCount == 1);
}

BOOST_FIXTURE_TEST_CASE(testRetry, Fixture)
{
    int callCount = 0;
    BOOST_REQUIRE_THROW(
        httpRequest([&] {
            ++callCount;
            return retriableError;
        }, " my awesome task #", 2, " failed"),
        HttpException);
    BOOST_REQUIRE(callCount > 1);
}

BOOST_FIXTURE_TEST_CASE(testFatal, Fixture)
{
    int callCount = 0;
    BOOST_REQUIRE_THROW(
        httpRequest([&] {
            ++callCount;
            return fatalError;
        }),
        HttpException);
    BOOST_REQUIRE(callCount == 1);
}

BOOST_AUTO_TEST_SUITE_END()
}
#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/utils/serialization/common/time+serialization.h>

namespace yandex {
namespace metrokit {
namespace utils {
namespace serialization {

namespace {

auto parseDateTime(const std::string& dateTimeString) -> Result<TimePoint, ParsingError> {
    return parse<TimePoint, format::ISO8601>(document::Node { dateTimeString });
}

auto assertEquivalent(const std::string& dateTimeString, double expected) -> void {
    const auto result = parseDateTime(dateTimeString);
    BOOST_REQUIRE(result.isOk());
    const auto& resultTime = result.okValue().time_since_epoch().count();
    BOOST_CHECK_EQUAL(resultTime, expected);
}

auto assertMalformed(const std::string& dateTimeString) -> void {
    const auto result = parseDateTime(dateTimeString);
    BOOST_REQUIRE(result.isError());
}

} // namespace

BOOST_AUTO_TEST_CASE(datetime_parsing_sanity_check) {
    assertEquivalent("2019-02-16T01:00:00+00:00", 1550278800.0);
    assertEquivalent("2019-02-16T04:00:00+03:00", 1550278800.0);
    assertEquivalent("2019-02-16T01:00:00+03:00", 1550268000.0);
    assertEquivalent("2019-02-16T01:00:00-03:00", 1550289600.0);
    assertEquivalent("2019-02-16T01:00:00Z", 1550278800.0);
    assertEquivalent("2019-02-16 01:00:00+00:00", 1550278800.0);
    assertEquivalent("2019-02-16T01:00:00+14:00", 1550228400.0);
    assertMalformed("2019-02-16T01:00:00+14:01");
    assertEquivalent("2019-02-16T01:00:00-12:00", 1550322000.0);
    assertMalformed("2019-02-16T01:00:00-12:01");
    assertEquivalent("2019-02-16T01:00:00", 1550278800.0);
    assertMalformed("abracadabra");
}

}}}}

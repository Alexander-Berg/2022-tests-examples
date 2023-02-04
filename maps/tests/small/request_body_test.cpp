#include <maps/indoor/long-tasks/src/evotor-sender/lib/impl.h>

#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::evotor::sender::impl::tests {

Y_UNIT_TEST_SUITE(evotor_sender_impl)
{

Y_UNIT_TEST(generate_request_body)
{
    TransmitterData txData {
        .sourceId = "sourceId",
        .lon = 37.5,
        .lat = 55.5,
        .indoorLevelUniversalId = "EG",
        .indoorName = "Hbf",
    };

    auto test = json::Value::fromString(transmitterToJson(txData));
    auto reference = json::Value::fromString(R"end(
        {
            "lon":37.5,
            "lat":55.5,
            "levelId":"EG",
            "name":"Hbf"
        }
    )end");

    UNIT_ASSERT_EQUAL(test, reference);
}

}

} // namespace maps::libs::evotor::sender::impl::tests

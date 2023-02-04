#include <maps/indoor/long-tasks/src/barrier-feedback/lib/barrier.h>

#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

namespace maps::mirc::barriers::test {

Y_UNIT_TEST_SUITE(small_test_mirc_barriers)
{

Y_UNIT_TEST(generate_barrier_json)
{
    Barrier barrier{
        .id = "1",
        .ll = { 37.7, 55.5 },
        .level = "1WC",
        .track = "кратчайший в туалет",
        .comment = "туалет платный, не пройти",
        .ticket = "DARIA-5006",
    };

    auto test = json::Value::fromString(generateFeedbackTaskDefinition(barrier));
    auto reference = json::Value::fromString(R"raw(
        {
            "workflow": "task",
            "source": "radiomaps-barrier",
            "position": {
                "coordinates": [37.7, 55.5],
                "type": "Point"
            },
            "type": "indoor-barrier",
            "indoorLevel": "1WC",
            "description": {
                "i18nKey": "feedback-descriptions:indoor-barrier",
                "i18nParams": {
                    "track": "кратчайший в туалет",
                    "stIssue": "DARIA-5006",
                    "userComment": "туалет платный, не пройти"
                }
            },
            "hidden": true
        }
    )raw");

    UNIT_ASSERT_EQUAL(test, reference);
}

}

} // namespace maps::mirc::barriers::test

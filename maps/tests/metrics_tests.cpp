#include <maps/infra/quotateka/agent/include/metrics.h>
#include <maps/infra/quotateka/agent/tests/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

using std::chrono::steady_clock;

class ManualClock
{
    steady_clock::time_point t_;
public:
    explicit ManualClock(steady_clock::time_point t) : t_(t) {}

    steady_clock::time_point& operator()() { return t_; }
};

Y_UNIT_TEST_SUITE(moving_sum_test) {

Y_UNIT_TEST(accumulate_and_extract)
{
    MovingSum sum;
    steady_clock::time_point now;

    for (int i = 0; i <= 100; ++i) {
        sum.push(5, now);
        now += std::chrono::milliseconds(100);
        sum.push(5, now);
        now += std::chrono::milliseconds(800);
        if (i == 5) {
            // Expect one more bucket value for the first extraction
            EXPECT_EQ(sum.extractValue(now), 60);
        } else if (i && i % 5 == 0) {
            // Expect equal sum for regular extraction
            EXPECT_EQ(sum.extractValue(now), 50);
        }
        now += std::chrono::milliseconds(100);
    }

    for (int i = 0; i < 10; ++i) {
        now += std::chrono::seconds(1);
        sum.push(5, now);
    }
    // Expect sum for the last 7 seconds in the worst case
    EXPECT_EQ(sum.extractValue(now), 35);
}

}  // Y_UNIT_TEST_SUITE(moving_sum_test)

Y_UNIT_TEST_SUITE(access_metrics_test) {

Y_UNIT_TEST(accumulate_extrace)
{
    ManualClock clock(steady_clock::now());
    AccessMetrics accumulator(std::ref(clock));
    accumulator.setProviderId("providerX");

    using Status = AccessMetrics::Status;

    accumulator.pushTvm(Status::Ok, {111}, "resourceA", 1);
    accumulator.pushTvm(Status::Ok, {111}, "resourceA", 10);
    accumulator.pushTvm(Status::Ok, {222}, "resourceA", 2);
    accumulator.pushTvm(Status::Ok, {222}, "resourceB", 20);
    accumulator.pushAux(Status::Ok, "custom", "resourceA", 2);  // custom id
    accumulator.pushAux(Status::Ok, "custom", "resourceA", 3);  // custom id
    accumulator.pushTvm(Status::Reject, {111}, "resourceA", 1);
    accumulator.pushTvm(Status::Reject, {222}, "resourceB", 2);
    accumulator.pushAux(Status::Reject, "custom", "resourceA", 11);  // anon reject

    // Increment time to extract values from previous second
    clock() += std::chrono::seconds(1);
    EXPECT_EQ(
        toString(accumulator.extract()),
        R"([["tvm429_custom@providerX/resourceA_ammm",11],)"  // custom id rejects
        R"(["tvm429_222@providerX/resourceB_ammm",2],)"
        R"(["tvmok_111@providerX/resourceA_ammm",11],)"
        R"(["tvmok_222@providerX/resourceB_ammm",20],)"
        R"(["tvmok_222@providerX/resourceA_ammm",2],)"
        R"(["tvm429_111@providerX/resourceA_ammm",1],)"
        R"(["tvmok_custom@providerX/resourceA_ammm",5]])"    // custom id successes
    );

    // Extraction clears state
    EXPECT_EQ(toString(accumulator.extract()), "[]");

    // Push more values
    accumulator.pushTvm(Status::Ok, {111}, "resourceA", 1);
    accumulator.pushTvm(Status::Ok, {153}, "resourceC", 153);
    accumulator.pushTvm(Status::Reject, {153}, "resourceC", 123);

    clock() += std::chrono::seconds(1);
    EXPECT_EQ(
        toString(accumulator.extract()),
        R"([["tvm429_153@providerX/resourceC_ammm",123],)"
        R"(["tvmok_153@providerX/resourceC_ammm",153],)"
        R"(["tvmok_111@providerX/resourceA_ammm",1]])"
    );
}

Y_UNIT_TEST(accumulate_extrace_scoped)
{
    ManualClock clock(steady_clock::now());
    AccessMetrics accumulator(std::ref(clock));
    accumulator.setProviderId("providerX");

    using Status = AccessMetrics::Status;

    accumulator.pushTvm(Status::Ok, {111, "slug1"}, "resourceA", 1);
    accumulator.pushTvm(Status::Ok, {111, "slug2"}, "resourceA", 10);

    // Increment time to extract values from previous second
    clock() += std::chrono::seconds(1);
    EXPECT_EQ(
        toString(accumulator.extract()),
        R"([["tvmok_111_slug2@providerX/resourceA_ammm",10],)"
        R"(["tvmok_111_slug1@providerX/resourceA_ammm",1]])"
    );
}

}  // Y_UNIT_TEST_SUITE(tvm_metrics_test)

} // namespace maps::quotateka::tests

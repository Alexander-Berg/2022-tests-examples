#include <maps/infra/ratelimiter2/common/include/test_helpers.h>

#include <maps/infra/ratelimiter2/common/include/limit_info.h>
#include <maps/infra/ratelimiter2/proto/limits.pb.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace maps::rate_limiter2;

using std::chrono::system_clock;

Y_UNIT_TEST_SUITE(leaky_bucket_test) {

Y_UNIT_TEST(LimitInfo_ctor)
{
    maps::proto::rate_limiter2::ResourceLimit protoLimit;

    {   // test default proto
        LimitInfo limit(protoLimit);
        EXPECT_TRUE(limit == LimitInfo({.rate = 0, .unit = 1}, 0));
        EXPECT_TRUE(limit.isForbidden());
    }

    protoLimit.set_rps(100);
    protoLimit.set_burst(200);
    protoLimit.set_unit(153);
    {
        LimitInfo limit(protoLimit);
        EXPECT_TRUE(limit == LimitInfo({.rate = 100, .unit = 153}, 200));
        EXPECT_FALSE(limit.isForbidden());
    }

    // default is not forbidding
    EXPECT_FALSE(LimitInfo::UNDEFINED.isForbidden());
}

// Leaky Bucket algorithm tests

Y_UNIT_TEST(rps)
{
    auto limit = LimitInfo({.rate = 2, .unit = 1},  5);  // 2 requests per second, burst 5

    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom

    system_clock::time_point now;

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 5); // expect +burst success

    now += std::chrono::seconds(1);

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 7);

    now += std::chrono::seconds(1);

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 9); // 2 more success attempts

    now += std::chrono::seconds(10); // meaning there where no requests for 10 sec - expect burst after that

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 14); // +burst
}

Y_UNIT_TEST(fillBucket)
{
    auto limit = LimitInfo({.rate = 2, .unit = 1},  5);  // 2 requests per second, burst 5

    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom

    system_clock::time_point now;

    for (int i = 0; i < 50; ++i) { // 100 request attempts
        fillLeakyBucket(limit, now, 3, counter, bucket);
    }
    EXPECT_EQ(counter, 150); // expect +burst success

    now += std::chrono::seconds(73);

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 151);

    now += std::chrono::seconds(1);

    passLeakyBucket(limit, now, 1, counter, bucket);
    EXPECT_EQ(counter, 152);
}

Y_UNIT_TEST(checkBucket)
{
    auto limit = LimitInfo({.rate = 2, .unit = 1},  5);  // 2 requests per second, burst 5

    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom

    system_clock::time_point now;

    for (int i = 0; i < 50; ++i) { // 50 request attempts
        bool check3 = checkLeakyBucket(limit, now, 3, counter, bucket);
        EXPECT_TRUE(check3); // weight(3) < burst(5)
    }
    fillLeakyBucket(limit, now, 3, counter, bucket);    // fill 3
    EXPECT_EQ(counter, 3);

    for (int i = 0; i < 50; ++i) { // 50 request attempts
        bool check3 = checkLeakyBucket(limit, now, 3, counter, bucket);
        EXPECT_FALSE(check3);   // counter(3) + weight(3) > burst(5) 
        bool check2 = checkLeakyBucket(limit, now, 2, counter, bucket);
        EXPECT_TRUE(check2);    // but counter(3) + weight(2) <= burst(5)
    }
    EXPECT_EQ(counter, 3);  // counter doesn't change after check

    now += std::chrono::seconds(1);

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        bool check3 = checkLeakyBucket(limit, now, 3, counter, bucket);
        EXPECT_TRUE(check3);   // counter(3) + weight(3) <= limit(7)
    }
    EXPECT_EQ(counter, 3);  // counter doesn't change after check

    now += std::chrono::seconds(1);

    passLeakyBucket(limit, now, 1, counter, bucket);
    EXPECT_EQ(counter, 4);
}

Y_UNIT_TEST(rpm)
{
    // 100 requests per minute, burst 20
    auto limit = LimitInfo({.rate = 100, .unit = 60}, 20);

    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom

    system_clock::time_point now;

    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, 20);    // expect burst success at start

    now += std::chrono::seconds(5);
    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_TRUE(counter > 20 && counter < 30); // + 1/12 of limit

    now += std::chrono::seconds(15);
    for (int i = 0; i < 100; ++i) { // 100 request attempts
        passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_TRUE(counter > 40 && counter < 50); // + burst

    // constant load for 60sec
    for (auto start = now;
         std::chrono::duration_cast<std::chrono::seconds>(now - start).count() < 60;
         now += std::chrono::seconds(1)) {
        for (int i = 0; i < 100; ++i) { // 100 attempts
            passLeakyBucket(limit, now, 1, counter, bucket);
        }
    }
    EXPECT_TRUE(counter > 140 && counter < 150);

    now += std::chrono::seconds(30);
    auto nkeep = counter;
    for (int i = 0; i < 100; ++i) { // 100 request attempts
        counter += passLeakyBucket(limit, now, 1, counter, bucket);
    }
    EXPECT_EQ(counter, nkeep + 20); // +burst

    // constant load for 30sec
    for (auto start = now;
         std::chrono::duration_cast<std::chrono::seconds>(now - start).count() < 30;
         now += std::chrono::seconds(1)) {
        for (int i = 0; i < 100; ++i) { // 100 attempts
            passLeakyBucket(limit, now, 1, counter, bucket);
        }
    }
    EXPECT_TRUE(counter > 210 && counter < 220); // gradual success count grow
}


Y_UNIT_TEST(resize)
{
    system_clock::time_point now = std::chrono::system_clock::now();

    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom

    auto limit = LimitInfo({.rate = 5, .unit = 1}, 5);

    EXPECT_TRUE(passLeakyBucket(limit, now, 5, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limit, now, 5, counter, bucket)); //exceeded

    // increase limit
    auto oldLimit = limit;
    limit = LimitInfo({.rate = 10, .unit = 1}, 10);
    resizeLeakyBucket(oldLimit, limit, now, bucket);

    EXPECT_TRUE(passLeakyBucket(limit, now, 5, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limit, now, 5, counter, bucket)); //exceeded
    now += std::chrono::seconds(1);
    EXPECT_TRUE(passLeakyBucket(limit, now, 10, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limit, now, 1, counter, bucket)); //exceeded
    now += std::chrono::seconds(1);
    EXPECT_TRUE(passLeakyBucket(limit, now, 5, counter, bucket));

    // decrease limit
    oldLimit = limit;
    limit = LimitInfo({.rate = 2, .unit = 1}, 2);
    resizeLeakyBucket(oldLimit, limit, now, bucket);

    //exceeded for now + 2secs
    EXPECT_FALSE(passLeakyBucket(limit, now, 2, counter, bucket)); //exceeded
    now += std::chrono::seconds(1);
    EXPECT_FALSE(passLeakyBucket(limit, now, 2, counter, bucket));
    now += std::chrono::seconds(1);
    EXPECT_FALSE(passLeakyBucket(limit, now, 2, counter, bucket));

    // now ok
    now += std::chrono::seconds(1);
    EXPECT_TRUE(passLeakyBucket(limit, now, 2, counter, bucket));
}


Y_UNIT_TEST(default_limit_to_specific)
{
    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom
    system_clock::time_point now = std::chrono::system_clock::now();

    EXPECT_TRUE(passLeakyBucket(LimitInfo::UNDEFINED, now, 100, counter, bucket));
    EXPECT_TRUE(passLeakyBucket(LimitInfo::UNDEFINED, now, 10, counter, bucket));

    // resize from default limit
    auto limitRPS = LimitInfo({.rate = 100, .unit = 1}, 100);
    resizeLeakyBucket(LimitInfo::UNDEFINED, limitRPS, now, bucket);
    // oops exceeded
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket)); // exceeded

    // but time passes and we good again
    now += std::chrono::seconds(1);
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 90, counter, bucket));
    // exceed
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));
}

Y_UNIT_TEST(to_default_and_back)
{
    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom
    system_clock::time_point now = std::chrono::system_clock::now();

    auto limitRPS = LimitInfo({.rate = 100, .unit = 1}, 100);
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 10, counter, bucket));
    counter += 1000000;   // huge delta out of the blue
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));

    // resize to default limit
    resizeLeakyBucket(limitRPS, LimitInfo::UNDEFINED, now, bucket);
    // not exceeded now
    EXPECT_TRUE(passLeakyBucket(LimitInfo::UNDEFINED, now, 100, counter, bucket));

    // time passed
    now += std::chrono::seconds(1);
    EXPECT_TRUE(passLeakyBucket(LimitInfo::UNDEFINED, now, 10, counter, bucket));

    // resize back from default
    resizeLeakyBucket(LimitInfo::UNDEFINED, limitRPS, now, bucket);
    // expect not exceeded
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 80, counter, bucket));
}


Y_UNIT_TEST(switch_rps_to_rph)
{
    int64_t counter = 0;
    int64_t bucket = 0;  // bucket bottom
    system_clock::time_point now(std::chrono::seconds(100500));

    auto limitRPS = LimitInfo({.rate = 10, .unit = 1}, 10);  // limit requests per second
    auto limitRPH = LimitInfo({.rate = 10, .unit = 60*60}, 10);  // limit requests per hour
    // start with RPS
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 10, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));

    now += std::chrono::seconds(1);

    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 10, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));

    // switch to RPH
    resizeLeakyBucket(limitRPS, limitRPH, now, bucket);

    // reject since current rate exceeds new limit
    EXPECT_FALSE(passLeakyBucket(limitRPH, now, 1, counter, bucket));
    now += std::chrono::seconds(10);
    // reject - not enough time passed
    EXPECT_FALSE(passLeakyBucket(limitRPH, now, 1, counter, bucket));

    now += std::chrono::seconds(30*60); // 1/2 of hour passed

    EXPECT_TRUE(passLeakyBucket(limitRPH, now, 5, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limitRPH, now, 1, counter, bucket));

    now += std::chrono::seconds(60*60); // full hour passed

    EXPECT_TRUE(passLeakyBucket(limitRPH, now, 10, counter, bucket));
    EXPECT_FALSE(passLeakyBucket(limitRPH, now, 1, counter, bucket));

    // back to RPS
    resizeLeakyBucket(limitRPH, limitRPS, now, counter);

    // reject since current rate exceeds new limit
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));

    now += std::chrono::seconds(1);

    fillLeakyBucket(limitRPS, now, 3, counter, bucket);
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 2, counter, bucket));
    fillLeakyBucket(limitRPS, now, 2, counter, bucket);
    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 2, counter, bucket));
    fillLeakyBucket(limitRPS, now, 1, counter, bucket);
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));

    now += std::chrono::seconds(1);

    EXPECT_TRUE(passLeakyBucket(limitRPS, now, 1, counter, bucket));
    fillLeakyBucket(limitRPS, now, 200, counter, bucket);
    EXPECT_FALSE(passLeakyBucket(limitRPS, now, 1, counter, bucket));
}


// limit selection tests

Y_UNIT_TEST(AccessLimiter_selectLimit)
{
    LimitsRegistry registry( {
        { makeClientHash("super.client.1"), {
            {"", LimitInfo({.rate = 50000, .unit = 1}, 1000)}  // super-limit for client1
        }},
        { makeClientHash("client.2"), {
            {"resource.1", LimitInfo({.rate = 1000, .unit = 1*60*60}, 1000)},
            {"resource.2", LimitInfo({.rate = 300, .unit = 24*60*60}, 300) }
        }},
        { makeClientHash("client.3"), {
            {"resource.2", LimitInfo({.rate = 500, .unit = 1}, 500)},
            {"resource.3", LimitInfo({.rate = 555, .unit = 60}, 333)}
        }},
        { makeClientHash(""), { // 'anybody' limit
            {"resource.3", LimitInfo({.rate = 100, .unit = 1}, 500)}
        }}
    } );

    {   // invalid case
        auto& lim =registry.select(makeClientHash(""), "");
        EXPECT_TRUE(&lim == &LimitInfo::UNDEFINED);
    }

    {   // 'anybody' limit
        auto& lim = registry.select(makeClientHash("nonspecific.client"), "resource.3");
        EXPECT_TRUE(lim == LimitInfo({.rate = 100, .unit = 1}, 500));
    }
    {   // limit not found
        auto& lim = registry.select(makeClientHash("nonspecific.client"), "resource.2");
        EXPECT_TRUE(&lim == &LimitInfo::UNDEFINED);
    }
    {   // super limit
        auto& limit = registry.select(makeClientHash("super.client.1"), "");
        EXPECT_TRUE(limit == LimitInfo({.rate = 50000, .unit = 1}, 1000));
    }
    {   // specific limit
        auto& lim = registry.select(makeClientHash("client.2"), "resource.2");
        EXPECT_TRUE(lim == LimitInfo({.rate = 300, .unit = 24*60*60}, 300));
    }
    {   // specific limit priority
        auto& lim = registry.select(makeClientHash("client.3"), "resource.3");
        EXPECT_TRUE(lim == LimitInfo({.rate = 555, .unit = 60}, 333));
    }
}

} // Y_UNIT_TEST_SUITE

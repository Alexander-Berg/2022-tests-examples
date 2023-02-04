#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/analyzer/tools/mapbox_quality/lib/operations.h>

namespace mq = maps::analyzer::tools::mapbox_quality;

Y_UNIT_TEST_SUITE(test_operations)
{
    Y_UNIT_TEST(test_typical_speeds)
    {
        mq::TypicalSpeedData speedValues;
        std::iota(speedValues.begin(), speedValues.end(), 0);
        const mq::TypicalLine typical {
            0u, 1u, std::move(speedValues)
        };
        const boost::posix_time::ptime utcTime{
            boost::gregorian::date{2020, 4, 16}, // thursday
            boost::posix_time::hours{11} + boost::posix_time::minutes{37}
        };
        int tzOffset = 3600;
        const auto res = mq::getTypicalLine(typical, utcTime, tzOffset);
        const auto expectedSpeed = speedValues[(1 + 3) * 24 * 12 + 11 * 12 + 7 + 3600 / 300];
        EXPECT_EQ(res.source, 0u);
        EXPECT_EQ(res.target, 1u);
        EXPECT_EQ(res.speedData, expectedSpeed);
    }
}

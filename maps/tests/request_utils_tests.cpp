#include <maps/infopoint/lib/misc/request_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;

TEST(request_utils_tests, testParseTimeUtc)
{
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("20141020T084858Z", "time")),
        1413794938
    );

    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("2014-10-20T08:48:58Z", "time")),
        1413794938
    );
}

TEST(request_utils_tests, testParseTimeLocal)
{
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("20141020T084858+0400", "time")),
        1413794938 - 4 * 3600
    );
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("2014-10-20T08:48:58+04:00", "time")),
        1413794938 - 4 * 3600
    );
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("2014-10-20T08:48:58+04", "time")),
        1413794938 - 4 * 3600
    );
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("20141020T084858-0030", "time")),
        1413794938 + 30 * 60
    );
    EXPECT_EQ(
        std::chrono::system_clock::to_time_t(
            parseTime("20141020T084858-00:30", "time")),
        1413794938 + 30 * 60
    );
}

TEST(request_utils_tests, testParseTimeThrows)
{
    EXPECT_THROW(
        std::chrono::system_clock::to_time_t(
            parseTime("2014-1020T08:48:58+0400", "time")),
        maps::RuntimeError
    );
}

TEST(request_utils_tests, testParseIntVectorException)
{
    EXPECT_THROW(
        parseCommaSeparatedVector<int>("34d"),
        boost::bad_lexical_cast
    );
}

TEST(request_utils_tests, testParseIntVectorEmpty)
{
    std::vector<int> expected {};
    std::vector<int> actual = parseCommaSeparatedVector<int>("");
    EXPECT_EQ(actual, expected);
}

TEST(request_utils_tests, testParseIntVectorSimple)
{
    std::vector<int> expected {33, 34};
    std::vector<int> actual = parseCommaSeparatedVector<int>("33,34");
    EXPECT_EQ(actual, expected);
}

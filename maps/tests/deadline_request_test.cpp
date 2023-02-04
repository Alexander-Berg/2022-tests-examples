#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <chrono>
#include <map>

namespace yacare::tests {

using maps::http::URL;
using maps::http::GET;
using namespace std::chrono_literals;

namespace {

const std::string START_TIME_HEADER_NAME = "X-Ya-Start-Time";
const std::string TIMEOUT_HEADER_NAME = "X-Ya-Backend-Timeout-Ms";

maps::http::MockResponse callPingWithDeadline(std::map<std::string, std::string> headers)
{
    maps::http::MockRequest mockRequest(GET, URL("http://localhost/mtroute/ping"));
    mockRequest.headers.insert(headers.begin(), headers.end());
    return performTestRequest(mockRequest);
}

std::pair<std::string, std::string> startHeader(std::chrono::milliseconds startDelta)
{
    auto timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch() + startDelta).count();

    std::string startTime = std::to_string(static_cast<double>(timestamp) / 1000.);
    return {START_TIME_HEADER_NAME, startTime};
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(test_request_timeout_suite)
{

Y_UNIT_TEST(test_request_normal_behaviour)
{
    // Test that requests that met deadline return 200
    std::pair<std::chrono::milliseconds, std::string> deadlines[] {
        {0ms, "1000"},
        {-100ms, "500"},
        {-200ms, "800"},
        {-500ms, "1000"},
        {-1000ms, "2000"}
    };
    for (auto [startDelta, timeout] : deadlines) {
        EXPECT_EQ(callPingWithDeadline({startHeader(startDelta), {TIMEOUT_HEADER_NAME, timeout}}).status, 200)
            << "With deadline:\n"
               "Start time delta: " << startDelta.count() << "\n"
            << TIMEOUT_HEADER_NAME << ": " << timeout;
    }
}

Y_UNIT_TEST(test_request_timeout)
{
    // Test that requests that didn't meet deadline return 504
    std::pair<std::chrono::milliseconds, std::string> deadlines[] {
        {-1000ms, "1000"},
        {-800ms, "500"},
        {-2000ms, "1200"},
        {-3000ms, "600"},
        {-10000ms, "5000"}
    };
    for (auto [startDelta, timeout] : deadlines) {
        EXPECT_EQ(callPingWithDeadline({startHeader(startDelta), {TIMEOUT_HEADER_NAME, timeout}}).status, 504)
            << "With deadline:\n"
               "Start time delta: " << startDelta.count() << "\n"
            << TIMEOUT_HEADER_NAME << ": " << timeout;
    }
}

Y_UNIT_TEST(test_broken_start_time)
{
    std::pair<std::string, std::string> deadlines[] {
        {"3k", "1000"},
        {"-1000", "500"},
        {"", "500"},
        {startHeader(-3h).second, "800"},
        {startHeader(2h).second, "600"}
    };
    for (auto [start, timeout] : deadlines) {
        EXPECT_EQ(callPingWithDeadline({{START_TIME_HEADER_NAME, start}, {TIMEOUT_HEADER_NAME, timeout}}).status, 200)
            << "With deadline:\n"
            << START_TIME_HEADER_NAME << ": " << start << "\n"
            << TIMEOUT_HEADER_NAME << ": " << timeout;
    }
}

Y_UNIT_TEST(test_broken_backend_timeout)
{
    std::pair<std::chrono::milliseconds, std::string> deadlines[] {
        {0ms, "Kek"},
        {-100ms, "-500"},
        {-2000ms, "13k"},
        {-1000ms, "33333O"}, // last symbol is letter
        {500ms, ""}
    };
    for (auto [startDelta, timeout] : deadlines) {
        auto response = callPingWithDeadline({startHeader(startDelta), {TIMEOUT_HEADER_NAME, timeout}});
        EXPECT_EQ(response.status, 200)
            << "With deadline:\n"
               "Start time delta: " << startDelta.count() << "\n"
            << TIMEOUT_HEADER_NAME << ": " << timeout;
    }
}

Y_UNIT_TEST(test_missing_start_time)
{
    EXPECT_EQ(callPingWithDeadline({{TIMEOUT_HEADER_NAME, "1000"}}).status, 200);
}

Y_UNIT_TEST(test_missing_backend_timeout)
{
    EXPECT_EQ(callPingWithDeadline({startHeader(0s)}).status, 200);
}

Y_UNIT_TEST(test_missing_deadline)
{
    EXPECT_EQ(callPingWithDeadline({}).status, 200);
}

} //Y_UNIT_TEST_SUITE

} //namespace yacare::tests

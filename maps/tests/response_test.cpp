#include <maps/infra/yacare/include/error.h>
#include <maps/infra/yacare/include/response.h>
#include <maps/libs/common/include/type_traits.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

static_assert(maps::is_ostreamable_to<yacare::Response, std::string>);
static_assert(maps::is_ostreamable_to<std::ostream, maps::Exception>);
static_assert(!maps::is_ostreamable_to<yacare::Response, maps::Exception>);
static_assert(!maps::is_ostreamable_to<yacare::Response, const maps::RuntimeError&>);
static_assert(maps::is_ostreamable_to<yacare::Response, yacare::errors::NotFound>);
static_assert(maps::is_ostreamable_to<yacare::Response, const yacare::Error&>);

Y_UNIT_TEST_SUITE(response_streaming_api) {
Y_UNIT_TEST(exception_backtrace_leak)
{
    yacare::Response response;
    std::ostringstream ostream;
    try {
        throw yacare::errors::BadRequest{"Exception with backtrace"};
    } catch (const yacare::errors::BadRequest& ex) {
        // response implicilty casts to it's base class std::ostream
        // after the first operator<< invocation,
        // std::ostream& operator<<(std::ostream&, const maps::Exception&) is selected for the second
        response << "Some preamble:\n" << ex;
        ostream << "Some preamble:\n" << ex;
    }

    // The only guaranteed backtrace element is the brace-enclosed function address in hex format
    const auto hasBacktrace{testing::ContainsRegex(R"(\[0x[a-fA-F0-9]+\])")};
    EXPECT_THAT(ostream.str(), hasBacktrace);
    EXPECT_THAT(response.body(), testing::Not(hasBacktrace));
}
} // Y_UNIT_TEST_SUITE(response_streaming_api)

#include <maps/renderer/staticapi/yacare/tests/test_utils/test_utils.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/hook/hook.h>

namespace maps::staticapi::tests {

namespace {
std::optional<http::MockHandle> g_forceHttpMocks;
} // namespace

Y_TEST_HOOK_BEFORE_RUN(initStaticApi)
{
    initHandlers();

    // To enable mocks mode in http lib and prevent performing
    // requests via network a mock must be created and holded.
    g_forceHttpMocks = http::addMock(
        "http://unused",
        [](const http::MockRequest& request) -> http::MockResponse {
            throw LogicError() << request.url;
        });
}

Y_TEST_HOOK_AFTER_RUN(cleanupStaticApi)
{
    g_forceHttpMocks.reset();
}

} // namespace maps::staticapi::tests

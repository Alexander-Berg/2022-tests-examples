#include <yandex/maps/mirc/unittest/local_server.h>
#include <maps/libs/http/include/http.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <sstream>

namespace maps::mirc::unittest::tests {

namespace {

const char* SERVER_BINARY_PATH
        = "maps/indoor/libs/unittest/tests/medium/pyserver/pyserver";
const char* LOCAL_HOST = "127.0.0.1";

std::string pingUrl(uint16_t port)
{
    std::ostringstream url;
    url << "http://" << LOCAL_HOST << ":" << port << "/ping";
    return url.str();
}

}

Y_UNIT_TEST_SUITE(local_server) {

Y_UNIT_TEST(start_server)
{
    const TestServer testServer(
        [](uint16_t port) {
            return process::Command(
                   {BinaryPath(SERVER_BINARY_PATH),
                   LOCAL_HOST,
                   std::to_string(port)});
        });

    http::Client httpClient;
    http::Request request(httpClient, http::GET, pingUrl(testServer.getPort()));
    auto response = request.perform();
    EXPECT_EQ(response.status(), 200);
}

} // Y_UNIT_TEST_SUITE


} //namespace maps::mrc::unittest::tests

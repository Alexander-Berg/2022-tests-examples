#include <yandex_io/libs/http_client/http_client.h>

#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <utility>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;

class RedirectHostFixture: public QuasarUnitTestFixture {
public:
    RedirectHostFixture() {
        /* Create Mock Backend for Metrica Startup requests */
        mockBackend_.onHandlePayload = std::bind(&RedirectHostFixture::handleRequest,
                                                 this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

        mockBackendUrl_ = "http://localhost:" + std::to_string(mockBackend_.start(getPort()));
    };

protected:
    void handleRequest(const TestHttpServer::Headers& header, const std::string& /*payload*/,
                       TestHttpServer::HttpConnection& handler) {
        const std::string redirectParam = "/redirect";
        if (header.resource == redirectParam) {
            handler.doReplay(200, "application/json", "{}");
        } else {
            const std::string redirectHost = mockBackendUrl_ + redirectParam;
            const std::vector<std::pair<std::string, std::string>> extraHeaders = {
                std::make_pair("Location", redirectHost),
            };
            handler.doReplay(301, "application/json", "{}", extraHeaders);
        }
    }

    /* Data */
protected:
    std::string mockBackendUrl_;
    TestHttpServer mockBackend_;
};

class UrlDownloadFixture: public QuasarUnitTestFixture {
public:
    UrlDownloadFixture() {
        mockBackend_.onHandlePayload = std::bind(&UrlDownloadFixture::handleRequest,
                                                 std::placeholders::_1, std::placeholders::_2,
                                                 std::placeholders::_3);

        mockBackendUrl_ = "http://localhost:" + std::to_string(mockBackend_.start(getPort()));
    };

protected:
    static void handleRequest(const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/,
                              TestHttpServer::HttpConnection& handler) {
        handler.doReplay(200, "application/json", "GIF89a\u0001\u0001blahblahblah;", {});
    }

    /* Data */
protected:
    std::string mockBackendUrl_;
    TestHttpServer mockBackend_;
};

Y_UNIT_TEST_SUITE(HttpClientTest1) {
    Y_UNIT_TEST_F(TestHttpClientSupportRedirect, RedirectHostFixture) {
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{10000}); /* large timeout so test won't flap*/
        /* Check that by default HttpClient Do not follow redirects */
        auto response = client.get("testreq", mockBackendUrl_);
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 301);
        /* Check HttpClient will follow redirect after setFollowRedirect */
        client.setFollowRedirect(true);
        response = client.get("testreq", mockBackendUrl_);
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);

        /* Check that follow back works (HttpClient will not follow redirect) */
        client.setFollowRedirect(false);
        response = client.get("testreq", mockBackendUrl_);
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 301);
    }

    Y_UNIT_TEST_F(TestHttpclientBatchUrlDownload, UrlDownloadFixture) {
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{10000}); /* large timeout so test won't flap*/
        /* Check that by default HttpClient Do not follow redirects */

        std::vector<quasar::HttpClient::DownloadFileTask> downloadFileTasks;
        downloadFileTasks.emplace_back(quasar::HttpClient::DownloadFileTask{.url = mockBackendUrl_, .outputFileName = "./temp.gif"});
        downloadFileTasks.emplace_back(quasar::HttpClient::DownloadFileTask{.url = mockBackendUrl_, .outputFileName = "./temp2.gif"});

        auto response = client.download("testreq", downloadFileTasks);
        UNIT_ASSERT_VALUES_EQUAL(response.size(), 2);

        uint32_t checksum = 0;
        for (const auto& item : response) {
            UNIT_ASSERT(checksum == 0 || item == checksum);
            checksum = item;
        }
    }
}

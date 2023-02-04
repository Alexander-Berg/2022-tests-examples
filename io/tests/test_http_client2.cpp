#include <yandex_io/libs/http_client/http_client.h>

#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/external_libs/datacratic/soa/types/print_utils.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <boost/algorithm/string.hpp>

#include <library/cpp/testing/unittest/registar.h>
#include <util/folder/path.h>

#include <fstream>
#include <future>
#include <random>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace Datacratic;

Y_UNIT_TEST_SUITE_F(HttpClientTest2, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testHttpClientDownload)
    {
        TestHttpServer endpoint;
        std::string content = randomString(100);
        TestHttpServer::Headers receivedHeader;
        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                       const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            receivedHeader = header;
            int rangeFrom = 0;
            int rangeTo = content.length() - 1;
            if (!header.tryGetHeader("range").empty())
            {
                std::string range = header.getHeader("range").substr(strlen("bytes="));
                std::vector<std::string> rangeItems;
                boost::iter_split(rangeItems, range, boost::first_finder("-"));
                if (2 == rangeItems.size())
                {
                    if (!rangeItems[0].empty()) {
                        rangeFrom = atoi(rangeItems[0].c_str());
                    }
                    if (!rangeItems[1].empty()) {
                        rangeTo = atoi(rangeItems[1].c_str());
                    }
                }
            }

            if (rangeFrom < 0 || rangeFrom >= (int)content.length())
            {
                handler.doReplay(416, "text/html", "<title>416</title>",
                                 {{"Content-Range", "bytes */" + std::to_string(content.length())}});
            } else {
                handler.doReplay(200, "text/html", content.substr(rangeFrom, rangeTo - rangeFrom + 1));
            }
        };

        int port = endpoint.start(getPort());

        HttpClient client("test", getDeviceForTests());
        const int corruptedBlockSize = 5;
        client.setCorruptedBlockSize(corruptedBlockSize);
        unlink("content");
        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content"), getCrc32(content));
        UNIT_ASSERT(receivedHeader.tryGetHeader("range").empty());
        std::string downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);
        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content"), getCrc32(content));
        downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);
        UNIT_ASSERT_VALUES_EQUAL(receivedHeader.getHeader("range"), "bytes=" + std::to_string(content.length() - corruptedBlockSize) + "-");
        unlink("content");
    }

    Y_UNIT_TEST(testHttpClinetDownloadNoInternet)
    {
        TestHttpServer endpoint;
        std::string content = randomString(100);
        TestHttpServer::Headers receivedHeader;

        std::atomic_bool noInternet{true};

        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                       const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            if (noInternet) {
                /* pretend like there was a connect, but then internet was lost, so no answer */
                handler.close();
                return;
            }
            receivedHeader = header;
            int rangeFrom = 0;
            int rangeTo = content.length() - 1;
            if (!header.tryGetHeader("range").empty())
            {
                std::string range = header.getHeader("range").substr(strlen("bytes="));
                std::vector<std::string> rangeItems;
                boost::iter_split(rangeItems, range, boost::first_finder("-"));
                if (2 == rangeItems.size())
                {
                    if (!rangeItems[0].empty()) {
                        rangeFrom = atoi(rangeItems[0].c_str());
                    }
                    if (!rangeItems[1].empty()) {
                        rangeTo = atoi(rangeItems[1].c_str());
                    }
                }
            }

            if (rangeFrom < 0 || rangeFrom >= (int)content.length())
            {
                handler.doReplay(416, "text/html", "<title>416</title>", {{"Content-Range", "bytes */" + std::to_string(content.length())}});
            } else {
                handler.doReplay(200, "text/html", content.substr(rangeFrom, rangeTo - rangeFrom + 1));
            }
        };

        int port = endpoint.start(getPort());

        HttpClient client("test", getDeviceForTests());
        const int corruptedBlockSize = 5;
        client.setCorruptedBlockSize(corruptedBlockSize);
        unlink("content");

        try {
            client.download("testreq", "http://localhost:" + std::to_string(port), "content", {}, nullptr, 1, 1);
            UNIT_FAIL("Download should fail exception because 0 bytes was downloaded in a sec");
        } catch (const std::runtime_error& e) {
            YIO_LOG_INFO(std::string("Download failed (correct behavior): ") + e.what());
        }

        noInternet = false; /* pretend that internet returned */

        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content"), getCrc32(content));
        UNIT_ASSERT(receivedHeader.tryGetHeader("range").empty());
        std::string downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);

        const std::string notFullDownloadedContent = downloadedContent.substr(0, downloadedContent.size() / 2);
        {
            /* truncate file so file upload will start from 50% of file */
            std::ofstream contentFile("content", std::ofstream::trunc);
            contentFile << notFullDownloadedContent;
        }

        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content"), getCrc32(content));
        downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);
        UNIT_ASSERT_VALUES_EQUAL(receivedHeader.getHeader("range"),
                                 "bytes=" + std::to_string(notFullDownloadedContent.length() - corruptedBlockSize) + "-");
        unlink("content");
    }

    Y_UNIT_TEST(testHttpClinetDownloadWithInternetAndLowSpeedLimits)
    {
        TestHttpServer endpoint;
        std::string content = randomString(100);
        TestHttpServer::Headers receivedHeader;
        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                       const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            receivedHeader = header;
            int rangeFrom = 0;
            int rangeTo = content.length() - 1;
            if (!header.tryGetHeader("range").empty())
            {
                std::string range = header.getHeader("range").substr(strlen("bytes="));
                std::vector<std::string> rangeItems;
                boost::iter_split(rangeItems, range, boost::first_finder("-"));
                if (2 == rangeItems.size())
                {
                    if (!rangeItems[0].empty()) {
                        rangeFrom = atoi(rangeItems[0].c_str());
                    }
                    if (!rangeItems[1].empty()) {
                        rangeTo = atoi(rangeItems[1].c_str());
                    }
                }
            }

            if (rangeFrom < 0 || rangeFrom >= (int)content.length())
            {
                handler.doReplay(416, "text/html", "<title>416</title>", {{"Content-Range", "bytes */" + std::to_string(content.length())}});
            } else {
                handler.doReplay(200, "text/html", content.substr(rangeFrom, rangeTo - rangeFrom + 1));
            }
        };

        int port = endpoint.start(getPort());

        HttpClient client("test", getDeviceForTests());
        const int corruptedBlockSize = 5;
        client.setCorruptedBlockSize(corruptedBlockSize);
        unlink("content");
        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content", {}, nullptr, 1, 1), getCrc32(content));
        UNIT_ASSERT(receivedHeader.tryGetHeader("range").empty());
        std::string downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);
        UNIT_ASSERT_VALUES_EQUAL(client.download("testreq", "http://localhost:" + std::to_string(port), "content", {}, nullptr, 1, 1), getCrc32(content));
        downloadedContent = getFileContent("content");
        UNIT_ASSERT_VALUES_EQUAL(downloadedContent, content);
        UNIT_ASSERT_VALUES_EQUAL(receivedHeader.getHeader("range"), "bytes=" + std::to_string(content.length() - corruptedBlockSize) + "-");
        unlink("content");
    }

    Y_UNIT_TEST(testHttpClientRetries)
    {
        TestHttpServer endpoint;
        int tryNumber = 0;

        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& /* header */,
                                       const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            ++tryNumber;
            if (tryNumber <= 2)
            {
                handler.close();
                return;
            } else {
                handler.doReplay(200, "text/html", "OK");
            }
        };

        HttpClient client("test", getDeviceForTests());
        client.setRetriesCount(1);
        int port = endpoint.start(getPort());

        UNIT_ASSERT_EXCEPTION(client.get("testreq", "http://localhost:" + std::to_string(port)), std::runtime_error);

        tryNumber = 0;

        client.setRetriesCount(2);
        auto response = client.get("testreq", "http://localhost:" + std::to_string(port));
        UNIT_ASSERT_VALUES_EQUAL(response.body, "OK");
    }

    Y_UNIT_TEST(testHttpClientUserAgent)
    {
        YandexIO::Configuration::TestGuard testGuard;
        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["common"]["deviceType"] = "testDevice";
        config["common"]["softwareVersion"] = "123.456";

        std::promise<std::string> ua;

        TestHttpServer endpoint;
        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                       const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            ua.set_value(header.tryGetHeader("user-agent"));
            handler.doReplay(200, "text/html", "OK");
        };
        int port = endpoint.start(getPort());
        HttpClient client("test", getDeviceForTests());
        client.get("testreq", "http://localhost:" + std::to_string(port));
        UNIT_ASSERT_VALUES_EQUAL(ua.get_future().get(), "testDevice/123.456");
    }

    Y_UNIT_TEST(testHttpClientCustomDelayFunction)
    {
        TestHttpServer endpoint;
        int tryNumber = 0;

        std::atomic<bool> gotFirst(false);
        std::atomic<bool> gotSecond(false);
        std::atomic<bool> gotThird(false);

        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& /* header */, const std::string& /* payload */,
                                       TestHttpServer::HttpConnection& handler) {
            ++tryNumber;
            if (tryNumber == 1) { // main attempt (not retry)
                handler.close();
                return;
            } else if (tryNumber == 2) {
                gotFirst = true;
                handler.close();
                return;
            } else if (tryNumber == 3) {
                gotSecond = true;
                handler.close();
                return;
            } else {
                gotThird = true;
                handler.doReplay(200, "text/html", "OK");
            }
        };

        std::mt19937 randomGenerator;
        randomGenerator.seed(1);

        std::vector<std::chrono::milliseconds> resultDelays(4, std::chrono::milliseconds{-1});

        auto calcDelay = [&randomGenerator, &resultDelays](const int retryNum) {
            std::chrono::milliseconds basicDelay{0};
            switch (retryNum) {
                case 0:
                    basicDelay = std::chrono::milliseconds{5};
                    break;
                case 1:
                    basicDelay = std::chrono::milliseconds{25};
                    break;
                case 2:
                    basicDelay = std::chrono::milliseconds{50};
                    break;
                default:
                    basicDelay = std::chrono::milliseconds{1000};
                    break;
            }

            std::uniform_int_distribution<int> distribution{1, (int)(basicDelay.count() * 2)};
            auto resultDelay = basicDelay + decltype(basicDelay){distribution(randomGenerator)};
            resultDelays[retryNum] = resultDelay;
            return resultDelay;
        };

        HttpClient client("test", getDeviceForTests());
        client.setRetriesCount(3);
        client.setCalcRetryDelayFunction(calcDelay);
        int port = endpoint.start(getPort());

        auto response = client.get("testreq", "http://localhost:" + std::to_string(port));
        UNIT_ASSERT(gotFirst);
        UNIT_ASSERT(gotSecond);
        UNIT_ASSERT(gotThird);

        UNIT_ASSERT(resultDelays[0].count() > 5 && resultDelays[0].count() <= 15);
        UNIT_ASSERT(resultDelays[1].count() > 25 && resultDelays[1].count() <= 75);
        UNIT_ASSERT(resultDelays[2].count() > 50 && resultDelays[2].count() <= 150);
        UNIT_ASSERT_VALUES_EQUAL(resultDelays[3].count(), -1);

        UNIT_ASSERT_VALUES_EQUAL(response.body, "OK");
    }

    Y_UNIT_TEST(testHttpClientCancelRetries)
    {
        TestHttpServer endpoint;
        int tryNumber = 0;

        HttpClient client("test", getDeviceForTests());
        client.setRetriesCount(3);

        endpoint.onHandlePayload = [&](const TestHttpServer::Headers& /* header */, const std::string& /* payload */,
                                       TestHttpServer::HttpConnection& handler) {
            ++tryNumber;
            if (tryNumber == 1) { // main attempt (not retry)
                handler.close();
                return;
            } else if (tryNumber == 2) {
                client.cancelRetries();
                handler.close();
            } else {
                handler.doReplay(200, "text/html", "OK");
                return;
            }
        };

        int port = endpoint.start(getPort());
        UNIT_ASSERT_EXCEPTION(client.get("testreq", "http://localhost:" + std::to_string(port)), std::runtime_error);
    };

    Y_UNIT_TEST(testHttpClientEmptyBodyResponse)
    {
        TestHttpServer endpoint;
        endpoint.onHandlePayload = [i{0}](const TestHttpServer::Headers& /* header */, const std::string& /* payload */,
                                          TestHttpServer::HttpConnection& handler) mutable {
            if (i > 2) {
                // download
                handler.doReplay(429, "test/html", "");
            }
            handler.doReplay(200 + i, "text/html", "");
            i++;
        };
        const auto port = getPort();
        endpoint.start(port);
        const auto url = "http://localhost:" + std::to_string(port);

        HttpClient client("test", getDeviceForTests());
        auto response = client.get("testreq", url);
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);

        response = client.post("testreq", url, "data");
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 201);

        response = client.head("testreq", url);
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 202);

        // FIXME: Datacratic http client hang up on download...
        // 429 with empty body on download
        // UNIT_ASSERT_EXCEPTION(client.download("download", url, "test.txt"), std::runtime_error);
        // TFsPath("test.txt").ForceDelete(); // clean up
    };
}

#include <yandex_io/tests/testlib/unittest_helper/logging_test_fixture.h>
#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/modules/backend_client/lib/backend_client_core.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestBackendClientCore, QuasarLoggingTestFixture) {
    Y_UNIT_TEST(simpleGet) {
        std::promise<std::pair<std::string, proto::BackendRequest>> reqPromise;
        BackendClientCore client([&reqPromise](auto reqId, auto req) {
            reqPromise.set_value({reqId, req});
        });
        std::promise<ISimpleHttpClient::HttpResponse> promise;
        std::thread thr1([&]() {
            promise.set_value(client.get("test_tag", "test_url", {{"testName", "testVal"}}));
        });
        auto [reqId, req] = reqPromise.get_future().get();
        UNIT_ASSERT_EQUAL(req.tag(), "test_tag");
        UNIT_ASSERT_EQUAL(req.url(), "test_url");
        UNIT_ASSERT_EQUAL(req.method(), proto::BackendRequest::GET);
        UNIT_ASSERT_EQUAL(req.headers().size(), 1);
        UNIT_ASSERT_EQUAL(req.headers().begin()->name(), "testName");
        UNIT_ASSERT_EQUAL(req.headers().begin()->value(), "testVal");
        proto::BackendResponse response;
        response.set_response_code(200);
        response.set_response_status("OK");
        response.set_content_type("text/json");
        response.set_body("testBody");
        response.set_content_length(8);
        client.handleResponse(reqId, response);
        auto recvResponse = promise.get_future().get();
        UNIT_ASSERT_EQUAL(recvResponse.responseCode, 200);
        UNIT_ASSERT_EQUAL(recvResponse.responseStatus, "OK");
        UNIT_ASSERT_EQUAL(recvResponse.contentType, "text/json");
        UNIT_ASSERT_EQUAL(recvResponse.body, "testBody");
        UNIT_ASSERT_EQUAL(recvResponse.contentLength, 8);
        thr1.join();
    }

    Y_UNIT_TEST(timeouted) {
        BackendClientCore client([](auto reqId, auto /*req*/) {
            YIO_LOG_INFO("Should send " << reqId);
        });
        client.setTimeoutInterval(std::chrono::seconds(3));
        std::promise<void> promise;
        std::thread thr([&promise, &client]() {
            try {
                client.get("test", "url", {});
            } catch (...) {
                YIO_LOG_INFO("Timeouted");
                promise.set_value();
            }
        });
        std::this_thread::sleep_for(std::chrono::seconds(5));
        client.checkTimeouts();
        promise.get_future().get();
        thr.join();
    }
}

#include <yandex_io/tests/testlib/unittest_helper/logging_test_fixture.h>
#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/modules/backend_client/service/queued_backend.h>

using namespace quasar;

namespace {
    class TestHttpClient: public ISimpleHttpClient {
        int idx_{0};

        HttpResponse defaultAnswer(const std::string& body) {
            HttpResponse result;
            result.responseCode = 200;
            result.responseStatus = "OK " + std::to_string(++idx_);
            result.contentType = "text/html";
            result.body = body;
            result.contentLength = body.length();
            return result;
        }

    public:
        HttpResponse get(std::string_view /*tag*/, const std::string& url, const Headers& /*headers*/) override {
            YIO_LOG_INFO("get " << url);
            return defaultAnswer(url);
        }

        HttpResponse post(std::string_view /*tag*/, const std::string& url, const std::string& data, const Headers& /*headers*/) override {
            YIO_LOG_INFO("post " << url);
            return defaultAnswer(data);
        }

        HttpResponse head(std::string_view /*tag*/, const std::string& /*url*/, const Headers& /*headers*/) override {
            UNIT_ASSERT_C(false, "'head' method should be called");
            return HttpResponse();
        }
    };

    struct Fixture: public QuasarLoggingTestFixture {
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarLoggingTestFixture::SetUp(context);

            cbQueue = std::make_shared<NamedCallbackQueue>("sync-quasar-backend", 100);
            backend = std::make_unique<QueuedBackend>(cbQueue, std::make_unique<TestHttpClient>(), "somehost");
        }

        std::shared_ptr<ICallbackQueue> cbQueue;
        std::unique_ptr<QueuedBackend> backend;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(TestQueuedBackend, Fixture) {
    Y_UNIT_TEST(oneRealRequest) {
        std::promise<void> trigger;
        cbQueue->add([&trigger]() {
            trigger.get_future().get();
        });

        using ResponsePromise = std::promise<ISimpleHttpClient::HttpResponse>;

        ResponsePromise res1p;
        ResponsePromise res2p;
        ResponsePromise res3p;
        ResponsePromise res4p;
        ResponsePromise res5p;

        auto makeCb = [](ResponsePromise& promise) {
            return [&promise](const ISimpleHttpClient::HttpResponse& response, std::string error) {
                if (error.empty()) {
                    promise.set_value(response);
                } else {
                    try {
                        throw std::runtime_error(error);
                    } catch (...) {
                        promise.set_exception(std::current_exception());
                    }
                }
            };
        };

        backend->getCb("test", "/path?params", {}, makeCb(res1p));
        backend->getCb("test", "/path?params", {}, makeCb(res2p));
        backend->getCb("test", "/path?params", {}, makeCb(res3p));
        backend->postCb("test", "/post?params", {}, "data", makeCb(res4p));
        backend->postCb("test", "/post?params", {}, "data2", makeCb(res5p));

        trigger.set_value();

        auto res1 = res1p.get_future().get();
        auto res2 = res2p.get_future().get();
        auto res3 = res3p.get_future().get();
        auto res4 = res4p.get_future().get();
        auto res5 = res5p.get_future().get();

        UNIT_ASSERT_EQUAL(res1.responseStatus, "OK 1");
        UNIT_ASSERT_EQUAL(res2.responseStatus, "OK 1");
        UNIT_ASSERT_EQUAL(res3.responseStatus, "OK 1");
        UNIT_ASSERT_EQUAL(res4.responseStatus, "OK 2");
        YIO_LOG_INFO(res4.body);
        UNIT_ASSERT_EQUAL(res4.body, "data2");
        UNIT_ASSERT_EQUAL(res5.responseStatus, "OK 2");
    }
}

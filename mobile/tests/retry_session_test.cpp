#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/retry_session.h>

#include <yandex/maps/runtime/async/utils/retryable_session.h>
#include <yandex/maps/runtime/error.h>

using yandex::maps::runtime::Error;
namespace async = yandex::maps::runtime::async;

namespace yandex::maps::navikit {
namespace {

class DummySession {
public:
    virtual ~DummySession() {}
    using OnOk = std::function<void(int result)>;
    using OnError = std::function<void(Error* error)>;
    virtual void cancel() = 0;
    virtual void retry(const OnOk&, const OnError&) = 0;
};

class RetrySessionFixture {
public:
    async::Handle handle;
    std::shared_ptr<async::Promise<int>> completePromise = std::make_shared<async::Promise<int>>();
    async::Future<int> completeFuture = completePromise->future();

    void startDummySession(
        std::function<int()> work, std::function<void(int)> ok, std::function<void(Error*)> error)
    {
        handle = navikit::startAndRetry(
            [work](DummySession::OnOk ok, DummySession::OnError error) {
                return async::utils::makeRetryableSession<DummySession>(
                    std::move(work), std::move(ok), std::move(error));
            },
            [ok, completePromise = completePromise](int res) {
                ok(res);
                completePromise->setValue(42);
            },
            [error, completePromise = completePromise](Error* err) {
                error(err);
                completePromise->setValue(42);
            });
        completeFuture.wait();
        async::runOnUiThread([&] { handle.reset(); });
    }
};

struct LivenessChecker {
    bool shouldLive = false;

    ~LivenessChecker() { BOOST_CHECK(!shouldLive); }
};

}  // namespace

BOOST_FIXTURE_TEST_SUITE(RetrySessionTests, RetrySessionFixture)

BOOST_AUTO_TEST_CASE(retrySessionCallsOk)
{
    startDummySession(
        [] { return 42; },
        [](int res) { BOOST_CHECK_EQUAL(res, 42); },
        [](Error*) { BOOST_CHECK_MESSAGE(false, "expected ok, not error"); });
}

BOOST_AUTO_TEST_CASE(retrySessionCallsError)
{
    startDummySession(
        [] {
            throw runtime::RuntimeError() << "retry should stop now";
            return 42;
        },
        [](int) { BOOST_CHECK_MESSAGE(false, "expected error, not ok"); },
        [](Error*) { BOOST_CHECK(true); });
}

BOOST_AUTO_TEST_CASE(retrySessionErrorsDoNotDestroyThemselves)
{
    auto checker = LivenessChecker();
    startDummySession(
        [] {
            static int takes = 0;
            ++takes;
            if (takes == 1) {
                throw runtime::network::NetworkException();
            } else {
                throw runtime::RuntimeError() << "retry should stop now";
            }
            return 42;
        },
        [](int) { BOOST_CHECK_MESSAGE(false, "expected error, not ok"); },
        [&handle = handle, checker](Error*) mutable {
            BOOST_CHECK(true);
            checker.shouldLive = true;
            handle.reset();
            checker.shouldLive = false;
        });
}

BOOST_AUTO_TEST_CASE(retrySessionResultsDoNotDestroyThemselves)
{
    auto checker = LivenessChecker();
    startDummySession(
        [] {
            static int takes = 0;
            ++takes;
            if (takes == 1) {
                throw runtime::network::NetworkException();
            }
            return 24;
        },
        [&handle = handle, checker](int res) mutable {
            BOOST_CHECK_EQUAL(res, 24);
            checker.shouldLive = true;
            handle.reset();
            checker.shouldLive = false;
        },
        [](Error*) { BOOST_CHECK_MESSAGE(false, "expected ok, not error"); });
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex

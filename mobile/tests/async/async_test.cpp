#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/async/async.h>
#include <yandex/metrokit/async/executor/ui.h>
#include <yandex/metrokit/async/executor/bg_serial.h>
#include <yandex/metrokit/async/executor/bg_concurrent.h>

#include <thread>
#include <utility>

namespace yandex {
namespace metrokit {
namespace async {

BOOST_AUTO_TEST_CASE(when_fulfill_result__should_receive_result_in_completion) {
    using AsyncType = Async<int>;

    static const int testValue1 = 1;
    static const int testValue2 = 2;

    auto async = AsyncType {
        [](auto promise) {
            promise->fulfill(testValue1);
            return testValue2;
        },
        [](const auto&) {
            BOOST_FAIL("This method should not be called since we didn't cancel");
        }
    };

    const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([](const auto& value) {
        BOOST_CHECK_EQUAL(value, testValue1);
    });

    async.subscribe(subscriber);

    async
        .start()
        .wait();
}

BOOST_AUTO_TEST_CASE(when_cancel__should_receive_token_in_cancel) {
    using AsyncType = Async<int>;

    static const int testValue1 = 1;
    static const int testValue2 = 2;

    std::thread t;
    std::mutex m;
    std::condition_variable cv;
    auto isCancelled = false;

    auto async = AsyncType {
        [&t, &m, &cv, &isCancelled](auto promise) {
            t = std::thread {
                [promise, &m, &cv, &isCancelled] {
                    {
                        std::unique_lock<std::mutex> lk{m};
                        cv.wait(lk, [&isCancelled] { return isCancelled; });
                    }
                    promise->fulfill(testValue1);
                }
            };
            return testValue2;
        },
        [](const auto& token) {
            BOOST_CHECK_EQUAL(boost::any_cast<int>(token), testValue2);
        }
    };

    const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([](const auto&) {
        BOOST_FAIL("This method should not be called since we cancelled");
    });

    async.subscribe(subscriber);
    async.start().cancel();

    {
        std::unique_lock<std::mutex> lk { m };
        isCancelled = true;
        cv.notify_one();
    }

    t.join();
    async.wait();
}

namespace {

void simpleTestWith(executor::Executor* executor) {
    static const int testValue = 1;

    schedule(std::move(executor), [] {
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
        BOOST_CHECK(true); // Will throw if executed out of boost test case scope
        return testValue;
    }).start().wait();
}

} // namespace

BOOST_AUTO_TEST_CASE(simple_test_with_executor_ui) {
    simpleTestWith(executor::ui());
}

BOOST_AUTO_TEST_CASE(simple_test_with_executor_serial_bg) {
    const auto serialBg = executor::makeSerialBg("test");
    simpleTestWith(serialBg.get());
}

BOOST_AUTO_TEST_CASE(simple_test_with_executor_concurrent_bg) {
    simpleTestWith(executor::bgNormal());
}

BOOST_AUTO_TEST_CASE(map__should_be_fine) {
    using AsyncType = Async<int>;

    auto async = AsyncType {
        [](auto promise) {
            promise->fulfill(1);
            return 0;
        },
    }
        .map([](const auto& result) {
            BOOST_CHECK_EQUAL(result, 1);
            return result + 1;
        });

    const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([](const auto& value) {
        BOOST_CHECK_EQUAL(value, 2);
    });

    async.subscribe(subscriber);
    async.start().wait();
}

BOOST_AUTO_TEST_CASE(flat_map__should_be_fine) {
    using AsyncType = Async<int>;

    auto async = AsyncType {
        [](auto promise) {
            promise->fulfill(1);
            return 0;
        },
    }
        .flatMap([](const auto& result) {
            BOOST_CHECK_EQUAL(result, 1);
            return Async<int> {
                [result](auto promise) {
                    promise->fulfill(result + 1);
                    return 0;
                }
            };
        });

    const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([](const auto& value) {
        BOOST_CHECK_EQUAL(value, 2);
    });

    async.subscribe(subscriber);
    async.start().wait();
}

BOOST_AUTO_TEST_CASE(switch_executor__should_be_fine) {
    const auto serialBg = executor::makeSerialBg("test");
    
    auto async = schedule(serialBg.get(), [] {
        return 1;
    }).switchExecutor(executor::ui());

    const auto subscriber = makeShared<Async<int>::ConcreteSubscriber>([](const auto& value) {
        BOOST_CHECK_EQUAL(value, 1);
    });

    async.subscribe(subscriber);
    async.start().wait();
}

BOOST_AUTO_TEST_CASE(blocking_get__should_be_fine) {
    static int testValue = 1;

    const auto async = Async<int> {
        [](auto promise) {
            promise->fulfill(testValue);
            return 0;
        },
    };

    BOOST_CHECK_EQUAL(async.start().wait().peek().value(), testValue);
}

BOOST_AUTO_TEST_CASE(when_start_async_composed_from_started_async__should_deliver_result_normally) {
    const auto async1 = Async<int> {
        [](auto promise) {
            promise->fulfill(1);
            return 0;
        },
    }.start();

    const auto async2 = async1
        .map([](const auto& result) {
            return result + 1;
        })
        .start();

    const auto serialBg = executor::makeSerialBg("test");

    const auto async3 = async2
        .flatMap([&](const auto& result) {
            return schedule(serialBg.get(), [result] {
                return result + 1;
            });
        })
        .start();

    BOOST_CHECK_EQUAL(async2.wait().peek().value(), 2);
    BOOST_CHECK_EQUAL(async3.wait().peek().value(), 3);
}

BOOST_AUTO_TEST_CASE(when_hold_weak_ref_on_susbcriber__should_not_deliver_result) {
    using AsyncType = Async<Unit>;

    auto async = AsyncType {
        [](auto promise) {
            promise->fulfill(unit);
            return 0;
        },
    };

    {
        const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([](const auto&) {
            BOOST_FAIL("Should not be called");
        });

        async.subscribe(subscriber);
    }

    async.start().wait();
}

BOOST_AUTO_TEST_CASE(when_hold_strong_ref_on_susbcriber__should_deliver_result) {
    using AsyncType = Async<Unit>;

    auto async = AsyncType {
        [](auto promise) {
            promise->fulfill(unit);
            return 0;
        },
    };

    bool isSubscriberCalled = false;

    const auto subscriber = makeShared<AsyncType::ConcreteSubscriber>([&isSubscriberCalled](const auto&) {
        isSubscriberCalled = true;
    });

    async.subscribe(subscriber);

    async.start().wait();

    BOOST_CHECK_EQUAL(isSubscriberCalled, true);
}

}}}

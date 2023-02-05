#include <yandex/maps/navikit/subscriptions/lambda_subscription.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit::subscriptions {

BOOST_AUTO_TEST_SUITE(LambdaSubscriptionSuite)

BOOST_AUTO_TEST_CASE(constructor) {
    BOOST_CHECK_NO_THROW( LambdaSubscription() );
}

struct CallbackTest {
    int invoke_count = 0;
    std::function<void()> callback = [&]() { ++invoke_count; };
};

struct Fixture : CallbackTest {
    LambdaSubscription subscription;
};

BOOST_FIXTURE_TEST_CASE(notifyAllWithoutSubscribers, Fixture)
{
    BOOST_CHECK_NO_THROW(subscription.notifyAll());
}

BOOST_FIXTURE_TEST_CASE(notifyAllForOneSubscriber, Fixture)
{
    auto disposable = subscription.subscribe(callback);

    subscription.notifyAll();

    BOOST_CHECK_EQUAL(1, invoke_count);
}

BOOST_FIXTURE_TEST_CASE(notifyAllForTwoSubscribers, Fixture)
{
    auto disposable1 = subscription.subscribe(callback);
    auto disposable2 = subscription.subscribe(callback);

    subscription.notifyAll();

    BOOST_CHECK_EQUAL(2, invoke_count);
};

BOOST_FIXTURE_TEST_CASE(notifyAutoUnsubscribed, Fixture)
{
    {
        auto disposable = subscription.subscribe(callback);
    }
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(0, invoke_count);
}

BOOST_FIXTURE_TEST_CASE(notifyManualUnsubscribed, Fixture)
{
    auto disposable = subscription.subscribe(callback);
    disposable.dispose();
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(0, invoke_count);
}

BOOST_FIXTURE_TEST_CASE(doubleDisposed, Fixture) {
    auto disposable = subscription.subscribe(callback);
    disposable.dispose();
    BOOST_CHECK_NO_THROW(disposable.dispose());
}

BOOST_FIXTURE_TEST_CASE(disposableMoveConstructor, Fixture) {
    auto disposable1 = subscription.subscribe(callback);
    auto disposable2 = std::move(disposable1);
    disposable1.dispose();

    subscription.notifyAll();

    BOOST_CHECK_EQUAL(1, invoke_count);
}

BOOST_FIXTURE_TEST_CASE(disposableMoveAssignment, Fixture) {
    auto disposable1 = subscription.subscribe(callback);
    decltype(disposable1) disposable2;
    disposable2 = std::move(disposable1);
    disposable1.dispose();

    subscription.notifyAll();

    BOOST_CHECK_EQUAL(1, invoke_count);
}

BOOST_FIXTURE_TEST_CASE(disposableMustBeNonCopied, Fixture)
{
    auto disposable1 = subscription.subscribe(callback);
    BOOST_CHECK(!std::is_copy_constructible_v<decltype(disposable1)>);
    BOOST_CHECK(!std::is_copy_assignable_v<decltype(disposable1)>);
}

BOOST_AUTO_TEST_CASE(unsubscribeOneInTheList)
{
    const size_t subscribersCount = 3;
    for (size_t i=0; i<subscribersCount; ++i) {
        LambdaSubscription subscription;
        std::vector<CallbackTest> subscribers(subscribersCount);
        std::vector<Disposable> disposable;
        for(auto& s : subscribers)
            disposable.push_back(subscription.subscribe(s.callback));
        disposable[i].dispose();
        subscription.notifyAll();
        for(size_t j=0; j<subscribersCount; ++j)
        BOOST_CHECK_EQUAL(j==i ? 0 : 1, subscribers[j].invoke_count);
    }
}

BOOST_AUTO_TEST_CASE(unsubscribeSelfDuringTheCall)
{
    LambdaSubscription subscription;
    const size_t subscribersCount = 3;
    std::vector<Disposable> disposable;
    size_t callsCount = 0;
    for (size_t i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe([&, yours = i] {
                ++callsCount;
                disposable[yours].dispose();
            }));
    }
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    callsCount = 0;
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(0, callsCount);
};

BOOST_AUTO_TEST_CASE(unsubscribePreviousDuringTheCall)
{
    LambdaSubscription subscription;
    const int subscribersCount = 10;
    std::vector<Disposable> disposable;
    int callsCount = 0;
    for (int i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe([&, i] {
              ++callsCount;
              if (i>0)
                disposable[i-1].dispose();
            }));
    }
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    callsCount = 0;
    subscription.notifyAll(); // only last subscriber is still subscribed
    BOOST_CHECK_EQUAL(1, callsCount);
};

BOOST_AUTO_TEST_CASE(unsubscribeNextDuringTheCall)
{
    LambdaSubscription subscription;
    const int subscribersCount = 2*5; // number must be even
    std::vector<Disposable> disposable;
    int callsCount = 0;
    for (int i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe([&, i] {
              ++callsCount;
              if (i+1<subscribersCount)
                  disposable[i+1].dispose();
            }));
    }
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(subscribersCount/2, callsCount);
};

BOOST_AUTO_TEST_CASE(subscribeDuringTheCall)
{
    LambdaSubscription subscription;
    const int subscribersCount = 8;
    std::vector<Disposable> disposable;
    int callsCount = 0;
    auto callback = [&] {
      ++callsCount;
      disposable.push_back(
          subscription.subscribe([&]{ ++callsCount; })
      );
    };

    for (int i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe(callback));
    }
    subscription.notifyAll();
    // we don't expect calls for new subscribed callers
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    callsCount = 0;
    subscription.notifyAll();
    BOOST_CHECK_EQUAL(subscribersCount*2, callsCount);
};


BOOST_AUTO_TEST_CASE(UnsubscribeAfterSubscriptionDied)
{
    Disposable disposable;
    {
        LambdaSubscription subscription;
        disposable = subscription.subscribe([]() {});
    }
    BOOST_CHECK_NO_THROW(disposable.dispose());
}

BOOST_AUTO_TEST_SUITE_END()

}

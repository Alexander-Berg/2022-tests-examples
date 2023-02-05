#include <yandex/maps/navikit/subscriptions/interface_subscription.h>

#include <boost/test/unit_test.hpp>
#include <vector>

namespace yandex::maps::navikit::subscriptions {

struct XYListener
{
    std::function<void()> xChangeImplementation;
    void onXChanged() { if (xChangeImplementation) xChangeImplementation(); }

    std::function<void()> yChangeImplementation;
    void onYChanged() { if (yChangeImplementation) yChangeImplementation(); }
};

BOOST_AUTO_TEST_SUITE (ClassicSubscriptionSuite)

BOOST_AUTO_TEST_CASE (constructor)
{
    BOOST_CHECK_NO_THROW(InterfaceSubscription<XYListener>());
}

struct Fixture {
    InterfaceSubscription<XYListener> subscription;
};

BOOST_FIXTURE_TEST_CASE(notificationWithoutSubscribers, Fixture){
    BOOST_CHECK_NO_THROW(subscription.notifyAll(&XYListener::onXChanged));
    BOOST_CHECK_NO_THROW(subscription.notifyAll(&XYListener::onYChanged));
}

/* We can't test this cases because of ASSERT cannot be tested. I think we should deal with it
BOOST_FIXTURE_TEST_CASE(subscribeTwiceIsNotAllowed, Fixture)
{
    XYListener listener;
    auto disposable = subscription.subscribe(&listener);

    BOOST_CHECK_THROW(auto d = subscription.subscribe(&listener), std::exception);
}

BOOST_FIXTURE_TEST_CASE(subscriptionOnNullptrIsNotAllowed, Fixture) {
    BOOST_CHECK_THROW(auto d =subscription.subscribe(nullptr), std::exception);
}
*/

BOOST_FIXTURE_TEST_CASE(autoDispose, Fixture)
{
    XYListener listener;
    int count = 0;
    listener.xChangeImplementation = [&count] { ++count; };
    {
        auto disposable = subscription.subscribe(&listener);
    }
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(0, count);
}

struct XListener {
    XYListener listener;
    int count = 0;
    XListener() {
        listener.xChangeImplementation = [&] { ++count; };
    }
};

struct FixtureWithListener: Fixture, XListener {};

BOOST_FIXTURE_TEST_CASE(manualDispose, FixtureWithListener)
{
    auto disposable = subscription.subscribe(&listener);
    disposable.dispose();
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(0, count);
}

BOOST_FIXTURE_TEST_CASE(notification, Fixture)
{
    const int count = 3;
    std::vector<XYListener> listeners(count);
    std::vector<Disposable> disposables;
    int xCount = 0, yCount = 0;
    for (auto& listener : listeners) {
        listener.xChangeImplementation = [&] { ++xCount; };
        listener.yChangeImplementation = [&] { ++yCount; };
        disposables.push_back(subscription.subscribe(&listener));
    }

    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(count, xCount);
    BOOST_CHECK_EQUAL(0, yCount);

    subscription.notifyAll(&XYListener::onYChanged);
    BOOST_CHECK_EQUAL(count, xCount);
    BOOST_CHECK_EQUAL(count, yCount);
}

BOOST_FIXTURE_TEST_CASE(doubleDisposed, Fixture) {
    XYListener listener;
    auto disposable = subscription.subscribe(&listener);
    disposable.dispose();
    BOOST_CHECK_NO_THROW(disposable.dispose());
}

BOOST_FIXTURE_TEST_CASE(disposableMoveConstructor, FixtureWithListener) {
    auto disposable1 = subscription.subscribe(&listener);
    auto disposable2 = std::move(disposable1);
    disposable1.dispose();

    subscription.notifyAll(&XYListener::onXChanged);

    BOOST_CHECK_EQUAL(1, count);
}

BOOST_FIXTURE_TEST_CASE(disposableMoveAssignment, FixtureWithListener) {
    auto disposable1 = subscription.subscribe(&listener);
    decltype(disposable1) disposable2;
    disposable2 = std::move(disposable1);
    disposable1.dispose();

    subscription.notifyAll(&XYListener::onXChanged);

    BOOST_CHECK_EQUAL(1, count);
}

BOOST_FIXTURE_TEST_CASE(disposableMustBeNonCopied, FixtureWithListener)
{
    BOOST_CHECK(!std::is_copy_constructible_v<Disposable>);
    BOOST_CHECK(!std::is_copy_assignable_v<Disposable>);
}

BOOST_AUTO_TEST_CASE(unsubscribeOneInTheList)
{
    const size_t subscribersCount = 3;
    for (size_t i=0; i<subscribersCount; ++i) {
        InterfaceSubscription<XYListener> subscription;
        std::vector<XListener> subscribers(subscribersCount);
        std::vector<Disposable> disposable;
        for(auto& s : subscribers)
            disposable.push_back(subscription.subscribe(&s.listener));
        disposable[i].dispose();
        subscription.notifyAll(&XYListener::onXChanged);
        for(size_t j=0; j<subscribersCount; ++j)
            BOOST_CHECK_EQUAL(j==i ? 0 : 1, subscribers[j].count);
    }
}

BOOST_FIXTURE_TEST_CASE(unsubscribeSelfDuringTheCall, Fixture)
{
    const size_t subscribersCount = 3;
    std::vector<Disposable> disposable;
    std::vector<XYListener> listeners(subscribersCount);
    int callsCount = 0;
    for (size_t i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe(&listeners[i]));
        listeners[i].xChangeImplementation = [&,i]{
            ++callsCount;
            disposable[i].dispose();
        };
    };
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    subscription.notifyAll(&XYListener::onXChanged);
    // no more notifications
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);
}

BOOST_FIXTURE_TEST_CASE(unsubscribePreviousDuringTheCall, Fixture)
{
    const int subscribersCount = 10;
    std::vector<Disposable> disposable;
    std::vector<XYListener> listeners(subscribersCount);
    int callsCount = 0;
    for (int i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe(&listeners[i]));
        listeners[i].xChangeImplementation =
            [&, i] {
              ++callsCount;
              if (i>0)
                  disposable[i-1].dispose();
            };
    }
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    callsCount = 0;
    subscription.notifyAll(&XYListener::onXChanged); // only last subscriber is still subscribed
    BOOST_CHECK_EQUAL(1, callsCount);
}

BOOST_FIXTURE_TEST_CASE(unsubscribeNextDuringTheCall, Fixture)
{
    const int subscribersCount = 2*5; // number must be even
    std::vector<Disposable> disposable;
    std::vector<XYListener> listeners(subscribersCount);
    int callsCount = 0;
    for (int i=0; i<subscribersCount; ++i) {
        disposable.push_back(
            subscription.subscribe(&listeners[i]));
        listeners[i].xChangeImplementation =
            [&, i] {
              ++callsCount;
              if (i+1<subscribersCount)
                  disposable[i+1].dispose();
            };
    }
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(subscribersCount/2, callsCount);
}

BOOST_FIXTURE_TEST_CASE(subscribeDuringTheCall, Fixture)
{
    const int subscribersCount = 8;
    std::vector<Disposable> disposable;
    std::vector<XYListener> listeners(subscribersCount);
    std::vector<XYListener> newListeners(subscribersCount);
    int callsCount = 0;
    bool addSubscribers = true;
    for (int i = 0; i < subscribersCount; ++i) {
        disposable.push_back(subscription.subscribe(&listeners[i]));
        listeners[i].xChangeImplementation = [&, i] {
            ++callsCount;
            if (addSubscribers)
                disposable.push_back(subscription.subscribe(&newListeners[i]));
        };
        newListeners[i].xChangeImplementation = [&] {
          ++callsCount;
        };
    }
    subscription.notifyAll(&XYListener::onXChanged);
    // we don't expect calls for new subscribed callers
    BOOST_CHECK_EQUAL(subscribersCount, callsCount);

    callsCount = 0;
    addSubscribers = false;
    subscription.notifyAll(&XYListener::onXChanged);
    BOOST_CHECK_EQUAL(subscribersCount * 2, callsCount);
}

BOOST_AUTO_TEST_CASE(UnsubscribeAfterSubscriptionDied)
{
    Disposable disposable;
    XYListener listener;
    {
        InterfaceSubscription<XYListener> subscription;
        disposable = subscription.subscribe(&listener);
    }
    BOOST_CHECK_NO_THROW(disposable.dispose());
}

BOOST_AUTO_TEST_SUITE_END()

}

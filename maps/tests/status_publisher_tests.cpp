#include <maps/libs/concurrent/include/status_publisher.h>
#include <maps/libs/concurrent/include/latch.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <thread>
#include <vector>
#include <stdexcept>
#include <ostream>
#include <memory>

using std::chrono::milliseconds;

namespace maps {
namespace concurrent {
namespace tests {

namespace {

enum class Status { Old, New };
std::ostream& operator<<(std::ostream& os, Status status)
{
    switch (status) {
    case Status::Old:
        return os << "Old";
    case Status::New:
        return os << "New";
    }
    return os << "(unknown)";
}
bool isNewStatus(Status s) { return s == Status::New; }

}

Y_UNIT_TEST_SUITE(test_status_publisher) {

Y_UNIT_TEST(test_single_threaded_publish_works)
{
    auto publisher = statusPublisherOf(Status::Old);
    auto subscriber = publisher.subscribe();

    EXPECT_EQ(Status::Old, publisher.get());
    EXPECT_EQ(Status::Old, subscriber.get());

    publisher.set(Status::New);
    EXPECT_EQ(Status::New, publisher.get());
    EXPECT_EQ(Status::New, subscriber.get());
}

Y_UNIT_TEST(test_subscriber_wait)
{
    auto publisher = statusPublisherOf(Status::Old);
    auto subscriber = publisher.subscribe();

    std::thread backgroundSetter([&] {
        std::this_thread::sleep_for(milliseconds(10));
        publisher.set(Status::New);
    });

    subscriber.wait(isNewStatus);
    backgroundSetter.join();
}

Y_UNIT_TEST(test_subscriber_wait_for)
{
    auto publisher = statusPublisherOf(Status::Old);
    auto subscriber = publisher.subscribe();

    std::thread backgroundSetter([&] {
        std::this_thread::sleep_for(milliseconds(1));
        publisher.set(Status::New);
    });

    auto waitStatus = subscriber.waitFor(milliseconds(500), isNewStatus);
    backgroundSetter.join();
    EXPECT_EQ(waitStatus, SubscriptionStatus::Ready);
}

Y_UNIT_TEST(test_subscriber_wait_until)
{
    auto publisher = statusPublisherOf(Status::Old);
    auto subscriber = publisher.subscribe();

    std::thread backgroundSetter([&] {
        std::this_thread::sleep_for(milliseconds(1));
        publisher.set(Status::New);
    });

    auto timePoint = std::chrono::system_clock::now() + milliseconds(500);
    auto waitStatus = subscriber.waitUntil(timePoint, isNewStatus);
    backgroundSetter.join();
    EXPECT_EQ(waitStatus, SubscriptionStatus::Ready);
}

Y_UNIT_TEST(test_publisher_moveable)
{
    auto publisherOld = statusPublisherOf(Status::Old);
    auto subscriberOld = publisherOld.subscribe();

    ASSERT_TRUE(publisherOld.valid());

    auto publisherNew = std::move(publisherOld);
    auto subscriberNew = publisherNew.subscribe();

    ASSERT_TRUE(!publisherOld.valid());
    ASSERT_TRUE(publisherNew.valid());

    publisherNew.set(Status::New);

    EXPECT_EQ(Status::New, subscriberOld.get());
    EXPECT_EQ(Status::New, subscriberNew.get());
}

Y_UNIT_TEST(test_subscriber_copyable)
{
    auto publisher = statusPublisherOf(Status::Old);

    auto subscriber1 = publisher.subscribe();
    auto subscriber2 = publisher.subscribe();
    StatusSubscriber<Status> subscriber3(subscriber1);

    publisher.set(Status::New);
    EXPECT_EQ(Status::New, subscriber1.get());
    EXPECT_EQ(Status::New, subscriber2.get());
    EXPECT_EQ(Status::New, subscriber3.get());
}

Y_UNIT_TEST(test_no_hang_when_many_subscribers)
{
    constexpr size_t NUM_THREADS = 100;
    auto publisher = statusPublisherOf(Status::Old);
    std::vector<std::thread> threads;
    threads.reserve(NUM_THREADS);

    Latch latch(NUM_THREADS + 1);
    for (size_t i = 0; i < NUM_THREADS; ++i) {
        threads.emplace_back([&] {
            auto subscriber = publisher.subscribe();
            latch.arrive();
            subscriber.wait(isNewStatus);
        });
    }
    latch.arrive();
    publisher.set(Status::New);
    for (auto& thread : threads) {
        thread.join();
    }
}

Y_UNIT_TEST(test_publisher_destruction_wakes_up_subscribers)
{
    std::unique_ptr<StatusPublisher<Status>> publisherPtr(
        new StatusPublisher<Status>(Status::Old));
    auto subscriber = publisherPtr->subscribe();

    std::thread t([&] {
        std::this_thread::sleep_for(milliseconds(1));
        publisherPtr.reset();
    });
    auto waitResult = subscriber.wait(isNewStatus);
    t.join();
    EXPECT_EQ(waitResult, SubscriptionStatus::Aborted);
}

Y_UNIT_TEST(test_subscriber_timeout)
{
    auto publisher = statusPublisherOf(Status::Old);
    auto subscriber = publisher.subscribe();
    auto waitResult = subscriber.waitFor(milliseconds(1), isNewStatus);
    EXPECT_EQ(waitResult, SubscriptionStatus::Timeout);
}

Y_UNIT_TEST(test_subscribe_on_empty_publisher_throws)
{
    StatusPublisher<int> publisher;
    EXPECT_THROW(publisher.subscribe(), std::logic_error);
}

} //Y_UNIT_TEST_SUITE

} //namespace tests
} //namespace concurrent
} //namespace maps

#include <yandex_io/libs/threading/lifetime.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(Lifetime) {
    Y_UNIT_TEST(testLifetimeInitialState)
    {
        Lifetime lifetime;
        UNIT_ASSERT_VALUES_EQUAL(lifetime.expired(), true);
    }

    Y_UNIT_TEST(testLifetimeTracker)
    {
        Lifetime lifetime;

        auto t1 = lifetime.tracker().lock();
        auto t2 = lifetime.tracker().lock();
        UNIT_ASSERT(t1);
        UNIT_ASSERT_EQUAL(t1, t2);
    }

    Y_UNIT_TEST(testLifetimeDie)
    {
        Lifetime lifetime;
        Lifetime::Tracker w1 = lifetime.tracker();
        UNIT_ASSERT(!w1.expired());
        lifetime.die();
        UNIT_ASSERT(w1.expired());
    }

    Y_UNIT_TEST(testLifetimeScope)
    {
        std::weak_ptr<const void> w1;
        {
            Lifetime lifetime;
            Lifetime::Tracker w1 = lifetime.tracker();
            UNIT_ASSERT(!w1.expired());
        }
        UNIT_ASSERT(w1.expired());
    }

    Y_UNIT_TEST(testLifetimeImmortal)
    {
        Lifetime::Tracker w1 = Lifetime::immortal.tracker();
        UNIT_ASSERT(!w1.expired());
    }

    Y_UNIT_TEST(testExternalLifetime1)
    {
        auto owner = std::make_shared<int>(555);

        Lifetime::Tracker w1{owner};
        UNIT_ASSERT(!w1.expired());

        owner.reset();
        UNIT_ASSERT(w1.expired());
    }

    Y_UNIT_TEST(testExternalLifetime2)
    {
        auto owner = std::make_shared<int>(555);
        std::weak_ptr<int> weakOwner = owner;

        Lifetime::Tracker w1{owner};
        UNIT_ASSERT(!w1.expired());
        UNIT_ASSERT(!weakOwner.expired());

        auto lock = w1.lock();

        owner.reset();
        UNIT_ASSERT(!w1.expired());
        UNIT_ASSERT(!weakOwner.expired());

        lock.reset();
        UNIT_ASSERT(w1.expired());
        UNIT_ASSERT(weakOwner.expired());
    }

    Y_UNIT_TEST(testExternalLifetime3)
    {
        auto owner = std::make_shared<int>(555);
        std::weak_ptr<int> weakOwner = owner;

        Lifetime::Tracker w1{weakOwner};
        UNIT_ASSERT(!w1.expired());

        owner.reset();
        UNIT_ASSERT(w1.expired());
    }

    Y_UNIT_TEST(testExternalLifetime4)
    {
        auto owner = std::make_shared<int>(555);
        std::weak_ptr<int> weakOwner = owner;

        Lifetime::Tracker w1{std::move(weakOwner)};
        UNIT_ASSERT(weakOwner.expired());
        UNIT_ASSERT(!w1.expired());

        owner.reset();
        UNIT_ASSERT(w1.expired());
    }

    Y_UNIT_TEST(testLifetimeReusability)
    {
        Lifetime lifetime;

        auto t1 = lifetime.tracker();
        lifetime.die();
        auto t2 = lifetime.tracker();

        UNIT_ASSERT(t1.expired());
        UNIT_ASSERT(!t2.expired());

        lifetime.die();

        UNIT_ASSERT(t1.expired());
        UNIT_ASSERT(t2.expired());
    }

    Y_UNIT_TEST(testExternalMultiThread1)
    {
        struct A {
        };

        auto a = std::make_shared<A>();
        std::weak_ptr weakA = a;

        Lifetime::Tracker trackerA{a};
        UNIT_ASSERT(!trackerA.expired());
        UNIT_ASSERT(!weakA.expired());

        {
            auto lockA = trackerA.lock();
            std::atomic<bool> wait{true};
            auto handle = std::async(std::launch::async,
                                     [&] {
                                         waitUntil([&] { return wait == false; });
                                         UNIT_ASSERT(!weakA.expired()); // A still alive
                                     });
            a.reset(); // reset "main" instance of shared_ptr A
            UNIT_ASSERT(!trackerA.expired());
            UNIT_ASSERT(!weakA.expired());
            wait = false;
            handle.wait();
        }
        UNIT_ASSERT(trackerA.expired());
        UNIT_ASSERT(weakA.expired());
    }

    Y_UNIT_TEST(testExternalMultiThread2)
    {
        struct A {
        };

        auto a = std::make_shared<A>();
        std::weak_ptr weakA = a;

        Lifetime::Tracker trackerA{a};
        UNIT_ASSERT(!trackerA.expired());
        UNIT_ASSERT(!weakA.expired());

        std::atomic<int> stage{0};
        auto handle = std::async(std::launch::async,
                                 [&] {
                                     {
                                         auto lock = trackerA.lock();
                                         stage = 1;
                                         waitUntil([&] { return stage == 2; });
                                         UNIT_ASSERT(!weakA.expired()); // A still alive
                                     }
                                     stage = 3;
                                     waitUntil([&] { return stage == 4; });
                                 });
        waitUntil([&] { return stage == 1; });
        a.reset(); // reset "main" instance of shared_ptr A when tracker was locked
        UNIT_ASSERT(!trackerA.expired());
        UNIT_ASSERT(!weakA.expired());
        stage = 2; // Release async awating
        waitUntil([&] { return stage == 3; });
        // async unlock tracker
        UNIT_ASSERT(trackerA.expired());
        UNIT_ASSERT(weakA.expired());
        stage = 4; // finish async
        handle.wait();
    }

    Y_UNIT_TEST(LockTrackerWhileDie)
    {
        Lifetime lifetime;
        std::atomic<int> stage{0};
        auto tracker = lifetime.tracker();
        auto handle = std::async(
            std::launch::async,
            [&] {
                auto lock = tracker.lock();
                UNIT_ASSERT(lock); // lock successful, die() can't finish while this lock will alive
                stage = 1;
                waitUntil([&] { return stage == 2; });

                stage = 3;
                waitUntil([&] { return !tracker.lock(); }); // old tracker die and can't be locked

                auto newLock = lifetime.tracker().lock(); // new tracker can't be locked while last old lock will alive
                UNIT_ASSERT(!newLock);

                stage = 4;
            });

        waitUntil([&] { return stage == 1; });
        {
            auto lock = tracker.lock();
            UNIT_ASSERT(lock);
        }
        stage = 2;
        waitUntil([&] { return stage == 3; });

        lifetime.die();
        UNIT_ASSERT(stage == 4);

        handle.wait();
    }

}

#include <yandex_io/callkit/util/observer_list.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <functional>
#include <string>
#include <tuple>

using messenger::Callback;
using messenger::CallbackSubscription;
using messenger::ObserverList;

template <typename... Args>
struct ObserverTestHelper {
    std::function<void(Args...)> get() {
        return [this](Args... args) { this->onCall(args...); };
    }

    virtual void onCall(Args... args) {
        count++;
        lastArgs = std::make_tuple(std::forward<Args>(args)...);
    }

    size_t count = 0u;
    std::tuple<Args...> lastArgs;
};

template <typename... Args>
struct ObserverTestAdderHelper: public ObserverTestHelper<Args...> {
    using ListType = ObserverList<Args...>;
    using HelperType = ObserverTestHelper<Args...>;

    ObserverTestAdderHelper(ListType* list, HelperType* other)
        : list(list)
        , other(other)
    {
    }

    void onCall(Args... args) override {
        HelperType::onCall(args...);
        if (!subscription) {
            subscription = list->subscribe(other->get());
        }
    }

    ListType* list;
    HelperType* other;
    typename ListType::ScopedSubscription subscription;
};

template <typename... Args>
struct ObserverTestRemoverHelper: public ObserverTestHelper<Args...> {
    using ListType = ObserverList<Args...>;
    using HelperType = ObserverTestHelper<Args...>;

    ObserverTestRemoverHelper(
        ListType* list, typename ListType::ScopedSubscription subscription)
        : list(list)
        , subscription(std::move(subscription))
    {
    }

    ObserverTestRemoverHelper(ListType* list)
        : list(list)
        , subscription(nullptr)
    {
    }

    void onCall(Args... args) override {
        HelperType::onCall(args...);
        if (subscription) {
            subscription.reset();
        }
    }

    ListType* list;
    HelperType* other;
    typename ListType::ScopedSubscription subscription;
};

Y_UNIT_TEST_SUITE_F(TestObserverList, QuasarUnitTestFixture) {
    Y_UNIT_TEST(basic_test) {
        ObserverList<> list;
        ObserverTestHelper<> o1;
        ObserverTestHelper<> o2;
        ObserverTestHelper<> o3;
        auto s1 = list.subscribe(o1.get());
        auto s2 = list.subscribe(o2.get());
        auto s3 = list.subscribe(o3.get());

        list.notifyObservers();
        list.notifyObservers();

        UNIT_ASSERT_VALUES_EQUAL(2u, o1.count);
        UNIT_ASSERT_VALUES_EQUAL(2u, o2.count);
        UNIT_ASSERT_VALUES_EQUAL(2u, o3.count);
    }

    Y_UNIT_TEST(call_arguments) {
        ObserverList<int, std::string> list;
        ObserverTestHelper<int, std::string> o1;
        ObserverTestHelper<int, std::string> o2;
        ObserverTestHelper<int, std::string> o3;
        auto s1 = list.subscribe(o1.get());
        auto s2 = list.subscribe(o2.get());
        auto s3 = list.subscribe(o3.get());

        list.notifyObservers(-56, "Ho ho ho");

        UNIT_ASSERT_VALUES_EQUAL(-56, std::get<0>(o1.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL("Ho ho ho", std::get<1>(o1.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL(-56, std::get<0>(o2.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL("Ho ho ho", std::get<1>(o2.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL(-56, std::get<0>(o3.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL("Ho ho ho", std::get<1>(o3.lastArgs));
    }

    Y_UNIT_TEST(subscription_scope) {
        ObserverList<int> list;
        ObserverTestHelper<int> o1;
        ObserverTestHelper<int> o2;
        ObserverTestHelper<int> o3;

        {
            auto s1 = list.subscribe(o1.get());
            {
                auto s2 = list.subscribe(o2.get());
                {
                    auto s3 = list.subscribe(o3.get());
                    list.notifyObservers(1);
                }
                list.notifyObservers(2);
            }
            list.notifyObservers(3);
        }
        list.notifyObservers(4);

        UNIT_ASSERT_VALUES_EQUAL(3u, o1.count);
        UNIT_ASSERT_VALUES_EQUAL(2u, o2.count);
        UNIT_ASSERT_VALUES_EQUAL(1u, o3.count);
        UNIT_ASSERT_VALUES_EQUAL(3, std::get<0>(o1.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL(2, std::get<0>(o2.lastArgs));
        UNIT_ASSERT_VALUES_EQUAL(1, std::get<0>(o3.lastArgs));
    }

    Y_UNIT_TEST(added_while_notify) {
        ObserverList<> list;
        ObserverTestHelper<> o2;
        ObserverTestAdderHelper<> o1(&list, &o2);
        auto s1 = list.subscribe(o1.get());

        list.notifyObservers();
        list.notifyObservers();

        UNIT_ASSERT_VALUES_EQUAL(2u, o1.count);
        UNIT_ASSERT_VALUES_EQUAL(1u, o2.count);
    }

    Y_UNIT_TEST(removed_while_notify) {
        ObserverList<> list;
        ObserverTestHelper<> o1;
        ObserverTestHelper<> o4;
        auto s1 = list.subscribe(o1.get());
        ObserverTestRemoverHelper<> o2(&list, std::move(s1));
        auto s2 = list.subscribe(o2.get());
        ObserverTestRemoverHelper<> o3(&list);
        auto s3 = list.subscribe(o3.get());
        auto s4 = list.subscribe(o4.get());
        o3.subscription = std::move(s4);

        list.notifyObservers();
        list.notifyObservers();

        UNIT_ASSERT_VALUES_EQUAL(1u, o1.count);
        UNIT_ASSERT_VALUES_EQUAL(2u, o2.count);
        UNIT_ASSERT_VALUES_EQUAL(2u, o3.count);
        UNIT_ASSERT_VALUES_EQUAL(0u, o4.count);
    }

    Y_UNIT_TEST(self_removed_while_notify) {
        ObserverList<> list;
        ObserverTestRemoverHelper<> o1(&list);
        auto s1 = list.subscribe(o1.get());
        o1.subscription = std::move(s1);

        list.notifyObservers();
        list.notifyObservers();

        UNIT_ASSERT_VALUES_EQUAL(nullptr, o1.subscription.get());
        UNIT_ASSERT_VALUES_EQUAL(1u, o1.count);
    }
}

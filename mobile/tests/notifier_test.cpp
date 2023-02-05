#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/notifier.h>

namespace yandex {
namespace metrokit {

namespace {

const auto TEST_STRING_1 = "asd";
const auto TEST_NUMBER_1 = 12;
const auto TEST_STRING_2 = "assal;dfjadjfd";
const auto TEST_NUMBER_2 = 122;

} // namespace

class TestListener {
public:
    virtual ~TestListener() = default;
    virtual void didChangeString(const std::string&) = 0;
    virtual void didChangeNumber(int) = 0;
};

class TestHandler: public TestListener {
public:
    std::string str;
    int number;
    
    // TestListener
    
    void didChangeString(const std::string& str) override {
        this->str = str;
    }
    
    void didChangeNumber(int number) override {
        this->number = number;
    }
};

template<typename T>
auto countSubscribers(Notifier<T>& notifier) -> size_t {
    auto result = size_t { 0 };
    notifier.forEach([&result](auto) {
        result += 1;
    });
    
    return result;
}

BOOST_AUTO_TEST_CASE(subscribe_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler = makeShared<TestHandler>();
    
    notifier.subscribe(handler);
    
    BOOST_CHECK_EQUAL(countSubscribers(notifier), 1);
}

BOOST_AUTO_TEST_CASE(no_duplicate_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler = makeShared<TestHandler>();
    
    notifier.subscribe(handler);
    notifier.subscribe(handler);
    notifier.subscribe(handler);
    
    BOOST_CHECK_EQUAL(countSubscribers(notifier), 1);
}

BOOST_AUTO_TEST_CASE(notify_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler = makeShared<TestHandler>();
    
    notifier.subscribe(handler);
    notifier.notify(&TestListener::didChangeString, TEST_STRING_1);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_1);
    
    BOOST_CHECK_EQUAL(handler->str, TEST_STRING_1);
    BOOST_CHECK_EQUAL(handler->number, TEST_NUMBER_1);
}

BOOST_AUTO_TEST_CASE(unsubscribe_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler = makeShared<TestHandler>();
    notifier.subscribe(handler);
    
    notifier.notify(&TestListener::didChangeString, TEST_STRING_1);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_1);
    
    notifier.unsubscribe(handler);
    
    notifier.notify(&TestListener::didChangeString, TEST_STRING_2);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_2);
    
    BOOST_CHECK_EQUAL(handler->str, TEST_STRING_1);
    BOOST_CHECK_EQUAL(handler->number, TEST_NUMBER_1);
}

BOOST_AUTO_TEST_CASE(multiple_listeners_subscribe_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler1 = makeShared<TestHandler>();
    auto handler2 = makeShared<TestHandler>();
    
    notifier.subscribe(handler1);
    notifier.subscribe(handler2);
    
    BOOST_CHECK_EQUAL(countSubscribers(notifier), 2);
}

BOOST_AUTO_TEST_CASE(multiple_listeners_notify_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler1 = makeShared<TestHandler>();
    auto handler2 = makeShared<TestHandler>();
    
    notifier.subscribe(handler1);
    notifier.subscribe(handler2);
    notifier.notify(&TestListener::didChangeString, TEST_STRING_1);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_1);
    
    BOOST_CHECK_EQUAL(handler1->str, TEST_STRING_1);
    BOOST_CHECK_EQUAL(handler1->number, TEST_NUMBER_1);
    
    BOOST_CHECK_EQUAL(handler2->str, TEST_STRING_1);
    BOOST_CHECK_EQUAL(handler2->number, TEST_NUMBER_1);
}

BOOST_AUTO_TEST_CASE(multiple_listeners_unsubscribe_test) {
    auto notifier = Notifier<TestListener> {};
    auto handler1 = makeShared<TestHandler>();
    auto handler2 = makeShared<TestHandler>();
    
    notifier.subscribe(handler1);
    notifier.subscribe(handler2);
    
    notifier.notify(&TestListener::didChangeString, TEST_STRING_1);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_1);
    
    notifier.unsubscribe(handler1);
    
    notifier.notify(&TestListener::didChangeString, TEST_STRING_2);
    notifier.notify(&TestListener::didChangeNumber, TEST_NUMBER_2);
    
    BOOST_CHECK_EQUAL(countSubscribers(notifier), 1);
    
    BOOST_CHECK_EQUAL(handler1->str, TEST_STRING_1);
    BOOST_CHECK_EQUAL(handler1->number, TEST_NUMBER_1);
    
    BOOST_CHECK_EQUAL(handler2->str, TEST_STRING_2);
    BOOST_CHECK_EQUAL(handler2->number, TEST_NUMBER_2);
}

BOOST_AUTO_TEST_CASE(weak_subscription_test) {
    auto notifier = Notifier<TestListener> {};
    
    {
        auto handler = makeShared<TestHandler>();
        notifier.subscribe(handler);
    }
    
    BOOST_CHECK_EQUAL(countSubscribers(notifier), 0);
}

} }

#include <maps/infra/yacare/include/termination_handling.h>

#include <library/cpp/sighandler/async_signals_handler.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <memory>

using namespace std::literals;

namespace yacare::tests {

Y_UNIT_TEST_SUITE(test_termination_handling) {
Y_UNIT_TEST(invocation_order)
{
    SetAsyncSignalHandler(SIGTERM, [](int) {
        detail::invokeTerminationHandlers();
    });

    testing::MockFunction<void()> earlierHandler;
    testing::MockFunction<void()> laterHandler;
    auto event{std::make_shared<std::promise<void>>()};
    {
        testing::InSequence sequense;
        EXPECT_CALL(laterHandler, Call()).Times(1);
        EXPECT_CALL(earlierHandler, Call()).WillOnce([event] {
            event->set_value();
        });
    }

    addTerminationHandler(earlierHandler.AsStdFunction(), "earlier");
    addTerminationHandler(laterHandler.AsStdFunction(), "later");

    std::raise(SIGTERM);
    ASSERT_EQ(event->get_future().wait_for(10s), std::future_status::ready);
}

Y_UNIT_TEST(add_handler_from_another)
{
    SetAsyncSignalHandler(SIGTERM, [](int) {
        detail::invokeTerminationHandlers();
    });

    auto event{std::make_shared<std::promise<void>>()};
    addTerminationHandler(
        [event] {
            addTerminationHandler([event] { event->set_value(); }, "inner");
        },
        "outer");

    std::raise(SIGTERM);
    ASSERT_EQ(event->get_future().wait_for(10s), std::future_status::ready);
}

Y_UNIT_TEST(raise_exception_from_handler)
{
    SetAsyncSignalHandler(SIGTERM, [](int) {
        detail::invokeTerminationHandlers();
    });

    testing::MockFunction<void()> goodHandler;
    testing::MockFunction<void()> faultyHandler;
    auto event{std::make_shared<std::promise<void>>()};
    {
        testing::InSequence sequense;
        EXPECT_CALL(faultyHandler, Call())
            .WillOnce(testing::Throw(std::runtime_error{"failure"}));
        EXPECT_CALL(goodHandler, Call()).WillOnce([event] {
            event->set_value();
        });
    }

    addTerminationHandler(goodHandler.AsStdFunction(), "good");
    addTerminationHandler(faultyHandler.AsStdFunction(), "faulty");

    std::raise(SIGTERM);
    ASSERT_EQ(event->get_future().wait_for(10s), std::future_status::ready);
}
} // Y_UNIT_TEST_SUITE(test_termination_handling)
} // namespace yacare::tests

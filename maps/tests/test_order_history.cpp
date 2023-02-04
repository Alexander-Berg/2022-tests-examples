#include "maps/b2bgeo/on_demand/libs/state/include/order.h"
#include "maps/b2bgeo/on_demand/libs/state/include/order_history.h"

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::b2bgeo::on_demand::state {

namespace {

OrdersMap makeOrdersMap(TimePoint start, size_t count, size_t startId = 0)
{
    OrdersMap result;
    for (size_t i = 0; i < count; ++i) {
        Order order{
            .id = Order::Id{std::to_string(startId + i)},
            .createdAt = start + Duration{i},
            .size = ShipmentSize{Weight{0}, Units{0}},
            .fromDepotId = Depot::Id{""}};
        result.emplace(order.id, order);
    }
    return result;
}

} // anonymous namespace

TEST(TestOrderHistory, CheckStatisticsWithOneAdd)
{
    const size_t count = 10;
    const auto start = TimePoint{Duration{0}} + Duration{1};
    const auto orders = makeOrdersMap(start, count);
    OrderHistory orderHistory;
    orderHistory.addOrders(orders, start + Duration{60 * 10 + 5});
    const auto statistics = orderHistory.getStatistic();
    EXPECT_EQ(statistics[OrderStatisticsPosition::shortWindow].value(), 5UL);
    EXPECT_EQ(statistics[OrderStatisticsPosition::longWindow].value(), 10UL);
}

TEST(TestOrderHistory, CheckStatisticsWithFewAdds)
{
    const size_t count = 10;
    const auto start1 = TimePoint{Duration{0}} + Duration{1};
    const auto start2 = start1 + Duration{60 * 10 - 5};
    const auto start3 = start1 + Duration{60 * 60 - 3};
    const auto orders1 = makeOrdersMap(start1, count);
    const auto orders2 = makeOrdersMap(start2, count, 100);
    const auto orders3 = makeOrdersMap(start3, count, 200);
    OrderHistory orderHistory;
    orderHistory.addOrders(orders1, start1 + Duration{60});
    auto statistics = orderHistory.getStatistic();
    EXPECT_EQ(statistics[OrderStatisticsPosition::shortWindow].value(), 10UL);
    EXPECT_EQ(statistics[OrderStatisticsPosition::longWindow].value(), 10UL);
    orderHistory.addOrders(orders2, start2 + Duration{10});
    statistics = orderHistory.getStatistic();
    EXPECT_EQ(statistics[OrderStatisticsPosition::shortWindow].value(), 15UL);
    EXPECT_EQ(statistics[OrderStatisticsPosition::longWindow].value(), 20UL);
    orderHistory.addOrders(orders3, start3 + Duration{10});
    statistics = orderHistory.getStatistic();
    EXPECT_EQ(statistics[OrderStatisticsPosition::shortWindow].value(), 10UL);
    EXPECT_EQ(statistics[OrderStatisticsPosition::longWindow].value(), 23UL);
}

} // namespace maps::b2bgeo::on_demand::state

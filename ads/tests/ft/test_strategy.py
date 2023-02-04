import pytest

from ads.bsyeti.libs.events.proto.strategy_update_pb2 import TStrategyUpdate
from ads.bsyeti.caesar.libs.profiles.proto.order_pb2 import TOrderProfileProto
from ads.bsyeti.caesar.libs.profiles.python import get_order_table_row_from_proto
from ads.bsyeti.caesar.tests.ft.common import select_profiles
from ads.bsyeti.caesar.tests.ft.common.event import make_event


def _make_order_row(order_id, strategy_id=None):
    order = TOrderProfileProto()
    order.OrderID = order_id
    order.Resources.AutoBudget.StrategyParams.StrategyID = strategy_id or order_id
    serialized_profile = order.SerializeToString()
    return get_order_table_row_from_proto(str(order_id), serialized_profile)


@pytest.mark.extra_profiles(
    {
        "Orders": [_make_order_row(1, 1), _make_order_row(2, 2),  _make_order_row(3, 3)],
    }
)
@pytest.mark.table("Strategies")
def test_profiles(yt_cluster, caesar, tables, queue, get_timestamp):
    expected = {}
    with queue.writer() as queue_writer:
        for strategy_id in range(3):
            body = TStrategyUpdate()
            body.TimeStamp = get_timestamp(2021)
            body.StrategyID = strategy_id
            body.OrderID = strategy_id
            body.Command = TStrategyUpdate.ADD_ORDER
            event = make_event(body.StrategyID, body.TimeStamp, body)
            queue_writer.write(event)

            body = TStrategyUpdate()
            body.TimeStamp = get_timestamp(2022)
            body.StrategyID = strategy_id
            body.OrderID = strategy_id + 1
            body.Command = TStrategyUpdate.ADD_ORDER
            event = make_event(body.StrategyID, body.TimeStamp, body)
            queue_writer.write(event)

            expected[strategy_id] = body.StrategyID

    profiles = select_profiles(yt_cluster, tables, "Strategies")
    assert len(expected) - 1 == len(profiles)  # ID shouldn't be equal to zero
    for profile in profiles:
        assert expected[profile.StrategyID] == profile.StrategyID
        assert len(profile.StrategyOrders.OrderID) == 1
        assert profile.StrategyOrders.OrderID[0] == profile.StrategyID  # will be removed after experiment
        assert len(profile.StrategyOrders.OrderIDs) == 1
        assert profile.StrategyOrders.OrderIDs[0] == profile.StrategyID


@pytest.mark.extra_profiles(
    {
        "Orders": [_make_order_row(1, 1), _make_order_row(2, 2)],
    }
)
@pytest.mark.table("Strategies")
def test_deleted_profiles(yt_cluster, caesar, tables, queue, get_timestamp):
    expected = {}
    with queue.writer() as queue_writer:
        for strategy_id in range(3):
            body = TStrategyUpdate()
            body.TimeStamp = get_timestamp(2021)
            body.StrategyID = strategy_id
            body.OrderID = strategy_id
            body.Command = TStrategyUpdate.ADD_ORDER
            event = make_event(body.StrategyID, body.TimeStamp, body)
            queue_writer.write(event)

            body = TStrategyUpdate()
            body.TimeStamp = get_timestamp(2022)
            body.StrategyID = strategy_id
            body.OrderID = strategy_id
            body.Command = TStrategyUpdate.DELETE_ORDER
            event = make_event(body.StrategyID, body.TimeStamp, body)
            queue_writer.write(event)

            expected[strategy_id] = body.StrategyID

    profiles = select_profiles(yt_cluster, tables, "Strategies")
    assert len(expected) - 1 == len(profiles)  # ID shouldn't be equal to zero
    for profile in profiles:
        assert expected[profile.StrategyID] == profile.StrategyID
        assert profile.StrategyOrders.OrderID[0] == profile.StrategyID  # will be removed after experiment
        assert not profile.StrategyOrders.OrderIDs

from datetime import timedelta

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    OrderByField,
    OrderDirection,
    OrderEvent,
)
from maps_adv.geosmb.doorman.server.lib.exceptions import BadClientData
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


def make_input_params(**overrides):
    params = dict(biz_id=123, limit=100500, offset=0)
    params.update(overrides)

    return params


async def test_sorts_by_created_time_by_default(factory, dm):
    id_1 = await factory.create_empty_client()
    id_2 = await factory.create_empty_client()
    id_3 = await factory.create_empty_client()

    _, got = await dm.list_clients(**make_input_params())

    assert extract_ids(got) == [id_3, id_2, id_1]


@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (OrderByField.EMAIL, "email"),
        (OrderByField.PHONE, "phone"),
        (OrderByField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [222, 111, 444, 333]),
        (OrderDirection.DESC, [333, 444, 111, 222]),
    ],
)
async def test_sorts_by_single_column(
    factory, dm, order_by_field, db_sorted_column, order_direction, expected_ids
):
    await factory.create_empty_client(client_id=111, **{db_sorted_column: "2"})
    await factory.create_empty_client(client_id=222, **{db_sorted_column: "1"})
    await factory.create_empty_client(client_id=333, **{db_sorted_column: None})
    await factory.create_empty_client(client_id=444, **{db_sorted_column: "3"})

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=order_by_field, order_direction=order_direction
        )
    )

    assert extract_ids(got) == expected_ids


@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (OrderByField.EMAIL, "email"),
        (OrderByField.PHONE, "phone"),
        (OrderByField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_secondary_sorts_by_created_when_sort_by_single_column(
    factory, dm, order_by_field, db_sorted_column, order_direction
):
    for idx in [111, 222, 333]:
        await factory.create_empty_client(
            client_id=idx, passport_uid=idx, **{db_sorted_column: "1"}
        )

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=order_by_field, order_direction=order_direction
        )
    )

    assert extract_ids(got) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [444, 666, 555, 777, 999, 888, 333, 222, 111]),
        (OrderDirection.DESC, [111, 222, 333, 888, 999, 777, 555, 666, 444]),
    ],
)
async def test_sorts_by_name(factory, dm, order_direction, expected_ids):
    await factory.create_empty_client(client_id=111, first_name=None, last_name=None)
    await factory.create_empty_client(client_id=222, first_name=None, last_name="5")
    await factory.create_empty_client(client_id=333, first_name=None, last_name="4")
    await factory.create_empty_client(client_id=444, first_name="1", last_name=None)
    await factory.create_empty_client(client_id=555, first_name="1", last_name="7")
    await factory.create_empty_client(client_id=666, first_name="1", last_name="6")
    await factory.create_empty_client(client_id=777, first_name="2", last_name=None)
    await factory.create_empty_client(client_id=888, first_name="2", last_name="9")
    await factory.create_empty_client(client_id=999, first_name="2", last_name="8")

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=OrderByField.FIRST_AND_LAST_NAME,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids


@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_secondary_sorts_by_created_at_when_sort_by_name(
    factory, dm, order_direction
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(
            client_id=client_id, first_name="Кек", last_name="Кекович"
        )

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=OrderByField.FIRST_AND_LAST_NAME,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (OrderByField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (OrderByField.STAT_ORDERS_SUCCESSFUL, OrderEvent.ACCEPTED),
        (OrderByField.STAT_ORDERS_UNSUCCESSFUL, OrderEvent.REJECTED),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [111, 222, 333, 444]),
        (OrderDirection.DESC, [444, 333, 222, 111]),
    ],
)
async def test_sorts_by_orders_stat(
    factory, dm, order_by_field, order_direction, event_type, expected_ids
):
    for idx, client_id in enumerate([111, 222, 333, 444]):
        await factory.create_empty_client(client_id=client_id)
        for _ in range(idx):
            await factory.create_order_event(client_id=client_id, event_type=event_type)

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=order_by_field, order_direction=order_direction
        )
    )

    assert extract_ids(got) == expected_ids


@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (OrderByField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (OrderByField.STAT_ORDERS_SUCCESSFUL, OrderEvent.ACCEPTED),
        (OrderByField.STAT_ORDERS_UNSUCCESSFUL, OrderEvent.REJECTED),
    ],
)
@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_secondary_sorts_by_created_at_when_sort_by_orders_stat(
    factory, dm, order_by_field, order_direction, event_type
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(client_id=client_id, event_type=event_type)

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=order_by_field, order_direction=order_direction
        )
    )

    assert extract_ids(got) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [444, 333, 222, 111]),
        (OrderDirection.DESC, [111, 222, 333, 444]),
    ],
)
async def test_sorts_by_last_order_ts(factory, dm, order_direction, expected_ids):
    event_latest_ts = dt("2020-03-03 00:00:00")
    for idx, client_id in enumerate([111, 222, 333, 444]):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=idx),
        )

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=OrderByField.STAT_ORDERS_LAST_ORDER_TS,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids


@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_secondary_sorts_by_created_at_when_sort_by_last_order_ts(
    factory, dm, order_direction
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=OrderByField.STAT_ORDERS_LAST_ORDER_TS,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == [333, 222, 111]


async def test_sorts_ignoring_search_ranking(factory, dm):
    await factory.create_empty_client(client_id=111, comment="ccc")
    await factory.create_empty_client(client_id=222, comment="bbb Иванов Вася")
    await factory.create_empty_client(
        client_id=333, comment="aaa Иванов Вася 123456789"
    )
    await factory.create_empty_client(client_id=444, comment="ddd Вася")

    _, got = await dm.list_clients(
        **make_input_params(
            order_by_field=OrderByField.COMMENT,
            order_direction=OrderDirection.ASC,
            search_string="Вася Иванов 123456789 email@yandex.ru работает",
        )
    )

    assert extract_ids(got) == [333, 222, 444]


async def test_raises_for_incomplete_sorting_params(factory, dm):
    with pytest.raises(BadClientData) as exc:
        await dm.list_clients(
            **make_input_params(
                order_by_field=OrderByField.COMMENT, order_direction=None
            )
        )

    assert exc.value.args == ("Order direction must be set with order field",)


@pytest.mark.parametrize("offset", range(2))
async def test_respects_default_sorting_in_pagination(factory, dm, offset):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(6)]))

    _, got = await dm.list_clients(**make_input_params(limit=2, offset=offset))

    assert extract_ids(got) == client_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (OrderByField.EMAIL, "email"),
        (OrderByField.PHONE, "phone"),
        (OrderByField.FIRST_AND_LAST_NAME, "first_name"),
        (OrderByField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [111, 222, 333, 444, 555, 666]),
        (OrderDirection.DESC, [666, 555, 444, 333, 222, 111]),
    ],
)
async def test_respects_sorting_by_column_in_pagination(
    factory, dm, offset, order_by_field, db_sorted_column, order_direction, expected_ids
):
    for idx, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(
            client_id=client_id, **{db_sorted_column: str(idx)}
        )

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=order_by_field,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (OrderByField.EMAIL, "email"),
        (OrderByField.PHONE, "phone"),
        (OrderByField.FIRST_AND_LAST_NAME, "first_name"),
        (OrderByField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_column(
    factory, dm, offset, order_by_field, db_sorted_column, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for idx in reversed(expected_ids):
        await factory.create_empty_client(
            client_id=idx, passport_uid=idx, **{db_sorted_column: "1"}
        )

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=order_by_field,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (OrderByField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (OrderByField.STAT_ORDERS_SUCCESSFUL, OrderEvent.ACCEPTED),
        (OrderByField.STAT_ORDERS_UNSUCCESSFUL, OrderEvent.REJECTED),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [111, 222, 333, 444, 555, 666]),
        (OrderDirection.DESC, [666, 555, 444, 333, 222, 111]),
    ],
)
async def test_respects_sorting_by_orders_stat_in_pagination(
    factory, dm, offset, order_by_field, event_type, order_direction, expected_ids
):
    for idx, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(client_id=client_id)
        for _ in range(idx):
            await factory.create_order_event(client_id=client_id, event_type=event_type)

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=order_by_field,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (OrderByField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (OrderByField.STAT_ORDERS_SUCCESSFUL, OrderEvent.ACCEPTED),
        (OrderByField.STAT_ORDERS_UNSUCCESSFUL, OrderEvent.REJECTED),
    ],
)
@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_orders_stat(
    factory, dm, offset, order_by_field, event_type, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for client_id in reversed(expected_ids):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(client_id=client_id, event_type=event_type)

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=order_by_field,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (OrderDirection.ASC, [666, 555, 444, 333, 222, 111]),
        (OrderDirection.DESC, [111, 222, 333, 444, 555, 666]),
    ],
)
async def test_respects_sorting_by_last_order_ts_in_pagination(
    factory, dm, offset, order_direction, expected_ids
):
    event_latest_ts = dt("2020-03-03 00:00:00")
    for idx, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=idx),
        )

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=OrderByField.STAT_ORDERS_LAST_ORDER_TS,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize("order_direction", OrderDirection)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_last_order_ts(
    factory, dm, offset, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for client_id in reversed(expected_ids):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    _, got = await dm.list_clients(
        **make_input_params(
            limit=2,
            offset=offset,
            order_by_field=OrderByField.STAT_ORDERS_LAST_ORDER_TS,
            order_direction=order_direction,
        )
    )

    assert extract_ids(got) == expected_ids[offset : offset + 2]  # noqa

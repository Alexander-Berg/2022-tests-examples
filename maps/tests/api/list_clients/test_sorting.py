from datetime import timedelta

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2
from maps_adv.geosmb.doorman.server.lib.enums import OrderEvent
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


url = "/v1/list_clients/"


def make_input_pb(**overrides):
    params = dict(biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0))
    params.update(overrides)

    return clients_pb2.ClientsListInput(**params)


@pytest.mark.real_db
async def test_sorts_by_created_time_by_default(factory, api):
    id_1 = await factory.create_empty_client()
    id_2 = await factory.create_empty_client()
    id_3 = await factory.create_empty_client()

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_3, id_2, id_1]


@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (clients_pb2.ClientsOrderBy.OrderField.EMAIL, "email"),
        (clients_pb2.ClientsOrderBy.OrderField.PHONE, "phone"),
        (clients_pb2.ClientsOrderBy.OrderField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [222, 111, 444, 333]),
        (clients_pb2.ClientsOrderBy.OrderDirection.DESC, [333, 444, 111, 222]),
    ],
)
async def test_sorts_by_single_column(
    factory, api, order_by_field, db_sorted_column, order_direction, expected_ids
):
    await factory.create_empty_client(client_id=111, **{db_sorted_column: "2"})
    await factory.create_empty_client(client_id=222, **{db_sorted_column: "1"})
    await factory.create_empty_client(client_id=333, **{db_sorted_column: None})
    await factory.create_empty_client(client_id=444, **{db_sorted_column: "3"})

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (clients_pb2.ClientsOrderBy.OrderField.EMAIL, "email"),
        (clients_pb2.ClientsOrderBy.OrderField.PHONE, "phone"),
        (clients_pb2.ClientsOrderBy.OrderField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_secondary_sorts_by_created_when_sort_by_single_column(
    factory, api, order_by_field, db_sorted_column, order_direction
):
    for idx in [111, 222, 333]:
        await factory.create_empty_client(
            client_id=idx, passport_uid=idx, **{db_sorted_column: "1"}
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (
            clients_pb2.ClientsOrderBy.OrderDirection.ASC,
            [444, 666, 555, 777, 999, 888, 333, 222, 111],
        ),
        (
            clients_pb2.ClientsOrderBy.OrderDirection.DESC,
            [111, 222, 333, 888, 999, 777, 555, 666, 444],
        ),
    ],
)
async def test_sorts_by_name(factory, api, order_direction, expected_ids):
    await factory.create_empty_client(client_id=111, first_name=None, last_name=None)
    await factory.create_empty_client(client_id=222, first_name=None, last_name="5")
    await factory.create_empty_client(client_id=333, first_name=None, last_name="4")
    await factory.create_empty_client(client_id=444, first_name="1", last_name=None)
    await factory.create_empty_client(client_id=555, first_name="1", last_name="7")
    await factory.create_empty_client(client_id=666, first_name="1", last_name="6")
    await factory.create_empty_client(client_id=777, first_name="2", last_name=None)
    await factory.create_empty_client(client_id=888, first_name="2", last_name="9")
    await factory.create_empty_client(client_id=999, first_name="2", last_name="8")

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.FIRST_AND_LAST_NAME,
                direction=order_direction,
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_secondary_sorts_by_created_at_when_sort_by_name(
    factory, api, order_direction
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(
            client_id=client_id, first_name="Кек", last_name="Кекович"
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.FIRST_AND_LAST_NAME,
                direction=order_direction,
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_SUCCESSFUL,
            OrderEvent.ACCEPTED,
        ),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_UNSUCCESSFUL,
            OrderEvent.REJECTED,
        ),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [111, 222, 333, 444]),
        (clients_pb2.ClientsOrderBy.OrderDirection.DESC, [444, 333, 222, 111]),
    ],
)
async def test_sorts_by_orders_stat(
    factory, api, order_by_field, order_direction, event_type, expected_ids
):
    for idx, client_id in enumerate([111, 222, 333, 444]):
        await factory.create_empty_client(client_id=client_id)
        for _ in range(idx):
            await factory.create_order_event(client_id=client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_SUCCESSFUL,
            OrderEvent.ACCEPTED,
        ),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_UNSUCCESSFUL,
            OrderEvent.REJECTED,
        ),
    ],
)
@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_secondary_sorts_by_created_at_when_sort_by_orders_stat(
    factory, api, order_by_field, order_direction, event_type
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(client_id=client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [333, 222, 111]


@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [444, 333, 222, 111]),
        (clients_pb2.ClientsOrderBy.OrderDirection.DESC, [111, 222, 333, 444]),
    ],
)
async def test_sorts_by_last_order_ts(factory, api, order_direction, expected_ids):
    event_latest_ts = dt("2020-03-03 00:00:00")
    for idx, client_id in enumerate([111, 222, 333, 444]):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=idx),
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_LAST_ORDER_TS,
                direction=order_direction,
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_secondary_sorts_by_created_at_when_sort_by_last_order_ts(
    factory, api, order_direction
):
    for client_id in [111, 222, 333]:
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_LAST_ORDER_TS,
                direction=order_direction,
            )
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [333, 222, 111]


async def test_sorts_ignoring_search_ranking(factory, api):
    await factory.create_empty_client(client_id=111, comment="ccc")
    await factory.create_empty_client(client_id=222, comment="bbb Иванов Вася")
    await factory.create_empty_client(
        client_id=333, comment="aaa Иванов Вася 123456789"
    )
    await factory.create_empty_client(client_id=444, comment="ddd Вася")

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.COMMENT,
                direction=clients_pb2.ClientsOrderBy.OrderDirection.ASC,
            ),
            search_string="Вася Иванов 123456789 email@yandex.ru работает",
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [333, 222, 444]


@pytest.mark.real_db
@pytest.mark.parametrize("offset", range(2))
async def test_respects_default_sorting_in_pagination(factory, api, offset):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(6)]))

    got = await api.post(
        url,
        proto=make_input_pb(pagination=common_pb2.Pagination(limit=2, offset=offset)),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (clients_pb2.ClientsOrderBy.OrderField.EMAIL, "email"),
        (clients_pb2.ClientsOrderBy.OrderField.PHONE, "phone"),
        (clients_pb2.ClientsOrderBy.OrderField.FIRST_AND_LAST_NAME, "first_name"),
        (clients_pb2.ClientsOrderBy.OrderField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [111, 222, 333, 444, 555, 666]),
        (
            clients_pb2.ClientsOrderBy.OrderDirection.DESC,
            [666, 555, 444, 333, 222, 111],
        ),
    ],
)
async def test_respects_sorting_by_column_in_pagination(
    factory,
    api,
    offset,
    order_by_field,
    db_sorted_column,
    order_direction,
    expected_ids,
):
    for i, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(
            client_id=client_id, **{db_sorted_column: str(i)}
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, db_sorted_column",
    [
        (clients_pb2.ClientsOrderBy.OrderField.EMAIL, "email"),
        (clients_pb2.ClientsOrderBy.OrderField.PHONE, "phone"),
        (clients_pb2.ClientsOrderBy.OrderField.FIRST_AND_LAST_NAME, "first_name"),
        (clients_pb2.ClientsOrderBy.OrderField.COMMENT, "comment"),
    ],
)
@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_column(
    factory, api, offset, order_by_field, db_sorted_column, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for idx in reversed(expected_ids):
        await factory.create_empty_client(
            client_id=idx, passport_uid=idx, **{db_sorted_column: "1"}
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_SUCCESSFUL,
            OrderEvent.ACCEPTED,
        ),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_UNSUCCESSFUL,
            OrderEvent.REJECTED,
        ),
    ],
)
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [111, 222, 333, 444, 555, 666]),
        (
            clients_pb2.ClientsOrderBy.OrderDirection.DESC,
            [666, 555, 444, 333, 222, 111],
        ),
    ],
)
async def test_respects_sorting_by_orders_stat_in_pagination(
    factory, api, offset, order_by_field, event_type, order_direction, expected_ids
):
    for idx, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(client_id=client_id)
        for _ in range(idx):
            await factory.create_order_event(client_id=client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_by_field, event_type",
    [
        (clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_TOTAL, OrderEvent.CREATED),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_SUCCESSFUL,
            OrderEvent.ACCEPTED,
        ),
        (
            clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_UNSUCCESSFUL,
            OrderEvent.REJECTED,
        ),
    ],
)
@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_orders_stat(
    factory, api, offset, order_by_field, event_type, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for client_id in reversed(expected_ids):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(client_id=client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=order_by_field, direction=order_direction
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_direction, expected_ids",
    [
        (clients_pb2.ClientsOrderBy.OrderDirection.ASC, [666, 555, 444, 333, 222, 111]),
        (
            clients_pb2.ClientsOrderBy.OrderDirection.DESC,
            [111, 222, 333, 444, 555, 666],
        ),
    ],
)
async def test_respects_sorting_by_last_order_ts_in_pagination(
    factory, api, offset, order_direction, expected_ids
):
    event_latest_ts = dt("2020-03-03 00:00:00")
    for idx, client_id in enumerate([111, 222, 333, 444, 555, 666]):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=idx),
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_LAST_ORDER_TS,
                direction=order_direction,
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa


@pytest.mark.parametrize("offset", range(2))
@pytest.mark.parametrize(
    "order_direction",
    [
        clients_pb2.ClientsOrderBy.OrderDirection.ASC,
        clients_pb2.ClientsOrderBy.OrderDirection.DESC,
    ],
)
async def test_respects_secondary_sorting_in_pagination_when_sort_by_last_order_ts(
    factory, api, offset, order_direction
):
    expected_ids = [666, 555, 444, 333, 222, 111]
    for client_id in reversed(expected_ids):
        await factory.create_empty_client(client_id=client_id)
        await factory.create_order_event(
            client_id=client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    got = await api.post(
        url,
        proto=make_input_pb(
            order_by=clients_pb2.ClientsOrderBy(
                field=clients_pb2.ClientsOrderBy.OrderField.STAT_ORDERS_LAST_ORDER_TS,
                direction=order_direction,
            ),
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids[offset : offset + 2]  # noqa

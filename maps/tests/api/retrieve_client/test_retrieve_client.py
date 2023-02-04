from datetime import timedelta

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import (
    clients_pb2,
    common_pb2,
    errors_pb2,
    statistics_pb2,
)
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, OrderEvent, Source

pytestmark = [pytest.mark.asyncio]

url = "v1/retrieve_client/"


@pytest.mark.parametrize("cleared_for_gdpr", [True, False])
async def test_returns_client_details(api, factory, cleared_for_gdpr):
    client_id = await factory.create_client(cleared_for_gdpr=cleared_for_gdpr)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=123, id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got == clients_pb2.ClientData(
        id=client_id,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=common_pb2.ClientGender.MALE,
        comment="this is comment",
        cleared_for_gdpr=cleared_for_gdpr,
        labels=["mark-2021"],
        segments=[SegmentTypePb.NO_ORDERS],
        statistics=statistics_pb2.ClientStatistics(
            orders=statistics_pb2.OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
        source=common_pb2.Source.CRM_INTERFACE,
        registration_timestamp=got.registration_timestamp,
    )


async def test_returns_source_from_first_revision(factory, api):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=123, id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.source == common_pb2.Source.CRM_INTERFACE


async def test_calculates_client_order_statistics(api, factory):
    client_id = await factory.create_empty_client()
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00") - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=123, id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.statistics == statistics_pb2.ClientStatistics(
        orders=statistics_pb2.OrderStatistics(
            total=4,
            successful=2,
            unsuccessful=1,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    )


async def test_call_events_does_not_affect_statistics(api, factory):
    client_id = await factory.create_empty_client()
    event_latest_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)
    for _ in range(5):
        await factory.create_call_event(client_id)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=123, id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.statistics == statistics_pb2.ClientStatistics(
        orders=statistics_pb2.OrderStatistics(
            total=4,
            successful=2,
            unsuccessful=1,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    )


async def test_does_not_return_last_order_timestamp_if_no_order_created_events(
    api, factory
):
    client_id = await factory.create_client(client_id=111)
    for event_type in (OrderEvent.ACCEPTED, OrderEvent.REJECTED, CallEvent.INITIATED):
        await factory.create_event(client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=123, id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.statistics.orders == statistics_pb2.OrderStatistics(
        total=0, successful=1, unsuccessful=1
    )


@pytest.mark.parametrize("biz_id, client_id", [(123, 9999), (9999, 564)])
async def test_returns_error_for_unknown_client(biz_id, client_id, factory, api):
    await factory.create_client(client_id=564)

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(biz_id=biz_id, id=client_id),
        decode_as=errors_pb2.Error,
        expected_status=404,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id={biz_id}, client_id={client_id}",
    )


@pytest.mark.parametrize("field_name", ["biz_id", "id"])
async def test_returns_error_for_wrong_input(api, field_name):
    input_params = dict(biz_id=123, id=564)
    input_params.update({field_name: 0})

    got = await api.post(
        url,
        proto=clients_pb2.ClientRetrieveInput(**input_params),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.VALIDATION_ERROR,
        description=f"{field_name}: ['Must be at least 1.']",
    )

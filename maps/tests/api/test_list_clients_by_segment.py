import pytest

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    ClientContacts,
    ClientContactsList,
    ClientsListBySegmentInput,
)
from maps_adv.geosmb.doorman.proto.errors_pb2 import Error
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.doorman.server.lib.enums import SegmentType as SegmentTypeEnum
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "/v1/list_clients_by_segment/"


@pytest.mark.parametrize(
    "segment_params, expected_error",
    [
        (dict(label=""), "label: ['Length must be between 1 and 256.']"),
        (dict(label="x" * 257), "label: ['Length must be between 1 and 256.']"),
        (dict(), "_schema: ['At least one of segment or label must be specified.']"),
    ],
)
async def test_return_error_for_bad_params(
    api, factory, segment_params, expected_error
):
    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, **segment_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.ERROR_CODE.VALIDATION_ERROR, description=expected_error
    )


@pytest.mark.parametrize(
    "segment, segment_enum",
    [
        (SegmentType.ACTIVE, SegmentTypeEnum.ACTIVE),
        (SegmentType.REGULAR, SegmentTypeEnum.REGULAR),
        (SegmentType.LOST, SegmentTypeEnum.LOST),
        (SegmentType.UNPROCESSED_ORDERS, SegmentTypeEnum.UNPROCESSED_ORDERS),
        (SegmentType.NO_ORDERS, SegmentTypeEnum.NO_ORDERS),
        (SegmentType.SHORT_LAST_CALL, SegmentTypeEnum.SHORT_LAST_CALL),
        (SegmentType.MISSED_LAST_CALL, SegmentTypeEnum.MISSED_LAST_CALL),
    ],
)
async def test_returns_list_of_clients_in_passed_segment(
    segment, segment_enum, api, factory
):
    client_id_1 = await factory.create_empty_client(
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        cleared_for_gdpr=True,
        segments=[segment_enum],
    )
    client_id_2 = await factory.create_empty_client(
        phone=78002000600,
        last_name="Лапенко",
        email="kek@cheburek.ru",
        cleared_for_gdpr=False,
        segments=[segment_enum],
    )

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, segment=segment),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    assert got == ClientContactsList(
        clients=[
            ClientContacts(
                id=client_id_1,
                biz_id=123,
                passport_uid=354628,
                first_name="Вася",
                last_name="Иванов",
                cleared_for_gdpr=True,
            ),
            ClientContacts(
                id=client_id_2,
                biz_id=123,
                last_name="Лапенко",
                phone=78002000600,
                email="kek@cheburek.ru",
                cleared_for_gdpr=False,
            ),
        ]
    )


async def test_returns_clients_matched_by_label(api, factory):
    id_1 = await factory.create_empty_client(labels=["orange"])
    id_2 = await factory.create_empty_client(labels=["lemon", "orange"])
    await factory.create_empty_client(labels=[])
    await factory.create_empty_client(labels=["kiwi"])

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, label="orange"),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1, id_2]


@pytest.mark.parametrize(
    "segment_params", [dict(segment=SegmentType.ACTIVE), dict(label="orange")]
)
async def test_returns_client_contact_details(api, factory, segment_params):
    client_id = await factory.create_empty_client(
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        phone=78002000600,
        email="kek@cheburek.ru",
        segments=[SegmentTypeEnum.ACTIVE],
        labels=["orange"],
    )

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, **segment_params),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    assert list(got.clients) == [
        ClientContacts(
            id=client_id,
            biz_id=123,
            passport_uid=354628,
            first_name="Вася",
            last_name="Иванов",
            phone=78002000600,
            email="kek@cheburek.ru",
            cleared_for_gdpr=False,
        )
    ]


async def test_does_not_return_clients_which_are_not_in_segment(api, factory):
    await factory.create_empty_client(segments=[SegmentTypeEnum.NO_ORDERS])
    client_id = await factory.create_empty_client(segments=[SegmentTypeEnum.REGULAR])

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, segment=SegmentType.REGULAR),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    clients = got.clients
    assert len(clients) == 1
    assert clients[0].id == client_id


async def test_returns_nothing_if_nothing_in_passed_segment(api, factory):
    # no order segment
    await factory.create_empty_client(segments=[SegmentTypeEnum.NO_ORDERS])
    # active segment
    await factory.create_empty_client(segments=[SegmentTypeEnum.ACTIVE])
    # lost segment
    await factory.create_empty_client(segments=[SegmentTypeEnum.LOST])
    # unprocessed segment
    await factory.create_empty_client(segments=[SegmentTypeEnum.UNPROCESSED_ORDERS])

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, segment=SegmentType.REGULAR),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    assert got == ClientContactsList(clients=[])


async def test_does_not_return_other_business_clients(api, factory):
    # no order segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentTypeEnum.NO_ORDERS])
    # regular & unprocessed segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentTypeEnum.REGULAR])
    # lost segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentTypeEnum.LOST])
    # active segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentTypeEnum.ACTIVE])

    got = await api.post(
        url,
        proto=ClientsListBySegmentInput(biz_id=123, segment=SegmentType.LOST),
        decode_as=ClientContactsList,
        expected_status=200,
    )

    assert got == ClientContactsList(clients=[])

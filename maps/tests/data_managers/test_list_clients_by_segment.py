import pytest

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


@pytest.mark.parametrize("segment", SegmentType)
async def test_returns_list_of_clients_in_passed_segment(segment, dm, factory):
    client_id_1 = await factory.create_empty_client(
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        cleared_for_gdpr=True,
        segments=[segment],
    )
    client_id_2 = await factory.create_empty_client(
        phone=78002000600,
        last_name="Лапенко",
        email="kek@cheburek.ru",
        cleared_for_gdpr=False,
        segments=[segment],
    )

    got = await dm.list_clients_by_segment(biz_id=123, segment=segment, label=None)

    assert got == [
        dict(
            id=client_id_1,
            biz_id=123,
            first_name="Вася",
            last_name="Иванов",
            passport_uid=354628,
            phone=None,
            email=None,
            cleared_for_gdpr=True,
        ),
        dict(
            id=client_id_2,
            biz_id=123,
            first_name=None,
            last_name="Лапенко",
            passport_uid=None,
            phone=78002000600,
            email="kek@cheburek.ru",
            cleared_for_gdpr=False,
        ),
    ]


async def test_returns_client_contact_details(dm, factory):
    client_id = await factory.create_empty_client(
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        phone=78002000600,
        email="kek@cheburek.ru",
        segments=[SegmentType.ACTIVE],
    )

    got = await dm.list_clients_by_segment(
        biz_id=123, segment=SegmentType.ACTIVE, label=None
    )

    client = got[0]
    assert client == dict(
        id=client_id,
        biz_id=123,
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        phone=78002000600,
        email="kek@cheburek.ru",
        cleared_for_gdpr=False,
    )


async def test_does_not_return_clients_which_are_not_in_segment(dm, factory):
    await factory.create_empty_client(segments=[SegmentType.NO_ORDERS])
    client_id = await factory.create_empty_client(segments=[SegmentType.REGULAR])

    got = await dm.list_clients_by_segment(
        biz_id=123, segment=SegmentType.REGULAR, label=None
    )

    assert len(got) == 1
    assert got[0]["id"] == client_id


async def test_returns_nothing_if_nothing_in_passed_segment(dm, factory):
    # no order segment
    await factory.create_empty_client(segments=[SegmentType.NO_ORDERS])
    # active segment
    await factory.create_empty_client(segments=[SegmentType.ACTIVE])
    # lost segment
    await factory.create_empty_client(segments=[SegmentType.LOST])
    # unprocessed segment
    await factory.create_empty_client(segments=[SegmentType.UNPROCESSED_ORDERS])

    got = await dm.list_clients_by_segment(
        biz_id=123, segment=SegmentType.REGULAR, label=None
    )

    assert got == []


async def test_does_not_return_other_business_clients(dm, factory):
    # no order segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentType.NO_ORDERS])
    # regular & unprocessed segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentType.REGULAR])
    # lost segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentType.LOST])
    # active segment
    await factory.create_empty_client(biz_id=888, segments=[SegmentType.ACTIVE])

    got = await dm.list_clients_by_segment(
        biz_id=123, segment=SegmentType.ACTIVE, label=None
    )

    assert got == []

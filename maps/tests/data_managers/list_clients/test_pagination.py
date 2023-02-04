import pytest
from freezegun import freeze_time

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_clients/"


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.parametrize("offset", range(1, 4))
async def test_returns_nothing_if_there_are_no_clients(limit, offset, dm):
    got = await dm.list_clients(biz_id=123, limit=limit, offset=offset)

    assert got == (0, [])


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit(limit, dm, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got_count, got = await dm.list_clients(biz_id=123, limit=limit, offset=0)

    assert extract_ids(got) == client_ids[:limit]
    assert got_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
async def test_respects_offset(offset, dm, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got_count, got = await dm.list_clients(biz_id=123, limit=2, offset=offset)

    assert extract_ids(got) == client_ids[offset : offset + 2]  # noqa
    assert got_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_with_search(limit, dm, factory):
    [await factory.create_empty_client() for _ in range(3)]
    client_ids = list(
        reversed([await factory.create_empty_client(comment="Иван") for _ in range(3)])
    )

    got_count, got = await dm.list_clients(
        biz_id=123, search_string="Иван", limit=limit, offset=0
    )

    assert extract_ids(got) == client_ids[:limit]
    assert got_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
async def test_respects_offset_with_search(offset, dm, factory):
    [await factory.create_empty_client() for _ in range(3)]
    client_ids = list(
        reversed([await factory.create_empty_client(comment="Иван") for _ in range(3)])
    )

    got_count, got = await dm.list_clients(
        biz_id=123, search_string="Иван", limit=2, offset=offset
    )

    assert extract_ids(got) == client_ids[offset : offset + 2]  # noqa
    assert got_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
async def test_respects_pagination_with_search(dm, db, factory, offset):
    # FTS
    id_1 = await factory.create_empty_client(comment="emaiN")
    id_2 = await factory.create_empty_client(comment="emaiN")
    # Priority column search (strict)
    id_3 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    id_4 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    # Priority column search (fuzzy)
    id_5 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_6 = await factory.create_empty_client(email="email_2@yandex.ru")
    full_expected_ids = [id_4, id_3, id_6, id_5, id_2, id_1]

    got_count, got = await dm.list_clients(
        biz_id=123, search_string="emaiN", limit=2, offset=offset
    )

    assert extract_ids(got) == full_expected_ids[offset : offset + 2]  # noqa
    assert got_count == 6


@freeze_time("2020-01-01 00:00:01", tick=True)
@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_with_segment_filter(limit, dm, factory):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(segments={SegmentType.ACTIVE})
                for _ in range(3)
            ]
        )
    )

    got_count, got = await dm.list_clients(
        biz_id=123, segment_type=SegmentType.ACTIVE, limit=limit, offset=0
    )

    assert extract_ids(got) == client_ids[:limit]
    assert got_count == 3


@freeze_time("2020-01-01 00:00:01", tick=True)
@pytest.mark.parametrize("offset", range(0, 3))
async def test_respects_offset_with_segment_filter(offset, dm, factory):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(segments={SegmentType.ACTIVE})
                for _ in range(3)
            ]
        )
    )

    got_count, got = await dm.list_clients(
        biz_id=123, segment_type=SegmentType.ACTIVE, limit=2, offset=offset
    )

    assert extract_ids(got) == client_ids[offset : offset + 2]  # noqa
    assert got_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_with_filter_by_ids(limit, dm, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got_count, got = await dm.list_clients(
        biz_id=123, client_ids=client_ids, limit=limit, offset=0
    )

    assert extract_ids(got) == client_ids[:limit]
    assert got_count == 3


@pytest.mark.parametrize("offset", range(0, 3))
async def test_respects_offset_with_filter_by_ids(offset, dm, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got_count, got = await dm.list_clients(
        biz_id=123, client_ids=client_ids, limit=2, offset=offset
    )

    assert extract_ids(got) == client_ids[offset : offset + 2]  # noqa
    assert got_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.parametrize("offset", range(1, 4))
@pytest.mark.parametrize("search_string", (None, "Иван"))
async def test_filters_by_biz_id(limit, offset, search_string, dm, factory):
    unexpected_id = await factory.create_empty_client(biz_id=999, comment="Иван")
    await factory.create_empty_client(comment="Иван")

    got_count, got = await dm.list_clients(
        biz_id=123, search_string=search_string, limit=limit, offset=offset
    )

    assert unexpected_id not in extract_ids(got)
    assert got_count == 1

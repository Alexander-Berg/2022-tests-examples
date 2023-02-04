from operator import attrgetter, itemgetter

import pytest

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


def extract_ids(results):
    return list(map(itemgetter("doorman_id"), results))


async def test_returns_all_clients(factory, dm):
    client1_id = await factory.create_client(biz_id=11)
    client2_id = await factory.create_client(biz_id=22, passport_uid=None)
    client3_id = await factory.create_client(passport_uid=123, phone=None)
    client4_id = await factory.create_client(passport_uid=124, email=None)
    client5_id = await factory.create_client(passport_uid=125, phone=None, email=None)

    results = []
    async for res in dm.iter_clients_for_export(2):
        results.extend(res)

    assert sorted(extract_ids(results)) == sorted(
        [client1_id, client2_id, client3_id, client4_id, client5_id]
    )


@pytest.mark.parametrize(
    ("chunk_size", "expected_results_sizes"),
    [(1, (1, 1, 1, 1, 1)), (2, (2, 2, 1)), (3, (3, 2)), (5, (5,)), (6, (5,))],
)
async def test_returns_data_in_chunks(factory, dm, chunk_size, expected_results_sizes):
    for _ in range(5):
        await factory.create_empty_client()

    results = []
    async for res in dm.iter_clients_for_export(chunk_size):
        results.append(res)

    results_sizes = tuple(map(len, results))
    assert results_sizes == expected_results_sizes


@pytest.mark.parametrize(
    "segments",
    [
        {SegmentType.LOST},
        {SegmentType.ACTIVE},
        {SegmentType.ACTIVE, SegmentType.UNPROCESSED_ORDERS},
    ],
)
async def test_returns_valid_segments(factory, dm, segments):
    await factory.create_empty_client(segments=segments)

    results = []
    async for res in dm.iter_clients_for_export(2):
        results.extend(res)

    assert len(results) == 1
    assert sorted(results[0]["segments"]) == sorted(map(attrgetter("name"), segments))


async def test_return_data(factory, dm):
    client1_id = await factory.create_client(
        biz_id=11,
        phone=322223,
        email="email1@yandex.ru",
        passport_uid=456,
        first_name="Василий",
        last_name="Пупкин",
        gender=ClientGender.MALE,
        labels=["mark-2021", "mark-2022"],
    )
    client2_id = await factory.create_client(
        biz_id=22,
        phone=222333,
        email="email2@yandex.ru",
        passport_uid=654,
        first_name="Василиса",
        last_name="Пупкина",
        gender=ClientGender.FEMALE,
        labels=[],
    )

    results = []
    async for res in dm.iter_clients_for_export(2):
        results.extend(res)

    assert results == [
        {
            "doorman_id": client1_id,
            "biz_id": 11,
            "phone": "322223",
            "email": "email1@yandex.ru",
            "passport_uid": 456,
            "first_name": "Василий",
            "last_name": "Пупкин",
            "gender": "MALE",
            "segments": ["NO_ORDERS"],
            "labels": ["mark-2021", "mark-2022"],
        },
        {
            "doorman_id": client2_id,
            "biz_id": 22,
            "phone": "222333",
            "email": "email2@yandex.ru",
            "passport_uid": 654,
            "first_name": "Василиса",
            "last_name": "Пупкина",
            "gender": "FEMALE",
            "segments": ["NO_ORDERS"],
            "labels": [],
        },
    ]


async def test_stops_immediately_if_not_data(factory, dm):
    results = []
    async for res in dm.iter_clients_for_export(2):
        results.extend(res)

    assert results == []

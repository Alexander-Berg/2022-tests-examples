from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.clients.bvm import BvmNotFound

pytestmark = [pytest.mark.asyncio]


URL = "/v1/business/"


async def test_creates_business(factory, api):
    await api.post(URL, json={"biz_id": 123})

    assert await factory.list_businesses() == [
        {
            "id": Any(int),
            "biz_id": 123,
            "permalink": 54321,
            "counter_id": 444,
            "created_at": Any(datetime),
        }
    ]


async def test_does_not_update_existing_business(factory, api):
    await factory.create_business(biz_id=123, permalink=99999, counter_id=999)

    await api.post(URL, json={"biz_id": 123})

    assert await factory.fetch_business(123) == {
        "id": Any(int),
        "biz_id": 123,
        "permalink": 99999,
        "counter_id": 999,
        "created_at": Any(datetime),
    }


async def test_does_not_affect_other_businesses(factory, api):
    await factory.create_business(biz_id=345, permalink=99999, counter_id=999)

    await api.post(URL, json={"biz_id": 123})

    assert await factory.list_businesses() == [
        {
            "id": Any(int),
            "biz_id": 345,
            "permalink": 99999,
            "counter_id": 999,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 123,
            "permalink": 54321,
            "counter_id": 444,
            "created_at": Any(datetime),
        },
    ]


async def test_returns_201_with_empty_body_on_success(factory, api):
    got = await api.post(
        URL,
        json={"biz_id": 123},
        expected_status=201,
    )

    assert got == b""


async def test_uses_clients(api, bvm, geosearch):
    await api.post(URL, json={"biz_id": 123})

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=123)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_returns_error_if_no_data_from_bvm(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.side_effect = BvmNotFound

    got = await api.post(URL, json={"biz_id": 123}, expected_status=400)

    assert got == {"error": "BIZ_NOT_FOUND"}


async def test_returns_error_if_not_data_from_geosearch(api, geosearch):
    geosearch.resolve_org.coro.return_value = None

    got = await api.post(URL, json={"biz_id": 123}, expected_status=400)

    assert got == {"error": "NO_ORG_INFO"}


async def test_returns_error_if_org_has_no_counter(api, geosearch):
    geosearch.resolve_org.coro.return_value.metrika_counter = None

    got = await api.post(URL, json={"biz_id": 123}, expected_status=400)

    assert got == {"error": "NO_COUNTER"}


async def test_returns_error_if_no_biz_id_provided(api):
    got = await api.post(URL, json={"something_else": 123}, expected_status=400)

    assert got == {"biz_id": ["Missing data for required field."]}

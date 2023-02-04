from copy import deepcopy

import pytest

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]

sensor_url = "/sensors/"


def is_sensor_exists(sensors: dict) -> bool:
    return any(
        [
            sensor["labels"]["metric_group"] == "empty_clients_search"
            for sensor in sensors["sensors"]
        ]
    )


def make_input(search_string: str = "ива"):
    return clients_pb2.ClientsListInput(
        biz_id=123,
        search_string=search_string,
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )


@pytest.fixture
def example_sensor():
    return deepcopy(_example_sensor)


async def test_sensor_is_empty_by_default(api):
    got = await api.get(sensor_url)

    assert not is_sensor_exists(got)


async def test_returns_sensor_details(api):
    await api.post("/v1/list_clients/", proto=make_input())
    got = await api.get(sensor_url)

    assert {
        "labels": {"metric_group": "empty_clients_search"},
        "type": "HIST_RATE",
        "hist": {
            "bounds": list(range(1, 51)),
            "buckets": [0, 0, 1] + [0] * 47,
            "inf": 0,
        },
    } in got["sensors"]


@pytest.mark.parametrize(
    "search_len, expected_bucket_index", ([3, 2], [10, 9], [50, 49])
)
async def test_uses_expected_bucket(
    api, example_sensor, search_len, expected_bucket_index
):
    await api.post("/v1/list_clients/", proto=make_input("x" * search_len))
    got = await api.get(sensor_url)

    example_sensor["hist"]["buckets"][expected_bucket_index] = 1
    assert example_sensor in got["sensors"]


async def test_uses_inf_for_too_long_search(api, example_sensor):
    await api.post("/v1/list_clients/", proto=make_input("x" * 51))
    got = await api.get(sensor_url)

    example_sensor["hist"]["inf"] = 1
    assert example_sensor in got["sensors"]


async def test_does_not_log_if_result_is_not_empty(api, factory):
    await factory.create_client(first_name="Иван")

    await api.post("/v1/list_clients/", proto=make_input("Иван"))
    got = await api.get(sensor_url)

    assert not is_sensor_exists(got)


async def test_does_not_log_if_no_search_string(api, factory):
    await factory.create_client(first_name="Иван")

    await api.post(
        "/v1/list_clients/",
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
    )
    got = await api.get(sensor_url)

    assert not is_sensor_exists(got)


async def test_pagination_does_not_affect_sensor_logging(api, factory):
    await factory.create_client(first_name="Иван")

    await api.post(
        "/v1/list_clients/",
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            search_string="ива",
            pagination=common_pb2.Pagination(limit=1, offset=100),
        ),
    )
    got = await api.get(sensor_url)

    assert not is_sensor_exists(got)


_example_sensor = {
    "labels": {"metric_group": "empty_clients_search"},
    "type": "HIST_RATE",
    "hist": {"bounds": list(range(1, 51)), "buckets": [0] * 50, "inf": 0},
}

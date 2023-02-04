import json

import pytest

from maps_adv.geosmb.landlord.proto import landing_config_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/get_landing_config/"


async def test_returns_data(api, factory):
    await factory.set_cached_landing_config({"key": "value"})

    got = await api.post(
        URL,
        proto=landing_config_pb2.LandingConfigInput(token="fetch_data_token"),
        decode_as=landing_config_pb2.LandingConfig,
        expected_status=200,
    )

    assert got == landing_config_pb2.LandingConfig(
        json_config=json.dumps({"key": "value"})
    )


async def test_returns_401_if_token_is_invalid(api, factory):
    await factory.set_cached_landing_config({"key": "value"})

    await api.post(
        URL,
        proto=landing_config_pb2.LandingConfigInput(token="BAD_TOKEN"),
        expected_status=401,
    )

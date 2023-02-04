import pytest
from aiohttp import web

from maps_adv.export.lib.core.client import OldGeoAdvClient
from maps_adv.common.third_party_clients.base.exceptions import UnknownResponse
from maps_adv.export.lib.core.client.old_geoadv import OrgPlace

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "response_data",
    (
        # full original
        {
            "orgs": [
                {
                    "address": "Mahmutbey Mah., Ordu Cad., İstanbul, Türkiye",
                    "chain_id": 122181296232,
                    "geo_id": 106120,
                    "latitude": 41.04977073,
                    "longitude": 28.8138199,
                    "name": "Occasion",
                    "permalink": 231208051791,
                    "providers": ["tr-pedestrians:737031517"],
                }
            ],
            "chains": [],
        },
        # minimum required
        {
            "orgs": [
                {
                    "address": "Mahmutbey Mah., Ordu Cad., İstanbul, Türkiye",
                    "latitude": 41.04977073,
                    "longitude": 28.8138199,
                    "name": "Occasion",
                    "permalink": 231208051791,
                }
            ]
        },
    ),
)
async def test_returns_orgs_info(mock_old_geoadv, config, response_data):
    mock_old_geoadv(web.json_response(data=response_data, status=200))

    async with OldGeoAdvClient.from_config(config) as client:
        got = await client([112233, 231208051791])

        assert got == {
            231208051791: OrgPlace(
                address="Mahmutbey Mah., Ordu Cad., İstanbul, Türkiye",
                latitude=41.04977073,
                longitude=28.8138199,
                title="Occasion",
                permalink=231208051791,
            )
        }


async def test_returns_nothing_if_no_orgs(mock_old_geoadv, config):
    mock_old_geoadv(web.json_response(data={"orgs": []}, status=200))

    async with OldGeoAdvClient.from_config(config) as client:
        got = await client([112233, 231208051791])

        assert got == {}


@pytest.mark.parametrize(
    "missed_field", ("address", "latitude", "longitude", "name", "permalink")
)
async def test_raises_if_corrupted_response(mock_old_geoadv, config, missed_field):
    response_data = {
        "orgs": [
            {
                "address": "Mahmutbey Mah., Ordu Cad., İstanbul, Türkiye",
                "chain_id": 122181296232,
                "geo_id": 106120,
                "latitude": 41.04977073,
                "longitude": 28.8138199,
                "name": "Occasion",
                "permalink": 231208051791,
                "providers": ["tr-pedestrians:737031517"],
            }
        ],
        "chains": [],
    }
    del response_data["orgs"][0][missed_field]

    mock_old_geoadv(web.json_response(data=response_data, status=200))

    async with OldGeoAdvClient.from_config(config) as client:
        with pytest.raises(KeyError) as exc_info:
            await client([231208051791])

    assert missed_field in exc_info.value.args


async def test_raises_for_unknown_response(mock_old_geoadv, config):
    mock_old_geoadv(web.json_response(data={"key": "value"}, status=500))

    async with OldGeoAdvClient.from_config(config) as client:
        with pytest.raises(UnknownResponse) as exc_info:
            await client([231208051791])

    assert (
        "Response with bad status. Status=500, "
        "host=example.com, payload={'key': 'value'}" in exc_info.value.args
    )

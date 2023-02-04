import pytest
from aiohttp.web import json_response

from maps_adv.geosmb.clients.geobase import GeoBaseClient

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def mock_fetch_linguistics_for_region(aresponses):
    return aresponses.add(
        "geobase.test",
        "/v1/linguistics_for_region",
        "GET",
        json_response(
            status=200,
            data={
                "ablative_case": "",
                "accusative_case": "Владивосток",
                "dative_case": "Владивостоку",
                "directional_case": "",
                "genitive_case": "Владивостока",
                "instrumental_case": "Владивостоком",
                "locative_case": "",
                "nominative_case": "Владивосток",
                "preposition": "во",
                "prepositional_case": "Владивостоке",
            },
        ),
    )


@pytest.fixture
async def client():
    async with GeoBaseClient(url="https://geobase.test") as _client:
        yield _client

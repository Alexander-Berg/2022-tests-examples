import pytest
from aiohttp.web import json_response

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, aresponses):
    request_url = None

    async def _handler(request):
        nonlocal request_url
        request_url = str(request.url)
        return json_response(
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
        )

    aresponses.add("geobase.test", "/v1/linguistics_for_region", "GET", _handler)

    await client.fetch_linguistics_for_region(12345)

    assert (
        request_url == "http://geobase.test/v1/linguistics_for_region?id=12345&lang=ru"
    )


async def test_returns_none_on_empty_reply(client, mock_fetch_linguistics_for_region):
    result = await client.fetch_linguistics_for_region(12345)

    assert result == {
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
    }

import json
import pytest
from mock import patch

from intranet.trip.src.lib.aeroclub import enums
from intranet.trip.src.logic.aeroclub.reference import get_reference_list


pytestmark = pytest.mark.asyncio

MOSCOW_SUGGEST = [
    {
        "id": 16381,
        "code": "MOW",
        "name": {
            "ru": "Москва",
            "en": "Moscow"
        },
        "type": "City",
    },
]


@pytest.mark.parametrize('is_suggest_cached', (True, False))
async def test_get_reference_list_city(cache, is_suggest_cached):
    cache.CACHE = {}
    if is_suggest_cached:
        cache.CACHE['city:моск'] = json.dumps(MOSCOW_SUGGEST)

    with patch('intranet.trip.src.lib.aeroclub.api.Aeroclub.get_references',
               return_value=MOSCOW_SUGGEST) as get_reference_mock:
        result = await get_reference_list('моск', enums.ReferenceType.city, cache)
    assert result == MOSCOW_SUGGEST
    assert get_reference_mock.called == (not is_suggest_cached)

from unittest import mock

import furl
import pytest
from async_asgi_testclient import TestClient
from fastapi import status

from app.core.config import settings
from app.schemas.location import LocationSchema, LocationSuggestSchema
from tests.helpers import random_data as rnd
from tests.helpers.locations import random_loc

_prefix = furl.furl(settings.API_V1_STR) / "loc"

pytestmark = pytest.mark.asyncio

_dao = "app.api.helpers.locations.dao"


@mock.patch(_dao)
def test_get(dao, loc, async_client: TestClient):
    dao.get = mock.AsyncMock(return_value=loc.dict())
    route = _prefix / f"{loc.id}"
    response = await async_client.get(route.url)
    assert status.HTTP_200_OK == response.status_code
    j = response.json()
    # we need to cast dat shit to model, so floats can be compared
    assert loc == LocationSchema(**j)


@mock.patch(_dao)
def test_get_negative(dao, async_client: TestClient):
    dao.get = mock.AsyncMock(return_value=None)
    route = _prefix / f"{rnd.random_integer()}"
    response = await async_client.get(route.url)
    assert status.HTTP_404_NOT_FOUND == response.status_code


@mock.patch(_dao)
def test_suggest(dao, async_client: TestClient):
    expected = [*map(LocationSuggestSchema.dict, (random_loc() for _ in range(16)))]
    dao.suggest = mock.AsyncMock(return_value=expected)
    route = _prefix / "suggest"
    response = await async_client.get(route.url, query_string={"q": rnd.random_lower_string()})
    assert status.HTTP_200_OK == response.status_code
    j = response.json()
    assert dao.has_calls

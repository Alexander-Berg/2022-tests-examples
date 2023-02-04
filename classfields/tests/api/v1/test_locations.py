import furl
import pytest
from fastapi import status

from app.core.config import settings
from app.db import models
from app.schemas.location import LocationSuggestSchema


class TestClass:

    prefix = furl.furl(settings.API_V1_STR) / "loc"

    @pytest.mark.asyncio
    async def test_suggest(self, async_client):
        route = self.prefix / "suggest"

        response = await async_client.get(route.url)
        assert status.HTTP_422_UNPROCESSABLE_ENTITY == response.status_code

        await models.Location.create(
            parent_id=1, country=1, name="test", en_name="test", synonyms="[{}]", type=1, lat=2, lon=3
        )
        await models.Location.create(
            parent_id=1, country=1, name="x_test", en_name="x_test", synonyms="[{}]", type=1, lat=2, lon=3
        )

        response = await async_client.get(route.url, query_string={"q": "x"})
        resp = response.json()
        assert status.HTTP_200_OK == response.status_code
        assert len(resp) == 1
        LocationSuggestSchema(**resp[0])
        assert resp[0]["name"] == "x_test"

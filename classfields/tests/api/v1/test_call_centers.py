import furl
import pytest
from async_asgi_testclient import TestClient
from starlette import status
from tortoise.contrib.test import TestCase

from app.core.config import settings
from app.schemas.call_center import CallCenterInCreateSchema, CallCenterInUpdateSchema
from tests.helpers.random_data import random_lower_string

pytestmark = pytest.mark.asyncio

_prefix = furl.furl(settings.API_V1_STR) / "call-centers/"


@pytest.mark.usefixtures("create_superuser", "async_client_class", "superuser_token_headers_class")
class TestCallCenters(TestCase):
    async_client: TestClient

    async def test_read_all_empty(self):
        expected = []
        res = await self.async_client.get(_prefix.url, headers=self.superuser_token_headers)
        assert status.HTTP_200_OK == res.status_code
        assert expected == res.json()

    async def test_read_all_some_and_batch_delete(self):
        call_center_in = CallCenterInCreateSchema(
            name=random_lower_string(), bunker_name=random_lower_string(), region_id=1
        )
        call_center_in2 = CallCenterInCreateSchema(
            name=random_lower_string(), bunker_name=random_lower_string(), region_id=1
        )
        await self.async_client.post(
            _prefix.url, headers=self.superuser_token_headers, json=call_center_in.dict(exclude_unset=True)
        )
        await self.async_client.post(
            _prefix.url, headers=self.superuser_token_headers, json=call_center_in2.dict(exclude_unset=True)
        )

        call_centers = await self.async_client.get(_prefix.url, headers=self.superuser_token_headers)

        assert status.HTTP_200_OK == call_centers.status_code
        ids = []
        for cc in call_centers.json():
            ids.append(cc["id"])
        assert len(call_centers.json()) == 2
        path = _prefix / "batch"
        path.args = {"ids": ids}
        await self.async_client.delete(path.url, headers=self.superuser_token_headers)

    async def test_delete(self):
        call_center_in = CallCenterInCreateSchema(
            name=random_lower_string(), bunker_name=random_lower_string(), region_id=1
        )
        await self.async_client.post(
            _prefix.url, headers=self.superuser_token_headers, json=call_center_in.dict(exclude_unset=True)
        )
        call_center = await self.async_client.get(_prefix.url, headers=self.superuser_token_headers)
        url = _prefix / f"{call_center.json()[0]['id']}"
        response = await self.async_client.delete(url.url, headers=self.superuser_token_headers)
        assert response.status_code == status.HTTP_204_NO_CONTENT

    async def test_update(self):
        call_center_in = CallCenterInCreateSchema(
            name=random_lower_string(), bunker_name=random_lower_string(), region_id=1
        )
        call_center_in2 = CallCenterInUpdateSchema(name=random_lower_string())
        call_center = await self.async_client.post(
            _prefix.url, headers=self.superuser_token_headers, data=call_center_in.json()
        )
        path = _prefix / f"{call_center.json()['id']}"
        resp = await self.async_client.patch(
            path.url, json=call_center_in2.dict(exclude_unset=True), headers=self.superuser_token_headers
        )
        resp_json = resp.json()
        assert resp_json["name"] == call_center_in2.name
        assert resp_json["region_id"] == call_center_in.region_id
        await self.async_client.delete(path.url, headers=self.superuser_token_headers)

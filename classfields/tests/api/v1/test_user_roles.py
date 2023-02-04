import pytest
from async_asgi_testclient import TestClient
from tortoise.contrib.test import TestCase

from app.core.config import settings
from app.schemas.user_role import UserRoleInCreateSchema
from tests.helpers.random_data import random_lower_string


@pytest.mark.usefixtures("create_superuser", "async_client_class", "superuser_token_headers_class")
class TestApi(TestCase):
    async_client: TestClient

    @property
    def good_data(self):
        return UserRoleInCreateSchema(name=random_lower_string(16), code=random_lower_string(16))

    async def create_user_role(self, user_role_in: UserRoleInCreateSchema):
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/user-roles/",
            headers=self.superuser_token_headers,
            json=user_role_in.dict(exclude_unset=True),
        )
        return r

    async def test_create_user_role_good(self):
        good_data = self.good_data
        r = await self.create_user_role(good_data)
        self.assertEqual(200, r.status_code)
        created_user_role = r.json()
        self.assertEqual(created_user_role["name"], good_data.name)
        self.assertEqual(created_user_role["code"], good_data.code)

    async def test_create_user_role_existing(self):
        good_data = self.good_data
        r = await self.create_user_role(good_data)
        self.assertEqual(200, r.status_code)
        r = await self.create_user_role(good_data)
        self.assertEqual(400, r.status_code)

    async def test_update_user_role_good(self):
        good_data = self.good_data
        r = await self.create_user_role(good_data)
        user_role_id = r.json()["id"]
        new_name = random_lower_string()
        data = {"name": new_name}
        r = await self.async_client.patch(
            f"{settings.API_V1_STR}/user-roles/{user_role_id}",
            headers=self.superuser_token_headers,
            json=data,
        )
        self.assertEqual(200, r.status_code)
        self.assertEqual(r.json()["name"], new_name)
        self.assertEqual(r.json()["code"], good_data.code)

    async def test_update_user_role_empty(self):
        good_data = self.good_data
        r = await self.create_user_role(good_data)
        user_role_id = r.json()["id"]
        data = {}
        r = await self.async_client.patch(
            f"{settings.API_V1_STR}/user-roles/{user_role_id}",
            headers=self.superuser_token_headers,
            json=data,
        )
        self.assertEqual(200, r.status_code)
        self.assertEqual(r.json()["name"], good_data.name)

    async def test_user_role_delete(self):
        good_data = self.good_data
        r = await self.create_user_role(good_data)
        user_role_id = r.json()["id"]
        r = await self.async_client.delete(
            f"{settings.API_V1_STR}/user-roles/{user_role_id}",
            headers=self.superuser_token_headers,
        )
        self.assertEqual(204, r.status_code)
        r = await self.create_user_role(good_data)
        user_role_id = r.json()["id"]
        r = await self.async_client.delete(
            f"{settings.API_V1_STR}/user-roles/delete/", headers=self.superuser_token_headers, json=[user_role_id]
        )
        self.assertEqual(204, r.status_code)

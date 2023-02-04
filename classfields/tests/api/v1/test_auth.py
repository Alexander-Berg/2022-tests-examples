import pytest
from async_asgi_testclient import TestClient
from fastapi import status
from tortoise.contrib.test import TestCase

from app.core.config import settings
from app.schemas.user_role import UserRoleInCreateSchema, UserRoleSchema
from tests.helpers.random_data import random_lower_string


@pytest.mark.usefixtures("create_superuser", "async_client_class", "superuser_token_headers_class")
class TestAuth(TestCase):
    async_client: TestClient

    async def user_role(self) -> UserRoleSchema:
        user_role_in = UserRoleInCreateSchema(name=random_lower_string(), code=random_lower_string())
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/user-roles/",
            headers=self.superuser_token_headers,
            json=user_role_in.dict(exclude_unset=True),
        )
        return UserRoleSchema(**r.json())

    async def test_login(self):
        login_data = {"username": settings.FIRST_SUPERUSER_LOGIN, "password": settings.FIRST_SUPERUSER_PASSWORD}
        r = await self.async_client.post(f"{settings.API_V1_STR}/auth/login", form=login_data)
        tokens = r.json()
        assert r.status_code == status.HTTP_200_OK
        assert "access_token" in tokens
        assert tokens["access_token"]

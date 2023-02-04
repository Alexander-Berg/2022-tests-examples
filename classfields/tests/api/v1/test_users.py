from typing import Tuple

import pytest
from async_asgi_testclient import TestClient
from fastapi import status
from tortoise.contrib.test import TestCase

from app.core.config import settings
from app.schemas.user import UserInCreateSchema, UserInviteInSchema
from app.schemas.user_role import UserRoleInCreateSchema, UserRoleSchema
from tests.helpers.random_data import random_lower_string
from tests.helpers.user import user_authentication_headers


@pytest.mark.usefixtures("create_superuser", "async_client_class", "superuser_token_headers_class")
class TestUsers(TestCase):
    async_client: TestClient

    async def create_role(self) -> UserRoleSchema:
        user_role_in = UserRoleInCreateSchema(name=random_lower_string(), code=random_lower_string())
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/user-roles/",
            headers=self.superuser_token_headers,
            json=user_role_in.dict(exclude_unset=True),
        )
        return UserRoleSchema(**r.json())

    async def create_user(self, user_in: UserInCreateSchema):
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/users/", headers=self.superuser_token_headers, json=user_in.dict(exclude_unset=True)
        )
        return r

    async def invite_user(self, user_invite_in: UserInviteInSchema):
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/users/invite",
            headers=self.superuser_token_headers,
            json=user_invite_in.dict(exclude_unset=True),
        )
        return r

    async def generate_user_data(self) -> Tuple[str, str, str, str, UserRoleSchema]:
        login = f"{random_lower_string()}"
        password = random_lower_string()
        first_name = random_lower_string()
        last_name = random_lower_string()
        user_role = await self.create_role()
        return login, password, first_name, last_name, user_role

    async def test_create_user_new_login(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        r = await self.create_user(user_in)
        self.assertEqual(r.status_code, status.HTTP_200_OK)
        created_user = r.json()
        self.assertEqual(created_user["login"], login)
        self.assertNotIn("password", created_user)
        self.assertEqual(created_user["first_name"], first_name)
        self.assertEqual(created_user["last_name"], last_name)
        self.assertTrue(created_user["is_active"])
        self.assertFalse(created_user["is_superuser"])

    async def test_get_users_superuser_me(self):
        r = await self.async_client.get(f"{settings.API_V1_STR}/users/me", headers=self.superuser_token_headers)
        current_user = r.json()
        self.assertFalse(not current_user)
        self.assertTrue(current_user["is_active"])
        self.assertTrue(current_user["is_superuser"])
        self.assertEqual(current_user["login"], settings.FIRST_SUPERUSER_LOGIN)

    async def test_get_existing_user(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        r = await self.create_user(user_in)
        user_json = r.json()
        user_id = user_json["id"]
        r = await self.async_client.get(f"{settings.API_V1_STR}/users/{user_id}", headers=self.superuser_token_headers)
        self.assertEqual(r.status_code, status.HTTP_200_OK)

    async def test_create_user_existing_login(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        await self.create_user(user_in)
        data = {"login": login, "password": password, "role_id": user_role.id}
        r = await self.async_client.post(
            f"{settings.API_V1_STR}/users/", headers=self.superuser_token_headers, json=data
        )
        created_user = r.json()
        self.assertEqual(r.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertNotIn("_id", created_user)

    async def test_create_user_by_normal_user(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        await self.create_user(user_in)
        user_token_headers = await user_authentication_headers(self.async_client, login, password)
        data = {"password": password}
        r = await self.async_client.post(f"{settings.API_V1_STR}/users/", headers=user_token_headers, json=data)
        self.assertEqual(r.status_code, status.HTTP_403_FORBIDDEN)

    async def test_retrieve_users(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        await self.create_user(user_in)

        login2, password2, first_name2, last_name2, user_role_2 = await self.generate_user_data()
        user_in2 = UserInCreateSchema(
            login=login2, password=password2, first_name=first_name2, last_name=last_name2, role_id=user_role_2.id
        )
        await self.create_user(user_in2)

        r = await self.async_client.get(f"{settings.API_V1_STR}/users/", headers=self.superuser_token_headers)
        items, total = r.json()["items"], r.json()["total"]

        self.assertGreater(len(items), 1)
        self.assertGreater(total, 1)
        self.assertGreaterEqual(total, len(items))
        for user in items:
            self.assertIn("login", user)

    async def test_update_user(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        r = await self.create_user(user_in)
        user_json = r.json()
        user_id = user_json["id"]
        new_first_name = random_lower_string()
        new_last_name = random_lower_string()
        new_data = {"first_name": new_first_name, "last_name": new_last_name}
        r = await self.async_client.patch(
            f"{settings.API_V1_STR}/users/{user_id}", headers=self.superuser_token_headers, json=new_data
        )
        self.assertEqual(r.status_code, status.HTTP_200_OK)
        self.assertEqual(r.json()["first_name"], new_first_name)
        self.assertEqual(r.json()["last_name"], new_last_name)

    async def test_user_delete(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_in = UserInCreateSchema(
            login=login, password=password, first_name=first_name, last_name=last_name, role_id=user_role.id
        )
        r = await self.create_user(user_in)
        user_json = r.json()
        user_id = user_json["id"]
        r = await self.async_client.delete(
            f"{settings.API_V1_STR}/users/{user_id}", headers=self.superuser_token_headers
        )
        self.assertEqual(status.HTTP_204_NO_CONTENT, r.status_code)
        r = await self.create_user(user_in)
        user_json = r.json()
        user_id = user_json["id"]
        r = await self.async_client.delete(
            f"{settings.API_V1_STR}/users/delete/", headers=self.superuser_token_headers, json=[user_id]
        )
        self.assertEqual(status.HTTP_204_NO_CONTENT, r.status_code)

    async def test_invite_user(self):
        login, password, first_name, last_name, user_role = await self.generate_user_data()
        user_invite_in = UserInviteInSchema(login=login, call_center_id=None, role_id=user_role.id)
        r = await self.invite_user(user_invite_in=user_invite_in)
        self.assertEqual(r.status_code, status.HTTP_200_OK)
        user_invite_json = r.json()
        self.assertTrue("code" in user_invite_json)

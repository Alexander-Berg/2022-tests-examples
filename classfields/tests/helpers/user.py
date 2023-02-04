from async_asgi_testclient import TestClient

from app.core.config import settings


async def get_superuser_token_headers(async_client: TestClient):
    login_data = {
        "username": settings.FIRST_SUPERUSER_LOGIN,
        "password": settings.FIRST_SUPERUSER_PASSWORD,
    }

    r = await async_client.post(f"{settings.API_V1_STR}/auth/login", form=login_data)
    tokens = r.json()
    a_token = tokens["access_token"]
    headers = {"Authorization": f"Bearer {a_token}"}
    return headers


async def user_authentication_headers(async_client: TestClient, login, password):
    data = {"username": login, "password": password}

    r = await async_client.post(f"{settings.API_V1_STR}/auth/login", form=data)
    response = r.json()
    auth_token = response["access_token"]
    headers = {"Authorization": f"Bearer {auth_token}"}
    return headers

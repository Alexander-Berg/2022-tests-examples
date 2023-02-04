import logging
import pytest
from yandex_io.pylibs.functional_tests.http_client import retrying_client

logger = logging.getLogger(__name__)


PASSPORT_HOST = "passport.yandex.ru"


class PassportClient:
    def __init__(self, passport_host=PASSPORT_HOST):
        self.passport_host = passport_host
        self.http_client = retrying_client()

    def getSessionid(self, user):
        """
        Login and extract Session_id cookie from passport's response
        """
        payload = {"passwd": user["account"]["password"], "login": user["account"]["login"]}
        resp = self.http_client.post(f"https://{self.passport_host}/auth", data=payload, allow_redirects=False)
        session_id = resp.cookies.get("Session_id")
        if session_id is None:
            logger.error(f"Passport's response cookies: {resp.cookies.get_dict()}; body: {resp.text}")
            pytest.fail("No Session_id cookie in passport's response")

        return resp.cookies["Session_id"]

    def getAccountManagerCode(self, sessionid, client_id, client_secret):
        """
        Get verification code in exchange for sessionid, client_id and client_secret
        See https://wiki.yandex-team.ru/passport/api/bundle/auth/oauth/#obmensessii/x-tokenanakodpodtverzhdenija
        """
        response = self.http_client.post(
            "https://mobileproxy.passport.yandex.net/1/bundle/auth/oauth/code_for_am/",
            headers={
                "Ya-Client-Host": "yandex.ru",
                "Ya-Client-Cookie": f"Session_id={sessionid}",
            },
            data={
                "client_id": client_id,
                "client_secret": client_secret,
            },
        )
        logger.debug(f"Response from account manager: {response.text}")
        if "code" not in response.json():
            raise RuntimeError("Failed to get auth code. Backend response: " << response.text)
        return response.json()["code"]

    def getXToken(self, auth_code, client_id, client_secret):
        """
        Get x-token in exchange for auth_code, client_id and client_secret
        See https://wiki.yandex-team.ru/passport/mobileproxy/api/#/1/token
        """
        response = self.http_client.post(
            "https://mobileproxy.passport.yandex.net/1/token",
            data={
                "client_id": client_id,
                "client_secret": client_secret,
                "grant_type": "authorization_code",
                "code": auth_code,
            },
        )
        logger.debug(f"Response from account manager: {response.text}")
        return response.json().get("access_token")

    def getOAuthToken(self, x_token, client_id, client_secret):
        """
        Get oauth token in exchange for x_token, client_id and client_secret
        See https://wiki.yandex-team.ru/passport/mobileproxy/api/#/1/token
        """
        response = self.http_client.post(
            "https://mobileproxy.passport.yandex.net/1/token",
            data={
                "client_id": client_id,
                "client_secret": client_secret,
                "grant_type": "x-token",
                "access_token": x_token,
            },
        )
        logger.debug(f"Response from account manager: {response.text}")
        return response.json().get("access_token")

import logging
import os

from yandex_io.pylibs.functional_tests.http_client import retrying_client

from library.python import oauth


"""
Module wrapping a fraction of api for https://wiki.yandex-team.ru/tus/ providing the ability to create temporary users.
"""


logger = logging.getLogger(__name__)


TUS_HOST = "tus.yandex-team.ru"
OAUTH_HOST = "oauth.yandex.ru"
TUS_CONSUMER = "yandex_io"
PASSPORT_ENVIRONMENT = "prod"


class TUSClient:
    def __init__(self, tus_host=TUS_HOST, tus_consumer=TUS_CONSUMER, oauth_host=OAUTH_HOST):
        self.tus_host = tus_host
        self.tus_consumer = tus_consumer
        self.oauth_host = oauth_host
        self.http_client = retrying_client()

    def getOAuthTokenForAPI(self):
        token = os.environ.get("TUS_TOKEN", None)
        if token is None:
            # Get OAUth token from available SSH key
            # https://oauth.yandex-team.ru/client/6c9c5b0d4efe492593c5086b94cb8c10
            token = oauth.get_token(
                client_id="6c9c5b0d4efe492593c5086b94cb8c10",
                client_secret="275305d9642b48f7a1b48d91c64c2172",
                raise_errors=True,
            )

        return token

    def getTUSHeaders(self):
        """
        Common headers used for TUS requests
        """
        return {"Authorization": f"OAuth {self.getOAuthTokenForAPI()}"}

    def createUser(self, tags=None, login=None):
        """
        Create user in production passport's environment
        """
        payload = {"env": PASSPORT_ENVIRONMENT, "tus_consumer": self.tus_consumer, "tags": tags}
        if login is not None:
            payload["login"] = login

        resp = self.http_client.post(
            f"https://{self.tus_host}/1/create_account/portal/", data=payload, headers=self.getTUSHeaders()
        )

        resp.raise_for_status()

        return resp.json()

    def getUser(self, lock_duration, tags=None):
        """
        Get existing user with specified tags from tus, locking it for a specified amount of seconds
        """
        params = {
            "env": PASSPORT_ENVIRONMENT,
            "tus_consumer": self.tus_consumer,
            "lock_duration": lock_duration,
            "tags": tags,
            "with_saved_tags": "true",
        }
        resp = self.http_client.get(
            f"https://{self.tus_host}/1/get_account/", params=params, headers=self.getTUSHeaders()
        )

        try:
            resp.raise_for_status()
        except Exception as e:
            logger.error(f"Error trying to get user: {e}")
            return None

        if "error" in resp:
            logger.error(f"Error trying to get user: {resp['error_description']}")
            return None
        return resp.json()

    def unlockUser(self, user):
        """
        Unlocks user account
        """
        params = {"uid": user["account"]["uid"], "env": user["passport_environment"], "tus_consumer": self.tus_consumer}
        resp = self.http_client.post(
            f"https://{self.tus_host}/1/unlock_account/", params=params, headers=self.getTUSHeaders()
        )
        resp.raise_for_status()
        return resp.json()

import logging

from yandex_io.pylibs.functional_tests.http_client import retrying_client

logger = logging.getLogger(__name__)


MUSIC_QA_API_HOST = "api.music.qa.yandex.net"


class MusicApiClient:
    def __init__(self, music_api_host=MUSIC_QA_API_HOST):
        self.music_api_host = music_api_host
        self.http_client = retrying_client()

    def debugResetAccountMusicInfo(self, auth_code):
        """
        Remove all data associated with current user from music backend
        """
        logger.debug("Calling debug/reset-account-music-info")
        response = self.http_client.post(
            f"https://{self.music_api_host}/debug/reset-account-music-info",
            headers={
                "Authorization": f"OAuth {auth_code}",
            },
        )
        logger.debug(f"Response from music api: {response.text}")

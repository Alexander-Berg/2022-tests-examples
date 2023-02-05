import datetime as dt
import logging
import requests
import typing as tp
from time import sleep

from requests_toolbelt.sessions import BaseUrlSession

from maps.garden.libs.auth import auth_client

from maps.garden.sandbox.server_autotests.lib import constants as const
from maps.garden.sandbox.server_autotests.lib.build import BuildInfo


logger = logging.getLogger("server_tester")


class ServerTester:
    def __init__(
        self,
        server_hostname: str,
        contour_name: str,
        retry_interval_sec: int = 5,
    ):
        self.contour_name = contour_name
        self.retry_interval_sec = retry_interval_sec
        self.session = BaseUrlSession(base_url=f"http://{server_hostname}")
        self.session.headers.update(auth_client.OAuth().get_garden_server_auth_headers())
        self.start_time = dt.datetime.now(dt.timezone.utc).isoformat()

    def get(self, handler: str, params: tp.Dict[str, tp.Any] = {}) -> requests.Response:
        logger.info(f"Sending GET request to {self.session.base_url}{handler}.")
        response = self.session.get(handler, params=params)
        logger.info(f"{response.request.url} send `{response.status_code}` response code.")

        response.raise_for_status()

        return response

    def post(
        self, handler: str,
        json: tp.Dict[str, tp.Any] = None,
        params: tp.Dict[str, tp.Any] = None,
    ) -> requests.Response:
        logger.info(f"Sending POST request to {self.session.base_url}{handler}.")
        response = self.session.post(handler, json=json, params=params)
        logger.info(f"{response.request.url} send `{response.status_code}` response code.")

        response.raise_for_status()

        return response

    def delete(self, handler: str):
        logger.info(f"Sending DELETE request to {self.session.base_url}{handler}.")
        response = self.session.delete(handler)
        logger.info(f"{response.request.url} send `{response.status_code}` response code.")

        response.raise_for_status()

        return response

    def wait_until(
        self,
        check_function: tp.Callable,
        testing_object: tp.Union[BuildInfo, str],
        timeout: int = const.ONE_MINUTE,
        ignore_exception: bool = False,
    ):
        if type(testing_object) == str:
            error_object = f"{testing_object}"
        else:
            error_object = f"Build {testing_object.description}"
        elapsed_time = 0
        while True:
            try:
                if check_function(self, testing_object):
                    return
            except Exception:
                logger.exception("Exception in function {check_function.__name__}")
                if not ignore_exception:
                    raise

            assert elapsed_time < timeout, f"{error_object} failed function {check_function.__name__}"
            logger.info(f"Checking again in {self.retry_interval_sec} seconds...")
            sleep(self.retry_interval_sec)
            elapsed_time += self.retry_interval_sec

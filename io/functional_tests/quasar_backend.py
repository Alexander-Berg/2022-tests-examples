import logging
from yandex_io.pylibs.functional_tests.http_client import retrying_client


logger = logging.getLogger(__name__)


class QuasarBackendClient:
    def __init__(self, backend_url, auth_cookies, device_id, platform):
        self.backend_url = backend_url
        self.auth_cookies = auth_cookies
        self.device_id = device_id
        self.http_client = retrying_client()
        self.device_params = {"device_id": self.device_id, "platform": platform}
        logger.info(f"Using backend url: {self.backend_url}")

    def _getConfig(self, config_type, params=None):
        response = self.http_client.get(
            f"{self.backend_url}/get_{config_type}_config", cookies=self.auth_cookies, params=params
        )
        logger.debug(f"Request headers for get_{config_type}_config: {response.request.headers}")
        logger.debug(f"Response for get_{config_type}_config: {response.text}")
        return response.json().get("config")

    def _setConfig(self, config_type, config, params=None):
        csrf_token = self.getCSRFToken()
        resp = self.http_client.post(
            f"{self.backend_url}/set_{config_type}_config",
            json=config,
            cookies=self.auth_cookies,
            headers={'X-CSRF-Token': csrf_token},
            params=params,
        )
        logger.debug(f"set_{config_type}_config response: {resp.text}")
        return resp.json()

    def getAccountConfig(self):
        return self._getConfig("account")

    def setAccountConfig(self, config):
        return self._setConfig("account", config)

    def getDeviceConfig(self):
        return self._getConfig("device", self.device_params)

    def setDeviceConfig(self, config):
        return self._setConfig("device", config, self.device_params)

    def getCSRFToken(self):
        response = self.http_client.get(f"{self.backend_url}/csrf_token", cookies=self.auth_cookies)
        logger.debug(f"CSRF token response: {response.text}")
        return response.json()["token"]

    def dropUser(self):
        csrf_token = self.getCSRFToken()
        headers = {'X-CSRF-Token': csrf_token}
        if self.device_id is not None:
            headers["X-YaQuasarDeviceId"] = self.device_id
        response = self.http_client.post(
            f"{self.backend_url}/drop_current_user", cookies=self.auth_cookies, headers=headers
        )
        logger.debug("Request headers for drop_current_user: {}".format(response.request.headers))
        logger.debug("Response for drop_current_user: {}".format(response.text))
        return response.json()

    @staticmethod
    def glagolHeaders(authToken):
        return {"Authorization": "OAuth " + authToken, "Content-Type": "application/json"}

    def getGlagolDevicesList(self, authToken):
        response = self.http_client.get(f"{self.backend_url}/glagol/device_list", headers=self.glagolHeaders(authToken))
        logger.debug("Response status = {}, body = {}".format(response.status_code, response.json()))
        return response.json().get('devices')

    def getGlagolToken(self, authToken, deviceId, platform):
        params = {"device_id": deviceId, "platform": platform}
        response = self.http_client.get(
            f"{self.backend_url}/glagol/token", params=params, headers=self.glagolHeaders(authToken)
        )
        logger.debug("Response status = {}".format(response.status_code))
        return response.json().get('token')

    def unregister(self):
        csrf_token = self.getCSRFToken()
        response = self.http_client.post(
            f"{self.backend_url}/unregister",
            params=self.device_params,
            cookies=self.auth_cookies,
            headers={"X-CSRF-Token": csrf_token},
        )
        logger.info("Response status = {}, body = {}".format(response.status_code, response.text))

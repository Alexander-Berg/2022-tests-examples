import logging
import requests

logger = logging.getLogger(__name__)


class QuasarFirstrundBackendClient:
    def __init__(self, backend_url):
        self.backend_url = backend_url

    def connect(self, payload):
        response = requests.post(f"{self.backend_url}/connect", data=payload)
        logger.debug("Response for /connect: {}".format(response.text))
        logger.debug("Request headers for /connect: {}".format(response.headers))
        return response

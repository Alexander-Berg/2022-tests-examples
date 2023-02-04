import requests
import time
from sys import stderr
import logging
import os
from datetime import datetime
from retry import retry

_1_SEC = 1

# APIKEY for user robot-b2bgeo-yaru@yandex.ru
DEFAULT_APIKEY = os.environ.get("B2BGEO_APIKEY", "1b37b3ee-e975-444f-9e49-5323a63841b1")
# APIKEY for user b2b-geo@yandex.ru
BANNED_APIKEY = os.environ.get("B2BGEO_BANNED_APIKEY", "2780b593-ea88-417d-9529-af088028cfb6")

session = requests.Session()
requests.packages.urllib3.disable_warnings(
    requests.packages.urllib3.exceptions.InsecureRequestWarning)


class RequestClient:
    def __init__(self, max_rps):
        self.max_rps = max_rps
        self._requests_this_second = 0
        self._request_chain_start = time.time()

    def request(self, url, headers=None):
        if self._requests_this_second < self.max_rps:
            self._requests_this_second += 1
        else:
            time_to_wait = self._request_chain_start + _1_SEC - time.time()
            if time_to_wait > 0:
                print(f"Sleep to keep max rps for {time_to_wait}", file=stderr)
                time.sleep(time_to_wait)
            self._requests_this_second = 1
            self._request_chain_start = time.time()
        return self._request(url, headers)

    @retry(tries=3, delay=1, backoff=1.2, logger=logging)
    def _request(self, url, headers):
        print(f"{datetime.strftime(datetime.now(), '%Y-%m-%d %H:%M:%S')} Request {url}", file=stderr)
        resp = session.get(url, verify=False, headers=headers)
        if resp.status_code >= 500:
            raise Exception(
                "Query {} finished with server error {}".format(
                    resp.url,
                    resp.status_code))
        return resp

import requests
import os
import time
import signal
from maps.b2bgeo.ya_courier.backend.test_lib.util import env_get_request, no_ssl_verification
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def request(**kwargs):
    with no_ssl_verification():
        return requests.request(**kwargs)


@skip_if_remote
def test_ping(system_env_with_db_and_process):
    (env, pid,) = system_env_with_db_and_process
    response = env_get_request(env, path='ping')
    assert response.status_code == requests.codes.ok
    assert pid is not None

    os.kill(pid, signal.SIGUSR1)
    time.sleep(0.5)

    for i in range(10):
        response = request(method='get', url="{}/api/v1/{}".format(env.url, 'ping'))
        assert response.status_code == requests.codes.service_unavailable
        response = request(method='get', url="{}/api/v1/{}".format(env.url, 'unistat'))
        assert response.status_code == requests.codes.ok
        time.sleep(0.1)

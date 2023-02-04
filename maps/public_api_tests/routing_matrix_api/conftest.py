import os
import pytest
import time


@pytest.fixture(scope="session")
def url_routing_matrix_api(request):
    return os.environ.get('ROUTING_MATRIX_API', 'https://api.routing.yandex.net')


@pytest.fixture(scope="session")
def routing_matrix_cgi(request):
    cgi = {
        "origins": "55.73831800000001,37.600792|55.717045999999996,37.578953999999996|55.728611,37.586518|55.7,37.6",
        "destinations": "55.755773,37.614608|55.727856,37.584002000000005",
        "departure_time": int(time.time())
    }
    return cgi

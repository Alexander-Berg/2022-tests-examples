import time
import requests

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_local
from maps.b2bgeo.ya_courier.backend.test_lib.util import env_patch_request, env_get_request


@skip_if_local
def test_BBGEO_1914(system_env_with_db):
    start = time.time()
    response = env_patch_request(
        system_env_with_db,
        path="couriers/16050/routes/149778/orders/2101911",
        data={"status": "finished"},
        auth=system_env_with_db.auth_header_super
    )
    assert response.status_code == requests.codes.ok
    end = time.time()
    assert (end - start) < 3


@skip_if_local
def test_BBGEO_1926(system_env_with_db):
    start = time.time()
    response = env_get_request(
        system_env_with_db,
        path="companies/123/courier-quality?date=2019-02-01",
        auth=system_env_with_db.auth_header_super
    )
    assert response.status_code == requests.codes.ok
    end = time.time()
    expected_time_limit_s = 7
    assert (end - start) < 3 * expected_time_limit_s

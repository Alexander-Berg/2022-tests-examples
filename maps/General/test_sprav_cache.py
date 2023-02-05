from lib.sprav import get_permissions_check_counter
from lib.server import server
from lib.waiter import wait_for


def test_sprav_permission_reqest_count(user, organization):
    requests_count = 20
    for _ in range(requests_count):
        server.get_prices(user, organization) >> 200
    assert 1 <= get_permissions_check_counter() < requests_count


def test_permission_expiration(user, company):
    server.get_prices(user, company) >> 200

    wait_time_seconds = 3
    # wait_time must be greater then cache_expiration_seconds in api_server config

    def do_request_and_check_permissions_count():
        server.get_prices(user, company) >> 200
        return get_permissions_check_counter() > 1

    assert wait_for(do_request_and_check_permissions_count, timeout=wait_time_seconds)

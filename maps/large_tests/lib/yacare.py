import time
import logging
from collections import deque

from maps.automotive.libs.large_tests.lib.http import http_request
import maps.automotive.libs.large_tests.lib.docker as docker


def wait_for_yacare_startup(url, host, timeout_sec=120):
    start_time = time.time()
    while time.time() - start_time < timeout_sec:
        try:
            status, _ = http_request('GET', url + '/ping', headers={'Host': host})
            if status == 200:
                return
        except:
            pass
        time.sleep(0.5)

    assert False, "'%s' still does not work" % url


def check_yacare_service_startup(url, host):
    try:
        status, _ = http_request('GET', url + '/ping', headers={'Host': host}, timeout=0.010)
        if status == 200:
            return True
    except:
        pass
    return False


all_services_started = False


def wait_for_yacare_services_startup(urls, hosts, timeout_sec=120):
    global all_services_started
    if all_services_started:
        return True

    assert len(urls) == len(hosts), "URLs and Hosts length must be equal"
    to_check = deque(zip(urls, hosts))

    start_time = time.time()

    while time.time() - start_time < timeout_sec and to_check:
        if not docker.check_containers_health():
            return False

        service_to_check = to_check.popleft()
        if check_yacare_service_startup(service_to_check[0], service_to_check[1]):
            continue
        to_check.append(service_to_check)
        time.sleep(0.5)

    if to_check or not docker.check_containers_health():
        logging.error("URLs '%s' still don't work" % list(to_check))
        return False

    all_services_started = True
    return True

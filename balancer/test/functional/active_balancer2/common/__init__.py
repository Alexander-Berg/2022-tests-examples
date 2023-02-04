import datetime
from balancer.test.util.balancer import asserts
from Queue import Empty


DELAY = 1

MIN_DELTA = datetime.timedelta(seconds=0.9 * DELAY)
MAX_DELTA = datetime.timedelta(seconds=1.1 * DELAY)

TEST_URL = '/test.html'


def get_times(backend):
    req_infos = list()
    has_reqs = True
    while has_reqs:
        try:
            req_infos.append(backend.state.requests.get(timeout=0.01))
        except Empty:
            has_reqs = False
    tests = list()
    requests = list()
    for req_info in req_infos:
        if req_info.request.request_line.path == TEST_URL:
            tests.append(req_info.start_time)
        else:
            requests.append(req_info.start_time)
    return tests, requests


def check_near(grid, points):
    """
    Проверяет, что рядом с каждым узлом сетки есть хотя бы одна точка
    """
    for node in grid:
        near = lambda point: abs(node - point) < MAX_DELTA
        if not any(map(near, points)):
            return False
    return True


def assert_responses(responses):
    map(lambda response: asserts.status(response, 200), responses)

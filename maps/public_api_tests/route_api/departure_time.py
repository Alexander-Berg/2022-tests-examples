from test_generic import request, TestRouterApi, SIMPLE_ONE_SEGMENT_QUERY
import time


def _request_with_time(timestamp):
    return request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time={timestamp}")


def _request_with_time_and_mode(timestamp, mode):
    return request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time={timestamp}&mode={mode}")


def _now():
    return int(time.time())


class TestRouterApiDepartureTime(TestRouterApi):
    def test_departure_time_generic(self):
        resp = request(
            f"?waypoints=55.740055,37.543377|55.730344,37.548722|55.753475,37.582908|55.753083,37.602568&departure_time={_now()}")
        self.assert_paths(resp, 3)

    def _assert_traffic_type(self, resp, expected_type):
        self.assert_code(resp, 200)
        resp = resp.json()
        self.assertEqual(resp["traffic_type"], expected_type)

    def test_departure_in_past(self):
        _5_min_ago = _now() - 60 * 5
        resp = _request_with_time(_5_min_ago)
        self._assert_traffic_type(resp, "realtime")
        self.assert_paths(resp, 1)

        _15_min_ago = _now() - 60 * 15
        resp = _request_with_time(_15_min_ago)
        self.assert_code(resp, 400)

    def test_departure_in_future(self):
        _1_month_later = _now() + 60 * 60 * 24 * 30
        resp = _request_with_time(_1_month_later)
        self._assert_traffic_type(resp, "forecast")
        self.assert_paths(resp, 1)

        _40_min_later = _now() + 60 * 40
        resp = _request_with_time(_40_min_later)
        self._assert_traffic_type(resp, "forecast")
        self.assert_paths(resp, 1)

        _20_min_later = _now() + 60 * 20
        resp = _request_with_time(_20_min_later)
        self._assert_traffic_type(resp, "realtime")
        self.assert_paths(resp, 1)

    def test_departure_different_modes(self):
        for mode in ["driving", "transit"]:
            resp = _request_with_time_and_mode(_now(), mode)
            self.assert_code(resp, 200)
            self.assert_paths(resp, 1)

    def test_departure_time_with_walking_not_supported(self):
        resp = _request_with_time_and_mode(_now(), 'walking')
        self.assert_code(resp, 400)

    def test_departure_wrong_format(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time={-_now()}")
        self.assert_code(resp, 400)

        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time=trololo")
        self.assert_code(resp, 400)

    def test_departure_explicit_empty(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time=")
        self.assert_code(resp, 400)

    def test_multiple_departure_time(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&departure_time={_now()}&departure_time={_now()}")
        self.assert_code(resp, 400)

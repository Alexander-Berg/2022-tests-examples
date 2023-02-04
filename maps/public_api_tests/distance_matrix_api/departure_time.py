from test_generic import request, TestDistanceMatrixApi, SIMPLE_ONE_TO_ONE_QUERY
import time


def _request_with_time(timestamp):
    return request("{}&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, timestamp))


def _request_with_time_and_mode(timestamp, mode):
    return request(SIMPLE_ONE_TO_ONE_QUERY + "&departure_time={}&mode={}".format(timestamp, mode))


def _now():
    return int(time.time())


class TestDistanceMatrixApiDepartureTime(TestDistanceMatrixApi):
    def test_departure_time_generic(self):
        resp = request(
            "?origins=55.740055,37.543377|55.730344,37.548722&destinations=55.753475,37.582908|55.753083,37.602568&departure_time={}".format(_now()))
        self.assert_code(resp, 200)

        resp = resp.json()
        rows = resp["rows"]
        self.assertEqual(len(rows), 2)
        for row in rows:
            elements = row["elements"]
            self.assertEqual(len(elements), 2)
            for element in elements:
                self.assertEqual(element["status"], "OK")
                self.assertTrue("distance" in element)
                self.assertTrue("duration" in element)

    def test_departe_in_past_internal_request(self):
        _15_min_ago = _now() - 60 * 15
        resp = request("{}&departure_time={}&internal_request=1".format(SIMPLE_ONE_TO_ONE_QUERY, _15_min_ago))
        self.assert_code(resp, 200)

    def test_departure_different_modes(self):
        for mode in ["driving", "transit"]:
            resp = _request_with_time_and_mode(_now(), mode)
            self.assert_code(resp, 200)
            resp = resp.json()
            rows = resp["rows"]
            self.assertEqual(len(rows), 1)
            elements = rows[0]["elements"]
            self.assertEqual(len(elements), 1)
            element = elements[0]
            self.assertEqual(element["status"], "OK")
            self.assertTrue("distance" in element)
            self.assertTrue("duration" in element)

    def test_departure_time_with_walking_not_supported(self):
        resp = _request_with_time_and_mode(_now(), 'walking')
        self.assert_code(resp, 400)

    def test_departure_wrong_format(self):
        resp = request("{}&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, -_now()))
        self.assert_code(resp, 400)

        resp = request("{}&departure_time=trololo".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

    def test_departure_explicit_empty(self):
        resp = request("{}&departure_time=".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

    def test_multiple_departure_time(self):
        resp = request("{}&departure_time={}&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, _now(), _now()))
        self.assert_code(resp, 400)

    def test_departure_time_for_transit_mode_of_internal_request(self):
        resp = request("?origins=55.799087,37.729377&destinations=55.799087,37.729377&departure_time={}&mode=transit&internal_request=1".format(_now()))
        self.assert_code(resp, 200)

    def test_diffrent_result_for_different_departure_times(self):
        resp_now = request("{}&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, _now()))
        self.assert_code(resp_now, 200)
        data_now = resp_now.json()["rows"][0]["elements"][0]

        resp_future = request("{}&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, _now() + 12 * 60 * 60))
        self.assert_code(resp_future, 200)
        data_future = resp_future.json()["rows"][0]["elements"][0]

        assert(data_now["status"] == "OK")
        assert(data_future["status"] == "OK")
        assert(data_now["duration"] != data_future["duration"])

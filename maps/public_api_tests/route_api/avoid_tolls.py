from test_generic import request, TestRouterApi, SIMPLE_ONE_SEGMENT_QUERY


def _request_with_avoid_tolls(avoid_tolls_value):
    return request(f"{SIMPLE_ONE_SEGMENT_QUERY}&avoid_tolls={avoid_tolls_value}")


class RouterApiAvoidTollsTest(TestRouterApi):
    def test_tolls(self):
        resp = _request_with_avoid_tolls("true")
        self.assert_paths(resp, 1)

        resp = _request_with_avoid_tolls("false")
        self.assert_paths(resp, 1)

        resp = _request_with_avoid_tolls("1")
        self.assert_paths(resp, 1)

        resp = _request_with_avoid_tolls("0")
        self.assert_paths(resp, 1)

    def test_tolls_wrong_format(self):
        resp = _request_with_avoid_tolls("")
        self.assert_code(resp, 400)

        resp = _request_with_avoid_tolls("qwerty")
        self.assert_code(resp, 400)

    def test_multiple_tolls(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&avoid_tolls=true&avoid_tolls=false")
        self.assert_code(resp, 400)

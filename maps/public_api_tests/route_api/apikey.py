from test_generic import (request, request_without_apikey, TestRouterApi,
                          DEFAULT_APIKEY, SIMPLE_ONE_SEGMENT_QUERY)


class TestRouterApiKey(TestRouterApi):
    def test_no_apikey(self):
        resp = request_without_apikey(SIMPLE_ONE_SEGMENT_QUERY)
        self.assert_code(resp, 401)

    def test_wrong_apikey(self):
        resp = request(SIMPLE_ONE_SEGMENT_QUERY, apikey="trololo")
        self.assert_code(resp, 401)

    def test_empty_apikey(self):
        resp = request(SIMPLE_ONE_SEGMENT_QUERY, apikey="")
        self.assert_code(resp, 401)

    def test_correct_apikey(self):
        resp = request(SIMPLE_ONE_SEGMENT_QUERY)
        self.assert_code(resp, 200)
        self.assert_paths(resp, 1)

    def test_multiple_apikeys(self):
        resp = request_without_apikey(f"{SIMPLE_ONE_SEGMENT_QUERY}&apikey={DEFAULT_APIKEY}&apikey={DEFAULT_APIKEY}")
        self.assert_code(resp, 400)

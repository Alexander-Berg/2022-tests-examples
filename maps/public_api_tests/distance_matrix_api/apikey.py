from test_generic import (request, request_without_apikey, TestDistanceMatrixApi,
                          SIMPLE_ONE_TO_ONE_QUERY)
from request_client import (DEFAULT_APIKEY, BANNED_APIKEY)


class TestDistanceMatrixApiKey(TestDistanceMatrixApi):
    def test_no_apikey(self):
        resp = request_without_apikey(SIMPLE_ONE_TO_ONE_QUERY)
        self.assert_code(resp, 401)

    def test_wrong_apikey(self):
        resp = request(SIMPLE_ONE_TO_ONE_QUERY, apikey="trololo")
        self.assert_code(resp, 401)

    def test_empty_apikey(self):
        resp = request(SIMPLE_ONE_TO_ONE_QUERY, apikey="")
        self.assert_code(resp, 401)

    def test_banned_apikey(self):
        resp = request(SIMPLE_ONE_TO_ONE_QUERY, apikey=BANNED_APIKEY)
        self.assert_code(resp, 401)

    def test_correct_apikey(self):
        resp = request(SIMPLE_ONE_TO_ONE_QUERY)
        self.assert_code(resp, 200)

    def test_multiple_apikeys(self):
        resp = request_without_apikey("{}&apikey={}&apikey={}".format(SIMPLE_ONE_TO_ONE_QUERY, DEFAULT_APIKEY, DEFAULT_APIKEY))
        self.assert_code(resp, 400)

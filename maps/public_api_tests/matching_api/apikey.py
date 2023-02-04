from test_generic import (request, request_without_apikey, TestMatchApi,
                          DEFAULT_APIKEY, SIMPLE_QUERY)


class TestMatchApiKey(TestMatchApi):
    def test_no_apikey(self):
        resp = request_without_apikey(SIMPLE_QUERY)
        self.check_response(resp, 401)

    def test_wrong_apikey(self):
        resp = request(SIMPLE_QUERY, apikey="trololo")
        self.check_response(resp, 401)

    def test_empty_apikey(self):
        resp = request(SIMPLE_QUERY, apikey="")
        self.check_response(resp, 401)

    def test_correct_apikey(self):
        resp = request(SIMPLE_QUERY)
        self.check_response(resp, 200)

    def test_multiple_apikeys(self):
        resp = request_without_apikey("{}&apikey={}&apikey={}".format(SIMPLE_QUERY, DEFAULT_APIKEY, DEFAULT_APIKEY))
        self.check_response(resp, 400)

from test_generic import request, TestDistanceMatrixApi, SIMPLE_ONE_TO_ONE_QUERY


def _request_with_priority(priority):
    return request("{}&priority={}".format(SIMPLE_ONE_TO_ONE_QUERY, priority))


class TestDistanceMatrixApiPriority(TestDistanceMatrixApi):
    def test_corrent_priority(self):
        resp = _request_with_priority("realtime")
        self.assert_code(resp, 200)

        resp = _request_with_priority("normal")
        self.assert_code(resp, 200)

from test_generic import request, TestRouterApi


POINT_VECTOR_LIMIT = 50
POINTS = ["55.594416,37.606422", "55.597530,37.607528"]


def _make_point_query(count):
    points = [POINTS[i % len(POINTS)] for i in range(count)]
    return '|'.join(points)


class TestRouterApiQueryLimit(TestRouterApi):
    def test_origins_limit(self):
        waypoints = _make_point_query(POINT_VECTOR_LIMIT)
        resp = request(f"?waypoints={waypoints}")
        self.assert_code(resp, 200)
        self.assert_paths(resp, POINT_VECTOR_LIMIT - 1)

    def test_destiantions_over_limit(self):
        waypoints = _make_point_query(POINT_VECTOR_LIMIT + 1)
        resp = request(f"?waypoints={waypoints}")
        print(resp.json())
        self.assert_code(resp, 400)

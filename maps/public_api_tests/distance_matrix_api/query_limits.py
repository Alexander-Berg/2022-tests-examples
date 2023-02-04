from test_generic import request, TestDistanceMatrixApi


POINT_VECTOR_LIMIT = 100
POINT_SQUARE_LIMIT = 10


def make_point_query(point, count):
    points = [point] * count
    return '|'.join(points)


def make_origins_query(count):
    return make_point_query("55.594416,37.606422", count)


def make_destinations_query(count):
    return make_point_query("55.597530,37.607528", count)


class TestDistanceMatrixApiQueryLimit(TestDistanceMatrixApi):
    def test_origins_limit(self):
        origins_query = make_origins_query(POINT_VECTOR_LIMIT)
        resp = request("?origins={}&destinations=55.597530,37.607528".format(origins_query))
        self.assert_code(resp, 200)

        resp = resp.json()
        rows = resp["rows"]

        self.assertEqual(len(rows), POINT_VECTOR_LIMIT)
        row = rows[0]
        elements = row["elements"]
        self.assertEqual(len(elements), 1)

    def test_destiantions_over_limit(self):
        destinations_query = make_destinations_query(POINT_VECTOR_LIMIT + 1)
        resp = request("?origins=55.594416,37.606422&destinations={}".format(destinations_query))
        self.assert_code(resp, 400)

    def test_square_limit(self):
        origins_query = make_origins_query(POINT_SQUARE_LIMIT)
        destinations_query = make_destinations_query(POINT_SQUARE_LIMIT)
        resp = request("?origins={}&destinations={}".format(origins_query, destinations_query))

        resp = resp.json()
        rows = resp["rows"]

        self.assertEqual(len(rows), POINT_SQUARE_LIMIT)
        for row in rows:
            elements = row["elements"]
            self.assertEqual(len(elements), POINT_SQUARE_LIMIT)

    def test_square_over_limit(self):
        origins_query = make_origins_query(POINT_SQUARE_LIMIT + 1)
        destinations_query = make_destinations_query(POINT_SQUARE_LIMIT + 1)
        resp = request("?origins={}&destinations={}".format(origins_query, destinations_query))
        self.assert_code(resp, 400)

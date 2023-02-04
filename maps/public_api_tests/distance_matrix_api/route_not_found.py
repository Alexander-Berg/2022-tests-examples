from test_generic import request, TestDistanceMatrixApi


class TestDistanceMatrixApiRouteNotFound(TestDistanceMatrixApi):
    def test_route_not_found(self):
        resp = request("?origins=55.726026,37.642115&destinations=-79.379209,42.357108")
        self.assert_code(resp, 200)

        resp = resp.json()
        rows = resp["rows"]

        self.assertEqual(len(rows), 1)
        row = rows[0]
        elements = row["elements"]
        self.assertEqual(len(elements), 1)
        element = elements[0]

        self.assertEqual(element["status"], "FAIL")
        self.assertFalse("distance" in element)
        self.assertFalse("duration" in element)

    def test_one_from_two_not_found(self):
        resp = request("?origins=55.594416,37.606422&destinations=55.597530,37.607528|-79.379209,42.357108")
        self.assert_code(resp, 200)

        resp = resp.json()
        rows = resp["rows"]

        self.assertEqual(len(rows), 1)
        row = rows[0]
        elements = row["elements"]
        self.assertEqual(len(elements), 2)

        element = elements[0]
        self.assertEqual(element["status"], "OK")
        self.assertTrue("distance" in element)
        self.assertTrue("duration" in element)

        element = elements[1]
        self.assertEqual(element["status"], "FAIL")
        self.assertFalse("distance" in element)
        self.assertFalse("duration" in element)

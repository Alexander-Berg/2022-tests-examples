from test_generic import request, TestRouterApi, ROUTING_MODES


URL_TEMPLATE = "?waypoints={waypoints}"
ORG = "55.594416,37.606422"
DST = "55.597530,37.607528"


class TestRouterWaypoints(TestRouterApi):
    def _assert_error_contains(self, resp, text):
        resp = resp.json()
        error = resp["errors"][0]
        self.assertTrue(text in error)

    def test_one(self):
        resp = request(URL_TEMPLATE.format(waypoints=ORG))
        self.assert_code(resp, 400)

    def test_empty(self):
        resp = request(URL_TEMPLATE.format(waypoints="|"))
        self.assert_code(resp, 400)
        resp = request(URL_TEMPLATE.format(waypoints=ORG + "|"))
        self.assert_code(resp, 400)
        resp = request(URL_TEMPLATE.format(waypoints="|" + DST))
        self.assert_code(resp, 400)

    def test_paths_count(self):
        resp = request(URL_TEMPLATE.format(waypoints="|".join([ORG, DST])))
        self.assert_paths(resp, 1)
        resp = request(URL_TEMPLATE.format(
            waypoints="|".join([ORG, DST, ORG])))
        self.assert_paths(resp, 2)
        resp = request(URL_TEMPLATE.format(
            waypoints="|".join([DST, ORG, DST])))
        self.assert_paths(resp, 2)
        resp = request(URL_TEMPLATE.format(
            waypoints="|".join([ORG, DST, ORG, DST, ORG])))
        self.assert_paths(resp, 4)

    def test_invalid_format(self):
        # invalid format - semicolon instead of comma
        resp = request(URL_TEMPLATE.format(
            waypoints="55.594416;37.606422|" + DST))
        self.assert_code(resp, 400)
        self._assert_error_contains(resp, "waypoints")

    def test_out_of_bounds_coordinates(self):
        # Valid bounds: (-90 <= latitude <= 90)  and (-180 <= longitude <= 180)
        # latitude less then -90 degrees (55.594416 - 180 = -124.405584
        resp = request(URL_TEMPLATE.format(
            waypoints="-124.405584,37.606422|" + DST))
        self.assert_code(resp, 400)
        # latitude less then -90 degrees (55.594416 - 360 = -305.405584
        resp = request(URL_TEMPLATE.format(
            waypoints="-305.405584,37.606422|" + DST))
        self.assert_code(resp, 400)
        # latitude greater then 90 degrees (55.594416 + 180 = 235.594416)
        resp = request(URL_TEMPLATE.format(
            waypoints="235.594416,37.606422|" + DST))
        self.assert_code(resp, 400)
        # latitude greater then 90 degrees (55.594416 + 360 = 415.594416)
        resp = request(URL_TEMPLATE.format(
            waypoints="415.594416,37.606422|" + DST))
        self.assert_code(resp, 400)
        # longitude less then -180 degrees (37.606422 - 360 = -323.393578
        resp = request(URL_TEMPLATE.format(
            waypoints="55.405584,-322.393578|" + DST))
        self.assert_code(resp, 400)
        # longitude greater then 180 degrees (37.606422 + 360 = 397.606422
        resp = request(URL_TEMPLATE.format(
            waypoints="55.405584,397.606422|" + DST))
        self.assert_code(resp, 400)

    def _get_total(self, resp, step_field):
        result = 0
        for leg in resp["route"]["legs"]:
            self.assertEqual(leg["status"], "OK")
            for step in leg["steps"]:
                result = result + step[step_field]
        return result

    def _get_duration(self, resp):
        return self._get_total(resp, "duration")

    def _get_distance(self, resp):
        return self._get_total(resp, "length")

    def test_origin_is_destination(self):
        resp = request(URL_TEMPLATE.format(waypoints="|".join([ORG, ORG])))

        self.assert_paths(resp, 1)

        self.assertEqual(self._get_distance(resp.json()), 0)
        self.assertEqual(self._get_duration(resp.json()), 0)

    def test_distance_and_duration(self):
        resp = request(URL_TEMPLATE.format(waypoints="|".join([ORG, DST])))
        self.assertTrue(self._get_distance(resp.json()) > 0)
        self.assertTrue(self._get_duration(resp.json()) > 0)

    def test_long_distance(self):
        # destination is Vladivostok 43.116365,131.882470
        resp = request(URL_TEMPLATE.format(
            waypoints="|".join([ORG, "43.116365,131.882470"])))
        self.assert_paths(resp, 1)

    def test_multiple_origins_and_destinations(self):
        url = URL_TEMPLATE.format(waypoints="|".join([ORG, DST]))
        resp = request(url + '&' + url[1:])
        self.assert_code(resp, 400)

    def test_almost_zero_distance(self):
        for mode in ROUTING_MODES:
            resp = request(URL_TEMPLATE.format(waypoints="|".join(["55.649158,37.743844", "55.649158,37.743844"]))
                           + "&mode=" + mode)
            self.assertTrue(self._get_distance(resp.json()) >= 0)
            self.assertTrue(self._get_duration(resp.json()) >= 0)

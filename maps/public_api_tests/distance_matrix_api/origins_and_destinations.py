from test_generic import request, TestDistanceMatrixApi
from query_limits import make_point_query

URL_TEMPLATE = "?origins={origins}&destinations={destinations}"
ORG = "55.594416,37.606422"
DST = "55.597530,37.607528"
UNREACHABLE = "44.137584,31.760533"


class TestDistanceMatrixOriginsAndDestinations(TestDistanceMatrixApi):
    def _assert_error_contains(self, resp, text):
        resp = resp.json()
        error = resp["errors"][0]
        print(error)
        self.assertTrue(text in error)

    def test_empty(self):
        resp = request(URL_TEMPLATE.format(origins="", destinations=""))
        self.assert_code(resp, 400)
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations=""))
        self.assert_code(resp, 400)
        resp = request(URL_TEMPLATE.format(origins="", destinations=DST))
        self.assert_code(resp, 400)

    def test_paths_count(self):
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations=DST))
        self.assert_paths(resp, 1)
        resp = request(URL_TEMPLATE.format(origins="|".join([ORG]*2), destinations=DST))
        self.assert_paths(resp, 2)
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations="|".join([DST]*2)))
        self.assert_paths(resp, 2)
        resp = request(URL_TEMPLATE.format(origins="|".join([ORG]*2), destinations="|".join([DST]*2)))
        self.assert_paths(resp, 4)

    def test_invalid_format(self):
        # invalid format - semicolon instead of comma
        resp = request(URL_TEMPLATE.format(origins="55.594416;37.606422", destinations=DST))
        self.assert_code(resp, 400)
        self._assert_error_contains(resp, "origins")

    def test_out_of_bounds_coordinates(self):
        # Valid bounds: (-90 <= latitude <= 90)  and (-180 <= longitude <= 180)
        # latitude less then -90 degrees (55.594416 - 180 = -124.405584
        resp = request(URL_TEMPLATE.format(origins="-124.405584,37.606422", destinations=DST))
        self.assert_code(resp, 400)
        # latitude less then -90 degrees (55.594416 - 360 = -305.405584
        resp = request(URL_TEMPLATE.format(origins="-305.405584,37.606422", destinations=DST))
        self.assert_code(resp, 400)
        # latitude greater then 90 degrees (55.594416 + 180 = 235.594416)
        resp = request(URL_TEMPLATE.format(origins="235.594416,37.606422", destinations=DST))
        self.assert_code(resp, 400)
        # latitude greater then 90 degrees (55.594416 + 360 = 415.594416)
        resp = request(URL_TEMPLATE.format(origins="415.594416,37.606422", destinations=DST))
        self.assert_code(resp, 400)
        # longitude less then -180 degrees (37.606422 - 360 = -323.393578
        resp = request(URL_TEMPLATE.format(origins="55.405584,-322.393578", destinations=DST))
        self.assert_code(resp, 400)
        # longitude greater then 180 degrees (37.606422 + 360 = 397.606422
        resp = request(URL_TEMPLATE.format(origins="55.405584,397.606422", destinations=DST))
        self.assert_code(resp, 400)

    def test_status_fail(self):
        # destination somewhere deep in the Black Sea
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations=UNREACHABLE))
        self.assert_code(resp, 200)
        self.assertEqual(resp.json()["rows"][0]["elements"][0]["status"], "FAIL")

    def test_origin_is_destination(self):
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations=ORG))
        self.assert_paths(resp, 1)
        self.assertEqual(resp.json()["rows"][0]["elements"][0]["distance"]["value"], 0)
        self.assertEqual(resp.json()["rows"][0]["elements"][0]["duration"]["value"], 0)

    def test_distance_and_duration(self):
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations=DST))
        self.assertTrue(resp.json()["rows"][0]["elements"][0]["distance"]["value"] > 0)
        self.assertTrue(resp.json()["rows"][0]["elements"][0]["duration"]["value"] > 0)

    def test_long_distance(self):
        # destination is Vladivostok 43.116365,131.882470
        resp = request(URL_TEMPLATE.format(origins=ORG, destinations="43.116365,131.882470"))
        self.assert_paths(resp, 1)

    def test_order_of_cells(self):
        resp = request(URL_TEMPLATE.format(
            origins="|".join([ORG, UNREACHABLE, DST]),
            destinations="|".join([ORG, UNREACHABLE, DST])))
        self.assert_code(resp, 200)
        self.assertEqual(resp.json()["rows"][0]["elements"][0]["status"], "OK")
        self.assertEqual(resp.json()["rows"][0]["elements"][1]["status"], "FAIL")
        self.assertEqual(resp.json()["rows"][0]["elements"][2]["status"], "OK")

        self.assertEqual(resp.json()["rows"][1]["elements"][0]["status"], "FAIL")
        self.assertEqual(resp.json()["rows"][1]["elements"][1]["status"], "FAIL")
        self.assertEqual(resp.json()["rows"][1]["elements"][2]["status"], "FAIL")

        self.assertEqual(resp.json()["rows"][2]["elements"][0]["status"], "OK")
        self.assertEqual(resp.json()["rows"][2]["elements"][1]["status"], "FAIL")
        self.assertEqual(resp.json()["rows"][2]["elements"][2]["status"], "OK")

    def test_multiple_origins_and_destinations(self):
        url = URL_TEMPLATE.format(origins=ORG, destinations=DST)
        resp = request(url + '&' + url[1:])
        self.assert_code(resp, 400)

    # EXTDATA-1595
    def test_order_of_rows(self):
        TEST_POINT = "55.594010,37.612424"

        origins = make_point_query(ORG, 2) + "|{}|".format(TEST_POINT) + make_point_query(ORG, 8)
        destinations = make_point_query(DST, 2) + "|{}".format(TEST_POINT)

        resp = request(URL_TEMPLATE.format(origins=origins, destinations=destinations))
        self.assert_code(resp, 200)
        resp = resp.json()

        self.assertTrue(resp["rows"][2]["elements"][2]["duration"]["value"] == 0)
        self.assertTrue(resp["rows"][3]["elements"][2]["duration"]["value"] != 0)

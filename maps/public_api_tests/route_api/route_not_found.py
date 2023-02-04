import unittest
from test_generic import request, TestRouterApi, is_v2_api, ROUTING_MODES


ORG = "55.594416,37.606422"
DST = "55.597530,37.607528"
UNREACHABLE = "44.137584,31.760533"  # somewhere deep in the Black Sea


class TestRouterApiRouteNotFound(TestRouterApi):
    def run_route_not_found(self, mode):
        waypoints = "|".join([ORG, UNREACHABLE])
        resp = request(f"?waypoints={waypoints}&mode={mode}")
        self.assert_code(resp, 200)
        self.assert_paths(resp, 1)

        legs = resp.json()["route"]["legs"]
        self.assertEqual(legs[0]["status"], "FAIL")

    def test_route_not_found(self):
        for mode in ROUTING_MODES:
            self.run_route_not_found(mode)

    def run_one_from_two_not_found(self, mode):
        waypoints = "|".join([ORG, DST, UNREACHABLE])
        resp = request(f"?waypoints={waypoints}&mode={mode}")
        self.assert_code(resp, 200)
        self.assert_paths(resp, 2)

        legs = resp.json()["route"]["legs"]
        self.assertEqual(legs[0]["status"], "OK")
        self.assertEqual(legs[1]["status"], "FAIL")

    @unittest.skipIf(is_v2_api(), "skiping for v2, see BBGEO-6556")
    def test_one_from_two_not_found(self):
        for mode in ROUTING_MODES:
            self.run_one_from_two_not_found(mode)

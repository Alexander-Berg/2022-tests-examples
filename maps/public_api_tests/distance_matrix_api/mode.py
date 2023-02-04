from test_generic import request, TestDistanceMatrixApi, SIMPLE_ONE_TO_ONE_QUERY


class DistanceMatrixApiModeTest(TestDistanceMatrixApi):
    def _collect_durations(self, resp):
        durations = []

        self.assert_code(resp, 200)
        resp = resp.json()
        self.assertIn("rows", resp)
        for row in resp["rows"]:
            for element in row["elements"]:
                self.assertEqual(element["status"], "OK")
                self.assertTrue("distance" in element)
                self.assertTrue("duration" in element)
                durations.append(element["duration"]["value"])

        return durations

    def test_modes(self):
        durations_per_mode = {}
        for mode in ["driving", "walking", "transit"]:
            resp = request(
                "?origins=55.740055,37.543377|55.730344,37.548722"
                "&destinations=55.753475,37.582908|55.753083,37.602568&mode={}".format(mode))
            durations_per_mode[mode] = self._collect_durations(resp)
            self.assertEqual(len(durations_per_mode[mode]), 4)

        for idx in range(4):
            self.assertGreater(
                durations_per_mode["walking"][idx], durations_per_mode["driving"][idx],
                "Duration ({}) with mode walking: {} should be greater than with mode driving: {}".format(
                    idx, durations_per_mode["walking"][idx], durations_per_mode["driving"][idx]))
            self.assertGreater(
                durations_per_mode["walking"][idx], durations_per_mode["transit"][idx],
                "Duration ({}) with mode walking: {} should be greater than with mode transit: {}".format(
                    idx, durations_per_mode["walking"][idx], durations_per_mode["transit"][idx]))

    def test_mode_wrong_format(self):
        resp = request("{}&mode=1".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

        resp = request("{}&mode=unknown".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

    def test_mode_explicit_empty(self):
        resp = request("{}&mode=".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

    def test_multiple_modes(self):
        resp = request("{}&mode=driving&mode=walking".format(SIMPLE_ONE_TO_ONE_QUERY))
        self.assert_code(resp, 400)

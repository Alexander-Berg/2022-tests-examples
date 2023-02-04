from test_generic import request, TestRouterApi, SIMPLE_ONE_SEGMENT_QUERY


class RouterApiModeTest(TestRouterApi):
    def _collect_durations(self, resp):
        durations = []

        self.assert_code(resp, 200)
        resp = resp.json()
        for leg in resp["route"]["legs"]:
            self.assertEqual(leg["status"], "OK")
            duration = 0
            for step in leg["steps"]:
                duration = duration + step["duration"]
            durations.append(duration)

        return durations

    def test_modes(self):
        durations_per_mode = {}
        for mode in ["driving", "transit", "walking"]:
            resp = request(
                f"?waypoints=55.740055,37.543377|55.753475,37.682908|55.730344,37.548722|55.753083,37.602568&mode={mode}")
            self.assert_paths(resp, 3)
            durations_per_mode[mode] = self._collect_durations(resp)
            self.assertEqual(len(durations_per_mode[mode]), 3)

        for idx in range(3):
            self.assertTrue(
                durations_per_mode["walking"][idx] > durations_per_mode["driving"][idx])
            self.assertTrue(
                durations_per_mode["walking"][idx] > durations_per_mode["transit"][idx])

    def test_mode_wrong_format(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&mode=1")
        self.assert_code(resp, 400)

        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&mode=unknown")
        self.assert_code(resp, 400)

    def test_mode_explicit_empty(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&mode=")
        self.assert_code(resp, 400)

    def test_multiple_modes(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&mode=driving&mode=walking")
        self.assert_code(resp, 400)

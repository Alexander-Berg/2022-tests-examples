import unittest

from .common import solve_task, create_task


class MultpleRoutingModesTest(unittest.TestCase):
    def test_routing_modes_total_duration_s(self):
        self.assertEqual(True, True)
        routing_modes = ['driving', 'truck', 'transit', 'walking']
        result = solve_task(create_task(routing_modes), 100)
        routes = result['result']['routes']
        self.assertEqual(len(routes), len(routing_modes))

        durations = {mode: routes[i]['metrics']['total_duration_s'] for i, mode in enumerate(routing_modes)}

        self.assertLessEqual(durations['driving'], durations['truck'])
        self.assertLess(durations['truck'], durations['transit'])
        self.assertLess(durations['transit'], durations['walking'])

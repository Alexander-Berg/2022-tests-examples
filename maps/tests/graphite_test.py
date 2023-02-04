import unittest
from mock import patch, Mock

from maps.infra.monitoring.sla_calculator.core.graphite import graphite_statuses


class GraphiteStatusesTest(unittest.TestCase):
    @patch('requests.get')
    def test(self, get_method):
        SAMPLING_RATE = 15
        response = Mock()
        response.json.return_value = [{
            "target": "test_target",
            "datapoints": [
                # Spend 2 + 1 intervals under the treshold
                [
                    # value
                    1.0,
                    # timestamp
                    10 * SAMPLING_RATE
                ],
                [
                    # This is still not more than 2. So this is counted as bad request.
                    2.0,
                    (10 + 2) * SAMPLING_RATE
                ],
                # Following two intervals are good
                [
                    3.0,
                    (10 + 2 + 1) * SAMPLING_RATE
                ],
                [
                    4.0,
                    (10 + 2 + 1 + 1) * SAMPLING_RATE
                ]]}]
        get_method.return_value = response

        statuses = graphite_statuses('test_target', '2017-01-01', more_than=2)
        statuses.set_index('status', inplace=True)

        self.assertAlmostEqual(statuses['amount'][200], 2)
        self.assertAlmostEqual(statuses['amount'][500], 3)

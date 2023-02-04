import mock
import unittest

import maps.carparks.tools.carparks_miner.lib.create_filtered_relocated_targets_table \
    as tested_module


def _make_row(target_lat, target_lon, timestamp=0,
              lat=None, lon=None):
    if lat is None:
        lat = target_lat
    if lon is None:
        lon = target_lon
    result = {'target_lat': target_lat,
              'target_lon': target_lon,
              'timestamp': timestamp,
              'lat': lat, 'lon': lon}
    return result


class CorrectTargetsReducerTests(unittest.TestCase):
    def assert_results_equal(self, results, expected):
        self.assertEqual(len(results), len(expected))
        for i in range(len(results)):
            result_keys = sorted(results[i].keys())
            expected_keys = sorted(expected[i].keys())
            self.assertListEqual(result_keys, expected_keys)
            for key in result_keys:
                result_value = results[i][key]
                expected_value = expected[i][key]
                message = "Result {}.{} value differs: result {}, expected {}" \
                    .format(i, key, result_value, expected_value)
                message += "\nFull: results {}, expected {}" \
                    .format(results, expected)
                if isinstance(result_value, int) \
                        or isinstance(result_value, float):
                    self.assertAlmostEqual(
                        result_value, expected_value, 7, msg=message)
                else:
                    self.assertEqual(
                        result_value, expected_value, msg=message)

    def test_orgid_is_not_replaced_to_coordinates(self):
        rows = [_make_row(50, 40, timestamp=0, lat=60, lon=50)]
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(50, 40, timestamp=0, lat=60, lon=50)]
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_choose_new_points_and_delete_old(self):
        rows = [_make_row(0, 0, timestamp=0)] * 3 + \
               [_make_row(0, 1, timestamp=5)] * 2
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 1, timestamp=5)] * 2
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_new_points_quantity_under_threshold_returns_largest_domain_points(self):
        rows = [_make_row(0, 0, timestamp=0)] * 3 + \
               [_make_row(0, 1, timestamp=5)] * 1
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 0, timestamp=0)] * 3
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_new_points_quantity_above_threshold_returns_newest_domain_points(self):
        rows = [_make_row(0, 0, timestamp=0)] * 3 + \
               [_make_row(0, 1, timestamp=5)] * 2
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 1, timestamp=5)] * 2
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    def test_far_and_near_points_returns_near_points(self):
        '''
        note: one degree on equator is about 111111 meters
        so, 0.0001 degree a little bigger than 11 meters
        '''
        rows = [_make_row(target_lat=0, target_lon=0, timestamp=0),
                _make_row(target_lat=0, target_lon=0, timestamp=0),
                _make_row(target_lat=0, target_lon=0.0004, timestamp=0),
                _make_row(target_lat=0, target_lon=0.0005, timestamp=0)]
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 0, timestamp=0),
                    _make_row(0, 0, timestamp=0),
                    _make_row(target_lat=0, target_lon=0.0004, timestamp=0)]
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_little_difference_in_target_does_not_matter(self):
        rows = [_make_row(target_lat=0, target_lon=0, timestamp=0)] * 3 + \
               [_make_row(target_lat=0, target_lon=1.00000, timestamp=5),
                _make_row(target_lat=0, target_lon=1.00004, timestamp=0)]
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(target_lat=0, target_lon=1.0000,
                                   timestamp=5, lat=0, lon=1.0000),
                    _make_row(target_lat=0, target_lon=1.00004,
                                   timestamp=0, lat=0, lon=1.00004)]

        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_return_old_target_when_new_target_is_not_fresh_enough(self):
        rows = [_make_row(target_lat=0, target_lon=0, timestamp=0)] * 3 + \
               [_make_row(target_lat=0, target_lon=1.00000, timestamp=3),
                _make_row(target_lat=0, target_lon=1.00004, timestamp=0)]
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 0, timestamp=0)] * 3
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_choose_newest_big_target_and_delete_other(self):
        rows = [_make_row(0, 0, timestamp=0)] * 6 + \
               [_make_row(0, 2, timestamp=4)] * 3 + \
               [_make_row(0, 1, timestamp=5)] * 5
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 1, timestamp=5)] * 5
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_choose_bigger_target_if_it_is_not_too_old(self):
        rows = [_make_row(0, 0, timestamp=3)] * 6 + \
               [_make_row(0, 2, timestamp=4)] * 3 + \
               [_make_row(0, 1, timestamp=5)] * 5
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 0, timestamp=3)] * 6
        self.assert_results_equal(results, expected)

    @mock.patch(tested_module.__name__ + '.MIN_BUCKET_SIZE', 2)
    @mock.patch(tested_module.__name__ + '.TIMESTAMP_THRESHOLD', 4)
    def test_choose_newest_target_instead_of_two_old_bigger_targets(self):
        rows = [_make_row(0, 0, timestamp=0)] * 6 + \
               [_make_row(0, 2, timestamp=0)] * 10 + \
               [_make_row(0, 1, timestamp=5)] * 5
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))

        expected = [_make_row(0, 1, timestamp=5)] * 5
        self.assert_results_equal(results, expected)


@mock.patch(tested_module.__name__ + '.MAX_PRE_SNAP_POINTS', 5)
class MaxRowsSize(unittest.TestCase):
    def test_max_return_row_size(self):
        rows = [_make_row(target_lon=0, target_lat=0)] * 10
        results = list(tested_module.filter_relocated_target_rows_reducer(None, rows))
        self.assertEqual(len(results), 5)

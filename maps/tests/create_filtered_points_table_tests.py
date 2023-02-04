import mock
import unittest
from datetime import date, timedelta

import maps.carparks.tools.carparks_miner.lib.create_filtered_points_table \
    as tested_module


'''
in tests we need only timestamp attribute
'''

MAX_POINTS_TEST = 5
MIN_LOOK_UP_DAYS_TEST = 5

DAY_1969_NOV_30 = [-2751243, -2749992, -2740809, -2714668, -2699117, -2692689]
DAY_1969_DEC_27 = [-419127, -412534, -411774, -382004, -377719, -363671]
DAY_1969_DEC_28 = [-329958, -319640, -293317, -290666, -284283, -271187]
DAY_1969_DEC_30 = [-163949, -155967, -144423, -141556, -122596, -105499]
DAY_1969_DEC_31 = [-79373, -52567, -46928, -44734, -39546, -20191]
DAY_1970_JAN_01 = [8420, 27899, 32796, 39103, 58996, 68945, 80550, 86325]
FUTURE_EVENTS = [105499, 2692689]


def _make_test_data(timestamps):
    return [{'timestamp': t} for t in timestamps]


def _fake_filter(x):
    return x


def _to_timestamp(time):
    return (time - date(1970, 1, 1)).total_seconds()


class BaseTest(unittest.TestCase):
    def _run_and_check(self, data, expected, num_days=1):
        reducer = tested_module.FilterPointsReducer(end_date=date(1970, 1, 1),
                                                    num_suggested_days=num_days)
        result = list(reducer(None, iter(data)))
        self.assertListEqual(result, expected)


@mock.patch(tested_module.__name__ + '.MAX_POINTS_PER_DAY', MAX_POINTS_TEST)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class SingleDayTests(BaseTest):
    def test_empty_input(self, *mocks):
        self._run_and_check(data=[], expected=[])

    def test_small_input(self, *mocks):
        self._run_and_check(data=_make_test_data(DAY_1970_JAN_01[:2]),
                            expected=_make_test_data(DAY_1970_JAN_01[:2]))

    def test_exact_input_size(self, *mocks):
        self._run_and_check(
            data=_make_test_data(DAY_1970_JAN_01[:MAX_POINTS_TEST]),
            expected=_make_test_data(DAY_1970_JAN_01[:MAX_POINTS_TEST]))

    def test_result_size_and_order(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=date(1970, 1, 1),
                                                    num_suggested_days=1)
        result = list(reducer(None, iter(_make_test_data(DAY_1970_JAN_01[:]))))
        self.assertEqual(len(result), MAX_POINTS_TEST)
        timestamps = [row['timestamp'] for row in result]
        self.assertEqual(timestamps, sorted(timestamps[:]))


@mock.patch(tested_module.__name__ + '.MAX_POINTS_PER_DAY', MAX_POINTS_TEST)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class FewDaysBatchTests(BaseTest):
    '''we test three days batch'''
    end_date = date(1970, 1, 1)
    num_days = 3
    min_timestamp = _to_timestamp(end_date - timedelta(days=num_days - 1))
    max_timestamp = _to_timestamp(end_date + timedelta(days=1))
    DATA = _make_test_data(DAY_1969_DEC_30 + DAY_1969_DEC_31 + DAY_1970_JAN_01)

    def test_small_input(self, *mocks):
        self._run_and_check(data=self.DATA[:10],
                            expected=sorted(self.DATA[:10]),
                            num_days=self.num_days)

    def test_max_result_size(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=self.num_days)
        result = list(reducer(None, iter(self.DATA[:])))
        self.assertEqual(len(result), MAX_POINTS_TEST * self.num_days)


@mock.patch(tested_module.__name__ + '.MAX_POINTS_PER_DAY', MAX_POINTS_TEST)
@mock.patch(tested_module.__name__ + '.MIN_LOOK_UP_DAYS', MIN_LOOK_UP_DAYS_TEST)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class GetAllOldPointsTests(unittest.TestCase):
    '''we test three days batch'''
    end_date = date(1970, 1, 1)
    num_days = 2
    min_timestamp = _to_timestamp(end_date - timedelta(days=num_days - 1))
    max_timestamp = _to_timestamp(end_date + timedelta(days=1))
    old_timestamp = _to_timestamp(end_date - timedelta(days=MIN_LOOK_UP_DAYS_TEST - 1))
    NEW_DATA = _make_test_data(DAY_1969_DEC_31 + DAY_1970_JAN_01)
    OLD_DATA = _make_test_data(DAY_1969_DEC_28 + DAY_1969_DEC_30)
    DATA = NEW_DATA + OLD_DATA

    def test_result_size_all_old_rows_in_result(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=self.num_days)
        result = list(reducer(None, iter(self.DATA[:])))
        self.assertEqual(len(result), MAX_POINTS_TEST * self.num_days + len(self.OLD_DATA))
        self.assertListEqual(result[:len(self.OLD_DATA)], sorted(self.OLD_DATA))


@mock.patch(tested_module.__name__ + '.MAX_POINTS_PER_DAY', MAX_POINTS_TEST)
@mock.patch(tested_module.__name__ + '.MIN_LOOK_UP_DAYS', MIN_LOOK_UP_DAYS_TEST)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class VeryOldPointsTests(unittest.TestCase):
    '''we test three days batch'''
    end_date = date(1970, 1, 1)
    num_days = 2
    NEW_DATA = _make_test_data(DAY_1969_DEC_31 + DAY_1970_JAN_01)
    OLD_DATA = _make_test_data(DAY_1969_DEC_28 + DAY_1969_DEC_30)
    VERY_OLD_DATA = _make_test_data(DAY_1969_NOV_30 + DAY_1969_DEC_27)

    def test_do_not_choose_very_old_points(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=self.num_days)
        new_data = self.NEW_DATA[:MAX_POINTS_TEST]
        result = list(reducer(None, iter(self.OLD_DATA + new_data)))
        self.assertListEqual(result, sorted(self.OLD_DATA + new_data))

    def test_choose_last_very_old_points(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=self.num_days)
        data = self.NEW_DATA[:1] + self.OLD_DATA[:2]
        result = list(reducer(None, iter(data + self.VERY_OLD_DATA)))
        self.assertListEqual(result, sorted(data + sorted(self.VERY_OLD_DATA)[-2:]))

    def test_choose_all_very_old_points_if_num_points_not_enough(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=self.num_days)
        data = self.NEW_DATA[:2]
        result = list(reducer(None, iter(data + self.VERY_OLD_DATA[:2])))
        self.assertListEqual(result, sorted(data + sorted(self.VERY_OLD_DATA)[:2]))


@mock.patch(tested_module.__name__ + '.MAX_LOOK_UP_DAYS', 5)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class LongTimeAgoTest(unittest.TestCase):
    '''
    In this case checks long-time-ago points drop out
    '''
    end_date = date(1970, 1, 1)
    data = _make_test_data(DAY_1969_DEC_27 + DAY_1969_DEC_28)

    def test_long_time_ago_drop_out(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=1)
        result = list(reducer(None, iter(self.data[:])))
        timestamps = set([r['timestamp'] for r in result])
        self.assertTrue(timestamps.isdisjoint(set(DAY_1969_DEC_27)))

    def test_not_long_time_ago_points_in_result(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=1)
        result = list(reducer(None, iter(self.data[:])))
        timestamps = set([r['timestamp'] for r in result])
        self.assertEqual(timestamps, set(DAY_1969_DEC_28))


@mock.patch(tested_module.__name__ + '.MAX_LOOK_UP_DAYS', 5)
@mock.patch(tested_module.__name__ + '.FilterPointsReducer._filter_far_rows', side_effect=_fake_filter)
class FutureEventsTest(unittest.TestCase):
    '''
    In this case we checks fix timestamp for events from future
    '''
    end_date = date(1970, 1, 1)
    data = _make_test_data(FUTURE_EVENTS)

    def test_fix_future_events_timestamp(self, *mocks):
        reducer = tested_module.FilterPointsReducer(end_date=self.end_date,
                                                    num_suggested_days=1)
        result = list(reducer(None, iter(self.data[:])))
        timestamps = [r['timestamp'] for r in result]
        self.assertEqual(timestamps, [0] * len(FUTURE_EVENTS))


class FilterFarRowsTests(unittest.TestCase):
    '''
    One degree on equator is 111111 meters.
    In tests we get few close and few far points.
    We should return only close points.
    '''
    @staticmethod
    def _make_test_row(lat=0, lon=0, target_lat=0, target_lon=0):
        return {
            'lat': lat,
            'lon': lon,
            'target_lat': target_lat,
            'target_lon': target_lon,
        }

    def _run_and_check(self, data, expected):
        '''
        distance=500000 is less than 5 and greater than 4 degrees on equator
        '''
        result = tested_module.FilterPointsReducer._filter_far_rows(
            rows=data, distance=500000)
        self.assertListEqual(result, expected)

    def test_drop_on_equator_lon(self):
        close_points = [self._make_test_row(lon=4, lat=0)]
        far_points = [self._make_test_row(lon=5, lat=0)]
        self._run_and_check(close_points + far_points, close_points)

    def test_drop_on_equator_lat(self):
        close_points = [self._make_test_row(lon=0, lat=4)]
        far_points = [self._make_test_row(lon=0, lat=5)]
        self._run_and_check(close_points + far_points, close_points)

    def test_drop_on_lat60_lon(self):
        '''
        An 60 North we have two times shorter longitude degree
        '''
        close_points = [
            self._make_test_row(target_lon=0, target_lat=60, lon=8, lat=60),
        ]
        far_points = [
            self._make_test_row(target_lon=0, target_lat=60, lon=10, lat=60),
        ]
        self._run_and_check(close_points + far_points, close_points)

    def test_drop_on_lat60_lat(self):
        '''
        An 60 North we have two times shorter longitude degree
        '''
        close_points = [
            self._make_test_row(target_lon=0, target_lat=60, lon=0, lat=64),
        ]
        far_points = [
            self._make_test_row(target_lon=0, target_lat=60, lon=0, lat=65),
        ]
        self._run_and_check(close_points + far_points, close_points)

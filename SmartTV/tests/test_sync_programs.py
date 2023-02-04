from datetime import datetime
import mock

from smarttv.droideka.utils.caching_date import split_time_interval

# 'split_time_interval' uses 'smarttv.droideka.utils.caching_date.reduce_accuracy' which depends on the cache time
# if this time will suddenly changed, the values needs to be updated
# to avoid it, this constant will freeze caching time for tests
CACHE_TIME = 1


@mock.patch('smarttv.droideka.utils.caching_date.RATE', CACHE_TIME)
class TestSplitTimeInterval:
    start_time = datetime(2020, 4, 24, 0, 0, 0, 0)
    end_time = datetime(2020, 4, 24, 4, 0, 0, 0)

    def get_splited_test_data(self):
        return split_time_interval(self.start_time.timestamp(), self.end_time.timestamp(), hours=2)

    def test_split_4_hours_in_2_items(self):
        result = self.get_splited_test_data()
        assert len(result) == 2

    def test_split_4_hours_first_interval_is_correct(self):
        first_interval = self.get_splited_test_data()[0]

        assert datetime.fromtimestamp(first_interval[0]) == datetime(2020, 4, 24, 0, 0, 0)
        assert datetime.fromtimestamp(first_interval[1]) == datetime(2020, 4, 24, 1, 59, 59)

    def test_split_4_hours_second_interval_is_correct(self):
        first_interval = self.get_splited_test_data()[1]

        assert datetime.fromtimestamp(first_interval[0]) == datetime(2020, 4, 24, 2, 0, 0)
        assert datetime.fromtimestamp(first_interval[1]) == datetime(2020, 4, 24, 3, 59, 59)

import luigi.date_interval as di
from dwh.grocery.tools import *  # noqa: F401, F403


class TestYQLTask:

    def test_patched_di(self):
        month = di.Month.parse("2018-04")
        three_ago = month.prev(3)
        assert di.Month.parse("2018-01") == three_ago

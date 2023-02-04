from luigi.date_interval import Month
from dwh.grocery.task.yql_task import (
    MaybeYQLTablesRange,
    YTTableTarget,
)


class TestYQLTask:

    def test_make_range(self):
        month = Month.parse("1488-03")
        dates = month.dates()
        start, end = dates[0], dates[-1]
        tables_range = [YTTableTarget(f"/kek/{d}") for d in dates]
        r = MaybeYQLTablesRange(tables_range)
        assert r == MaybeYQLTablesRange("/kek", start.strftime("%Y-%m-%d"), end.strftime("%Y-%m-%d")), r

    def test_fail_to_make_range(self):
        r = MaybeYQLTablesRange([1, 2, 3])
        assert r == [1, 2, 3], r

    def test_format(self):
        month = Month.parse("1488-03")
        dates = month.dates()
        tables_range = [YTTableTarget(f"/kek/{d}") for d in dates]
        r = MaybeYQLTablesRange(tables_range)
        formatted = f"{r}"
        assert formatted == "range(`/kek`, `1488-03-01`, `1488-03-31`)", formatted

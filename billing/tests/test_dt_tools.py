import luigi.date_interval as di

from dwh.grocery.tools.datetime import months_in_range


class TestDatetimeTools:

    def test_months_in_range(self):
        test_months_str = [
            '2018-01',
            '2018-02',
            '2018-03',
            '2018-04',
            '2018-05',
            '2018-06',
            '2018-07',
        ]
        test_months = [di.Month.parse(m) for m in test_months_str]
        months = list(months_in_range(test_months[0], test_months[-1].next()))
        assert months == test_months

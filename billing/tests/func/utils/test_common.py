# coding: utf-8

from datetime import datetime

from billing.dcs.dcs.utils.common import add_workdays

from billing.dcs.tests import utils as test_utils


class AddWorkdaysTestCase(test_utils.BaseTestCase):
    def test_time_preserved(self):
        base_dt = datetime.now()
        new_dt = add_workdays(base_dt, 10)
        self.assertEqual(new_dt.time(), base_dt.time())

    def test_workday_add_zero_days(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 11), 0),
                         datetime(2017, 1, 11))

    def test_holiday_add_zero_days(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 14), 0),
                         datetime(2017, 1, 14))

    def test_workday_add_one_day_left(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 11), -1),
                         datetime(2017, 1, 11))

    def test_holiday_add_one_day_left(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 14), -1),
                         datetime(2017, 1, 13))

    def test_workday_add_one_day_right(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 11), 1),
                         datetime(2017, 1, 11))

    def test_holiday_add_one_day_right(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 14), 1),
                         datetime(2017, 1, 16))

    def test_workday_add_two_days_left(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 11), -2),
                         datetime(2017, 1, 10))

    def test_workday_add_two_days_right(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 11), 2),
                         datetime(2017, 1, 12))

    def test_same_result_regardless_base_day_type(self):
        self.assertEqual(add_workdays(datetime(2017, 1, 14), -2),
                         add_workdays(datetime(2017, 1, 13), -2))

    def test_end_of_month_offsets(self):
        """ CHECK-2321 """
        # Отсчитывать offset от первого дня следующего месяца неправильно
        # Если первый день следующего месяца выходной, получаем ожидаемый
        # результат (предпоследний рабочий день месяца)
        self.assertEqual(add_workdays(datetime(2017, 01, 01), -2),
                         datetime(2016, 12, 29))
        # Но если первый день следующего месяца рабочий, то он учитывается
        # в workdays offset, и мы получаем последний рабочий день месяца
        self.assertEqual(add_workdays(datetime(2017, 02, 01), -2),
                         datetime(2017, 1, 31))

        # Правильно: отсчитывать offset от последнего дня месяца
        # Всегда попадаем на предпоследний рабочий, независимо от типа
        # последнего дня месяца
        self.assertEqual(add_workdays(datetime(2017, 1, 31), -2),
                         datetime(2017, 1, 30))
        self.assertEqual(add_workdays(datetime(2016, 12, 31), -2),
                         datetime(2016, 12, 29))

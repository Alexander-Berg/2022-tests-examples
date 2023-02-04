from datetime import date

from django.test import TestCase
from mock import patch

from plan.holidays.calendar import days_of_month, workdays_of_month
from utils import Response, source_path


class HelpersTest(TestCase):

    def test(self):
        path = source_path(
            'intranet/plan/tests/test_data/calendar/holydays.xml'
        )
        holydays = open(path).read()
        days = days_of_month(2012, 3)

        with patch('plan.common.utils.http.Session.get') as request:
            request.return_value = Response(200, holydays)

            workdays = workdays_of_month(2012, 3)

        # 3 рабочих дня до конца месяца регулярно
        assert workdays[-4] == date(2012, 3, 27)
        # Последний рабочий день месяца
        assert workdays[-1] == date(2012, 3, 30)
        # Последний день месяца
        assert days[-1] == date(2012, 3, 31)
        # 1 число месяца регулярно
        assert days[0] == date(2012, 3, 1)
        # 4 число месяца регулярно
        assert days[3] == date(2012, 3, 4)
        # 5 число регулярно
        assert days[4] == date(2012, 3, 5)
        # 7 числа месяца регулярно
        assert days[6] == date(2012, 3, 7)
        # 8 число регулярно
        assert days[7] == date(2012, 3, 8)
        # 8 марта – выходной
        assert date(2012, 3, 8) not in workdays

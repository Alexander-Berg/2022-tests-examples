import unittest

from datetime import datetime
from analytics.plotter_lib.utils import date_range
from analytics.collections.plotter_collections.plots.active_users_retention import ActiveUsersReducer, BracketRetentionReducer
from nile.api.v1 import Record


def records_date_range(date, dateend, **kwargs):
    return [Record(fielddate=dt.strftime('%Y-%m-%d'), **kwargs) for dt in date_range(date, dateend)]


def assert_sorted_list_equal(list1, list2):
    assert sorted(list1) == sorted(list2)


class TestActiveUsersReducer(unittest.TestCase):
    def test_1(self):
        """Если пользователь сделал одно добавление 2020-01-01,
        то для period=7 возвращается набор записей с датами 2020-01-01 по 2020-01-08"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01')])]
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '7', 'user_type': 'test_user', 'actions': 1}
        answer = records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 7), **record_fields)

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 10))
        result = filter(lambda x: x.period == '7', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_2(self):
        """Если пользователь сделал два добавления 2020-01-01 и 2020-01-2020-01-06,
        то для period=7 возвращается набор записей с датами 2020-01-01 по 2020-01-12 +
        за 2020-01-06 будет возвращена запись с action_count=2"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06')])]
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '7', 'user_type': 'test_user', 'actions': 1}
        answer = (records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 5), **record_fields) +
                  records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 7), **dict(record_fields, actions=2)) +
                  records_date_range(datetime(2020, 1, 8), datetime(2020, 1, 12), **record_fields))
        answer += records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 7), **dict(record_fields, actions_count='2', actions=2))

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 13))
        result = filter(lambda x: x.period == '7', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_3(self):
        """Два пользователя: первый из теста 1, а второй из теста 2"""

        test_input_1 = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01')])]
        test_input_2 = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06')])]
        test_input = test_input_1 + test_input_2
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '7', 'user_type': 'test_user', 'actions': 1}
        answer_1 = records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 7), **record_fields)
        answer_2 = (records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 5), **record_fields) +
                    records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 7), **dict(record_fields, actions=2)) +
                    records_date_range(datetime(2020, 1, 8), datetime(2020, 1, 12), **record_fields))
        answer_2 += records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 7), **dict(record_fields, actions_count='2', actions=2))
        answer = answer_1 + answer_2

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 13))
        result = filter(lambda x: x.period == '7', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_4(self):
        """Кейс из второго теста с периодом 30"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06')])]
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '30', 'user_type': 'test_user', 'actions': 1}
        answer = (records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 5), **record_fields) +
                  records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 30), **dict(record_fields, actions=2)) +
                  records_date_range(datetime(2020, 1, 31), datetime(2020, 2, 4), **record_fields))
        answer += records_date_range(datetime(2020, 1, 6), datetime(2020, 1, 30), **dict(record_fields, actions_count='2', actions=2))

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 2, 6))
        result = filter(lambda x: x.period == '30', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_key_1(self):
        """Если пользователь сделал одно добавление 2020-01-01,
        то для period=7 возвращается набор записей с датами 2020-01-01 по 2020-01-08"""

        test_input = [(Record(user_type='test_user', ui='desktop'), [Record(fielddate='2020-01-01')])]
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '7', 'user_type': 'test_user', 'ui': 'desktop', 'actions': 1}
        answer = records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 7), **record_fields)

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 10), key_fields=['ui'])
        result = filter(lambda x: x.period == '7', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_key_2(self):
        """Если пользователь сделал одно добавление 2020-01-01,
        то для period=7 возвращается набор записей с датами 2020-01-01 по 2020-01-08"""

        test_input = [(Record(user_type='test_user', client_name='lego', client_ui='desktop'), [Record(fielddate='2020-01-01')])]
        record_fields = {'action_type': 'test', 'actions_count': '1', 'period': '7', 'user_type': 'test_user', 'client_name': 'lego', 'client_ui': 'desktop', 'actions': 1}
        answer = records_date_range(datetime(2020, 1, 1), datetime(2020, 1, 7), **record_fields)

        test_reducer = ActiveUsersReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 10), key_fields=['client_name', 'client_ui'])
        result = filter(lambda x: x.period == '7', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)


class TestBracketRetentionReducer(unittest.TestCase):
    def test_1(self):
        """Один заход"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01')])]
        answer = [Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', returned=0)]

        test_reducer = BracketRetentionReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 10))
        result = filter(lambda x: x.period == '7' and x.actions_count == '1', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_2(self):
        """два захода"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06')])]
        answer = [
            Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='1', fielddate='2020-01-06', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-01', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-06', period='7', user_type='test_user', returned=0)
        ]

        test_reducer = BracketRetentionReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 13))
        result = filter(lambda x: x.period == '7' and x.actions_count <= '2', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_3(self):
        """Три захода"""

        test_input = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06'), Record(fielddate='2020-01-07')])]
        answer = [
            Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='2', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='1', fielddate='2020-01-06', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='2', fielddate='2020-01-06', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='1', fielddate='2020-01-07', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-07', period='7', user_type='test_user', returned=0)
        ]

        test_reducer = BracketRetentionReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 13))
        result = filter(lambda x: x.period == '7' and x.actions_count <= '2', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_4(self):
        """Два пользователя: первый из теста 2, а второй из теста 3"""

        test_input_1 = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06')])]
        test_input_2 = [(Record(user_type='test_user'), [Record(fielddate='2020-01-01'), Record(fielddate='2020-01-06'), Record(fielddate='2020-01-07')])]
        test_input = test_input_1 + test_input_2

        answer_1 = [
            Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='1', fielddate='2020-01-06', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-01', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-06', period='7', user_type='test_user', returned=0)
        ]
        answer_2 = [
            Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='2', fielddate='2020-01-01', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='1', fielddate='2020-01-06', period='7', user_type='test_user', returned=1),
            Record(action_type='test', actions_count='2', fielddate='2020-01-06', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='1', fielddate='2020-01-07', period='7', user_type='test_user', returned=0),
            Record(action_type='test', actions_count='2', fielddate='2020-01-07', period='7', user_type='test_user', returned=0)
        ]
        answer = answer_1 + answer_2

        test_reducer = BracketRetentionReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 13))
        result = filter(lambda x: x.period == '7' and x.actions_count <= '2', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

    def test_key_1(self):
        """Один заход"""

        test_input = [(Record(user_type='test_user', ui='desktop'), [Record(fielddate='2020-01-01')])]
        answer = [Record(action_type='test', actions_count='1', fielddate='2020-01-01', period='7', user_type='test_user', ui='desktop', returned=0)]

        test_reducer = BracketRetentionReducer('test', True, datetime(2019, 1, 1), datetime(2020, 1, 10), key_fields=['ui'])
        result = filter(lambda x: x.period == '7' and x.actions_count == '1', test_reducer(test_input))

        assert_sorted_list_equal(result, answer)

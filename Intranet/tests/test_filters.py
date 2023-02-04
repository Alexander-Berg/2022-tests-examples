from builtins import object

from django_filters.filters import Filter, Lookup
from mock.mock import MagicMock, call

from kelvin.common.filters import ListFilter, precise_datetime_filter


def test_precise_date_updated_filter(mocker):
    """
    Тест `precise_date_updated_filter`
    """
    queryset = MagicMock()
    queryset.filter.return_value = 'filtered queryset'
    mocked = mocker.patch('kelvin.common.filters.dt_from_microseconds')
    mocked.return_value = 'datetime'

    # нормальное поведение
    assert precise_datetime_filter(queryset, 'date_updated', 'dt') == (
        'filtered queryset')
    assert queryset.mock_calls == [call.filter(date_updated='datetime')]
    assert mocked.mock_calls == [call('dt')]

    # неправильное значение, приводящее к исключению
    mocked.reset_mock()
    queryset.reset_mock()
    mocked.side_effect = TypeError
    assert precise_datetime_filter(queryset, 'date_updated', 'dt') is queryset
    assert queryset.mock_calls == []
    assert mocked.mock_calls == [call('dt')]


class TestListFilter(object):
    """Тесты списочного фильтра"""
    tested_filter = ListFilter(field_name='testarg')

    def test_filter(self, mocker):
        """Тесты фильтрации"""
        mocked_super_filter = mocker.patch.object(Filter, 'filter')
        mocked_super_filter.return_value = '<filtered_qs>'

        # Нормальная фильтрация
        assert self.tested_filter.filter('<qs>', 'val') == '<filtered_qs>'
        assert mocked_super_filter.mock_calls == [
            call('<qs>', Lookup(value=['val'], lookup_type='in'))
        ]

        # Ошибка в фильтрации
        mocked_super_filter.reset_mock()
        mocked_super_filter.side_effect = ValueError

        assert self.tested_filter.filter('<qs>', 'val1,val2') == '<qs>'
        assert mocked_super_filter.mock_calls == [
            call('<qs>', Lookup(value=['val1', 'val2'], lookup_type='in'))
        ]

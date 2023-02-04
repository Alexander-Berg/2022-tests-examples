from builtins import object

import pytest
from mock import MagicMock, call

from rest_framework import status, viewsets

from kelvin.group_levels.models import GroupLevel
from kelvin.group_levels.views import GroupLevelView


class TestGroupLevelView(object):
    """
    Тесты ручки просмотра уровней подгрупп
    """
    test_list_cases = (
        (
            [
                GroupLevel(id=1, baselevel=1,
                           name=u"testgroup 2", slug='slug1'),
                GroupLevel(id=2, baselevel=1,
                           name=u"testgroup 1", slug='slug2'),
            ],
            [2, 1],
        ),
        (
            [
                GroupLevel(id=1, baselevel=2,
                           name=u"testgroup 1", slug='slug1'),
                GroupLevel(id=2, baselevel=1,
                           name=u"testgroup 2", slug='slug2'),
            ],
            [2, 1],
        ),
        (
            [
                GroupLevel(id=1, baselevel=1,
                           name=u"3класс", slug='slug1'),
                GroupLevel(id=2, baselevel=1,
                           name=u"1 класс", slug='slug2'),
                GroupLevel(id=3, baselevel=1,
                           name=u"2 класс", slug='slug3'),
                GroupLevel(id=4, baselevel=2,
                           name=u"0 класс", slug='slug4'),
            ],
            [2, 3, 1, 4],
        ),
    )

    @pytest.mark.parametrize('data,expected_id_order', test_list_cases)
    def test_get_data(self, mocker, data, expected_id_order):
        """
        Тест получения данных для списка уровней групп
        """
        # Мокаем содержащиеся в базе уровни групп, а также упрощаем их
        # представление сериализатором
        mocked_get_queryset = mocker.patch.object(viewsets.GenericViewSet,
                                                  'get_queryset')
        mocked_get_queryset.return_value = data
        mocked_filter_queryset = mocker.patch.object(viewsets.GenericViewSet,
                                                     'filter_queryset')
        mocked_filter_queryset.return_value = data
        mocker.patch.object(GroupLevelView.serializer_class,
                            'to_representation',
                            lambda self, obj: obj.id)

        view = GroupLevelView()
        view.request = MagicMock()
        view.format_kwarg = {}

        data = view._get_data()
        assert mocked_get_queryset.called, (
            u"Не было вызова `GroupLevelView.get_queryset`"
        )
        assert data == expected_id_order, (
            u"Неправильные данные ответа. "
            u"Ожидалось: {0}, получено: {1}".format(expected_id_order, data)
        )

    def test_list(self, mocker):
        """
        Тест просмотра кешированного списка уровней групп
        """
        mocked_cache = mocker.patch('kelvin.group_levels.views.cache')
        mocked_cache.get.return_value = None

        mocked_get_data = mocker.patch.object(GroupLevelView, '_get_data')
        mocked_get_data.return_value = 'some data'

        view = GroupLevelView()
        view.request = MagicMock()
        view.format_kwarg = {}

        # в кеше ничего нет
        response = view.list('sample request')
        assert response.status_code == status.HTTP_200_OK
        assert mocked_get_data.called
        assert mocked_cache.mock_calls == [
            call.get('kelvin.group_levels.list.'),
            call.set('kelvin.group_levels.list.', 'some data', 1800),
        ]

        # в кеше есть данные ручки
        mocked_cache.reset_mock()
        mocked_get_data.reset_mock()
        mocked_cache.get.return_value = 'some data'
        response = view.list('sample request')
        assert response.status_code == status.HTTP_200_OK
        assert mocked_cache.mock_calls == [
            call.get('kelvin.group_levels.list.'),
        ]
        assert not mocked_get_data.called

# coding: utf8
from builtins import object

import pytest
from mock import MagicMock

from kelvin.resources.models import Resource
from kelvin.resources.views import ResourceViewSet


class TestResourceViewSet(object):
    """
    Тесты для API `ResourceViewSet`
    """
    get_queryset_cases = [
        ('GET', True),
        ('POST', False),
        ('PUT', False),
        ('HEAD', True),
        ('DELETE', False),
        ('PATCH', False),
        ('OPTIONS', True),
    ]

    @pytest.mark.parametrize('method,expected', get_queryset_cases)
    def test_get_queryset(self, method, expected, mocker):
        """
        Тестирование метода `get_queryset`
        """
        mocked_objects = mocker.patch.object(Resource, 'objects')
        mocked_objects.all.return_value.prefetch_related.return_value = [1]
        request = MagicMock()
        request.method = method
        view = ResourceViewSet()
        view.request = request
        view.get_queryset()
        assert mocked_objects.all.called
        assert (mocked_objects.all.return_value.prefetch_related.called == expected)

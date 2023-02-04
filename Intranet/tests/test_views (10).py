from builtins import object

import pytest
from mock import MagicMock

from kelvin.problem_meta.models import ProblemMeta
from kelvin.problem_meta.views import ProblemMetaViewSet


class TestProblemMetaViewSet(object):
    """
    Тестирование `ProblemMetaViewSet`
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
        mocked_objects = mocker.patch.object(ProblemMeta, 'objects')
        mocked_objects.all.return_value.prefetch_related.return_value = [1]
        request = MagicMock()
        request.method = method
        view = ProblemMetaViewSet()
        view.request = request
        view.get_queryset()
        assert mocked_objects.all.called
        assert (mocked_objects.all.return_value.prefetch_related.called == expected)

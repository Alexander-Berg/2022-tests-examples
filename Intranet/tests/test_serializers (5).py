import datetime
from builtins import object

import pytest
from mock import MagicMock

from kelvin.problem_meta.models import ProblemMeta
from kelvin.problem_meta.serializers import ProblemMetaExpansionAwareSerializer, ProblemMetaSerializer
from kelvin.subjects.models import Theme


class TestProblemMetaExpansionAwareSerializer(object):
    """
    Тесты вложенного сериализатора метаинформации задачи
    """
    to_representation_short_cases = (
        (
            ProblemMeta(
                id=1,
                date_updated=datetime.datetime(1970, 2, 1, 0, 1),
            ),
            {
                'id': 1,
                'date_updated': 2678460,
            },
        ),
        (
            ProblemMeta(
                id=2,
                date_updated=datetime.datetime(2015, 6, 1, 1, 1),
                main_theme=Theme(id=1),

            ),
            {
                'id': 2,
                'date_updated': 1433120460,
            },
        ),
    )

    @pytest.mark.parametrize("problem_meta,expected",
                             to_representation_short_cases)
    def test_to_representation_short(self, problem_meta, expected):
        """
        Тест короткого представления в
        `ProblemMetaExpansionAwareSerializer.to_representation`
        """
        # первый сценарий - запроса нет
        serializer = ProblemMetaExpansionAwareSerializer()
        represented = serializer.to_representation(problem_meta)

        assert represented == expected, (
            u"Неверное представление. Ожидалось {0}, получено {1}".format(
                expected, represented)
        )

        # второй сценарий - есть запрос, он не содержит expand_meta
        serializer = ProblemMetaExpansionAwareSerializer()
        mocked_request = MagicMock()
        mocked_request.query_params.get.return_value = None
        serializer._context = {'request': mocked_request}

        represented = serializer.to_representation(problem_meta)

        assert represented == expected, (
            u"Неверное представление. Ожидалось {0}, получено {1}".format(
                expected, represented)
        )
        assert mocked_request.query_params.get.called, (
            u"Не было обращения к параметрам запроса")

    def test_to_representation_full(self, mocker):
        """
        Тест полного представления в
        `ProblemMetaExpansionAwareSerializer.to_representation`
        """
        # Проверим, что вызывается `ProblemMetaSerializer` без
        # аргументов, если есть запрос и есть expand_meta в нем
        expected = {'test': 'successful'}
        mocked_representation = mocker.patch.object(ProblemMetaSerializer,
                                                    'to_representation')
        mocked_representation.return_value = expected
        serializer = ProblemMetaExpansionAwareSerializer()
        mocked_request = MagicMock()
        mocked_request.query_params = {'expand_meta': True}
        serializer._context = {'request': mocked_request}

        represented = serializer.to_representation(ProblemMeta(id=1))

        assert represented == expected, (
            u"Неверное представление. Ожидалось {0}, получено {1}".format(
                expected, represented)
        )
        assert mocked_representation.called, (
            u'Не было вызова `ProblemMetaSerializer.to_representation`')

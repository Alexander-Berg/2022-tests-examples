from builtins import object

import pytest

from kelvin.problems.markers import BaseMarker, ChoiceMarker, InlineMarker, Marker


class TestMarker(object):
    """
    Тесты класса инициализации маркера, дефолтного маркера
    """
    new_data = (
        ({'type': 'inline'}, InlineMarker),
        ({'type': 'choice', 'options': {}}, ChoiceMarker),
        ({'type': 'unknown type'}, Marker),
    )

    @pytest.mark.parametrize('marker_data,klass', new_data)
    def test_new(self, marker_data, klass):
        """
        Проверяем, что создается нужный класс маркера
        """
        marker = Marker(marker_data, None)
        assert isinstance(marker, klass), (
            u'Неправильный класс созданного маркера')

    check_data = (
        ('1', (BaseMarker.CORRECT, 0)),
        ('1.0', (BaseMarker.INCORRECT, 1)),
        (None, (BaseMarker.SKIPPED, 1)),
    )

    @pytest.mark.parametrize('answer,expected', check_data)
    def test_check(self, answer, expected):
        """
        Проверка ответа состоит в простом сравнении с данными из маркера
        """
        marker = Marker({'type': 'unknown type'}, '1')
        assert marker.check(answer) == expected, (
            u'Неправильно проверили маркер')

    def test_max_attempts(self):
        """Тест максимального числа ошибок"""
        assert Marker({'type': 'any'}, None).max_mistakes == 1, (
            u'По умолчанию 1 ошибка')

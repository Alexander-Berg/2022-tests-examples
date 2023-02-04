from builtins import object

import jsonschema
import pytest
from mock import call

from django.core.exceptions import ValidationError

from kelvin.problems.markers import BaseMarker


class TestBaseMarker(object):
    """
    Тесты базового класса маркеров
    """

    def test_init(self):
        """
        Проверяем, что надо передавать параметр
        """
        with pytest.raises(TypeError):
            BaseMarker()
        with pytest.raises(TypeError):
            BaseMarker(None)

        marker = BaseMarker('1', '2')
        assert hasattr(marker, 'data'), u'Маркер должен иметь атрибут `data`'
        assert hasattr(marker, 'answer'), (
            u'Маркер должен иметь атрибут `answer`')
        assert hasattr(marker, 'checks'), (
            u'Маркер должен иметь атрибут `checks`')
        assert marker.data == '1', u'Неправильное значение данных в маркере'
        assert marker.answer == '2', (
            u'Неправильное значение данных в ответе маркета')
        assert marker.checks is None, (
            u'Неправильное значение данных в проверках маркера')

        marker = BaseMarker('1', '2', '3')
        assert hasattr(marker, 'data'), u'Маркер должен иметь атрибут `data`'
        assert hasattr(marker, 'answer'), (
            u'Маркер должен иметь атрибут `answer`')
        assert hasattr(marker, 'checks'), (
            u'Маркер должен иметь атрибут `checks`')
        assert marker.data == '1', u'Неправильное значение данных в маркере'
        assert marker.answer == '2', (
            u'Неправильное значение данных в ответе маркета')
        assert marker.checks == '3', (
            u'Неправильное значение данных в проверках маркера')

    def test_check_implemented(self):
        """
        Проверяем, что надо реализовать метод
        """
        marker = BaseMarker({}, None)
        with pytest.raises(NotImplementedError):
            marker.check('1')

    def test_get_embedded_objects(self):
        """
        Проверяем нахождение формул и ресурсов в разметке
        """
        marker = BaseMarker({
            'type': 'choice',
            'options': {
                'choices': [
                    u'{formula:1}',
                    u'{resource:2}',
                ],
            },
        }, [1])
        assert marker.get_embedded_objects() == [], (
            u'По умолчанию должен быть пустой список')

    def test_is_skipped(self):
        """
        Проверяем, что `None` считается пропущенным ответом
        """
        marker = BaseMarker({'data': 1}, None)
        assert marker.is_skipped(None) is True
        assert marker.is_skipped(False) is False

    def test_validate_answer(self, mocker):
        """
        Проверяет, что валидация ответа происходит по схеме
        """
        mocked_validate = mocker.patch('jsonschema.validate')
        mocked_validate.return_value = 2

        marker = BaseMarker({'data': 2}, None)
        try:
            marker.validate_answer('answer')
        except ValidationError:
            pytest.fail(u'Не должно быть исключения')
        assert mocked_validate.mock_calls == [call('answer', {})], (
            u'Должна быть вызвана валидация json-схемы')

        mocked_validate.reset_mock()
        mocked_validate.side_effect = jsonschema.ValidationError('error')
        marker = BaseMarker({'data': 3}, None)
        with pytest.raises(ValidationError) as excinfo:
            marker.validate_answer('answer')
        assert excinfo.value.message == 'error'
        assert mocked_validate.mock_calls == [call('answer', {})], (
            u'Должна быть вызвана валидация json-схемы')

    def test_max_attempts(self):
        """Тест максимального числа ошибок"""
        with pytest.raises(NotImplementedError):
            BaseMarker({'data': 1}, None).max_mistakes

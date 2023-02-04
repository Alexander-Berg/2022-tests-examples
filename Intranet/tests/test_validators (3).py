from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.common.validators import validate_color


class TestValidateColor(object):
    """
    тесты функции `validate_color`
    """
    correct_colors = (
        '#abcdef', '#1a2b3c', '#000000', '#ffffff',
    )
    incorrect_colors = (
        '#abcdeg', '#1abcdef', '#1abcd', 'abcdef', 'abgdef',
        '', 'abcdefa', '###abcd',
    )

    @pytest.mark.parametrize("color", correct_colors)
    def test_validate_color_positive(self, color):
        """
        Проверяем, что валидация правильного цвета не вызывает эксепшенов
        """
        try:
            validate_color(color)
        except ValidationError:
            pytest.fail(u"Ошибка валидации на правильном цвете")

    @pytest.mark.parametrize("color", incorrect_colors)
    def test_validate_color_negative(self, color):
        """
        Проверяем, что валидация неправильного цвета вызывает нужный эксепшен
        """
        with pytest.raises(ValidationError) as excinfo:
            validate_color(color)

        assert excinfo.value.message == "Incorrect color: {0}".format(color)

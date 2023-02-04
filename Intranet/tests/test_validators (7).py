from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.staff_notifications.validators import validate_code


class TestCodeValidator(object):
    """
    Тесты проверки валидности кода
    """
    valid_codes = (
        'abcdefghjk',
        'j012345678',
    )

    @pytest.mark.parametrize('code', valid_codes)
    def test_code_validation_positive(self, code):
        """
        Тест проверки валидных кодов
        """
        validate_code(code)

    invalid_codes = (
        '012345678',
        '01234567899',
        u'фabcdefghi',
        'J012345678',
    )

    @pytest.mark.parametrize('code', invalid_codes)
    def test_code_validation_negative(self, code):
        """
        Тест проверки невалидных кодов
        """
        with pytest.raises(ValidationError):
            validate_code(code)

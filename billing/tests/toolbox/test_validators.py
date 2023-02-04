import pytest

from bcl.toolbox import validators


class TestNDocDateValidator:
    """Валидатор поля "Показатель даты документа"
    """
    def test_validate(self):
        # arrange
        validator = validators.NDocDateValidator()

        # act

        date_0 = validator('0')
        date_ddmmyyyy = validator('10122017')

        # assert
        assert date_0 == '0'
        assert date_ddmmyyyy == '10122017'

        with pytest.raises(validators.ValidationError):
            validator('2')

        with pytest.raises(validators.ValidationError):
            validator('20171012')
import pytest

from staff.achievery.request import validate_one


@pytest.mark.parametrize(
    'value, result',
    [
        ('TRUE', True),
        ('true', True),
        ('FALSE', False),
        ('false', False),
        ('TRUEuser', 'TRUEuser'),
        ('trueuser', 'trueuser'),
        ('FALSEuser', 'FALSEuser'),
        ('falseuser', 'falseuser'),
        ('null', None),
        ('nulluser', 'nulluser'),
    ]
)
def test_validate_one_reserved_words(value, result):
    assert validate_one(value) == result

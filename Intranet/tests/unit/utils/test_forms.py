import pytest

from django.core.exceptions import ValidationError

from intranet.femida.src.utils.forms.fields import CommaSeparatedIntegerField


comma_separated_integer_field_test_cases = [
    ('1', True),
    ('1,2,3,4,5', True),
    ('1,2,', False),
    ('1,2,x', False),
]


@pytest.mark.parametrize('test_case', comma_separated_integer_field_test_cases)
def test_comma_separated_integer_field(test_case):
    value, result = test_case
    f = CommaSeparatedIntegerField()
    try:
        f.clean(value)
        assert result
    except ValidationError:
        assert not result

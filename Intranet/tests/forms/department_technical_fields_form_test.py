import pytest
import sform
from django.core.exceptions import ValidationError

from staff.proposal.forms.department import DepartmentTechnicalFields


def test_code_field_state():
    form = DepartmentTechnicalFields()
    assert form.get_field_state('code') == sform.READONLY

    form = DepartmentTechnicalFields(base_initial={'creating_department': True})
    assert form.get_field_state('code') == sform.REQUIRED


code_test_values = [
    ('my_code', None, 'invalid'),
    ('my-code', None, 'invalid'),
    ('mycode', 'mycode', None),
    ('MyCode', 'mycode', None),
    ('1234321', '1234321', None),
    ('longlonglong' * 5, None, 'invalid'),
    ('', None, 'invalid'),
    ('русскийкод', None, 'invalid'),
]


@pytest.mark.parametrize('code_value, result, error_code', code_test_values)
def test_code_validation(code_value, result, error_code):
    if error_code:
        with pytest.raises(ValidationError) as exc:
            DepartmentTechnicalFields.clean_code(code_value)
            assert exc.value.code == 'invalid'
            assert exc.value.message == 'Code value does not validate'
    else:
        assert DepartmentTechnicalFields.clean_code(code_value) == result


def test_default_order():
    assert DepartmentTechnicalFields.clean_order(1234) == 1234
    assert DepartmentTechnicalFields.clean_order(0) == 0
    assert DepartmentTechnicalFields.clean_order(None) == 0

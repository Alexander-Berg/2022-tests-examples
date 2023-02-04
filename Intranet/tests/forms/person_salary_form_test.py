from staff.payment.enums import WAGE_SYSTEM

from staff.proposal.forms.person import PersonSalaryForm


BASE_FORM_DATA = {
    'old_currency': 'USD',
    'new_currency': 'RUB',

    'old_salary': 321,
    'old_wage_system': WAGE_SYSTEM.FIXED,
    'old_rate': 0.5,

    'new_salary': 123,
    'new_wage_system': WAGE_SYSTEM.FIXED,
    'new_rate': 1,
}


def test_salary_rate_required_on_fixed_wage():
    data = BASE_FORM_DATA.copy()
    data['new_rate'] = None
    form = PersonSalaryForm(data=data)
    assert not form.is_valid()
    assert form.errors['errors'] == {
        'new_rate': [{'code': 'required'}, ]
    }


def test_salary_rate_is_not_required_on_other_wage():
    data = BASE_FORM_DATA.copy()
    data['new_wage_system'] = WAGE_SYSTEM.PIECEWORK
    data['old_wage_system'] = WAGE_SYSTEM.HOURLY

    data['old_rate'] = None
    data['new_rate'] = None

    form = PersonSalaryForm(data=data)
    assert form.is_valid()

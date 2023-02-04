from datetime import datetime
import json

from mock import patch
import pytest

from django.core.urlresolvers import reverse

from staff.person.models import Occupation
from staff.budget_position.views.tableflow_views import validate_rules
from staff.budget_position.forms.tableflow_validation_forms import (
    RewardRulesValidationForm,
    ReviewGroupRulesValidationForm,
    BonusRulesValidationForm,
    ReviewRulesValidationForm,
)


@pytest.mark.django_db
def test_rewards_rules_validation(company):
    # given
    data = {'cases': [
        {
            'out': {'priority': '50', 'reward_scheme_id': '8'},
            'checks': {
                'category': {'eq': 'Professionals'},
                'department_url': {'contains': ['yandex', 'yandex_dep1', 'yandex_dep2']},
                'is_internship': {'eq': '0'},
            },
        },
        {
            'out': {'priority': '50', 'reward_scheme_id': '8'},
            'checks': {
                'category': {'eq': 'Professionals'},
                'department_url': {'eq': 'yandex'},
                'is_internship': {'eq': '0'},
            }
        },
        {
            'out': {'priority': '50', 'reward_scheme_id': '8'},
            'checks': {
                'category': {'eq': 'Professionals'},
                'department_url': {'ne': 'Any'},
                'is_internship': {'eq': '0'},
            }
        },
    ]}

    form = RewardRulesValidationForm(data=data)

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_review_group_rules_validation(company):
    # given
    data = {'cases': [
        {
            'out': {'priority': '10', 'review_id': '20'},
            'checks': {
                'review_group': {'eq': 'Sales_review'},
                'department_url': {'eq': 'yandex'},
                'grade_level': {'ne': 'Any'},
            }
        },
        {
            'out': {'priority': '0', 'review_id': '20'},
            'checks': {
                'review_group': {'eq': 'Sales_review'},
                'department_url': {'ne': 'Any'},
                'grade_level': {'ne': 'Any'},
            }
        },
    ]}

    form = ReviewGroupRulesValidationForm(data=data)

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_bonus_rules_validation(company):
    # given
    Occupation.objects.create(name='sfs', created_at=datetime.now(), modified_at=datetime.now())

    data = {
        'cases': [
            {
                'out': {'bonus_id': '0', 'priority': '0'},
                'checks': {
                    'grade_level': {'ge': 0},
                    'department_url': {'eq': 'yandex'},
                    'occupation_code': {'eq': 'sfs'}
                }
            },
            {
                'out': {'bonus_id': '0', 'priority': '0'},
                'checks': {
                    'grade_level': {'lt': 0},
                    'department_url': {'ne': 'Any'},
                    'occupation_code': {'eq': 'sfs'}
                }
            },
        ],
        'in_fields': {
            'grade_level': {'field_type': 'int'},
            'department_url': {'field_type': 'str'},
            'occupation_code': {'field_type': 'str'},
        },
        'out_fields': {'bonus_id': {'field_type': 'str'}, 'priority': {'field_type': 'str'}},
    }

    form = BonusRulesValidationForm(data=data)

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_review_rules_validation(company):
    # given
    Occupation.objects.create(name='SMM', created_at=datetime.now(), modified_at=datetime.now())
    data = {'cases': [
        {
            'out': {'priority': '0', 'review_id': '0'},
            'checks': {
                'department_url': {'eq': 'yandex'},
                'occupation_code': {'eq': 'SMM'},
                'grade_level': {'ne': 'Any'},
            }
        },
        {
            'out': {'priority': '0', 'review_id': '0'},
            'checks': {
                'department_url': {'ne': 'any'},
                'occupation_code': {'eq': 'SMM'},
                'grade_level': {'ne': 'Any'},
            }
        },
    ]}

    form = ReviewRulesValidationForm(data=data)

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_occupation_field_can_handle_invalid_values(company):
    # given
    data = {'cases': [
        {
            'out': {'priority': '0', 'review_id': '0'},
            'checks': {
                'department_url': {'eq': 'yandex'},
                'occupation_code': {'eq': 'SMM1'}
            }
        },
    ]}

    form = ReviewRulesValidationForm(data=data)

    # when
    result = form.is_valid()

    # then
    assert not result


@pytest.mark.django_db
def test_human_readable_error_for_absent_value(company):
    # given
    data = {'cases': [
        {
            'out': {'priority': '0', 'review_id': '0'},
            'checks': {
                'department_url': {'eq': 'yandex'},
                'occupation_code': {'eq': ''}
            }
        },
    ]}

    form = ReviewRulesValidationForm(data=data)

    # when
    form.is_valid()
    result = form.human_readable_errors()

    # then
    assert result[0] == 'Required value at row 0 at field occupation_code'


@pytest.mark.django_db
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_view_error(company, rf):
    # given
    data = {'cases': [
        {
            'out': {'priority': '0', 'review_id': '0'},
            'checks': {
                'department_url': {'eq': 'yandex1'},
                'occupation_code': {'eq': 'SMM'}
            }
        },
    ]}

    url = reverse('budget-position-api:validate-rules', kwargs={'rule': 'review_rules'})
    request = rf.post(url, data=json.dumps(data), content_type='application/json')
    request.user = None
    request.yauser = None

    # when
    response = validate_rules(request, 'review_rules')

    # then
    assert response.status_code == 400
    errors = json.loads(response.content)

    assert len(errors['messages']) > 0

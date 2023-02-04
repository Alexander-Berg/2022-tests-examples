import pytest
from mock import patch, MagicMock

import json
import random
from datetime import datetime

from django.core.urlresolvers import reverse

from staff.lib.auth.auth_mechanisms import TVM
from staff.person.models import Occupation

from staff.budget_position.const import FemidaProfessionalLevel
from staff.budget_position.views.femida_views import (
    bonus_scheme_view,
    check_if_hr_product_changed_view,
    review_scheme_view,
    reward_scheme_view,
)
from staff.budget_position.workflow_service.gateways import TableflowService, StaffService, OEBSService
from staff.budget_position.workflow_service.entities import (
    BonusSchemeRow,
    BonusSchemeDetails,
    Change,
    InsuranceDetails,
    MONTHS_PER_YEAR,
    ReviewSchemeDetails,
    RewardSchemeDetails,
)


@pytest.mark.django_db()
@patch.object(TableflowService, 'bonus_scheme_id')
@patch.object(StaffService, 'bonus_scheme_details')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_bonus_scheme_view(mocked_staff, mocked_tableflow, rf, company, femida_user):
    # given
    bonus = random.random()
    details = BonusSchemeDetails(
        scheme_id=0,
        name='some',
        description='Стандартная',
        scheme_rows=[
            BonusSchemeRow(
                -1,
                value_type='ыыы',
                value_source='Значением',
                value=random.random(),
            ),
            BonusSchemeRow(
                -1,
                value_type='Процент от оклада',
                value_source='Значением',
                value=random.random(),
            ),
            BonusSchemeRow(
                -1,
                value_type='Процент от оклада',
                value_source='Значением',
                value=random.random(),
            ),
            BonusSchemeRow(
                -1,
                value_type='Процент от оклада',
                value_source='ss',
                value=random.random(),
            ),
        ],
        non_review_bonus=bonus,
    )

    mocked_staff.return_value = details
    mocked_tableflow.return_value = [100500]

    occupation = Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    request = rf.get(
        reverse('budget-position-api:bonus-scheme'),
        {'department': company.yandex.id, 'grade_level': 16, 'occupation': occupation.pk},
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = bonus_scheme_view(request)

    # then
    assert response.status_code == 200, response.content
    data = json.loads(response.content)
    assert data['scheme_id'] == details.scheme_id
    assert data['name'] == details.name
    assert data['description'] == details.description
    assert data['non_review_bonus'] == bonus


@pytest.mark.django_db()
@patch.object(TableflowService, 'review_scheme_id')
@patch.object(StaffService, 'review_scheme_details')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_review_scheme_view(mocked_staff, mocked_tableflow, rf, company, femida_user):
    # given
    mocked_tableflow.return_value = [100500]
    details = ReviewSchemeDetails(
        scheme_id=0,
        name='Медиасервисы/Дзен sales',
        description='',
        schemes_line_id=103,
        schemes_line_description='актуальная 4/6',
        target_bonus=2.,
        grant_type='RSU',
        grant_type_description='RSU Yandex NV',
    )
    mocked_staff.return_value = details

    occupation = Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    form_data = {'department': company.yandex.id, 'occupation': occupation.pk, 'grade_level': 16}
    request = rf.get(
        reverse('budget-position-api:review-scheme'), form_data,
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = review_scheme_view(request)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['has_review']
    assert data['review_bonus'] == details.target_bonus * MONTHS_PER_YEAR


@pytest.mark.django_db()
@patch.object(TableflowService, 'reward_category')
@patch.object(TableflowService, 'reward_scheme_id')
@patch.object(StaffService, 'reward_scheme_details')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_reward_scheme_view(mocked_staff, mocked_reward_scheme_id, mocked_reward_category, rf, femida_user, company):
    # given
    mocked_reward_scheme_id.return_value = [100500]
    mocked_reward_category.return_value = ['some_test_cat']
    mocked_staff.return_value = RewardSchemeDetails(
        scheme_id=0,
        name='test',
        description='',
        schemes_line_id=3,
        category='Professionals',
        food='Стандартное',
        dms=[InsuranceDetails(name='1', type='test', ya_insurance=True)],
        dms_group='Стандартный',
        bank_cards=[],
        ai=[],
    )

    occupation = Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    request = rf.get(
        reverse('budget-position-api:reward-scheme'),
        {'grade_level': 16, 'occupation': occupation.pk, 'department': company.yandex.id},
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = reward_scheme_view(request)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['has_food_compensation']
    assert not data['category_changed']


@pytest.mark.django_db()
@patch.object(TableflowService, 'reward_category')
@patch.object(TableflowService, 'reward_scheme_id')
@patch.object(StaffService, 'reward_scheme_details')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_reward_scheme_view_works_with_femida_professional_level(
    mocked_staff,
    mocked_reward_scheme_id,
    mocked_reward_category,
    rf,
    femida_user,
    company,
):
    # given
    mocked_reward_scheme_id.return_value = [100500]
    mocked_reward_category.return_value = ['some_test_cat']
    mocked_staff.return_value = RewardSchemeDetails(
        scheme_id=0,
        name='test',
        description='',
        schemes_line_id=3,
        category='Professionals',
        food='Стандартное',
        dms=[InsuranceDetails(name='1', type='test', ya_insurance=True)],
        dms_group='Стандартный',
        bank_cards=[],
        ai=[],
    )

    occupation = Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    form_data = {
        'occupation': occupation.pk,
        'department': company.yandex.id,
        'professional_level': FemidaProfessionalLevel.middle.value,
    }
    request = rf.get(reverse('budget-position-api:reward-scheme'), form_data)
    request.user = femida_user
    request.yauser = None

    # when
    response = reward_scheme_view(request)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['has_food_compensation']
    assert not data['category_changed']


@pytest.mark.django_db()
@patch.object(OEBSService, 'get_position_as_change')
@patch.object(TableflowService, 'reward_category')
@patch.object(TableflowService, 'reward_scheme_id')
@patch.object(StaffService, 'reward_scheme_details')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_reward_scheme_view_can_detect_reward_scheme_change(
    mocked_staff,
    mocked_reward_scheme_id,
    mocked_reward_category,
    mocked_oebs,
    rf,
    femida_user,
    company,
):
    # given
    new_reward_scheme_id = 10
    mocked_oebs.return_value = Change(
        reward_scheme_id=new_reward_scheme_id,
    )
    mocked_reward_scheme_id.return_value = [100500]
    mocked_reward_category.return_value = ['some_test_cat']
    mocked_staff.side_effect = [
        RewardSchemeDetails(
            scheme_id=0,
            name='test',
            description='',
            schemes_line_id=3,
            category='Professionals',
            food='Стандартное',
            dms=[InsuranceDetails(name='1', type='test', ya_insurance=True)],
            dms_group='Стандартный',
            bank_cards=[],
            ai=[InsuranceDetails(name='Без НС', type='', ya_insurance=False)],
        ),
        RewardSchemeDetails(
            scheme_id=new_reward_scheme_id,
            name='test2',
            description='',
            schemes_line_id=3,
            category='Mass positions',
            food='Стандартное',
            dms=[InsuranceDetails(name='1', type='test', ya_insurance=True)],
            dms_group='Стандартный',
            bank_cards=[],
            ai=[InsuranceDetails(name='Без НС', type='', ya_insurance=False)],
        ),
    ]

    occupation = Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    request = rf.get(
        reverse('budget-position-api:reward-scheme'),
        {'grade_level': 16, 'occupation': occupation.pk, 'department': company.yandex.id, 'budget_position_code': 100},
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = reward_scheme_view(request)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['category_changed']


def test_check_if_hr_product_changed_view_not_valid(rf):
    test_data = {'test data': random.random()}
    request = _create_request(rf, test_data)
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random.random()}

    with patch('staff.budget_position.views.femida_views.CheckIfHrProductChangedForm', form_class):
        response = check_if_hr_product_changed_view(request)
        form_class.assert_called_once_with(data=request.GET)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


def test_check_if_hr_product_changed_view(rf):
    test_data = {'test data': random.random()}
    request = _create_request(rf, test_data)
    cleaned_data = {
        'budget_position': random.random(),
        'hr_product': random.random(),
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    controller = MagicMock(return_value={'test response': random.random()})

    with patch('staff.budget_position.views.femida_views.CheckIfHrProductChangedForm', form_class):
        with patch('staff.budget_position.views.femida_views.check_if_hr_product_changed_controller', controller):
            response = check_if_hr_product_changed_view(request)
            form_class.assert_called_once_with(data=request.GET)
            controller.asert_called_once_with(cleaned_data['budget_position'], cleaned_data.get('hr_product'))

    assert response.status_code == 200, response.content
    assert json.loads(response.content) == controller.return_value


def _create_request(rf, test_data: dict):
    request = rf.get(reverse('budget-position-api:check-if-hr-product-changed'), **test_data)
    request.auth_mechanism = TVM
    request.yauser = None
    request.user = MagicMock(is_superuser=True)
    return request

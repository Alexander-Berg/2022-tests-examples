import json

import pytest
from mock import patch

from django.core.urlresolvers import reverse

from staff.departments.models import HeadcountPosition
from staff.departments.tests.factories import HRProductFactory
from staff.departments.tests.factories import VacancyFactory
from staff.lib.testing import BudgetPositionFactory, OccupationFactory
from staff.lib.testing import StaffFactory, ValueStreamFactory
from staff.oebs.constants import PERSON_POSITION_STATUS
from staff.person.models import StaffExtraFields

from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.headcounts.views.for_goals_views import (
    _staff_value_stream_url,
    persons_by_vs,
    positions_by_vs,
    occupation_by_logins,
    vs_by_persons,
)


def test_staff_value_stream_url():
    assert _staff_value_stream_url('meta_search') == 'svc_meta_search'
    assert _staff_value_stream_url('svc_meta_search') == 'svc_meta_search'


@pytest.mark.django_db
def test_that_view_returns_all_required_fields(rf):
    # given
    valuestream = ValueStreamFactory()
    person = StaffFactory()
    HeadcountPositionFactory(current_person=person, current_login=person.login, valuestream=valuestream)
    request = rf.get(reverse('headcounts-api:persons_by_vs'), data={'vs': valuestream.url})
    expected_fields = (
        'login',
        'first_name',
        'first_name_en',
        'last_name',
        'last_name_en',
        'department',
    )

    # when
    result = persons_by_vs(request)

    # then
    result = json.loads(result.content)
    for field in expected_fields:
        assert field in result['persons'][0]
        assert result['persons'][0][field]


@pytest.mark.django_db
def test_that_view_returns_only_occupied_positions_with_persons_by_given_valuestream(rf):
    # given
    valuestream = ValueStreamFactory()
    another_valuestream = ValueStreamFactory()
    person = StaffFactory()
    another_person = StaffFactory()
    HeadcountPositionFactory(valuestream=valuestream)
    HeadcountPositionFactory(
        current_person=person,
        current_login=person.login,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
    )
    HeadcountPositionFactory(
        current_person=another_person,
        current_login=another_person.login,
        valuestream=another_valuestream,
        status=PERSON_POSITION_STATUS.OCCUPIED,
    )
    request = rf.get(reverse('headcounts-api:persons_by_vs'), data={'vs': valuestream.url})

    # when
    result = persons_by_vs(request)

    # then
    result = json.loads(result.content)
    assert len(result['persons']) == 0


@pytest.mark.django_db
def test_positions_by_vs(rf):
    valuestream = ValueStreamFactory()
    another_valuestream = ValueStreamFactory()
    person = StaffFactory()
    another_person = StaffFactory()
    budget_position = BudgetPositionFactory()
    another_budget_position = BudgetPositionFactory()
    hidden_budget_position = BudgetPositionFactory()
    occupied_budget_position = BudgetPositionFactory()
    VacancyFactory(is_hidden=False, is_published=True, budget_position=budget_position)
    VacancyFactory(is_hidden=False, is_published=True, budget_position=another_budget_position)
    VacancyFactory(is_hidden=True, is_published=True, budget_position=hidden_budget_position)
    VacancyFactory(is_hidden=False, is_published=True, budget_position=occupied_budget_position)
    HeadcountPositionFactory(
        headcount=0,
        current_person=person,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.OCCUPIED,
        code=person.budget_position.code,
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=person,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.OCCUPIED,
        code=person.budget_position.code,
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=another_person,
        valuestream=another_valuestream,
        status=PERSON_POSITION_STATUS.OCCUPIED,
        code=another_person.budget_position.code,
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=None,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        code=budget_position.code
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=None,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        code=hidden_budget_position.code
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=None,
        valuestream=another_valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        code=another_budget_position.code
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=person,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.OCCUPIED,
        code=occupied_budget_position.code
    )
    HeadcountPositionFactory(valuestream=valuestream, code=1221212121)
    request = rf.get(reverse('headcounts-api:persons_by_vs'), data={'vs': valuestream.url})
    expected_person_fields = (
        'login',
        'first_name',
        'first_name_en',
        'last_name',
        'last_name_en',
        'department',
    )
    expected_vacancy_fields = (
        'id',
        'budget_position',
        'is_published',
        'name',
        'occupation',
        'department',
    )

    result = positions_by_vs(request)

    result = json.loads(result.content)
    assert len(result['positions']) == 2
    assert result.get('continuation_token', None) is None

    for position in result['positions']:
        if position['entity_type'] == 'person':
            for field in expected_person_fields:
                assert field in position
                assert position.get(field, None) is not None
        elif position['entity_type'] == 'vacancy':
            for field in expected_vacancy_fields:
                assert field in position
                assert position.get(field, None) is not None


@pytest.mark.django_db
def test_positions_by_vs_paging(rf):
    page_size = 5
    starting_from = 1
    total_matched_entities = page_size * 5
    valuestream = ValueStreamFactory()

    for _ in range(total_matched_entities):
        person = StaffFactory()
        HeadcountPositionFactory(
            headcount=1,
            current_person=person,
            current_login=person.login,
            valuestream=valuestream,
            status=PERSON_POSITION_STATUS.VACANCY_OPEN,
            code=person.budget_position.code,
        )

    codes = HeadcountPosition.objects.all().order_by('code').values_list('code', flat=True)
    continuation_token = codes[starting_from]
    request = rf.get(
        reverse('headcounts-api:persons_by_vs'),
        data={'vs': valuestream.url, 'continuation_token': continuation_token},
    )

    with patch('staff.headcounts.views.for_goals_views.PAGE_SIZE', page_size):
        result = positions_by_vs(request)

    result = json.loads(result.content)
    assert len(result['positions']) < total_matched_entities
    assert len(result['positions']) == page_size
    assert result['continuation_token'] == codes[starting_from + page_size]


@pytest.mark.django_db
def test_positions_by_vs_published(rf):
    valuestream = ValueStreamFactory()
    budget_position = BudgetPositionFactory()
    published_budget_position = BudgetPositionFactory()
    VacancyFactory(is_hidden=False, is_published=False, budget_position=budget_position)
    VacancyFactory(is_hidden=False, is_published=True, budget_position=published_budget_position)
    HeadcountPositionFactory(
        headcount=1,
        current_person=None,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        code=budget_position.code
    )
    HeadcountPositionFactory(
        headcount=1,
        current_person=None,
        valuestream=valuestream,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        code=published_budget_position.code
    )
    request = rf.get(reverse('headcounts-api:persons_by_vs'), data={'vs': valuestream.url})

    result = positions_by_vs(request)

    result = json.loads(result.content)
    assert len(result['positions']) == 2

    for position in result['positions']:
        if position['is_published']:
            assert position['name']
        else:
            assert position.get('name', None) is None


@pytest.mark.django_db
def test_that_vs_by_persons_works(rf):
    # given
    valuestream1 = ValueStreamFactory()
    valuestream2 = ValueStreamFactory()
    hr_product1 = HRProductFactory(service_id=100500)
    hr_product2 = HRProductFactory(service_id=100501)
    person1 = StaffFactory()
    person2 = StaffFactory()

    HeadcountPositionFactory(
        current_login=person1.login,
        current_person=person1,
        valuestream=valuestream1,
        hr_product=hr_product1,
    )

    HeadcountPositionFactory(
        current_login=person2.login,
        current_person=person2,
        valuestream=valuestream2,
        hr_product=hr_product2,
    )

    request = rf.post(reverse('headcounts-api:vs_by_persons'), data={'persons': [person1.login, person2.login]})

    # when
    result = vs_by_persons(request)

    # then
    assert result.status_code == 200
    response = json.loads(result.content)
    assert person1.login in response
    assert person2.login in response

    assert response[person1.login] == {
        'vs_url': valuestream1.url,
        'hr_product_id': hr_product1.id,
        'abc_service_id': hr_product1.service_id,
    }

    assert response[person2.login] == {
        'vs_url': valuestream2.url,
        'hr_product_id': hr_product2.id,
        'abc_service_id': hr_product2.service_id,
    }


@pytest.mark.django_db
def test_occupation_by_logins(rf):
    occupation1 = OccupationFactory()
    occupation2 = OccupationFactory()

    person1 = StaffFactory()
    StaffExtraFields.objects.create(staff=person1, occupation=occupation1)

    person2 = StaffFactory()
    StaffExtraFields.objects.create(staff=person2, occupation=occupation2)

    form_data = {'persons': [person1.login, person2.login]}
    request = rf.post(reverse('headcounts-api:occupation_by_logins'), data=form_data)

    # when
    response = occupation_by_logins(request)

    # then
    assert response.status_code == 200
    response_data = json.loads(response.content)
    assert len(response_data) == 2
    assert response_data[person1.login] == occupation1.pk
    assert response_data[person2.login] == occupation2.pk

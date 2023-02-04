import json
from datetime import datetime, timedelta

import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.auth.utils import get_or_create_test_user
from staff.lib.testing import CountryFactory, CityFactory, OfficeFactory, OrganizationFactory
from staff.person.models.person import WORK_MODES

from staff.gap.edit_views.edit_gap_view import edit_gap
from staff.gap.exceptions import MandatoryVacationTooShortError
from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_new_gap_has_empty_label(gap_test, client):
    url = reverse('gap:edit-gap', kwargs={'login': 'tester'})

    response = client.get(url)
    result = json.loads(response.content)

    assert '' in {item['value'] for item in result['structure']['workflow']['choices']}
    assert '' in result['fields']
    assert result['data']['workflow']['value'] == ''


@pytest.mark.django_db
def test_new_gap_has_work_mode(gap_test, client):
    url = reverse('gap:edit-gap', kwargs={'login': 'tester'})

    response = client.get(url)
    result = json.loads(response.content)

    assert result['person']
    assert result['person']['work_mode'] == WORK_MODES.OFFICE


@pytest.mark.django_db
def test_new_gap_for_non_russian_office_has_no_paid_day_off_workflow(gap_test, client):
    response = client.get(reverse('gap:edit-gap', kwargs={'login': 'tester'}))
    result = json.loads(response.content)

    assert 'paid_day_off' not in result['fields']
    choices = {choice['value'] for choice in result['structure']['workflow']['choices']}
    assert 'paid_day_off' not in choices


@pytest.mark.django_db
def test_new_gap_for_russian_office_has_paid_day_off_workflow(gap_test, client):
    tester = get_or_create_test_user().get_profile()
    tester.office = OfficeFactory(city=CityFactory(country=CountryFactory(code='ru')))
    tester.save()
    response = client.get(reverse('gap:edit-gap', kwargs={'login': 'tester'}))
    result = json.loads(response.content)

    assert 'paid_day_off' in result['fields']
    choices = {choice['value'] for choice in result['structure']['workflow']['choices']}
    assert 'paid_day_off' in choices


@pytest.mark.django_db
def test_form_with_empty_workflow_will_not_pass_validation(gap_test, client):
    url = reverse('gap:edit-gap', kwargs={'login': 'tester'})

    response = client.post(
        url,
        json.dumps({'workflow': ''}),
        content_type='application/json'
    )

    result = json.loads(response.content)

    assert response.status_code == 200
    assert result['errors']['workflow'][0]['code'] == 'required'


@pytest.fixture
def paid_day_off_gap_and_person(mocked_mongo, gap_test):
    workflow_class = find_workflow('paid_day_off')
    person = gap_test.test_person
    person.organization = OrganizationFactory(country_code=settings.RUSSIA_CODE)
    person.save()
    gap_base = gap_test.get_base_gap(workflow_class)
    gap_base['date_from'] = datetime(2019, 11, 28, 0, 0)
    gap_base['date_to'] = datetime(2019, 11, 30, 0, 0)
    gap = workflow_class.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(gap_base)
    return gap, person


def _update_workflow_days(rf, person, gap_id, num_of_days):
    assert 0 < num_of_days < 10
    request = rf.post(
        reverse('gap:edit-gap', kwargs={'login': person.login, 'gap_id': gap_id}),
        json.dumps({
            'workflow': 'paid_day_off',
            'full_day': 'on',
            'date_from': '2019-11-20T00:00:00',
            'date_to': '2019-11-2{}T00:00:00'.format(num_of_days-1),
        }),
        content_type='application/json',
    )
    request.user = person.user
    response = edit_gap(request, person.login, gap_id)
    return json.loads(response.content)


@pytest.mark.django_db
def test_update_gap_exceed_max(rf, paid_day_off_gap_and_person):
    gap, person = paid_day_off_gap_and_person

    response = _update_workflow_days(rf, person, gap['id'], 3)
    assert response == {'errors': {'date_to': [{'code': 'days_limit_reached'}]}}


@pytest.mark.django_db
def test_update_gap_when_no_paid_day_off_left(rf, paid_day_off_gap_and_person):
    gap, person = paid_day_off_gap_and_person

    response = _update_workflow_days(rf, person, gap['id'], 1)
    assert response == {u'id': 1}


@pytest.mark.django_db
def test_update_gap_with_left_gap_days(rf, paid_day_off_gap_and_person):
    gap, person = paid_day_off_gap_and_person
    person.paid_day_off = 1
    person.save()

    response = _update_workflow_days(rf, person, gap['id'], 3)
    assert response == {u'id': 1}


def get_edit_gap(rf, observer, login):
    request = rf.get(reverse('gap:edit-gap', kwargs={'login': login}))
    request.user = observer.user
    response = edit_gap(request, login)
    try:
        js = json.loads(response.content)
    except Exception:
        js = None
    return response.status_code, js


@pytest.mark.django_db
def test_cant_edit_external_self_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']

    code, resp = get_edit_gap(rf, observer, observer.login)

    assert code == 403, resp


@pytest.mark.django_db
def test_can_edit_external_self_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    external_gap_case['open_for_self']()

    code, resp = get_edit_gap(rf, observer, observer.login)

    assert code == 200, resp
    assert resp


@pytest.mark.django_db
def test_403_external_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    login = external_gap_case['inner_person'].login

    code, resp = get_edit_gap(rf, observer, login)

    assert code == 403, resp


@pytest.mark.django_db
def test_external_can_edit_other_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    login = external_gap_case['inner_person'].login
    external_gap_case['create_permission']()

    code, resp = get_edit_gap(rf, observer, login)

    assert code == 200, resp
    assert resp


@pytest.mark.django_db
def test_absence_form_can_create_gap(gap_test, client):
    url = reverse('gap:edit-gap', kwargs={'login': 'tester'})
    data = {
        'workflow': 'absence',
        'date_from': '2020-02-28T00:00:00.000Z',
        'date_to': '2020-02-29T00:00:00.000Z',
        'comment': 'Еду в Питер в командировку.',
        'full_day': True,
    }
    response = client.post(
        url,
        json.dumps(data),
        content_type='application/json'
    )
    gap_id = json.loads(response.content)['id']
    url = reverse('gap:edit-gap', kwargs={'login': 'tester', 'gap_id': gap_id})

    result = client.get(url, content_type='application/json')
    result_data = json.loads(result.content)['data']

    data['date_from'] = data['date_from'][:-5]
    data['date_to'] = data['date_to'][:-5]

    for key, value in data.items():
        assert result_data[key]['value'] == value


@pytest.fixture
def mandatory_vacation_gap_and_person(mocked_mongo, gap_test):
    workflow_class = find_workflow('vacation')
    person = gap_test.test_person
    person.organization = OrganizationFactory(country_code=settings.RUSSIA_CODE)
    person.save()
    gap_base = gap_test.get_base_gap(workflow_class)
    gap_base['date_from'] = datetime(2022, 3, 1, 0, 0)
    gap_base['date_to'] = datetime(2022, 3, 15, 0, 0)
    gap_base['mandatory'] = True
    gap = workflow_class.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(gap_base)
    return gap, person


@pytest.mark.django_db
def test_existing_gap_workflow_comes_from_db(rf, mandatory_vacation_gap_and_person):
    gap, person = mandatory_vacation_gap_and_person

    new_date_from = datetime(2022, 8, 1)
    new_date_to = datetime(2022, 8, 2)
    new_min_date_to = new_date_from.date() + timedelta(settings.MANDATORY_VACATION_DURATION - 1)

    request = rf.post(
        reverse('gap:edit-gap', kwargs={'login': person.login, 'gap_id': gap['id']}),
        json.dumps({
            'workflow': 'remote_work',
            'date_from': new_date_from.isoformat(),
            'date_to': new_date_to.isoformat(),
        }),
        content_type='application/json',
    )
    request.user = person.user

    response = edit_gap(request, person.login, gap['id'])

    assert response.status_code == 400
    assert json.loads(response.content) == MandatoryVacationTooShortError(new_min_date_to).error_dict

from datetime import date

from django.core.urlresolvers import reverse

from intranet.audit.src.core import models

import pytest


def test_filter_by_id_success(db, client, process, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'filter_by': 'process:{}'.format(process.id), },)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['method'] == models.ControlPlan.METHODS.manual
    assert response_json[0]['process_data'][0]['id'] == process.id
    assert response.status_code == 200


def test_filter_by_name_success(db, client, process_two, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'filter_by': 'process__name:process_two', },)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['method'] == models.ControlPlan.METHODS.auto
    assert response_json[0]['process_data'][0]['id'] == process_two.id
    assert response.status_code == 200


def test_filter_by_multiple_values_success(db, client, process, process_two, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'filter_by': 'process__id:{},{}'.format(process.id, process_two.id), },)
    response_json = response.json()['results']
    assert len(response_json) == 2
    assert response.status_code == 200


def test_filter_by_blank_response_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'filter_by': 'process:44', }, )
    response_json = response.json()['results']
    assert len(response_json) == 0
    assert response.status_code == 200


def test_filter_by_fail(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'filter_by': 'test_me:TEST', }, )
    assert response.status_code == 409
    assert b'You have to re-create the filter, your version of the filter is deprecated' in response.content


@pytest.mark.parametrize('left_date, right_date', (
    (None, None),
    ('2019-06-10', None),
    (None, '2019-08-20'),
    ('2019-06-10', '2019-08-20'),
    ('2019-06-10', '2019-06-10'),
))
def test_filter_by_date_success(db, client, control_plan, left_date, right_date):
    control_plan.test_period_started = date(2019, 6, 10)
    control_plan.test_period_finished = date(2019, 8, 20)
    control_plan.save()

    url = reverse("api_v1:controlplan")
    response = client.get(
        path=url,
        data={'filter_by': ' '.join([
            'test_period_started__gte:{}'.format(left_date) * bool(left_date),
            'test_period_started__lte:{}'.format(right_date) * bool(right_date),
        ])},
    )
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response.status_code == 200

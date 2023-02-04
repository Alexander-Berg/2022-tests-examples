import pytest

from django.core.urlresolvers import reverse
from rest_framework.settings import api_settings

from intranet.audit.src.api_v1.pagination import AuditPagination
from intranet.audit.src.core import models


@pytest.fixture
def control_1(db, author):
    return models.Control.objects.create(
        number='123',
        name='control2',
        author=author,
    )


@pytest.fixture
def control_plan_another(db, process, risk, control_1, author, ):
    control_plan_obj = models.ControlPlan(
        control=control_1,
        method=models.ControlPlan.METHODS.manual,
        control_type=models.ControlPlan.TYPES.warning,
        author=author,
        frequency=models.ControlPlan.FREQUENCIES.adhoc,
        comment='comment for one',
    )
    control_plan_obj.save()
    control_plan_obj.process.add(process)
    control_plan_obj.risk.add(risk)
    return control_plan_obj


def test_pagination_success(db, client, control_plan, control_plan_two, ):
    AuditPagination.page_size = 1
    url = reverse("api_v1:controlplan")
    response = client.get(url)
    response_json = response.json()
    assert len(response_json['results']) == 1
    next_url = response_json['next']
    assert 'http://testserver/api/v1/resources/controlplan?page=' in next_url
    assert response_json['previous'] is None
    assert response_json['results'][0]['id'] == control_plan_two.id
    assert response.status_code == 200

    response = client.get(next_url)
    response_json = response.json()
    assert len(response_json['results']) == 1
    assert 'http://testserver/api/v1/resources/controlplan?page=' in response_json['previous']
    assert response_json['next'] is None
    assert response_json['results'][0]['id'] == control_plan.id
    AuditPagination.page_size = api_settings.PAGE_SIZE


def test_filter_by_manytomany_with_pagination_success(db, client, control_plan, control_plan_two, control_plan_three):
    AuditPagination.page_size = 1
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': 'process__name'})
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan_three.id
    assert 'order_by=process__name' in response_json['next']
    assert response.status_code == 200

    response = client.get(response_json['next'])
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan.id
    assert 'order_by=process__name' in response_json['next']

    response = client.get(response_json['next'])
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan_two.id
    assert response_json['next'] is None

    AuditPagination.page_size = api_settings.PAGE_SIZE


def test_order_by_bool_success(db, client, control_plan, control_plan_two):
    control_plan.antifraud = True
    control_plan.save()
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': 'antifraud'})
    response_json = response.json()
    results = response_json['results']
    assert response.status_code == 200
    assert [item['id'] for item in results] == [control_plan_two.id, control_plan.id]

    response = client.get(url, {'order_by': '-antifraud'})
    response_json = response.json()
    results = response_json['results']
    assert response.status_code == 200
    assert [item['id'] for item in results] == [control_plan.id, control_plan_two.id]


def test_filter_by_related_with_pagination_success(db, client, control_plan, control_plan_two, control_plan_another):
    AuditPagination.page_size = 1
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': 'control__name'})
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan_two.id
    assert 'order_by=control__name' in response_json['next']
    assert response.status_code == 200

    response = client.get(response_json['next'])
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan.id
    assert 'order_by=control__name' in response_json['next']

    response = client.get(response_json['next'])
    response_json = response.json()
    results = response_json['results']
    assert len(results) == 1
    assert results[0]['id'] == control_plan_another.id
    assert response_json['next'] is None

    AuditPagination.page_size = api_settings.PAGE_SIZE

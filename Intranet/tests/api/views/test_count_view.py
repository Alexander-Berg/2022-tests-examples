import pytest

from django.core.urlresolvers import reverse

from intranet.audit.src.core import models


@pytest.fixture
def risk_two(db, author):
    return models.Risk.objects.create(
        number='risk number2',
        name='risk2',
        author=author,
    )


def test_count_view_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:count", kwargs={'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json == {"objects_count": 2}


def test_count_view_with_filter_success(db, client, process, control_plan, control_plan_two):
    url = reverse("api_v1:count", kwargs={'obj_class': 'controlplan'})
    response = client.get(url, {'filter_by': 'process__id:{}'.format(process.id)})
    response_json = response.json()
    assert response.status_code == 200
    assert response_json == {"objects_count": 1}


def test_count_view_with_filter_return_distinct_success(db, client, risk, risk_two, control_plan, control_plan_two):
    control_plan.risk.add(risk_two)
    control_plan_two.risk.add(risk_two)
    url = reverse("api_v1:count", kwargs={'obj_class': 'controlplan'})
    response = client.get(url, {'filter_by': 'risk__id:{},{}'.format(risk.id, risk_two.id)})
    response_json = response.json()
    assert response.status_code == 200
    assert response_json == {"objects_count": 2}

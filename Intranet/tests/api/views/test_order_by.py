import pytest

from django.core.urlresolvers import reverse
from intranet.audit.src.core import models


@pytest.fixture
def control_1(db, author):
    return models.Control.objects.create(
        number='123',
        name='control',
        author=author,
    )


@pytest.fixture
def control_2(db, author):
    return models.Control.objects.create(
        number='222',
        name='control',
        author=author,
    )


@pytest.fixture
def control_3(db, author):
    return models.Control.objects.create(
        number='10',
        name='control1',
        author=author,
    )


def test_filter_by_id_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': 'id'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == sorted(models.ControlPlan.objects.values_list('id', flat=True))
    assert response.status_code == 200


def test_filter_by_id_desc_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': '-id'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == sorted(models.ControlPlan.objects.values_list('id', flat=True),
                                                          reverse=True,
                                                          )
    assert response.status_code == 200


def test_filter_by_multiple_arguments_success(db, client, control_1, control_2, control_3, ):
    url = reverse("api_v1:control")
    response = client.get(url, {'order_by': 'name,number'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == [obj.id for obj in (control_1,
                                                                       control_2,
                                                                       control_3,
                                                                       )
                                                    ]
    assert response.status_code == 200


def test_filter_by_multiple_arguments_second_desc_success(db, client, control_1, control_2, control_3, ):
    url = reverse("api_v1:control")
    response = client.get(url, {'order_by': 'name,-number'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == [obj.id for obj in (control_2,
                                                                       control_1,
                                                                       control_3,
                                                                       )
                                                    ]
    assert response.status_code == 200


def test_filter_by_multiple_arguments_first_desc_success(db, client, control_1, control_2, control_3, ):
    url = reverse("api_v1:control")
    response = client.get(url, {'order_by': '-name,number'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == [obj.id for obj in (control_3,
                                                                       control_1,
                                                                       control_2,
                                                                       )
                                                    ]
    assert response.status_code == 200


def test_filter_by_related_success(db, client, control_plan, control_plan_two, control_plan_three):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'order_by': 'process__name,-id'})
    response_json = response.json()['results']
    assert [obj['id'] for obj in response_json] == [obj.id for obj in (control_plan_three,
                                                                       control_plan,
                                                                       control_plan_two,
                                                                       )
                                                    ]
    assert response.status_code == 200

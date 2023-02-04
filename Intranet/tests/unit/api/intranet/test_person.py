import pretend
import pytest
from django.core.urlresolvers import reverse

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data():
    department = factories.DepartmentFactory()
    staff1 = factories.StaffFactory(department=department)
    staff2 = factories.StaffFactory()
    staff3 = factories.StaffFactory()

    return pretend.stub(
        staff1=staff1,
        staff2=staff2,
        staff3=staff3,
        department=department,
    )


def check_staff(client, params, persons):
    response = client.json.get(
        reverse('api-v3:intranet-person-list'),
        params,
    )

    assert response.status_code == 200

    expected_ids = [s.staff_id for s in persons]
    real_ids = [s['id'] for s in response.json()['results']]
    assert real_ids == expected_ids


def test_get_staff(client, data):
    response = client.json.get(reverse('api-v3:intranet-person-list'), {'login': data.staff1.login})

    assert response.status_code == 200

    json = response.json()['results']
    assert json[0]['id'] == data.staff1.staff_id
    assert json[0]['uid'] == data.staff1.uid
    assert json[0]['department'] == {
        'id': data.department.pk,
        'name': {'ru': data.department.name, 'en': data.department.name_en},
        'url': data.department.url,
    }


def test_filter_by_login(client, data):
    check_staff(
        client,
        {'login': data.staff1.login},
        [data.staff1],
    )

    check_staff(
        client,
        {'login__in': f'{data.staff1.login},{data.staff2.login}'},
        [data.staff1, data.staff2],
    )


def test_filter_services_by_department(client, data):
    department2 = factories.DepartmentFactory()

    check_staff(
        client,
        {'department': data.department.pk},
        [data.staff1],
    )

    check_staff(
        client,
        {'department': [data.department.pk, department2.pk]},
        [data.staff1],
    )

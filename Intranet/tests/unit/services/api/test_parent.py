import pretend
import pytest
from django.conf import settings

from plan.services.state import SERVICE_STATE
from plan.staff.constants import DEPARTMENT_ROLES
from common import factories
from rest_framework.reverse import reverse

pytestmark = [pytest.mark.django_db(transaction=True)]


def department_with_chief(parent=None):
    department = factories.DepartmentFactory(parent=parent)
    staff = factories.StaffFactory(department=department)
    factories.DepartmentStaffFactory(department=department, staff=staff, role=DEPARTMENT_ROLES.CHIEF)
    return department, staff


@pytest.fixture
def russia():
    country, president = department_with_chief()
    region, governor = department_with_chief(country)
    big_city, big_major = department_with_chief(region)
    small_city, small_major = department_with_chief(region)

    district = factories.DepartmentFactory(parent=big_city)
    factories.DepartmentStaffFactory(department=district, staff=big_major, role=DEPARTMENT_ROLES.CHIEF)

    citizen = factories.StaffFactory(department=district)
    factories.DepartmentStaffFactory(department=district, staff=citizen)

    return pretend.stub(
        country=country,
        president=president,
        region=region,
        governor=governor,
        big_city=big_city,
        big_major=big_major,
        small_city=small_city,
        small_major=small_major,
        district=district,
        citizen=citizen
    )


def test_get_parent(client, russia, django_assert_num_queries):
    service1 = factories.ServiceFactory(owner=russia.small_major)
    service2 = factories.ServiceFactory(owner=russia.small_major, parent=service1)
    service3 = factories.ServiceFactory(owner=russia.small_major)
    factories.ServiceFactory(owner=russia.small_major, is_exportable=False)

    factories.ServiceFactory(owner=russia.small_major, state=SERVICE_STATE.CLOSED)
    factories.ServiceFactory(owner=russia.small_major, state=SERVICE_STATE.DELETED)

    factories.ServiceFactory(owner=russia.president)
    factories.ServiceFactory(owner=russia.big_major)
    factories.ServiceFactory()

    # 2 middleware
    # 1 select staff
    # 1 select department closure
    # 1 select department staff
    # 1 select service closure
    # 2 select service
    # 1 pg in recovery
    with django_assert_num_queries(9):
        response = client.json.get(
            reverse('api-frontend:service-parent-list'),
            {'owner': russia.small_major.login}
        )

    assert response.status_code == 200
    services = response.json()['results']
    assert len(services) == 3
    assert services[0]['id'] == service1.id
    assert {service['id'] for service in services} == {service1.id, service2.id, service3.id}


def test_get_parent_outsider(client, russia):
    staff = factories.StaffFactory()

    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': staff.login}
    )

    assert response.status_code == 200
    assert len(response.json()['results']) == 0


def test_404_on_wrong_owner(client):
    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': 'trump'}
    )

    assert response.status_code == 404


def test_get_parent_no_own_services(client, russia):
    service1 = factories.ServiceFactory(owner=russia.governor)
    service2 = factories.ServiceFactory(owner=russia.governor, parent=service1)

    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': russia.big_major.login}
    )

    assert response.status_code == 200
    services = response.json()['results']
    assert len(services) == 2
    assert services[0]['id'] == service1.id
    assert {service['id'] for service in services} == {service1.id, service2.id}


def test_get_parent_limit(client, russia):
    for _ in range(2 * settings.PARENT_HINT_LIMIT):
        factories.ServiceFactory(owner=russia.governor)

    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': russia.governor.login}
    )

    assert response.status_code == 200
    assert len(response.json()['results']) == settings.PARENT_HINT_LIMIT


def test_get_parent_skip_level(client, russia):
    service1 = factories.ServiceFactory(owner=russia.president)
    service2 = factories.ServiceFactory(owner=russia.president, parent=service1)
    service3 = factories.ServiceFactory(owner=russia.president, parent=service2)

    factories.ServiceFactory(owner=russia.small_major)

    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': russia.citizen.login}
    )

    assert response.status_code == 200
    services = response.json()['results']
    assert len(services) == 3
    assert [service['id'] for service in services] == [service1.id, service2.id, service3.id]


def test_exclude_base_non_leaf(client, russia):
    service1 = factories.ServiceFactory(owner=russia.small_major, is_base=True)
    service2 = factories.ServiceFactory(owner=russia.small_major, is_base=True, parent=service1)

    response = client.json.get(
        reverse('api-frontend:service-parent-list'),
        {'owner': russia.small_major.login}
    )

    assert response.status_code == 200
    services = response.json()['results']
    assert len(services) == 1
    assert services[0]['id'] == service2.id

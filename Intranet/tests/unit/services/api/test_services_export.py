import pytest

from django.core.urlresolvers import reverse
from django.conf import settings
from freezegun import freeze_time
from io import BytesIO

from plan.services.models import Service
from common import factories
from utils import source_path, compare_xlsx

pytestmark = pytest.mark.django_db


@pytest.fixture
def export_service_tree():
    schema = [
        ('A', None),
        ('B', None),
        ('C', 'A'),
        ('D', 'C'),
        ('E', 'C'),
        ('F', 'E'),
    ]
    service_type = factories.ServiceTypeFactory(name='Сервис')
    owner = factories.StaffFactory(first_name='AAAA', last_name='OOO', login='xxx')
    for index, (slug, parent_slug) in enumerate(schema):
        service = factories.ServiceFactory(
            service_type=service_type,
            slug=slug,
            parent=Service.objects.get(slug=parent_slug) if parent_slug else None,
            id=index + 1,
            name=slug + 'ru',
            name_en=slug + 'en',
            owner=owner,
        )
        tag = factories.ServiceTagFactory(name=f'tag {slug}')
        service.tags.add(tag)


@freeze_time('2020-01-01')
def test_export_parents_filter(client, export_service_tree):
    A = Service.objects.get(slug='A').id
    response = client.json.get(reverse('api-frontend:services-export-list'), {'parents': A})
    assert response.status_code == 400
    assert response.json()['error']['message']['en'] == 'Export does not support the parent filter'


@freeze_time('2020-01-01')
def test_export_root_filter(client, export_service_tree):
    service = Service.objects.select_related('owner').get(slug='E')
    service.use_for_procurement = True
    service.save()
    rtype = factories.ResourceTypeFactory(
        code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE,
        supplier=service,
    )
    resource = factories.ResourceFactory(type=rtype)
    factories.ServiceResourceFactory(
        type=rtype,
        resource=resource,
        state='granted',
        service=service,
        attributes={
            'leaf_oebs_id': 'leaf_some_id',
            'parent_oebs_id': 'parent_some_id',
        }
    )
    C = Service.objects.select_related('owner').get(slug='C').id
    response = client.get(reverse('api-frontend:services-export-list'), {'root': C})
    assert response.status_code == 200
    path = source_path(
        'intranet/plan/tests/test_data/frontend_export_root_filter.xlsx'
    )
    compare_xlsx(path, BytesIO(response.content))


@freeze_time('2020-01-01')
def test_export_search_filter(client, export_service_tree):
    response = client.get(reverse('api-frontend:services-export-list'), {'search': 'Dr'})
    assert response.status_code == 200
    path = source_path(
        'intranet/plan/tests/test_data/frontend_export_search_filter.xlsx'
    )
    compare_xlsx(path, BytesIO(response.content))


def test_export_too_broad(client, export_service_tree):
    response = client.get(reverse('api-frontend:services-export-list'))
    assert response.status_code == 400
    assert response.json()['error']['message']['en'] == 'The search term is too broad'


@pytest.mark.parametrize(
    'staff_role', ['full_access', 'own_only_viewer', 'services_viewer']
)
@freeze_time('2020-01-01')
def test_export_deny(client, staff_role, staff_factory, export_service_tree):
    Service.objects.get(slug='C')
    staff_for = staff_factory(staff_role)
    client.login(staff_for.login)
    response = client.get(reverse('api-frontend:services-export-list'), {'search': 'Dr'})
    if staff_role == 'full_access':
        assert response.status_code == 200
    else:
        assert response.status_code == 403

import pytest
from io import BytesIO

from rest_framework.reverse import reverse

from utils import source_path, compare_xlsx
from common import factories

from plan.resources.models import ServiceResource

pytestmark = pytest.mark.django_db


@pytest.fixture
def gdpr_export_data(gdpr_resource_type):
    target_service = factories.ServiceFactory(name='target_service')
    service = factories.ServiceFactory(name='source_service')
    for is_storing, is_using, data, store_in, store_for, purpose, subject in (
        ('Нет', 'Да', 'Имя', None, '5 лет', 'важная цель', 'пользователь'),
        ('Нет', 'Да', 'Имя', None, '5 лет', 'важная цель', 'пользователь'),
        ('Нет', 'Да', 'Фамилия', None, '12 лет', 'важная цель', 'пользователь'),
        ('Да', 'Нет', 'Лайки', 'YT', '12 лет', 'важная цель', 'пользователь'),
        ('Да', 'Нет', 'Лайки', 'MDS', '12 лет', 'еще цель', 'что-то еще'),
        ('Да', 'Да', 'Фамилия', 'YT', '12 лет', 'для сохранности', 'какой-то субъект'),
    ):
        resource = factories.ResourceFactory(
            type=gdpr_resource_type,
            name='name',
            attributes={
                'is_storing': is_storing,
                'is_using': is_using,
                'data': data,
                'store_in': store_in,
                'store_for': store_for,
                'purpose': purpose,
                'service_from_slug': target_service.slug,
                'subject': subject,
            }
        )
        factories.ServiceResourceFactory(
            resource=resource,
            service=service,
            state=ServiceResource.GRANTED,
        )


def test_export_gdpr(client, gdpr_export_data):
    response = client.get(reverse('api-frontend:gdpr-export-list'))
    assert response.status_code == 200
    path = source_path(
        'intranet/plan/tests/test_data/gdpr_export_test.xlsx'
    )
    compare_xlsx(path, BytesIO(response.content))

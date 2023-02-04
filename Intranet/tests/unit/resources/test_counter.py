import pretend
import pytest
from rest_framework.reverse import reverse

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def counter_data():
    service = factories.ServiceFactory()
    for i in range(5):
        factories.ServiceResourceCounterFactory(
            service=service,
            count=i,
        )

    factories.ServiceResourceCounterFactory(
        count=10,
    )
    return pretend.stub(
        service=service,
    )


def test_get_resources_counter(client, counter_data, django_assert_num_queries):
    with django_assert_num_queries(5):
        response = client.get(
            reverse('api-frontend:resources-counter-list'),
            {'service_id': counter_data.service.id},
        )
    assert response.status_code == 200
    result = response.json()['results']
    assert len(result) == 5
    assert all(item['service_id'] == counter_data.service.id for item in result)

import pretend
import pytest
from django.core.urlresolvers import reverse

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role):
    category = factories.ResourceTagCategoryFactory()
    service = factories.ServiceFactory()
    staff1 = factories.StaffFactory()
    staff2 = factories.StaffFactory()
    member1 = factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff1)

    return pretend.stub(
        category=category,
        service=service,
        owner_role=owner_role,
        staff1=staff1,
        staff2=staff2,
        member1=member1,
    )


def test_get_categories(client, data):
    response = client.json.get(reverse('resources-api:category-list'))

    assert response.status_code == 200

    results = response.json()['results']
    assert len(results) == 1
    assert results[0] == {
        'id': data.category.id,
        'slug': data.category.slug,
        'name': {'ru': data.category.name, 'en': data.category.name_en},
    }


def test_get_categories_with_tags(client):
    category = factories.ResourceTagCategoryFactory()
    factories.ResourceTagCategoryFactory()

    factories.ResourceTagFactory(category=category)

    response = client.json.get(reverse('resources-api:category-list'))
    assert response.status_code == 200
    assert len(response.json()['results']) == 2

    response = client.json.get(
        reverse('resources-api:category-list'),
        {
            'with_tags': True,
        }
    )

    assert response.status_code == 200

    results = response.json()['results']
    assert len(results) == 1
    assert results[0]['id'] == category.id

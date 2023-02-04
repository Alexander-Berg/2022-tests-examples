from functools import partial

import pytest
from django.core.urlresolvers import reverse

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def get_categories(client):
    return partial(
        client.json.get,
        reverse('resources-api:typecategory-list')
    )


def test_get_categories(get_categories):
    cat = factories.ResourceTypeCategoryFactory()

    response = get_categories()
    assert response.status_code == 200

    results = response.json()['results']
    assert len(results) == 1
    assert results[0] == {
        'id': cat.id,
        'slug': cat.slug,
        'name': {'ru': cat.name, 'en': cat.name_en},
        'description': cat.description,
    }


def test_get_categories_by_pk(get_categories):
    factories.ResourceTypeCategoryFactory(id=100, name='name')
    factories.ResourceTypeCategoryFactory(id=99, name='name')
    factories.ResourceTypeCategoryFactory(id=98, name='name')

    response = get_categories({'name': 'name'})
    results = response.json()['results']
    assert [cat['id'] for cat in results] == [98, 99, 100]

    response = get_categories({'ordering': '-id'})
    results = response.json()['results']
    assert [cat['id'] for cat in results] == [100, 99, 98]


def test_get_categories_with_types(get_categories):
    category = factories.ResourceTypeCategoryFactory()
    factories.ResourceTypeCategoryFactory()

    factories.ResourceTypeFactory(category=category)

    response = get_categories()
    assert len(response.json()['results']) == 2

    response = get_categories({'with_types': True})
    results = response.json()['results']
    assert len(results) == 1
    assert results[0]['id'] == category.id

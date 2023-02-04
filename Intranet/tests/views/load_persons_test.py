import json
import pytest

from django.core.urlresolvers import reverse


@pytest.mark.django_db
def test_load_persons(company, mocked_mongo, client):
    department_url = 'yandex_dep1'
    view_url = reverse('proposal-api:load-persons', kwargs={'department_url': department_url})
    response = client.get(view_url)
    assert response.status_code == 200
    content = json.loads(response.content)
    assert set(content.keys()) == {
        'info',
        'persons_qty',
        'is_deep',
        'name',
        'chain',
        'level',
        'url',
        'order_field',
        'descendants',
        'vacancies_count',
        'has_descendants',
        'position',
        'id',
        'headcounts_available',
        'is_expanded',
        'is_hidden',
        'section_group_id',
        'section_caption_id',
        'tags',
    }
    assert content['is_deep'] is False
    assert content['persons_qty'] == 12
    assert content['url'] == department_url

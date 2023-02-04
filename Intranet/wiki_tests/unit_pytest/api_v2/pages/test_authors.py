import datetime

import pytest
from wiki.pages.models import Revision


@pytest.fixture
def page_with_revisions(wiki_users, page_cluster):
    page = page_cluster['root']

    for i in range(10):
        new_rev: Revision = Revision.objects.produce_from_page(page)
        new_rev.mds_storage_id = None
        new_rev.created_at = datetime.datetime(year=2021, month=11, day=i + 1)
        new_rev.body = f'foo_bar_{i}'
        new_rev.save()

    page_2 = page_cluster['root/a']
    return page, page_2


@pytest.mark.django_db
def test_revision_collection(wiki_users, page_with_revisions, client):
    page, page_2 = page_with_revisions
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages/{page.id}/revisions', {'page_id': 1, 'page_size': 25})
    assert response.status_code == 200, response.json()
    assert len(response.json()['results']) == 11

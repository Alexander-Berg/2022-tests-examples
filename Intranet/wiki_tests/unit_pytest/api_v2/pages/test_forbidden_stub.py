import pytest
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json


@pytest.mark.django_db
def test_forbidden_stub_view(client, wiki_users, page_cluster):
    client.login(wiki_users.robot_wiki)
    stub_slug = 'root/a/aa'

    response = client.get(
        f'/api/v2/public/pages/forbidden_stub?slug={stub_slug}&fields=breadcrumbs,authors,pending_access_request'
    )
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'breadcrumbs': [
                {'page_exists': True, 'slug': 'root'},
                {'page_exists': True, 'slug': 'root/a'},
                {'page_exists': True, 'slug': 'root/a/aa'},
            ],
            'authors': {},
            'id': page_cluster[stub_slug].id,
            'pending_access_request': None,
            'slug': stub_slug,
            'title': 'aa',
        },
    )

import pytest
from pprint import pprint

from django.conf import settings

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from wiki.pages.models import LocationHistory


@pytest.mark.django_db
def test_stub_view(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    stub_slug = '/root/a/aa/bcd'
    response = client.get(
        f'/api/v2/public/pages/stub?slug={stub_slug}&fields=breadcrumbs,cluster,user_permissions,moved_out_pages'
    )

    expected_cluster_slug = 'root' if settings.NAVIGATION_TREE_CLUSTER_LEVEL > 0 else ''  # b2b == 0

    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'breadcrumbs': [
                {'page_exists': True, 'slug': 'root'},
                {'page_exists': True, 'slug': 'root/a'},
                {'page_exists': True, 'slug': 'root/a/aa'},
                {'page_exists': False, 'slug': 'root/a/aa/bcd'},
            ],
            'cluster': {'slug': expected_cluster_slug},
            'user_permissions': ['create_page'],
            'moved_out_pages': [],
            'slug': 'root/a/aa/bcd',
            'title': 'bcd',
        },
    )
    pprint(response.json())


@pytest.mark.django_db
def test_stub_view_moved_out_pages(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)

    stub_slug = 'root/f'

    page = page_cluster['root/a']
    LocationHistory(slug=stub_slug, page=page).save()

    page_non_accessible = page_cluster['root/b']
    set_access_author_only(page_non_accessible, new_authors=[wiki_users.volozh])
    LocationHistory(slug=stub_slug, page=page_non_accessible).save()

    page_deleted = page_cluster['root/c']
    page_deleted.status = 0
    page_deleted.save()
    LocationHistory(slug=stub_slug, page=page_deleted).save()

    LocationHistory(slug='other_page', page=page).save()

    response = client.get(f'/api/v2/public/pages/stub?slug={stub_slug}&fields=moved_out_pages')
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'moved_out_pages': [
                {'accessible': True, 'slug': page.slug, 'title': page.title},
                {'accessible': False, 'slug': page_non_accessible.slug, 'title': page_non_accessible.slug},
            ],
        },
    )
    pprint(response.json())

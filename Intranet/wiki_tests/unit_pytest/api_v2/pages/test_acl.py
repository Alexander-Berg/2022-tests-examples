import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only, set_access_custom
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.pages.access.utils import get_legacy_acl
from wiki.pages.models.consts import AclType


def _get_acl_apicall(client, wiki_users, page):
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}?fields=acl')
    data = response.json()
    assert response.status_code == 200
    return response.status_code, data['acl']


@pytest.mark.django_db
def test_get_page_acl__inherits(client, wiki_users, test_group, organizations, page_cluster, test_org_id):
    set_access_custom(page_cluster['root'], [wiki_users.asm, wiki_users.thasonic], [test_group])

    status_code, data = _get_acl_apicall(client, wiki_users, page_cluster['root/a'])

    assert status_code == 200
    assert data['inherits_from']['slug'] == 'root'
    assert len(data['groups']) == 1
    assert len(data['users']) == 2
    assert not data['break_inheritance']

    status_code, data = _get_acl_apicall(client, wiki_users, page_cluster['root/a/aa'])

    assert status_code == 200
    assert data['inherits_from']['slug'] == 'root'
    assert not data['break_inheritance']


@only_intranet
@pytest.mark.django_db
def test_get_page_acl__force_inherit(client, wiki_users, test_group, organizations, page_cluster, test_org_id):
    set_access_custom(page_cluster['root'], [wiki_users.asm, wiki_users.thasonic], [test_group])
    set_access_custom(page_cluster['root/a/aa'], [], [test_group])

    l = get_legacy_acl(page_cluster['root/a/aa'], force_inheritance=True)
    assert l.inherits_from == 'root'

    l = get_legacy_acl(page_cluster['root'], force_inheritance=True)
    assert l.inherits_from is None
    assert l.acl_type == AclType.DEFAULT

    page = page_cluster['root/a/aa']

    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}/inheritable_acl')
    data = response.json()
    assert response.status_code == 200
    assert data['inherits_from']['slug'] == 'root'


@pytest.mark.django_db
def test_get_page_acl__default(client, wiki_users, test_group, organizations, page_cluster, test_org_id):
    status_code, data = _get_acl_apicall(client, wiki_users, page_cluster['root/a/aa'])

    assert status_code == 200
    assert data['inherits_from'] is None
    assert not data['break_inheritance']
    assert data['acl_type'] == 'default'


@pytest.mark.django_db
def test_get_page_acl__author_only(client, wiki_users, test_group, organizations, page_cluster, test_org_id):
    set_access_author_only(page_cluster['root/a/aa'])
    status_code, data = _get_acl_apicall(client, wiki_users, page_cluster['root/a/aa'])

    assert status_code == 200
    assert data['inherits_from'] is None
    assert data['break_inheritance']
    assert data['acl_type'] == 'only_authors'

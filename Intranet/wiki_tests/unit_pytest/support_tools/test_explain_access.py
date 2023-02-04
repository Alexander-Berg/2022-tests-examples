import pytest

from django.conf import settings

from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory
from intranet.wiki.tests.wiki_tests.common.skips import only_biz, only_intranet
from wiki.pages.models import Access
from wiki.support_tools.models import SupportLog, SupportOperation

pytestmark = [pytest.mark.django_db]


def test_no_access_anonymous(client):
    response = client.post('/api/v2/support/explain_access', data={})
    assert response.status_code == 403


def test_no_access_non_support_group(client, wiki_users):
    client.login(wiki_users.asm)
    response = client.post('/api/v2/support/explain_access', data={})
    assert response.status_code == 403


@only_biz
def test_validation_require_org_for_biz(support_client):
    data = {'org_id': None, 'slug': '...', 'username': '...'}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 400


@only_intranet
def test_validation_org_is_none_for_intranet(support_client):
    data = {'org_id': '42', 'slug': '...', 'username': '...'}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 400


def test_invalid_slug_or_username(support_client, org_dir_id, wiki_users, test_page):
    data = {'org': {'dir_id': org_dir_id}, 'slug': 'invalid', 'username': wiki_users.asm.username}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 400

    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug, 'username': 'invalid'}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 400


@only_biz
def test_user_membership_biz(support_client, wiki_users, groups, add_user_to_group, org_dir_id, test_page):
    add_user_to_group(group=groups.group_org_42, user=wiki_users.chapson)

    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug, 'username': wiki_users.chapson.username}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200
    assert len(response.json()['user_membership']) == 1
    assert response.json()['user_membership'][0]['name'] == groups.group_org_42.name


@only_intranet
def test_user_membership_intranet(support_client, wiki_users, groups, add_user_to_group, test_page):
    add_user_to_group(group=groups.child_group, user=wiki_users.chapson)

    data = {'slug': test_page.slug, 'username': wiki_users.chapson.username}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200

    user_membership = response.json()['user_membership']
    assert {group['name'] for group in user_membership} == {groups.child_group.name, groups.root_group.name}


def test_page_access_owner(support_client, test_page, wiki_users, org_dir_id):
    Access(page=test_page, is_owner=True).save()
    data = {'slug': test_page.slug, 'username': wiki_users.chapson.username, 'org': {'dir_id': org_dir_id}}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.json()['page_access']['type'] == 'only_authors'
    assert len(response.json()['page_access']['users']) == 0


def test_page_access_restricted(support_client, test_page, wiki_users, org_dir_id):
    Access(page=test_page, staff=wiki_users.chapson.staff).save()
    data = {'slug': test_page.slug, 'username': wiki_users.chapson.username, 'org': {'dir_id': org_dir_id}}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200
    assert response.json()['page_access']['type'] == 'custom'
    assert response.json()['page_access']['users'][0]['login'] == wiki_users.chapson.username


def test_page_access_inheritance_from_root(support_client, page_cluster, wiki_users, org_dir_id):
    Access(page=page_cluster['root'], staff=wiki_users.chapson.staff).save()
    data = {
        'slug': page_cluster['root/a'].slug,
        'username': wiki_users.chapson.username,
        'org': {'dir_id': org_dir_id},
    }
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.json()['page_access']['type'] == 'custom'
    assert response.json()['page_access']['users'][0]['login'] == wiki_users.chapson.username


def test_page_access_group(support_client, page_cluster, wiki_users, org_dir_id, groups):
    group = groups.group_org_42 if settings.IS_BUSINESS else groups.child_group
    Access(page=page_cluster['root'], group=group).save()
    data = {
        'slug': page_cluster['root/a'].slug,
        'username': wiki_users.chapson.username,
        'org': {'dir_id': org_dir_id},
    }
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200
    assert response.json()['page_access']['type'] == 'custom'
    assert response.json()['page_access']['groups'][0]['name'] == group.name


@only_biz
def test_get_info_from_another_org(support_client, wiki_users, organizations, groups, add_user_to_group):
    """Будем из 42 организации запрашивать данные из 21"""
    assert list(support_client.user.orgs.all()) == [organizations.org_42]

    wiki_users.chapson.orgs.set([organizations.org_21])
    wiki_users.chapson.save()
    add_user_to_group(group=groups.group_org_21, user=wiki_users.chapson)
    new_page = PageFactory(supertag='newpage', last_author=wiki_users.chapson, org_id=organizations.org_21.id)

    Access(page=new_page, staff=wiki_users.chapson.staff).save()  # restricted access
    Access(page=new_page, group=groups.group_org_21).save()

    data = {
        'slug': new_page.slug,
        'username': wiki_users.chapson.username,
        'org': {'dir_id': organizations.org_21.dir_id},
    }
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200
    assert response.json()['page_access']['type'] == 'custom'
    assert len(response.json()['page_access']['users']) == 1
    assert len(response.json()['page_access']['groups']) == 1
    assert len(response.json()['user_membership']) == 1


def test_write_log_to_db(support_client, org_dir_id, test_page, wiki_users):
    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug, 'username': wiki_users.chapson.username}
    response = support_client.post('/api/v2/support/explain_access', data=data)
    assert response.status_code == 200
    assert SupportLog.objects.filter(user=support_client.user, operation=SupportOperation.DIAG_403).count() == 1

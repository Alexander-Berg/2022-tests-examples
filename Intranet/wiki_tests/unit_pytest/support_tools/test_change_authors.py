import pytest

from intranet.wiki.tests.wiki_tests.common.skips import only_biz, only_intranet
from wiki.support_tools.models import SupportLog, SupportOperation
from wiki.utils.django_redlock.redlock import RedisLock

pytestmark = [pytest.mark.django_db]


def test_no_access_anonymous(client):
    response = client.post('/api/v2/support/change_authors', data={})
    assert response.status_code == 403


def test_no_access_non_support_group(client, wiki_users):
    client.login(wiki_users.asm)
    response = client.post('/api/v2/support/change_authors', data={})
    assert response.status_code == 403


def test_validation_need_any_usernames(support_client):
    data = {'remove_usernames': [], 'add_usernames': [], 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


def test_validation_need_remove_usernames_when_mode_replace(support_client):
    data = {'add_usernames': ['...'], 'mode': 'replace', 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


@only_biz
def test_validation_require_org_for_biz(support_client):
    data = {'add_usernames': ['...'], 'org_id': None, 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


@only_intranet
def test_validation_org_is_none_for_intranet(support_client):
    data = {'add_usernames': ['...'], 'org_id': '42', 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


def test_lock(support_client, org_dir_id):
    slug = 'some_supertag_for_lock'
    with RedisLock(f'ChangeAuthors: {slug}'):
        data = {'add_usernames': ['...'], 'org': {'dir_id': org_dir_id}, 'slug': slug}
        response = support_client.post('/api/v2/support/change_authors', data=data)
        assert response.status_code == 102


def test_users_not_found(support_client, org_dir_id):
    data = {'add_usernames': ['invalid_user_name'], 'org': {'dir_id': org_dir_id}, 'slug': 'root'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


@only_biz
def test_users_filter_by_org(support_client, wiki_users, organizations):
    org_21 = organizations.org_21  # where only robot_wiki

    data = {'add_usernames': [wiki_users.robot_wiki.username], 'org': {'dir_id': org_21.dir_id}, 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200

    data = {'add_usernames': [wiki_users.asm.username], 'org': {'dir_id': org_21.dir_id}, 'slug': '...'}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


@only_biz
def test_pages_filter_by_org(support_client, wiki_users, organizations, test_page):
    org_21 = organizations.org_21  # no page
    data = {
        'add_usernames': [wiki_users.robot_wiki.username],
        'org': {'dir_id': org_21.dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert len(response.json()['changes']) == 0

    org_42 = organizations.org_42  # have page
    data = {
        'add_usernames': [wiki_users.robot_wiki.username],
        'org': {'dir_id': org_42.dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert len(response.json()['changes']) == 1


def test_pages_mode_replace(support_client, wiki_users, org_dir_id, test_page):
    data = {
        'remove_usernames': [wiki_users.chapson.username],  # no author test_page
        'add_usernames': [wiki_users.asm.username],
        'mode': 'replace',
        'org': {'dir_id': org_dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert len(response.json()['changes']) == 0

    data['remove_usernames'] = [wiki_users.thasonic.username]  # author test_page
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert len(response.json()['changes']) == 2  # 2 == delete + add


def test_remove_last_author(support_client, wiki_users, org_dir_id, test_page):
    # try 1. test_page have one author == thasonic
    data = {
        'remove_usernames': [wiki_users.thasonic.username],
        'org': {'dir_id': org_dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400

    # try 2. deleting the added author. And the page also has no author
    data['add_usernames'] = [wiki_users.asm.username]
    data['remove_usernames'] += [wiki_users.asm.username]
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400

    # try 3. also fall in dry_run
    data['dry_run'] = True
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 400


def test_change_authors(support_client, wiki_users, org_dir_id, test_page):
    assert set(test_page.get_authors()) == {wiki_users.thasonic}

    data = {
        'add_usernames': [wiki_users.asm.username],
        'dry_run': False,
        'org': {'dir_id': org_dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert set(test_page.get_authors()) == {wiki_users.thasonic, wiki_users.asm}

    data = {
        'remove_usernames': [wiki_users.thasonic.username],
        'org': {'dir_id': org_dir_id},
        'dry_run': False,
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert set(test_page.get_authors()) == {wiki_users.asm}


def test_change_authors_on_cluster(support_client, wiki_users, org_dir_id, page_cluster):
    assert set(page_cluster['root/a/aa'].get_authors()) == {wiki_users.thasonic}

    data = {
        'add_usernames': [wiki_users.asm.username],
        'dry_run': False,
        'org': {'dir_id': org_dir_id},
        'slug': 'root',
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert set(page_cluster['root/a/aa'].get_authors()) == {wiki_users.thasonic, wiki_users.asm}


def test_no_change_authors_dry_run(support_client, wiki_users, org_dir_id, test_page):
    assert set(test_page.get_authors()) == {wiki_users.thasonic}

    data = {
        'add_usernames': [wiki_users.asm.username],
        'dry_run': True,
        'org': {'dir_id': org_dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert set(test_page.get_authors()) == {wiki_users.thasonic}


def test_count_logs_note(support_client, wiki_users, org_dir_id, test_page):
    data = {
        'remove_usernames': [wiki_users.chapson.username, wiki_users.thasonic.username],
        'add_usernames': [wiki_users.chapson.username, wiki_users.asm.username],
        'org': {'dir_id': org_dir_id},
        'slug': test_page.slug,
    }
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert len(response.json()['changes']) == 4  # 4 == add(chapson, asm) + delete(chapson, thasonic)


def test_write_log_to_db(support_client, wiki_users, org_dir_id, test_page):
    data = {'add_usernames': [wiki_users.asm.username], 'org': {'dir_id': org_dir_id}, 'slug': test_page.slug}
    response = support_client.post('/api/v2/support/change_authors', data=data)
    assert response.status_code == 200
    assert SupportLog.objects.filter(user=support_client.user, operation=SupportOperation.CHANGE_AUTHOR).count() == 1

import pytest

from intranet.wiki.tests.wiki_tests.common.skips import only_biz, only_intranet
from wiki.pages.utils.remove import delete_page
from wiki.support_tools.models import SupportLog, SupportOperation
from wiki.utils.django_redlock.redlock import RedisLock

pytestmark = [pytest.mark.django_db]


def test_no_access_anonymous(client):
    response = client.post('/api/v2/support/restore_page', data={})
    assert response.status_code == 403


def test_no_access_non_support_group(client, wiki_users):
    client.login(wiki_users.asm)
    response = client.post('/api/v2/support/restore_page', data={})
    assert response.status_code == 403


@only_biz
def test_validation_require_org_for_biz(support_client):
    data = {'org_id': None, 'slug': '...'}
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400


@only_intranet
def test_validation_org_is_none_for_intranet(support_client):
    data = {'slug': '...'}
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400


def test_lock(support_client, org_dir_id):
    slug = 'some_slug_for_lock'
    with RedisLock(f'RestorePage: {slug}'):
        data = {'org': {'dir_id': org_dir_id}, 'slug': slug}
        response = support_client.post('/api/v2/support/restore_page', data=data)
        assert response.status_code == 102


def test_invalid_slug(support_client, org_dir_id):
    data = {'org': {'dir_id': org_dir_id}, 'slug': 'invalid'}
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400


def test_restore_page(support_client, org_dir_id, test_page):
    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug}
    delete_page(test_page)
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 200

    test_page.refresh_from_db()
    assert test_page.status == 1


def test_restore_alive_page(support_client, org_dir_id, test_page):
    assert test_page.status == 1
    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug}
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400


@only_biz
def test_pages_filter_by_org(support_client, test_page, organizations):
    data = {'slug': test_page.slug, 'org': {'dir_id': organizations.org_21.dir_id}}
    delete_page(test_page)

    # test page only in org_42
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400

    data['org']['dir_id'] = organizations.org_42.dir_id
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 200


@only_biz
def test_pages_filter_by_another_org(support_client, test_page, organizations):
    data = {'slug': test_page.slug, 'org': {'dir_id': organizations.org_42.dir_id}}
    test_page.org = organizations.org_21
    test_page.save()

    delete_page(test_page)

    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 400

    # test page only in org_21
    data['org']['dir_id'] = organizations.org_21.dir_id
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 200


def test_write_log_to_db(support_client, org_dir_id, test_page):
    data = {'org': {'dir_id': org_dir_id}, 'slug': test_page.slug}
    delete_page(test_page)
    response = support_client.post('/api/v2/support/restore_page', data=data)
    assert response.status_code == 200
    assert SupportLog.objects.filter(user=support_client.user, operation=SupportOperation.RESTORE_PAGE).count() == 1

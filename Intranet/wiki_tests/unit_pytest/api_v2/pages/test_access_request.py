from django.conf import settings
from pprint import pprint

import mock
import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.api_v2.public.pages.access_requests.exceptions import AlreadyProcessed, AlreadyHasAccess
from wiki.intranet.models.consts import GROUP_TYPE_CHOICES

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def access_request(client, wiki_users, page_cluster):
    page = page_cluster['root/a']
    set_access_author_only(page, [wiki_users.thasonic])

    client.login(wiki_users.asm)
    response = client.post(f'/api/v2/public/pages/{page.id}/access_requests', {'reason': 'give me plz'})
    assert response.status_code == 200
    assert_json(
        response.json(),
        {'applicant': {'user': {'username': 'asm'}}, 'reason': 'give me plz', 'resolution': None, 'status': 'pending'},
    )

    return page, response.json()['id']


def test_access_request__deny(access_request, client, wiki_users, organizations, page_cluster, test_org_id):
    page, idx = access_request

    # повторный запрос должен изменять причину, но не менять ID
    response = client.post(f'/api/v2/public/pages/{page.id}/access_requests', {'reason': 'give me plz one more time'})
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'applicant': {'user': {'username': 'asm'}},
            'reason': 'give me plz one more time',
            'id': idx,
            'status': 'pending',
        },
    )

    response = client.get(f'/api/v2/public/pages/{page.id}/access_requests/{idx}')
    assert response.status_code == 403

    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}/access_requests/{idx}', {'reason': 'give me plz'})
    assert response.status_code == 200

    response = client.post(f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process', {'decision': 'deny'})
    pprint(response.json())
    assert response.status_code == 200
    assert_json(
        response.json(),
        {
            'applicant': {'user': {'username': 'asm'}},
            'reason': 'give me plz one more time',
            'resolution': {'comment': None, 'decision': 'deny', 'processed_by': {'username': 'thasonic'}},
            'status': 'processed',
        },
    )
    response = client.post(f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process', {'decision': 'deny'})
    assert response.status_code == 400
    assert response.json()['error_code'] == AlreadyProcessed.error_code


def test_already_has_access(access_request, client, wiki_users, organizations, page_cluster, test_org_id):
    page, idx = access_request

    set_access_author_only(page, [wiki_users.thasonic, wiki_users.asm])

    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process', {'decision': 'deny'})
    assert response.status_code == 400
    assert response.json()['error_code'] == AlreadyHasAccess.error_code


def test_allow(access_request, client, wiki_users, organizations, page_cluster, test_org_id):
    page, idx = access_request

    client.login(wiki_users.asm)
    response = client.get(f'/api/v2/public/pages/{page.id}')
    assert response.status_code == 403

    client.login(wiki_users.thasonic)
    response = client.post(
        f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process', {'decision': 'allow_applicant'}
    )
    assert response.status_code == 200

    client.login(wiki_users.asm)
    response = client.get(f'/api/v2/public/pages/{page.id}')
    assert response.status_code == 200


def test_allow_groups(access_request, groups, add_user_to_group, client, wiki_users, page_cluster):
    page, idx = access_request

    client.login(wiki_users.thasonic)
    if settings.IS_INTRANET:
        group_id = groups.root_group.id
        add_user_to_group(groups.root_group, wiki_users.asm)
    else:
        group_id = groups.group_org_42.dir_id
        add_user_to_group(groups.group_org_42, wiki_users.asm)

    response = client.post(
        f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process',
        {'decision': 'allow_groups', 'groups': [group_id], 'departments': []}
    )
    assert response.status_code == 200

    client.login(wiki_users.asm)
    with mock.patch('wiki.pages.access.cache.cache_is_enabled', lambda: False):
        response = client.get(f'/api/v2/public/pages/{page.id}')
    assert response.status_code == 200


@only_intranet
def test_department(access_request, groups, add_user_to_group, client, wiki_users, page_cluster, test_org_id):
    page, idx = access_request

    groups.child_group.type = GROUP_TYPE_CHOICES.DEPARTMENT
    groups.child_group.save()

    groups.root_group.type = GROUP_TYPE_CHOICES.DEPARTMENT
    groups.root_group.save()

    groups.side_group.type = GROUP_TYPE_CHOICES.SERVICE
    groups.side_group.save()

    add_user_to_group(groups.child_group, wiki_users.asm)
    add_user_to_group(groups.side_group, wiki_users.asm)

    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}/access_requests/{idx}')
    assert_json(
        response.json(),
        {
            'applicant': {
                'departments': [
                    {'name': 'main_group', 'type': 'department'},
                    {'name': 'child_group', 'type': 'department'},
                ],
                'services': [{'name': 'side_group', 'type': 'service'}],
                'user': {'username': 'asm'},
            },
            'status': 'pending',
        },
    )

    client.login(wiki_users.thasonic)
    response = client.post(
        f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process',
        {'decision': 'allow_groups', 'groups': [str(groups.side_group.id)]},
    )
    assert response.status_code == 200


def test_must_check_permissions(access_request, client, wiki_users, organizations, page_cluster, test_org_id):
    page, idx = access_request

    client.login(wiki_users.kolomeetz)
    response = client.post(
        f'/api/v2/public/pages/{page.id}/access_requests/{idx}/process', {'decision': 'allow_applicant'}
    )
    assert response.status_code == 403

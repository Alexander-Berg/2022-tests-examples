from pprint import pprint

import pytest

from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json, in_any_order
from intranet.wiki.tests.wiki_tests.common.skips import only_biz, only_intranet


@only_intranet
@pytest.mark.django_db
def test_organization_group_dep__intranet(client, wiki_users, page_cluster, organizations, intranet_groups):
    client.login(wiki_users.thasonic)
    intranet_groups.root_group.members.add(wiki_users.thasonic.staff, wiki_users.asm.staff)

    response = client.get(f'/api/v2/public/organization/groups/{intranet_groups.root_group.id}')
    assert response.status_code == 200
    pprint(response.json())
    assert_json(
        response.json(),
        {
            'members': {
                'results': in_any_order(
                    [
                        {'username': 'asm'},
                        {'username': 'thasonic'},
                    ]
                )
            },
            'metadata': {'externals_count': 0, 'url': None},
            'name': 'main_group',
            'type': 'wiki',
        },
    )


@only_biz
@pytest.mark.django_db
def test_organization_group_dep__biz(client, wiki_users, page_cluster, organizations, business_groups):
    client.login(wiki_users.thasonic)
    business_groups.group_org_42.user_set.add(wiki_users.thasonic, wiki_users.asm)
    business_groups.department_org_42.user_set.add(wiki_users.robot_wiki, wiki_users.volozh)

    response = client.get(f'/api/v2/public/organization/groups/{business_groups.group_org_21.dir_id}')
    assert response.status_code == 404

    response = client.get(f'/api/v2/public/organization/groups/{business_groups.group_org_42.dir_id}')
    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'members': {
                'has_next': False,
                'page_id': 1,
                'results': in_any_order([{'username': 'thasonic'}, {'username': 'asm'}]),
            },
            'metadata': {'dir_id': '5624'},
            'name': 'Accounting',
            'type': 'group',
        },
    )

    response = client.get(f'/api/v2/public/organization/groups/{business_groups.department_org_42.dir_id}')
    assert response.status_code == 404
    response = client.get(f'/api/v2/public/organization/departments/{business_groups.department_org_42.dir_id}')
    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'members': {'results': in_any_order([{'username': 'volozh'}, {'username': 'robot-wiki'}])},
            'metadata': {'dir_id': '5625'},
            'name': 'DevOps',
            'type': 'department',
        },
    )


@only_biz
@pytest.mark.django_db
def test_checkmembership__biz(client, wiki_users, page_cluster, organizations, business_groups):
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/organization/check_membership?cloud_uid={wiki_users.asm.cloud_uid}')
    assert response.status_code == 200
    assert response.json()['is_member']

    client.login(wiki_users.thasonic, organizations.org_21)
    response = client.get(f'/api/v2/public/organization/check_membership?cloud_uid={wiki_users.asm.cloud_uid}')
    assert response.status_code == 200
    assert not response.json()['is_member']

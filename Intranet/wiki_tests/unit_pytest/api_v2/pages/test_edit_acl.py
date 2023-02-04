import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_custom
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json, in_any_order
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.pages.access.exceptions import OutstaffRuleViolation


@pytest.mark.django_db
def test_page_edit__acl__403(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']

    client.login(wiki_users.kolomeetz)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.robot_wiki.get_uid(),
                    }
                ],
            }
        },
    )

    assert response.status_code == 403


@pytest.mark.django_db
def test_page_edit__acl(client, wiki_users, page_cluster, organizations, groups, intranet_outstaff_manager):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.asm.get_uid(),
                    }
                ],
            }
        },
    )
    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [{'username': 'asm'}],
            }
        },
    )

    assert response.status_code == 200

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': False,
            }
        },
    )
    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': False,
                'acl_type': 'default',
                'users': [],
            }
        },
    )

    assert response.status_code == 200

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl', {'acl': {'break_inheritance': True, 'acl_type': 'only_authors'}}
    )
    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'only_authors',
                'users': [],
            }
        },
    )

    assert response.status_code == 200


@only_intranet
@pytest.mark.django_db
def test_page_edit__acl__outstaff_rule(
    client, wiki_users, page_cluster, organizations, groups, intranet_outstaff_manager
):
    # чтобы в интранете добавить аутстафф-пользователя или группу с аутстаффом надо быть аутстафф менеджером
    # wiki_users.kolomeetz - аутстафф менеджер
    # wiki_users.thasonic - неаутстафф менеджер

    page = page_cluster['root/a/ad']
    page.authors.add(wiki_users.kolomeetz)
    intranet_outstaff_manager.user_set.add(wiki_users.kolomeetz)
    intranet_outstaff_manager.user_set.remove(wiki_users.thasonic)

    # 1. не аутстафф манагер - 400

    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.robot_wiki.get_uid(),
                    }
                ],
            }
        },
    )

    assert response.status_code == 400
    assert response.json()['error_code'] == OutstaffRuleViolation.error_code

    # 2. аутстафф манагер - 200

    client.login(wiki_users.kolomeetz)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.robot_wiki.get_uid(),
                    }
                ],
            }
        },
    )

    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [{'username': 'robot-wiki'}],
            }
        },
    )

    assert response.status_code == 200

    # 3. неаутстафф манагер - 200. разрешается добавлять не-аутстафф если права уже настроены

    client.login(wiki_users.thasonic)
    #

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.robot_wiki.get_uid(),
                    },
                    {
                        'uid': wiki_users.asm.get_uid(),
                    },
                ],
            }
        },
    )

    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': in_any_order(
                    [
                        {'username': 'asm'},
                        {'username': 'robot-wiki'},
                    ]
                ),
            }
        },
    )

    # 4. неаутстафф манагер - 200. разрешается удалять не-аутстафф

    client.login(wiki_users.thasonic)
    #

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.asm.get_uid(),
                    },
                ],
            }
        },
    )

    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': in_any_order(
                    [
                        {'username': 'asm'},
                    ]
                ),
            }
        },
    )


@pytest.mark.django_db
def test_page_edit__nochanges(client, wiki_users, page_cluster, organizations, groups, intranet_outstaff_manager):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)
    set_access_custom(page, users=[wiki_users.asm, wiki_users.thasonic])

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [
                    {
                        'uid': wiki_users.asm.get_uid(),
                    },
                    {
                        'uid': wiki_users.thasonic.get_uid(),
                    },
                ],
            }
        },
    )
    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'users': [{'username': 'asm'}, {'username': 'thasonic'}],
            }
        },
    )

    assert response.status_code == 200


@pytest.mark.django_db
def test_page_edit__is_readonly(client, wiki_users, page_cluster, organizations, groups, intranet_outstaff_manager):
    page = page_cluster['root/a/ad']
    set_access_custom(page, users=[wiki_users.asm, wiki_users.thasonic])
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages/{page.id}?fields=acl')
    assert_json(
        response.json(),
        {'acl': {'is_readonly': False}},
    )

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=acl',
        {
            'acl': {
                'is_readonly': True,
            }
        },
    )
    assert_json(
        response.json(),
        {
            'acl': {
                'break_inheritance': True,
                'acl_type': 'custom',
                'is_readonly': True,
                'users': [{'username': 'asm'}, {'username': 'thasonic'}],
            }
        },
    )

    assert response.status_code == 200

from idm.core.constants.node_relocation import RELOCATION_STATE
from idm.core.models import System, Role
from idm.core.node_pipeline import NodePipeline

from idm.tests.utils import mock_tree, refresh

GROUP_NAME = 'OU=group1'
GROUP_NAME2 = 'OU=group2'

TREE_INITIAL = {
    "slug": "role",
    "name": "роль",
    "values": {
        "OU=group1": {
            "name": "OU=group1",
            "roles": {
                "slug": "group_roles",
                "name": {
                    "ru": "Роли группы",
                    "en": "Group roles"
                },
                "values": {
                    "member": {
                        "name": {
                            "ru": "Участник",
                            "en": "Member"
                        }
                    },
                    "responsible": {
                        "name": {
                            "ru": "Ответственный",
                            "en": "Responsible"
                        }
                    }
                }
            }
        },
        GROUP_NAME2: {
            "name": GROUP_NAME2,
            "roles": {
                "slug": "group_roles",
                "name": {
                    "ru": "Роли группы",
                    "en": "Group roles"
                },
                "values": {
                    "member": {
                        "name": {
                            "ru": "Участник",
                            "en": "Member"
                        }
                    },
                    "responsible": {
                        "name": {
                            "ru": "Ответственный",
                            "en": "Responsible"
                        }
                    }
                }
            }
        }
    }}

TREE_WITH_UNIQUE_ID = {
    "slug": "role",
    "name": "роль",
    "values": {
        "OU=group1": {
            "unique_id": "OU=group1",
            "name": "OU=group1",
            "roles": {
                "slug": "group_roles",
                # "unique_id": "OU=group1::group_roles",
                "name": {
                    "ru": "Роли группы",
                    "en": "Group roles"
                },
                "values": {
                    "member": {
                        "unique_id": "OU=group1::member",
                        "name": {
                            "ru": "Участник",
                            "en": "Member"
                        }
                    },
                    "responsible": {
                        "unique_id": "OU=group1::responsible",
                        "name": {
                            "ru": "Ответственный",
                            "en": "Responsible"
                        }
                    }
                }
            }
        },
        GROUP_NAME2: {
            "unique_id": GROUP_NAME2,
            "name": GROUP_NAME2,
            "roles": {
                "slug": "group_roles",
                "name": {
                    "ru": "Роли группы",
                    "en": "Group roles"
                },
                "values": {
                    "member": {
                        "unique_id": f"{GROUP_NAME2}::member",
                        "name": {
                            "ru": "Участник",
                            "en": "Member"
                        }
                    },
                    "responsible": {
                        "unique_id": f"{GROUP_NAME2}::responsible",
                        "name": {
                            "ru": "Ответственный",
                            "en": "Responsible"
                        }
                    }
                }
            }
        }
    }}


NEW_TREE = {
    'slug': 'type',
    'name': 'тип роли',
    'values': {
        'global': {
            'unique_id': 'global',
            'name': 'global',
            'roles': {
                'slug': 'role',
                'name': 'роль',
                'values': {
                    'system_group_relation': {
                        'name': {'en': 'system-group relation', 'ru': 'связь системы с группой'},
                        'unique_id': 'system_group_relation',
                        'fields': [
                            {'is_required': True,
                             'name': {'ru': 'слаг системы', 'en': 'system slug'},
                             'slug': 'system'},
                            {'is_required': True, 'name': {'ru': 'DN группы', 'en': 'group DN'},
                             'slug': 'group_dn'}]}}}},

        'roles_in_groups': {
            'unique_id': 'roles_in_groups',
            'name': 'roles_in_groups',
            'roles': {
                'slug': 'ad_group',
                'name': 'AD-группа',
                'values': {
                    'OU=group1': {
                        'name': 'OU=group1',
                        'unique_id': 'OU=group1',
                        'roles': {
                            'slug': 'group_roles',
                            'name': {'ru': 'Роли группы', 'en': 'Group roles'},
                            'values': {
                                'member': {
                                    'unique_id': 'OU=group1::member',
                                    'name': {'ru': 'Участник', 'en': 'Member'}
                                },
                                'responsible': {
                                    'unique_id': 'OU=group1::responsible',
                                    'name': {'ru': 'Ответственный', 'en': 'Responsible'}
                                }
                            }
                        }
                    },
                    'OU=group2': {
                        'name': 'OU=group2',
                        'unique_id': 'OU=group2',
                        'roles': {
                            'slug': 'group_roles',
                            'name': {'ru': 'Роли группы', 'en': 'Group roles'},
                            'values': {
                                'member': {
                                    'unique_id': 'OU=group2::member',
                                    'name': {'ru': 'Участник', 'en': 'Member'}},
                                'responsible': {
                                    'unique_id': 'OU=group2::responsible',
                                    'name': {'ru': 'Ответственный', 'en': 'Responsible'}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


def test_migration(simple_system: System, arda_users):
    """
    добавляем unique_id в узлы дерева AD-system, когда они прорастают в IMD, перемещаем дерево,
    убеждаемся что роли не сломались
    """
    frodo = arda_users.frodo
    simple_system.nodes.exclude(parent_id__isnull=True).all().delete()
    with mock_tree(simple_system, {"code": 0, "roles": TREE_INITIAL}):
        simple_system.synchronize(force_update=True)
        r = Role.objects.request_role(frodo, frodo, simple_system, '', {
            'role': GROUP_NAME,
            'group_roles': 'responsible'
        })
        assert refresh(r).state == 'granted'

    # в дерево добавляем unique_id
    with mock_tree(simple_system, {"code": 0, "roles": TREE_WITH_UNIQUE_ID}):
        simple_system.synchronize(force_update=True)
        r = refresh(r)
        assert r.state == 'granted'
        assert r.node.slug_path == '/role/' \
                                   f'{GROUP_NAME}' \
                                   '/group_roles/responsible/'

    # узлы групп переезжают по дереву
    with mock_tree(simple_system, {"code": 0, "roles": NEW_TREE}):
        # root = simple_system.root_role_node
        # root.relocation_state = RELOCATION_STATE.SUPERDIRTY
        # root.save(update_fields=['relocation_state'])
        # NodePipeline(simple_system).run()
        simple_system.synchronize(force_update=True)
        r = refresh(r)
        assert r.state == 'granted'
        assert r.node.slug_path == '/type/roles_in_groups' \
                                   f'/ad_group/{GROUP_NAME}' \
                                   '/group_roles/responsible/'

        # проверим что механизм запроса ролей не сломался
        member_role = Role.objects.request_role(frodo, arda_users.sam, simple_system, '', {
            'type': 'roles_in_groups',
            'ad_group': GROUP_NAME2,
            'group_roles': 'member'
        })
        member_role = refresh(member_role)
        assert member_role.state == 'granted'
        assert member_role.node.slug_path == '/type/roles_in_groups' \
                                             f'/ad_group/{GROUP_NAME2}' \
                                             '/group_roles/member/'

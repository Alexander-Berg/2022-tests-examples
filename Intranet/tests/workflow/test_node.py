# coding: utf-8


import pytest

from idm.core.workflow.exceptions import RoleNodeDoesNotExist, Forbidden
from idm.core.models import Role, RoleNode, ApproveRequest
from idm.core.workflow.plain.node import NodeWrapper
from idm.tests.utils import raw_make_role, create_system, set_workflow, refresh

pytestmark = pytest.mark.django_db


@pytest.fixture
def bunker():
    terminator = {
        'name': 'Выбор узла завершен',
        'roles': {
            'name': 'Уровень доступа',
            'slug': 'role',
            'values': {
                'grant': 'Управление правами',
                'publish': 'Публикация узлов',
                'store': 'Редактирование данных',
            }
        }
    }

    data = {
        'code': 0,
        'roles': {
            'slug': 'level0',
            'name': 'level0',
            'values': {
                '*': terminator,
                'project1': {
                    'name': 'Project 1',
                    'roles': {
                        'slug': 'level1',
                        'name': 'Project 1',
                        'values': {
                            '*': terminator,
                            'subproject1': {
                                'name': 'Subproject 1',
                                'roles': {
                                    'slug': 'level2',
                                    'name': 'Subproject 1',
                                    'values': {
                                        '*': terminator,
                                        'rabbithole': {
                                            'name': 'Deep down the rabbit hole',
                                            'roles': {
                                                'slug': 'level3',
                                                'values': {
                                                    '*': terminator
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            'subproject2': {
                                'name': 'Subproject 2',
                                'roles': {
                                    'slug': 'level2',
                                    'name': 'Subproject 2',
                                    'values': {
                                        '*': terminator
                                    }
                                }
                            }
                        }
                    }
                },
                'project2': {
                    'slug': 'level1',
                    'name': 'Project 2',
                    'values': {
                        '*': terminator,
                    }
                }
            }
        }
    }

    system = create_system('bunker', role_tree=data)
    return system


def test_bunkerlike_system(bunker, arda_users):
    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, bunker, '', {'level0': '*', 'role': 'grant'}, None)
    Role.objects.request_role(frodo, frodo, bunker, '', {'level0': 'project1',
                                                         'level1': '*',
                                                         'role': 'store'}, None)
    Role.objects.request_role(frodo, frodo, bunker, '', {'level0': 'project1',
                                                         'level1': 'subproject1',
                                                         'level2': '*',
                                                         'role': 'grant'}, None)
    Role.objects.request_role(frodo, frodo, bunker, '', {'level0': 'project1',
                                                         'level1': 'subproject1',
                                                         'level2': 'rabbithole',
                                                         'level3': '*',
                                                         'role': 'grant'}, None)
    with pytest.raises(Forbidden):
        Role.objects.request_role(frodo, frodo, bunker, '', {'level0': 'project1'}, None)
    with pytest.raises(RoleNodeDoesNotExist):
        Role.objects.request_role(frodo, frodo, bunker, '', {'level0': 'project1',
                                                             'level1': 'subproject1',
                                                             'level2': 'grant'}, None)


def test_get_owners_same_level(bunker, arda_users):
    """Проверим работу get_owners() для того же уровня, где и интересующий нас узел"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    store_node = RoleNode.objects.get_node_by_data(bunker, {'level0': '*', 'role': 'store'})
    grant_node = RoleNode.objects.get_node_by_data(bunker, {'level0': '*', 'role': 'grant'})
    store_wrapper = NodeWrapper(store_node)
    grant_wrapper = NodeWrapper(grant_node)

    # по умолчанию владельцев нет
    assert store_wrapper.get_owners('grant') == []
    assert grant_wrapper.get_owners('grant') == []

    # наличие ролей на другие листья не задевает работу get_owners
    raw_make_role(legolas, bunker, {'level0': '*', 'role': 'publish'}, state='granted')
    assert store_wrapper.get_owners('grant') == []
    assert grant_wrapper.get_owners('grant') == []

    # наличие неактивных ролей тоже:
    requested_role = raw_make_role(frodo, bunker, {'level0': '*', 'role': 'grant'}, state='requested')
    assert store_wrapper.get_owners('grant') == []
    assert grant_wrapper.get_owners('grant') == []
    requested_role.deprive_or_decline(None, bypass_checks=True)

    # а нужная роль приводит к ожидаемому результату
    raw_make_role(frodo, bunker, {'level0': '*', 'role': 'grant'}, state='granted')
    assert store_wrapper.get_owners('grant') == ['frodo']
    assert grant_wrapper.get_owners('grant') == ['frodo']

    # мы можем посчитать granting-слагом какой-то другой, тогда и результат будет другим
    assert store_wrapper.get_owners('publish') == ['legolas']
    assert grant_wrapper.get_owners('publish') == ['legolas']

    # даже если выдадим ещё одну роль на другой лист frodo, то результат будет всё равно тот же
    raw_make_role(frodo, bunker, {'level0': '*', 'role': 'publish'}, state='granted')
    assert store_wrapper.get_owners('grant') == ['frodo']
    assert grant_wrapper.get_owners('grant') == ['frodo']

    # но, конечно, владельцев может быть больше одного
    raw_make_role(arda_users.varda, bunker, {'level0': '*', 'role': 'grant'}, state='granted')
    assert store_wrapper.get_owners('grant') == ['frodo', 'varda']
    assert grant_wrapper.get_owners('grant') == ['frodo', 'varda']


def test_get_owners_look_up(bunker, arda_users):
    """Проверим работу get_owners(), "смотрящего" вверх"""

    data = {
        'level0': 'project1',
        'level1': 'subproject1',
        'level2': 'rabbithole',
        'level3': '*',
        'role': 'publish'
    }
    raw_make_role(arda_users.frodo, bunker, data, state='granted')
    node = RoleNode.objects.get_node_by_data(bunker, data)
    wrapper = NodeWrapper(node)
    assert wrapper.get_owners('grant', look_up=True) == []

    role = raw_make_role(arda_users.legolas, bunker, dict(data, role='grant'), state='granted')
    # даже смотрящий вверх get_owners() выдаст роль на своём уровне
    assert wrapper.get_owners('grant', look_up=True) == ['legolas']
    role.delete()
    assert wrapper.get_owners('grant', look_up=True) == []

    top_data = {
        'level0': 'project1',
        'level1': '*',
        'role': 'grant',
    }
    upper_data = {
        'level0': 'project1',
        'level1': 'subproject1',
        'level2': '*',
        'role': 'grant',
    }
    unrelated_data = {
        'level0': 'project1',
        'level1': 'subproject2',
        'level2': '*',
        'role': 'grant'
    }
    raw_make_role(arda_users.gandalf, bunker, upper_data, state='granted')
    raw_make_role(arda_users.varda, bunker, top_data, state='granted')
    raw_make_role(arda_users.gimli, bunker, unrelated_data, state='granted')

    assert wrapper.get_owners('grant') == []
    # если смотрим вверх, то вернется первый владелец вверх по дереву
    assert wrapper.get_owners('grant', look_up=True) == ['gandalf']
    # если включен collect, то вернутся все владельцы вверх по дереву
    assert wrapper.get_owners('grant', look_up=True, collect=True) == ['gandalf', 'varda']
    # collect без look_up не имеет смысла, но работает так же, как look_up=False + collect=False
    assert wrapper.get_owners('grant', look_up=False, collect=True) == []

    same_level_data = {
        'level0': 'project1',
        'level1': 'subproject1',
        'level2': 'rabbithole',
        'level3': '*',
        'role': 'grant'
    }
    raw_make_role(arda_users.legolas, bunker, same_level_data, state='granted')

    # если include_self == True (дефолт), вернутся все владельцы, в том числе и той же ноды
    assert wrapper.get_owners('grant', look_up=True, collect=True) == ['legolas', 'gandalf', 'varda']
    # если include_self == False, владельца той же ноды не будет
    assert wrapper.get_owners('grant', look_up=True, collect=True, include_self=False) == ['gandalf', 'varda']


def test_groups(bunker, arda_users, department_structure):
    """Проверим работу get_owners для групп"""

    fellowships = department_structure.fellowship
    valinor = department_structure.valinor
    associations = department_structure.associations
    earth = department_structure.earth
    shire = department_structure.shire

    data = {
        'level0': 'project1',
        'level1': 'subproject1',
        'level2': 'rabbithole',
        'level3': '*',
        'role': 'publish'
    }
    raw_make_role(fellowships, bunker, data, state='granted')
    node = RoleNode.objects.get_node_by_data(bunker, data)
    wrapper = NodeWrapper(node)
    assert wrapper.get_owners('grant', look_up=True, groups=True) == []

    granting_data = dict(data, role='grant')
    role = raw_make_role(valinor, bunker, granting_data, state='granted')
    # пользовательская роль на результат влиять не будет
    raw_make_role(arda_users.frodo, bunker, granting_data, state='granted')
    # даже смотрящий вверх get_owners() выдаст роль на своём уровне
    assert wrapper.get_owners('grant', look_up=True, groups=True) == [valinor.external_id]
    role.delete()
    assert wrapper.get_owners('grant', look_up=True, groups=True) == []

    top_data = {
        'level0': 'project1',
        'level1': '*',
        'role': 'grant',
    }
    upper_data = {
        'level0': 'project1',
        'level1': 'subproject1',
        'level2': '*',
        'role': 'grant',
    }
    unrelated_data = {
        'level0': 'project1',
        'level1': 'subproject2',
        'level2': '*',
        'role': 'grant'
    }
    raw_make_role(shire, bunker, upper_data, state='granted')
    raw_make_role(associations, bunker, top_data, state='granted')
    raw_make_role(earth, bunker, unrelated_data, state='granted')

    assert wrapper.get_owners('grant', groups=True) == []
    # если смотрим вверх, то вернется первый владелец вверх по дереву
    assert wrapper.get_owners('grant', groups=True, look_up=True) == [shire.external_id]
    # если включен collect, то вернутся все владельцы вверх по дереву
    assert wrapper.get_owners('grant', groups=True, look_up=True, collect=True) == [shire.external_id,
                                                                                    associations.external_id]
    # collect без look_up не имеет смысла, но работает так же, как look_up=False + collect=False
    assert wrapper.get_owners('grant', groups=True, look_up=False, collect=True) == []


def test_get_responsibilities(arda_users):
    frodo = arda_users.frodo

    system = create_system('cauth', name='CAuth', group_policy='aware', role_tree={
        'code': 0,
        'roles': {
            'slug': 'dst',
            'name': 'Назначение',
            'values': {
                'server1': {
                    'name': 'server1',
                    'responsibilities': [{
                        'username': 'legolas',
                        'notify': True,
                    }, {
                        'username': 'gandalf',
                        'notify': False,
                    }, {
                        'username': 'gimli',
                        'notify': True,
                    }]
                },
            }
        }
    })
    gimli = arda_users.gimli
    gimli.is_active = False
    gimli.save()

    set_workflow(system, 'approvers = [any_from([resp.user for resp in node.get_responsibilities()])]')
    role = Role.objects.request_role(frodo, frodo, system, '', {'dst': 'server1'}, None)
    role = refresh(role)
    assert role.state == 'requested'
    approvers = (
        ApproveRequest.objects.filter(approve__role_request__role=role).values_list('approver__username', flat=True)
    )
    assert set(approvers) == {'legolas', 'gandalf'}

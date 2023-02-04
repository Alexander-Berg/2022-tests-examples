# -*- coding: utf-8 -*-


import mock
import pytest
from django.conf import settings
from django.core.management import call_command
from freezegun import freeze_time

from idm.core.models import Role, RoleNode, Action
from idm.inconsistencies.models import Inconsistency, MatchingRole
from idm.core.queues import RoleNodeQueue
from idm.core.canonical import CanonicalNode
from idm.tests.utils import (create_user, refresh, set_workflow, make_role, raw_make_role,
                             mock_all_roles, mock_tree, assert_inconsistency, compare_time, mock_roles)


# разрешаем использование базы в тестах
pytestmark = [
    pytest.mark.django_db,
    pytest.mark.robot,
    pytest.mark.parametrize('audit_backend', ('database', 'memory')),
]

OUR, THEIR = Inconsistency.TYPE_OUR, Inconsistency.TYPE_THEIR


@pytest.mark.parametrize('subject_types', [False, True])
def test_compare_several_our_roles_matches_one_system_role(simple_system, arda_users, department_structure,
                                                           audit_backend, subject_types):
    """ Проверим, что если у нас есть несколько одинаковых ролей, соответствующих одной системной, то
    неконсистентностей не создаётся"""

    simple_system.use_tvm_role = subject_types
    simple_system.audit_backend = audit_backend
    simple_system.save()
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    group_role = raw_make_role(fellowship, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'admin'}, parent=group_role)

    roles = [{
        'login': 'frodo',
        'subject_type': ('user' if subject_types else None),
        'roles': [{'role': 'admin'}]
    }]
    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 0


def test_compare_several_our_roles_matches_no_system_role(simple_system, arda_users, department_structure,
                                                          audit_backend):
    """ Проверим, что если у нас есть несколько одинаковых ролей, не соответствующих ни одной системной, то
    создаётся только одна неконсистентность"""

    simple_system.audit_backend = audit_backend
    simple_system.save()
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    group_role = raw_make_role(fellowship, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'admin'}, parent=group_role)

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert Inconsistency.objects.select_related('our_role__parent').get().our_role.parent is not None


def test_check_roles_before_sync(simple_system, arda_users, audit_backend):
    """ Тест функции сверки ролей. """

    simple_system.audit_backend = audit_backend
    simple_system.save()

    assert Inconsistency.objects.count() == 0

    roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [{'role': 'admin'}],
    }]
    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(inconsistency, user=arda_users.frodo, system=simple_system, path='/role/admin/',
                         state='active', type=Inconsistency.TYPE_THEIR, ident_type='data')

    report = Inconsistency.objects.get_report(simple_system)
    assert report == {
        simple_system: {
            'system_has_we_dont': {
                arda_users.frodo: [inconsistency]
            },
            'format': 'full',
        }
    }


def test_passport_login_case_sensitivity(simple_system, arda_users, audit_backend):
    """ Тест, проверяющий, что регистр не влияет на паспортные логины """

    simple_system.audit_backend = audit_backend
    simple_system.save()
    frodo = arda_users.frodo

    roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'admin'}, {'passport-login': 'yndx-FRoDo'}],
        ],
    }]
    raw_make_role(frodo, simple_system, {'role': 'admin'}, {'passport-login': 'yndx-frodo'},
                  system_specific={'passport-login': 'yndx-frodo'})
    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 0


def test_check_roles_w_unaware_system(simple_system, arda_users, audit_backend):
    """Тест функции сверки для unaware системы, тем не менее отдающей групповые роли"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    user_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            {'role': 'manager'}
        ]
    }]
    group_roles = [{
        'group': 777,
        'roles': [
            {'role': 'admin'}
        ]
    }]

    with mock_all_roles(simple_system, user_roles=user_roles, group_roles=group_roles):
        Inconsistency.objects.check_roles()

    # должна появиться одна неконсистентность, про пользователя
    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        system=simple_system,
        user=arda_users.frodo,
        path='/role/manager/',
        remote_fields=None,
    )


def test_unaware_system_match(simple_system, arda_users, department_structure, audit_backend):
    """Проверим совпадения и расхождения с обеих сторон в unaware-системе"""

    system = simple_system
    system.audit_backend = audit_backend
    system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    # у нас будет:
    # совпадающая роль для пользователя
    make_role(frodo, system, {'role': 'manager'}, {'login': 'frodo'})
    # групповая роль
    make_role(fellowship, system, {'role': 'manager'}, {'login': 'fellowship'})
    user_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'login': 'frodo'}],
            [{'role': 'manager'}, {'login': 'fellowship'}],
        ]
    }]
    for member in fellowship.members.all():
        if member != frodo:
            user_roles.append({
                'login': member.username,
                'subject_type': 'user',
                'roles': [
                    [{'role': 'manager'}, {'login': 'fellowship'}]
                ],
            })
    # роль для пользователя, которая есть только у нас
    our_role = make_role(legolas, system, {'role': 'admin'}, {'login': 'legolas'})
    # роль для пользователя и для группы, которая есть только в системе
    user_roles.append({
        'login': 'sauron',
        'subject_type': 'user',
        'roles': [
            [{'role': 'superuser'}, {'login': 'sauron'}]
        ]
    })
    group_roles = [{
        'group': shire.external_id,
        'roles': [
            [{'role': 'superuser'}, {'login': 'shire'}]
        ]
    }]

    with mock_all_roles(system, user_roles=user_roles, group_roles=group_roles):
        Inconsistency.objects.check_roles()
    # должно получиться две неконсистентности: одна про нашу роль для пользователя, другая про роль из системы
    assert Inconsistency.objects.count() == 2
    ic1 = Inconsistency.objects.get(type=Inconsistency.TYPE_THEIR)
    assert_inconsistency(ic1, remote_username='sauron', remote_fields={'login': 'sauron'}, path='/role/superuser/',
                         system=simple_system, user=arda_users.sauron)
    ic2 = Inconsistency.objects.get(type=Inconsistency.TYPE_OUR)
    assert_inconsistency(ic2, path='/role/admin/', our_role=our_role)
    assert compare_time(ic1.added, ic2.added, epsilon=30)


def test_aware_system_match(aware_simple_system, arda_users, department_structure, audit_backend):
    """Проверим совпадения и расхождения с обеих сторон в aware-системе"""

    system = aware_simple_system
    system.audit_backend = audit_backend
    system.save()
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    shire = department_structure.shire
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    # у нас будет:
    # совпадающая роль для пользователя и для группы
    make_role(fellowship, system, {'role': 'manager'}, {'login': 'fellowship'})
    make_role(frodo, system, {'role': 'manager'}, {'login': 'frodo'})
    user_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'login': 'frodo'}]
        ]
    }]
    group_roles = [{
        'group': fellowship.external_id,
        'roles': [
            [{'role': 'manager'}, {'login': 'fellowship'}]
        ]
    }]
    # роль для пользователя и для группы, которая есть только у нас
    group_role = make_role(valinor, system, {'role': 'admin'}, {'login': 'valinor'})
    user_role = make_role(legolas, system, {'role': 'admin'}, {'login': 'legolas'})
    # роль для пользователя и для группы, которая есть только в системе
    user_roles.append({
        'login': 'sauron',
        'subject_type': 'user',
        'roles': [
            [{'role': 'superuser'}, {'login': 'sauron'}]
        ]
    })
    group_roles.append({
        'group': shire.external_id,
        'roles': [
            [{'role': 'superuser'}, {'login': 'shire'}]
        ]
    })

    with mock_all_roles(system, user_roles=user_roles, group_roles=group_roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 4
    assert Inconsistency.objects.our().count() == 2
    assert Inconsistency.objects.their().count() == 2
    our_user = Inconsistency.objects.our().get(group=None)
    our_group = Inconsistency.objects.our().get(user=None)
    their_user = Inconsistency.objects.their().get(group=None)
    their_group = Inconsistency.objects.their().get(user=None)
    assert_inconsistency(our_user, system=system, our_role=user_role)
    assert_inconsistency(our_group, system=system, our_role=group_role)
    assert_inconsistency(their_user, system=system, user=arda_users.sauron, path='/role/superuser/',
                         remote_fields={'login': 'sauron'})
    assert_inconsistency(their_group, system=system, group=shire, path='/role/superuser/',
                         remote_fields={'login': 'shire'})
    assert compare_time(our_user.added, their_user.added, epsilon=60)
    assert compare_time(our_group.added, their_group.added, epsilon=60)


def test_check_roles_when_system_specific_data_differs(simple_system, arda_users, audit_backend):
    """ Тест функции сверки ролей когда роли различаются только в части system_specific данных. """

    simple_system.audit_backend = audit_backend
    simple_system.save()
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'manager'},
                         system_specific={'passport-login': 'our_login'}, state='granted')
    all_roles = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [
                [{'role': 'manager'}, {'passport-login': 'system_login'}]
            ]
        }
    ]

    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 2
    our = Inconsistency.objects.get(type=Inconsistency.TYPE_OUR)
    their = Inconsistency.objects.get(type=Inconsistency.TYPE_THEIR)
    assert_inconsistency(our, user=arda_users.frodo, system=simple_system, path='/role/manager/',
                         our_role=role, remote_fields=None)
    assert_inconsistency(their, user=arda_users.frodo, system=simple_system, path='/role/manager/',
                         our_role=None, remote_fields={'passport-login': 'system_login'})


def test_check_roles_when_login_is_not_found(simple_system, audit_backend):
    """Тестирует создание инконсистентности при получении ролей пользователей, не заведенных в IDM
        Важно, чтобы пользователя с таким логином не было в базе
    """

    simple_system.audit_backend = audit_backend
    simple_system.save()

    all_roles = [{
        'login': 'fantom',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'passport-login': 'minor'}]
        ]
    }]

    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        user=None,
        system=simple_system,
        type=Inconsistency.TYPE_UNKNOWN_USER,
        remote_username='fantom'
    )

    report = Inconsistency.objects.get_report(simple_system)
    assert report == {
        simple_system: {
            'system_has_unknown_user': [inconsistency],
            'format': 'full',
        }
    }

    # попробуем еще раз и убедимся, что активная неконсистентность всё ещё одна (а вторая закрылась как obsolete)
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 2
    old_one = refresh(inconsistency)
    assert old_one.state == 'obsolete'
    new_inconsistency = Inconsistency.objects.active().get()

    assert_inconsistency(
        new_inconsistency,
        user=None,
        system=simple_system,
        type=Inconsistency.TYPE_UNKNOWN_USER,
        remote_username='fantom',
        state='active'
    )

    # теперь создадим такого пользователя и проверим, что неконсистентность закроется
    # (но откроется неконсистентность роли)
    fantom = create_user('fantom')
    all_roles = [{
        'login': 'fantom',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'passport-login': 'minor'}]
        ]
    }]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 3
    inconsistency = refresh(inconsistency)
    new_inconsistency = refresh(new_inconsistency)
    assert inconsistency.state == 'obsolete'
    assert new_inconsistency.state == 'obsolete'
    role_inconsistency = Inconsistency.objects.active().get()

    assert_inconsistency(
        role_inconsistency,
        user=fantom,
        system=simple_system,
        path='/role/manager/',
        remote_fields={'passport-login': 'minor'},
    )

    report = Inconsistency.objects.get_report(simple_system)
    assert report == {
        simple_system: {
            'system_has_we_dont': {
                fantom: [role_inconsistency]
            },
            'format': 'full',
        }
    }


def test_check_group_inconsistency(aware_simple_system, audit_backend):
    """Тестирует закрытие инконсистентности group_id, когда система перестает её отдавать"""

    aware_simple_system.audit_backend = audit_backend
    aware_simple_system.save()

    all_roles = [{
        'group': 42,
        'roles': [
            [{'role': 'manager'}, {'login': 'minor'}]
        ]
    }]
    with mock_all_roles(aware_simple_system, group_roles=all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        user=None,
        group=None,
        system=aware_simple_system,
        state='active',
        remote_group=42,
    )
    assert inconsistency.type == Inconsistency.TYPE_UNKNOWN_GROUP

    # если неправильная группа из системы пропала, то неконсистентность должна закрыться
    with mock_all_roles(aware_simple_system, []):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'
    assert Inconsistency.objects.active().count() == 0


def test_wrong_format_of_passport_logins(simple_system, arda_users, audit_backend):
    """Проверим случай, когда система отдаём нам паспортные логины с точками"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    for fieldname in ('passport-login', 'passportLogin'):
        Inconsistency.objects.all().delete()
        all_roles = [{
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [
                [{'role': 'manager'}, {fieldname: 'yndx.frodo.the.ring.bearer'}]
            ]
        }]

        with mock_all_roles(simple_system, all_roles):
            Inconsistency.objects.check_roles()
        inconsistency = Inconsistency.objects.active().get()
        assert_inconsistency(
            inconsistency,
            system=simple_system,
            user=arda_users.frodo,
            path='/role/manager/',
            remote_fields={'passport-login': 'yndx-frodo-the-ring-bearer'}
        )


def test_check_roles_when_password_missing(generic_system, arda_users, audit_backend):
    """Система не хранит у себя пароль, и мы должны игнорировать его отсутствие при сверке."""
    generic_system.audit_backend = audit_backend
    generic_system.save()
    set_workflow(generic_system)

    with mock.patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 0,
            'data': {
                'passport-login': 'yndx-frodo',
                'my-password': 'mellon',
                'password': 'hello, world',
                'unrelated': 'funny pic',
            }
        }
        role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, generic_system, '', {'role': 'manager'}, None)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific == {'passport-login': 'yndx-frodo', 'unrelated': 'funny pic'}
    all_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'passport-login': 'yndx-frodo', 'unrelated': 'funny pic'}]
        ]
    }]

    with mock_all_roles(generic_system, all_roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.active().count() == 0


def test_check_roles_when_login_in_system_missing(simple_system, arda_users, audit_backend):
    """Проверка для случая, когда у нас есть роль, а система не отдаёт даже логин этого сотрудника"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'manager'},
                         system_specific={'passport-login': 'yndx-frodo'}, state='granted')

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        user=arda_users.frodo,
        type=Inconsistency.TYPE_OUR,
        system=simple_system,
        path='/role/manager/',
        our_role=role,
    )


def test_check_roles_when_login_in_system_missing_and_appear(simple_system, arda_users, audit_backend):
    """ Это тот случай, когда сотрудник почему-то пропал из системы, мы обнаружили неконсистентность,
    а потом он снова появился. При этом мы пометили неконсистентность как obsolete и не создали новой.
    """
    simple_system.audit_backend = audit_backend
    simple_system.save()

    raw_make_role(arda_users.frodo, simple_system, {'role': 'manager'},
                  system_specific={'passport-login': 'yndx-frodo'}, state='granted')

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()

    all_roles = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [
                [{'role': 'manager'}, {'passport-login': 'yndx-frodo'}]
            ]
        }
    ]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'

    assert Inconsistency.objects.active().count() == 0


def test_resolve_inconsistency_when_role_disappear_from_system(simple_system, arda_users, audit_backend):
    """Роль была в выдаче системы, мы завели неконсистентность, запросили роль,
    роль пропала из выдачи системы - надо удалить и закрыть неконсистентность"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    set_workflow(simple_system, 'approvers = [approver("frodo")]')

    all_roles = [
        {
            'login': 'legolas',
            'subject_type': 'user',
            'roles': [
                [{'role': 'admin'}, {}]
            ]
        }
    ]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    # должна создаться неконсистентность
    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        system=simple_system,
        user=arda_users.legolas,
        type=Inconsistency.TYPE_THEIR,
        path='/role/admin/',
        remote_fields=None,
        state='active',
    )

    # теперь роль пропадает из выдачи системы
    all_roles = [{
        'login': 'legolas',
        'subject_type': 'user',
        'roles': []
    }]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    # неконсистентность должна закрыться
    assert Inconsistency.objects.count() == 1
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'
    assert Role.objects.count() == 0


def test_resolve_inconsistency_when_role_disappear_from_system_with_another(simple_system, arda_users, audit_backend):
    """Роль была в выдаче системы, мы завели неконсистентность, запросили роль,
    роль пропала из выдачи системы - надо удалить и закрыть неконсистентность.
    Плюс существует неконсистентность на нашей стороне"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    all_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [
            [{'role': 'admin'}, {}]
        ]
    }]
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'},
                         system_specific={'passport-login': 'yndx-frodo'}, state='granted')
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    # должна создаться неконсистентность
    assert Inconsistency.objects.count() == 2
    their_inconsistency = Inconsistency.objects.get(type=Inconsistency.TYPE_THEIR)
    our_inconsistency = Inconsistency.objects.get(type=Inconsistency.TYPE_OUR)
    assert_inconsistency(
        their_inconsistency,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/admin/'
    )
    assert_inconsistency(
        our_inconsistency,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/admin/',
        our_role=role,
    )

    # теперь роль пропадает из выдачи системы
    all_roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': []
    }]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    # неконсистентности должны закрыться, неконсистентность на нашей стороне остаётся
    assert Inconsistency.objects.count() == 3
    assert Inconsistency.objects.active().count() == 1
    role = refresh(role)
    assert role.state == 'granted'


def test_check_roles_when_login_in_upper_case(simple_system, arda_users, audit_backend):
    """Тестируем инвариантность регистра логина при аудите ролей"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'manager'},
                         system_specific={'passport-login': 'yndx-frodo'}, state='granted')

    all_roles = [{
        'login': 'FRODO',
        'subject_type': 'user',
        'roles': [
            [{'role': 'manager'}, {'passport-login': 'frodo.baggins'}]
        ]
    }]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 2
    our, their = (Inconsistency.objects.active().get(type=x) for x in (OUR, THEIR))
    assert_inconsistency(
        our,
        user=arda_users.frodo,
        system=simple_system,
        our_role=role,
        path='/role/manager/',
    )
    assert_inconsistency(
        their,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/manager/',
        remote_fields={'passport-login': 'frodo-baggins'}
    )


def test_if_audit_is_broken_we_should_not_resolve_inconsistencies(simple_system, arda_users, audit_backend):
    """Если проверка системы ломается где-то посередине, мы не должны разрешать (даже пытаться разрешать)
    неконсистентности"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    roles = [{
        'login': 'frodo',
        'subject_type': 'user',
        'roles': [{'role': 'admin'}]
    }]
    with mock_all_roles(simple_system, roles):
        module = 'audit' if audit_backend == 'database' else 'audit_memory'
        with mock.patch('idm.inconsistencies.{}.InconsistencyProcessor.process'.format(module)) as process:
            process.side_effect = ValueError
            call_command('idm_check_and_resolve')
            assert Action.objects.filter(action='started_sync_with_system').count() == 0


def test_not_account_new_roles_as_inconsistencies(simple_system, arda_users, audit_backend):
    simple_system.audit_backend = audit_backend
    simple_system.save()

    frodo = arda_users.frodo
    sam = arda_users.sam
    peregrin = arda_users.peregrin
    meriadoc = arda_users.meriadoc
    gimli = arda_users.gimli
    legolas = arda_users.legolas

    # Достаточно старые роли
    with freeze_time("1999-12-31 23:59:59"):
        raw_make_role(frodo, simple_system, {'role': 'admin'})
        raw_make_role(gimli, simple_system, {'role': 'admin'}, state='depriving')
        raw_make_role(meriadoc, simple_system, {'role': 'admin'}, state='deprived')

    # Слишком новые роли
    with freeze_time("3000-01-01 00:00:01"):
        raw_make_role(sam, simple_system, {'role': 'admin'})
        raw_make_role(legolas, simple_system, {'role': 'admin'}, state='depriving')
        raw_make_role(peregrin, simple_system, {'role': 'admin'}, state='deprived')

    roles = [
        {
            'login': 'meriadoc',
            'subject_type': 'user',
            'roles': [{'role': 'admin'}]
        },
        {
            'login': 'peregrin',
            'subject_type': 'user',
            'roles': [{'role': 'admin'}]
        },
    ]
    with freeze_time("2999-12-31 23:59:59"):
        with mock_all_roles(simple_system, roles):
            Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 3
    assert {item.user for item in Inconsistency.objects.all()} == {frodo, gimli, meriadoc}


def test_system_lost_node_and_its_roles(simple_system, arda_users, audit_backend):
    """ Проверим, что если система потеряла узел и роли, выданные на него (=> узел в статусе depriving),
    то неконсистентности по этим ролям не ломают систему и не разрешаются"""

    simple_system.audit_backend = audit_backend
    simple_system.save()

    assert not simple_system.is_broken

    # Условия того, что система может сломаться
    assert len(arda_users.values()) > settings.IDM_SYSTEM_BREAKDOWN_INCONSYSTENCY_COUNT
    assert simple_system.inconsistency_policy != 'trust'

    for user in arda_users.values():
        raw_make_role(user, simple_system, {'role': 'admin'})

    node = RoleNode.objects.get(slug='admin')
    node.state = 'depriving'  # Как будто система не отдала нам узел
    node.save(update_fields=['state'])

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()

    # Неконсистентности создались
    assert Inconsistency.objects.count() == len(arda_users.values())

    # Но система не ломается
    simple_system = refresh(simple_system)
    assert not simple_system.is_broken

    # И мы не разрешаем такие неконсистентности
    assert Inconsistency.objects.resolvable_for_system(simple_system).count() == 0
    assert Inconsistency.objects.unresolvable_for_system(simple_system).count() == Inconsistency.objects.count()

    # Проверяем, что они действительно не разрешаются
    Inconsistency.objects.resolve_system(simple_system)
    assert Inconsistency.objects.active().count() == Inconsistency.objects.count()


def test_role_with_unknown_node(simple_system, arda_users, audit_backend):
    simple_system.audit_backend = audit_backend
    simple_system.save()

    # выданные роли на активные и пока что не умершие узлы
    poweruser_role = raw_make_role(arda_users.gandalf, simple_system, {'role': 'poweruser'})
    admin_role = raw_make_role(arda_users.manve, simple_system, {'role': 'admin'})
    # отозванная роль, так как узел будет тоже отозван
    superuser_role = raw_make_role(arda_users.varda, simple_system, {'role': 'superuser'}, state='deprived')

    # Узел в самом расцвете сил
    poweruser = RoleNode.objects.get(slug='poweruser')
    assert poweruser.state == 'active'

    # Еле-еле живой узел
    admin = RoleNode.objects.get(slug='admin')
    admin.state = 'depriving'
    admin.save(update_fields=['state'])

    # Уже дохлый
    superuser = RoleNode.objects.get(slug='superuser')
    superuser.state = 'deprived'
    superuser.save(update_fields=['state'])

    # На стороне системы последние два узла удаляются
    tree = simple_system.plugin.get_info()
    del tree['roles']['values']['admin']
    del tree['roles']['values']['superuser']

    # Роли не меняются
    roles = [
        {
            'login': 'gandalf',
            'subject_type': 'user',
            'roles': [{'role': 'poweruser'}]
        },
        {
            'login': 'manve',
            'subject_type': 'user',
            'roles': [{'role': 'admin'}]
        },
        {
            'login': 'varda',
            'subject_type': 'user',
            'roles': [{'role': 'superuser'}]
        },
    ]

    with mock_tree(simple_system, tree):
        with mock_all_roles(simple_system, roles):
            Inconsistency.objects.check_roles()

    # Только deprived узлы считаются нам неизвестными
    assert Inconsistency.objects.count() == 1
    i1 = Inconsistency.objects.get(type=Inconsistency.TYPE_UNKNOWN_ROLE)
    assert i1.remote_data == {'role': 'superuser'}


def test_check_system_which_cannot_be_broken(simple_system, arda_users, audit_backend):
    """Проверка флага can_be_broken"""
    simple_system.audit_backend = audit_backend
    simple_system.save()
    for user in arda_users:
        raw_make_role(arda_users[user], simple_system, {'role': 'poweruser'})
    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == len(arda_users)
    simple_system = refresh(simple_system)
    assert simple_system.is_broken

    Inconsistency.objects.all().delete()
    simple_system.is_broken = False
    simple_system.can_be_broken = False
    simple_system.save()

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == len(arda_users)
    simple_system = refresh(simple_system)
    assert not simple_system.is_broken


def test_with_new_plugin(generic_new_system, department_structure, audit_backend):
    """ Проверим, что использовании нового плагина сверка работает корректно"""
    generic_new_system.audit_backend = audit_backend
    generic_new_system.save()
    fellowship = department_structure.fellowship
    raw_make_role(fellowship, generic_new_system, {'role': 'admin'}, with_inheritance=False)
    raw_make_role(fellowship, generic_new_system, {'role': 'manager'})
    roles = [
        {
            'group': fellowship.external_id,
            'path': '/role/admin/',
            'with_inheritance': False,
            'with_robots': True,
            'with_external': True,
        },
        {
            'group': fellowship.external_id,
            'path': '/role/manager/',
            'with_inheritance': False,
            'with_robots': True,
            'with_external': True,
        },
    ]
    with mock_roles(generic_new_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 2


def test_check_roles_if_matching_role_from_another_sync_exists_unaware(
        simple_system,
        arda_users,
        department_structure,
        audit_backend):
    """ Проверим, что если предыдущий синк упал и matching_role не удалились,
    в новом синке мы не будем их учитывать. Для unaware систем"""
    simple_system.audit_backend = audit_backend
    simple_system.save()

    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'})
    action = Action.objects.create(id=-1)
    inconsistency = Inconsistency.objects.create(
        system=simple_system,
        state='active',
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        sync_key_id=action.id,
    )
    MatchingRole.objects.create(role_id=role.id, inconsistency_id=inconsistency.id)

    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 2
    new_inconsistency = Inconsistency.objects.exclude(sync_key_id=action.id)[0]
    assert_inconsistency(new_inconsistency, user=frodo, system=simple_system, path='/role/admin/',
                         state='active', type=Inconsistency.TYPE_OUR, ident_type='data')


def test_check_roles_if_matching_role_from_another_sync_exists_aware(
        aware_simple_system,
        arda_users,
        department_structure,
        audit_backend):
    """ Проверим, что если предыдущий синк упал и matching_role не удалились,
    в новом синке мы не будем их учитывать. Для aware систем"""
    aware_simple_system.audit_backend = audit_backend
    aware_simple_system.save()
    frodo = arda_users.frodo
    role = raw_make_role(frodo, aware_simple_system, {'role': 'admin'})
    action = Action.objects.create(id=-1)
    inconsistency = Inconsistency.objects.create(
        system=aware_simple_system,
        state='active',
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        sync_key_id=action.id,
    )
    MatchingRole.objects.create(role_id=role.id, inconsistency_id=inconsistency.id)

    with mock_all_roles(aware_simple_system, []):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 2
    new_inconsistency = Inconsistency.objects.exclude(sync_key_id=action.id)[0]
    assert_inconsistency(new_inconsistency, user=frodo, system=aware_simple_system, path='/role/admin/',
                         state='active', type=Inconsistency.TYPE_OUR, ident_type='data')


@pytest.mark.parametrize('audit_method', ['get_all_roles', 'get_roles'])
def test_active_node_priority(client, simple_system, arda_users, department_structure, audit_method, audit_backend):
    """ В ситуации, когда у нас есть две одинаковые ноды в статусах active, depriving, роли системы должны
    линковаться к active ноде."""
    simple_system.audit_backend = audit_backend
    simple_system.audit_method = audit_method
    simple_system.save()
    frodo = arda_users.frodo
    node = simple_system.nodes.get(slug='admin')
    node.mark_depriving()
    node.refresh_from_db()
    assert node.state == 'depriving'

    queue = RoleNodeQueue(system=simple_system)
    canonical_node = CanonicalNode(hash='', slug='admin', name='admin', name_en='admin')
    queue.push_addition(node=node.parent, child_data=canonical_node, in_auto_mode=True)
    queue.apply(user=frodo, from_api=True)

    raw_make_role(frodo, simple_system, {'role': 'admin'})
    mock_methods = {'get_all_roles': mock_all_roles, 'get_roles': mock_roles}

    if audit_method == 'get_all_roles':
        roles = [{'login': 'frodo', 'subject_type': 'user', 'roles': [{'role': 'admin'}]}]
    else:
        roles = [{'login': 'frodo', 'subject_type': 'user', 'path': '/role/admin/'}]

    with mock_methods[audit_method](simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 0


def test_ours_with_deprived_nodes(simple_system, arda_users, audit_backend):
    """ При наличии активной нашей роли на отозванном узле расхождение должно создасться корректно"""
    simple_system.audit_backend = audit_backend
    simple_system.save()
    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'superuser'})
    admin_node = RoleNode.objects.get(slug='admin')
    assert admin_node.state == 'active'
    admin_node.state = 'deprived'
    admin_node.save(update_fields=['state'])
    roles = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [{'role': 'superuser'}]
        },
    ]

    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    inc = Inconsistency.objects.get()
    assert inc.type == 'we_have_system_dont'
    assert inc.remote_data is None
    assert inc.node == admin_node


def test_uid_support(simple_system, arda_users, audit_backend):
    subject_type = 'user'
    simple_system.audit_backend = audit_backend
    simple_system.save()
    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'superuser'})  # consistent
    raw_make_role(frodo, simple_system, {'role': 'admin'})  # IDM has, system hasn't
    frodo.uid = 'fr0d0'
    frodo.save(update_fields=['uid'])
    data = [
        {
            'uid': 'fr0d0',
            'subject_type': subject_type,
            'roles': [{'role': 'superuser'}]  # consistent
        },
        {
            'uid': 'fr0d0',
            'subject_type': subject_type,
            'roles': [{'role': 'manager'}]  # system has; IDM hasn't
        },
        {
            'uid': 'deadbeef',
            'subject_type': subject_type,
            'roles': [{'role': 'admin'}]  # unknown user
        },
    ]
    with mock_all_roles(simple_system, data):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 3
    assert Inconsistency.objects.get(type=OUR).node.slug == 'admin'
    assert Inconsistency.objects.get(type=THEIR).remote_data == {'role': 'manager'}
    assert Inconsistency.objects.get(type=Inconsistency.TYPE.UNKNOWN_USER).remote_uid == 'deadbeef'
    assert Inconsistency.objects.get(type=Inconsistency.TYPE.UNKNOWN_USER).remote_username == 'uid:deadbeef'


@pytest.mark.parametrize('use_tvm_role', [False, True])
def test_subject_type(simple_system, arda_users, audit_backend, use_tvm_role):
    """
    https://st.yandex-team.ru/IDM-9485 сверка не работает если передать subject_type
    """
    simple_system.audit_backend = audit_backend
    simple_system.use_tvm_role = use_tvm_role
    simple_system.save()
    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'superuser'})  # consistent
    raw_make_role(frodo, simple_system, {'role': 'admin'})  # IDM has, system hasn't
    data = [
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [{'role': 'superuser'}]  # consistent
        },
        {
            'login': 'frodo',
            'subject_type': 'user',
            'roles': [{'role': 'manager'}]  # system has; IDM hasn't
        }
    ]
    with mock_all_roles(simple_system, data):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 2
    assert Inconsistency.objects.get(type=OUR).node.slug == 'admin'
    assert Inconsistency.objects.get(type=THEIR).remote_data == {'role': 'manager'}

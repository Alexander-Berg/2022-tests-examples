# coding: utf-8


from textwrap import dedent

import pytest

from idm.core.models import RoleField, Workflow
from idm.users.models import GroupResponsibility
from idm.core.workflow.common.subject import subjectify
from idm.core.workflow.plain.user import userify
from idm.core.workflow.shortcuts import run_doctests, workflow_check, run_doctests_check
from idm.tests.utils import create_user, set_workflow, add_perms_by_role, refresh
import idm.users.ranks as rank_constants

pytestmark = pytest.mark.django_db

""" Тесты доктестов """


def test_doctests(simple_system):
    assert run_doctests('"""\n>>> True\nTrue\n\n"""', simple_system) == (True, '')
    expected_message = '''**********************************************************************
Line 3, in workflow.py
Failed example:
    True
Expected:
    False
Got:
    True
'''
    result = run_doctests('"""\n>>> True\nFalse\n\n"""', simple_system)
    assert result == (False, expected_message)
    expected_message = '''**********************************************************************
Line 3, in workflow.py
Failed example:
    some_value
Expected:
    True
Got:
    False
'''
    assert run_doctests('"""\n>>> some_value\nTrue\n\n"""', simple_system, some_value=False) == (False, expected_message)


def test_incorrect_workflow(simple_system):
    """Проверим результат workflow с ошибкой"""
    create_user('terran')

    # случай синтаксической ошибки в workflow
    workflow_text = 'approvers = approver("terran")]'

    result = run_doctests(
        workflow_text,
        simple_system,
        check=lambda: workflow_check(
            workflow_text,
            requester=userify('terran'),
            subject=userify('terran'),
            role_data={'role': 'manager'},
            system=simple_system,
        )
    )
    assert result[0] is False

    # случай невыполняющихся тестов
    workflow_text = '''
"""
>>> check()
[approver('terran')]

"""
approvers = []
'''

    result = run_doctests(
        workflow_text,
        simple_system,
        check=lambda: workflow_check(
            workflow_text,
            requester=userify('terran'),
            subject=userify('terran'),
            role_data={'role': 'manager'},
            system=simple_system,
        )
    )
    assert result[0] is False


def test_save_workflow_with_doctest(simple_system, arda_users):
    """Проверим сохранение workflow с доктестами"""

    set_workflow(simple_system)
    frodo = arda_users.frodo
    add_perms_by_role('responsible', frodo, simple_system)

    simple_system = refresh(simple_system)
    wf_dev = simple_system.clone_workflow(frodo)
    wf_dev.workflow = dedent('''
    approvers = ['legolas']

    """
    >>> run(user('frodo'), user('frodo'), {'role': 'admin'})
    [approver(legolas, priority=1)]

    """
    ''')
    wf_dev.save()
    wf_dev = Workflow.objects.select_related('system__actual_workflow', 'user').get(pk=wf_dev.pk)
    wf_dev.commit(frodo)
    wf_dev.approve(frodo)


def test_additional_parameters(simple_system, arda_users):
    admin_node = simple_system.nodes.get(slug='admin')
    field = RoleField(type='charfield', slug='param', node=admin_node)
    field.save()

    workflow_text = r'''

"""
>>> run(user('frodo'), user('frodo'), {'role': 'admin'}, expected_fields=['approvers', 'ttl_days'], fields_data={'param': 'supervalue'})
[[approver(legolas, priority=1)], 7]

"""
"""
>>> run(user('frodo'), user('frodo'), {'role': 'admin'}, expected_fields=['approvers', 'email_cc'], fields_data={})
[[approver(sam, priority=1)], {'granted': [{'email': 'legolas@example.com', 'lang': 'ru', 'pass_to_personal': False}]}]

"""

if fields_data.get('param') == 'supervalue':
    approvers = ['legolas']
    ttl_days = 7
else:
    approvers = ['sam']
    email_cc = ['legolas@example.com']
'''

    result, _ = run_doctests_check(workflow_text, system=simple_system)
    assert result


@pytest.mark.parametrize('success', [True, False])
def test_group_code(simple_system, arda_users, department_structure, success):
    frodo = arda_users.frodo
    lands = department_structure.lands
    gr = GroupResponsibility.objects.create(user=frodo, group=lands, rank=rank_constants.HEAD, is_active=True)
    gr.save()

    workflow_text = r'''
heads = group.get_heads()
approvers = heads

"""
>>> run(user('sam'), group('lands'), {'role': 'admin'})
[approver(%s, priority=1)]

"""
''' % ('frodo' if success else 'gandalf')

    set_workflow(simple_system, group_code=workflow_text)
    if success:
        simple_system.actual_workflow.test()
    else:
        with pytest.raises(ValueError):
            simple_system.actual_workflow.test()

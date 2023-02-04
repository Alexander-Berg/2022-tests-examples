# coding: utf-8
from datetime import timedelta
from textwrap import dedent

import pytest
from django.conf import settings
from django.core.cache import cache
from django.utils import timezone

from idm.core.constants.role import ROLE_STATE
from idm.core.constants.system import SYSTEM_REQUEST_POLICY
from idm.core.constants.workflow import DEFAULT_PRIORITY
from idm.core.workflow.exceptions import AccessDenied, RecipientValidationError, NoApproversDefined, WorkflowError
from idm.core.models import Role, RoleField
from idm.core.workflow.plain.approver import Approver
from idm.core.workflow.plain.user import userify
from idm.core.workflow.shortcuts import workflow
from idm.framework.requester import requesterify
from idm.monitorings.metric import OverlengthedRefRoleChainMetric
from idm.permissions.shortcuts import can_deprive_role
from idm.tests.utils import create_user, set_workflow, raw_make_role, add_perms_by_role

pytestmark = pytest.mark.django_db

__doc__ = """
Тесты механизмов вызова workflow, которые относятся к логике выполнения workflow, а не к интерфейсам *Wrapper
или функциям, которые предоставляются в *Context
"""


def get_data_after_applying_workflow(workflow_, superuser_node):
    """
        Функция принимает текстовый workflow, прогоняет его и возвращает данные.
        Используется вместе с фикстурами superuser_node, arda_users
    """
    return workflow(workflow_, requester=userify('gandalf'), subject=userify('galadriel'),
                    role_data={'role': 'manager'},
                    system=superuser_node.system,
                    node=superuser_node,
                    )


def test_simple_workflow(simple_system, users_for_test, superuser_node):
    """
    workflow function smoke tests
    """
    (art, fantom, terran, admin) = users_for_test
    result = workflow('result = [1,2,3]; approvers = []', None, simple_system, art, art, node=superuser_node)
    assert result.get('result') == [1, 2, 3]
    result = workflow("result = ['sasha', 'masha']; approvers = []", None, simple_system, art, art, node=superuser_node)
    assert result.get('result') == ['sasha', 'masha']


def test_prevention_of_sending_two_email_to_one_approver(superuser_node):
    """Тестируем предотвращение отправки двух и более писем с подтверждениями одному аппруверу путем упрощения approvers"""
    create_user('user1')
    create_user('user2')
    create_user('user3')
    create_user('user4')
    create_user('petya')
    create_user('vasya')

    # случай повторяющихся аппрувером с логикой И
    workflow_ = "approvers = [approver('user1'), approver('user1')]"

    assert workflow(workflow_, requester=userify('vasya'), subject=userify('petya'),
                    role_data={'role': 'manager'},
                    system=superuser_node.system,
                    node=superuser_node,
                    )['approvers'] == [Approver('user1')]

    # случай объединения логики И и ИЛИ
    workflow_ = "approvers = [approver('user1') | approver('user2'), approver('user1')]"
    assert workflow(workflow_, requester=userify('vasya'), subject=userify('petya'),
                    role_data={'role': 'manager'},
                    system=superuser_node.system,
                    node=superuser_node,
                    )['approvers'] == [Approver('user1')]

    # случай совсем странный
    workflow_ = "approvers = [approver('user1') | approver('user2'), approver('user1'), approver('user2')]"
    wf_data = workflow(workflow_,
                       requester=userify('vasya'),
                       subject=userify('petya'),
                       role_data={'role': 'manager'},
                       system=superuser_node.system,
                       node=superuser_node)
    assert wf_data['approvers'] == [Approver('user1'), Approver('user2')]

    # случай пустого workflow
    workflow_ = "approvers = []"
    wf_data = workflow(workflow_,
                       requester=userify('vasya'),
                       subject=userify('petya'),
                       role_data={'role': 'manager'},
                       system=superuser_node.system,
                       node=superuser_node)
    assert wf_data['approvers'] == []

    # случай на всякий случай
    workflow_ = ("approvers = [approver('user1') | approver('user2') | approver('user3'),"
                 "approver('user1'), approver('user2'), "
                 "approver('user1') | approver('user2') | approver('user3') | approver('user4')]")
    wf_data = workflow(workflow_,
                       requester=userify('vasya'),
                       subject=userify('petya'),
                       role_data={'role': 'manager'},
                       system=superuser_node.system,
                       node=superuser_node)
    assert wf_data['approvers'] == [Approver('user1'), Approver('user2')]

    # случай двух пересекающихся OR-групп. В этом случае мы оставляем всё как есть и сохраняем порядок аппруверов
    # В этом случае обязанность проверить, что нет дублирования, возлагается не на код workflow,
    # а на код, который в дальнейшем обрабатывает полученные workflow данные
    workflows_ = (
        (
            "approvers = [approver('user1') | approver('user2'), approver('user1') | approver('user3')]",
            [Approver('user1') | Approver('user2'), Approver('user1') | Approver('user3')]
        ),
        (
            "approvers = [approver('user2') | approver('user1'), approver('user3') | approver('user1')]",
            [Approver('user2') | Approver('user1'), Approver('user3') | Approver('user1')]
        )
    )
    for workflow_, expected in workflows_:
        assert workflow(
            workflow_,
            requester=userify('vasya'),
            subject=userify('petya'),
            role_data={'role': 'manager'},
            system=superuser_node.system,
            node=superuser_node,
        )['approvers'] == expected


def test_raise_access_denied_in_workflow(superuser_node):
    """Проверим выброс AccessDenied в workflow"""
    create_user('terran')

    workflow_text = "raise AccessDenied(u'Доступ запрещен')"

    with pytest.raises(AccessDenied):
        workflow(workflow_text, requester=userify('terran'), subject=userify('terran'), role_data={'role': 'manager'},
                 node=superuser_node)


def test_access_denied_is_converted_to_unicode_and_str():
    obj = AccessDenied('некоторый текст')
    assert repr(obj) == 'AccessDenied(' + repr('некоторый текст') + ',)'
    assert str(obj) == 'некоторый текст'


def test_email_cc_in_workflow(superuser_node):
    """Тестируем различные варианты email_cc в workflow"""
    create_user('terran')
    create_user('zerg')

    workflow_data = workflow(
        'approvers = approver("zerg")',
        requester=userify('terran'),
        subject=userify('terran'),
        role_data={'role': 'manager'},
        system=superuser_node.system,
        node=superuser_node,
    )
    assert workflow_data.get('email_cc') == {}

    workflow_data = workflow(
        'email_cc = "zerg@example.yandex.ru"; approvers = []',
        requester=userify('terran'),
        subject=userify('terran'),
        role_data={'role': 'manager'},
        system=superuser_node.system,
        node=superuser_node,
    )
    assert workflow_data.get('email_cc') == {
        'granted': [{
            'lang': 'ru',
            'email': 'zerg@example.yandex.ru',
            'pass_to_personal': False
        }]
    }

    params = {
        'requester': userify('zerg'),
        'subject': userify('terran'),
        'role_data': {'role': 'manager'},
        'system': superuser_node.system,
        'node': superuser_node,
    }
    workflow_data = workflow('email_cc = requester; approvers = []', **params)
    assert workflow_data.get('email_cc') == {
        'granted': [{
            'lang': 'ru',
            'email': 'zerg@example.yandex.ru',
            'pass_to_personal': False
        }]
    }

    workflow_data = workflow('email_cc = [requester, "mainboss@example.yandex.ru"]; approvers = []', **params)
    assert workflow_data.get('email_cc') == {
        'granted': [{
            'lang': 'ru',
            'email': 'zerg@example.yandex.ru',
            'pass_to_personal': False
        }, {
            'lang': 'ru',
            'email': 'mainboss@example.yandex.ru',
            'pass_to_personal': False
        }]
    }
    workflow_data = workflow(dedent('''
    email_cc = [recipient(requester, granted=True),
    recipient("mainboss@example.yandex.ru", requested=True),
    recipient("someone@example.yandex.ru", granted=True, requested=True)]
    approvers=[]'''), **params)
    assert workflow_data.get('email_cc') == {
        'granted': [{
            'lang': 'ru',
            'email': 'zerg@example.yandex.ru',
            'pass_to_personal': False
        }, {
            'lang': 'ru',
            'email': 'someone@example.yandex.ru',
            'pass_to_personal': False
        }],
        'requested': [{
            'lang': 'ru',
            'email': 'mainboss@example.yandex.ru',
            'pass_to_personal': False
        }, {
            'lang': 'ru',
            'email': 'someone@example.yandex.ru',
            'pass_to_personal': False
        }]
    }
    workflow_data = workflow('email_cc = [recipient(requester, lang="en", sent=True)]; approvers=[]', **params)
    assert workflow_data.get('email_cc') == {
        'sent': [{
            'lang': 'en',
            'email': 'zerg@example.yandex.ru',
            'pass_to_personal': False
        }]
    }
    workflow_data = workflow('email_cc = [recipient(requester, lang="en")]; approvers=[]', **params)
    assert workflow_data.get('email_cc') == {}
    with pytest.raises(RecipientValidationError):
        workflow('email_cc = [recipient(requester, lang="tr")]; approvers=[]', **params)
    with pytest.raises(RecipientValidationError):
        workflow('email_cc = [recipient(requester, wtf=True)]; approvers=[]', **params)


def test_previous(simple_system, users_for_test):
    w1 = set_workflow(simple_system, "approvers = [approver('art')]")
    w2 = set_workflow(simple_system, "approvers = [approver('terran')]")

    assert w1.id == w2.parent_id


def test_hidden_role_with_approvers(pt1_system, users_for_test):
    (art, fantom, terran, admin) = users_for_test
    set_workflow(pt1_system, 'approvers = [approver("art")]')

    role = raw_make_role(fantom, pt1_system, {'project': 'proj2', 'role': 'invisible_role'}, state='requested')

    with pytest.raises(AccessDenied):
        role.apply_workflow(fantom)


def test_ignore_approvers(simple_system, arda_users):
    """Проверим режим игнорирования подтверждающих"""
    frodo = arda_users.frodo

    with pytest.raises(NoApproversDefined):
        workflow('', None, simple_system, frodo, frodo)

    result = workflow('', None, simple_system, frodo, frodo, ignore_approvers=True)
    assert 'approvers' not in result


def test_order(simple_system, arda_users, superuser_node):

    frodo = arda_users.frodo

    workflow_ = "approvers = [any_from([]) | approver('frodo')]"
    assert workflow(workflow_, None, simple_system,
                    frodo, frodo, node=superuser_node)['approvers'] == [Approver('frodo')]

    workflow_ = "approvers = [approver('frodo') | any_from([])]"
    assert workflow(workflow_, None, simple_system,
                    frodo, frodo, node=superuser_node)['approvers'] == [Approver('frodo')]


def test_workflow_with_priorities(arda_users, superuser_node):

    # несколько человек с приоритетом по умолчанию(0)
    # им всем проставится приоритет по порядку слева направо
    workflow_ = "approvers = approver('bilbo') | 'sam' | approver('frodo', notify=True)"

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    assert len(approvers) == 1
    group = approvers[0]

    expected_priorities = {
        'bilbo': 1,
        'sam': 2,
        'frodo': 3,
    }

    for approver in group:
        assert approver.priority == expected_priorities[approver.user.username]

    # несколько сотрудников с разными приоритетами
    workflow_ = "approvers = [approver('bilbo', priority=1) | 'gandalf' | approver('sam', priority=2, notify=True)]"

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    assert len(approvers) == 1
    group = approvers[0]
    expected_priorities = {
        'bilbo': 1,
        'gandalf': DEFAULT_PRIORITY,
        'sam': 2,
    }

    assert len(group) == 3

    for approver in group:
        assert approver.priority == expected_priorities[approver.user.username]

    # 2 группы с приоритетом. Сотрудникам присваивается приоритет своей группы
    workflow_ = "approvers = [any_from(['frodo', 'bilbo'], priority=1) | any_from(['sam', 'gandalf'], priority=2)]"

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    assert len(approvers) == 1
    group = approvers[0]
    assert len(group) == 4

    expected_priorities = {
        'frodo': 1,
        'bilbo': 1,
        'sam': 2,
        'gandalf': 2,
    }

    for approver in group:
        assert approver.priority == expected_priorities[approver.user.username]

    # сотрудникам в группе без приоритета присваивается приоритет группы.
    # У сотрудников с личным приоритетом он не меняется.
    workflow_ = """approvers = [any_from([approver('frodo', priority=1, notify=False), approver('gimli', priority=2),
                 approver('bilbo')], priority=4, notify=True) | 'sam' | any_from(['gandalf', 'saruman'], priority=3)]"""
    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    assert len(approvers) == 1
    group = approvers[0]

    # у sam приоритет будет проставлен от группы слева(4)
    # у bilbo приоритет от своей группы(4)
    expected_priorities = {
        'frodo': 1,
        'gimli': 2,
        'bilbo': 4,
        'sam': DEFAULT_PRIORITY,
        'gandalf': 3,
        'saruman': 3,
    }

    assert len(group) == 6

    for approver in group:
        assert approver.priority == expected_priorities[approver.user.username]

    # Проверка случая когда повторяющиеся сотрудники объединяются в группу и у них сохраняется минимальный приоритет
    # У всей группы будет приоритет последней группы справа, т.е 1
    workflow_ = """approvers=[approver('gimli', priority=10) | any_from([approver('gimli', priority=2),
                              approver('saruman', priority=4), approver('gandalf')], priority=1) | approver('saruman')
                              | 'gandalf' | approver('gandalf', priority=6) | 'manve']"""

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
        'gimli': 2,
        'saruman': 4,
        'gandalf': 1,
        'manve': DEFAULT_PRIORITY,
    }

    assert len(approvers) == 1
    group = approvers[0]
    assert len(group) == 4

    for approver in group:
        assert approver.priority == expected_priorities[approver.user.username]


def test_user_get_chain_heads_in_workflow(arda_users, superuser_node):
    """
        Тест проверяет метод user.get_chain_of_heads() в коде workflow
    """

    # priority_policy = head_less

    workflow_ = dedent("""
    approvers = [userify('aragorn').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.HEAD_LESS)]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    assert len(approvers) == 1

    expected_priorities = {
            'frodo': 1,
            'sam': 2,
            'gandalf': 2,
            'galadriel': 2,
            'varda': 3,
    }

    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]

    # priority_policy = deputy_less

    workflow_ = dedent("""
    approvers = [userify('peregrin').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
        'frodo': 2,
        'sam': 1,
        'gandalf': 1,
        'galadriel': 1,
        'varda': 4,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]

    # priority_policy = equal
    # up_to_level = 0 вернуть руководителей и заместителей только с текушего уровня

    workflow_ = dedent("""
    approvers = [userify('boromir').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.EQUAL,
                                                          up_to_level=0,)]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
            'frodo': 1,
            'sam': 1,
            'gandalf': 1,
            'galadriel': 1,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]

    # priority_policy = no_deputies
    workflow_ = dedent("""
    approvers = [userify('gimli').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.NO_DEPUTIES)]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
            'frodo': 1,
            'varda': 2,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]

    # проверим у начальника
    workflow_ = dedent("""
        approvers = [userify('frodo').get_chain_of_heads(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)]
        """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
        'varda': 2,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]

    # у самого главного начальника
    workflow_ = dedent("""
        approvers = [userify('varda').get_chain_of_heads()]
        """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = {
        'varda': 1,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_priorities)

    for approver in approvers[0]:
        assert approver.priority == expected_priorities[approver.user.username]


def test_setting_consecutive_priority_to_group(arda_users, superuser_node):
    """
        Проверяет случаи когда у всех аппруверов в одной OR-группе стоит дефолтный приоритет.
        В этом случае каждому присваивается приоритет слева направо начиная с 1
    """
    workflow_ = dedent("""
    approvers = [any_from(['gimli', 'sam', 'frodo']) | approver('bilbo'), approver('galadriel',
                 priority=1) | any_from(['saruman', 'gandalf', 'varda']), approver('manve') | 'aragorn' | 'meriadoc',
                 any_from(['boromir', 'peregrin'], priority=1) | any_from(['galadriel', 'sauron'])]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_priorities = [
        # в группе ни у кого не было проставлено приоритета, поэтому приоритеты по порядку
        {
            'gimli': 1,
            'sam': 2,
            'frodo': 3,
            'bilbo': 4,
        },
        # в группе у galadriel приоритет 1, у всех остальных DEFAULT_PRIORIty
        {
            'galadriel': 1,
            'saruman': DEFAULT_PRIORITY,
            'gandalf': DEFAULT_PRIORITY,
            'varda': DEFAULT_PRIORITY,
        },
        # в группе ни у кого не было проставлено приоритета, поэтому приоритеты по порядку
        {
            'manve': 1,
            'aragorn': 2,
            'meriadoc': 3,
        },
        {
            'boromir': 1,
            'peregrin': 1,
            'galadriel': DEFAULT_PRIORITY,
            'sauron': DEFAULT_PRIORITY,
        }
    ]

    assert len(approvers) == 4

    for index, or_group in enumerate(approvers):
        assert len(or_group) == len(expected_priorities[index])
        for approver in or_group:
            assert approver.priority == expected_priorities[index][approver.user.username]


def test_group_get_chain_of_heads_in_workflow(arda_users, superuser_node, department_structure):
    workflow_ = dedent("""
        approvers = [userify('frodo').department_group.get_chain_of_heads()]
    """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']

    expected_result = {
        'frodo': 1,
        'sam': 2,
        'galadriel': 2,
        'gandalf': 2,
        'varda': 3,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_result)

    for approver in approvers[0]:
        assert approver.priority == expected_result[approver.user.username]

    # priority_policy = EQUAL
    workflow_ = dedent("""
            approvers = [userify('manve').department_group.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.EQUAL)]
        """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']
    expected_result = {
        'manve': 1,
        'varda': 1,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_result)

    for approver in approvers[0]:
        assert approver.priority == expected_result[approver.user.username]

    # up_to_level = 0, priority_policy = deputy_less
    workflow_ = dedent("""
                approvers = [userify('frodo').department_group.get_chain_of_heads(
                    priority_policy=PRIORITY_POLICIES.DEPUTY_LESS, up_to_level=0
                )]
            """)

    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']
    expected_result = {
        'frodo': 2,
        'sam': 1,
        'gandalf': 1,
        'galadriel': 1,
    }

    assert len(approvers) == 1
    assert len(approvers[0]) == len(expected_result)

    for approver in approvers[0]:
        assert approver.priority == expected_result[approver.user.username]


def test_empty_anyapprover_in_workflow(arda_users, superuser_node):
    """
        Проверяет что any_from с пустым листом равен пустому листу
    """
    workflow_ = 'approvers = [any_from([])]'
    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']
    assert approvers == []

    # nested any_froms
    workflow_ = 'approvers = [any_from([any_from([])])]'
    approvers = get_data_after_applying_workflow(workflow_, superuser_node)['approvers']
    assert approvers == []


def test_context_reuse(arda_users, superuser_node):
    """
    Проверяет, что ad_groups не просачивается в следующие запуски
    """
    workflow_ = 'approvers = []; ad_groups.append("xxxxx")'
    ad_groups = get_data_after_applying_workflow(workflow_, superuser_node)['ad_groups']
    assert ad_groups == ['xxxxx']

    workflow_ = 'approvers = []'
    ad_groups = get_data_after_applying_workflow(workflow_, superuser_node)['ad_groups']
    assert ad_groups == []


def test_workflow_comment(arda_users, superuser_node):
    workflow = 'approvers = []; workflow_comment = "12"'
    assert get_data_after_applying_workflow(workflow, superuser_node)['workflow_comment'] == '12'


def test_workflow_comment_not_str(arda_users, superuser_node):
    workflow = 'approvers = []; workflow_comment = 1'
    with pytest.raises(WorkflowError):
        get_data_after_applying_workflow(workflow, superuser_node)


@pytest.mark.parametrize('recursion_type', ['direct', 'indirect'])
def test_recursive_ref_role(arda_users, simple_system, department_structure, client, recursion_type):

    frodo = arda_users.frodo
    shire = department_structure['shire']

    workflow = {'direct': dedent("""
        approvers = []
        ref_roles = [{
            'system': '%s',
            'role_data': {'role': 'admin'},
        }]
    """ % simple_system.slug),
                'indirect': dedent("""
        approvers = []
        if role == {'role': 'admin'}:
            ref_roles = [{
                'system': '%(slug)s',
                'role_data': {'role': 'manager'},
            }]
        if role == {'role': 'manager'}:
            ref_roles = [{
                'system': '%(slug)s',
                'role_data': {'role': 'admin'},
            }]
    """ % {'slug': simple_system.slug})
                }[recursion_type]

    set_workflow(simple_system, workflow, workflow)

    cache.clear()

    url = '/monitorings/overlengthed-ref-roles/'

    response = client.json.get(url)

    assert response.status_code == 400
    assert response.content.decode('utf-8') == 'Cache is empty!'

    OverlengthedRefRoleChainMetric.set([])

    response = client.json.get(url)
    assert response.status_code == 200
    assert response.content.decode('utf-8') == 'ok'

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == settings.IDM_MAX_REF_ROLES_CHAIN_LENGTH

    response = client.json.get(url)
    assert response.status_code == 400
    bad_role1 = Role.objects.order_by('pk').last()
    assert response.content.decode('utf-8') == 'Roles with overlengthed refs chain: {pk}'.format(pk=bad_role1.pk)

    Role.objects.all().delete()
    assert Role.objects.count() == 0

    Role.objects.request_role(frodo, shire, simple_system, '', {'role': 'admin'}, None)

    owners_count = len([shire]) + len(shire.members)
    assert Role.objects.count() == settings.IDM_MAX_REF_ROLES_CHAIN_LENGTH * owners_count


def test_user_has_role_with_system(arda_users, simple_system, complex_system):
    RoleField.objects.all().delete()
    frodo = arda_users.frodo
    workflow = dedent('''
    if requester.has_role({'role': 'manager'}, system='simple'):
        raise AccessDenied('Requester has manager role')

    approvers = []
    ''')

    set_workflow(complex_system, workflow)

    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules', 'role': 'admin'})
    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'})
    manager_role.refresh_from_db()
    assert manager_role.state == ROLE_STATE.GRANTED

    with pytest.raises(AccessDenied):
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'})


def _set_deprive_workflow(system, code):
    system.use_workflow_for_deprive = True
    system.save(update_fields=('use_workflow_for_deprive',))
    set_workflow(system, code)


@pytest.mark.django_db
def test_workflow_for_deprive_no_approvers_defined(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        pass
    """))
    with pytest.raises(NoApproversDefined):
        can_deprive_role(
            requester=requesterify(arda_users.frodo),
            role=arda_users_with_roles.frodo[0])


@pytest.mark.django_db
def test_workflow_request_type_deprive(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = ['frodo']
        assert request_type == 'deprive' == REQUEST_TYPE.DEPRIVE
    """))
    arda_users_with_roles.frodo[0].deprive_or_decline(arda_users.frodo)


def test_workflow_request_type_request(arda_users, arda_users_with_roles, simple_system):
    frodo = arda_users.frodo
    set_workflow(simple_system, dedent("""
        approvers = []
        assert request_type == 'request' == REQUEST_TYPE.REQUEST
    """))
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'poweruser'})


@pytest.mark.django_db
def test_workflow_for_deprive_flatten_approvers(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = [any_from(['gandalf', 'legolas']), 'bilbo']
    """))
    role = arda_users_with_roles.frodo[0]
    assert can_deprive_role(requesterify(arda_users.legolas), role)
    assert can_deprive_role(requesterify(arda_users.bilbo), role)
    assert not can_deprive_role(requesterify(arda_users.aragorn), role)


@pytest.mark.django_db
def test_workflow_for_deprive_on(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = ['gandalf', 'sam']
    """))
    result = can_deprive_role(
        requester=requesterify(arda_users.frodo),
    role=arda_users_with_roles.frodo[0])
    assert not result
    possible_deprivers = set(a['username'] for a in result.data['approvers'])
    assert possible_deprivers == {'gandalf', 'sam'}


@pytest.mark.django_db
def test_workflow_for_deprive_access_denied(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        raise AccessDenied()
    """))
    result = can_deprive_role(
        requester=requesterify(arda_users.legolas),
        role=arda_users_with_roles.legolas[0])
    assert not result
    assert not result.data


@pytest.mark.django_db
def test_workflow_for_deprive_exception(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        raise_syntax_error()
    """))
    with pytest.raises(WorkflowError):
        can_deprive_role(
            requester=requesterify(arda_users.frodo),
            role=arda_users_with_roles.frodo[0])


def test_workflow_for_deprive_off(arda_users, arda_users_with_roles, simple_system):
    set_workflow(simple_system, dedent("""
        approvers = ['gandalf', 'sam']
    """))
    action = arda_users_with_roles.frodo[0].deprive_or_decline(arda_users.frodo)
    assert not action.error


@pytest.mark.django_db
def test_workflow_for_deprive_empty_approvers(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = []
    """))
    assert can_deprive_role(
        requester=requesterify(arda_users.legolas),
        role=arda_users_with_roles.frodo[0])


@pytest.mark.django_db
def test_workflow_for_deprive_only_fired_depriver(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = ['legolas']
    """))
    arda_users.legolas.is_active = False
    arda_users.legolas.save(update_fields=('is_active',))
    with pytest.raises(WorkflowError, match='отозвать.*уволены'):
        can_deprive_role(
            requester=requesterify(arda_users.frodo),
            role=arda_users_with_roles.frodo[0])


@pytest.mark.django_db
def test_workflow_for_deprive_fired_depriver(arda_users, arda_users_with_roles, simple_system):
    _set_deprive_workflow(simple_system, dedent("""
        approvers = ['legolas', any_from(['sam', 'gandalf'])]
    """))
    arda_users.legolas.is_active = False
    arda_users.legolas.save(update_fields=('is_active',))
    result = can_deprive_role(
        requester=requesterify(arda_users.frodo),
        role=arda_users_with_roles.frodo[0])
    possible_deprivers = set(a['username'] for a in result.data['approvers'])
    assert possible_deprivers == {'sam', 'gandalf'}


@pytest.mark.django_db
def test_workflow_for_deprive_approver_and_depriver(arda_users, arda_users_with_roles, simple_system):
    simple_system.request_policy = SYSTEM_REQUEST_POLICY.ANYONE
    original_requester = arda_users.legolas
    _set_deprive_workflow(simple_system, dedent("""
        if request_type == REQUEST_TYPE.DEPRIVE:
            boss = user.get_boss()
            approvers = [user, boss]
            if original_requester:
                approvers.append(original_requester)
            raise Return()
        approvers = []
    """))
    role = Role.objects.request_role(original_requester, arda_users.frodo, simple_system, '', {'role': 'poweruser'}, None)
    role.refresh_from_db()
    assert can_deprive_role(requesterify(arda_users.frodo), role)
    assert not can_deprive_role(requesterify(arda_users.sam), role)
    assert not can_deprive_role(requesterify(arda_users.gandalf), role)
    assert can_deprive_role(requesterify(arda_users.varda), role)
    assert can_deprive_role(requesterify(original_requester), role)
    assert not can_deprive_role(requesterify(original_requester), arda_users_with_roles.frodo[0])


def test_raise_return(arda_users, simple_system):
    frodo = arda_users.frodo
    set_workflow(simple_system, dedent("""
    # action1
    approvers = []
    arbitrary_variable = 12345
    raise Return()
    arbitrary_variable = 54321
    """))
    _, context = Role.objects.request_or_simulate(
        requesterify(frodo), frodo, simple_system, {'role': 'manager'},
        fields_data=None, ttl_date=None, ttl_days=None, review_date=None, parent=None)
    assert context['arbitrary_variable'] == 12345


@pytest.mark.django_db
@pytest.mark.parametrize('local_permission', (True, False))
def test_super_depriver(arda_users, simple_system, depriver_users, local_permission):
    superuser = create_user('depriver123')
    user = create_user('user')
    role = raw_make_role(user, simple_system, {'role': 'admin'}, state='granted')
    _set_deprive_workflow(simple_system, dedent("""
        raise AccessDenied()
    """))
    assert not can_deprive_role(superuser, role)
    add_perms_by_role('roles_depriver', superuser, system=simple_system if local_permission else None)
    assert can_deprive_role(superuser, role)


def test_ensure_wrappers(users_for_test, simple_system):
    user = users_for_test[0]
    set_workflow(simple_system, dedent("""
    for item in [requester, user, original_requester, system, node]:
        if item is not None:
            assert 'Wrapper' in item.__class__.__name__
    approvers = []
    """))
    Role.objects.request_role(user, user, simple_system, '', {'role': 'poweruser'}, None)


def test_ensure_ttl_days(users_for_test, simple_system):
    user = users_for_test[0]
    set_workflow(simple_system, dedent("""
    assert ttl_days == 10
    approvers = []
    """))
    Role.objects.request_role(user, user, simple_system, '', {'role': 'poweruser'}, None, ttl_days=10)


def test_ttl_days_overwrite_ttl_date(users_for_test, simple_system):
    user = users_for_test[0]
    role = Role.objects.request_role(user, user, simple_system, '', {'role': 'poweruser'}, None,
                                     ttl_date=timezone.now() + timedelta(days=10),
                                     ttl_days=15)
    assert not role.ttl_days
    set_workflow(simple_system, dedent("""
        ttl_days=7
        approvers=[]
    """))
    role = Role.objects.request_role(user, user, simple_system, '', {'role': 'admin'}, None,
                                     ttl_date=timezone.now() + timedelta(days=10),
                                     ttl_days=15)
    assert not role.ttl_date
    assert role.ttl_days == 7

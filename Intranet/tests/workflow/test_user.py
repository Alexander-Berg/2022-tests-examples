# coding: utf-8


from textwrap import dedent

import pytest
import yaml
from mock import patch

from idm.core.constants.workflow import PRIORITY_POLICIES
from idm.core.workflow import exceptions
from idm.core.models import Role, RoleNode
from idm.core.workflow.plain.approver import Approver
from idm.core.workflow.shortcuts import workflow
from idm.core.workflow.plain.user import userify, all_heads_of, get_head_of_or_zam, get_head_of, UserWrapper
from idm.permissions.utils import add_perms_by_role
from idm.tests.utils import set_workflow, refresh, DEFAULT_WORKFLOW, raw_make_role
from idm.users import ranks
from idm.users.constants.user import USER_TYPES
from idm.users.models import User, Group

pytestmark = pytest.mark.django_db

__doc__ = """Тесты, относящиеся к интерфейсу UserWrapper и функциям, которые мы предоставляем в UserContext"""


_workflow = """
approvers = []

# если роль запрашивается не непосредственным руководителем сотрудника,
# запросить одобрение непосредственного руководителя;
if not requester.is_boss_of(user):
    approvers.append(approver(user.get_boss()))

# если сотрудник запрашивает роль менеджера
if role['role'] == 'manager':
    if user.works_in_dep(10) or user.works_in_dep(20):
        # и работает в отделе по работе с клиентами
        # или отделе по работе с агентствами коммерческого департамента,
        # выдать роль;
        approvers[:] = []

    elif user.works_in_dep(30):
        # и работает в другом подразделении,
        # запросить одобрение Иткиной, затем выдать роль;
        approvers.append(approver('itkina'))

# если сотрудник запрашивает роль суперпользователя
if role['role'] == 'superuser':
    if user.works_in_dep(40):
        # и работает в отделе разработке рекламных технологий департамента разработки
        # запросить одобрение (Высоцкой или Ломизе) и выдать роль;

        approvers.append(approver('visotskaya') | approver('lomidze'))
    else:
        # запросить одобрение службы ИБ
        # запросить одобрение Иткиной
        # и выдать роль
        approvers.append(approver('lomidze'))
        approvers.append(approver('itkina'))

"""


def _create_structure(yaml_with_deps_and_users):
    def create_departments(data, parent=None):
        """Рекурсивно проходится по полученной структуре данных и создает департаменты и пользователей."""
        for key, value in data.items():
            if parent is None:
                parent = Group.objects.get_root('department')
            dep, created = Group.objects.get_or_create(
                external_id=value['id'],
                name=key,
                parent=parent,
                type='department',
            )
            if 'slug' in value:
                dep.slug = value['slug']

            create_departments(value.get('deps', {}), parent=dep)

            users = value.get('users')
            if isinstance(users, list):
                users = dict((user, {}) for user in users)

            for username, attrs in users.items():
                user, created = User.objects.get_or_create(username=username)
                user.department_group = dep
                user.save()

                if attrs and attrs.get('chief'):
                    dep.responsibilities.create(rank=ranks.HEAD, user=user, is_active=True)
            dep.save()

    create_departments(yaml.safe_load(yaml_with_deps_and_users))


@pytest.mark.robotless
def test_get_boss_or_zam(group_roots):
    _create_structure("""
        dep1:
            id: 1
            slug: "dev"
            users:
                bigboss:
                    chief: true
                zam1:
                zam2:
            deps:
                dep2:
                    id: 2
                    slug: "misc"
                    users:
                        terran:
    """)
    terran = userify(User.objects.select_related('department_group').get(username='terran'))
    bigboss = userify(User.objects.get(username='bigboss'))
    zam1 = userify(User.objects.get(username='zam1'))
    zam2 = userify(User.objects.get(username='zam2'))
    absent_tuple = tuple()

    def _absence(username):
        if username in absent_tuple:
            return True
        else:
            return False

    with patch('idm.sync.gap.is_user_absent') as is_absent:
        is_absent.side_effect = _absence

        # начальник на работе
        absent_tuple = tuple()
        assert terran.get_boss_or_zam() == bigboss
        assert hasattr(terran.get_boss_or_zam(), 'get_boss')

        # начальник не на работе
        absent_tuple = ('bigboss', )

        # замов не существует
        with patch('idm.core.workflow.plain.user.get_department_zams') as get_chiefs:
            get_chiefs.return_value = []
            with pytest.raises(exceptions.BossAndZamNotAvailableError):
                terran.get_boss_or_zam()

        # но если запрашивающий - начальник, то ошибки нет
        with patch('idm.core.workflow.plain.user.get_department_zams') as get_chiefs:
            terran.context['requester'] = bigboss
            get_chiefs.return_value = []
            assert terran.get_boss_or_zam() == bigboss
            del terran.context['requester']

        # замы существуют
        with patch('idm.core.workflow.plain.user.get_department_zams') as get_chiefs:
            get_chiefs.return_value = ['zam1', 'zam2']

            # замы отсутствуют
            absent_tuple = ('bigboss', 'zam1', 'zam2')
            with pytest.raises(exceptions.BossAndZamNotAvailableError):
                terran.get_boss_or_zam()
            try:
                terran.get_boss_or_zam()
            except exceptions.BossAndZamNotAvailableError as e:
                assert len(e.zams) == 2
                assert hasattr(e.boss, 'get_boss')
                for zam in e.zams:
                    assert hasattr(zam, 'get_boss')

            # один зам на работе
            absent_tuple = ('bigboss', 'zam1')
            assert terran.get_boss_or_zam() == zam2
            assert hasattr(terran.get_boss_or_zam(), 'get_boss')

            # все замы на работе
            absent_tuple = ('bigboss', )
            assert terran.get_boss_or_zam() in [zam1, zam2]
            assert hasattr(terran.get_boss_or_zam(), 'get_boss')

            # все не на работе, но один из замов является запрашивающим
            absent_tuple = ('bigboss', 'zam1', 'zam2')
            terran.context['requester'] = zam1
            assert terran.get_boss_or_zam() == zam1
            terran.context['requester'] = zam2
            assert terran.get_boss_or_zam() == zam2

            # запрашивает зам, а начальник на работе
            absent_tuple = ('zam1', 'zam2')
            terran.context['requester'] = zam1
            assert terran.get_boss_or_zam() == bigboss

            # на работе один зам, а запрашивает другой
            absent_tuple = ('bigboss', 'zam2')
            terran.context['requester'] = zam2
            assert terran.get_boss_or_zam() == zam1

            absent_tuple = ('bigboss', 'zam1')
            terran.context['requester'] = zam1
            assert terran.get_boss_or_zam() == zam1


@pytest.mark.robotless
def test_get_boss_or_zam_complex(group_roots):
    """
    В RULES-937 описана ошибка get_head_or_zam в поиске заместителей,
    когда начальник департамента в нем не работает, это тест для него.
    """
    _create_structure("""
        dep1:
            id: 10
            slug: "dev"
            users:
                zam1:
                zam2:
            deps:
                dep2:
                    id: 20
                    slug: "misc"
                    users:
                        terran:
        dep3:
            id: 30
            slug: "other"
            users:
                bigboss:
                zam_o1:
    """)
    bigboss = User.objects.get(username='bigboss')
    zam2_wf_user = userify(User.objects.get(username='zam2'))
    terran_wf_user = userify(User.objects.select_related('department_group').get(username='terran'))

    dep1 = Group.objects.get(external_id=10)
    dep1.responsibilities.update(is_active=False)
    dep1.responsibilities.create(is_active=True, user=bigboss, rank=ranks.HEAD)

    absent_tuple = tuple()

    def _absence(username):
        if username in absent_tuple:
            return True
        else:
            return False

    def _fake_get_zam(dep_slug):
        if dep_slug != 'dev':
            raise RuntimeError('wrong departments slug received: "%s"' % dep_slug)
        return ('zam1', 'zam2')

    with patch('idm.sync.gap.is_user_absent') as is_absent:
        is_absent.side_effect = _absence

        # начальник на работе
        absent_tuple = tuple()
        assert terran_wf_user.get_boss_or_zam() == userify(bigboss)

        # начальник и 1й зам не на работе
        absent_tuple = ('bigboss', 'zam1')

        with patch('idm.core.workflow.plain.user.get_department_zams') as get_zams:
            get_zams.side_effect = _fake_get_zam

            # должен вернуться именно 2й зам из 1го департамента
            assert terran_wf_user.get_boss_or_zam() == zam2_wf_user


@pytest.mark.robotless
def test_basic_structure_requests(group_roots):
    _create_structure("""
        dep1:
            id: 1
            users:
                vasya:
                    chief: true
                petya:
    """)

    vasya = userify('vasya')
    petya = userify('petya')

    assert vasya.is_boss_of(petya)
    assert vasya.is_boss_of('petya')
    assert not petya.is_boss_of(vasya)
    assert not petya.is_boss_of('vasya')

    assert vasya == petya.get_boss()
    # проверяем, что объект, возвращаемый методом get_boss
    # является оберткой над классом User и имеет дополнительные методы
    assert hasattr(petya.get_boss(), 'get_boss')

    assert petya.works_in_dep(1)
    assert not petya.works_in_dep(2)


@pytest.mark.robotless
def test_boss_of_the_boss(group_roots):
    _create_structure("""
        dep1:
            id: 1
            slug: "dev"
            users:
                vasya:
                    chief: true
            deps:
                dep2:
                    id: 2
                    slug: "misc"
                    users:
                        petya:
                            chief: true
                        ivan:
    """)
    vasya = userify('vasya')
    petya = userify('petya')
    ivan = userify('ivan')

    assert vasya == petya.get_boss()
    with pytest.raises(exceptions.NoBossError):
        vasya.get_boss()
    assert vasya.is_boss_of(petya)

    assert vasya.is_head_of('dev')
    assert not petya.is_head_of('dev')

    # проверим двойную выборку для Ивана
    boss = ivan.get_boss()
    boss.fetch_department_group()
    assert vasya == boss.get_boss()
    # и заодно то, что Иван числится как в "misc", так и в "dev"
    assert ivan.works_in_dep('misc')
    assert ivan.works_in_dep('dev')


@pytest.mark.robotless
def test_plural_deps(group_roots):
    _create_structure("""
        dep1:
            id: 10
            slug: "dev"
            users:
                vasya:
                    chief: true
            deps:
                dep2:
                    id: 20
                    slug: "misc"
                    users:
                        petya:
                            chief: true
                        ivan:
    """)
    vasya = userify('vasya')
    petya = userify('petya')
    ivan = userify('ivan')
    dev = Group.objects.get(slug='dev')
    misc = Group.objects.get(slug='misc')

    # is_head_of
    assert vasya.is_head_of('dev')
    assert not petya.is_head_of('dev')
    assert not ivan.is_head_of('dev')

    assert not vasya.is_head_of('misc')
    assert petya.is_head_of('misc')
    assert not ivan.is_head_of('misc')

    assert vasya.is_head_of('dev', 'misc')
    assert petya.is_head_of('dev', 'misc')
    assert not ivan.is_head_of('dev', 'misc')

    assert vasya.is_head_of(dev.external_id, misc.external_id)
    assert petya.is_head_of(dev.external_id, misc.external_id)
    assert not ivan.is_head_of(dev.external_id, misc.external_id)

    # works_in_dep
    assert vasya.works_in_dep('dev')
    assert petya.works_in_dep('dev')
    assert ivan.works_in_dep('dev')

    assert not vasya.works_in_dep('misc')
    assert petya.works_in_dep('misc')
    assert ivan.works_in_dep('misc')

    assert vasya.works_in_dep('dev', 'misc')
    assert petya.works_in_dep('dev', 'misc')
    assert ivan.works_in_dep('dev', 'misc')

    assert vasya.works_in_dep(dev.external_id, misc.external_id)
    assert petya.works_in_dep(dev.external_id, misc.external_id)
    assert ivan.works_in_dep(dev.external_id, misc.external_id)


@pytest.mark.robotless
def test_department_heads(group_roots):
    """
    TestpalmID: 3456788-215
    """
    _create_structure("""
        dep1:
            id: 1
            slug: "dev"
            users:
                vasya:
                    chief: true
            deps:
                dep2:
                    id: 2
                    slug: "misc"
                    users:
                        petya:
                            chief: true
                        ivan:
                dep3:
                    id: 3
                    slug: "test"
                    users:
                        anton:
                            chief: true
                        sergey:
                            chief: true
    """)
    vasya = userify('vasya')
    petya = userify('petya')
    ivan = userify('ivan')
    anton = userify('anton')
    sergey = userify('sergey')

    heads_of = all_heads_of('dev')
    assert vasya in heads_of
    assert petya in heads_of
    assert anton in heads_of
    assert sergey in heads_of
    assert ivan not in heads_of

    heads_of = all_heads_of('misc')
    assert petya in heads_of
    assert vasya not in heads_of
    assert ivan not in heads_of
    assert anton not in heads_of
    assert sergey not in heads_of

    heads_of = all_heads_of('test')
    assert anton in heads_of
    assert sergey in heads_of
    assert ivan not in heads_of
    assert petya not in heads_of
    assert vasya not in heads_of

@pytest.mark.robotless
def test_no_direct_boss(group_roots):
    # Иван и Петя находятся в группе, и у них нет руководителя группы
    # И вдобавок, у службы куда они входят, тоже нет руководителя, зато
    # есть руководитель у отдела, куда входит служба
    _create_structure("""
        dep1:
            id: 1
            users:
                vasya:
                    chief: true
            deps:
                dep2:
                    id: 2
                    users: []
                    deps:
                        dep3:
                            id: 3
                            users:
                                petya:
                                ivan:
    """)
    vasya = userify('vasya')
    petya = userify('petya')

    assert vasya == petya.get_boss()


@pytest.mark.robotless
def test_get_head_of(group_roots):
    _create_structure("""
        dep1:
            id: 1
            slug: "dev"
            users:
                bigboss:
                    chief: true
                zerg:
            deps:
                dep2:
                    id: 2
                    slug: "misc"
                    users:
                        terran:
    """)
    bigboss = userify(User.objects.get(username='bigboss'))

    assert get_head_of('dev') == bigboss
    assert hasattr(get_head_of('dev'), 'get_boss')

    with pytest.raises(exceptions.NoDepartmentError):
        get_head_of('dep_magic')
    with pytest.raises(exceptions.NoBossError):
        get_head_of('misc')


@pytest.mark.robotless
def test_get_head_of_or_zam(group_roots):
    """
    TestpalmID: 3456788-214
    """
    _create_structure("""
        dep1:
            id: 10
            slug: "dev"
            users:
                bigboss:
                    chief: true
                zam1:
                zam2:
            deps:
                dep2:
                    id: 20
                    slug: "misc"
                    users:
                        terran:
    """)
    bigboss = userify(User.objects.get(username='bigboss'))
    zam1 = userify(User.objects.get(username='zam1'))
    zam2 = userify(User.objects.get(username='zam2'))
    absent_tuple = tuple()

    def _absence(username):
        if username in absent_tuple:
            return True
        else:
            return False

    with patch('idm.sync.gap.is_user_absent') as is_absent:
        is_absent.side_effect = _absence

        # начальник на работе
        absent_tuple = tuple()
        assert get_head_of_or_zam('dev') == bigboss
        assert hasattr(get_head_of_or_zam('dev'), 'get_boss')

        # начальник не на работе
        absent_tuple = ('bigboss', )

        # замов не существует
        with patch('idm.core.workflow.plain.user.get_department_zams') as get_zams:
            get_zams.return_value = []
            with pytest.raises(exceptions.BossAndZamNotAvailableError):
                get_head_of_or_zam('dev')

        # замы существуют
        with patch('idm.core.workflow.plain.user.get_department_zams') as get_zams:
            get_zams.return_value = ['zam1', 'zam2']

            # оба отсутствуют на работе
            absent_tuple = ('bigboss', 'zam1', 'zam2')
            with pytest.raises(exceptions.BossAndZamNotAvailableError):
                get_head_of_or_zam('dev')
            try:
                get_head_of_or_zam('dev')
            except exceptions.BossAndZamNotAvailableError as e:
                assert len(e.zams) == 2
                assert hasattr(e.boss, 'get_boss')
                for zam in e.zams:
                    assert hasattr(zam, 'get_boss')

            # zam2 присутствует
            absent_tuple = ('bigboss', 'zam1')
            assert get_head_of_or_zam('dev') == zam2
            assert hasattr(get_head_of_or_zam('dev'), 'get_boss')

            # оба на работе
            absent_tuple = ('bigboss',)
            assert get_head_of_or_zam('dev') == zam1
            assert hasattr(get_head_of_or_zam('dev'), 'get_boss')

    with pytest.raises(exceptions.NoDepartmentError):
        get_head_of_or_zam('dep_magic')
    with pytest.raises(exceptions.NoBossError):
        get_head_of_or_zam('misc')


def test_complex_scenario(superuser_node, group_roots):
    _create_structure("""
        dep1:
            id: 10
            users:
                itkina:
                    chief: true
                vasya:
        dep3:
            id: 30
            users:
                ivan:
                    chief: true
                petya:
        dep4:
            id: 40
            users: [kostya, visotskaya, lomidze]
    """)

    assert workflow(
        _workflow,
        role_data={'role': 'manager'},
        requester=userify('vasya'),
        subject=userify('petya'),
        system=superuser_node.system,
        node=superuser_node,
    )['approvers'] == [Approver('ivan'), Approver('itkina')]
    with pytest.raises(exceptions.NoBossError):
        workflow(
            _workflow,
            role_data={'role': 'superuser'},
            system=superuser_node.system,
            node=superuser_node,
            requester=userify('kostya'),
            subject=userify('kostya')
        )


def test_user_has_role(arda_users, simple_system):
    """Проверим работу метода has_role"""

    wf = dedent('''
    if user.has_role({'role': 'admin'}):
        approvers = []
    else:
        approvers = ['gandalf']
    ''')
    frodo = arda_users.frodo

    set_workflow(simple_system, wf)
    role1 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role1 = refresh(role1)
    assert role1.state == 'requested'
    approve_request = role1.requests.get().approves.get().requests.select_related_for_set_decided().get()
    assert approve_request.approver_id == arda_users.gandalf.id
    # подтвердим роль
    approve_request.set_approved(arda_users.gandalf)

    role2 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'


def test_group_has_role(arda_users, department_structure, simple_system):
    """Проверим работу метода has_role"""

    wf = dedent('''
    if group.has_role({'role': 'admin'}):
        approvers = []
    else:
        approvers = ['gandalf']
    ''')
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    set_workflow(simple_system, group_code=wf)
    role1 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role1 = refresh(role1)
    assert role1.state == 'requested'
    approve_request = role1.requests.get().approves.get().requests.select_related_for_set_decided().get()
    assert approve_request.approver_id == arda_users.gandalf.id
    # подтвердим роль
    approve_request.set_approved(arda_users.gandalf)

    role2 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'


def test_user_has_role_no_such_node(arda_users, simple_system):
    """Если узел не нашёлся, метод has_role должен пробрасывать исключение наверх"""

    wf = dedent('''
    if user.has_role({'role': 'salesman'}):
        approvers = []
    else:
        approvers = ['gandalf']
    ''')

    set_workflow(simple_system, wf)
    frodo = arda_users.frodo
    with pytest.raises(exceptions.WorkflowError) as info:
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert str(info.value) == "В дереве ролей системы Simple система отсутствует узел {'role': 'salesman'}"


def test_user_has_role_for_group_aware_system(arda_users, aware_simple_system, department_structure):
    """Проверим работу метода has_role для group-aware систем"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    wf = dedent('''
    if user.has_role({'role': 'admin'}):
        approvers = []
    else:
        approvers = ['gandalf']
    ''')
    set_workflow(aware_simple_system, wf, DEFAULT_WORKFLOW)

    group_role = Role.objects.request_role(frodo, fellowship, aware_simple_system, '', {'role': 'admin'}, None)
    group_role = refresh(group_role)
    assert group_role.state == 'granted'
    assert Role.objects.count() == 1

    role = Role.objects.request_role(frodo, frodo, aware_simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'


def test_user_actual_mobile_phone_for_tvm(arda_users, simple_system, generic_system_with_tvm):
    """Проверим работу метода actual_mobile_phone для tvm-app юзеров"""

    frodo = arda_users.frodo
    frodo.type = USER_TYPES.TVM_APP
    frodo.save()
    simple_system.use_tvm_role = True
    simple_system.save()

    wf = 'approvers = user.actual_mobile_phone'
    set_workflow(simple_system, wf)

    with pytest.raises(exceptions.AccessDenied):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)


def test_user_all_roles(arda_users, simple_system, department_structure):
    """Проверим работу свойства all_roles"""

    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'poweruser'}, state='deprived')

    wf = dedent('''
    if user.all_roles == [{'role': 'admin'}]:
        approvers = []
    else:
        raise AccessDenied()
    ''')

    set_workflow(simple_system, wf)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'


def test_get_robot_owners(simple_system, arda_users, robot_gollum):
    frodo, sam = arda_users['frodo'], arda_users['sam']
    robot_gollum.responsibles.add(frodo, sam)

    wf = dedent('''
        approvers = user.get_robot_owners()
        ''')
    set_workflow(simple_system, wf)
    role = Role.objects.request_role(robot_gollum, robot_gollum, simple_system, '', {'role': 'manager'}, None)
    assert {
       approve.requests.select_related('approver').get().approver for approve in role.requests.get().approves.all()
    } == {frodo, sam}


def test_is_owner_of(simple_system, robot_gollum, arda_users):
    frodo, sam = arda_users['frodo'], arda_users['sam']
    for user in frodo, sam:
        add_perms_by_role('users_view', user, simple_system)
    robot_gollum.responsibles.add(frodo)

    wf = dedent('''
        if requester.is_owner_of(user):
            approvers = [requester.username]
        else:
            raise AccessDenied('Only owners can request roles for robots')
        ''')
    set_workflow(simple_system, wf)
    role = Role.objects.request_role(frodo, robot_gollum, simple_system, '', {'role': 'manager'}, None)
    assert role.requests.get().approves.get().requests.get().approver_id == frodo.id

    with pytest.raises(exceptions.AccessDenied, match='Only owners can request roles for robots'):
        Role.objects.request_role(sam, robot_gollum, simple_system, '', {'role': 'admin'}, None)


def test_get_chain_heads(arda_users, department_structure):
    """
        Вызывает у userwrapper метод для 4 типов priority_policy
    """
    legolas = userify(arda_users.legolas)
    heads_and_deputies = legolas.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.HEAD_LESS)

    expected_result = {
            'frodo': 1,
            'sam': 2,
            'gandalf': 2,
            'galadriel': 2,
            'varda': 3,
        }

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    # руководитель подразделения в котором он работает
    frodo = userify(arda_users.frodo)

    heads_and_deputies = frodo.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)

    expected_result = {
        'varda': 2,
    }

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    peregrin = userify(arda_users.peregrin)

    heads_and_deputies = peregrin.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.EQUAL)

    expected_result = {
            'frodo': 1,
            'sam': 1,
            'gandalf': 1,
            'galadriel': 1,
            'varda': 2,
    }

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    aragorn = userify(arda_users.aragorn)

    # Только руководители
    heads_and_deputies = aragorn.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.NO_DEPUTIES)

    expected_result = {
        'frodo': 1,
        'varda': 2,
    }

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    # самый высокий руководитель
    heads_and_deputies = userify('varda').get_chain_of_heads()

    expected_result = {
        'varda': 1,
    }

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]


def test_get_priorities_nonchained(arda_users, department_structure):
    frodo = userify(arda_users.frodo)

    heads_and_deputies = frodo.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.EQUAL)

    expected = [userify('varda')]

    expected_priorities = {
        'varda': 1,
    }

    assert heads_and_deputies == expected

    for approver in heads_and_deputies:
        assert approver.priority == expected_priorities[approver.user.username]


def test_has_role_unit(arda_users, simple_system):
    """Проверим работу метода has_role"""
    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    frodo_wrapper = UserWrapper(frodo, context={'system': simple_system})
    assert frodo_wrapper.has_role(role_data={'role': 'admin'})
    assert not frodo_wrapper.has_role(role_data={'role': 'manager'})

    node = RoleNode.objects.get(system=simple_system, slug='admin')
    assert frodo_wrapper.has_role(node_id=node.id)
    assert not frodo_wrapper.has_role(node_id=RoleNode.objects.get(system=simple_system, slug='manager').id)


def test_get_groups(arda_users, simple_system):
    frodo = arda_users.frodo
    slugs = ['the-shire', 'lands', 'fellowship-of-the-ring', 'associations', 'middle-earth', 'valinor']
    frodo_groups = set(Group.objects.filter(slug__in=slugs))
    found_groups = {group_wrapper.group for group_wrapper in UserWrapper(frodo).groups}
    assert frodo_groups == found_groups

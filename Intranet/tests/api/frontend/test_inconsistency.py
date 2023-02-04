
# coding: utf-8


from datetime import datetime
import pytest
import operator

from django.utils import six
from django.utils.encoding import force_text
from django.utils.dateparse import parse_datetime
from django.utils.functional import partition

from idm.core.models import Role
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import make_inconsistency, raw_make_role, attrdict, CountIncreasedContext, add_perms_by_role
from idm.utils import reverse


pytestmark = [pytest.mark.django_db, pytest.mark.robot, pytest.mark.parametrize('api_name', ('frontend', 'v1'))]


@pytest.fixture
def inconsistencies_for_test(api_name, arda_users, simple_system, department_structure):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = department_structure.fellowship

    their1 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=frodo,
        path='/role/superuser/',
        remote_fields={'login': 'hello'},
        remote_username='frodo',
    )
    their2 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=legolas,
        path='/role/superuser/',
        remote_fields={'login': 'world'},
    )
    their2 = Inconsistency.objects.select_related('system__actual_workflow', 'node__system').get(pk=their2.pk)
    their2.resolve()
    their3 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        group=fellowship,
        remote_group=fellowship.external_id,
        path='/role/superuser/',
    )
    their4 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=frodo,
        path=None,
        remote_fields={'login': 'hello'},
        remote_username='frodo',
    )
    imported_role = Role.objects.get()
    assert their2.role == imported_role
    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'})
    role2 = raw_make_role(legolas, simple_system, {'role': 'admin'})
    role3 = raw_make_role(fellowship, simple_system, {'role': 'poweruser'})
    make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        system=simple_system,
        user=frodo,
        our_role=role1,
    )
    inc_our2 = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        system=simple_system,
        user=legolas,
        our_role=role2,
    )
    inc_our2.resolve()
    inc_our2 = Inconsistency.objects.select_related('system__actual_workflow', 'node__system', 'role__system').get(pk=inc_our2.pk)
    inc_our3 = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        system=simple_system,
        group=fellowship,
        our_role=role3,
    )

    unknown_login = make_inconsistency(
        type=Inconsistency.TYPE_UNKNOWN_USER,
        system=simple_system,
        remote_username='johnsnow'
    )

    unknown_group = make_inconsistency(
        type=Inconsistency.TYPE_UNKNOWN_GROUP,
        system=simple_system,
        remote_group=100,
    )

    unknown_node = make_inconsistency(
        type=Inconsistency.TYPE_UNKNOWN_ROLE,
        system=simple_system,
        user=frodo,
    )

    return attrdict({
        'imported_role': imported_role,
        'our_roles': [role1, role2, role3],
        'count': Inconsistency.objects.count()
    })


def test_get_inconsistencies(api_name, client, simple_system, arda_users, responsible_gandalf,
                             inconsistencies_for_test):
    """
    GET /testapi/inconsistencies/
    """

    imported_role = inconsistencies_for_test.imported_role
    total_count = inconsistencies_for_test.count
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')

    # запрос всех несоответствий
    client.login('frodo')
    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    # обычному смертному не доступны неконсистентности
    assert data['meta']['total_count'] == 0
    # gandalf – ответственный системы, поэтому имеет соответствующие права
    client.login('gandalf')
    data = client.json.get(url).json()
    assert data['meta']['total_count'] == total_count
    ids = [inc_data['id'] for inc_data in data['objects']]
    assert ids == sorted(ids)

    # проверки на содержимое данных
    inc_data = data['objects'][0]
    assert set(inc_data.keys()) == {
        'id',
        'added', 'updated',
        'state', 'type', 'human_state', 'human_type',
        'user', 'group',  'node', 'our_role', 'role', 'system',
        'ident_type',
        'remote_group', 'remote_username', 'remote_node',
        'remote_data', 'remote_path', 'remote_fields'
    }

    assert {inc_data['state'] for inc_data in data['objects']} == {'active', 'resolved'}
    assert {inc_data['human_state'] for inc_data in data['objects']} == {'Активна', 'Разрешена'}
    expected = Inconsistency.ALL_TYPES - {Inconsistency.TYPE_UNDECIDED_YET}
    assert {inc_data['type'] for inc_data in data['objects']} == expected
    expected_human = {force_text(dict(Inconsistency.TYPES)[type_]) for type_ in expected}
    assert {inc_data['human_type'] for inc_data in data['objects']} == expected_human
    assert {inc_data['user']['username'] for inc_data in data['objects'] if inc_data['user']} == {'frodo', 'legolas'}

    assert data['objects'][1]['role']['id'] == imported_role.pk


def test_inconsistency_filter_by_username(api_name, client, simple_system, arda_users, responsible_gandalf,
                                          inconsistencies_for_test):
    """Фильтр по пользователю/имени пользователя"""

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')
    data = client.json.get(url, {'user': 'johnsnow'}).json()
    assert data['meta']['total_count'] == 1

    data = client.json.get(url, {'user': 'johnsnow,frodo'}).json()
    assert data['meta']['total_count'] == 5


def test_inconsistency_filter_by_group_id(api_name, client, simple_system, arda_users, department_structure,
                                          responsible_gandalf, inconsistencies_for_test):
    """Фильтр по id группы"""

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')
    data = client.json.get(url, {'group': '100'}).json()
    assert data['meta']['total_count'] == 1

    data = client.json.get(url, {'group': '100,%d' % department_structure.fellowship.external_id}).json()
    assert data['meta']['total_count'] == 3


def test_inconsistency_filter_by_subject(api_name, client, simple_system, arda_users, department_structure,
                                         responsible_gandalf, inconsistencies_for_test):
    """Фильтр по пользователю/id группы без учёта различий, пользователь это или группа"""

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')
    data = client.json.get(url, {'subject': '100,johnsnow'}).json()
    assert data['meta']['total_count'] == 2

    data = client.json.get(url, {'subject': 'frodo,%d' % department_structure.fellowship.external_id}).json()
    assert data['meta']['total_count'] == 6
    assert len([node for node in data['objects'] if node['remote_node'] == 'Node is not defined']) == 1


def test_inconsistency_filter_by_state(api_name, client, simple_system, arda_users, responsible_gandalf,
                                       inconsistencies_for_test):
    """Фильтр по статусу"""

    imported_role = inconsistencies_for_test.imported_role
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')
    data = client.json.get(url, {'state': 'resolved'}).json()

    assert data['meta']['total_count'] == 2
    assert data['objects'][0]['role']['id'] == imported_role.id
    assert data['objects'][0]['state'] == 'resolved'
    assert data['objects'][0]['user']['username'] == 'legolas'
    assert data['objects'][1]['role'] is None
    assert data['objects'][1]['type'] == 'we_have_system_dont'

    # можно указать несколько состояний
    data = client.json.get(url, {'state': 'active,resolved'}).json()
    assert data['meta']['total_count'] == inconsistencies_for_test.count


def test_inconsistency_filter_by_type(api_name, client, simple_system, arda_users, responsible_gandalf,
                                      inconsistencies_for_test):
    """Фильтр по типу"""

    imported_role = inconsistencies_for_test.imported_role
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')

    data = client.json.get(url, {'type': 'we_have_system_dont'}).json()
    assert data['meta']['total_count'] == 3

    data = client.json.get(url, {'type': 'we_have_system_dont,system_has_unknown_role'}).json()
    assert data['meta']['total_count'] == 4


def test_inconsistency_filter_by_our_role(api_name, client, simple_system, arda_users, responsible_gandalf,
                                          inconsistencies_for_test):
    """Фильтр по id роли"""

    role1, role2, role3 = inconsistencies_for_test.our_roles
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')

    data = client.json.get(url, {'our_role': role1.id}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['our_role']['id'] == role1.id
    assert data['objects'][0]['user']['username'] == role1.user.username


def test_inconsistency_filter_by_result_role(api_name, client, simple_system, arda_users, responsible_gandalf,
                                             inconsistencies_for_test):
    """Фильтр по id роли"""

    imported_role = inconsistencies_for_test.imported_role
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')

    data = client.json.get(url, {'new_role': imported_role.id}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['role']['id'] == imported_role.id
    imported_role.fetch_user()
    assert data['objects'][0]['user']['username'] == imported_role.user.username


def test_inconsistency_filter_by_node(api_name, client, simple_system, arda_users, responsible_gandalf,
                                      inconsistencies_for_test):
    """Фильтр по узлу дерева ролей"""

    client.login('gandalf')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')

    response = client.json.get(url, {'system': simple_system.slug, 'role': '/xxx/'})
    assert response.status_code == 400
    assert response.json()['errors'] == {
        'role': ['Узел в дереве ролей не найден']
    }
    response = client.json.get(url, {'system': simple_system.slug, 'role': '/superuser/'})
    assert response.status_code == 200
    data = response.json()
    assert [item['type'] for item in data['objects']] == [Inconsistency.TYPE_THEIR] * 3
    assert data['meta']['total_count'] == 3

    data = client.json.get(url, {'system': simple_system.slug, 'role': '/manager/'}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['type'] == Inconsistency.TYPE_OUR

    data = client.json.get(url, {'system': simple_system.slug, 'role': '/'}).json()
    assert data['meta']['total_count'] == 6


def test_inconsistency_sorting(api_name, client, simple_system, arda_users, responsible_gandalf,
                               inconsistencies_for_test):
    """Сортировки"""
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='inconsistencies')
    client.login('gandalf')
    extractors = {
        'type': operator.itemgetter('type'),
        'state': operator.itemgetter('state'),
        'added': lambda item: parse_datetime(item['added']),
        'updated': lambda item: parse_datetime(item['updated']),
        'user': lambda item: (
            ((item.get('our_role') or {}).get('user') or {}).get('username') or
            (item.get('user') or {}).get('username') or
            item.get('remote_username')
        ),
        'group': lambda item: (
            ((item.get('our_role') or {}).get('group') or {}).get('id') or
            (item.get('group') or {}).get('id') or
            item.get('remote_group')
        ),
        'subject': lambda item: (
            ((item.get('our_role') or {}).get('user') or {}).get('username') or
            (item.get('user') or {}).get('username') or
            item.get('remote_username') or
            ((item.get('our_role') or {}).get('group') or {}).get('id') or
            (item.get('group') or {}).get('id') or
            item.get('remote_group')
        )
    }

    def comparator(item):
        if item is None:
            result = ('', )
        elif isinstance(item, int):
            # integer must be "bigger" then any string.
            # Python2 tells us that any string is bigger then any number.
            # We trick it into otherwise by using a tilda (which is "big" enough string)
            result = ('~', item)
        else:
            result = (item, )
        return result

    for order_by, extractor in list(extractors.items()):
        for is_reversed in (False, True):
            response = client.json.get(url, {'order_by': '%s%s' % ('-' if is_reversed else '', order_by)})
            data = response.json()
            results = [extractor(item) for item in data['objects']]
            expected = sorted(results, reverse=is_reversed, key=comparator)
            notnulls, nulls = partition(lambda item: item is None, expected)
            if is_reversed:
                expected = nulls + notnulls
            else:
                expected = notnulls + nulls
            assert results == expected


def test_inconsistency_detail(api_name, client, simple_system, arda_users, responsible_gandalf,
                              inconsistencies_for_test):
    """Взять неконсистентность по id"""

    client.login('gandalf')
    legolas = arda_users.legolas
    inconsistency = Inconsistency.objects.get(user=legolas, type=Inconsistency.TYPE_THEIR)

    detail_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='inconsistencies', pk=inconsistency.pk)
    data = client.json.get(detail_url).json()
    assert data['user']['username'] == 'legolas'


def test_inconsistency_delete(api_name, client, simple_system, arda_users, responsible_gandalf,
                              inconsistencies_for_test):

    client.login('gandalf')
    legolas = arda_users.legolas
    inconsistency = Inconsistency.objects.get(user=legolas, type=Inconsistency.TYPE_THEIR)

    detail_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='inconsistencies', pk=inconsistency.pk)

    # нужно быть суперпользователем, чтобы удалять неконсистентности
    response = client.json.delete(detail_url)
    assert response.status_code == 403

    add_perms_by_role('superuser', responsible_gandalf)

    with CountIncreasedContext((Inconsistency, -1)):
        response = client.json.delete(detail_url)
    assert response.status_code == 204


def test_view_inconsistency_impersonator(client, simple_system, arda_users, responsible_gandalf,
                                         inconsistencies_for_test, generic_system):
    client.login('gimli')
    url = reverse('api_dispatch_list', api_name='v1', resource_name='inconsistencies')
    data = client.json.get(url, {'_requester': 'gandalf'}).json()
    assert data['meta']['total_count'] == 0
    add_perms_by_role('impersonator', arda_users.gimli, generic_system)

    data = client.json.get(url, {'_requester': 'gandalf'}).json()
    assert data['meta']['total_count'] == 0

    add_perms_by_role('superuser', arda_users.gandalf)
    data = client.json.get(url, {'_requester': 'gandalf'}).json()
    assert data['meta']['total_count'] == 0

    add_perms_by_role('impersonator', arda_users.gimli, simple_system)
    data = client.json.get(url, {'_requester': 'gandalf'}).json()
    assert data['meta']['total_count'] == inconsistencies_for_test.count

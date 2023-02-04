import contextlib
import datetime
import operator
import random
import re
import string
import weakref
from abc import abstractmethod
from contextlib import contextmanager
from copy import copy
from io import StringIO
from itertools import chain
from operator import attrgetter
from typing import Any, Dict, List, Optional, TypeVar, Union, Iterable, Tuple
from urllib.parse import parse_qs, urlencode, urlparse

import mock
import waffle.testutils
from django import db
from django.conf import settings
from django.core import mail
from django.core.management import call_command
from django.core.serializers.json import Serializer as JSONSerializer
from django.db import transaction
from django.db.models.query import QuerySet
from django.http import HttpRequest
from django.test.client import MULTIPART_CONTENT, Client, signals
from django.test.utils import CaptureQueriesContext
from django.utils import timezone
from django.utils.encoding import force_text
from django_idm_api.constants import IDM_CERT_ISSUERS, IDM_CERT_SUBJECTS
from django_yauth.user import BaseYandexUser
from freezegun import freeze_time
from waffle.models import Switch

from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.system import (SYSTEM_GROUP_POLICY,
                                       SYSTEM_REQUEST_POLICY)
from idm.core.exceptions import TransferStateSwitchError
from idm.core.models import (Action, GroupMembershipSystemRelation, Role,
                             RoleNode, System, SystemMetainfo, Transfer,
                             UserPassportLogin, Workflow)
from idm.core.plugins.errors import PluginFatalError
from idm.framework.requester import requesterify
from idm.inconsistencies.models import Inconsistency
from idm.permissions import utils as permissions_utils
from idm.sync import everysync
from idm.users.canonical import CanonicalMember
from idm.users.constants.group import GROUP_TYPES
from idm.users.constants.user import USER_TYPES
from idm.users.fetchers import IDSGroupFetcher
from idm.users.models import Group, GroupMembership, GroupResponsibility, User
from idm.users.queues import GroupQueue
from idm.users.sync.groups import sync_indirect_memberships
from idm.utils import json
from idm.utils.i18n import get_lang_pair

# workflow, при котором любая роль будет одобрена автоматически
DEFAULT_WORKFLOW = 'approvers = []'
API_NAMES = ('frontend', 'testapi')
TESTSERVERNAME = 'localtestserver'

T = TypeVar('T')


def assert_contains(snippets_list, text):
    """Проверяет, что все кусочки текста из списка упоминаются в тексте."""
    not_in = [
        snippet
        for snippet in snippets_list
        if snippet not in text
    ]
    if not_in:
        raise AssertionError(
            'Эти фрагменты не найдены в тексте: ' + ', '.join(not_in)
        )


@contextlib.contextmanager
def models_didnt_change(*models):
    """Контекст для проверки, что количество моделей определённого типа не изменилось.
    Пример использования:
    with models_didnt_change(User, Action):
        ... # код, который не должен изменять количество пользователей и действий
    """
    counts = {}

    for model in models:
        counts[model._meta.object_name] = model.objects.count()

    yield

    for model in models:
        count = counts[model._meta.object_name]
        assert count == model.objects.count()


class CountIncreasedContext(object):
    """Контекст для проверки, что количество моделей определённых типов увеличилось/уменьшилось
    на нужную величину.
    Пример использования:
    with CountIncreasedContext((User, 2), (Action, 1)):
        ... # код, который должен увеличить число пользователей на 2, а действий на 1
    """

    def __init__(self, *models_increments):
        self.increments = {}
        self.models = []
        for model, increment in models_increments:
            self.models.append(model)
            self.increments[model._meta.object_name] = increment
        self.counts = {}
        self.initial_state = {}
        self.changed_data = {}

    def __enter__(self):
        for model in self.models:
            name = model._meta.object_name
            self.counts[name] = model.objects.count()
            qs = model.objects.order_by('-pk')
            self.initial_state[name] = qs.first().pk if qs.exists() else -1
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is None:
            for model in self.models:
                name = model._meta.object_name
                count = model.objects.count()
                expected_increment = self.increments[name]
                got_increment = count - self.counts[name]
                assert got_increment == expected_increment
                initial_state = self.initial_state[name]
                qs = model.objects.order_by('pk')
                if initial_state != -1:
                    qs = qs.filter(pk__gt=initial_state)
                self.changed_data[name] = qs

    def get_new_objects(self, model):
        return self.changed_data[model._meta.object_name]


class RemoteDataIterator(object):
    def __init__(self, data, lookup):
        self.lookup = lookup
        self.data = data
        self.total = len(data)

    def __iter__(self):
        for thing in self.data:
            yield thing

    @property
    def first_page(self):
        """Имитируем поведение first_page для обновления wiki-групп"""
        query = self.lookup.get('_query')
        if query and 'id>' in query:
            return iter([])
        return iter(self.data)


class LocalRepo(object):
    def __init__(self, key):
        self.key = key
        self.default_data = None
        self.lookups = {}

    def set_data(self, remote_data, for_lookup=None):
        if for_lookup is None:
            self.default_data = remote_data
        else:
            self.lookups[for_lookup] = remote_data

    def getiter(self, lookup):
        remote_data = None
        for (key, value), data in self.lookups.items():
            if lookup.get(key) == value:
                remote_data = data
                break
        if remote_data is None:
            remote_data = self.default_data
        return RemoteDataIterator(remote_data, lookup)


class LocalFetcherMixin(object):
    def __init__(self):
        super(LocalFetcherMixin, self).__init__()
        KEYS = [
            ('staff', 'group'),
            ('staff', 'groupmembership'),
            ('abc', 'services'),
            ('abc', 'service_members'),
        ]
        self.repos = {}
        for key in KEYS:
            self.repos[key] = LocalRepo(key)
        self.fake_common_ancestor = True
        self.check_ancestors_db = True
        self.page_size = 1000

    def set_data(self, key, data, for_lookup=None):
        self.repos[key].set_data(data, for_lookup=for_lookup)

    @property
    def staff_groups_repo(self):
        return self.repos[('staff', 'group')]

    @property
    def staff_memberships_repo(self):
        return self.repos[('staff', 'groupmembership')]

    @property
    def abc_services_repo(self):
        return self.repos[('abc', 'services')]

    @property
    def abc_members_repo(self):
        return self.repos[('abc', 'service_members')]


def ensure_group_roots():
    return [
        Group.objects.get_or_create(type=type_type, external_id=0, defaults={'slug': type_type})[0]
        for type_type in (GROUP_TYPES.DEPARTMENT, GROUP_TYPES.SERVICE, GROUP_TYPES.WIKI)
    ]


def random_slug(length: int = 8):
    return ''.join(random.choices(string.ascii_lowercase, k=length))


def random_username():
    return f'user-{random_slug()}'


def create_user(username: str = None, *, superuser: bool = False, **kwargs) -> User:
    """Создает пользователя с заданным username и таким же паролем."""
    ensure_group_roots()
    username = username or random_username()

    try:
        user = User.objects.get(username=username)
        user.fetch_department_group()
        department_group = kwargs.pop('department_group', None)
        if department_group and (not user.department_group or user.department_group.level <= department_group.level):
            kwargs['department_group'] = department_group
        for key, value in kwargs.items():
            setattr(user, key, value)
        user.save()
    except User.DoesNotExist:
        if 'department_group' not in kwargs:
            kwargs['department_group'] = create_group(
                external_id=7,
                name='Арда',
                name_en='Arda',
                parent=Group.objects.get_root(GROUP_TYPES.DEPARTMENT),
                slug='arda',
                type=GROUP_TYPES.DEPARTMENT,
            )

        user = User.objects.create_user(username, username + '@example.yandex.ru', username)
        # Значения по умолчанию для affiliation.
        # Если affiliation присутствует в kwargs, то эти значения будут перезаписаны.
        if user.is_robot:
            user.affiliation = AFFILIATION.EXTERNAL
        else:
            user.affiliation = AFFILIATION.YANDEX
        for key, value in kwargs.items():
            setattr(user, key, value)
        center_id = kwargs.get('center_id')
        if center_id is None:
            center_id = 10000 + user.id
        user.center_id = user.staff_id = center_id
        user.save()
    if superuser:
        add_perms_by_role('superuser', user)
    return user


def create_tvm_app(tvm_id: str = None, tvm_group: Group = None):
    tvm_app = create_user(tvm_id or str(random.randint(1, 10**8)), type=USER_TYPES.TVM_APP, department_group=None)
    if not tvm_group:
        tvm_group = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, name=random_slug())
    GroupMembership.objects.create(user=tvm_app, group=tvm_group, is_direct=True, state=GROUPMEMBERSHIP_STATE.ACTIVE)
    return tvm_app


def create_group(
        external_id: int = None,
        slug: str = None,
        group_type: str = GROUP_TYPES.DEPARTMENT,
        name: Union[str, Dict[str, str]] = None,
        **kwargs,
) -> Group:
    external_id = external_id or random.randint(1, 1000)
    slug = slug or f'group-{random_slug()}'
    name_en = kwargs.pop('name_en', None)
    if name and not name_en:
        name, name_en = get_lang_pair(name)
    elif not name and not name_en:
        name = name_en = f'Group {slug}'

    kwargs['parent'] = kwargs.pop('parent', Group.objects.get_root(group_type))

    return Group.objects.get_or_create(
        external_id=external_id,
        defaults={
            'slug': slug,
            'name': name,
            'name_en': name_en,
            'type': group_type,
            **kwargs,
        },
    )[0]


def create_group_structure(
        structure: Union[List[Dict[str, Any]], Dict[str, Any]],
        root: Group,
        type_: str = GROUP_TYPES.DEPARTMENT,
):
    def _create_group_recursive(group_data: Dict[str, Any], parent: Group = None):
        """Рекурсивно проходится по полученной структуре данных и создает группы, пользователей и ответственных"""
        children = group_data.pop('children', {})
        members = group_data.pop('members', {})
        responsible = group_data.pop('responsible', [])

        group = create_group(**group_data, parent=parent)

        for children_group_data in children:
            children_group_data['group_type'] = type_
            _create_group_recursive(children_group_data, parent=group)

        for username in members:
            kwargs = {}
            if type_ == 'department':
                kwargs['department_group'] = group
            user = create_user(username, **kwargs)
            GroupMembership.objects.create(
                user=user, group=group, state=GROUPMEMBERSHIP_STATE.ACTIVE, is_direct=True
            )
        for username, rank in responsible:
            user = create_user(username)
            GroupResponsibility.objects.create(user=user, group=group, rank=rank, is_active=True)
        group.save()

    if isinstance(structure, dict):
        structure = [structure]

    for _group_data in structure:
        _group_data.update(group_type=type_)
        _create_group_recursive(_group_data, parent=root)

    sync_indirect_memberships(type_)


def get_idm_robot():
    try:
        robot = User.objects.get_idm_robot(fresh=True)
    except User.DoesNotExist:
        robot = create_user(settings.IDM_ROBOT_USERNAME)
    return robot


def create_system(
        slug: str = None,
        plugin_type: str = 'idm.tests.base.SimplePlugin',
        node_plugin_type: str = None,
        sync_role_tree: bool = True,
        role_tree: Dict[str, Any] = None,
        workflow: Optional[str] = DEFAULT_WORKFLOW,
        group_workflow: Optional[str] = DEFAULT_WORKFLOW,
        public: bool = False,
        **kwargs
) -> System:
    slug = slug or random_slug()
    get_idm_robot()  # idm/core/models/system.py:871

    defaults = {
        'slug': slug,
        'name': random_slug(),
        'name_en': random_slug(),
        'plugin_type': plugin_type,
        'node_plugin_type': node_plugin_type,
        'group_policy': SYSTEM_GROUP_POLICY.UNAWARE,
        'use_requests': True,
        'workflow_execution_method': 'plain',
    }
    defaults.update(**kwargs)
    if public:
        defaults.update(request_policy=SYSTEM_REQUEST_POLICY.ANYONE)

    system, _ = System.objects.get_or_create(**defaults)  # type: System, bool
    system.metainfo = SystemMetainfo.objects.create()
    system.save()
    if sync_role_tree:
        set_roles_tree(system, role_tree or system.plugin.get_info())
    if workflow is not None:
        set_workflow(system, workflow, group_workflow)
    return system


def clear_mailbox():
    mail.outbox = []


@contextmanager
def assert_sent_emails_count(expected):
    count_before = len(mail.outbox)
    yield
    new_emails = len(mail.outbox) - count_before
    assert new_emails == expected


def refresh(obj: T, select_related=None) -> T:
    """Перезапрашивает объект из базы и возвращает новый инстанс."""
    select_related = select_related or []
    qs = obj.__class__.objects
    return qs.select_related(*select_related).get(pk=obj.pk)


def make_role(subject, system, data, fields_data=None, organization_id=None):
    role_node = RoleNode.objects.get_node_by_data(system, data)
    role = Role.objects.create_role(subject, system, role_node, fields_data, save=True, organization_id=organization_id)
    role.set_state('requested')
    role.set_state('approved')
    role = refresh(role)
    return role


def raw_make_role(subject, system, data, fields_data=None, state='granted', system_specific=None, parent=None,
                  expire_at=None, review_at=None, ad_groups=None, updated=None, organization_id=None, **kwargs):
    role_node = RoleNode.objects.get_node_by_data(system, data)
    role = Role.objects.create_role(subject,
                                    system,
                                    role_node,
                                    fields_data,
                                    parent=parent,
                                    save=True,
                                    organization_id=organization_id)
    kwargs.update({
        'is_active': state in ROLE_STATE.ACTIVE_STATES,
        'expire_at': expire_at,
        'review_at': review_at,
        'system_specific': system_specific,
        'ad_groups': ad_groups,
    })
    if updated is not None:
        kwargs['updated'] = updated
    role.set_raw_state(state, **kwargs)

    if isinstance(fields_data, dict) and 'passport-login' in fields_data:
        login = UserPassportLogin.objects.add(fields_data['passport-login'], role)
        login.is_fully_registered = True
        login.save(update_fields=['is_fully_registered'])
    return role


def make_inconsistency(**kwargs):
    defaults = {
        'state': 'active',
        'ident_type': 'data',
        'remote_fields': None,
    }
    defaults.update(kwargs)
    path = defaults.pop('path', None)
    if path:
        defaults['node'] = RoleNode.objects.get_node_by_slug_path(defaults['system'], path)
    our_role = defaults.get('our_role')
    if our_role is not None:
        defaults['node'] = our_role.node
    inconsistency = Inconsistency.objects.create(**defaults)
    return inconsistency


def patch_role(role, **kwargs):
    for key, value in kwargs.items():
        setattr(role, key, value)

    role.save(update_fields=list(kwargs.keys()))


def expire_role(role, days=3):
    patch_role(role, expire_at=timezone.now() - datetime.timedelta(days=days))


def role_actions(role: 'Role') -> List[str]:
    return list(role.actions.order_by('id').values_list('action', flat=True))


def all_actions() -> List[str]:
    return list(Action.objects.order_by('id').values_list('action', flat=True))


def assert_action_chain(role, expected_chain):
    actions = role_actions(role)
    assert actions == expected_chain


class NumQueryAssertion(CaptureQueriesContext):
    message = '%d queries executed, %d expected'

    def __init__(self, expected_query_count, show_queries=True, connection=None):
        if connection is None:
            connection = db.connection

        super(NumQueryAssertion, self).__init__(connection)

        self.expected_query_count = expected_query_count
        self.show_queries = show_queries

    def __exit__(self, exc_type, exc_value, traceback):
        super(NumQueryAssertion, self).__exit__(exc_type, exc_value, traceback)

        if exc_type is not None:
            return

        executed = len(self)
        result = self.test_passed(self.expected_query_count)
        if not result:
            msg = self.message % (executed, self.expected_query_count)

            if self.show_queries:
                queries = [query['sql'] for query in self.of_interest()]
                msg += "\nCaptured queries were:\n%s" % '\n'.join(queries)

            raise AssertionError(msg)

    def test_passed(self, expected):
        return len(self) == expected

    def of_interest(self):
        return self.captured_queries


class NumQueryLessOrEqualAssertion(NumQueryAssertion):
    message = '%d queries executed, <= than %d expected'

    def test_passed(self, expected):
        return len(self) <= expected


class NumModelQueryAssertion(NumQueryAssertion):
    def __init__(self, model, *args, **kwargs):
        self.model = model
        self.exclude_estimated = kwargs.pop('exclude_estimated', True)
        super(NumModelQueryAssertion, self).__init__(*args, **kwargs)

    def test_passed(self, expected):
        model_queries = [query for query in self if self.model._meta.db_table in query['sql']]
        if self.exclude_estimated:
            model_queries = [query for query in model_queries if 'count_estimate' not in query['sql']]
        return len(model_queries) == expected

    def of_interest(self):
        model_queries = [query for query in self if self.model._meta.db_table in query['sql']]
        return model_queries


# Better looking aliases
assert_num_queries = NumQueryAssertion
assert_num_queries_lte = NumQueryLessOrEqualAssertion
assert_num_model_queries = NumModelQueryAssertion


def assert_http(mocked, url=None, data=None, timeout=None):
    assert len(mocked.call_args_list) == 1
    call = mocked.call_args_list[0]
    if url is not None:
        assert call[0][0] == url
    assert call[1]['data'] == data
    if timeout is not None:
        assert call[1]['timeout'] == timeout


def assert_raw_http(mocked, url=None, method=None, data=None, cert=None, timeout=None):
    assert len(mocked.call_args_list) == 1
    call = mocked.call_args_list[0]
    call_kwargs = call[1]
    if url is not None:
        assert call_kwargs['url'] == url
    if method is not None:
        assert call_kwargs['method'] == method
    assert call_kwargs['data'] == data
    if timeout is not None:
        assert call_kwargs['timeout'] == timeout
    if cert is not None:
        assert call_kwargs['cert'] == cert


@contextlib.contextmanager
def catch_templates():
    holder = {
        'templates': [],
        'context': {},
    }

    def _signal_handler(signal, sender, template, context, **kwargs):
        holder['templates'].append(template.name)
        holder['context'][template.name] = copy(context)

    signals.template_rendered.connect(_signal_handler, dispatch_uid='catch-templates')

    yield holder

    signals.template_rendered.disconnect(dispatch_uid='catch-templates')


def compare_time(left, right, epsilon=1):
    """Сравнивает два времени, и если разница между
    ними менше epsilon секунд - возврщает True"""
    delta = left - right
    delta = delta.days * 24 * 60 * 60 + delta.seconds
    return abs(delta) <= epsilon


def create_fake_response(content, status_code=200):
    if not isinstance(content, str):
        content = json.dumps(content)
    fake_response = Response(status_code, content, '', '')
    return fake_response


add_perms_by_role = permissions_utils.add_perms_by_role
remove_perms_by_role = permissions_utils.remove_perms_by_role


def set_workflow(system, code=DEFAULT_WORKFLOW, group_code='', user=None, bypass_checks=True):
    if not user:
        user = get_idm_robot()

    workflow = Workflow.objects.create(
        system=system,
        workflow=code,
        group_workflow=group_code,
        state='commited',
        user=user,
        approver=user,
        approved=timezone.now(),
    )

    workflow.approve(user, send_mail=False, bypass_checks=bypass_checks)
    return workflow


def sync_role_nodes(system, requester=None, from_api=False):
    requester = requesterify(requester)
    everysync.sync_roles_and_nodes(
        system,
        steps=everysync.SYNC_NODES_ONLY,
        requester=requester,
        force_nodes=True,
        block=True,
        from_api=from_api
    )


def sync_roles(system, requester):
    requester = requesterify(requester)
    everysync.sync_roles_and_nodes(
        system,
        steps=everysync.SYNC_ROLES_ONLY,
        requester=requester,
        force_roles=True,
        block=True,
    )


class MockedTrackerIssue(object):
    def __init__(self, key):
        self.key = key
        self.transitions = {'close': self}

    execute = mock.Mock()


@contextlib.contextmanager
def mock_ids_repo(repo_key, *args, **kwargs):
    class MockedIntrasearchRepository(object):
        def __init__(self, layer, objects, exception=None):
            self.layer = layer
            self.objects = objects
            if exception is None:
                self.get.side_effect = self.make_response
            else:
                self.get.side_effect = exception

        def make_response(self, lookup, **options):
            return [{
                'layer': self.layer,
                'result': self.objects,
                'pagination': {
                    'page': lookup.get('{}.page'.format(self.layer), 0),
                    'per_page': lookup.get('{}.per_page'.format(self.layer), 5),
                    'pages': 1234
                }
            }]

        get = mock.Mock()

    class MockedStaffPersonRepository(object):
        def __init__(self, _object, exception=None):
            self.object = _object
            if exception is None:
                self.get_one.side_effect = self.make_response
            else:
                self.get_one.side_effect = exception

        def make_response(self, *args, **options):
            return self.object

        get_one = mock.Mock()

    class MockedTrackerRepository(object):

        create = mock.Mock()
        get_one = mock.Mock()

    mocked_repo_classes = {
        'intrasearch': MockedIntrasearchRepository,
        'startrek2': MockedTrackerRepository,
        'staff.person': MockedStaffPersonRepository,
    }

    with mock.patch('ids.registry.registry.get_repository') as get_repository:
        mocked_repo_class = mocked_repo_classes[repo_key]
        get_repository.return_value = mocked_repo_class(*args, **kwargs)
        yield mocked_repo_class


def set_roles_tree(system: System, data: Dict[str, Any]):
    with mock_tree(system, data):
        system.synchronize(force_update=True)
    system.actions.filter(action='role_tree_started_sync').delete()


@contextlib.contextmanager
def crash_system(system):
    with mock.patch.object(system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = PluginFatalError(1, 'weird error', {})
        yield


@contextlib.contextmanager
def mock_tree(system, tree):
    with mock.patch.object(system.plugin.__class__, 'get_info') as get_info:
        with mock.patch.object(system.node_plugin.__class__, 'get_info') as get_info_node:
            if isinstance(tree, Exception) or isinstance(tree, type) and issubclass(tree, Exception):
                get_info.side_effect = tree
                get_info_node.side_effect = tree
            else:
                get_info.return_value = tree
                get_info_node.return_value = tree

            yield


@contextlib.contextmanager
def disable_tasks():
    with mock.patch('idm.framework.task.BaseTask.run'):
        yield


def use_proxied_suggest(should_use=True):
    return waffle.testutils.override_switch('idm.proxied_suggest', active=should_use)


@contextlib.contextmanager
def use_intrasearch_for_roles(should_use=True):
    switch, created = Switch.objects.update_or_create(
        name='idm.use_intrasearch_for_roles', defaults={'active': should_use}
    )
    yield
    switch.active = not should_use
    switch.save()


@contextlib.contextmanager
def set_read_only():
    switch, created = Switch.objects.update_or_create(name='idm.readonly', defaults={'active': True})
    yield
    switch.active = False
    switch.save()


@contextlib.contextmanager
def enable_intrasearch_pushes(enable=True):
    prev, settings.IDM_SEND_INTRASEARCH_PUSHES = settings.IDM_SEND_INTRASEARCH_PUSHES, enable
    with capture_requests(answer={'status': 'ok'}) as pusher:
        yield pusher
    settings.IDM_SEND_INTRASEARCH_PUSHES = prev


def add_client_certificate_data(extra):
    if 'HTTP_X_QLOUD_SSL_ISSUER' not in extra and extra.get('HTTP_X_QLOUD_SSL_SUBJECT'):
        extra['HTTP_X_QLOUD_SSL_ISSUER'] = IDM_CERT_ISSUERS[1]
    if extra.get('with_idm_credentials'):
        extra['HTTP_X_QLOUD_SSL_VERIFIED'] = 'True'
        extra['HTTP_X_QLOUD_SSL_SUBJECT'] = IDM_CERT_SUBJECTS[1]
        extra['HTTP_X_QLOUD_SSL_ISSUER'] = IDM_CERT_ISSUERS[1]


class IDMClient(Client):
    def __init__(self, *args, **kwargs):
        super(IDMClient, self).__init__(*args, **kwargs)
        self.json = JsonClient(weakref.proxy(self))

    def _base_environ(self, **request):
        environ = super(IDMClient, self)._base_environ(**request)
        environ['SERVER_NAME'] = TESTSERVERNAME
        environ.update(**request)
        return environ

    def get(self, path, data=None, follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).get(path, data, follow, secure, **extra)

    def post(self, path, data=None, content_type=MULTIPART_CONTENT, follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).post(path, data, content_type, follow, secure, **extra)

    def put(self, path, data='', content_type='application/octet-stream', follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).put(path, data, content_type, follow, secure, **extra)

    def patch(self, path, data='', content_type='application/octet-stream', follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).patch(path, data, content_type, follow, secure, **extra)

    def delete(self, path, data='', content_type='application/octet-stream', follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).delete(path, data, content_type, follow, secure, **extra)

    def head(self, path, data=None, follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).head(path, data, follow, secure, **extra)

    def options(self, path, data='', content_type='application/octet-stream', follow=False, secure=False, **extra):
        add_client_certificate_data(extra)
        return super(IDMClient, self).options(path, data, content_type, follow, secure, **extra)


class JsonClient(object):
    """Класс, аналогичный django.test.client.Client, но делающий все запросы с заголовком content_type,
    равным application/json, а для запросов post, put и patch кодирующий тело запроса в JSON.
    Также добавляет к ответу метод json(), который пытается декодировать тело ответа из формата JSON.
    """

    def __init__(self, client):
        self.client = client

    def get(self, path, data=None, **extra):
        add_client_certificate_data(extra)
        return self.jsonify(self.client.get(path, data=data, **extra))

    def post(self, path, data=None, content_type='application/json', **extra):
        add_client_certificate_data(extra)
        return self.jsonify(
            self.client.post(path, data=json.dumps(data) if data else data, content_type=content_type, **extra)
        )

    def head(self, path, data=None, **extra):
        add_client_certificate_data(extra)
        return self.jsonify(self.client.head(path, data=data, **extra))

    def options(self, path, data='', content_type='application/json', **extra):
        add_client_certificate_data(extra)
        return self.jsonify(self.client.options(path, data=data, content_type=content_type, **extra))

    def put(self, path, data='', content_type='application/json', **extra):
        add_client_certificate_data(extra)
        return self.jsonify(
            self.client.put(path, data=json.dumps(data) if data else data, content_type=content_type, **extra)
        )

    def patch(self, path, data='', content_type='application/json', **extra):
        add_client_certificate_data(extra)
        return self.jsonify(
            self.client.patch(path, data=json.dumps(data) if data else data, content_type=content_type, **extra)
        )

    def delete(self, path, data='', query=None, content_type='application/json', **extra):
        add_client_certificate_data(extra)
        query = {} if query is None else query
        env = {
            'QUERY_STRING': urlencode(query, doseq=True),
        }
        env.update(extra)
        return self.jsonify(
            self.client.delete(path, data=json.dumps(data) if data else data, content_type=content_type, **env)
        )

    def jsonify(self, response):
        response.json = lambda: json.loads(force_text(response.content)) if hasattr(response, 'content') else None
        return response


class attrdict(dict):
    def __getattr__(self, item):
        return self[item]


class Serializer(JSONSerializer):
    """json_with_no_dates"""
    excluded_fields = ('updated', 'added', 'updated_at', 'removed_at', 'created_at')

    def handle_field(self, obj, field):
        if field.name not in self.excluded_fields:
            return super(Serializer, self).handle_field(obj, field)

    def get_dump_object(self, obj):
        if hasattr(obj, 'natural_key'):
            pk = obj.natural_key()
        elif obj._meta.object_name == 'System':
            pk = obj.slug
        else:
            raise ValueError('Should not be here')
        return {
            "pk": pk,
            "model": force_text(obj._meta),
            "fields": self._current
        }


@contextlib.contextmanager
def mock_all_roles(system, user_roles=None, group_roles=None, side_effect=None):
    with mock.patch.object(system.plugin.__class__, 'get_all_roles') as get_all_roles:
        if side_effect:
            get_all_roles.side_effect = side_effect
        else:
            retvalue = {}
            if user_roles is not None:
                retvalue['users'] = user_roles
            if group_roles is not None:
                retvalue['groups'] = group_roles
            get_all_roles.return_value = retvalue
        yield


@contextlib.contextmanager
def mock_group_memberships(system, memberships=None, side_effect=None):
    with mock.patch.object(system.plugin.__class__, 'get_memberships') as get_memberships:
        if side_effect:
            get_memberships.side_effect = side_effect
        else:
            get_memberships.return_value = memberships
        yield


@contextlib.contextmanager
def mock_roles(system, roles=None, side_effect=None):
    with mock.patch.object(system.plugin.__class__, 'get_roles') as get_roles:
        if side_effect:
            get_roles.side_effect = side_effect
        else:
            get_roles.return_value = iter(roles)
        yield


@contextlib.contextmanager
def capture_plugin_data(system, answer=None, side_effect=None):
    with mock.patch.object(system.plugin.__class__, '_send_data') as sender:
        if answer is not None:
            sender.return_value = answer
        if side_effect is not None:
            sender.side_effect = side_effect
        yield sender


@contextlib.contextmanager
def capture_http(system, answer=None, side_effect=None):
    if system.use_requests:
        get = 'idm.utils.http.get'
        post = 'idm.utils.http.post'
        delete = 'idm.utils.http.delete'
    else:
        get = 'idm.utils.curl.get'
        post = 'idm.utils.curl.post'
        delete = 'idm.utils.curl.delete'
    with mock.patch(get) as mocked_get, mock.patch(post) as mocked_post, mock.patch(delete) as mocked_delete:
        if answer is not None:
            if isinstance(answer, dict):
                answer = Response(200, answer)
            mocked_get.return_value = answer
            mocked_post.return_value = answer
            mocked_delete.return_value = answer
        if side_effect is not None:
            mocked_get.side_effect = side_effect
            mocked_post.side_effect = side_effect
            mocked_delete.side_effect = side_effect
        yield attrdict({
            'http_get': mocked_get,
            'http_post': mocked_post,
            'http_delete': mocked_delete
        })


@contextlib.contextmanager
def capture_requests(answer=None, side_effect=None):
    with mock.patch('idm.utils.http.get') as mocked_get, mock.patch('idm.utils.http.post') as mocked_post, \
            mock.patch('idm.utils.http.delete') as mocked_delete:
        if answer is not None:
            if isinstance(answer, dict):
                answer = Response(200, answer)
            mocked_get.return_value = answer
            mocked_post.return_value = answer
            mocked_delete.return_value = answer
        if side_effect is not None:
            mocked_get.side_effect = side_effect
            mocked_post.side_effect = side_effect
            mocked_delete.side_effect = side_effect
        yield attrdict({
            'http_get': mocked_get,
            'http_post': mocked_post,
            'http_delete': mocked_delete
        })


@contextlib.contextmanager
def capture_raw_http(system, answer=None, side_effect=None):
    if system.use_requests:
        with mock.patch('requests.sessions.Session.request') as mocked:
            if answer is not None:
                mocked.return_value = answer
            if side_effect is not None:
                mocked.side_effect = side_effect
            yield mocked
    else:
        with mock.patch('pycurl.Curl') as mocked_constructor:
            mocked_curl = mock.MagicMock()
            mocked_constructor.return_value = mocked_curl
            with mock.patch('idm.utils.curl.BytesIO') as request_io:
                if answer is not None:
                    mocked_curl.getinfo.return_value = answer.status_code
                    request_io.return_value = StringIO(answer.content)
                yield mocked_curl


@contextlib.contextmanager
def ignore_tasks():
    from idm.celery_app import app
    with contextlib.ExitStack() as stack:
        for task in app._tasks:
            if 'idm' in task:
                stack.enter_context(mock.patch(task, new=mock.MagicMock()))

        yield


def assert_inconsistency(inconsistency, **kwargs):
    for key, value in kwargs.items():
        if key == 'path':
            assert inconsistency.node is not None
            assert inconsistency.node.slug_path == value, 'Expected: {}, got: {}'.format(value,
                                                                                         inconsistency.node.slug_path)
        else:
            assert getattr(inconsistency, key) == value, 'Expected: {}, got: {}'.format(value,
                                                                                        getattr(inconsistency, key))


def assert_sorted(items, keyfunc, descending=False):
    if not items:
        return

    if descending:
        testfunc = lambda a, b: a >= b
    else:
        testfunc = lambda a, b: a <= b

    previous = keyfunc(items[0])
    for item in items:
        current = keyfunc(item)

        if not testfunc(previous, current):
            raise AssertionError("{} should go after {}".format(previous, current))
        previous = current


def assert_response(response, **kwargs):
    assert response.status_code == 200
    data = response.json()
    meta = kwargs.pop('meta', None)
    if meta:
        assert data['meta'] == meta
    assert_details(data, **kwargs)


def assert_details(data, **kwargs):
    for key, expected in list(kwargs.items()):
        result = {user[key] for user in data['data']}
        assert result == expected


def assert_stable_hash(system):
    system.root_role_node.refresh_from_db()
    prevhash = system.root_role_node.hash
    RoleNode.objects.filter(system=system).update(hash='')
    system.root_role_node.refresh_from_db()
    system.root_role_node.rehash()
    system.root_role_node.refresh_from_db()
    newhash = system.root_role_node.hash
    assert prevhash == newhash


def assert_approvers(role_request, expected, format='short'):
    expected_role_request_keys = {'id', 'added', 'approves', 'is_done'}
    if format == 'full':
        expected_role_request_keys |= {'requester'}

    expected_approve_request_keys = {
        'approver', 'approved', 'decision', 'is_done',
    }

    approves = []
    assert role_request.keys() == expected_role_request_keys
    for approve in role_request['approves']:
        assert approve.keys() == {'status', 'requests'}
        usernames = []
        for request in approve['requests']:
            assert request.keys() == expected_approve_request_keys
            usernames.append(request['approver']['username'])
        approves.append(usernames)
    assert approves == expected


class Response(object):
    """Эмулятор requests.response"""

    def __init__(self, status_code, content, headers=None, url=None):
        self.status_code = status_code
        self.content = content
        if isinstance(self.content, dict):
            self.content = json.dumps(self.content)
        self.headers = headers
        self.url = url

    def __repr__(self):
        return '<Response [%s]>' % (self.status_code)

    @property
    def text(self):
        return force_text(self.content)

    def json(self, **kwargs):
        return json.loads(self.text)

    def raise_for_status(self):
        pass


def ctt_data_is_consistent(obj):
    path_ids = set()

    current_node = obj
    while current_node:
        path_ids.add(current_node.id)
        current_node = current_node.parent

    closure_query = obj._closure_model.objects.filter(child_id=obj.id)
    closure_ids = {c.parent_id for c in closure_query}

    return path_ids == closure_ids


def remove_members(group, members):
    group = Group.objects.prefetch_for_remove_members().get(pk=group.pk)
    canonicals = [CanonicalMember(member.center_id, state=GROUPMEMBERSHIP_STATE.ACTIVE) for member in members]
    group.remove_members(canonicals)
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')


def add_members(group, members, add_to_service=False):
    canonicals = [CanonicalMember(member.center_id, state=GROUPMEMBERSHIP_STATE.ACTIVE) for member in members]
    group.add_members(canonicals)
    if add_to_service and group.type == GROUP_TYPES.SERVICE and group.level == 2:
        group.parent.add_members(canonicals)
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')


def change_department(user, group_leaved, group_joined):
    group_leaved = Group.objects.prefetch_for_remove_members().get(pk=group_leaved.pk)
    group_joined = refresh(group_joined)
    remove_members(group_leaved, [user])
    add_members(group_joined, [user])


def collect_items(group, queue=None, new_parent=None, parent_item=None):
    if queue is None:
        queue = GroupQueue()
    movement = queue.push_movement(node=group, parent_item=parent_item, new_parent=new_parent)
    for subgroup in group.get_child_nodes(include_depriving=False):
        collect_items(subgroup, queue, new_parent=group, parent_item=movement)
    return queue


def move_group(group, new_parent):
    queue = collect_items(group, new_parent=new_parent)
    queue.apply()
    sync_indirect_memberships(group.type)
    Transfer.objects.create_user_group_transfers()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')


def accept(transfers):
    if isinstance(transfers, QuerySet):
        transfers = transfers.select_related('user', 'group')

    for transfer in transfers:
        try:
            transfer.accept(bypass_checks=True)
        except TransferStateSwitchError:
            pass


def is_subdict(superset, subset):
    return all(item in superset.items() for item in subset.items())


@contextmanager
def days_from_now(days, now=None):
    if now is None:
        now = timezone.now()
    freezer = freeze_time(now + timezone.timedelta(days=days))
    freezer.start()
    yield
    freezer.stop()


def batch_sql(connection, batch):
    with connection.cursor() as cursor:
        for sql in batch:
            cursor.execute(sql)


def make_absent(approvers):
    """
        Сотрудники будут отсутствовать больше 10 часов в ближайшие сутки
    """
    ids = [approver.id for approver in approvers]
    User.objects.filter(id__in=ids).update(is_absent=True)


def make_attending(approvers):
    """
        Сотрудники будут отсутствовать меньше 10 часов в ближайшие сутки
    """
    ids = [approver.id for approver in approvers]
    User.objects.filter(id__in=ids).update(is_absent=False)


def get_all_roles(client, url):
    query = {}
    roles = []
    while True:
        response = client.json.get(url, query, with_idm_credentials=True)
        data = response.json()
        roles += data['roles']
        next_url = data.get('next-url')
        if not next_url:
            break
        query_string = urlparse(next_url).query if next_url else None
        query = {key: value[0] for key, value in parse_qs(query_string).items()} if query_string else {}
    return roles


def setify_changes(changes):
    for table in changes.values():
        table['obtained'] = set(table['obtained'])
        table['lost'] = set(table['lost'])
        table['ancestors'] = set(table['ancestors'])


def get_recievers(outbox):
    return set(chain(*map(attrgetter('to'), mail.outbox)))


def setify(container):
    for key, value in container.items():
        if isinstance(value, list):
            container[key] = set(value)
    return container


def force_awaiting_memberships_role_grant(role: Role) -> None:
    role.refresh_from_db()
    assert role.state == ROLE_STATE.AWAITING
    call_command('idm_sync_groupmembership_system_relations')
    GroupMembershipSystemRelation.objects.update(state='activated')
    Role.objects.poke_awaiting_roles()
    role.refresh_from_db()


def refresh_from_db(*objects):
    for obj in objects:
        obj.refresh_from_db()


def get_mocked_tvm_client(src: str = None, uid: str = None):
    class TVMClient(object):
        def parse_service_ticket(self, *args, **kwargs):
            if src is None:
                return None

            result = mock.Mock()
            result.src = src
            return result

        def parse_user_ticket(self, *args, **kwargs):
            if uid is None:
                return None

            result = mock.Mock()
            result.default_uid = uid
            return result

    return TVMClient()


@contextlib.contextmanager
def nullcontext(enter_result=None):
    yield enter_result


class FakeRequest(HttpRequest):
    def __init__(
            self,
            *,
            method: str = 'GET',
            path: str = '',
            get: dict = None,
            post: dict = None,
            meta: dict = None,
            is_ajax: bool = False,
            yauser: BaseYandexUser = None,
            user: User = None,
    ):
        super().__init__()
        self.method = method
        self.path = path
        self._is_ajax = is_ajax
        if get:
            self.GET.update(get)
        if post:
            self.POST.update(post)
        if meta:
            self.META.update(meta)

        if yauser:
            self.yauser = yauser
        if user:
            self.user = user

    def is_ajax(self):
        return self._is_ajax


def members_created(members_count, actions_count, groups_count=0):
    return CountIncreasedContext(
        (Group, groups_count),
        (GroupResponsibility, 0),
        (GroupMembership, members_count),
        (User, 0),
        (Action, actions_count),
    )


class LocalFetcher(LocalFetcherMixin, IDSGroupFetcher):
    pass


@contextmanager
def set_switch(switch_name: str, value: bool):
    """установить waffle.switch в заданное значение"""
    switch, _ = Switch.objects.get_or_create(name=switch_name)
    old_value = switch.active
    switch.active = value
    switch.save()
    switch.flush()
    assert waffle.switch_is_active(switch_name) == value
    try:
        yield switch
    finally:
        switch.active = old_value
        switch.save()
        switch.flush()
        assert waffle.switch_is_active(switch_name) == old_value


@contextmanager
def run_commit_hooks():
    """
    Используется для запуска функций, переданных в transaction.on_commit,
    которые не запускаются в нетранзакционных тестах,
    потому что каждый запуск такого теста оборачивается в транзакцию
    """
    yield
    connection = transaction.get_connection()
    with mock.patch.object(connection, 'validate_no_atomic_block', return_value=False):
        while connection.run_on_commit:
            connection.run_and_clear_commit_hooks()


class _MockedCollection:
    expression_regex = re.compile(r'^\$\w+$')
    expressions = {
        '$in': lambda value, items: value in items,
        '$eq': lambda value, other: value == other,
    }

    def __init__(self, items: Iterable[T] = None):
        self._items = []
        if items:
            self._items.extend(items)

    def __iter__(self) -> Iterable[T]:
        return iter(self._items)

    def __len__(self):
        return len(self._items)

    @staticmethod
    def get_jsonpath(obj, path: Tuple[str] = (), safe: bool = False):
        origin = obj
        for idx, key in enumerate(path):
            try:
                obj = obj[key]
            except (IndexError, KeyError, TypeError) as e:
                if safe:
                    return None
                raise KeyError(f'Can not retrieve path {path[:idx+1]} from {origin}') from e
        return obj

    def _check_filter_expression(self, value, expression: str, args: Dict[str, Any]) -> bool:
        if expression not in expression:
            raise ValueError(f'_{self.__class__.__name__} don\'t support {expression} expression')
        return self.expressions[expression](value, args)

    @classmethod
    def _build_filter_expressions(cls, query: Dict[str, Any]) -> List[Tuple[tuple, Any, Any]]:
        filter_expressions = []
        query_queue = [((), k,v) for k, v in query.items()] # type: List[Tuple[tuple, Any, Any]]
        default_expression = '$eq'
        while query_queue:
            json_path, key, value_or_filter = query_queue.pop(0)
            if cls.expression_regex.match(key):
                filter_expressions.append((json_path, key, value_or_filter))
            elif isinstance(value_or_filter, dict):
                for _key, _value_or_filter in value_or_filter.items():
                    query_queue.append(((*json_path, key), _key, _value_or_filter))
            else:
                filter_expressions.append(((*json_path, key), default_expression, value_or_filter))
        return filter_expressions

    def _apply_filter(self, query: Dict[str, Any] = None, indexed: bool = False, **_) -> Iterable[T]:
        if not isinstance(query, dict):
            raise ValueError()

        for idx, item in enumerate(self):
            for json_path, expression, args in self._build_filter_expressions(query):
                if not self._check_filter_expression(self.get_jsonpath(item, json_path, safe=True), expression, args):
                    break
            else:
                if indexed:
                    yield idx, item
                else:
                    yield item

    def estimated_document_count(self, **_) -> int:
        return len(self._items)

    def count(self, query: Dict[str, Any] = None, **_) -> int:
        if not query:
            return self.estimated_document_count()
        return len(list(self._apply_filter(query)))

    count_documents = count

    def find(self, query: Dict[str, Any] = None, **_) -> '_MockedCollection':
        if not query:
            return self
        return _MockedCollection(self._apply_filter(query))

    def insert_one(self, item: T, **_):
        if '_id' not in item:
            item['_id'] = random_slug()
        self._items.append(item)
        return item['_id']

    def insert_many(self, *items: T, **_):
        for item in items:
            if '_id' not in item:
                item['_id'] = random_slug()
        self._items.extend(items)
        return [item['_id'] for item in items]

    def delete_one(self, query: Dict[str, Any], **_):
        if not query:
            return

        try:
            idx, _ = next(iter(self._apply_filter(query)))
            self._items.pop(idx)
        except StopIteration:
            pass

    def delete_many(self, query: Dict[str, Any], **_):
        if not query:
            return

        for idx, _ in list(self._apply_filter(query, indexed=True))[::-1]:
            self._items.pop(idx)


class _MockedMongo:
    def __getitem__(self, item: str) -> _MockedCollection:
        return getattr(self, item)

    def __getattribute__(self, item: str):
        try:
            return super().__getattribute__(item)
        except AttributeError:
            collection = _MockedCollection()
            setattr(self, item, collection)
            return collection

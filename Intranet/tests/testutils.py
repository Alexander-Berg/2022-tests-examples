# -*- coding: utf-8 -*-
import contextlib
import datetime
import pprint
import pytest
import random
import re
import string
import os
import yatest

from sqlalchemy.orm import Session

from intranet.yandex_directory.src import blackbox_client

from contextlib import contextmanager
from copy import copy
from copy import deepcopy
from functools import wraps
from itertools import count
from urllib.parse import (
    urlparse,
    parse_qsl,
)

from flask import g
from werkzeug.datastructures import Headers
from hamcrest import (
    equal_to,
    contains,
    assert_that,
    has_properties,
    has_entries,
    has_length,
    not_none,
    greater_than_or_equal_to,
    contains_inanyorder,
    all_of,
)
from hamcrest.core.base_matcher import BaseMatcher
from hamcrest.core.core.raises import Raises
from unittest.mock import patch, Mock
from requests.utils import parse_header_links

from intranet.yandex_directory.src.blackbox_client import odict
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.middlewares import Service, User, TeamUser, AuthMiddleware
from intranet.yandex_directory.src.yandex_directory.common import backpressure
from intranet.yandex_directory.src.yandex_directory.common import json
from intranet.yandex_directory.src.yandex_directory.common.db import (
    get_meta_connection,
    get_main_connection,
    set_permanent_meta_connection,
    set_permanent_main_connection,
)
from intranet.yandex_directory.src.yandex_directory.common.db import mogrify
from intranet.yandex_directory.src.yandex_directory.common.models.base import ALL_FIELDS
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    get_localhost_ip_address,
    url_join,
    NotGiven,
    utcnow,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import (
    action_organization_add,
    action_department_add,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.users.base import create_portal_user
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.features import CAN_WORK_WITHOUT_OWNED_DOMAIN
from intranet.yandex_directory.src.yandex_directory.core.features import DOMAIN_AUTO_HANDOVER
from intranet.yandex_directory.src.yandex_directory.core.features.utils import get_feature_by_slug
from intranet.yandex_directory.src.yandex_directory.core.features.utils import set_feature_value_for_organization
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    DomainModel,
    DepartmentModel,
    UserModel,
    UserMetaModel,
    GroupModel,
    ResourceModel,
    ServiceModel,
    OrganizationModel,
    OrganizationMetaModel,
    OrganizationBillingInfoModel,
    OrganizationSsoSettingsModel,
    ActionModel,
    EventModel,
    OrganizationServiceModel,
    PresetModel,
    PartnersMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import MAILLIST_SERVICE_SLUG
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    update_license_cache_task,
    enable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import SyncResult
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserRoles
from intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions import all_internal_admin_permissions
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    global_permissions,
    department_permissions,
    group_permissions,
    user_permissions,
    organization_permissions,
    all_global_permissions,
    service_permissions, sso_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    create_organization,
    is_outer_uid,
    create_root_department,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    create_organization as original_create_organization,
    build_email,
)
from intranet.yandex_directory.src.yandex_directory.core.views.api_versioning import build_cached_view_methods_map
from intranet.yandex_directory.src.yandex_directory.core.features.utils import get_organization_features_info
from intranet.yandex_directory.src.yandex_directory.setup import setup_app
from intranet.yandex_directory.src.yandex_directory.core.views.binding_widget.logic import BIND_ACCESS_SKIP_CHECK

NOT_FOUND_RESPONSE = {
    'message': 'Not found',
    'code': 'not_found',
}


PASSPORT_TEST_OUTER_UID = 14124  # passport-login: test-test@yandex.ru


AUTH_UID = None
# app.config['USER_DATA_FOR_TEST']['uid']
OAUTH_CLIENT_ID = 'oauth_client_id'
OAUTH_TOKEN = 'AAAjsdhfjhsjfhskjhfkshfd'

get_random_string = lambda N: ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(N))


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["Y_PYTHON_SOURCE_ROOT"], path)


def create_outer_admin(meta_connection,
                       main_connection,
                       num_organizations=1):

    """Создает N организаций, генерит произвольный uid
    и делает его внешним админом этих организаций.

    Возвращает uid и список id созданных организаций.
    """
    million = 1000000
    uid = random.randint(1 * million, 2 * million)
    assert is_outer_uid(uid)
    org_ids = []
    revisions = []

    for i in range(num_organizations):
        random_label = 'org_{0}'.format(random.randint(1, million))
        meta = create_organization(
            meta_connection,
            main_connection,
            name={'ru': random_label},
            label=random_label,
        )
        org_id = meta['organization']['id']
        UserMetaModel(meta_connection).create(uid, org_id)

        org_ids.append(org_id)
        revisions.append(meta['organization']['revision'])

    return uid, org_ids, revisions


def get_auth_headers(as_uid=None, as_org=None, as_outer_admin=None, as_anonymous=False, token=None, as_cloud_uid=None,
                     with_uid=True):
    """
    Возвращает заголовок для тестов. Вызывая функцию без аргументов, считаем, что
    тестируется API с заголовком, содержащим только uid внутреннего админа организации.
    :param as_uid: можем передать uid, отличный от внутреннего админа
    :param as_org: можем передать только org_id организации (для внутренних сервисов)
    :param as_outer_admin: dict: org_id - ID организации, id - uid внешнего админа
    :param as_anonymous: boolean, если True, то имитуруем вызов API без uid-a & org_id.
    :return: dict, заголовок.
    """

    token = token or app.config['INTERNAL_SERVICES_BY_IDENTITY']['autotest']['tokens'][0]

    auth_header = Headers({
        'Authorization':  'Token %s' % token,
        # IP пользователя, пришедшего на сервис (например в Портал)
        'X-User-IP': get_localhost_ip_address(),
        # IP бэкенда сервиса, с которого идёт обращение к API
        'X-Real-IP': get_localhost_ip_address(),
    })

    if as_anonymous:
        return auth_header

    if as_org is not None:
        if isinstance(as_org, dict):
            as_org = as_org['id']
        auth_header['X-ORG-ID'] = as_org
        if as_uid is not None:
            auth_header['X-UID'] = as_uid or AUTH_UID

    elif as_outer_admin is not None:
        if isinstance(as_outer_admin, dict):
            if 'org_id' in as_outer_admin:
                auth_header['X-ORG-ID'] = as_outer_admin['org_id']
            auth_header['X-UID'] = as_outer_admin['id']

    elif with_uid:
        auth_header['X-UID'] = as_uid or AUTH_UID

    if as_cloud_uid:
        auth_header['X-CLOUD-UID'] = as_cloud_uid

    return auth_header


def get_oauth_headers(as_uid=None, as_org=None, as_outer_admin=None, as_anonymous=False, oauth_token=None):
    """
    Возвращает заголовок для тестов. Вызывая функцию без аргументов, считаем, что
    тестируется API с заголовком, содержащим только uid внутреннего админа организации.
    :param as_uid: можем передать uid, отличный от внутреннего админа
    :param as_org: можем передать только org_id организации (для внутренних сервисов)
    :param as_outer_admin: dict: org_id - ID организации, id - uid внешнего админа
    :param as_anonymous: boolean, если True, то имитуруем вызов API без uid-a & org_id.
    :return: dict, заголовок.
    """

    auth_header = {
        'Authorization':  'OAuth %s' % (oauth_token or OAUTH_TOKEN),
        # IP бэкенда сервиса, с которого идёт обращение к API
        'X-Real-IP': get_localhost_ip_address(),
    }

    if as_anonymous:
        return auth_header

    if as_org is not None:
        if isinstance(as_org, dict):
            as_org = as_org['id']
        auth_header['X-ORG-ID'] = as_org

    elif as_outer_admin is not None:
        if isinstance(as_outer_admin, dict):
            auth_header['X-ORG-ID'] = as_outer_admin['org_id']

    return auth_header


def create_service(token):
    service = deepcopy(app.config['INTERNAL_SERVICES_BY_IDENTITY']['autotest'])
    service['identity'] = token
    service['token'] = token
    app.config['INTERNAL_SERVICES_BY_IDENTITY'][service['identity']] = service
    app.config['INTERNAL_SERVICES_BY_TOKEN'][service['token']] = service
    return service


def set_auth_uid(uid):
    global AUTH_UID
    AUTH_UID = uid


def get_auth_uid():
    """Эта функция нужна потому, что если в тестовом модуле
    проимпортировать просто
    from testutils import AUTH_UID
    то изменения переменной через set_auth_uid
    в том модуле никак не отразятся, так как это будут две разные
    переменные.

    А так, функция будет возвращать одну и ту же глобальную переменную.
    """
    return AUTH_UID


def create_organization(
        meta_connection,
        main_connection,
        name={'ru': 'Яндекс'},
        label='not_yandex_test',
        domain_part='.ws.autotest.yandex.ru',
        admin_uid=None,
        source='autotest',
        language='ru',
        tld='ru',
        root_dep_label=None,
        ready=True,
        cloud_org_id=None,
        ** kwargs,
    ):

    if not admin_uid:
        admin_uid = random.randint(113*10**13, 114*10**13)

    admin_nickname = 'admin'
    admin_gender = 'male'
    admin_birthday = utcnow().date()

    info = original_create_organization(
        meta_connection,
        main_connection,
        name=name,
        domain_part=domain_part,
        label=label,
        language=language,
        admin_uid=admin_uid,
        admin_nickname=admin_nickname,
        admin_first_name='Admin',
        admin_last_name='Adminovich',
        admin_gender=admin_gender,
        admin_birthday=admin_birthday,
        source=source,
        tld=tld,
        root_dep_label=root_dep_label,
        ready=ready,
        cloud_org_id=cloud_org_id,
        **kwargs
    )

    # чтобы диффы событий были с правильными счетчиками
    # надо обновить счетчик на корневом отделе
    DepartmentModel(main_connection).update_members_count(
        1,
        info['organization']['id'],
    )
    return info


def create_organization_without_domain(
        meta_connection,
        main_connection,
        admin_uid=None,
        source='autotest',
        language='ru',
        tld='ru',
        preset='without-domain',
):
    if not admin_uid:
        admin_uid = create_outer_uid()
    meta_model = OrganizationMetaModel(meta_connection)
    org_id = meta_model.get_id_for_new_organization()
    shard = main_connection.engine.db_info['shard']
    org_name = '#{}'.format(org_id)
    label = 'org-{}'.format(org_id)

    # создаем организацию в мета базе
    organization_meta_instance = meta_model.create(
        id=org_id,
        label=label,
        shard=shard,
    )

    # создаем организацию в базе
    organization = OrganizationModel(main_connection).create(
        id=org_id,
        name=org_name,
        label=label,
        admin_uid=admin_uid,
        language=language,
        source=source,
        tld=tld,
        country=language,
        maillist_type='inbox',
        preset=preset,
        organization_type='common',
    )
    # событие создания организации
    action_organization_add(
        main_connection,
        org_id=org_id,
        author_id=admin_uid,
        object_value=organization,
    )
    root_department = create_root_department(
        main_connection,
        org_id,
        language,
        root_dep_label=None,
    )
    action_department_add(
        main_connection,
        org_id=org_id,
        author_id=None,
        object_value=root_department,
    )

    group_model = GroupModel(main_connection)
    group_model.get_or_create_robot_group(org_id)

    # Добавим в организацию первого сотрудника
    user = create_yandex_user(
        meta_connection,
        main_connection,
        admin_uid,
        org_id,
        email='yandex_admin_test@yandex.ru',
        nickname='yandex_admin_test',
    )
    # Сделаем его админом
    UserModel(main_connection).change_user_role(
        org_id,
        admin_uid,
        UserRoles.admin,
        admin_uid=admin_uid,
        old_user=user,
    )
    # Включим фичу, чтобы были нужные права
    set_feature_value_for_organization(
        meta_connection,
        org_id,
        CAN_WORK_WITHOUT_OWNED_DOMAIN,
        True,
    )
    return {
        'domain': None,
        'organization': organization,
        'organization_meta_instance': organization_meta_instance,
        'root_department': root_department,
        'admin_user': user,
        'admin_user_uid': admin_uid,
    }


def create_yandex_user(
        meta_connection,
        main_connection,
        uid,
        org_id,
        email='yandex_user_test@yandex.ru',
        nickname='yandex_user_test',
        name={'first': 'Test', 'last': 'Test'},
        gender='male',

):
    # Такие яндексовые учетки (не перенесенные как внешние админы из пдд) считаются внутренними
    UserMetaModel(meta_connection) \
        .create(
        id=uid,
        org_id=org_id,
        is_outer=False,
    )
    user = UserModel(main_connection).create(
        id=uid,
        nickname=nickname,
        name=name,
        email=email,
        gender=gender,
        org_id=org_id,
    )
    return user


def create_user(meta_connection,
                main_connection,
                user_id,
                nickname,
                name,
                email,
                org_id,
                department_id=None,
                gender='male',
                groups=[],
                aliases=None):
    user_id = int(user_id)
    UserMetaModel(meta_connection).create(
        id=user_id,
        org_id=org_id,
    )
    user = UserModel(main_connection).create(
        id=user_id,
        nickname=nickname,
        name=name,
        email=email,
        gender=gender,
        department_id=department_id,
        org_id=org_id,
        groups=groups,
        aliases=aliases,
    )
    return user


def create_department(connection,
                      org_id,
                      dep_id=None,
                      name={'ru': 'Департамент'},
                      parent_id=None,
                      members_count=0,
                      label=None,
                      uid=None,
                      head_id=None,
                      aliases=None):
    department = DepartmentModel(connection).create(
        org_id=org_id,
        id=dep_id,
        name=name,
        parent_id=parent_id,
        label=label,
        aliases=aliases,
        head_id=head_id,
        uid=uid,
    )
    if members_count > 0:
        DepartmentModel(connection).update(
            {'members_count': members_count},
            filter_data={
                'id': department['id'],
                'org_id': org_id,
            }
        )
        department['members_count'] = members_count
    return department


def create_group(
        connection,
        org_id,
        name={'ru': 'Группа'},
        type='generic',
        label=None,
        admins=None,
        aliases=None,
        uid=None):

    if type == 'organization_admin':
        return GroupModel(connection).get_or_create_admin_group(
            org_id=org_id
        )

    return GroupModel(connection).create(
        org_id=org_id,
        name=name,
        type=type,
        label=label,
        admins=admins or [],
        aliases=aliases,
        generate_action=False,
        uid=uid,
    )


def set_sso_in_organization(main_connection, org_id, sso_status, provisioning_status):
    OrganizationSsoSettingsModel(main_connection).insert_or_update(
        org_id,
        sso_status,
        provisioning_status
    )


class PaginationTestsMixin(object):
    entity_counter = 0
    # entity_list_url = '/departments/'
    # entity_model = DepartmentModel
    entity_list_request_headers = None
    entity_model_filters = {}

    def setUp(self, *args, **kwargs):
        self.per_page = app.config['PAGINATION']['per_page']
        super(PaginationTestsMixin, self).setUp(*args, **kwargs)


    def get_entity_model_filters(self):
        filters = dict(self.entity_model_filters)
        if 'org_id' not in filters:
            filters.update({
                'org_id': self.organization['id']
            })
        return filters

    def get_entity_current_count_in_db(self):
        """
        Мы не можем просто сносить все сущности в базе, т.к. нам нужен как минимум пользователь,
        чтобы делать авторизированные запросы в ручки
        """
        return self.entity_model(self.main_connection).count(self.get_entity_model_filters())

    def create_entity(self):
        raise NotImplementedError()

    # def prepare_entity_for_api_response(self, entity):
    #     raise NotImplementedError()

    def get_response_data(self, response, expected_status=None):
        if expected_status:
            self.assertEqual(response.status_code, 200)
        return json.loads(response.data)

    def get_enitity_list_url(self, query_params):
        return url_join(
            app.config['SITE_BASE_URI'],
            self.entity_list_url,
            force_trailing_slash=True,
            query_params=query_params
        )

    def check_links(self, links, links_header_value, expected):
        self.assertEqual(links, expected)
        if expected != {}:
            self.assertEqual(
                links,
                dict([
                    (i['rel'], i['url']) for i in parse_header_links(links_header_value)
                ])
            )

    def test_pagination__no_entities(self):
        headers = self.entity_list_request_headers or get_auth_headers()
        with patch.object(self.entity_model, 'find', Mock(return_value=[])):
            with patch.object(self.entity_model, 'count', Mock(return_value=0)):
                response = self.client.get(self.entity_list_url, headers=headers)
                response_data = self.get_response_data(response, 200)
                self.check_links(response_data.get('links'), response.headers.get('Link'), {})
                self.assertEqual(response_data.get('page'), 1)
                self.assertEqual(response_data.get('pages'), 0)
                self.assertEqual(response_data.get('per_page'), self.per_page)
                self.assertEqual(response_data.get('total'), 0)
                self.assertEqual(response_data.get('result'), [])

    def test_pagination__all_on_one_page(self):
        for i in range(self.per_page - self.get_entity_current_count_in_db()):
            self.create_entity()
        headers = self.entity_list_request_headers or get_auth_headers()
        response = self.client.get(self.entity_list_url, headers=headers)
        response_data = self.get_response_data(response, 200)
        self.check_links(response_data.get('links'), response.headers.get('Link'), {})

        assert_that(
            response_data,
            has_entries(
                page=1,
                pages=1,
                per_page=self.per_page,
                total=self.per_page,
                result=has_length(self.per_page)
            )
        )

    def test_pagination__more_than_one_page(self):
        all_count = self.per_page * 3

        current_count = self.get_entity_current_count_in_db()
        for i in range(all_count - current_count):
            self.create_entity()
        headers = self.entity_list_request_headers or get_auth_headers()
        response = self.client.get(self.entity_list_url, headers=headers)
        response_data = self.get_response_data(response, 200)
#        response_data = self.get_json(self.entity_list_url)
        self.check_links(
            response_data.get('links'),
            response.headers.get('Link'),
            {
                'next': self.get_enitity_list_url({'page': 2}),
                'last': self.get_enitity_list_url({'page': 3}),
            }
        )
        self.assertEqual(response_data.get('page'), 1)
        self.assertEqual(response_data.get('pages'), 3)
        self.assertEqual(response_data.get('per_page'), self.per_page)
        self.assertEqual(response_data.get('total'), all_count)
        self.assertEqual(len(response_data.get('result')), self.per_page)

    def test_pagination__with_custom_per_page(self):
        per_page = 2
        all_count = 10
        current_count = self.get_entity_current_count_in_db()
        for i in range(all_count - current_count):
            self.create_entity()
        headers = self.entity_list_request_headers or get_auth_headers()

        url = self.entity_list_url
        if 'per_page' not in url:
            url += '&' if '?' in url else '?'
            url += 'per_page={0}'.format(per_page)

        # Зададим размер страницы
        response = self.client.get(url, headers=headers)
        response_data = self.get_response_data(response, 200)
        assert_that(
            response_data,
            has_entries(
                page=1,
                pages=5,
                per_page=2,
                total=10,
            )
        )


def check_response(response, status_code):
    """Проверяет, что пришел ответ с правильным кодом.
    Если код отличается, то бросает AssertionError с описанием проблемы.
    И в зависимости от проблемы, описание может включать например
    ту схему данных, которую ожидает ручка на входе.
    """
    if response.status_code != status_code:

        message = 'Response has status code {0} instead of {1}'.format(
            response.status_code, status_code)

        if response.content_type.startswith('application/json'):
            message += '. Data: ' + response.data
        raise AssertionError(message)


api_errors_pre = """============
 Ошибки API
============

"""

api_error_item = """{code}
{underline}

Сообщение:
    ``{message}``

"""

api_error_item_with_params = api_error_item + """
Параметры:
    ``{params}``

"""

def write_api_errors_to(filename):
    """Эта функция вызывается из runtests
    и автоматически генерирует rst файл
    со списком ошибок, которые может возвращать
    API.
    """
    # атрибут появится в модуле только если во время импорта переменная
    # окружения ENVIRONMENT=autotests
    from intranet.yandex_directory.src.yandex_directory.common.utils import all_json_errors

    already_written = set()

    with open(filename, 'w') as f:
        f.write(api_errors_pre)

        all_json_errors.sort(
            key=lambda item: item[0][1]
        )
        for item in all_json_errors:
            error_code = item[0][1]

            if error_code in already_written:
                continue
            already_written.add(error_code)

            error_message = item[0][2]
            error_params = item[1]
            item_template = api_error_item

            if error_params:
                error_params = json.dumps(
                    error_params,
                    sort_keys=True,
                )
                item_template = api_error_item_with_params

            f.write(
                item_template.format(
                    code=error_code,
                    underline='=' * len(error_code),
                    message=error_message,
                    params=error_params,
                )
            )

# В эту переменную окружения сохранятся скоупы, с которыми надо
# делать запросы в рамках кода, обёрнутого в контекстный
# менеджер `with scopes(...)`.
global_scopes = None


@contextmanager
def scopes(*args):
    global global_scopes

    # Сохраним старое и установим новое значение.
    previous = global_scopes
    global_scopes = args

    yield

    # Вернём старое
    global_scopes = previous


@contextmanager
def patched_scopes():
    def fake_get_service(token):
        # Тут мы подменяем список скоупов для сервиса, пришедшего с токеном,
        # если был использован контекстный менеджер 'with scopes'.
        result = app.config['INTERNAL_SERVICES_BY_TOKEN'].get(token)
        if result and global_scopes != None:
            # Тут важно скопировать данные, чтобы не повлиять
            # на тесты, которые будут выполняться после
            result = result.copy()
            result['scopes'] = global_scopes

        return result

    with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.get_internal_service_by_token') as mock:
        mock.side_effect = fake_get_service
        yield


@contextmanager
def patched_admin_permissions(admin_permission=all_internal_admin_permissions):
    with patch('intranet.yandex_directory.src.yandex_directory.auth.user.get_admin_permissions') as get_admin_permission:
        get_admin_permission.return_value = admin_permission
        yield


@contextmanager
def frozen_time(delta_or_timestamp=None):
    if isinstance(delta_or_timestamp, datetime.datetime):
        frozen_moment = delta_or_timestamp
    else:
        frozen_moment = utcnow()
        if isinstance(delta_or_timestamp, datetime.timedelta):
            frozen_moment += delta_or_timestamp

    with patch('intranet.yandex_directory.src.yandex_directory.common.utils._standard_now') as mocked_now:
        mocked_now.return_value = frozen_moment
        yield


class AssertMixin:
    def assertEqual(self, a, b, *args, **kwargs):
        assert a == b

    def assertNotEqual(self, a, b, *args, **kwargs):
        assert a != b

    def assertTrue(self, value, *args, **kwargs):
        assert bool(value)

    def assertFalse(self, value, *args, **kwargs):
        assert not bool(value)

    def assertIsNone(self, value, *args, **kwargs):
        assert value is None

    def assertIsNotNone(self, value, *args, **kwargs):
        assert value is not None

    def assertDictEqual(self, d1, d2, *args, **kwargs):
        assert d1 == d2

    def assertIn(self, v, l, *args, **kwargs):
        assert v in l

    def assertNotIn(self, v, l, *args, **kwargs):
        assert v not in l

    @contextlib.contextmanager
    def assertRaises(self, error):
        with pytest.raises(error):
            yield


class SimpleTestCase(AssertMixin):
    @classmethod
    def setup_class(cls):
        if hasattr(cls, 'setUpClass'):
            cls.setUpClass()

    def init(self, *args, **kwargs):
        pass

    def setUp(self):
        pass

    def tearDown(self):
        pass

    @contextlib.contextmanager
    def run(self, *args, **kwargs):
        yield

    @pytest.fixture(scope="function", autouse=True)
    def pytest_run_adapter(self):
        with self.run():
            self.init()
            self.setUp()
            yield
            self.tearDown()

class DictObject:
    def __init__(self, data):
        for key, value in data.items():
            if isinstance(value, dict):
                data[key] = DictObject(value)
            if isinstance(value, list):
                data[key] = [
                    DictObject(item) if isinstance(item, dict) else item
                    for item in value
                ]
        self.__dict__.update(**data)

    def make_dict(self):
        result = {}
        for key, value in self.__dict__.items():
            if isinstance(value, DictObject):
                result[key] = value.make_dict()
            elif isinstance(value, list):
                result[key] = [
                    item.make_dict() if isinstance(item, DictObject) else item
                    for item in value
                ]
            else:
                result[key] = value
        return result

    def ListFields(self):
        return [
            (DictObject({'name': key}), value)
            for key, value in self.__dict__.items()
        ]


class MockToDict:
    def __init__(self, grpc_response):
        self.grpc_response = grpc_response.make_dict()

    def __getitem__(self, item):
        return self.grpc_response[item]

    def get(self, item, default=None):
        return self.grpc_response.get(item, default)


class TestCase(SimpleTestCase):
    create_organization = True
    label = 'not_yandex_test'
    # По умолчанию не заводим рассылку для рутового отдела.
    # Я попробовал так сделать, но это привело к тому, что
    # больше 200 тестов сломались.
    # TODO: по хорошему, надо бы со всеми ними разобраться.
    root_dep_label = None
    api_version = None
    enable_admin_api = False
    # По умолчанию, организация заводится на русском языке.
    language = 'ru'
    # TLD по умолчанию для организации
    tld = 'ru'
    maillist_management = False
    mock_passport_path = 'intranet.yandex_directory.src.yandex_directory.app.requests'
    domain_part = None
    default_karma = 0
    # Выполнять ли обработку асинхронных тасков после
    # каждого вызова ручки с помощью методов get_json, post_json, etc.
    autoprocess_tasks = True

    def init(self, *args, **kwargs):
        self.next_org_label_idx = 0
        self.environment = app.config['ENVIRONMENT']
        # список патчеров, которые позволяют замокать разные
        # методы классов
        # перед каждым тестом, для каждого их патчеров будет
        # вызываться __enter__, а после – __exit__ метод.

        self.patchers = [
            # замокаем все запросы в паспорт
            (mocked_blackbox(), 'mocked_blackbox'),
            (mocked_passport(), 'mocked_passport'),
            (mocked_team_passport(), 'mocked_team_passport'),
            (mocked_domenator(), 'mocked_domenator'),

            # За кармой в blackbox тоже ходить не надо
            (patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.get_karma'), 'mocked_get_karma'),

            # В YT нам ходить не надо
            (patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client'), 'mocked_yt_client'),
            (patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.billing_yt_clients'), 'mocked_billing_yt_clients'),

            # tvm тоже надо замокать
            (patch.object(app, 'tvm2_client'), 'mocked_tvm2_client'),

            # клиент зора
            (mocked_zora_client(), 'mocked_zora_client'),

            (patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_info'), 'mocked_webmaster_inner_info'),
            (patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_verify'), 'mocked_webmaster_inner_verify'),
            (patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_add'), 'mocked_webmaster_inner_add'),
            (patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_list_applicable'), 'mocked_webmaster_inner_list_applicable'),
            (patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_lock_dns_delegation'), 'mocked_webmaster_inner_lock_dns_delegation'),

            # мокаем методы сервиса рассылок bigml
            (patch('intranet.yandex_directory.src.yandex_directory.bigml._get'), 'mocked_bigml_get'),
            (patch('intranet.yandex_directory.src.yandex_directory.bigml._post'), 'mocked_bigml_post'),

            # мокаем методы сервиса dkim
            (patch('intranet.yandex_directory.src.yandex_directory.fouras._get'), 'mocked_fouras_get'),
            (patch('intranet.yandex_directory.src.yandex_directory.fouras._post'), 'mocked_fouras_post'),

            # мокаем методы сервиса конфигов SSO
            (patch('intranet.yandex_directory.src.yandex_directory.sso.config_service.client._get'), 'mocked_sso_config_service_get'),
            (patch('intranet.yandex_directory.src.yandex_directory.sso.config_service.client._post'), 'mocked_sso_config_service_post'),
            (patch('intranet.yandex_directory.src.yandex_directory.sso.config_service.client._put'), 'mocked_sso_config_service_put'),
            (patch('intranet.yandex_directory.src.yandex_directory.sso.config_service.client._delete'), 'mocked_sso_config_service_delete'),

            # Эта функция мокается, чтобы не создавался отдельный поток, пытающийся
            # доставить письмо. Из-за него тоже случаются ошибки "Shard not found".
            patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'),

            # По умолчанию считаем, что у сервисов нет зависимостей и только
            # в тестах, которые проверяют обработку зависимостей, явно их задаём.
            patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new={}),
            # В тестах мы мокаем ensure_thread_is_running чтобы
            # не запускался реальный поток, который проверяет живость сервисов
            patch('intranet.yandex_directory.src.yandex_directory.common.smoke.ensure_thread_is_running'),

            (patch('intranet.yandex_directory.src.yandex_directory.core.resource_history.domain.create_meta_session'), 'create_meta_session'),
            (patch('intranet.yandex_directory.src.yandex_directory.core.resource_history.resource.create_meta_session'), 'create_meta_session_resource'),

            (mocked_idm_b2b(), 'mocked_idm_b2b')
        ]
        self.patchers_started = False

    def get_dict_object(self, data):
        return DictObject(data)

    def start_patchers(self):
        if not self.patchers_started:
            for patcher in self.patchers:
                if isinstance(patcher, tuple):
                    attr_name = patcher[1]
                    patcher = patcher[0]
                    setattr(self, attr_name, patcher.start())
                else:
                    patcher.start()
            self.patchers_started = True

            # Дополнительные настройки для запатченных объектов
            def return_next_pdd_uid(*args, **kwargs):
                return self.get_next_uid()

            def mocked_get_user_data_from_blackbox_by_uids_func(uids, **kwargs):
                return [fake_userinfo(uid) for uid in uids]

            self.mocked_get_karma = self.default_karma

            self.mocked_passport.maillist_add.side_effect = return_next_pdd_uid
            self.mocked_passport.account_add.side_effect = lambda *args, **kwargs: self.get_next_uid()
            self.mocked_passport.block_user.return_value = make_passport_response()
            self.mocked_passport.unblock_user.return_value = make_passport_response()
            self.mocked_blackbox.batch_userinfo.side_effect = mocked_get_user_data_from_blackbox_by_uids_func

            self.meta_session = Session(self.meta_connection)
            self.create_meta_session.return_value = self.meta_session
            self.create_meta_session_resource.return_value = self.meta_session

            self.mocked_domenator.private_get_domains.return_value = []

    def stop_patchers(self):
        if self.patchers_started:
            for patcher in self.patchers:
                if isinstance(patcher, tuple):
                    attr_name = patcher[1]
                    patcher = patcher[0]
                    delattr(self, attr_name)

                patcher.stop()
            self.patchers_started = False

    def tearDown(self):
        # Очистим очередь задач, если в ней вдруг почему-то остались какие-то задания
        # Это нужно, чтобы избавиться от ошибок типа "Shard not found", которые происходят
        # из-за того, что организация созданная для предыдущего текста, была откачена
        # через ROLLBACK, но задачи её упоминающие, остались в пайплайне.

        # Почистим содержимое кэшей
        caches = [
            ('intranet.yandex_directory.src.yandex_directory.core.events.utils', 'webhooks_cache'),
            # Временно отключено из-за мигания пермишшенов после смены платности
            # https://st.yandex-team.ru/DIR-3715
            # ('intranet.yandex_directory.src.yandex_directory.core.permissions', 'permissions_cache'),
            ('intranet.yandex_directory.src.yandex_directory.core.utils', 'organization_domains_cache'),
            ('intranet.yandex_directory.src.yandex_directory.core.utils.services', 'services_cache'),
        ]
        for module, name in caches:
            module = __import__(module, globals(), locals(), [name])
            cache = getattr(module, name)
            if cache is not None:
                cache._Cache__data.clear()

        self.stop_patchers()

    @classmethod
    def setUpClass(cls):
        setup_app(app)

    def setUp(self):
        self.start_patchers()
        # В конструкторе нельзя это делать, так как на момент его запуска
        # приложение ещё не проинициализировано
        self.domain_part = self.domain_part or app.config["DOMAIN_PART"]


        if self.enable_admin_api:
            with override_settings(ENABLE_ADMIN_API=True):
                try:
                    app.enable_admin_api_if_needed()
                except AssertionError:  # вьюхи уже могли быть добавлены
                    pass

        backpressure.reset_errors_count()  # сбросим число ошибок smoke-тестов

        with app.app_context():
            build_cached_view_methods_map()

        self.uid_sequence = count(1)
        self.user_counter = 0
        self.department_counter = 0
        self.group_counter = 0
        self.department_name = {
            'ru': 'Департамент',
            'en': 'Department'
        }
        self.group_name = {
            'ru': 'Группа',
            'en': 'Group'
        }
        self.name = {
            'first': {
                'ru': 'Пользователь',
                'en': ''
            },
            'last': {
                'ru': 'Автотестовый',
                'en': ''
            },
        }

        self.shard = 1

        # Создадим пресет по-умолчанию, который не будет включать никакие сервисы
        PresetModel(self.meta_connection).create('default', [], {})

        # # берем коннекты
        # self.meta_connection_ctx = get_meta_connection(for_write=True)
        # self.main_connection_ctx = get_main_connection(self.shard, for_write=True)

        # # и эмулируем вход в context manager
        # # позже надо будет обязательно сделать __exit__ внутри tearDown
        # self.meta_connection = self.meta_connection_ctx.__enter__
        # self.main_connection = self.main_connection_ctx.__enter__

        if self.create_organization:
            self.organization_info = create_organization(
                self.meta_connection,
                self.main_connection,
                label=self.label,
                domain_part=self.domain_part,
                language=self.language,
                root_dep_label=self.root_dep_label,
                tld=self.tld,
            )
            self.admin_uid = self.organization_info['admin_user']['id']
            self.organization = self.organization_info['organization']
            self.organization_domain = self.organization_info['domain']['name']
            self.department = self.organization_info['root_department']
            # self.user = self.organization_info['admin_user']
            self.outer_admin = UserMetaModel(self.meta_connection).create(
                id=PASSPORT_TEST_OUTER_UID,
                org_id=self.organization['id'],
            )
            self.user = UserModel(self.main_connection).get(
                user_id=self.admin_uid,
                org_id=self.organization['id'],
                fields=ALL_FIELDS,
            )
            set_auth_uid(self.user['id'])

            self.service = ServiceModel(self.meta_connection).create(
                slug='service-slug',
                name='Name',
                robot_required=True,
                client_id=OAUTH_CLIENT_ID,
            )
            BIND_ACCESS_SKIP_CHECK.add(self.service['slug'])
            ServiceModel(self.meta_connection).create(
                slug=MAILLIST_SERVICE_SLUG,
                name='Mail List',
                robot_required=False,
                client_id='mail_list_client_id',
            )
            UpdateServicesInShards().try_run()

            if self.maillist_management:
                enable_service(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                    MAILLIST_SERVICE_SLUG,
                )

            # обновим ревизию ведь у нас были события по созданию технических команд
            self.organization['revision'] = self.get_org_revision(self.organization['id'])
            self.set_feature_value_for_organization(DOMAIN_AUTO_HANDOVER, True)
            self.features_list = get_organization_features_info(self.meta_connection, self.organization['id'])

            # партнер для парнерской схемы
            self.partner = PartnersMetaModel(self.meta_connection).create(name='Test partner')

            if self.autoprocess_tasks:
                self.process_tasks()
        # в тестах часто проверяется что появились новые события или действия
        # почистим события и действия возникшие при создании тестовой организации
        self.clean_actions_events_and_tasks()

    def add_user_as_outer_admin(self, org, uid):
        return UserMetaModel(self.meta_connection).create(
            id=uid,
            org_id=org['id'],
            is_outer=True,
        )

    def add_user_by_invite(self, org, uid, department_id=1, domain=None):
        """Хелпер, чтобы симулировать пользователя, добавленного по инвайту.
        """
        from intranet.yandex_directory.src.yandex_directory.core.utils import add_existing_user

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_info_from_blackbox') as userinfo:
            if domain:
                login = '{0}@{1}'.format(uid, domain)
            else:
                login = 'user_{0}'.format(uid)
            first = login
            last = login
            gender = 0
            birthday = None
            cloud_uid = None
            userinfo.return_value = (login, first, last, gender, birthday, 'default@domain.ru', cloud_uid)

            user = add_existing_user(
                self.meta_connection,
                self.main_connection,
                org['id'],
                uid,
                department_id
            )
            return user

        if self.autoprocess_tasks:
            self.process_tasks()

    def process_tasks(self):
        """Этот метод выполняет все асинхронные таски.
           Он автоматически запускается после кадого get_json, post_json, etc.
           и может быть вызван врую в тестах, где таски создаются самим тестом.
        """
        tasks = TaskModel(self.main_connection).all()
        for task in tasks:
            self.process_task(task['id'])

    def process_task(self, task_id):
        SyncResult(self.main_connection, task_id)

    def assert_no_failed_tasks(self, allowed_states=['success']):
        """Этот вспомогательный метод полезен тогда, когда надо удостовериться, что
           фоновые таски выполнились без ошибок.

           В allowed_states можно указать как список строк, так и туплы,
           типа ('UpdateMembersCountTask', 'free'). Такой tuple
           разрешает данное состояние лишь для указанного типа тасков.

           Если его не вызывать, то даже в случае, когда внутри таска произошла ошибка,
           вызов метода .delay проходит без проблем, и тест может не падать.
        """
        def ensure_tuple(state):
            if isinstance(state, (list, tuple)):
                return state
            else:
                return ('', state)

        def task_is_ok(task):
            ok = False
            for suffix, state in allowed_states:
                if task['task_name'].endswith(suffix) and \
                   task['state'] == state:
                    ok = True
                    break
            return ok

        allowed_states = list(map(ensure_tuple, allowed_states))
        tasks = TaskModel(self.main_connection).all()
        for task in tasks:
            if not task_is_ok(task):
                raise AssertionError(
                    'Task {task_name} failed\n{traceback}'.format(**task))

    def assert_task_created(self, task_name):
        """Этот вспомогательный метод полезен тогда, когда надо удостовериться, что
           была создана определёная фоновая таска.
        """
        tasks = TaskModel(self.main_connection).all()
        created = False
        for task in tasks:
            if task['task_name'] == task_name \
               or task['task_name'].endswith('.' + task_name):
                created = True
                break
        if not created:
            raise AssertionError(
                'Task {0} was not created'.format(task_name))

    def dismiss(self, org, user_id, admin_id=NotGiven):
        if isinstance(org, dict):
            org_id = org['id']
        else:
            org_id = org

        if admin_id is NotGiven:
            admin_id = self.admin_uid

        UserModel(self.main_connection).dismiss(
            org_id=org_id,
            user_id=user_id,
            author_id=None,
        )
        if self.autoprocess_tasks:
            self.process_tasks()

    def create_organization(self, *args, **kwargs):
        """Небольшой хелпер, чтобы не так много писать, когда надо организацию завести.
        """
        if 'label' not in kwargs:
            kwargs = kwargs.copy()
            kwargs['label'] = self.get_next_org_label()

        return create_organization(
            self.meta_connection,
            self.main_connection,
            *args,
            **kwargs
        )['organization']

    def get_next_org_label(self):
        self.next_org_label_idx += 1
        return 'org{0}'.format(self.next_org_label_idx)

    def get_next_uid(self, outer_admin=False):
        base_non_admin = 111e13 + 101  # TODO: магическое 101, нужен какой-то оффсет, чтобы не конфликтовать с хардкодными uid
        uid = int(next(self.uid_sequence))
        return uid if outer_admin else uid + int(base_non_admin)

    @contextlib.contextmanager
    def run(self, *args, **kwargs):
        """
        Этот метод мы переопределяем, чтобы создавать и откатывать транзакцию
        при запуске каждого отдельного теста.
        """
        # пока будем считать, что у нас всё на одном шарде
        self.shard = 1

        with app.app_context():
#             app.test_request_context():

            # берем коннекты из пула
            with get_meta_connection(for_write=True) as self.meta_connection:
                with get_main_connection(self.shard, for_write=True) as self.main_connection:

                    # Это нужно для того, чтобы код, обрабатывающий запрос, видел
                    # те же данные, что были созданы во время теста.
                    # Другими словами, код теста и код директории, должны использовать
                    # одну и ту же транзакцию.
                    set_permanent_meta_connection(self.meta_connection)
                    set_permanent_main_connection(self.main_connection)

                    self.client = app.test_client()

                    # стартуем по транзакции
                    # тут мы это делаем на с помощью контекстных менеджеров, а вручную,
                    # потому что контекстный менеджер делает rollback только в случае
                    # исключения, а нам нужно откатывать результаты тесты всегда
                    meta_transaction = self.meta_connection.begin_nested()
                    main_transaction = self.main_connection.begin_nested()

                    # Чтобы ошибки с таймзонами были более явными и "вылезали"
                    # днём, а не после полуночи
                    self.meta_connection.execute("SET TIMEZONE TO 'Asia/Anadyr'")
                    self.main_connection.execute("SET TIMEZONE TO 'Asia/Anadyr'")

                    try:
                        # Сначала запустим тест.
                        # с пропатченным клиентом Blackbox.
                        # Мы ведь не хотим ходить в настоящий блэкбокс из юнит-тестов?
                        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.BlackboxWithTimings._blackbox_call') as _blackbox_call:
                            def fake_blackbox_call(handle, *args, **kwargs):
                                if handle == 'userinfo':
                                    return """<?xml version="1.0" encoding="UTF-8"?>
                                    <doc>
                                      <uid hosted="0"></uid>
                                      <karma confirmed="0">0</karma>
                                      <karma_status>0</karma_status>
                                    </doc>"""

                            _blackbox_call.side_effect = fake_blackbox_call
                            yield
                    finally:
                        # а потом откатим изменения в базах
                        # чтобы новый тест запускать с чистого листа
                        if meta_transaction.is_active:
                            meta_transaction.rollback()

                        if main_transaction.is_active:
                            main_transaction.rollback()

                        set_permanent_meta_connection(None)
                        set_permanent_main_connection(None)

    def get_json(self,
                 uri,
                 headers=None,
                 add_headers=None, # Используй этот параметр, чтобы добавить а не переопределить заголовки
                 expected_code=200,
                 as_uid=None,
                 query_string=None,
                 return_headers=False,
                 raw=False, # Если True, то контент не будет парситься через json.loads
                 pdb=False,
                 check_revision=True, # Проверяет ревизию в заголовках ответа
                 process_tasks=NotGiven, # Выполнять ли асинхронные таски после вызова ручки,
                 as_org=None
                 ):
        """Получить данные по указанному урлу, и конвертировать их из JSON в питон.

        Так же в запросе будут переданы указанные заголовки.
        Код, который вернет сервер, должен соответствовать expected_code,
        в противном случае возникает AssertionError.
        По умолчанию, запрос делается от имени администратора, но можно
        в параметре as_uid указать uid пользователя, от чьего имени надо делать
        запрос.

        """
        if headers is None:
            headers = get_auth_headers(as_uid=as_uid, as_org=as_org)

        if add_headers:
            # В объекте Headers нет метода update
            for key, value in list(add_headers.items()):
                headers[key] = value

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes(), patched_admin_permissions():
            response = self.client.get(
                    uri,
                    headers=headers,
                    query_string=query_string
                )
        if pdb:
            pprint.pprint(json.loads(response.data))
            import pdb; pdb.set_trace()  # DEBUG

        self.assertEqual(expected_code, response.status_code)

        if check_revision:
            if getattr(g, 'user', None) and getattr(g, 'org_id', None):
                headers = response.headers
                # у ответа должен быть заголовок с ревизией
                revision = headers.get('X-Revision')
                assert_that(revision, not_none())
                assert_that(int(revision),
                            greater_than_or_equal_to(0))

        if raw:
            data = response.data
        else:
            data = json.loads(response.data)

        if return_headers:
            return data, response.headers

        # Если в ручке были созданы таски, их надо выполнить
        if process_tasks is NotGiven:
            process_tasks = self.autoprocess_tasks
        if process_tasks:
            self.process_tasks()
        return data

    def post_json(self,
                  uri,
                  data,
                  headers=None,
                  expected_code=201,
                  pdb=False,
                  expected_message=None,
                  expected_error_code=None,
                  process_tasks=NotGiven,
                  content_type='application/json',
                  json_response=True,
                  ):

        if headers is None:
            headers = get_auth_headers()

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes(), patched_admin_permissions():
            if content_type is None:
                response = self.client.post(
                    uri,
                    headers=headers)
            else:
                response = self.client.post(
                    uri,
                    data=json.dumps(data) if data is not None else None,
                    content_type='application/json',
                    headers=headers)

        if pdb:
            pprint.pprint(json.loads(response.data))
            import pdb; pdb.set_trace()  # DEBUG

        data = response.data
        error = None
        if json_response:
            data = json.loads(response.data)

            # Попробуем вынуть из ответа сообщение об ошибке,
            # чтобы сделать ассерт более информативным
            if isinstance(data, dict):
                params = data.get('params', {})
                errors = params.get('errors', [])
                if errors:
                    error = errors[0]['message']

        assert_that(
            response,
            has_properties(status_code=expected_code),
            error,
        )

        if expected_message is not None:
            assert_that(
                json.loads(response.data),
                has_entries(
                    message=expected_message,
                )
            )

        if expected_error_code is not None:
            assert_that(
                json.loads(response.data),
                has_entries(
                    code=expected_error_code,
                )
            )

        # Если в ручке были созданы таски, их надо выполнить
        if process_tasks is NotGiven:
            process_tasks = self.autoprocess_tasks
        if process_tasks:
            self.process_tasks()
        if json_response:
            return data

        return data, response.headers

    # TODO: непонятный метод для мока, надо разобраться зачем он
    #       на самом деле, он мокает функции внутри либы requests
    @staticmethod
    def get_mocked_passport_api(status='ok', errors=None, **payload):
        raise RuntimeError('Fix get_mocked_passport_api')
        if errors is None:
            errors = []

        # Если errors не пустые, статус всегда 'error'.
        # Если пустые, то status берется из аргумента, что позволяет явно указать status='error' при пустых errors.
        status = 'error' if errors else status
        return_value = {'status': status}

        if errors:
            return_value['errors'] = errors
        else:
            return_value.update(payload)

        mocked_passport_api = Mock()
        result = Mock()
        result.json = Mock(return_value=return_value)
        mocked_passport_api.post = Mock(return_value=result)
        mocked_passport_api.delete = Mock(return_value=result)
        return mocked_passport_api

    def post_form_data(self, uri, data, headers=None, expected_code=201, pdb=False):
        if headers is None:
            headers = get_auth_headers()

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes():
            response = self.client.post(
                uri,
                data=data,
                headers=headers)

        if pdb:
            pprint.pprint(response.data)
            import pdb
            pdb.set_trace()  # DEBUG

        assert_that(response,
                    has_properties(status_code=expected_code))
        return json.loads(response.data)

    def put_json(self, uri, data, headers=None, expected_code=200, as_uid=None, pdb=False, process_tasks=NotGiven, as_org=None):
        if headers is None:
            headers = get_auth_headers(as_uid=as_uid, as_org=as_org)

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes():
            response = self.client.put(
                uri,
                data=json.dumps(data),
                content_type='application/json',
                headers=headers
            )

        if pdb:
            pprint.pprint(json.loads(response.data))
            import pdb; pdb.set_trace()  # DEBUG

        assert_that(
            response,
            has_properties(status_code=expected_code)
        )

        if process_tasks is NotGiven:
            process_tasks = self.autoprocess_tasks
        if process_tasks:
            self.process_tasks()
        return json.loads(response.data)

    def patch_json(self,
                   uri,
                   data,
                   headers=None,
                   expected_code=200,
                   return_headers=False,
                   pdb=False,
                   expected_message=None,
                   expected_error_code=None,
                   process_tasks=NotGiven,
                   ):
        if headers is None:
            headers = get_auth_headers()

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes(), patched_admin_permissions():
            response = self.client.patch(
                uri,
                data=json.dumps(data),
                content_type='application/json',
                headers=headers
            )

        if pdb:
            pprint.pprint(json.loads(response.data))
            import pdb; pdb.set_trace()  # DEBUG

        assert_that(
            response,
            has_properties(status_code=expected_code)
        )
        data = json.loads(response.data)

        if expected_message is not None:
            assert_that(
                data,
                has_entries(
                    message=expected_message,
                )
            )

        if expected_error_code is not None:
            assert_that(
                data,
                has_entries(
                    code=expected_error_code,
                )
            )

        # Если в ручке были созданы таски, их надо выполнить
        if process_tasks is NotGiven:
            process_tasks = self.autoprocess_tasks
        if process_tasks:
            self.process_tasks()

        if return_headers:
            return data, response.headers
        return data

    def delete_json(self,
                    uri,
                    headers=None,
                    as_uid=None,
                    expected_code=204,
                    data={},
                    pdb=False,
                    process_tasks=NotGiven):
        if headers is None:
            headers = get_auth_headers(as_uid=as_uid)

        if self.api_version:
            if uri.startswith('/'):
                uri = uri[1:]
            uri = url_join('/%s/' % self.api_version, uri)

        with patched_scopes(), patched_admin_permissions():
            response = self.client.delete(
                uri,
                data=json.dumps(data),
                content_type='application/json',
                headers=headers)

        if pdb:
            pprint.pprint(json.loads(response.data))
            import pdb; pdb.set_trace()  # DEBUG

        assert_that(response,
                    has_properties(status_code=expected_code))

        # Если в ручке были созданы таски, их надо выполнить
        if process_tasks is NotGiven:
            process_tasks = self.autoprocess_tasks
        if process_tasks:
            self.process_tasks()

        if response.data:
            return json.loads(response.data)
        else:
            return {}

    def disable_paid_mode(self, org_id=None, admin_uid=None):
        """Выключает платный режим в организации"""
        OrganizationModel(self.main_connection).disable_paid_mode(
            author_id=admin_uid or self.admin_uid,
            org_id=org_id or self.organization['id'],
        )

    def enable_paid_mode(self, org_id=None, subscription_plan_changed_at=None, first_debt_act_date=None, balance=None):
        """Включает платный режим в организации"""
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, random.randint(1, 100000))
        mocked_xmlrpc.Balance.CreatePerson.return_value = random.randint(1, 100000)
        mocked_xmlrpc.Balance.CreateOffer.return_value = {
            'EXTERNAL_ID': '%s/17' % random.randint(1, 100000),
            'ID': random.randint(1, 100000),
        }

        if org_id is None:
            org_id = self.organization['id']

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_natural_person(
                org_id=org_id,
                author_id=1,
                first_name='Alexander',
                last_name='Akhmetov',
                middle_name='R',
                phone='+7',
                email='akhmetov@yandex-team.ru',
            )

        # проверим, что платность включилась
        fresh_organization = OrganizationModel(self.main_connection).get(id=org_id)
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        if subscription_plan_changed_at:
            OrganizationModel(self.main_connection).update(
                filter_data={'id': org_id},
                update_data={'subscription_plan_changed_at': subscription_plan_changed_at},
            )

        if first_debt_act_date:
            OrganizationBillingInfoModel(self.main_connection).update(
                filter_data={'org_id': org_id},
                update_data={'first_debt_act_date': first_debt_act_date},
            )

        if balance:
            OrganizationBillingInfoModel(self.main_connection).update(
                filter_data={'org_id': org_id},
                update_data={'balance': balance},
            )

    def create_portal_user(self,
                           org_id=None,
                           user_id=None,
                           is_admin=False,
                           login='art',
                           firstname='Александр',
                           lastname='Артеменко',
                           birthday='1980-10-05',
                           country='ru',
                           email='default@domain.ru',
                           ):
        """ Добавляет в организацию портальную учётку, как сотрудника.
        """
        org_id = org_id or self.organization['id']
        user_id = user_id or create_outer_uid()

        # Патчим в отдельном контексте, чтобы не изменять работу userinfo
        # для теста, который вызовет этот метод.
        with patch.object(self.mocked_blackbox, 'userinfo') as mocked_userinfo:
            mocked_userinfo.return_value = {
                'fields': {
                    'country': country,
                    'login': login,
                    'firstname': firstname,
                    'lastname': lastname,
                    'sex': '1',
                    'birth_date': birthday,
                },
                'uid': user_id,
                'default_email': email,
            }
            ret = create_portal_user(
                self.meta_connection,
                self.main_connection,
                uid=user_id,
                org_id=org_id,
            )

            if is_admin:
                UserModel(self.main_connection).make_admin_of_organization(org_id, user_id)

            return ret


    def create_user(self,
                    department_id=1,
                    nickname=None,
                    groups=[],
                    uid=None,
                    org_id=None,
                    is_outer=False,
                    outer_admin=True,
                    name=None,
                    email=None,
                    gender='male',
                    domain_part=None,
                    created_at=None,
                    external_id=None,
                    is_dismissed=False,
                    is_sso=False,
                    aliases=[],
                    cloud_uid=None,
                    ):

        # По умолчанию, используем в качестве домена данные из
        # атрибутов testsuite
        domain_part = domain_part or self.label + self.domain_part

        self.user_counter += 1
        nickname = nickname or 'test-%s' % str(self.user_counter)
        name = name or self.name
        email = email or 'test-%s@%s' % (self.user_counter, domain_part)

        if not org_id:
            org_id = self.organization['id']

        if not uid:
            if is_outer:
                uid_offset = 0  # uid обычного пользователя
            else:
                uid_offset = 111*10**13  # uid пользователя директории
            uid = uid_offset + self.user_counter

        else:
            uid = int(uid)
            if is_outer and not is_outer_uid(uid):
                raise ValueError('uid "%s" is not outer!' % uid)
        user_data = {'id': uid, 'org_id': org_id, 'is_dismissed': is_dismissed}
        if not outer_admin:
            user_data['is_outer'] = False
        if cloud_uid:
            user_data['cloud_uid'] = cloud_uid
        user = UserMetaModel(self.meta_connection).create(**user_data)
        if not is_outer:
            user = UserModel(self.main_connection).create(
                id=uid,
                nickname=nickname,
                name=name,
                email=email,
                gender=gender,
                org_id=org_id,
                department_id=department_id,
                groups=groups,
                external_id=external_id,
                aliases=aliases,
                cloud_uid=cloud_uid,
                is_dismissed=is_dismissed,
                is_sso=is_sso,
            )

            user = UserModel(self.main_connection).get(
                user_id=user['id'],
                org_id=org_id,
                fields=[
                    '*',
                    'department.*',
                ]
            )
            # для отдела надо позвать метод пересчета мемберов
            # чтобы диффы событий были с правильными счетчиками
            DepartmentModel(self.main_connection).update_members_count(
                department_id,
                org_id,
            )
            # пересчитываем количество пользователей в организации
            OrganizationModel(self.main_connection).update_user_count(org_id)

            if created_at:
                self.main_connection.execute(
                    mogrify(
                        self.main_connection,
                        query='UPDATE users SET created=%(date)s WHERE id=%(id)s',
                        vars={
                            'date': created_at,
                            'id': user['id'],
                        },
                    )
                )
                # Получим данные из базы заново
                user = UserModel(self.main_connection).filter(id=user['id']).one()

        if self.autoprocess_tasks:
            self.process_tasks()
        return user

    def create_department(self, parent_id=1, label=None, org_id=None, head_id=None, **kwargs):
        self.department_counter += 1

        if org_id is None:
            org_id = self.organization['id']
        return DepartmentModel(self.main_connection).create(
            label=label,
            name=self.department_name,
            org_id=org_id,
            parent_id=parent_id,
            head_id=head_id,
            **kwargs
        )

    def create_group(self, org_id=None, **kwargs):
        self.group_counter += 1
        if org_id is None:
            org_id = self.organization['id']

        group_model = GroupModel(self.main_connection)

        if kwargs.get('type', '') == 'organization_admin':
            return group_model.get_or_create_admin_group(org_id)

        author_id = kwargs.pop('author_id', None)
        if not author_id:
            if self.user['org_id'] != org_id:
                raise AssertionError('Please, specify author_id for this group')

            author_id = self.user['id']

        group = group_model.create(
            name={'ru': 'g{0}'.format(self.group_counter)},
            author_id=author_id,
            org_id=org_id,
            generate_action=False,
            **kwargs
        )
        return group

    def refresh(self, obj):
        """Обновляет содержимое объекта, подгружая его из базы и заменяя содержимое словаря.

        Тип объекта и необходимые prefetch поля вычисляются на основе эвристик.

        WARNING: на данные момент поддерживается обновление отделов.
                 добавляйте другие типы по необходимости.
        """
        obj_type = None

        if 'parent_id' in obj:
            obj_type = 'department'

        new_obj = None

        if obj_type == 'department':
            new_obj = DepartmentModel(self.main_connection).get(
                obj['id'],
                obj['org_id'],
                fields=['*','email']
            )

        if obj_type:
            if new_obj:
                obj.clear()
                obj.update(new_obj)
            else:
                raise RuntimeError('Object wasn\'t found in the database')
        else:
            raise RuntimeError('Not supported object type')

    def create_resource_with_department(self, department_id, relation='read'):
        relations = [
            {
                'department_id': department_id,
                'name': relation,
            }
        ]
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='yamb',
            relations=relations,
        )
        return ResourceModel(self.main_connection).get(
            id=resource['id'],
            org_id=self.organization['id'],
        )

    def create_resource_with_group(self, group_id, relation='read'):
        relations = [
            {
                'group_id': group_id,
                'name': relation,
            }
        ]
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=relations,
        )
        return ResourceModel(self.main_connection).get(
            id=resource['id'],
            org_id=self.organization['id'],
        )

    def response_with_exception(self, raised_exception,
                                expected_msg='unknown',
                                expected_code=500,
                                ):
        the_exception = raised_exception.exception
        response_data = json.loads(response.data)
        self.assertEqual(response.status_code, expected_code)
        self.assertEqual(response_data.get('error'), expected_msg)

    def clean_actions_and_events(self):
        ActionModel(self.main_connection).delete(force_remove_all=True)
        EventModel(self.main_connection).delete(force_remove_all=True)


    def clean_actions_events_and_tasks(self):
        self.clean_actions_and_events()
        TaskModel(self.main_connection).delete(force_remove_all=True)

    def get_org_revision(self, org_id):
        return OrganizationModel(self.main_connection).get(org_id, ['revision'])['revision']

    def update_service_trial_expires_date(self, org_id, service_id, expire_date):
        if isinstance(expire_date, datetime.datetime):
            # Так как в базе может быть таймзона отличная от UTC,
            # а мы хоим сохранять дату в UTC, то тут надо явно
            # привести тип к date, чтобы не было проблем с запуском
            # тестов в период с 00:00 по 03:00 по Москве
            expire_date = expire_date.date()

        self.main_connection.execute(
            mogrify(
                self.main_connection,
                query="UPDATE organization_services SET trial_expires=%(date)s, trial_status='expired' "
                      "WHERE org_id=%(org_id)s AND service_id=%(service_id)s",
                vars={
                    'date': expire_date,
                    'service_id': service_id,
                    'org_id': org_id,
                },
            )
        )

    def create_licenses_for_service(self, service_id, user_ids=None, group_ids=None, department_ids=None, org_id=None):
        if not org_id:
            org_id = self.organization['id']

        licenses = []
        for user_id in user_ids or []:
            licenses.append({'user_id': user_id, 'name': 'member'})
        for group_id in group_ids or []:
            licenses.append({'group_id': group_id, 'name': 'member'})
        for department_id in department_ids or []:
            licenses.append({'department_id': department_id, 'name': 'member'})

        resource_id = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'service_id': service_id,
            },
            fields=[
                'resource_id',
            ],
            one=True,
        )['resource_id']
        ResourceModel(self.main_connection).update_relations(
            resource_id=resource_id,
            org_id=org_id,
            relations=licenses
        )
        update_license_cache_task(self.main_connection, org_id=org_id, service_id=service_id)

        if self.autoprocess_tasks:
            self.process_tasks()

    def create_deputy_admin(self, uid=None, org_id=None, is_outer=True):
        if not org_id:
            org_id = self.organization['id']
        if not uid:
            uid = self.get_next_uid(outer_admin=is_outer)

        user_type = 'deputy_admin' if is_outer else 'inner_user'
        user = UserMetaModel(self.meta_connection).create(uid, org_id, user_type=user_type)

        if not is_outer:
            group = GroupModel(self.main_connection).get_or_create_deputy_admin_group(org_id)
            UserModel(self.main_connection).create(
                id=uid,
                nickname='deputy_admin',
                name=self.name,
                email='deputy_admin@' + self.domain_part,
                gender='male',
                org_id=org_id,
                department_id=1,
                groups=[group['id']],
            )
            UserModel(self.main_connection).update(
                {'role': 'deputy_admin'},
                {'id': uid, 'org_id': org_id}
            )

            user = UserModel(self.main_connection).get(
                user_id=user['id'],
                org_id=org_id,
                fields=[
                    '*',
                    'department.*',
                ]
            )
            # для отдела надо позвать метод пересчета мемберов
            # чтобы диффы событий были с правильными счетчиками
            DepartmentModel(self.main_connection).update_members_count(
                1,
                org_id,
            )

        return user

    def create_feature(self, enabled_default, feature_slug=None):
        feature_id = random.randint(1, 100000)
        if not feature_slug:
            feature_slug = 'feature_slug_{}'.format(feature_id)

        query = """
             INSERT INTO features (slug, enabled_default, description) VALUES
                 ('{}', {}, 'Описание')
        """.format(
            feature_slug,
            enabled_default,
        )
        self.meta_connection.execute(query)

        return feature_id, feature_slug

    def set_feature_value_for_organization(self, feature_id, enabled, org_id=None):
        if isinstance(feature_id, str):
            feature_id = get_feature_by_slug(self.meta_connection, feature_id)['id']

        if not org_id:
            org_id = self.organization['id']

        query = """
             INSERT INTO org_features(org_id, feature_id, enabled)
                 SELECT '{org_id}', {feature_id}, {enabled}
             ON CONFLICT (org_id, feature_id) DO UPDATE
                 SET enabled={enabled}
        """.format(
            org_id=org_id,
            feature_id=feature_id,
            enabled=enabled,
        )
        self.meta_connection.execute(query)


class TestYandexTeamOrgMixin(object):

    def setUp(self):
        self.domain_part = self.domain_part or app.config["DOMAIN_PART"]

        self.uid_sequence = count(1)
        self.start_patchers()

        self.user_counter = 0
        self.department_counter = 0
        self.group_counter = 0
        self.department_name = {
            'ru': 'Департамент',
            'en': 'Department'
        }
        self.group_name = {
            'ru': 'Группа',
            'en': 'Group'
        }
        self.name = {
            'first': {
                'ru': 'Пользователь',
                'en': ''
            },
            'last': {
                'ru': 'Автотестовый',
                'en': ''
            },
        }
        PresetModel(self.meta_connection).create('default', [], {})
        self.organization_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex-team',
        )
        self.organization = self.organization_info['organization']
        self.organization_domain = self.organization_info['domain']
        self.user = UserModel(self.main_connection).get(
            user_id=self.organization_info['admin_user']['id'],
            org_id=self.organization['id'],
            fields=[
                '*',
                'groups.*',
            ]
        )
        self.department = self.organization_info['root_department']
        self.domain = 'yandex-team.ru'

        set_auth_uid(self.user['id'])


class TestOuterAdmin(object):
    def setUp(self):
        super(TestOuterAdmin, self).setUp()
        self.orginfo_1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='brain',
            domain_part='yabiz.ru',
        )
        self.orginfo_2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='pain',
            domain_part='yabiz.ru',
        )

        self.organization1 = self.orginfo_1['organization']
        self.organization2 = self.orginfo_2['organization']

        # Создаем одного внешнего админа
        self.admin_org1 = UserMetaModel(self.meta_connection).create(
            id=PASSPORT_TEST_OUTER_UID,
            org_id=self.organization1['id'],
        )

        self.admin_org2 = UserMetaModel(self.meta_connection).create(
            id=PASSPORT_TEST_OUTER_UID,
            org_id=self.organization2['id'],
        )

        # И одного обычного админа
        self.user = UserModel(self.main_connection).get(
            user_id=self.orginfo_1['admin_user']['id'],
            org_id=self.organization1['id'],
            fields=[
                '*',
                'groups.*',
            ]
        )
        set_auth_uid(self.user['id'])


class TestOrganizationWithoutDomainMixin(object):
    def setUp(self):
        super(TestOrganizationWithoutDomainMixin, self).setUp()
        # создадим организацию без домена
        yandex_organization = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        self.yandex_organization = yandex_organization['organization']
        self.yandex_admin = yandex_organization['admin_user']
        org_id = self.yandex_organization['id']
        # и добавим в неё одного пользователя
        user_id = create_outer_uid()
        self.yandex_user = create_yandex_user(
            self.meta_connection,
            self.main_connection,
            user_id,
            org_id,
        )


def create_inner_uid(small_part):
    """Создает uid в диапазоне PDD пользователей.
    Результат будет состоять из границы + small_part.
    """
    return 111*10**13 + small_part


def create_outer_uid():
    million = 1000000
    uid = random.randint(1 * million, 2 * million)
    assert is_outer_uid(uid)
    return uid


class override_settings(object):
    def __init__(self, **kwargs):
        self.settings_for_override = kwargs
        self.original_settings = None

    def __enter__(self):
        self.original_settings = {}
        for name, value in self.settings_for_override.items():
            self.original_settings[name] = app.config.get(name, None)
            app.config[name] = value

    def __exit__(self, exc_type, exc_value, traceback):
        for name, value in self.original_settings.items():
            app.config[name] = value
        self.original_settings = None

    def __call__(self, func):
        @wraps(func)
        def decorated_func(*args, **kwargs):
            with self:
                return func(*args, **kwargs)
        return decorated_func


class override_mailer(object):
    def __init__(self):
        from intranet.yandex_directory.src.yandex_directory.core import mailer
        self.mailer = mailer
        self.original_send = mailer.send

    def __enter__(self):
        from intranet.yandex_directory.src.yandex_directory.core.mailer.mailer import send
        self.mailer.send = send

    def __exit__(self, exc_type, exc_value, traceback):
        self.mailer.send = self.original_send

    def __call__(self, func):
        @wraps(func)
        def decorated_func(*args, **kwargs):
            with self:
                return func(*args, **kwargs)
        return decorated_func


class TestDepartments(object):
    def refresh_deparments(self):
        for name in dir(self):
            if re.match('department\d$', name):
                self.refresh(getattr(self, name))

    def assertSubMembersCount(self, department, count):
        self.assertEqual(department['members_count'], count)


def assert_that_not_found(response):
    """Проверяет, что ответ сервера содержит
    код и текст сообщения not found.
    """
    assert_that(
        response,
        has_entries(
            code='not_found',
            message='Not found',
        )
    )


def has_only_entries(**kwargs):
    """Возвращает Hamcrest матчер, который проверяет, что
    словарь содержит только те ключи, которые перечислены в kwargs.
    """
    return all_of(
        contains_inanyorder(*list(kwargs.keys())),
        has_entries(**kwargs)
    )


def is_same(obj):
    """Возвращает матчер, который проверяет, что объект соответствует obj.

    Но в отличие от простого сравнения путём equal_to, тут структура будет
    сравниваться посредством отдельных матчеров и сообщение об ошибке будет
    более детальным.
    """
    def get_matchers(obj):
        if isinstance(obj, dict):
            return has_only_entries(
                **{
                    key: get_matchers(value)
                    for key, value in list(obj.items())
                }
            )
        elif isinstance(obj, (list, tuple)):
            return contains(*list(map(get_matchers, obj)))
        elif isinstance(obj, BaseMatcher):
            return obj
        else:
            return equal_to(obj)
    return get_matchers(obj)


def extract_get_params(url):
    """
    Парсим GET параметры строки http запроса
    :param url: http запрос
    :rtype: dict
    """
    query = urlparse(url).query
    return dict(parse_qsl(query))


def oauth_success(client_id=OAUTH_CLIENT_ID, uid=None, scopes=None):
    """
    Декоратор для методов тестов.
    Подменяем ответ BB
    :param client_id: OAuth client_id
    :param uid: uid пользователя
    :param scope: OAuth scope
    """
    def wrapper(func):
        @wraps(func)
        def decorator(*args, **kwargs):
            with patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:
                oauth_info = odict({
                    "status": "VALID",
                    "uid": uid,
                    "oauth": {
                        "client_id": client_id,
                        "scope": ' '.join(scopes or []),
                        "client_name": "Autotest",
                    }
                })
                mock_blackbox_instance.oauth.return_value = oauth_info
                return func(*args, **kwargs)
        return decorator
    return wrapper


def tvm2_auth_success(
        uid,
        scopes=[],
        org_id=None,
        service=None,
        user_ticket=None,
):
    """
    Декоратор для методов тестов.
    Подменяет авторизацию через TVM2
    :param uid: uid пользователя
    :param scopes: сервисные скоупы
    :param org_id: id организации
    :param service: tvm service
    :return:
    """
    def wrapper(func):
        @wraps(func)
        def decorator(*args, **kwargs):
            default_service = Service(
                id=999,
                name='Test service',
                identity='test_service',
                is_internal=True,
                ip='127.0.0.1',
            )
            with patch.object(AuthMiddleware, '_authenticate') as tvm2_auth:
                auth_info = odict({
                    "org_id": org_id,
                    "user": TeamUser(uid, '127.0.0.1'),
                    "scopes": scopes,
                    "service": service or default_service,
                    "user_ticket": user_ticket,
                })
                tvm2_auth.return_value = auth_info
                return func(*args, **kwargs)
        return decorator
    return wrapper


@contextmanager
def tvm2_auth(
        uid,
        scopes=[],
        org_id=None,
        service=None,
        user_ticket=None,
):
    """
    Контекстный менеджер для тестов.
    Подменяет авторизацию через TVM2
    :param uid: uid пользователя
    :param scopes: сервисные скоупы
    :param org_id: id организации
    :param service: tvm service
    :return:
    """
    default_service = Service(
        id=999,
        name='Test service',
        identity='test_service',
        is_internal=True,
        ip='127.0.0.1',
    )
    with patch.object(AuthMiddleware, '_authenticate') as tvm2_auth:
        auth_info = odict({
            "org_id": org_id,
            "user": TeamUser(uid, '127.0.0.1'),
            "scopes": scopes,
            "service": service or default_service,
            "user_ticket": user_ticket,
        })
        tvm2_auth.return_value = auth_info
        yield


@contextmanager
def oauth_client(client_id=OAUTH_CLIENT_ID, uid=None, scopes=None):
    """
    Контекстный менеджер, который подменяет ответ от blackbox
    про oauth токен.

    :param client_id: OAuth client_id
    :param uid: uid пользователя
    :param scope: Список скоупов
    """
    with patch.object(app, 'blackbox_instance') as mock_blackbox_instance:
        oauth_info = odict({
            "status": "VALID",
            "uid": uid,
            "oauth": {
                "client_id": client_id,
                "scope": ' '.join(scopes or []),
                "client_name": "Autotest",
            }
        })
        mock_blackbox_instance.oauth.return_value = oauth_info
        yield


def create_robot_for_anothertest(meta_connection, main_connection,
                                 org_id, slug, post_request, name='Service Autotest Name'):
    # Создаем тестового робота и отдаем его uid
    # создаем нужные данные про сервис и включаем его у организации
    service_data = {
        'client_id': 'NogLWAmen3usaapXzhVABfCTWzrQxC',
        'slug': slug,
        'name': name,
        'robot_required': True,
    }
    ServiceModel(meta_connection).create(**service_data)
    with mocked_passport() as passport:
        # в момент запуска автотестов не ходим в паспорт, а хардкодим uid в Паспортном диапазоне
        passport.account_add.return_value = 111*10**13 + 100
        post_request('/services/%s/enable/' % service_data['slug'], data=None)
    robots_uids = OrganizationModel(main_connection).get_robots(org_id)
    return robots_uids[0]


def simplify_diff(diff):
    """
    Упрощает дифф типа такого:
    {
        'remove': {'users': [1135456376031983], 'groups': [], 'departments': []},
        'add': {'users': [], 'groups': [], 'departments': []}
    }
    До

    {
        'remove': {'users': [1135456376031983]}
    }
    """
    remove = {key: value
              for key, value in list(diff['remove'].items())
              if value}
    add = {key: value
              for key, value in list(diff['add'].items())
              if value}
    result = {}
    if remove:
        result['remove'] = remove
    if add:
        result['add'] = add

    return result


def print_event(event):
    """Печатает одно событие с сокращёной информацией,
    позволяющей разобраться в происходящем.
    """
    name = event['name']

    print(name)

    if name == 'group_membership_changed':
        print('  group_id:', event['object']['id'])
        print('  diff:', event['content']['diff']['members'])
    elif name == 'user_group_added':
        print('  user_id:', event['object']['id'])
        print('  group_id:', event['content']['subject']['id'])
    elif name == 'user_group_deleted':
        print('  user_id:', event['object']['id'])
        print('  group_id:', event['content']['subject']['id'])
    elif name == 'resource_grant_changed':
        print('  resource_id:', event['object']['id'])
        print('  diff:', simplify_diff(event['content']['relations']))
    elif name == 'user_property_changed':
        print('  user_id:', event['object']['id'])
        print('  diff:', event['content']['diff'])
    else:
        print('  not supported')


def print_events(events):
    """Вспомогательная функция для того, чтобы сжато вывести содержимое
    списка событий.

    Полезна когда нужно в тестах разобраться, что из себя представляют
    сгенерированные события.
    """
    for event in events:
        print_event(event)


class AdvancedRaises(Raises):
    """
    Дополнительно проверяет атрибуты исключения
    """

    def __init__(self, expected, **kwargs):
        super(AdvancedRaises, self).__init__(expected)
        self.params = kwargs
        self.mismatched_params = {}

    def _call_function(self, function):
        result = super(AdvancedRaises, self)._call_function(function)
        if result:
            # дополнительно проверим атрибуты
            for key, value in list(self.params.items()):
                actual_value = getattr(self.actual, key)
                if actual_value != value:
                    self.mismatched_params[key] = (actual_value, value)

            if self.mismatched_params:
                return False
            else:
                return True


    def describe_to(self, description):
        text = 'Expected a callable raising %s' % self.expected
        if self.params:
            text += ' with params: ' + ', '.join(
                map('{0[0]}={0[1]}'.format, list(self.params.items()))
            )
        description.append_text(text)

    def describe_mismatch(self, item, description):
        if self.mismatched_params:
            description.append_text('some params mismatched:')
            for key, (actual, expected) in list(self.mismatched_params.items()):
                description.append_text(
                    '\n       {key} == {actual!r} instead of {expected!r}'.format(
                        key=key,
                        actual=actual,
                        expected=expected,
                    )
                )
        else:
            super(AdvancedRaises, self).describe_mismatch(item, description)


def raises(exception, **params):
    """Более продвинутый Hamcrest матчер, который позволяет проверять
    атрибуты исключения. Например:

    assert_that(
        calling(authenticate_by_tvm).with_args(
            'some_ticket',
        ),
        raises(
            APIError,
            message='Header x-user-ip is required',
            status_code=400,
        )
    )
    """

    return AdvancedRaises(exception, **params)


def datefield_to_isoformat(data, *fields):
    """
    Преобразуем поля словаря из  datetime в в строку в формате iso
    :param data: словарь
    :type data: dict
    :param field: имя поля с датой
    :type field: str
    :rtype: dict
    """
    inner_data = data.copy()
    for field in fields:
        inner_data[field] = data[field].isoformat()
    return inner_data


def change_user_data_created_fields_to_isoformat(user):
    """
    Перобразуем поле created у пользователя и отдела пользователя из datetime в строку в iso формате
    :param user: данные о пользователе
    """
    inner_user = datefield_to_isoformat(user, 'created', 'updated_at')
    inner_user['department'] = datefield_to_isoformat(inner_user['department'], 'created')
    return inner_user


@contextmanager
def auth_as(service, org_id=None, user_id=None, scopes=[]):
    """Этот контекстный менеджер позволяет прикинуться, будто
    аутентификация прошла успешно и в директорию обратился указанный сервис.

    Опционально можно указать организацию или пользователя, а так же скоупы.
    Указанные явно скоупы имеют больший приоритет, чем скоупы определёные
    для сервиса.
    """

    with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.AuthMiddleware._authenticate') as authenticate:
        if user_id:
            user = User(
                uid=user_id,
                ip='127.0.0.1',
            )
        else:
            user = None

        authenticate.return_value = {
            'auth_type': 'tvm',
            'service': Service(
                id=service['id'],
                name=service['name'],
                identity=service['slug'],
                is_internal=True,
                ip='127.0.0.1',
            ),
            'scopes': scopes or service['scopes'],
            'user': user,
            'org_id': org_id,
        }
        yield


def assert_not_called(mocked_function):
    assert mocked_function.call_count == 0, \
        'Function {0} was called {1} times, but should not'.format(
            mocked_function,
            mocked_function.call_count,
        )


def assert_called_once(mocked_function, *args, **kwargs):
    """Лучше использовать эту функцию вместо метода mock объекта, чтобы избежать опечаток.
       Ведь в результате опечаток кажется, что тест проходит, хотя на самом деле assert не вызывается.
    """
    mocked_function.assert_called_once_with(*args, **kwargs)


def assert_called(mocked_function, count, msg=None):
    msg = msg or 'Function {0} was called {1} times, but should be called {2} times'.format(
        mocked_function,
        mocked_function.call_count,
        count
    )
    assert mocked_function.call_count == count, msg


@contextmanager
def calls_count(mocked_function, count, msg=None):
    """Аналог assert_called, но в виде контекстного менеджера.
    В отличие от assert_called, проверяет количество вызовов к замоканной функции внутри блока with.
    """
    initial_count = mocked_function.call_count
    yield
    # Так как в отчете об ошибке нам надо показывать цифры с учётом разницы,
    # то переиспользовать assert_called тут не получится.
    call_count = mocked_function.call_count - initial_count
    msg = msg or 'Function {0} was called {1} times, but should be called {2} times'.format(
        mocked_function,
        call_count,
        count
    )
    assert call_count == count, msg

def mocked_blackbox():
    return patch.object(app, 'blackbox_instance')

def mocked_zora_client():
    return patch.object(app, 'zora_client')

def mocked_passport():
    return patch.object(app, 'passport')

def mocked_idm_b2b():
    return patch.object(app, 'idm_b2b_client')

def mocked_team_passport():
    return patch.object(app, 'team_passport')

def mocked_domenator():
    return patch.object(app, 'domenator')


def mocked_requests():
    requests_mock = Mock()
    requests_mock.get.return_value.status_code = 200
    requests_mock.patch.return_value.status_code = 200
    requests_mock.post.return_value.status_code = 200
    requests_mock.delete.return_value.status_code = 200
    return patch.object(app, 'requests', requests_mock)


def make_passport_response(status='ok', errors=None, **payload):
    if errors is None:
        errors = []

    # Если errors не пустые, статус всегда 'error'.
    # Если пустые, то status берется из аргумента, что позволяет явно указать status='error' при пустых errors.
    status = 'error' if errors else status
    return_value = {'status': status}

    if errors:
        return_value['errors'] = errors
    else:
        return_value.update(payload)

    return return_value


def format_date(date):
    if isinstance(date, datetime.datetime):
        return date.isoformat()
    return date


def fake_userinfo(uid=1,
                  default_email='user@yandex.ru',
                  birth_date='1980-01-01',
                  login='user',
                  first_name='Vasya',
                  last_name='Pupkin',
                  sex='1',
                  language='ru',
                  is_maillist=False,
                  is_available=True,
                  aliases=[],
                  karma=0,
                  cloud_uid=None,
                  cloud_display_name=None,
                  country='ru',
                  timezone='Europe/Moscow',
                  **kwargs
                  ):
    """Эту функцию полезно использовать когда ты мокаешь
       app.blackbox_instance.userinfo.return_value

       Она возвращает словарь, в котором либо какие-то
       значения по-умолчанию, если они для теста не важны,
       либо те значения, которые переданы в fake_userinfo.
    """
    fields = dict(
        birth_date=birth_date,
        login=login,
        first_name=first_name,
        last_name=last_name,
        sex=sex,
        country=country,
        language=language,
        aliases=aliases,
    )
    attributes = {
        blackbox_client.IS_MAILLIST_ATTRIBUTE: '1' if is_maillist else '0',
        blackbox_client.IS_AVAILABLE_ATTRIBUTE: '1' if is_available else '0',
        blackbox_client.CLOUD_UID_ATTRIBUTE: cloud_uid,
        blackbox_client.TIMEZONE_ATTRIBUTE: timezone,
    }
    result = dict(
        uid=str(uid),  # от блекбокса uid приходит строкой
        default_email=default_email,
        fields=fields,
        attributes=attributes,
        karma=str(karma),
        display_name={
            'name': cloud_display_name,
        }
    )
    return result


all_object_permissions = []
permission_collections = [
    group_permissions,
    user_permissions,
    department_permissions,
    organization_permissions,
    service_permissions,
]
for permissions in permission_collections:
    all_object_permissions.extend([
        getattr(permissions, attr) for attr in vars(permissions)
        if not attr.startswith('__')
    ])


def get_all_admin_permssions():
    admin_permissions = copy(all_global_permissions + all_object_permissions)
    # С некоторых пор мы включили ЯОрг режим для всех организаций,
    # а у них недоступен расширенный коннект и смена тарифного плана.
    admin_permissions.remove(global_permissions.change_subscription_plan)
    # этот пермишен добавляется только если организация облачная
    admin_permissions.remove(global_permissions.manage_cloud_services_enable)
    # только внешний админ может добавлять себе организации
    admin_permissions.remove(organization_permissions.add_organization)
    # только владелец может менять владельца в организации с включенной фичей, добавляется отдельно
    admin_permissions.remove(organization_permissions.change_owner)
    # этот пермишшен добавляется только если объект - внештатник
    admin_permissions.remove(user_permissions.move_to_staff)
    # добавляем пермишен SSO
    admin_permissions.append(sso_permissions.read_sso_settings)
    return admin_permissions


def get_yandex_team_permissions():
    return [
        global_permissions.add_users,
        global_permissions.add_groups,
        group_permissions.edit,
        department_permissions.edit,
    ]


def get_all_permissions_without_current_user():
    # Пользователю запрещено блокировать себя, убирать из админов
    #
    # Но при этом пользователь может попытаться уволиться, это нормально,
    # потому что уже во вьюшке мы проверим это и
    # выдадим предупреждение, если он владелец организации:
    # https://st.yandex-team.ru/DIR-6264
    # вот тут проверка:
    # https://github.yandex-team.ru/yandex-directory/yandex-directory/blob/fc6d42acae79840d9d902a5472554c8c801486ae/src/yandex_directory/core/views/users.py#L1848-L1859

    current_user_restrict_permissions = [
        user_permissions.block,
        user_permissions.make_admin,
        global_permissions.leave_organization,
    ]
    all_admin_permissions = get_all_admin_permssions()
    return [p for p in all_admin_permissions if p not in current_user_restrict_permissions]


def get_outer_admin_permissions():
    outer_admin_permissions = copy(all_global_permissions + all_object_permissions)
    # внешний админ не может менять тарифный план организации
    outer_admin_permissions.remove(global_permissions.change_subscription_plan)
    outer_admin_permissions.remove(global_permissions.manage_licenses)
    outer_admin_permissions.remove(global_permissions.manage_tracker)
    # внешний админ не может оплачивать услуги
    outer_admin_permissions.remove(global_permissions.can_pay)
    # только владелец может менять владельца в организации с включенной фичей, добавляется отдельно
    outer_admin_permissions.remove(organization_permissions.change_owner)
    # этот пермишшен добавляется только если объект - внештатник
    outer_admin_permissions.remove(user_permissions.move_to_staff)
    outer_admin_permissions.remove(global_permissions.manage_cloud_services_enable)
    outer_admin_permissions.append(sso_permissions.read_sso_settings)
    return outer_admin_permissions


def get_deputy_admin_permissions():
    return [
        global_permissions.add_users,
        global_permissions.invite_users,
        global_permissions.add_groups,
        global_permissions.add_departments,
        global_permissions.remove_departments,
        department_permissions.edit,
        group_permissions.edit,
        user_permissions.edit,
        user_permissions.edit_info,
        user_permissions.block,
        user_permissions.change_avatar,
        user_permissions.change_password,
        user_permissions.dismiss,
        user_permissions.edit_birthday,
        user_permissions.edit_contacts,
        user_permissions.change_alias,
        global_permissions.use_connect,
        global_permissions.leave_organization,
    ]


def get_yandex_organization_permissions_with_domain():
    yandex_organization_admin_permissions = copy(all_global_permissions + all_object_permissions)
    # нельзя включать расширенный коннект
    yandex_organization_admin_permissions.remove(global_permissions.change_subscription_plan)
    # только владелец может менять владельца в организации с включенной фичей, добавляется отдельно
    yandex_organization_admin_permissions.remove(organization_permissions.change_owner)
    # нельзя добавлять организации
    yandex_organization_admin_permissions.remove(organization_permissions.add_organization)
    # нельзя включать сервисы в облачных организациях
    yandex_organization_admin_permissions.remove(global_permissions.manage_cloud_services_enable)

    yandex_organization_admin_permissions.append(sso_permissions.read_sso_settings)

    return yandex_organization_admin_permissions

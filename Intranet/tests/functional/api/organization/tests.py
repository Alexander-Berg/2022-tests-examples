# -*- coding: utf-8 -*-
import datetime
import json
import pytest
import responses
from intranet.yandex_directory.src import settings
import base64
import xmlrpc.client
from contextlib import contextmanager
from copy import copy
from unittest import skip
from xmlrpc.client import Fault

from hamcrest import (
    assert_that,
    contains_inanyorder,
    contains,
    contains_string,
    has_entries,
    equal_to,
    has_length,
    not_none,
    empty,
    all_of,
    is_not,
    has_key,
    none,
    has_items,
    has_item,
    not_,
)
from unittest.mock import (
    patch,
    ANY,
    Mock,
)
from werkzeug.datastructures import FileMultiDict

from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client
from .... import webmaster_responses
from ....testutils import (
    create_outer_uid,
    assert_not_called,
    assert_called_once,
    assert_called,
    TestCase,
    create_organization,
    PASSPORT_TEST_OUTER_UID,
    override_settings,
    has_only_entries,
    mocked_blackbox,
    create_organization_without_domain,
    create_yandex_user,
    get_auth_headers,
    get_oauth_headers,
    OAUTH_CLIENT_ID,
    oauth_success,
    set_auth_uid,
    NOT_FOUND_RESPONSE,
    TestOrganizationWithoutDomainMixin,
    source_path,
    calls_count,
    MockToDict, fake_userinfo, auth_as,
)
from intranet.yandex_directory.src.yandex_directory.core.cloud.tasks import SyncCloudOrgTask
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.mds import MdsS3ApiClient
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.billing.client import WORKSPACE_SERVICE_ID
from intranet.yandex_directory.src.yandex_directory.common.db import catched_sql_queries, mogrify
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    make_simple_strings,
    format_date,
    to_punycode,
    is_spammy,
    maybe_has_domain,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow, date_to_datetime
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.dependencies import (
    Service,
    Setting,
)
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.features import (
    set_feature_value_for_organization,
    CAN_WORK_WITHOUT_OWNED_DOMAIN,
    CHANGE_ORGANIZATION_OWNER,
    MULTIORG,
    USE_CLOUD_PROXY,
)

from intranet.yandex_directory.src.yandex_directory.core.features.utils import get_feature_by_slug, is_feature_enabled
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ImageModel,
    OrganizationMetaModel,
    EventModel,
    OrganizationModel,
    UserModel,
    GroupModel,
    DepartmentModel,
    ActionModel,
    OrganizationBillingInfoModel,
    OrganizationBillingConsumedInfoModel,
    OrganizationMetaKeysModel,
    UserMetaModel,
    DomainModel,
    ServiceModel,
    OrganizationServiceModel,
    PresetModel,
    PromocodeModel,
    OrganizationPromocodeModel,
    UserServiceLicenses,
    TaskModel,
    WebmasterDomainLogModel,
    ResourceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.group import (
    GROUP_TYPE_ROBOTS,
    GROUP_TYPE_ORGANIZATION_ADMIN,
    GROUP_TYPE_ORGANIZATION_DEPUTY_ADMIN,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    get_price_and_product_id_for_service,
    promocode_type,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    TRACKER_SERVICE_SLUG,
    MAILLIST_SERVICE_SLUG,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    disable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    global_permissions,
    organization_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.tasks import SyncSingleDomainTask
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    get_domains_by_org_id,
    only_attrs,
    only_ids,
    get_organization_admin_uid,
    RANGE_PASSPORT,
)

from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory.core.utils.tasks import (
    ChangeOrganizationOwnerTask,
    UpdateOrganizationMembersCountTask
)
from intranet.yandex_directory.src.yandex_directory.core.views.organization.view import OrganizationView
from intranet.yandex_directory.src.yandex_directory.core.views.organization import constants
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import PassportUnavailable
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.meta.models import Registrar
from sqlalchemy.orm import Session
from intranet.yandex_directory.src.yandex_directory.core.registrar import RegistrarPassword
from intranet.yandex_directory.src.yandex_directory.core.events.tasks import UpdateMembersCountTask
from intranet.yandex_directory.src.yandex_directory.core.tasks import SyncExternalIDS


class TestOrganization__get(TestCase):

    def test_me(self):
        auth_headers = get_auth_headers()

        # включаем сервис в организации
        service_slug = 'slug'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='Autotest Service',
            client_id='kjkasjdkakds',
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )

        response_data = self.get_json('/organization/', headers=auth_headers)

        assert_that(
            response_data,
            has_entries(
                id=self.organization['id'],
                name=self.organization['name'],
                label=self.organization['label'],
                domains=get_domains_by_org_id(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                ),
                # опциональные поля должны быть пусты
                head=None,
                law_address='',
                real_address='',
                # подключенные сервисы
                services=contains(service_slug),
                revision=self.organization['revision'],
                # там ещё есть и другие поля, но я не будут тут их все
                # перечислять
            )
        )

    def test_with_org_id_in_the_header(self):
        auth_headers = get_auth_headers(as_org=self.organization['id'])

        response_data = self.get_json('/organization/', headers=auth_headers)
        expected = {
            'id': self.organization['id'],
            'name': self.organization['name'],
            'label': self.organization['label']
        }
        self.assertEqual(response_data['id'], expected['id'])
        self.assertEqual(response_data['name'], expected['name'])
        self.assertEqual(response_data['label'], expected['label'])
        self.assertEqual(
            response_data['domains'],
            get_domains_by_org_id(
                self.meta_connection,
                self.main_connection,
                self.organization['id']
            )
        )

    @pytest.mark.skip('not check authrization headers for /organization/, just for easy in dev')
    def test_no_user_in_db(self):
        headers = get_auth_headers()
        headers['X-UID'] = 3442454524  # некий несуществующий в базе UID
        response = self.client.get('/organization/', headers=headers)
        self.assertEqual(response.status_code, 403)
        response_data = json.loads(response.data)
        expected = {'message': 'User has no organization'}
        self.assertEqual(response_data, expected)


class TestOrganization__post(TestCase):
    def setUp(self):
        super(TestOrganization__post, self).setUp()

        # id домена в паспорте
        self.domid = '1011403'
        # id админа организации в паспорте (создан при регистрации через паспорт)
        self.admin_uid = '1130000000293941'
        # домен новой организации
        self.domain = 'test-org{}'.format(app.config['DOMAIN_PART'])

        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'birth_date': '',
                'firstname': '\u041d\u0430\u0438\u043b\u044c',
                'lastname': '\u0425\u0443\u043d\u0430\u0444\u0438\u043d',
                'sex': '0',
                'login': 'admin@camomile.yaserv.biz',
            },
            'uid': '1130000000293941',
            'domain': 'camomile.yaserv.biz',
            'default_email': 'default@ya.ru',
        }
        self.mocked_blackbox.hosted_domains.return_value = {
            'hosted_domains': [{
                'admin': '1130000000293941',
                'born_date': '2016-08-24 13:22:25',
                'default_uid': '0',
                'domain': self.domain,
                'domid': self.domid,
                'ena': '1',
                'master_domain': '',
                'mx': '0',
                'options': '{"organization_name": "Название организации"}'
            }]
        }

    def test_finish_success(self):
        # удачно завершаем дорегистрацию новой организации

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email') as mocked_delay_welcome_email:
            result = self.post_json(
                '/organization/',
                data={},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )
        # данные по пользователю из ЧЯ
        assert_called_once(
            self.mocked_blackbox.userinfo,
            uid=int(self.admin_uid),
            userip=ANY,
            dbfields=ANY,
            emails='getdefault',
            attributes='193',
        )

        # получили ид созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )
        org_id = result['org_id']

        assert_called_once(
            self.mocked_passport.set_organization_name,
            self.domid,
            'org_id:{}'.format(org_id),
        )

        # источник организации код приглашения
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='unknown',
                # А язык по-умолчанию должен быть ru
                language='ru',
            )
        )

        # отправили приветственное письмо
        mocked_delay_welcome_email.assert_called_once_with(org_id, self.organization_info['admin_user']['nickname'],
                                                           int(self.admin_uid))

        # У корневого отдела должен быть label, чтобы создалась рассылка.
        # Долгое время у нас тут был баг, и рассылки не создавались:
        # https://st.yandex-team.ru/DIR-3055
        department = DepartmentModel(self.main_connection).find(
            {'org_id': org_id, 'id': ROOT_DEPARTMENT_ID},
            one=True,
        )
        assert_that(
            department,
            has_entries(
                label='all',
            )
        )

        # Про заведённый отдел должно было быть сгенерено событие
        events = EventModel(self.main_connection).find(
            {
                'org_id': org_id,
                'name': 'department_added',
            },
        )
        assert_that(
            events,
            contains(
                has_entries(
                    object=has_entries(
                        id=ROOT_DEPARTMENT_ID,
                    )
                )
            )
        )

        # проверяем, что создалась группа для роботов
        robot_group = GroupModel(self.main_connection).get_robot_group(org_id)
        assert_that(
            robot_group,
            has_entries(
                label=None,
                name_plain='Роботы организации',
            )
        )
        events = EventModel(self.main_connection).find(
            {
                'org_id': org_id,
                'name': 'group_added',
            },
        )

        assert_that(
            events,
            contains_inanyorder(
                has_entries(
                    object=has_entries(
                        type=GROUP_TYPE_ROBOTS,
                    )
                ),
                has_entries(
                    object=has_entries(
                        type=GROUP_TYPE_ORGANIZATION_ADMIN,
                    )
                ),
                has_entries(
                    object=has_entries(
                        type=GROUP_TYPE_ORGANIZATION_DEPUTY_ADMIN,
                    )
                )
            )
        )

    def test_set_language(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр language и он сохранится в базе

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'language': 'en'},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Теперь достанем организацию из базы и проверим,
        # что язык у неё - английский.
        assert_that(
            OrganizationModel(self.main_connection).get(result['org_id']),
            has_entries(
                source='unknown',
                language='en',
            )
        )

    def test_set_tld(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр tld и он сохранится в базе

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        tld = 'tld'

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'tld': tld},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Теперь достанем организацию из базы и проверим,
        # что записался переданный tld.
        assert_that(
            OrganizationModel(self.main_connection).get(result['org_id']),
            has_entries(
                tld=tld,
            )
        )

    def test_set_country(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр country и он сохранится в базе

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'country': 'Russia'},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Теперь достанем организацию из базы и проверим,
        # что язык у неё - английский.
        assert_that(
            OrganizationModel(self.main_connection).get(result['org_id']),
            has_entries(
                source='unknown',
                country='Russia',
            )
        )

    def test_set_source(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр source и он сохранится в базе

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        source = 'source'

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'source': source},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Теперь достанем организацию из базы и проверим,
        # источник организации
        assert_that(
            OrganizationModel(self.main_connection).get(result['org_id']),
            has_entries(
                source=source,
            )
        )

    def test_set_maillist_type(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр maillist_type и он сохранится в базе

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        maillist_type = 'shared'

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'maillist_type': maillist_type},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Теперь достанем организацию из базы и проверим,
        # источник организации
        assert_that(
            OrganizationModel(self.main_connection).get(result['org_id']),
            has_entries(
                maillist_type=maillist_type,
            )
        )

    def test_conflict_error(self):
        # домен уже кем-то подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'], True)

        result = self.post_json(
            '/organization/',
            data={},
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=409
        )
        assert_that(
            result,
            has_entries(
                code='domain_occupied'
            )
        )

    def test_idempotent(self):
        # организация для пользователя уже успешно заведена ранее

        # якобы созданная ранее новая организация
        some_organization_info = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            label='some-organization'
        )
        admin_uid = some_organization_info['admin_user_uid']
        org_id = some_organization_info['organization']['id']

        result = self.post_json(
            '/organization/',
            data={},
            headers=get_auth_headers(as_uid=admin_uid),
            expected_code=200
        )
        assert_that(
            result,
            has_entries(
                org_id=org_id  # получили ранее созданную организацию
            )
        )

    def test_post_internal(self):
        # ручка должна быть internal
        assert_that(
            OrganizationView.post.__dict__.get('internal', False),
            equal_to(True)
        )

    def test_russian_language(self):
        # Проверим, что для организации заведётся отдел Все сотрудники
        # и в ключе 'ru' будет лежать название на русском, а в en - на английском
        data = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            label='some-organization',
            language='ru',
        )
        org_id = data['organization']['id']
        department = DepartmentModel(self.main_connection).find(
            {'org_id': org_id, 'id': 1},
            one=True,
        )
        assert_that(
            department['name'],
            has_entries(
                ru='Все сотрудники',
                en='All employees',
            )
        )

    def test_english_language(self):
        # Проверим, что для англоязычной организации заведётся корневой отдел
        # и в ключе 'ru' будет лежать название на английском, как и в en.
        # Зачем это нужно - в тикете, описано в DIR-3000.

        data = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            label='some-organization',
            language='en',  # Эта организация на английском.
        )
        org_id = data['organization']['id']

        department = DepartmentModel(self.main_connection).find(
            {'org_id': org_id, 'id': 1},
            one=True,
        )
        assert_that(
            department['name'],
            has_entries(
                ru='All employees',
                en='All employees',
            )
        )

    def test_create_with_preset(self):
        # Проверим, что при дорегистрации можно указать пресет и включатся только
        # те сервисы, которые в нём указаны, а так же их зависимости

        # Сделаем сервис, который надо включить в пресет
        service_slug = 'the-service'
        ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='The Service',
        )

        # Создадим пресет
        PresetModel(self.meta_connection).create(
            'only-the-service',
            service_slugs=[service_slug],
            settings={}
        )
        dependencies = {
            Service(service_slug): [
                Setting('shared-contacts', True)
            ],
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=dependencies), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'preset': 'only-the-service'},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

            # Проверим, что в организации подключился только указанный сервис
            org_id = result['org_id']
            org = OrganizationModel(self.main_connection).find(
                {'id': org_id},
                fields=['services.slug'],
                one=True,
            )
            assert_that(
                org['services'],
                contains(
                    has_entries(
                        slug=service_slug,
                    )
                )
            )

    def test_create_with_maillist_service(self):
        # Создадим организацию с пресетом, включащим новый сервис рассылок

        # Создадим пресет
        PresetModel(self.meta_connection).create(
            'maillist',
            service_slugs=[MAILLIST_SERVICE_SLUG],
            settings={}
        )
        dependencies = {
            Service(MAILLIST_SERVICE_SLUG): [],
        }

        maillist_uid = 123123
        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=dependencies), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.create_maillist', return_value=maillist_uid) as create_maillist:
             result = self.post_json(
                '/organization/',
                data={'preset': 'maillist'},
                headers=get_auth_headers(as_uid=self.admin_uid),
             )
             assert_called_once(create_maillist, ANY, result['org_id'], 'all', ignore_login_not_available=True)

        # досаздали рассылку в паспорте и запомнили ее uid
        assert_that(
            DepartmentModel(self.main_connection).get(ROOT_DEPARTMENT_ID, result['org_id']),
            has_entries(uid=maillist_uid)
        )

    def test_set_outer_id(self):
        # Проверяем, что при дорегистрации может быть передан
        # параметр outer_id и если он внешний, то будет добавлен как внешний админ организации
        outer_id = 1

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'outer_id': outer_id},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # Проверим, что у организации создался внешний админ с id = outer_id
        assert_that(
            UserMetaModel(self.meta_connection).find(
                filter_data={'org_id': result['org_id'], 'is_outer': True},
                one=True,
            ),
            has_entries(
                id=outer_id,
                is_outer=True,
                org_id=result['org_id']
            )
        )

    def test_set_incorrect_outer_id(self):
        # Проверяем, что если при дорегистрации передан
        # ПДДшный outer_id, то он не будет добавлен как внешний админ
        pdd_id = 1234 + 111 * 10 ** 13

        # домен уже кем-то добавлен, но не подтвержден
        DomainModel(self.main_connection).create(self.domain, self.organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view._delay_welcome_email'):
            result = self.post_json(
                '/organization/',
                data={'outer_id': pdd_id},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # проверим, что у организации нет внешнего админа
        assert_that(
            UserMetaModel(self.meta_connection).find(
                filter_data={'org_id': result['org_id'], 'is_outer': True},
                one=True,
            ),
            equal_to(None)
        )

    def test_no_hosted_domains(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.app.blackbox_instance.hosted_domains',
                   return_value={'hosted_domains': None}):
            self.post_json(
                '/organization/',
                data={},
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=400,
            )


class TestOrganizationByLabel__get(TestCase):
    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_exists(self):
        # Проверим, что ручка ответит 200 кодом, если такая организация есть,
        # а у запраишвающего есть нужный scope.
        headers = get_oauth_headers()
        response = self.client.get(
            '/organization/%s/' % self.organization['label'],
            headers=headers,
        )
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data)
        expected = {
            'label': self.organization['label']
        }
        self.assertEqual(response_data, expected)

    def test_not_exists(self):
        response = self.client.get('/organization/not_exist/')
        self.assertEqual(response.status_code, 404)
        response_data = json.loads(response.data)
        self.assertEqual(response_data, NOT_FOUND_RESPONSE)


class TestOrganizations___get(TestCase):
    def test_with_only_org_id_in_the_header(self):
        # запрашиваем список организаций без передачи X-UID
        # или авторизации сервисом подключенным к организациям
        auth_headers = get_auth_headers(as_org=self.organization['id'])
        response = self.get_json('/organizations/', headers=auth_headers)
        assert_that(response, empty())

    def test_with_x_uid_in_the_header(self):
        # Проверим, что на запрос от имени пользователя мы отдадим
        # только его организации.

        # Включаем сервис в организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )

        auth_headers = get_auth_headers(as_uid=self.outer_admin['id'])
        response = self.get_json('/organizations/', headers=auth_headers)

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    label=self.organization['label'],
                    name=self.organization['name'],
                    domains=has_entries(
                        master=self.organization_info['domain']['name'],
                        display=self.organization_info['domain']['name'],
                    ),
                    services=contains(
                        self.service['slug']
                    ),
                    # поле revision, хоть и требует select_related запроса,
                    # но в v1 ручке должно отдаваться по-умолчанию:
                    # https://st.yandex-team.ru/DIR-3250
                    revision=self.organization['revision'],
                    partner_id=ANY,
                    subscription_plan_expires_at=ANY,
                    organization_type=ANY,
                    user_count=ANY,
                    vip=ANY,
                )
            )
        )

    def test_with_x_uid_no_organizations(self):
        # проверим, что для пользователя, которого нет в базе вернутся пустой список
        auth_headers = get_auth_headers(as_uid=123)
        response = self.get_json('/organizations/', headers=auth_headers)

        assert_that(
            response,
            empty()
        )

    def test_filter_by_type(self):
        response = self.get_json('/v6/organizations/?organization_type=cloud')
        self.assertEqual(len(response['result']), 0)

        response = self.get_json('/v6/organizations/?organization_type=common')
        self.assertEqual(len(response['result']), 1)

        response = self.get_json('/v6/organizations/?organization_type=cloud,common')
        self.assertEqual(len(response['result']), 1)

    def test_with_admin_only(self):
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        self.add_user_by_invite(second_organization, self.user['id'], domain='yandex.ru')

        # запросим сначала все организации пользователя
        response = self.get_json('/v11/organizations/?show=user')
        self.assertEqual(len(response['result']), 2)

        # затем запросим только те, где он админ - там не должно быть организации, которую мы создали выше
        response = self.get_json('/v11/organizations/?show=user&admin_only=true')
        self.assertEqual(len(response['result']), 1)
        self.assertEqual(response['result'][0]['id'], self.organization['id'])

    @override_settings(INTERNAL=False)
    @oauth_success(scopes=[scope.read_organization])
    def test_with_x_uid_in_the_header_external(self):
        # включаем сервис в организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )

        auth_headers = get_oauth_headers(as_uid=self.admin_uid)
        response = self.get_json('/organizations/', headers=auth_headers)

        assert_that(
            response,
            all_of(
                contains_inanyorder(
                    has_entries(
                        id=self.organization['id'],
                        label=self.organization['label'],
                        name=self.organization['name'],
                        domains=has_entries(
                            master=self.organization_info['domain']['name'],
                            display=self.organization_info['domain']['name'],
                        ),
                        services=contains(
                            self.service['slug']
                        ),
                        # поле revision, хоть и требует select_related запроса,
                        # но в v1 ручке должно отдаваться по-умолчанию:
                        # https://st.yandex-team.ru/DIR-3250
                        revision=self.organization['revision'],
                        user_count=ANY,
                    )
                ),
                not_(has_entries(
                    partner_id=ANY,
                    subscription_plan_expires_at=ANY,
                    organization_type=ANY,
                )),
            ),
        )

    def test_for_outer_admin(self):
        # тестируем работу ручки для внешнего админа у которого несколько организаций

        # создадим две организации
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        # и добавим одного и того же админа в них
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=first_organization['id'],
        )
        UserMetaModel(self.meta_connection).create(id=outer_admin['id'], org_id=second_organization['id'])

        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        del outer_admin_auth_headers['X-ORG-ID']

        # добавим сервис в первую организацию
        service_slug = 'slug'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='Autotest Service',
            client_id='kjkasjdkakds',
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=first_organization['id'],
            service_id=service['id'],
        )
        user_count_first = UserModel(self.main_connection).count({
            'org_id': first_organization['id'],
            'is_robot': False
        })
        user_count_second = UserModel(self.main_connection).count({
            'org_id': second_organization['id'],
            'is_robot': False
        })

        with catched_sql_queries() as queries:
            response = self.get_json('/organizations/', headers=outer_admin_auth_headers, process_tasks=False)

            # Здесь захардкожено количество запросов при отключенном
            # кэше для того, чтобы в случае внезапного увеличения числа
            # запросов, это не прошло незамеченным.
            #
            # Пожалуйста, не надо бездумно править этот счетчик, если
            # тест упал.

            # Счетчик увеличен, потому что добавлен запрос в метабазу для получения has_owned_domain
            # (DIR-4681)
            # Для каждой огранизации по одному запросу = +2 запроса
            # 8 + 2 = 10
            #
            # Ещё раз с 10 до 11 счетчик был увеличен, так как пришлось сделать
            # дополнительный запрос для определения сервиса который пришёл с
            # тикетом старого образца: https://st.yandex-team.ru/DIR-4952
            #
            # Счетчик уменьшен с 11 до 9, так как убран один begin_nested (DIR-5267)
            # и убрались два запроса: SAVEPOINT и RELEASE SAVEPOINT
            # +3 т.к. добавилось определение организации по умолчанию, что вызывает доп. загрузку инфы по ней
            # +8 проверка фичи USE_DOMENATOR
            assert_that(
                queries,
                has_length(19)
            )

        # проверим наличие организаций
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=first_organization['id'],
                    label=first_organization['label'],
                    name=first_organization['name'],
                    services=equal_to([service_slug]),
                    user_count=user_count_first,
                ),
                has_entries(
                    id=second_organization['id'],
                    label=second_organization['label'],
                    name=second_organization['name'],
                    services=equal_to([]),
                    user_count=user_count_second,
                ),
            )
        )

    def test_for_outer_admin_with_not_ready_domains(self):
        # тестируем работу ручки для внешнего админа у которого организации не готовы из-за ошибки миграции

        # создадим две организации
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google',
            ready=False,
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
            ready=False,
        )['organization']

        # и добавим одного и того же админа в них
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=first_organization['id'],
        )
        UserMetaModel(self.meta_connection).create(id=outer_admin['id'], org_id=second_organization['id'])

        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        del outer_admin_auth_headers['X-ORG-ID']

        response = self.get_json('/organizations/', headers=outer_admin_auth_headers)

        # проверим что список - пуст
        assert_that(response, empty())

    def test_with_user_and_org_id_in_the_header(self):
        auth_headers = get_auth_headers(as_outer_admin=self.outer_admin)
        response = self.get_json('/organizations/',
                                 headers=auth_headers)
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    label=self.organization['label'],
                    name=self.organization['name'],
                    domains=has_entries(
                        master=self.organization_info['domain']['name'],
                        display=self.organization_info['domain']['name'],
                    )
                )
            )
        )

    def test_with_user_not_in_organization_in_the_header(self):
        auth_headers = get_auth_headers(as_uid=1130000000219473)
        response = self.get_json('/organizations/',
                                 headers=auth_headers)
        assert_that(response, equal_to([]))

    def test_returns_head(self):
        # проверяем, что ручка /organizations/ возвращает
        # развернутое описание руководителя, если он есть

        # сначала укажем, кто Директор
        self.patch_json(
            '/organization/',
            {'head_id': self.user['id']},
            expected_code=200
        )

        auth_headers = get_auth_headers(as_uid=self.outer_admin['id'])
        response = self.get_json('/organizations/',
                                 headers=auth_headers)

        # а теперь проверим, что ручка его отдала в развернутом виде
        assert_that(
            response,
            contains(
                has_entries(
                    head=has_entries(
                        id=self.user['id'],
                    )
                )
            )
        )

    def test_returns_only_ready_true(self):
        # возращаются только организации с ready=True
        # всего две организации с ready=True и с ready=False
        not_ready_organization_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='not_ready',
            domain_part='not-migrated.com',
        )
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'ready': False},
            filter_data={'id': not_ready_organization_info['organization']['id']}
        )
        UserMetaModel(self.meta_connection).create(
            id=PASSPORT_TEST_OUTER_UID,
            org_id=not_ready_organization_info['organization']['id'],
        )

        auth_headers = get_auth_headers(as_outer_admin=self.outer_admin)
        response = self.get_json('/organizations/', headers=auth_headers)
        assert_that(
            response,
            has_length(1)
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_organizations(self):
        # получаем список организаций подключенных к сервису

        # создадим ещё одну организацию
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # подключим сервис в двух организациях
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=self.service['id'],
        )

        # авторизуемся обезличенным токеном  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        response = self.get_json('/organizations/', headers=oauth_headers)

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        self.service['slug']
                    )
                ),
                has_entries(
                    id=second_organization['id'],
                    services=contains(
                        self.service['slug']
                    )
                )
            )
        )

    @override_settings(INTERNAL=False)
    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_organizations_extenal_api(self):
        # получаем список организаций подключенных к сервису
        # для публичного апи

        # подключим сервис в двух организациях
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()

        # авторизуемся по OAuth
        oauth_headers = get_oauth_headers()
        response = self.get_json('/organizations/', headers=oauth_headers)

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        self.service['slug']
                    )
                ),
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_organizations_uses_caching(self):
        # Проверим, что при получении списка сервисов используется кэширование

        # создадим ещё одну организацию
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # подключим сервис в двух организациях
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=self.service['id'],
        )
        self.process_tasks()

        # авторизуемся обезличенным токеном  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        # Тут надо запатчить размер кэша, потому что для юнит-тестов по умолчанию
        # кэш отключен
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.services.services_cache._Cache__maxsize',
                   new=100), \
             patch('intranet.yandex_directory.src.yandex_directory.core.utils.organization_domains_cache._Cache__maxsize',
                   new=100):
            # Вычислим количество запросов в базу при
            # первом запросе, когда кэш пустой
            with catched_sql_queries() as queries1:
                self.get_json('/organizations/', headers=oauth_headers, process_tasks=False)
                num_queries1 = len(queries1)

            with catched_sql_queries() as queries2:
                response = self.get_json('/organizations/', headers=oauth_headers, process_tasks=False)
                num_queries2 = len(queries2)

                # Здесь захардкожено количество запросов при отключенном
                # кэше для того, чтобы в случае внезапного увеличения числа
                # запросов, это не прошло незамеченным.
                #
                # Пожалуйста, не надо бездумно править этот счетчик, если
                # тест упал.

                # Счетчик увеличен, потому что добавлен запрос в метабазу для получения has_owned_domain
                # (DIR-4681)
                # Для каждой огранизации по одному запросу = +2 запроса
                # 8 + 2 = 10
                #
                # Счетчик уменьшен с 10 до 8, так как убран один begin_nested (DIR-5267)
                # и убрались два запроса: SAVEPOINT и RELEASE SAVEPOINT
                # +8 проверка фичи USE_DOMENATOR
                assert_that(
                    num_queries1,
                    equal_to(16),
                )
                # Проверим, что при втором запросе был использован кэш
                assert_that(
                    num_queries1 - num_queries2,
                    equal_to(
                        # TODO: вообще тут теперь не должно быть разницы
                        # Сэкономили один запрос за счёт того, что список
                        # известных Директории сервисов закэшировался
                        1
                        # 6 запросов исчезают, потому что
                        # по 3 выполняется для получения списка сервисов каждой
                        # из организаций, а всего организаций две.
                        # 6 +
                        # И ещё по два запроса надо, чтобы достать домены
                        # организации - один на получение шарда, второй -
                        # за самими доменами.
                        # 4
                    )
                )

        # И результат тоже должен быть верный,
        # как и в предыдущем тесте.
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        self.service['slug']
                    )
                ),
                has_entries(
                    id=second_organization['id'],
                    services=contains(
                        self.service['slug']
                    )
                )
            )
        )

        # Зачистить кэш
        from intranet.yandex_directory.src.yandex_directory.core.utils.services import services_cache
        services_cache.clear()

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_has_no_organizations(self):
        # Проверим, что сервис получит пустой список, если
        # он не подключен ни в одной организации.

        # Сделаем вид, что сервис нигде не подключен
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)

        # Авторизуемся обезличенным токеном по OAuth.
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        response = self.get_json('/organizations/', headers=oauth_headers)
        # Список организаций должен быть пустым.
        assert_that(
            response,
            empty(),
        )


class TestOrganizations_v4___get(TestCase):
    """Эти тесты для ручки 4 версии, где мы добавили
    возможность пейджинации.
    """
    api_version = 'v4'

    def test_with_only_org_id_in_the_header(self):
        # Запрашиваем список организаций без передачи X-UID
        # или авторизации сервисом подключенным к организациям.
        auth_headers = get_auth_headers(as_org=self.organization['id'])
        response = self.get_json('/organizations/', headers=auth_headers)

        # В этом случае, результат должен быть пустой, и ссылок быть не должно
        assert_that(
            response,
            has_entries(
                result=empty(),
                links=empty(),
            )
        )

    def test_with_x_uid_in_the_header(self):
        # Случай, когда ручка дергается от лица внешнего админа

        auth_headers = get_auth_headers(as_uid=self.outer_admin['id'])
        response = self.get_json('/organizations/', headers=auth_headers)

        assert_that(
            response,
            has_entries(
                result=contains(
                    has_only_entries(
                        id=self.organization['id'],
                    )
                ),
                links=empty(),
            )
        )

    def test_for_outer_admin_with_few_organizations(self):
        # тестируем работу ручку для внешнего админа у которого несколько организаций

        # создадим две организации
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        # и добавим одного и того же админа в них
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=first_organization['id'],
        )
        UserMetaModel(self.meta_connection).create(id=outer_admin['id'], org_id=second_organization['id'])

        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        del outer_admin_auth_headers['X-ORG-ID']

        # Добавим сервис в первую организацию,
        # но это никак не должно отразиться на отдаваемых ручкой организациях,
        # так как они отдаются по пользователю а не по сервису
        OrganizationServiceModel(self.main_connection).create(
            org_id=first_organization['id'],
            service_id=self.service['id'],
        )
        self.process_tasks()

        with catched_sql_queries() as queries:
            response = self.get_json('/organizations/', headers=outer_admin_auth_headers, process_tasks=False)

            # Здесь захардкожено количество запросов при отключенном
            # кэше для того, чтобы в случае внезапного увеличения числа
            # запросов, это не прошло незамеченным.
            #
            # Пожалуйста, не надо бездумно править этот счетчик, если
            # тест упал.
            assert_that(
                queries,
                has_length(7)
            )

        # проверим наличие организаций
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_only_entries(
                        id=first_organization['id'],
                    ),
                    has_only_entries(
                        id=second_organization['id'],
                    ),
                ),
                links=empty(),
            )
        )

    def test_with_user_and_org_id_in_the_header(self):
        # В этом случае, должны отдаваться организации, к которым
        # привязан внешний админ
        auth_headers = get_auth_headers(as_outer_admin=self.outer_admin)
        response = self.get_json('/organizations/',
                                 headers=auth_headers)
        assert_that(
            response,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.organization['id'],
                    ),
                ),
                links=empty(),
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_organizations(self):
        # Проверим, что организации подключенные к сервису, будут отданы постранично

        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)

        # создадим ещё пару десятков организаций
        N = 20
        org_ids = []

        for i in range(N):
            organization = create_organization(
                self.meta_connection,
                self.main_connection,
                label='org-{}'.format(i)
            )['organization']

            org_id = organization['id']
            if org_id % 2 == 0:
                # подключим сервис в каждой чётной
                OrganizationServiceModel(self.main_connection).create(
                    org_id=org_id,
                    service_id=self.service['id'],
                )
                # ID надо запомнить, чтобы потом проверить выдачу
                org_ids.append(org_id)

        # Авторизуемся обезличенным токеном  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        response, response_headers = self.get_json(
            '/organizations/?per_page=5',
            headers=oauth_headers,
            return_headers=True,
        )

        # Первая страница должна содержать только 5 организаций и
        # ссылку на следующую страницу
        assert_that(
            response,
            has_entries(
                result=contains(
                    *(
                        has_entries(id=org_id)
                        for org_id in org_ids[:5]
                    )
                ),
                links=has_entries(
                    next=not_none()
                ),
            ),
        )

        # В ответе должна быть ссылка на следующую страницу.
        next_page = response['links']['next']

        assert_that(
            next_page,
            equal_to(
                '{}v4/organizations/?page=2&per_page=5&shard=1'.format(app.config['SITE_BASE_URI'])
            )
        )

        # Так же, в заголовках ответа, должен быть соотвествующий заголовок Link
        assert_that(
            response_headers,
            has_entries(
                Link='<{next_page}>; rel="next"'.format(next_page=next_page),
            )
        )

        response, response_headers = self.get_json(
            next_page,
            headers=oauth_headers,
            return_headers=True,
        )

        # Вторая страница должна содержать следующие 5 организаций
        assert_that(
            response,
            has_entries(
                result=contains(
                    *(
                        has_entries(id=org_id)
                        for org_id in org_ids[5:10]
                    )
                ),
                # Следующей страницы нет, так как мы подключили к сервису
                # лишь 10 организаций
                links=is_not(
                    has_key('next')
                ),
            ),
        )

        # Так как следующей страницы нет, то и заголовка Link быть не должно
        assert_that(
            response_headers,
            is_not(
                has_key('Link'),
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization,
                                            scope.work_with_any_organization])
    def test_all_organizations_if_service_can_work_with_any_organization(self):
        # Проверим, что если у сервиса есть скоуп work_with_any_organization,
        # то сервис может указать дополнительный параметр show=all
        # и получить вообще все организации которые есть в Коннекте

        # Отключим сервис от организаций
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)

        org_ids = [
            # одна организация создаётся внутри setUp
            self.organization['id']
        ]
        # создадим ещё десяток организаций
        N = 10

        for i in range(N):
            organization = create_organization(
                self.meta_connection,
                self.main_connection,
                label='org-{}'.format(i)
            )['organization']

            org_id = organization['id']
            # ID надо запомнить, чтобы потом проверить выдачу
            org_ids.append(org_id)

        # Авторизуемся обезличенным токеном  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        # При запросе укажем show=all, чтобы запросить все
        # организации
        response, response_headers = self.get_json(
            '/organizations/?per_page=5&show=all',
            headers=oauth_headers,
            return_headers=True,
        )

        # Первая страница должна содержать только 5 организаций и
        # ссылку на следующую страницу
        assert_that(
            response,
            has_entries(
                result=contains(
                    *(
                        has_entries(id=org_id)
                        for org_id in org_ids[:5]
                    )
                ),
            ),
        )

        # В ответе должна быть ссылка на следующую страницу.
        next_page = response['links']['next']
        # В ссылке на следующую страницу аргумент show=all
        # должен сохраниться, чтобы в результате прохода
        # по страницам можно было получить все организации.
        assert_that(
            next_page,
            equal_to(
                '{}v4/organizations/?page=2&per_page=5&shard=1&show=all'.format(app.config['SITE_BASE_URI'])
            )
        )

    def test_subscription_plan_is_returned(self):
        # Проверим, что можно запросить какой у организации тарифный
        # план и дисковый лимит.
        fields = ['subscription_plan', 'disk_usage', 'disk_limit']
        fields = ','.join(fields)

        response = self.get_json('/organizations/?fields=' + fields)
        assert_that(
            response,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.organization['id'],
                        subscription_plan='free',
                        disk_usage=0,
                        disk_limit=0,
                    )
                )
            )
        )

        # в платном режиме должен вернуться правильный лимит места и subscription_plan = paid
        self.enable_paid_mode()

        response = self.get_json('/organizations/?fields=' + fields)
        assert_that(
            response,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.organization['id'],
                        subscription_plan='paid',
                        disk_usage=0,
                        disk_limit=app.config['TEN_HUMAN_DISK_LIMIT'],
                    )
                )
            )
        )

    def test_private_fields(self):
        org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='test-org'
        )

        response = self.get_json('/organizations/')

        private_fields = constants.ORGANIZATION_PRIVATE_FIELDS + \
                         constants.ORGANIZATION_PRIVATE_FIELDS_V4

        for key in private_fields:
            assert_that(
                response['result'][0],
                is_not(has_key(key))
            )


class TestOrganizations_v6___get(TestCase):
    """Эти тесты для ручки 6 версии, где мы переименовали поле admin_uid в admin_id.
    """
    api_version = 'v6'

    def test_fields_returned_by_list_view(self):
        # Проверяем, что ручка возвращает все перечисленные поля
        # Если мы указываем admin_uid ручка не должна возвращать это поле,
        # пока мы не начнём возвращать ошибки из-за недоступных полей.
        fields = list(OrganizationModel.all_fields)

        for field in constants.ORGANIZATION_PRIVATE_FIELDS + constants.ORGANIZATION_PRIVATE_FIELDS_V4:
            fields.remove(field)

        headers = get_auth_headers()
        with mocked_blackbox() as blackbox:
            blackbox.hosted_domains.return_value = {"hosted_domains": [{'domain': 'test'}]}
            response = self.get_json(
                '/organizations/',
                headers=headers,
                query_string={
                    'fields': ','.join(map(str, fields))
                }
            )

        assert_that(
            list(response['result'][0].keys()),
            contains_inanyorder(*fields),
        )
        assert_that(
            response['result'][0],
            all_of(
                has_key('admin_id'),
                is_not(has_key('admin_uid')),
            )
        )

    def test_returns_has_owned_domains(self):
        # проверяем, что ручка /organizations/ возвращает
        # поле has_owned_domains

        DomainModel(self.main_connection).update(
            update_data={'master': False},
            filter_data={'org_id': self.organization['id']},
        )
        auth_headers = get_auth_headers(as_outer_admin=self.outer_admin)
        response = self.get_json(
            '/organizations/',
            headers=auth_headers,
            query_string={
                'fields': 'has_owned_domains',
            }
        )
        assert_that(
            response['result'][0],
            has_entries(
                has_owned_domains=False,
            ),
        )

    def test_responsible_can_be_requested(self):
        # Проверим, что если в полях запрошен ответственный,
        # то мы отдадим его id и имя

        # Включаем сервис в организации и делаем пользователя ответственным
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
            responsible_id=self.user['id'],
        )
        response = self.get_json('/organizations/?fields=services.responsible.name')
        assert_that(
            response['result'],
            contains(
                has_entries(
                    services=contains(
                        has_entries(
                            responsible=has_entries(
                                id=self.user['id'],
                                name=make_simple_strings(self.user['name']),
                            )
                        )
                    )
                )
            )
        )

    def test_returns_karma_and_ip(self):
        # Проверяем, что ручка /organizations/ не отдаёт поля про карму и ip.
        # Потому что смы не хотим делиться наружу такой информацией.

        auth_headers = get_auth_headers(as_outer_admin=self.outer_admin)
        response = self.get_json(
            '/organizations/?fields=karma,ip',
            headers=auth_headers,
            expected_code=422,
        )


class TestOrganization__change_default_uid(TestCase):
    def test_change_default_uid_to_user(self):
        # Проверим успешный сценарий, когда
        # в качестве default_uid можно задать uid пользователя
        patch_data = {
            'default_uid': self.user['id']
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )
        assert_called_once(
            self.mocked_passport.domain_edit,
            ANY,
            {'default': self.user['nickname']},
        )

    def test_reset_default_uid(self):
        # Проверим успешный сценарий, когда
        # мы сбрасываем в None
        patch_data = {
            'default_uid': None,
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )
        assert_called_once(
            self.mocked_passport.domain_edit,
            ANY,
            {'default': ''},
        )

    def test_change_to_user_from_other_org(self):
        # Проверим неуспешный сценарий, когда
        # мы пытаемся установить default_uid в uid сотрудника из другой организации
        domain = 'foo.bar.ru'
        other_org = self.create_organization(
            label='foo',
            domain_part='.bar.ru',
        )
        org_id = other_org['id']
        user = self.create_user(nickname='art', org_id=org_id, domain_part=domain)

        patch_data = {
            'default_uid': user['id'],
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=422,
            expected_error_code='user_not_found',
        )
        assert_not_called(self.mocked_passport.domain_edit)

    def test_change_to_portal_user(self):
        # Проверим неуспешный сценарий, когда
        # мы пытаемся установить default_uid в uid сотрудника из этой же организации
        # но сотрудник при этом – портальный.
        # Это не работает из-за ограничения в Паспорте. Сейчас он принимает только
        # логин сотрудника, в том же домене. Попытка передать портальный логин
        # проглатывается паспортом без ошибки, но смены default_uid не происходит.
        user = self.create_portal_user()

        patch_data = {
            'default_uid': user['id'],
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=422,
            expected_error_code='user_not_in_domain',
        )
        assert_not_called(self.mocked_passport.domain_edit)

    def test_change_without_domain(self):
        # Неуспешная попытка смены дефолтного ящика в организации без домена
        data = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        admin_uid = data['admin_user_uid']

        patch_data = {
            'default_uid': 12345,
        }

        response_data = self.patch_json(
            '/organization/',
            patch_data,
            headers=get_auth_headers(as_uid=admin_uid),
            expected_code=422,
            expected_error_code='no_domain',
        )
        assert_not_called(self.mocked_passport.domain_edit)


class TestOrganization__can_users_change_password(TestCase):
    def test_can_users_change_password(self):
        # Проверим успешный сценарий
        flag = False
        patch_data = {
            'can_users_change_password': flag
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )
        assert_called_once(
            self.mocked_passport.domain_edit,
            ANY,
            {'can_users_change_password': flag},
        )

    def test_change_without_domain(self):
        # Неуспешная попытка в организации без домена
        data = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        admin_uid = data['admin_user_uid']

        patch_data = {
            'can_users_change_password': True,
        }

        response_data = self.patch_json(
            '/organization/',
            patch_data,
            headers=get_auth_headers(as_uid=admin_uid),
            expected_code=422,
            expected_error_code='no_domain',
        )
        assert_not_called(self.mocked_passport.domain_edit)


class TestOrganization__patch(TestCase):
    def setUp(self):
        super(TestOrganization__patch, self).setUp()

        self.test_name = 'ТестовоеНазваниеОрганизации'
        self.org_label = 'test-org'
        self.first_user_nickname = 'first_user'
        self.domain = 'test-org{}'.format(app.config['DOMAIN_PART'])
        self.admin_uid = 10000000

    def test_patch_readonly_fields_is_prohibited(self):
        # Проверяем, что эти вот поля после создания организации
        # менять нельзя. В ответ должен приходить 400 код.
        data = {
            'domain': self.domain,
            'uid': 1000,
            'nickname': self.first_user_nickname,
            'first_name': 'yadir',
            'last_name': 'tester',
            'gender': 'male',
        }

        for key, value in list(data.items()):
            self.patch_json(
                '/organization/',
                {key: value},
                expected_code=422
            )

    def test_patch_with_long_name(self):
        # Проверяем, что нельзя задать слишком длинное имя для организации.

        self.patch_json(
            '/organization/',
            {'name': 'x' * 201},
            expected_code=422
        )

    def test_maybe_has_domain(self):
        assert_that(
            maybe_has_domain('Ваша электронная почта выиграла! Узнать подробнее о призе-"https://eisoz2.blogspot.com"'),
            equal_to(True)
        )
        assert_that(
            maybe_has_domain('Ваша электронная почта выиграла! Узнать подробнее о призе-http://xn----8sbenhfjdshrbio.xn--p1ai'),
            equal_to(True)
        )
        assert_that(
            maybe_has_domain('Ваш E-mail победил! Заберите приз - http://YOUNGGENIUS.RU'),
            equal_to(True),
        )

    def test_patch_with_spammy_name(self):
        # Проверяем, что нельзя задать слишком длинное имя для организации.
        assert_that(
            # реальная организация:
            # https://catalog.ws.yandex-team.ru/organizations/2746302
            is_spammy('Местная религиозная организация Церковь Христиан-Адвентистов Седьмого Дня "Радуга" г. Казани'),
            equal_to(False),
        )
        assert_that(
            is_spammy('вашпризhttp://snus24.su'),
            equal_to(True),
        )
        assert_that(
            is_spammy('ВыПобедилиВашПризНаСайтеwww.vk.com'),
            equal_to(True),
        )
        assert_that(
            is_spammy('ВАШ E-MAIL ПОБЕДИЛ! ПОЗДРАВЛЯЕМ ВАС! ПОЛУЧИТЬ ПРИЗ МОЖЕТЕ НА САЙТЕ-www.vk.cc/9eQlGx'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ВАШ_E-MAIL_ПОБЕДИЛ!_ВАШ_ПРИЗ_МОЖЕТЕ_НА_САЙТЕ-www.trafiklase.blogspot.com'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ВАШ E-MAIL ПОБЕДИЛ!'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ВаШ E-Mail П0БеДиЛ!'),
            # Такое спамом не считаем, потому что E-Mail разбивается
            # алгоритмом на два слова в каждом из которых только одна
            # заглавная буква
            equal_to(False)
        )
        assert_that(
            is_spammy('ВаШ E-MaiL П0БеДиЛ!'),
            equal_to(True)
        )
        assert_that(
            is_spammy('Ваш E-Mail Победил!'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ваш e-mail победил! поздравляем вас! получить приз можете на сайте-www.vk.cc/9eQlGx'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ваш e-mail победил! поздравляем вас! получить приз можете на сайте!'),
            equal_to(True)
        )
        # Одиночный домен это норм, потому что нет слов, которые на него завлекают.
        assert_that(
            is_spammy('dubovyjgaj.ru'),
            equal_to(False)
        )
        assert_that(
            is_spammy('my-domain-at.ru'),
            equal_to(False)
        )
        assert_that(
            is_spammy('ваш.email.победил.dubovyjgaj.ru'),
            equal_to(True)
        )
        assert_that(
            is_spammy('ваш-email-победил.dubovyjgaj.ru'),
            equal_to(True)
        )
        # Однако если ссылка ведёт через один из сокращателей, то не ОК
        assert_that(
            is_spammy('www.bit.ly/2OFmrer'),
            equal_to(True)
        )
        assert_that(
            is_spammy('www.vk.cc/9eQ'),
            equal_to(True)
        )
        assert_that(
            is_spammy('www.ya.cc/9eQ'),
            equal_to(True)
        )

    def test_patch_additional_fields(self):
        # проверяем, что ручка PATCH позволяет сменить название
        # организации и дополнительные поля
        # https://st.yandex-team.ru/DIR-1594

        data = {
            'name': 'name patched',
            # дополнительные поля
            'ogrn': 'ogrn patched',
            'inn': 'inn patched',
            'trc': 'trc patched',
            'corr_acc': 'corr_acc patched',
            'account': 'account patched',
            'law_address': 'law_address patched',
            'real_address': 'real_address patched',
            'head_id': self.user['id'],
            'phone_number': 'phone_number patched',
            'fax': 'fax patched',
            'email': 'email@patched.ru',
        }
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_id_from_blackbox') as domain_id:
            self.mocked_passport.set_organization_name.return_value = True
            domain_id.return_value = 923281

            org_id = self.organization['id']
            actions_before = ActionModel(self.main_connection).filter(org_id=org_id).all()

            for key, value in list(data.items()):
                # если что-то пойдет не так, то patch_json
                # получит не 200 и выкинет AssertionError
                response_data = self.patch_json(
                    '/organization/',
                    {key: value},
                    expected_code=200,
                )
                # руководителя организации мы должны
                # отдавать в раскрытом виде, в поле head
                if key == 'head_id':
                    assert_that(
                        response_data,
                        has_entries(
                            head=has_entries(
                                id=self.user['id']
                            )
                        )
                    )
                else:
                    assert_that(
                        response_data,
                        has_entries({key: value})
                    )

            actions_after = ActionModel(self.main_connection).filter(org_id=org_id).all()

            # При каждом изменении должен был быть сгенерён Action.
            assert_that(
                len(actions_after) - len(actions_before),
                equal_to(
                    len(data)
                )
            )

    @pytest.mark.skip('DIR-8881')
    def test_patch_organization_name(self):
        # проверяем, что при изменении имени организации мы меняем его и в паспорте, и в нашей базе
        # https://st.yandex-team.ru/DIR-1831

        patch_data = {'name': 'name patched'}
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_id_from_blackbox') as domain_id:
                self.mocked_passport.set_organization_name.return_value = True
                domain_id.return_value = 923281

                response_data = self.patch_json(
                    '/organization/',
                    patch_data,
                    expected_code=200
                )
                assert_that(
                    response_data,
                    has_entries(patch_data)
                )
                delay_set_name.assert_called_once()
                self.mocked_passport.set_organization_name.assert_called_once()

    def test_patch_organization_language(self):
        # изменяем язык организации

        # был "en"
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                language='ru'
            )
        )

        patch_data = {'language': 'en'}
        response_data = self.patch_json(
            '/organization/',
            data=patch_data,
        )

        # стал "en"
        assert_that(
            response_data,
            has_entries(patch_data)
        )
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                language='en'
            )
        )

    def test_patch_organization_country(self):
        # изменяем страну

        # по-умолчанию, страна не задана
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                country=None,
            )
        )

        patch_data = {'country': 'Ukrain'}
        response_data = self.patch_json(
            '/organization/',
            data=patch_data,
        )

        # стал "Ukrain"
        assert_that(
            response_data,
            has_entries(patch_data)
        )
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                country='Ukrain'
            )
        )

    def test_patch_organization_maillist_type(self):
        # изменяем тип подписки

        # по-умолчанию inbox
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                maillist_type='inbox',
            )
        )

        patch_data = {'maillist_type': 'both'}
        response_data = self.patch_json(
            '/organization/',
            data=patch_data,
        )

        # стал both
        assert_that(
            response_data,
            has_entries(patch_data)
        )
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(
                maillist_type='both'
            )
        )

    def test_failure_patch_if_empty_org_name(self):
        # проверяем, что возникает ошибка при попытке установить пустое название организации
        # и не происходит поход в паспорт

        self.mocked_passport.set_organization_name.return_value = False

        patch_data = {'name': None}
        self.patch_json(
            '/organization/',
            patch_data,
            expected_code=422
        )
        assert_not_called(self.mocked_passport.set_organization_name)

        patch_data = {'name': ''}
        self.patch_json(
            '/organization/',
            patch_data,
            expected_code=422
        )
        assert_not_called(self.mocked_passport.set_organization_name)

    def test_master_domain_should_not_change_if_passport_exception_was_raised(self):
        # Проверяем, что если паспорт вернул ошибку, мы ничего не сделаем и ответим 503
        new_master_domain = 'changed.yandex'

        # сделаем вид, что домен уже является алиасом
        DomainModel(self.main_connection).create(
            new_master_domain,
            self.organization['id'],
            owned=True,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.auth.user.get_permissions') as get_permissions, \
                mocked_blackbox() as blackbox:
            blackbox.hosted_domains.return_value = {
                "hosted_domains": [
                    {
                        "domain": "imap1.yandex.ru",
                        "domid": "1",
                        "master_domain": "",
                        "born_date": '2016-05-24 00:40:28',
                        "mx": "1",
                        "admin": self.admin_uid,
                    },
                    {
                        "domain": self.organization_domain,
                        "domid": "2",
                        "master_domain": "",
                        "born_date": '2016-05-24 00:40:28',
                        "mx": "1",
                        "admin": self.admin_uid,
                    },
                    {
                        "domain": new_master_domain,
                        "domid": "4",
                        "master_domain": self.organization_domain,
                        "born_date": '2016-05-24 00:40:28',
                        "mx": "1",
                        "admin": self.admin_uid,
                    },
                ]
            }

            get_permissions.return_value = [
                organization_permissions.edit,
                global_permissions.change_master_domain,
            ]
            self.mocked_passport.set_master_domain.side_effect = PassportUnavailable

            self.patch_json(
                '/organization/',
                {'master_domain': new_master_domain},
                expected_code=503,
            )

            # Проверим, что попробовали сменить домен в паспорте
            self.mocked_passport.set_master_domain.assert_called_once_with(
                ANY,  # master-domid
                ANY,  # new-master-domid
            )

    def test_having_not_permissions_change_master_domain(self):
        # если нет права change_master_domain, то 403
        new_master_domain = 'CHANGED.example.com'

        # сделаем вид, что домен уже является алиасом
        DomainModel(self.main_connection).create(
            new_master_domain.lower(),
            self.organization['id'],
            owned=True,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.auth.user.get_permissions') as get_permissions:
            get_permissions.return_value = [
                organization_permissions.edit,
            ]

            self.patch_json(
                '/organization/',
                {'master_domain': new_master_domain},
                expected_code=403
            )

    def test_having_permissions_user_can_change_master_domain(self):
        # Проверяем, что тот, у кого есть пермишшн change_master_domain,
        # может передать в patch поле domain, и это приведет к смене
        # отображаемого домена
        new_master_domain = 'CHANGED.example.com'
        new_master_domain_lower_case = new_master_domain.lower()

        # сделаем вид, что домен уже является алиасом
        DomainModel(self.main_connection).create(
            new_master_domain_lower_case,
            self.organization['id'],
            owned=True,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.auth.user.get_permissions') as get_permissions, \
                mocked_blackbox() as blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.core.actions.domain.event_domain_master_changed') as event:
            # немного нелогично, что мы мокаем событие event_domain_master_changed
            # внутри actions.organization, но так работает patch
            # если сделать иначе, то тест флапает

            get_permissions.return_value = [
                organization_permissions.edit,
                global_permissions.change_master_domain,
            ]
            self.mocked_passport.set_master_domain.return_value = True

            new_master_in_passp = {
                "domain": new_master_domain_lower_case,
                "domid": "2",
                "master_domain": self.organization_info['domain']['name'],
                "born_date": '2016-05-24 00:40:28',
                "mx": "1",
                "admin": self.admin_uid,
            }
            old_master_in_passp = {
                "domain": self.organization_info['domain']['name'],
                "domid": "3",
                "master_domain": "",
                "born_date": '2016-05-24 00:40:28',
                "mx": "1",
                "admin": self.admin_uid,
            }

            blackbox.hosted_domains.side_effect = [{
                "hosted_domains": [
                    old_master_in_passp,
                ]
            }, {
                "hosted_domains": [
                    new_master_in_passp,
                ]
            },
                {
                "hosted_domains": [
                    new_master_in_passp,
                ]
            }
            ]

            response = self.patch_json(
                '/organization/',
                {'master_domain': new_master_domain},
                expected_code=200
            )

            assert_that(
                response,
                has_entries(
                    domains=has_entries(
                        all=contains_inanyorder(
                            'not_yandex_test.ws.autotest.yandex.ru',
                            new_master_domain_lower_case,
                        ),
                        # отдаем вместо display master
                        # https://st.yandex-team.ru/DIR-1992
                        display=new_master_domain_lower_case,
                        master=new_master_domain_lower_case,
                    )
                )
            )

            # Проверим, что сменили домен в паспорте
            assert_that(
                self.mocked_passport.set_master_domain.call_count,
                equal_to(1)
            )

            # prepare_domain - пока не понимаю, почему prepare_domain не вызывается?
            # prepare_domain.assert_called_with(domain=u'changed.example.com')

            # проверим, что выстрелило событие domain_master_changed
            event.assert_called_with(
                ANY,
                object_type='domain',
                content={
                    'diff': {
                        'after': {
                            'owned': True,
                            'mx': False,
                            'name': 'changed.example.com',
                            'validated': False,
                            'delegated': False,
                            'org_id': self.organization['id'],
                            'display': True,
                            'master': True,
                            'via_webmaster': True,
                            'created_at': ANY,
                            'validated_at': None,
                            'tech': False,
                            'blocked_at': None,
                        },
                        'before': {
                            'owned': True,
                            'mx': False,
                            'name': 'not_yandex_test.ws.autotest.yandex.ru',
                            'validated': False,
                            'delegated': False,
                            'org_id': self.organization['id'],
                            'display': True,
                            'master': True,
                            'via_webmaster': True,
                            'created_at': ANY,
                            'validated_at': None,
                            'tech': True,
                            'blocked_at': None,
                        }
                    },
                    'directly': True
                },
                object_value={
                    'owned': True,
                    'mx': False,
                    'name': 'changed.example.com',
                    'validated': False,
                    'delegated': False,
                    'org_id': self.organization['id'],
                    'display': True,
                    'master': True,
                    'via_webmaster': True,
                    'created_at': ANY,
                    'validated_at': None,
                    'tech': False,
                    'blocked_at': None,
                },
                org_id=self.organization['id'],
                revision=self.organization['revision'] + 1,
                author_id=self.organization['admin_uid'],
            )

    def test_trying_to_change_master_domain_to_non_existent(self):
        # Проверяем, что если пытаться сменить домен на несуществующий, то будет ошибка
        new_master_domain = 'non-existent.example.com'

        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.permissions.get_permissions') as get_permissions:
            get_permissions.return_value = [
                global_permissions.change_master_domain,
            ]
            response = self.patch_json(
                '/organization/',
                {'display_domain': new_master_domain},
                expected_code=422
            )

            assert_that(
                response,
                has_entries(
                    code='domain_not_found',
                    message='Domain {domain} not found',
                    params={'domain': new_master_domain},
                )
            )

    def test_trying_to_change_master_domain_to_not_owned(self):
        # Проверяем, что если пытаться сменить домен на неподтвержденный, то будет ошибка
        new_domain = 'non-owned.example.com'
        DomainModel(self.main_connection).create(
            new_domain,
            self.organization['id'],
            owned=False
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.permissions.get_permissions') as get_permissions:
            get_permissions.return_value = [
                global_permissions.change_master_domain,
            ]
            response = self.patch_json(
                '/organization/',
                {'display_domain': new_domain},
                expected_code=422
            )

            assert_that(
                response,
                has_entries(
                    code='constraint_validation.not_owned_domain',
                    message='Can\'t make master not owned domain'
                )
            )

    def test_change_master_domain_and_update_all_emails_success(self):
        # Проверим, что в базе Директории все успешно поменялось
        # и у всех контейнеров поменялись email-ы

        new_master_domain = 'changed.example.com'

        # создадим разные виды контейнеров
        org_id = self.organization['id']
        domain = self.organization_domain

        u1 = self.create_user(nickname='user', org_id=org_id, domain_part=domain)
        g1 = self.create_group(label='group', org_id=org_id)
        ug = self.create_user(nickname='user_group', org_id=org_id, groups=[g1['id']], domain_part=domain)
        d1 = self.create_department(label='dep1', org_id=org_id)
        u1d = self.create_user(nickname='user1_dep', org_id=org_id, department_id=d1['id'], domain_part=domain)
        d2 = self.create_department(label='dep2', org_id=org_id, parent_id=d1['id'])
        u2d = self.create_user(nickname='user2_dep', org_id=org_id, department_id=d2['id'], domain_part=domain)
        u3d = self.create_user(nickname='user3_dep', org_id=org_id, department_id=d2['id'], domain_part=domain)

        # сделаем вид, что домен уже является алиасом
        DomainModel(self.main_connection).create(
            new_master_domain,
            self.organization['id'],
            owned=True,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.auth.user.get_permissions') as get_permissions:

            get_permissions.return_value = [
                organization_permissions.edit,
                global_permissions.change_master_domain,
            ]

            new_master_in_passp = {
                "domain": new_master_domain,
                "domid": "2",
                "master_domain": self.organization_info['domain']['name'],
                "born_date": '2016-05-24 00:40:28',
                "mx": "1",
                "admin": self.admin_uid,
            }
            old_master_in_passp = {
                "domain": self.organization_info['domain']['name'],
                "domid": "3",
                "master_domain": "",
                "born_date": '2016-05-24 00:40:28',
                "mx": "1",
                "admin": self.admin_uid,
            }

            self.mocked_blackbox.hosted_domains.side_effect = [{
                "hosted_domains": [
                    old_master_in_passp,
                ]
            }, {
                "hosted_domains": [
                    new_master_in_passp,
                ]
            },
                {
                "hosted_domains": [
                    new_master_in_passp,
                ]
            }
            ]

            response = self.patch_json(
                '/organization/',
                {'master_domain': new_master_domain},
                expected_code=200
            )

            assert_that(
                response,
                has_entries(
                    domains=has_entries(
                        all=contains_inanyorder(
                            'not_yandex_test.ws.autotest.yandex.ru',
                            new_master_domain,
                        ),
                        display=new_master_domain,
                        master=new_master_domain,
                    )
                )
            )

            # Проверим, что сменили домен в паспорте
            assert_called_once(
                self.mocked_passport.set_master_domain,
                old_master_in_passp['domid'],
                new_master_in_passp['domid']
            )

            # Проверим, что в базе Директории мастер поменялся
            changed_domain = DomainModel(self.main_connection).get(
                domain_name=new_master_domain,
                org_id=self.organization['id'],
            )
            assert changed_domain['master'] == True

            # Проверим, что все email-ы сменились и стали с новым мастером

            for item in [u1, ug, u1d, u2d, u3d]:
                user = UserModel(self.main_connection).get(org_id=org_id, user_id=item['id'])
                assert user['email'].endswith(new_master_domain) == True

            g = GroupModel(self.main_connection).get(org_id=org_id, group_id=g1['id'], fields=['*', 'email'])
            assert g['email'].endswith(new_master_domain) == True

            for item in [d1, d2]:
                d = DepartmentModel(self.main_connection).get(org_id=org_id, department_id=item['id'], fields=['email'])
                assert d['email'].endswith(new_master_domain) == True

    def test_patch_organization_with_enabled_service(self):
        # Проверяем, что если у организации есть подключенный сервис
        # то patch пройдёт без ошибок.
        # Потому что прежде была проблема с сериализацией данных
        # в ручке PATCH.
        # https://st.yandex-team.ru/DIR-3307

        # Подключим сервис к этой организации
        service_slug = 'test_service'
        service = ServiceModel(self.meta_connection).create(
            client_id='kjkasjdkakds',
            slug=service_slug,
            name='Autotest Service'
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )

        patch_data = {}
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )

    def test_patch_shared_contacts(self):
        # Проверяем, что можно менять настройку 'shared_contacts'

        patch_data = {
            'shared_contacts': True
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )
        assert_that(
            response_data,
            has_entries('shared_contacts', True)
        )

    def test_patch_shared_contacts_with_off_depended_service(self):
        # Проверяем, что можно менять настройку 'shared_contacts'
        # даже если она есть в зависимостях у сервиса, но это  сервис не подключен в организации

        deps = {
            Service(self.service['slug']): [
                Setting('shared-contacts', True)
            ],
        }

        patch_data = {
            'shared_contacts': True
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=deps):
            self.patch_json(
                '/organization/',
                patch_data,
                expected_code=200
            )

    def test_patch_shared_contacts_with_on_depended_service(self):
        # Проверяем, что  менять настройку 'shared_contacts' нельзя если есть
        # зависимый сервис, подключенный в организации

        service_slug = 'wiki'
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug=service_slug,
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service_slug,
        )

        deps = {
            Service(service_slug): [
                Setting('shared-contacts', True)
            ],
        }

        patch_data = {
            'shared_contacts': True
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=deps):
            response_data = self.patch_json(
                '/organization/',
                patch_data,
                expected_code=422
            )
        assert_that(
            response_data,
            has_entries(
                params=has_entries(
                    field='shared_contacts',
                    service=service_slug,
                )
            )
        )

    def test_patch_header(self):
        # Проверяем, что можно менять настройку 'header'
        self.clean_actions_and_events()
        patch_data = {
            'header': 'portal'
        }
        response_data = self.patch_json(
            '/organization/',
            patch_data,
            expected_code=200
        )
        assert_that(
            response_data,
            has_entries('header', 'portal')
        )
        assert_that(
            ActionModel(self.main_connection).all(),
            contains(
                has_entries('name', action.organization_modify)
            )
        )

    def test_patch_with_invalid_email(self):
        response_data = self.patch_json(
            '/organization/',
            {'email': 'invalid-email'},
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries('code', 'invalid_email')
        )

    def test_patch_with_valid_email(self):
        self.patch_json(
            '/organization/',
            {'email': 'validemail@yandex.ru'},
            expected_code=200
        )

    def test_patch_with_valid_email_with_plus(self):
        self.patch_json(
            '/organization/',
            {'email': 'valid+email@yandex.ru'},
            expected_code=200
        )

    def test_patch_org_type(self):
        # Проверяем, что можно менять тип организации
        self.clean_actions_and_events()
        patch_data = {
            'organization_type': 'cloud'
        }
        response_data = self.patch_json(
            '/v6/organization/',
            patch_data,
            expected_code=200
        )
        assert_that(
            response_data,
            has_entries('organization_type', 'cloud')
        )
        action1, action2 = ActionModel(self.main_connection).all()
        assert action1['name'] == action.organization_modify
        assert action2['name'] == action.organization_type_change

    def test_patch_cloud_org_id(self):
        # Проверяем, что можно менять тип организации
        self.clean_actions_and_events()
        patch_data = {
            'cloud_org_id': 'abcd'
        }
        response_data = self.patch_json(
            '/v6/organization/',
            patch_data,
            expected_code=200
        )
        assert_that(
            response_data,
            has_entries('cloud_org_id', 'abcd')
        )
        assert_that(
            ActionModel(self.main_connection).all(),
            contains(
                has_entries('name', action.organization_modify)
            )
        )
        org_meta = OrganizationMetaModel(self.meta_connection).get(id=self.organization['id'])
        assert org_meta['cloud_org_id'] == 'abcd'


class TestOrganizationWithoutDomain__patch(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestOrganizationWithoutDomain__patch, self).setUp()
        set_auth_uid(self.yandex_admin['id'])

    def test_can_edit_org_organization(self):
        # Проверяем, что для Яндекс орагнизаций можено менять информацию об организации
        data = {
            'name': 'name patched',
            # дополнительные поля
            'ogrn': 'ogrn patched',
            'inn': 'inn patched',
            'trc': 'trc patched',
            'corr_acc': 'corr_acc patched',
            'account': 'account patched',
            'law_address': 'law_address patched',
            'real_address': 'real_address patched',
            'phone_number': 'phone_number patched',
            'fax': 'fax patched',
            'email': 'email@patched.ru',
            'language': 'en',
            'country': 'Ukrain',
        }
        response = self.patch_json(
            '/organization/',
            data,
            expected_code=200
        )
        assert_that(
            response,
            has_entries(data)
        )

    def test_can_not_change_master_domain(self):
        # Нет прав редактировать мастер-домен (так как домена нет)
        self.patch_json(
            '/organization/',
            {'master_domain': 'new.domain.com'},
            expected_code=403,
            expected_error_code='forbidden',
            expected_message='Access denied',
        )

    def test_can_not_change_display_domain(self):
        # Нет прав редактировать отображаемый домен (так как домена нет)

        self.patch_json(
            '/organization/',
            {'display_domain': 'new.domain.com'},
            expected_code=422,
            expected_error_code='domain_not_found',
            expected_message='Domain {domain} not found',
        )


class TestOrganization__get_2(TestCase):
    def test_get_organizations_for_user_without_fields_arg(self):
        response = self.get_json('/v2/organizations/')

        assert_that(
            response,
            equal_to(
                [{'id': self.organization['id']}],
            )
        )

    def test_get_organizations_for_user_with_fields_arg(self):
        # включаем еще один сервис в организации
        service_slug = 'test_service'
        service = ServiceModel(self.meta_connection).create(
            client_id='kjkasjdkakds',
            slug=service_slug,
            name='Autotest Service'
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )

        response = self.get_json('/v2/organizations/?fields=id,revision,services')

        assert_that(
            response,
            equal_to(
                [
                    {
                        'id': self.organization['id'],
                        'revision': self.organization['revision'],
                        'services': [{
                            'slug': service_slug,
                            'ready': org_service['ready'],
                            'trial_expires': None,
                            'trial_expired': None,
                            'expires_at': None,
                            'user_limit': None,
                            'responsible': None,
                        }],
                    }
                ],
            )
        )

    def test_get_organizations_for_user_with_domains_field(self):
        response = self.get_json('/v2/organizations/?fields=id,revision,domains')

        assert_that(
            response,
            equal_to(
                [
                    {
                        'id': self.organization['id'],
                        'revision': self.organization['revision'],
                        'domains': {
                            'all': ['not_yandex_test.ws.autotest.yandex.ru'],
                            'display': 'not_yandex_test.ws.autotest.yandex.ru',
                            'master': 'not_yandex_test.ws.autotest.yandex.ru',
                            'owned': ['not_yandex_test.ws.autotest.yandex.ru'],
                        },
                    }
                ],
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_service_organizations(self):
        # получаем список организаций подключенных к сервису

        # создадим ещё одну организацию
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # подключим сервис в двух организациях
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=self.service['id'],
        )

        # авторизуемся обезличенным токеном  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        response = self.get_json('/v2/organizations/?fields=services,id', headers=oauth_headers)
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        has_entries(
                            slug=self.service['slug'],
                            ready=org_service['ready']
                        )
                    )
                ),
                has_entries(
                    id=second_organization['id'],
                    services=contains(
                        has_entries(
                            slug=self.service['slug'],
                            ready=org_service['ready']
                        )
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organizations_with_service_ready(self):
        # получаем список организаций в которых сервис готов/не готов
        # создадим ещё одну организацию
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # подключим сервис в двух организациях
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
            ready=True,  # сервис готов к использованию
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=self.service['id'],
            ready=False,  # сервис не готов к использованию
        )

        # авторизуемся сервисом  по OAuth
        set_auth_uid(None)
        oauth_headers = get_oauth_headers()

        # список организаций где сервис готов
        response = self.get_json('/v2/organizations/?fields=services,id&service.ready=True', headers=oauth_headers)
        assert_that(
            response,
            contains(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        has_entries(
                            ready=True,
                        )
                    )
                ),
            )
        )

        # список организаций где сервис не готов
        response = self.get_json('/v2/organizations/?fields=services,id&service.ready=False', headers=oauth_headers)
        assert_that(
            response,
            contains(
                has_entries(
                    id=second_organization['id'],
                    services=contains(
                        has_entries(
                            ready=False,
                        )
                    )
                ),
            )
        )

        # список организаций где есть сервис
        response = self.get_json('/v2/organizations/?fields=services,id', headers=oauth_headers)
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    id=self.organization['id'],
                    services=contains(
                        has_entries(
                            ready=True,
                        )
                    )
                ),
                has_entries(
                    id=second_organization['id'],
                    services=contains(
                        has_entries(
                            ready=False,
                        )
                    )
                ),
            )
        )


class TestOrganizationByOrgId__get_2(TestCase):
    def setUp(self):
        super(TestOrganizationByOrgId__get_2, self).setUp()
        # включаем сервис для всех методов
        self.org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        # и ходим везде с токеном
        set_auth_uid(None)
        self.oauth_headers = get_oauth_headers()

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_without_fields(self):
        # Просто проверяем, что отдается id, если не указаны доп. поля
        response = self.get_json('/v2/organizations/%s/' % self.organization['id'], headers=self.oauth_headers)
        assert_that(
            response,
            has_entries(
                id=self.organization['id'],

            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_for_disable_service(self):
        # проверяем, что если сервис не включен, то 403
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        response = self.get_json('/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
                                 headers=self.oauth_headers, expected_code=403)

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization,
                                            scope.work_with_any_organization])
    def test_service_can_work_with_any_organization(self):
        # проверяем, что даже если сервис не подключен, но имеет скоуп
        # позволяющий работать с любой организацией, то
        # он получит 200 ответ

        # Сначала отключим сервис от организаций
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)

        # Теперь сделаем запрос
        org_id = self.organization['id']
        response = self.get_json(
            '/v2/organizations/%s/' % org_id,
            headers=self.oauth_headers,
        )
        # И в ответ мы должны получить ответ про организацию
        assert_that(
            response,
            has_entries(
                id=org_id,
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_services_field(self):
        # включаем сервис в организации
        service_slug = 'my_service'
        service = ServiceModel(self.meta_connection).create(
            client_id='kjkasjdkakds',
            slug=service_slug,
            name='Autotest Service'
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        # проверяем, что сервис и ревизия отдаются
        response = self.get_json('/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                revision=self.organization['revision'],
                services=not_none(),
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_limited_services_field(self):
        # включаем сервис в организации
        service_slug = 'my_service'
        service = ServiceModel(self.meta_connection).create(
            client_id='kjkasjdkakds',
            slug=service_slug,
            name='Autotest Service',
            paid_by_license=True,
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        expires_at = datetime.datetime.utcnow().date()
        OrganizationServiceModel(self.main_connection) \
            .update_one(org_service['id'], {'expires_at': expires_at, 'user_limit': 100})
        # проверяем, что сервис и ревизия отдаются
        response = self.get_json('/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                revision=self.organization['revision'],
                services=not_none(),
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_tracker_service_field_and_tracker_is_not_ready(self):
        # включаем трекер в организации, но он еще не готов
        # поле trial_expires == None
        service = ServiceModel(self.meta_connection).create(
            client_id='svsdfewfwsd',
            slug=TRACKER_SERVICE_SLUG,
            name='Autotest Service'
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        # проверяем, что сервис отдаётся с нужными полями
        response = self.get_json(
            '/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=has_item(
                    has_entries(
                        slug=TRACKER_SERVICE_SLUG,
                        ready=org_service['ready'],
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_tracker_service_field_and_tracker_is_ready(self):
        # включаем трекер в организации и он готов сразу при включении
        # поле trial_expires == datetime...
        service = ServiceModel(self.meta_connection).create(
            client_id='svsdfewfwsd',
            slug=TRACKER_SERVICE_SLUG,
            name='Autotest Service',
            paid_by_license=True,
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
        )
        # проверяем, что сервис отдаётся
        response = self.get_json(
            '/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=has_item(
                    has_entries(
                        slug=TRACKER_SERVICE_SLUG,
                        ready=org_service['ready'],
                        trial_expires=format_date(org_service['trial_expires'], allow_none=True),
                        trial_expired=None,
                        expires_at=None,
                        user_limit=None,
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_tracker_service_trial(self):
        # включаем трекер в организации и он готов сразу при включении с триальным периодом
        service = ServiceModel(self.meta_connection).create(
            client_id='svsdfewfwsd',
            slug=TRACKER_SERVICE_SLUG,
            name='Autotest Service',
            paid_by_license=True,
            trial_period_months=1,
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
        )
        # проверяем, что сервис и ревизия отдаются, и триальный период еще не закончился
        response = self.get_json(
            '/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=has_item(
                    has_entries(
                        slug=TRACKER_SERVICE_SLUG,
                        ready=org_service['ready'],
                        trial_expires=org_service['trial_expires'].isoformat(),
                        trial_expired=False,
                        expires_at=None,
                        user_limit= None,
                    ),
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_disabled_services(self):
        # проверяем, что выключенные сервисы не отдаются
        service = ServiceModel(self.meta_connection).create(
            client_id='svsdfewfwsd',
            slug=TRACKER_SERVICE_SLUG,
            name='Autotest Service',
            paid_by_license=True,
            trial_period_months=1,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug']
        )

        # выклчюаем его
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
            'reason',
        )

        # проверяем, что отдается только включенный сервис self.service
        response = self.get_json(
            '/v2/organizations/%s/?fields=id,revision,services' % self.organization['id'],
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=contains(
                    has_entries(
                        slug=self.service['slug'],
                        ready=self.org_service['ready'],
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_domains_field(self):
        # проверяем, что домен и ревизия отдаются
        response = self.get_json('/v2/organizations/%s/?fields=id,revision,domains' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            equal_to(
                {
                    'id': self.organization['id'],
                    'revision': self.organization['revision'],
                    'domains': {
                        'all': ['not_yandex_test.ws.autotest.yandex.ru'],
                        'display': 'not_yandex_test.ws.autotest.yandex.ru',
                        'master': 'not_yandex_test.ws.autotest.yandex.ru',
                        'owned': ['not_yandex_test.ws.autotest.yandex.ru'],
                    },
                }
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[])
    def test_service_another_organization_without_scope(self):
        # не все права есть у сервиса, то возвращаем 403

        # создадим ещё одну организацию и проверим для нее
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # подключим сервис в этой организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=self.service['id'],
        )

        response = self.get_json('/v2/organizations/%s/?fields=services,id' % second_organization['id'],
                                 headers=self.oauth_headers, expected_code=403)

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_disabled_service(self):
        # включаем сервис в одной организации, но запрашиваем данные с org_id организации,
        # у которой этот сервис не включен
        service_slug = 'another_service'
        service = ServiceModel(self.meta_connection).create(
            client_id='kjkasjdkakds',
            slug=service_slug,
            name='Autotest Service'
        )
        org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        # создадим ещё одну организацию и проверим для нее
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_service'
        )['organization']

        # Проверяем, что данные про другую организацию не отдаются и мы возвращаем
        # 403 HTTP код
        self.get_json('/v2/organizations/%s/?fields=id,services' % second_organization['id'],
                      headers=self.oauth_headers, expected_code=403)

        # А для организации где сервис включён, всё отдаётся
        response = self.get_json('/v2/organizations/%s/?fields=id,services' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=contains(
                    has_entries(
                        slug=self.service['slug'],
                        ready=self.org_service['ready'],
                    ),
                    has_entries(
                        slug=service_slug,
                        ready=org_service['ready'],
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_without_id_field_directly(self):
        # Проверим, что будет отдавать id организации, даже если не задан ни один параметр в fields
        response = self.get_json('/v2/organizations/%s/' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            equal_to(
                {
                    'id': self.organization['id'],
                }
            )
        )

        # Проверим, что будет отдавать id организации, даже если не указано напрямую поле id в fields
        response = self.get_json('/v2/organizations/%s/?fields=revision,domains' % self.organization['id'],
                                 headers=self.oauth_headers)

        assert_that(
            response,
            equal_to(
                {
                    'id': self.organization['id'],
                    'revision': self.organization['revision'],
                    'domains': {
                        'all': ['not_yandex_test.ws.autotest.yandex.ru'],
                        'display': 'not_yandex_test.ws.autotest.yandex.ru',
                        'master': 'not_yandex_test.ws.autotest.yandex.ru',
                        'owned': ['not_yandex_test.ws.autotest.yandex.ru'],
                    },
                }
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    @override_settings(INTERNAL=False)
    def test_get_internal_services(self):
        from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import \
            Command as UpdateServicesInShardsCommand

        internal_service = ServiceModel(self.meta_connection).create(
            slug='internal-service',
            name='Внутренний сервис',
            client_id='client_id_internal',
            internal=True,
        )
        outer_service = ServiceModel(self.meta_connection).create(
            slug='outer-service',
            name='Внешний сервис',
            client_id='client_id_outer',
            internal=False,
        )

        internal_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=internal_service['id'],
        )
        outer_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=outer_service['id'],
        )

        # Обновляем сервисы на шарде.
        UpdateServicesInShardsCommand().try_run()

        response = self.get_json('/v2/organizations/%s/?fields=id,services' % self.organization['id'],
                                 headers=self.oauth_headers
                                 )
        assert_that(
            response['services'],
            has_item(
                has_entries(
                    ready=outer_service['ready'],
                    slug='outer-service',
                ),
            )
        )

        assert_that(
            response['services'],
            is_not(
                has_item(
                    {
                        'ready': internal_service['ready'],
                        'slug': 'internal-service',
                    }
                )
            )
        )


class TestOrganizationByOrgId__get_6(TestCase):
    api_version = 'v6'

    def test_fields_returned_by_detail_view(self):
        # Проверяем, что ручка возвращает все перечисленные поля
        # Если мы указываем admin_uid ручка не должна возвращать это поле,
        # пока мы не начнём возвращать ошибки из-за недоступных полей.
        fields = list(OrganizationModel.all_fields)

        for field in constants.ORGANIZATION_PRIVATE_FIELDS + constants.ORGANIZATION_PRIVATE_FIELDS_V4:
            fields.remove(field)

        headers = get_auth_headers()
        response = self.get_json(
            '/organizations/{organization_id}/'.format(organization_id=self.organization['id']),
            headers=headers,
            query_string={
                'fields': ','.join(map(str, fields))
            }
        )

        assert_that(
            list(response.keys()),
            contains_inanyorder(*fields),
        )
        assert_that(
            response,
            all_of(
                has_key('admin_id'),
                is_not(has_key('admin_uid')),
            )
        )

    def test_get_passport_fields(self):
        # проверям, что в полях default_uid и can_users_change_password данные из паспорта
        self.mocked_blackbox.hosted_domains.return_value = {
            "hosted_domains": [{
                'default_uid': 123456,
                'options': '{"organization_name": "org_id:278", "can_users_change_password": "1"}',
            }],
        }

        response = self.get_json(
            '/organizations/{organization_id}/?fields=default_uid,can_users_change_password'.format(
                organization_id=self.organization['id']
            ),
        )
        assert_that(
            response,
            has_entries(
                default_uid=123456,
                can_users_change_password=True
            )
        )

    def test_detailed_view_with_incorrect_org_id(self):
        # Проверяем, что возвращается 404, если организации нет в базе
        headers = get_auth_headers()
        org_id = OrganizationMetaModel(self.meta_connection).get_max_org_id() + 100
        response = self.get_json(
            '/organizations/{}/'.format(org_id),
            headers=headers,
            expected_code=404,
        )
        assert_that(
            response,
            equal_to({
                'message': 'Unknown organization',
                'code': 'unknown_organization',
            })
        )

        org_id -= 110
        response = self.get_json(
            '/organizations/{}/'.format(org_id),
            headers=headers,
            expected_code=404,
        )
        assert_that(
            response,
            equal_to({
                'message': 'Organization was deleted',
                'code': 'organization_deleted',
            })
        )

    def test_not_ready_organization(self):
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'ready': False},
            filter_data={'id': self.organization['id']}
        )
        response = self.get_json(
            '/organizations/{organization_id}/'.format(organization_id=self.organization['id']),
            expected_code=403
        )
        assert_that(
            response,
            has_entries('code', 'not-ready')
        )

    def test_service_responsible(self):
        # Проверим, что если у сервиса задан ответственный, то запросив
        # поле с его именем, мы получим его в ответе
        org_id = self.organization['id']
        OrganizationServiceModel(self.main_connection).create(
            org_id=org_id,
            service_id=self.service['id'],
            responsible_id=self.user['id'],
        )
        response = self.get_json('/organizations/{org_id}/?fields=services.responsible.name'.format(org_id=org_id))
        assert_that(
            response,
            has_entries(
                services=contains(
                    has_entries(
                        responsible=has_entries(
                            id=self.user['id'],
                            name=make_simple_strings(self.user['name']),
                        )
                )
                )
            )
        )



@contextmanager
def mocked_mds_api(status_code=200, status='ok', description=None):
    if description is None:
        description = ""

    api = Mock()
    result = Mock()

    return_value = {'status': status, 'description': description}
    if status_code == 200:
        return_value['sizes'] = {'orig': {'path': 'some-path'}}

    result.json.return_value = return_value
    result.status_code = status_code
    api.post = Mock(return_value=result)
    api.get = Mock(return_value=result)

    with patch('intranet.yandex_directory.src.yandex_directory.app.requests', api):
        yield api


class TestOrganizationsChangeLogoView__post(TestCase):
    def setUp(self):
        super(TestOrganizationsChangeLogoView__post, self).setUp()
        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)
        # Чтобы работала смена логотипа, организация должна быть
        # на платном тарифе.
        self.enable_paid_mode()

    def test_change_logo_by_file(self):
        # Меняем лого, передавая файл с картинкой

        with mocked_mds_api() as mds_api:
            filesdict = FileMultiDict()
            file_path = source_path(
                'intranet/yandex_directory/tests/unit/yandex_directory/passport/data/scream.jpg'
            )
            img = open(file_path, 'rb')
            filesdict.add_file('logo_file', img, filename=file_path)
            file_img = filesdict.get('logo_file')

            org_id = self.organization['id']
            actions_before = ActionModel(self.main_connection).filter(org_id=org_id).all()

            response = self.post_form_data(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_file': file_img
                },
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            assert_that(response, equal_to({}))
            assert_that(mds_api.post.call_count, equal_to(1))

            organization = OrganizationModel(self.main_connection).get(
                id=self.organization['id'],
                fields=['logo'],
            )

            assert_that(
                organization['logo'],
                not_none(),
            )

            actions_after = ActionModel(self.main_connection).filter(org_id=org_id).all()

            # При изменении logo должен был быть сгенерён Action.
            assert_that(
                len(actions_after) - len(actions_before),
                equal_to(1)
            )

    def test_change_logo_by_url(self):
        # Меняем лого, передавая url с картинкой
        img_url = 'ya.ru/pic.img'

        with mocked_mds_api() as mds_api:
            self.post_json(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_url': img_url
                },
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            self.assertEqual(mds_api.get.call_count, 1)

        organization = OrganizationModel(self.main_connection).get(
            id=self.organization['id'],
            fields=['logo'],
        )

        assert_that(
            organization['logo'],
            not_none(),
        )

    def test_error_required_logo_field(self):
        # Проверяем, что если не передали logo_file, то возвращается соотв. ошибка про поля, а не 500
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/passport/data/scream.jpg'
        )
        img = open(file_path, 'rb')
        filesdict.add_file('logo_field', img, filename=file_path)
        file_img = filesdict.get('logo_field')

        with mocked_mds_api() as mds_api:
            data = self.post_form_data(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_not_file_field': file_img
                },
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            assert_that(
                data,
                has_entries(
                    code='required_field',
                    message='Please, provide field "{field}"',
                    params={'field': 'logo_file'},
                )
            )
            self.assertEqual(mds_api.post.call_count, 0)

    def test_error_required_logo_url(self):
        # Проверяем, что если не передали logo_url, то возвращается соотв. ошибка про поля, а не 500
        img_url = 'ya.ru/pic.img'

        with mocked_mds_api() as mds_api:
            data = self.post_json(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_not_url': img_url
                },
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            assert_that(
                data,
                has_entries(
                    code='required_field',
                    message='Please, provide field "{field}"',
                    params={'field': 'logo_url'},
                )
            )
            self.assertEqual(mds_api.get.call_count, 0)

    def test_actions_and_events(self):
        # Проверяем, что вызывались нужные действия и события.
        self.clean_actions_and_events()

        with mocked_mds_api() as mds_api:
            img_url = 'ya.ru/pic.img'

            self.post_json(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_url': img_url
                },
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            self.assertEqual(mds_api.get.call_count, 1)

        actions = ActionModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
            },
        )
        actions = [x['name'] for x in actions]

        event_model = EventModel(self.main_connection)
        events = [x['name'] for x in event_model.find()]
        assert_that(
            actions,
            contains('organization_logo_change')
        )
        assert_that(
            events,
            empty(),
            # Событие про смену лого пока не генерим,
            # потому что не понятно, надо ли это кому-то.
            # contains('organization_logo_changed')
        )

    def test_mds_errors(self):
        # Проверяем, что обрабатывются ошибки со стороны mds.
        # Например, слишком маленькое изображение или неправильная ссылка.

        # Туплы содержат (ошибку от MDS, ожидаемый код ответа Директории)
        ERRORS = (
            ('Image is too small', 'mds.image_too_small', 'Image is too small.'),
            ('The image is found in blacklist', 'mds.image_blacklisted', 'Image was found in the blacklist.'),
            ('incorrect path', 'mds.url_invalid', 'Incorrect URL format'),
            ('bad arity', 'mds.url_invalid', 'Incorrect URL format'),
            ('blah minor', 'mds.unknown_error', 'Unknown API error.'),
        )

        for err_from_mds, expected_text_code, expected_message in ERRORS:
            with mocked_mds_api(status_code=400,
                                status='error',
                                description=err_from_mds):
                response = self.post_json(
                    '/organizations/%s/change-logo/' % self.organization['id'],
                    data={
                        'logo_url': 'ya.ru/pic.img'
                    },
                    expected_code=400,
                    headers=get_auth_headers(as_uid=self.admin_user['id']),
                )
                assert_that(
                    response,
                    has_entries(
                        code=expected_text_code,
                        message=expected_message,
                    )
                )

    def test_unknown_mds_error(self):
        # Проверяем, что возвращается 400 при иных ошибках в mds.
        with mocked_mds_api(status_code=401,
                            status='error',
                            description='Some shit happened'):
            response = self.post_json(
                '/organizations/%s/change-logo/' % self.organization['id'],
                data={
                    'logo_url': 'ya.ru/pic.img'
                },
                expected_code=400,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            assert_that(
                response,
                has_entries(
                    code='mds.unknown_error',
                    message='Unknown API error.',
                )
            )


class TestOrganizationsChangeLogoView__delete(TestCase):
    def setUp(self):
        super(TestOrganizationsChangeLogoView__delete, self).setUp()
        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)
        self.enable_paid_mode()

    def test_delete_logo(self):
        # Добавляем лого для организации, передавая файл с картинкой
        org_id = self.organization['id']

        image = ImageModel(self.main_connection).create(org_id, meta={'some': 'meta'})
        update_data = {'logo_id': image['id']}
        filter_data = {'id': org_id}
        OrganizationModel(self.main_connection).update(update_data=update_data, filter_data=filter_data)

        organization = OrganizationModel(self.main_connection).get(
            id=self.organization['id'],
            fields=['logo_id'],
        )
        assert_that(
            organization['logo_id'],
            not_none(),
        )

        # Удаляем лого.
        self.delete_json(
            '/organizations/%s/change-logo/' % self.organization['id'],
            headers=get_auth_headers(as_uid=self.admin_user['id']),
            expected_code=200
        )

        organization = OrganizationModel(self.main_connection).get(
            id=self.organization['id'],
            fields=['logo_id'],
        )

        assert_that(
            organization['logo_id'],
            none(),
        )


class TestOrganizationChangeSubscriptionPlanView(TestCase):
    valid_natural_person_data = {
        'person_type': 'natural',
        'subscription_plan': 'paid',
        'first_name': 'Alexander',
        'last_name': 'Akhmetov',
        'middle_name': 'R',
        'email': 'akhmetov@yandex-team.ru',
        'phone': '+79160000000',
    }

    valid_legal_person_data = {
        'subscription_plan': 'paid',
        'person_type': 'legal',
        'long_name': 'ООО Яндекс',
        'postal_code': '119021',
        'postal_address': 'Москва, Льва Толстого 18Б',
        'legal_address': 'Москва, Льва Толстого 16',
        'inn': '666',
        'kpp': '777',
        'bik': '888',
        'account': '999',
        'phone': '+79160000000',
        'email': 'akhmetov@yandex-team.ru',
    }

    def setUp(self):
        super(TestOrganizationChangeSubscriptionPlanView, self).setUp()

        # Так как смена тарифного плана доступна только организациям без фичи,
        # а она у нас с начала июня включается автоматически, то
        # нужно её предварительно отключить.
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            CAN_WORK_WITHOUT_OWNED_DOMAIN,
            False
        )

    def test_enabling_paid_mode_for_natural_person_with_outer_admin(self):
        # проверяем включение платности для физических лиц при отсутствии прав доступа
        # внешний админ не должен иметь возможности изменить тарифный план
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=self.organization['id'],
        )
        auth_headers = get_auth_headers(as_outer_admin={'id': outer_admin['id'], 'org_id': self.organization['id']})
        response = self.post_json(
            '/subscription/change/',
            data=self.valid_natural_person_data,
            expected_code=403,
            headers=auth_headers,
        )

        exp_response = {
            'code': 'forbidden',
            'message': 'Access denied',
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_for_natural_person_with_simple_user(self):
        # проверяем включение платности для физических лиц при отсутствии прав доступа
        # обычный пользователь не должен иметь возможности изменить тарифный план
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        simple_user = self.create_user(
            nickname='simple_user',
            org_id=self.organization['id'],
        )

        response = self.post_json(
            '/subscription/change/',
            data=self.valid_natural_person_data,
            expected_code=403,
            headers=get_auth_headers(as_uid=simple_user['id']),
        )

        exp_response = {
            'code': 'forbidden',
            'message': 'Access denied',
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_for_natural_person(self):
        # проверяем включение платности для физических лиц
        person_id = 500
        client_id = 999

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_natural_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

        # отправляем письма всем админам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['PAID_MODE_ENABLE_EMAIL'],
        )

    def test_enabling_paid_mode_for_natural_person_with_contract(self):
        # проверяем, что повторное включение платности вернет 422,
        # если по какой-то причине данные об организации есть в Биллинге, но пользователь ввел их еще раз
        self.enable_paid_mode()
        self.disable_paid_mode()

        # для организации сохранена запись в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        # попытаемся снова включить платный режим и передать информацию о плательщике
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_natural_person_data,
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'message': 'Organization already has contract',
                'code': 'organization_already_has_contract',
            })
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

        # при отключении нет письма админам
        assert_not_called(mock_send_email)

    def test_enabling_paid_mode_for_natural_person_with_not_initiated_in_billing_organization(self):
        # Проверяем включение платности для физических лиц для организации, которая
        # НЕ заведена в биллинге. Мы должны вернуть ошибку organization_is_without_contract

        # проверим, что организация в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # для организации нет записи в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(
                self.main_connection).count({
                'org_id': self.organization['id']
            }
            ),
            equal_to(0),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        # включаем платный режим
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data={'subscription_plan': 'paid'},
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'message': 'Organization is without contract',
                'code': 'organization_is_without_contract',
            })
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

        # нет письма админам
        assert_not_called(mock_send_email)

    def test_enabling_paid_mode_for_natural_person_with_initiated_in_billing_organization(self):
        # Проверяем включение платности для физических лиц для организации, которая
        # уже заведена в биллинге. Для неё в ручку достаточно передать:
        #
        # {
        #   "subscription_plan": "paid"
        # }

        # включим и выключим платный режим
        # после этого для организации сохранится инфомация о клиенте в биллинге
        self.enable_paid_mode()
        self.disable_paid_mode()

        # для организации сохранена запись в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '10',
            'ReceiptSum': '200',
        }]

        # включаем платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data={'subscription_plan': 'paid'},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'subscription_plan': 'paid',
            }),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

        # отправляем письма всем админам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['PAID_MODE_ENABLE_EMAIL'],
        )

    def test_enabling_paid_mode_for_initiated_in_billing_organization_with_unpaid_act(self):
        # Проверяем, что включение платности возможно для организаций,
        # у которых есть неоплаченный акт, но еще есть время на оплату

        # включим и выключим платный режим
        # после этого для организации сохранится инфомация о клиенте в биллинге
        self.enable_paid_mode()
        self.disable_paid_mode()

        # для организации сохранена запись в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123

        mocked_xmlrpc = Mock()
        first_debt_act_date = utcnow() - datetime.timedelta(days=20)
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '10',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        # включаем платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data={'subscription_plan': 'paid'},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'subscription_plan': 'paid',
            }),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

        # отправляем письма всем админам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['PAID_MODE_ENABLE_EMAIL'],
        )

    def test_enabling_paid_mode_for_initiated_in_billing_organization_with_debt(self):
        # Проверяем, что включение платности для организации с задолженостью вернет 402

        # включим и выключим платный режим
        # после этого для организации сохранится инфомация о клиенте в биллинге
        self.enable_paid_mode()
        self.disable_paid_mode()

        # для организации сохранена запись в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123
        first_debt_act_date = utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] + 1)

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '10',
            'ReceiptSum': '5',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        # пытаемся включить платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data={'subscription_plan': 'paid'},
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'message': 'Organization has debt',
                'code': 'organization_has_debt',
            })
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

        # нет письма админам
        assert_not_called(mock_send_email)

    def test_enabling_paid_mode_for_legal_person(self):
        # проверяем включение платности для юридических лиц
        person_id = 333
        client_id = 444

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_legal_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'legal',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

        # отправляем письма всем админам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['PAID_MODE_ENABLE_EMAIL'],
        )

    def test_enabling_paid_mode_for_legal_person_without_kpp(self):
        # проверяем включение платности для физических лиц без КПП (разрешено ИП)
        person_id = 333
        client_id = 444

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        data = self.valid_legal_person_data.copy()
        data['kpp'] = None

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'legal',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(self.admin_uid),
            {
                'type': 'ur',
                'email': data['email'],
                'postcode': data['postal_code'],
                'inn': data['inn'],
                'phone': data['phone'],
                'postaddress': data['postal_address'],
                'name': self.organization['name']['ru'],
                'legaladdress': data['legal_address'],
                'account': data['account'],
                'longname': data['long_name'],
                'client_id': client_id,
                'bik': data['bik'],
                'kpp': '',
            }
        )

        # отправляем письма всем админам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['PAID_MODE_ENABLE_EMAIL'],
        )

    def test_disabling_paid_mode_with_simple_user(self):
        self.enable_paid_mode()
        # обычный пользователь не может выключить платный режим
        simple_user = self.create_user(
            nickname='simple_user',
            org_id=self.organization['id'],
        )

        self.post_json(
            '/subscription/change/',
            data={'subscription_plan': 'free'},
            expected_code=403,
            headers=get_auth_headers(as_uid=simple_user['id']),
        )

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

    def test_disabling_paid_mode_with_outer_admin(self):
        # внешний админ не может выключить платный режим
        self.enable_paid_mode()
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=self.organization['id'],
        )
        auth_headers = get_auth_headers(as_outer_admin={'id': outer_admin['id'], 'org_id': self.organization['id']})
        self.post_json(
            '/subscription/change/',
            data={'subscription_plan': 'free'},
            headers=auth_headers,
            expected_code=403,
        )
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

    def test_disabling_paid_mode(self):
        # проверяем, что при выключении платного режима администратором организации,
        # он выключится и сохранятся все потребленные услуги за все необходимые дни
        # включаем платный режим в организации
        self.enable_paid_mode(subscription_plan_changed_at=utcnow() - datetime.timedelta(days=5))

        # создадим 5 тестовых пользователей за прошедшие 5 дней
        # отсчитывать время надо от "вчера" так чтобы не было пользователей, заведённых "сегодня"
        created_before = date_to_datetime(utcnow().date())

        for i in range(5):
            user = self.create_user(org_id=self.organization['id'])
            # апдейтим время создания пользователей в базе, чтобы сделать вид, будто они завелись дни назад
            self.main_connection.execute(
                mogrify(
                    self.main_connection,
                    query='UPDATE users SET created=%(created)s WHERE id=%(id)s',
                    vars={
                        'id': user['id'],
                        'created': created_before - datetime.timedelta(days=i),
                    }
                )
            )

        # А так же, 5 тестовых пользователей за сегодня,
        # они не должны быть учтены при подсчёте биллинга
        created_after = date_to_datetime(utcnow().date())
        interval = (utcnow() - created_after).total_seconds() / 5

        for i in range(5):
            user = self.create_user(org_id=self.organization['id'])
            # апдейтим время создания пользователей в базе, чтобы сделать вид, будто они завелись дни назад
            self.main_connection.execute(
                mogrify(
                    self.main_connection,
                    query='UPDATE users SET created=%(created)s WHERE id=%(id)s',
                    vars={
                        'id': user['id'],
                        'created': created_after + datetime.timedelta(seconds=interval * (i + 1)),
                    }
                )
            )

        # проверим, что нет данных о потребленных услугах
        consumed_info_count = OrganizationBillingConsumedInfoModel(self.main_connection).count(
            filter_data={'org_id': self.organization['id']}
        )
        assert_that(consumed_info_count, equal_to(0))

        # выключаем платный режим
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data={'subscription_plan': 'free'},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({'subscription_plan': 'free'}),
        )

        consumed_info_count = OrganizationBillingConsumedInfoModel(self.main_connection).count(
            filter_data={'org_id': self.organization['id']},
        )
        assert_that(consumed_info_count, equal_to(5))

        # при отключении нет письма админам
        assert_not_called(mock_send_email)

    def test_enabling_paid_mode_raise_exception_if_incorrect_email(self):
        # проверяем, что вернется 422, если неверно указано поле email
        client_id = 999

        invalid_natural_person_data = copy(self.valid_natural_person_data)
        invalid_natural_person_data['email'] = 'akhmetov'
        fault_msg = """
        <error><msg>Email address "akhmetov" is invalid</msg><email>akhmetov</email>
        <wo-rollback>0</wo-rollback><method>Balance.CreatePerson</method><code>WRONG_EMAIL</code></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=invalid_natural_person_data,
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'code': 'invalid_email',
            'message': 'Field "{field}" has invalid value',
            'params': {'field': 'email'}
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_raise_exception_if_incorrect_inn(self):
        # проверяем, что вернется 422, если неверно указано поле ИНН
        client_id = 999

        invalid_legal_person_data = copy(self.valid_legal_person_data)
        invalid_legal_person_data['inn'] = '123'
        fault_msg = """
        <error><msg>Invalid INN for ur or ua person</msg><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>INVALID_INN</code><parent-codes>
        <code>INVALID_PARAM</code><code>EXCEPTION</code></parent-codes>
        <contents>Invalid INN for ur or ua person</contents></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=invalid_legal_person_data,
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'code': 'invalid_inn',
            'message': 'Field "{field}" has invalid value',
            'params': {'field': 'inn'}
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_raise_exception_if_passport_not_found(self):
        # проверяем, что вернется 404, если биллинг вернул ошибку отсутствия uid при создании клиента

        fault_msg = """
        <error><msg>Passport with ID 1130000000621392 not found in DB</msg>
        <wo-rollback>0</wo-rollback><code>2</code><object-id>1130000000621392</object-id><object >
        </object><method>Balance.CreateClient</method><code>PASSPORT_NOT_FOUND</code><parent-codes>
        <code>NOT_FOUND</code><code>EXCEPTION</code></parent-codes><contents>Passport with ID 1130000000621392
        not found in DB</contents></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.side_effect = Fault(-1, fault_msg)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_natural_person_data,
                expected_code=404,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'code': 'user_uid_not_found_in_passport',
            'message': 'Passport account with ID "{uid}" not found',
            'params': {'uid': '1130000000621392'},
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_raise_exception_if_account_mismatch_bik(self):
        # проверяем, что вернется 422, если биллинг вернул ошибку не соотвествия аккаунта бику банка
        client_id = 999

        fault_msg = """
        <error><msg>Account 30101810400000000225 doesn't match bank with BIK=044525225</msg>
        <account>30101810400000000225</account><bik>044525225</bik><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>WRONG_ACCOUNT</code><parent-codes><code>INVALID_PARAM</code>
        <code>EXCEPTION</code></parent-codes><contents>Account 30101810400000000225 doesn't match bank
        with BIK=044525225</contents></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_natural_person_data,
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'code': 'invalid_account',
            'message': 'Field "{field}" has invalid value',
            'params': {'field': 'account'},
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_raise_exception_if_billing_fault(self):
        # проверяем, что вернется 503, если биллинг вернул ошибку создания клиента/плательщика
        client_id = 999

        fault_msg = """
        <error><msg>Unknown billing error</msg>
        <wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>UNKNOWN</code><parent-codes><code>INVALID_PARAM</code>
        <code>EXCEPTION</code></parent-codes></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_natural_person_data,
                expected_code=503,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'code': 'unknown',
            'message': 'Unknown billing error',
            'params': {},
        }
        assert_that(response, equal_to(exp_response))

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

    def test_enabling_paid_mode_for_existing_client_person(self):
        # включение платного режима с существующим клиентом и плательщиком
        person_id = 333
        client_id = 444

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = []

        data = {
            'person_id': person_id,
            'person_type': 'natural',
            'subscription_plan': 'paid',
        }

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )
        assert_not_called(mocked_xmlrpc.Balance.CreatePerson)
        assert_not_called(mocked_xmlrpc.Balance.CreateClient)

    def test_enabling_paid_mode_for_existing_client(self):
        # если у админа есть client_id используем его для создания плательщика
        person_id = 333
        client_id = 444

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id
        mocked_xmlrpc.Balance.GetClientContracts.return_value = []

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.subscription.send_email_to_admins') as mock_send_email:
            response = self.post_json(
                '/subscription/change/',
                data=self.valid_legal_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )

        # проверим, что организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))

        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(self.admin_uid),
            {
                'type': 'ur',
                'email': self.valid_legal_person_data['email'],
                'postcode': self.valid_legal_person_data['postal_code'],
                'inn': self.valid_legal_person_data['inn'],
                'phone': self.valid_legal_person_data['phone'],
                'postaddress': self.valid_legal_person_data['postal_address'],
                'name': self.organization['name']['ru'],
                'legaladdress': self.valid_legal_person_data['legal_address'],
                'account': self.valid_legal_person_data['account'],
                'longname': self.valid_legal_person_data['long_name'],
                'client_id': client_id,
                'bik': self.valid_legal_person_data['bik'],
                'kpp': self.valid_legal_person_data['kpp'],
            }
        )

        assert_not_called(mocked_xmlrpc.Balance.CreateClient)


class TestOrganizationPayView(TestCase):
    def test_paying_with_simple_user(self):
        self.enable_paid_mode()
        # обычный пользователь не может платить
        simple_user = self.create_user(
            nickname='simple_user',
            org_id=self.organization['id'],
        )

        self.post_json(
            '/subscription/pay/',
            data={'amount': 500},
            expected_code=403,
            headers=get_auth_headers(as_uid=simple_user['id']),
        )

    def test_pay_trust_with_simple_admin(self):
        # обычный админ может платить
        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id', 'subscription_plan'],
        )
        client_id = fresh_organization['billing_info']['client_id']

        exp_url_for_paying = 'https://trust-test.yandex.ru/web/payment'
        amount = 500
        request_id = 1992
        contract_id = 123
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetRequestPaymentMethods.return_value = [
            {
                'contract_id': contract_id,
                'person_id': fresh_organization['billing_info']['person_id'],
                'currency': 'RUB',
            },
        ]
        mocked_xmlrpc.Balance.PayRequest.return_value = {'payment_url': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'RequestID': request_id}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/trust/pay/',
                data={'amount': amount},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'payment_url': exp_url_for_paying,
            })
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            },
        )

        # в Биллинге были получены способы оплаты
        mocked_xmlrpc.Balance.GetRequestPaymentMethods.assert_called_once_with(
            {
                'RequestID': request_id,
                'OperatorUid': str(self.admin_uid),
            },
        )

        # в Биллинге была получена ссылка на оплату
        mocked_xmlrpc.Balance.PayRequest.assert_called_once_with(
            str(self.admin_uid),
            {
                'RequestID': request_id,
                'PaymentMethodID': 'trust_web_page',
                'Currency': 'RUB',  # пока только в рублях принимает, поменять когда появятся нерезиденты
                'PersonID': fresh_organization['billing_info']['person_id'],
                'ContractID': contract_id,
            },
        )

    def test_pay_trust_with_simple_admin_fail(self):
        # проверяем случай когда нет активного договора с плательщиком
        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id', 'subscription_plan'],
        )
        client_id = fresh_organization['billing_info']['client_id']

        exp_url_for_paying = 'https://trust-test.yandex.ru/web/payment'
        amount = 500
        request_id = 1992
        contract_id = 123
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetRequestPaymentMethods.return_value = [
            {
                'contract_id': contract_id,
                'person_id': '000001',
                'currency': 'RUB',
            },
        ]
        mocked_xmlrpc.Balance.PayRequest.return_value = {'payment_url': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'RequestID': request_id}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            self.post_json(
                '/subscription/trust/pay/',
                data={'amount': amount},
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # проверим что удалилась биллинговая информация
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            org_id=self.organization['id']
        )
        self.assertIsNone(billing_info)

        self.assertEqual(
            ActionModel(self.main_connection) \
                .filter(org_id=self.organization['id'], name='organization_billing_info_remove') \
                .count(),
            1,
        )

    def test_paying_with_outer_admin(self):
        # внешний админ не может платить
        self.enable_paid_mode()
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True,
            org_id=self.organization['id'],
        )

        auth_headers = get_auth_headers(
            as_outer_admin={
                'id': outer_admin['id'],
                'org_id': self.organization['id'],
            },
        )

        self.post_json(
            '/subscription/pay/',
            data={'amount': 500},
            headers=auth_headers,
            expected_code=403,
        )

    def test_pay_with_simple_admin(self):
        # обычный админ может платить
        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id', 'subscription_plan'],
        )

        exp_url_for_paying = 'https://billing_url'
        amount = 9999.5
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/pay/',
                data={'amount': amount},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'payment_url': exp_url_for_paying,
            })
        )

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            },
        )

    def test_pay_with_simple_admin_and_return_path(self):
        # проверяем, что если передать return_path для возврата после оплаты, то в биллинг он тоже прокинется
        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.client_id', 'id', 'subscription_plan'],
        )

        exp_url_for_paying = 'https://billing_url'
        return_path = 'https://return_path.yandex'
        amount = 55.5
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/pay/',
                data={'amount': amount, 'return_path': return_path},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'payment_url': exp_url_for_paying,
            })
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': return_path,
            },
        )

    def test_pay_with_simple_admin_and_billing_organization_on_free_subscription_plan(self):
        # платим в организации, которая заведена в биллинге, но сейчас в бесплатном режиме (например, отключили)
        self.enable_paid_mode()
        OrganizationModel(self.main_connection).disable_paid_mode(
            org_id=self.organization['id'],
            author_id=1,
        )
        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id', 'subscription_plan'],
        )
        # проверяем, что организация бесплатная
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        exp_url_for_paying = 'https://billing_url'
        amount = 1000
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/pay/',
                data={'amount': amount},
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'payment_url': exp_url_for_paying,
            })
        )

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            },
        )

    def test_paying_in_not_billing_organization(self):
        # в организации, которая НЕ заведена в биллинге платить нельзя
        with patch.object(app.billing_client, 'server', Mock()):
            response = self.post_json(
                '/subscription/pay/',
                data={'amount': 200},
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        assert_that(
            response,
            equal_to({
                'message': 'Organization is without contract',
                'code': 'organization_is_without_contract',
            })
        )


class TestOrganizationSubscriptionPricingView(TestCase):
    def test_get_pricing_should_return_pricing(self):
        self.enable_paid_mode()
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()

        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for i in range(10):
            self.create_user()

        # создадим робота
        robot_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )

        # выдаем лицензии на трекер, цена для них должна быть 0
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=only_ids(total_users[:5]))

        # проверим, что робот не попали в выборку
        users_ids = only_attrs(total_users, 'id')
        assert_that(
            users_ids,
            is_not(has_items(robot_id))
        )

        total_users = len(total_users)
        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(tracker_users, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': None,
            'services': {
                'tracker': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 0,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': None,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': None,
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': None,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_pricing_for_free_organization_should_return_pricing_with_valid_upgrade_option(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        for i in range(10):
            self.create_user()

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )

        total_users = len(total_users)

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')
            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(0, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': 0,
            'total_with_discount': None,
            'services': {
                'connect': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
                'tracker': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },

            },
            'promocode': None,
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': None,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_pricing_should_return_pricing_for_organization_with_promocode(self):
        # проверяем правильность возврата цены для организации с сервисом трекера и примененным для него промокодом
        self.enable_paid_mode()
        promocode_price = 1
        promocode_product_id = 12345
        promocode_id = 'CONNECT_80'
        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'connect': {
                    3: 12345,  # для 3 пользователей и больше цена будет по продукту 1234
                },
                'disk': {
                    1000000: 12345,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for _ in range(10):
            self.create_user()

        # создадим робота
        robot_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )
        # выдаем лицензии на трекер
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=only_ids(total_users[:7]))

        # проверим, что робот не попал в выборку
        users_ids = only_attrs(total_users, 'id')
        assert_that(
            users_ids,
            is_not(has_items(robot_id))
        )

        total_users = len(total_users)
        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '93'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            # сделаем вид что промокод истек и выполним второй запрос чтобы проверить что он не вернется
            OrganizationPromocodeModel(self.main_connection).update(
                update_data={'expires_at': datetime.date(year=1000, day=1, month=1)},
            )

            response_with_deactivated_promocode = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(tracker_users, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': total_users * promocode_price + tracker_users * per_user_price_tracker,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'total_with_discount': None,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': total_users * promocode_price,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': promocode_price,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': 0,
                    'per_user': 500,
                    'per_user_with_discount': promocode_price,
                    'users_count': 0,
                },
            },
            'promocode': {
                'id': promocode_id,
                'expires': format_date(promocode['expires_at']),
                'description': promocode['description']['ru'],
            },
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': total_users * promocode_price,
                'per_user': per_user_price_connect,
                'per_user_with_discount': promocode_price,
                'users_count': total_users,
            },
        }

        exp_response_without_promocode = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': None,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'total_with_discount': None,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': None,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': None,
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': None,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

        assert_that(
            response_with_deactivated_promocode,
            equal_to(
                exp_response_without_promocode,
            ),
        )

    @pytest.mark.skip('DIR-8844')
    def test_get_pricing_for_organization_with_promocode_for_no_licenses(self):
        # проверяем правильность возврата цены, если нет лицензий на сервис, либо бесплатный тарифный план для коннекта

        promocode_connect_price = 1
        promocode_connect_product_id = 12345
        promocode_tracker_product_id = 444
        promocode_tracker_price = 100
        promocode_id = 'CONNECT_80'
        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'connect': {
                    1: promocode_connect_product_id,
                    3: promocode_connect_product_id,
                },
                'tracker': {
                    5: promocode_tracker_product_id,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for _ in range(10):
            self.create_user()

        # создадим робота
        robot_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '200'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': str(promocode_connect_price)}],
                'ProductID': promocode_connect_product_id,
            },
            {
                'Prices': [{'Price': str(promocode_tracker_price)}],
                'ProductID': promocode_tracker_product_id,
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]
        tracker_users = 0  # нет лицензий
        total_users = 0  # бесплатный режим в организации
        total_users_upgrade = UserModel(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(tracker_users, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': total_users * promocode_connect_price + tracker_users * per_user_price_tracker,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'total_with_discount': tracker_users * promocode_tracker_price,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': promocode_tracker_price,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': total_users * promocode_connect_price,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': promocode_connect_price,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': {
                'id': promocode_id,
                'expires': format_date(promocode['expires_at']),
                'description': promocode['description']['ru'],
            },
            'connect_upgrade': {
                'total': total_users_upgrade * per_user_price_connect,
                'total_with_discount': total_users_upgrade * promocode_connect_price,
                'per_user': per_user_price_connect,
                'per_user_with_discount': promocode_connect_price,
                'users_count': total_users_upgrade,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    @override_settings(NEW_TRACKER_PRICING=True)
    def test_get_pricing_should_return_pricing_new_tracker(self):
        self.enable_paid_mode()

        promocode_id = 'CONNECT_FREE'
        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'tracker': {
                    1: app.config['PRODUCT_ID_FREE'],
                }
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for _ in range(110):
            self.create_user()

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )
        # выдаем лицензии на трекер
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=only_ids(total_users))


        total_users = len(total_users)
        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '400'}],
                'ProductID': settings.TRACKER_PRODUCT_ID_1,
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': settings.TRACKER_PRODUCT_ID_100,
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            # сделаем вид что промокод истек и выполним второй запрос чтобы проверить что он не вернется
            OrganizationPromocodeModel(self.main_connection).update(
                update_data={'expires_at': datetime.date(year=1000, day=1, month=1)},
            )

            response_with_deactivated_promocode = self.get_json('/subscription/pricing/')

        exp_response = {
            'total': 42090,
            'total_with_discount': 2090,
            'per_user': 0,
            'per_user_with_discount': 0,
            'users_count': tracker_users,
        }

        exp_response_without_promocode = {
            'total': 42090,
            'total_with_discount': None,
            'per_user': 0,
            'per_user_with_discount': 0,
            'users_count': tracker_users,
        }

        assert_that(
            response['services']['tracker'],
            equal_to(
                exp_response,
            ),
        )

        assert_that(
            response_with_deactivated_promocode['services']['tracker'],
            equal_to(
                exp_response_without_promocode,
            ),
        )


    @pytest.mark.skip('DIR-8844')
    def test_get_pricing_should_return_pricing_for_organization_with_free_promocode(self):
        # проверяем правильность возврата цены для организации c 'free' промокодом
        self.enable_paid_mode()

        promocode_id = 'CONNECT_FREE'
        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'connect': {
                    3: app.config['PRODUCT_ID_FREE'],  # для 3 пользователей и больше цена будет = 0
                },
                'tracker': {
                    10: app.config['PRODUCT_ID_FREE'],
                }
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for _ in range(10):
            self.create_user()

        # создадим робота
        robot_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )
        # выдаем лицензии на трекер
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=only_ids(total_users[:7]))

        # проверим, что робот не попал в выборку
        users_ids = only_attrs(total_users, 'id')
        assert_that(
            users_ids,
            is_not(has_items(robot_id))
        )

        total_users = len(total_users)
        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '93'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            # сделаем вид что промокод истек и выполним второй запрос чтобы проверить что он не вернется
            OrganizationPromocodeModel(self.main_connection).update(
                update_data={'expires_at': datetime.date(year=1000, day=1, month=1)},
            )

            response_with_deactivated_promocode = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(tracker_users, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': 0,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'total_with_discount': 0,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': 0,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': 0,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': 0,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': {
                'id': promocode_id,
                'expires': format_date(promocode['expires_at']),
                'description': promocode['description']['ru'],
            },
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': 0,
                'per_user': per_user_price_connect,
                'per_user_with_discount': 0,
                'users_count': total_users,
            },
        }

        exp_response_without_promocode = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + tracker_users * per_user_price_tracker,
            'total_with_discount': None,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'total_with_discount': None,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'total_with_discount': None,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': None,
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'total_with_discount': None,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

        assert_that(
            response_with_deactivated_promocode,
            equal_to(
                exp_response_without_promocode,
            ),
        )

    def test_get_pricing_should_return_pricing_not_enabled_service(self):
        # ручка должна вернуть цену, даже если сервис не подключен, но в общей цене она учитываться не должна
        self.enable_paid_mode()
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug']
        )

        for i in range(10):
            user = self.create_user()
        # выдаем лицензии на трекер
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=[user['id']])
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'],
            'because-i-can'
        )

        total_users = UserModel(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )

        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )
        assert_that(
            tracker_users,
            equal_to(1)
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '93'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID']
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_price_tracker = get_price_and_product_id_for_service(tracker_users, 'tracker', None)['price']

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect,
            'total_with_discount': None,
            'promocode': None,
            'services': {
                'tracker': {
                    'total': tracker_users * per_user_price_tracker,
                    'per_user': per_user_price_tracker,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                    'total_with_discount': None,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                    'total_with_discount': None,
                },
                'disk': {
                    'total': 0,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                    'total_with_discount': None,
                },

            },
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
                'total_with_discount': None,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_pricing_should_return_pricing_for_disk(self):
        # считаем цену для диска
        self.enable_paid_mode()
        disk = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='disk',
            name='disk',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disk['slug']
        )

        users = [self.create_user()['id'] for _ in range(10)]

        # выдаем лицензии на диск
        self.create_licenses_for_service(service_id=disk['id'], user_ids=users)

        total_users = UserModel(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )

        disk_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': disk['id'],
            }
        )
        assert_that(
            disk_users,
            equal_to(10)
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID']
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

            per_user_price_connect = get_price_and_product_id_for_service(total_users, 'connect', None)['price']
            per_user_disk = get_price_and_product_id_for_service(disk_users, 'disk', None)['price']

        assert per_user_disk == 500

        exp_response = {
            'currency': 'RUB',
            'total': total_users * per_user_price_connect + disk_users * per_user_disk,
            'total_with_discount': None,
            'promocode': None,
            'services': {
                'tracker': {
                    'total': 0,
                    'per_user': 0,
                    'per_user_with_discount': None,
                    'users_count': 0,
                    'total_with_discount': None,
                },
                'connect': {
                    'total': total_users * per_user_price_connect,
                    'per_user': per_user_price_connect,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                    'total_with_discount': None,
                },
                'disk': {
                    'total': disk_users * per_user_disk,
                    'per_user': per_user_disk,
                    'per_user_with_discount': None,
                    'users_count': disk_users,
                    'total_with_discount': None,
                },

            },
            'connect_upgrade': {
                'total': total_users * per_user_price_connect,
                'per_user': per_user_price_connect,
                'per_user_with_discount': None,
                'users_count': total_users,
                'total_with_discount': None,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_pricing_should_return_pricing_for_organization_with_internal_promocode(self):
        # проверяем, что не отдаем внутренние промокоды наружу
        self.enable_paid_mode()

        promocode_id = 'CONNECT_FREE'
        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'connect': {
                    3: app.config['PRODUCT_ID_FREE'],  # для 3 пользователей и больше цена будет = 0
                },
                'tracker': {
                    10: app.config['PRODUCT_ID_FREE'],
                }
            },
            promocode_type=promocode_type.internal,
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(self.meta_connection, self.main_connection, self.organization['id'], tracker['slug'])

        for _ in range(10):
            self.create_user()

        # создадим робота
        robot_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        total_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_robot': False,
                'is_dismissed': False,
            },
        )
        # выдаем лицензии на трекер
        self.create_licenses_for_service(service_id=tracker['id'], user_ids=only_ids(total_users[:7]))

        # проверим, что робот не попал в выборку
        users_ids = only_attrs(total_users, 'id')
        assert_that(
            users_ids,
            is_not(has_items(robot_id))
        )

        total_users = len(total_users)
        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '93'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID']
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/pricing/')

        exp_response = {
            'currency': 'RUB',
            'total': 0,
            'total_with_discount': None,
            'services': {
                'tracker': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 0,
                    'per_user_with_discount': None,
                    'users_count': tracker_users,
                },
                'connect': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 0,
                    'per_user_with_discount': None,
                    'users_count': total_users,
                },
                'disk': {
                    'total': 0,
                    'total_with_discount': None,
                    'per_user': 500,
                    'per_user_with_discount': None,
                    'users_count': 0,
                },
            },
            'promocode': None,
            'connect_upgrade': {
                'total': 0,
                'total_with_discount': None,
                'per_user': 0,
                'per_user_with_discount': None,
                'users_count': total_users,
            },
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )


class TestOrganizationCreateContractInfoView(TestCase):
    valid_natural_person_data = {
        'person_type': 'natural',
        'first_name': 'Alexander',
        'last_name': 'Akhmetov',
        'middle_name': 'R',
        'email': 'akhmetov@yandex-team.ru',
        'phone': '+79160000000',
    }

    valid_legal_person_data = {
        'person_type': 'legal',
        'long_name': 'ООО Яндекс',
        'postal_code': '119021',
        'postal_address': 'Москва, Льва Толстого 18Б',
        'legal_address': 'Москва, Льва Толстого 16',
        'inn': '666',
        'kpp': '777',
        'bik': '888',
        'account': '999',
        'phone': '+79160000000',
        'email': 'akhmetov@yandex-team.ru',
    }

    def test_create_billing_info_for_natural_person(self):
        # проверяем заведение биллинговой информации для физических лиц без смены тарифного плана

        # проверим, что организация в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # для организации нет записи в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({
                'org_id': self.organization['id']
            }),
            equal_to(0),
        )

        person_id = 500
        client_id = 999

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=self.valid_natural_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {}

        assert_that(
            response,
            equal_to(exp_response),
        )

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

    def test_create_billing_info_for_legal_person(self):
        # проверяем заведение биллинговой информации для юридических лиц без смены тарифного плана

        # проверим, что организация в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # для организации нет записи в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({
                'org_id': self.organization['id']
            }),
            equal_to(0),
        )

        person_id = 333
        client_id = 444

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=self.valid_legal_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {}

        assert_that(
            response,
            equal_to(exp_response),
        )

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_id': person_id,
            'person_type': 'legal',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

    def test_create_billing_info_for_existing_person(self):
        # проверяем заведение биллинговой информации для существущего плательщика без контрактов с коннектом

        # для организации нет записи в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({
                'org_id': self.organization['id']
            }),
            equal_to(0),
        )

        person_id = 500
        client_id = 999

        data = {
            'person_type': 'natural',
            'person_id': person_id,
        }

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = []

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {}

        assert_that(
            response,
            equal_to(exp_response),
        )

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
            'person_id': 500,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

    def test_create_billing_info_for_existing_person_with_contract(self):
        # проверяем, что вернется ошибка, если у клинета уже есть контракт с коннектом

        person_id = 500
        client_id = 999

        data = {
            'person_type': 'natural',
            'person_id': person_id,
        }

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': client_id}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': '444444',
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=data,
                expected_code=422,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        self.assertEqual(
            response['code'],
            'client_id_mismatch',
        )

        # для организации нет записи в OrganizationBillingInfoModel
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({
                'org_id': self.organization['id']
            }),
            equal_to(0),
        )

    def test_create_billing_info_for_client_with_contract(self):
        # у клиента есть client_id и контракт с коннектом, но мы заведем для него новый client_id

        person_id = 500
        client_id = 999

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': 12345678}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': '444444',
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=self.valid_natural_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
            'person_id': 500,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )

    def test_create_billing_info_for_client_with_represent(self):
        # клиент является представителем клиента и мы заведем для него новый client_id

        person_id = 500
        client_id = 999

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'RepresentedClientIds': [12345678]}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': '444444',
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/create-contract-info/',
                data=self.valid_natural_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        # должна была завестись запись в таблице OrganizationBillingInfoModel с информацией о договоре
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        exp_billing_info = {
            'client_id': client_id,
            'is_contract_active': True,
            'org_id': self.organization['id'],
            'person_type': 'natural',
            'contract_type': 'offer',
            'balance': 0,
            'first_debt_act_date': None,
            'last_mail_sent_at': None,
            'receipt_sum': 0,
            'act_sum': 0,
            'person_id': 500,
        }
        assert_that(
            billing_info,
            equal_to(exp_billing_info),
        )


class TestOrganizationDownloadContractView(TestCase):
    def test_get_contract_print_form(self):
        # получаем печатную форму для контракта

        # Так как расширенный коннект недоступен для ЯОрг организаций,
        # то нужно сначала выключить фичу:
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            CAN_WORK_WITHOUT_OWNED_DOMAIN,
            False
        )

        valid_legal_person_data = {
            'subscription_plan': 'paid',
            'person_type': 'legal',
            'long_name': 'ООО Яндекс',
            'postal_code': '119021',
            'postal_address': 'Москва, Льва Толстого 18Б',
            'legal_address': 'Москва, Льва Толстого 16',
            'inn': '666',
            'kpp': '777',
            'bik': '888',
            'account': '999',
            'phone': '+79160000000',
            'email': 'akhmetov@yandex-team.ru',
            'contract': True
        }

        # включаем платный режим с договором
        person_id = 333
        client_id = 444
        contract_id = 555
        pdf_contract = 'печатная форма договора'
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetContractPrintForm.return_value = xmlrpc.client.Binary(
            base64.b64encode(pdf_contract.encode('utf-8'))
        )

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/change/',
                data=valid_legal_person_data,
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        exp_response = {
            'subscription_plan': 'paid',
        }

        assert_that(
            response,
            equal_to(exp_response),
        )

        # проверим, что у организации появился контракт
        billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        assert_that(billing_info['contract_type'], equal_to('contract'))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response, headers = self.get_json(
                '/subscription/contract/download/',
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_uid),
                raw=True,
                return_headers=True,
            )

        assert_that(
            response.decode('utf-8'),
            equal_to(pdf_contract),
        )

        assert_that(
            headers['Content-Disposition'],
            equal_to('attachment;filename=contract.pdf'),
        )

        assert_that(
            headers['Content-Type'],
            equal_to('application/pdf; charset=utf-8'),
        )


class TestOrganizationByOrgId__get_8(TestCase):
    def setUp(self):
        super(TestOrganizationByOrgId__get_8, self).setUp()
        # включаем сервис для всех методов
        self.org_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service['id'],
        )
        # и ходим везде с токеном
        set_auth_uid(None)
        self.oauth_headers = get_oauth_headers()

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_organization_with_disabled_services(self):
        # проверяем, что отдаются и включенные и выключенные сервисы
        service = ServiceModel(self.meta_connection).create(
            client_id='svsdfewfwsd',
            slug=TRACKER_SERVICE_SLUG,
            name='Autotest Service',
            paid_by_license=True,
            trial_period_months=1,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug']
        )

        # выключаем его
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
            'reason',
        )

        # проверяем, что возвращаются и включенный и выключенный сервисы
        response = self.get_json(
            '/v8/organizations/%s/?fields=id,revision,services' % self.organization['id'],
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=self.organization['id'],
                services=contains(
                    has_entries(
                        slug=self.service['slug'],
                        enabled=True,
                    ),
                    has_entries(
                        slug=TRACKER_SERVICE_SLUG,
                        enabled=False,
                    )
                )
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.read_organization])
    def test_get_with_root_departments(self):
        # Проверяем, что если запросить поле root_departments,
        # то отдастся список корневых отделов и id, name и is_outstaff
        # проверяем, что возвращаются и включенный и выключенный сервисы
        org_id = self.organization['id']

        outstaff = DepartmentModel(self.main_connection).get_or_create_outstaff(org_id)

        response = self.get_json(
            '/v8/organizations/%s/?fields=root_departments.name,root_departments.is_outstaff' % org_id,
            headers=self.oauth_headers,
        )

        assert_that(
            response,
            has_entries(
                id=org_id,
                root_departments=contains(
                    has_entries(
                        id=1,
                        name='Все сотрудники',
                        is_outstaff=False,
                    ),
                    has_entries(
                        id=outstaff['id'],
                        name='Внешние сотрудники',
                        is_outstaff=True,
                    ),
                )
            )
        )


class TestOrganizationByOrgId__delete(TestCase):
    def setUp(self):
        super(TestOrganizationByOrgId__delete, self).setUp()
        yandex_organization = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        self.yandex_org_id = yandex_organization['organization']['id']
        self.yandex_admin_uid = yandex_organization['admin_user_uid']

    def test_successful_delete_without_owned_domains_and_without_accounts(self):
        # Внешний админ может удалять организацию без домена и без пользователей
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=False)

        # Удалим всех коннектных пользователей
        UserModel(self.main_connection).filter(org_id=self.organization['id']).delete()
        set_auth_uid(self.outer_admin['id'])
        self.delete_json(
            '/organizations/%s/' % self.organization['id'],
        )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_successful_delete_without_owned_domains_and_without_accounts_async(self):
        # Внешний админ может удалять организацию без домена и без пользователей
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=False)

        # Удалим всех коннектных пользователей
        UserModel(self.main_connection).filter(org_id=self.organization['id']).delete()
        set_auth_uid(self.outer_admin['id'])
        with calls_count(self.mocked_passport.unset_pdd_admin, 1):
            self.delete_json(
                '/organizations/%s/delete/' % self.organization['id'],
                expected_code=202,
            )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org


    def test_successful_delete_without_owned_domains_and_without_accounts_async_multiple(self):
        # Внешний админ может удалять организацию без домена и без пользователей
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=False)

        create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            admin_uid=self.admin_uid,
        )

        # Удалим всех коннектных пользователей
        UserModel(self.main_connection).filter(org_id=self.organization['id']).delete()
        set_auth_uid(self.outer_admin['id'])
        with calls_count(self.mocked_passport.unset_pdd_admin, 0):
            self.delete_json(
                '/organizations/%s/delete/' % self.organization['id'],
                expected_code=202,
            )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_success_delete_with_resources_async(self):
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='some_id',
        )
        self.delete_json(
            '/organizations/%s/delete/' % self.organization['id'],
            expected_code=202
        )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_delete_organization_with_inner_admin_async(self):
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)

        set_auth_uid(self.outer_admin['id'])
        self.delete_json(
            '/organizations/%s/delete/' % self.organization['id'],
            expected_code=202,
        )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_organization_has_owned_domains_and_billing_info_without_dept_async(self):
        # Можно удалить организацию с заполненной биллинговой информацией
        # через ручку /delete без долга
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)
        OrganizationBillingInfoModel(self.main_connection).create(
            self.organization['id'],
            123456,
            'offer',
            'legal',
            1234567,
        )
        with patch.object(app.billing_client, 'get_balance_info') as mocked_get_balance:
            mocked_get_balance.return_value = {'balance': 50}

            self.delete_json(
                '/organizations/%s/delete/' % self.organization['id'],
                expected_code=202,
            )
        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_organization_has_owned_domains_and_billing_info_with_negative_balance_async(self):
        # Нельзя удалить организацию с заполненной биллинговой информацией
        # через ручку /delete с отрицательным балансом
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)
        OrganizationBillingInfoModel(self.main_connection).create(
            self.organization['id'],
            123456,
            'offer',
            'legal',
            1234567,
        )
        ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
        )
        ServiceModel(self.main_connection).create(
            id=444,
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            'tracker',
        )
        with patch.object(app.billing_client, 'get_balance_info') as mocked_get_balance:
            with patch.object(app.billing_client, 'get_products_price') as mocked_price:
                mocked_price.return_value = {}
                mocked_get_balance.return_value = {'balance': -50}

                self.delete_json(
                    '/organizations/%s/delete/' % self.organization['id'],
                    expected_code=422,
                )

    def test_organization_has_owned_domains_and_billing_info_with_dept_async(self):
        # Нельзя удалить организацию с заполненной биллинговой информацией
        # через ручку /delete если начисления больше баланса
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)
        OrganizationBillingInfoModel(self.main_connection).create(
            self.organization['id'],
            123456,
            'offer',
            'legal',
            1234567,
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_balance_and_current_month_price') as get_price_mock:
            get_price_mock.return_value = (50, 100, )

            self.delete_json(
                '/organizations/%s/delete/' % self.organization['id'],
                expected_code=422,
            )

    def test_organization_has_owned_domains_and_billing_info_with_dept_less_balance_async(self):
        # Можно удалить организацию с заполненной биллинговой информацией
        # через ручку /delete если начисления меньше баланса
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)
        OrganizationBillingInfoModel(self.main_connection).create(
            self.organization['id'],
            123456,
            'offer',
            'legal',
            1234567,
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_balance_and_current_month_price') as get_price_mock:
            get_price_mock.return_value = (100, 80, )

            self.delete_json(
                '/organizations/%s/delete/' % self.organization['id'],
                expected_code=202,
            )

        org = OrganizationModel(self.main_connection).find(
            filter_data={
                'id': self.organization['id'],
            }
        )
        assert not org

    def test_fail_delete_with_resources(self):
        # Нельзя удалить организацию со связанным ресурсом
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='some_id',
        )
        response = self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=422
        )
        self.assertEqual(
            response['code'],
            'cannot_delete_organization_with_resources',
        )

    def test_organization_has_owned_domains_and_billing_info(self):
        # Нельзя удалить организацию с заполненной биллинговой информацией
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)
        OrganizationBillingInfoModel(self.main_connection).create(
            self.organization['id'],
            123456,
            'offer',
            'legal',
            1234567,
        )
        set_auth_uid(self.outer_admin['id'])
        self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=403,
        )

    def test_delete_organization_with_inner_admin(self):
        # Нельзя удалить организацию с коннектным админом
        # Смоделируем ситуацию, когда организацию удаляет
        # внешний админ, но в организации остался один внутренний
        # админ.
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)

        set_auth_uid(self.outer_admin['id'])
        response = self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='organization_has_accounts',
                message='Organizations has {accounts_number} accounts',
            )
        )

    def test_organization_with_users_outer_admin(self):
        # Нельзя удалить организацию, в которой остались учетки (кроме последнего админа)
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)

        set_auth_uid(self.outer_admin['id'])

        response = self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='organization_has_accounts',
                message='Organizations has {accounts_number} accounts',
                params=has_entries(
                    accounts_number=1,
                ),
            )
        )

    def test_organization_without_master_domain(self):
        # Если масетр домен не найден в базе, просто удаляем остатки организации из базы
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True, master=False)

        set_auth_uid(self.outer_admin['id'])
        UserModel(self.main_connection).delete(filter_data={'org_id': self.organization['id']})

        self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=204,
        )
        org = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            org,
            none(),
        )

    def test_successful_delete_with_owned_domains(self):
        # Если нет биллинговой инфы и не осталось учеток - удаляем организацию
        DomainModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(owned=True)

        UserModel(self.main_connection).delete(filter_data={'org_id': self.organization['id']})

        set_auth_uid(self.outer_admin['id'])
        self.delete_json(
            '/organizations/%s/' % self.organization['id'],
            expected_code=204,
        )
        org = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            org,
            none(),
        )
        domains = DomainModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
            }
        )
        assert_that(
            domains,
            equal_to([]),
        )

    def test_successful_delete_yandex_organization_without_domain_without_accounts(self):
        # Яндекс организация без доменов и без учеток удалется
        set_auth_uid(self.yandex_admin_uid)
        self.delete_json(
            '/organizations/%s/' % self.yandex_org_id,
        )
        org = OrganizationModel(self.main_connection).get(self.yandex_org_id)
        assert_that(
            org,
            none(),
        )
        # И при удалении мы должны удалить у админа лишний org_id из паспорта
        # для этого мы должны создать таск SyncExternalIDS
        self.assert_task_created('SyncExternalIDS')
        self.assert_no_failed_tasks()

    def test_failed_delete_yandex_organization_without_domain_with_accounts(self):
        # Яндекс организация без доменов но с учетками не удалется
        create_yandex_user(self.meta_connection, self.main_connection, 10, self.yandex_org_id)
        set_auth_uid(self.yandex_admin_uid)
        response = self.delete_json(
            '/organizations/%s/' % self.yandex_org_id,
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='organization_has_accounts',
                message='Organizations has {accounts_number} accounts',
                params=has_entries(
                    accounts_number=1,
                ),
            )
        )

    def test_successful_delete_yandex_organization_with_domain_without_accounts(self):
        # Яндекс организация с доменом и без учеток удалется
        set_auth_uid(self.yandex_admin_uid)
        DomainModel(self.main_connection).create(
            'test-domain.com',
            self.yandex_org_id,
            owned=True,
            master=True,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.delete_domain_with_accounts') as mock_delete_domains:
            self.delete_json(
                '/organizations/%s/' % self.yandex_org_id,
            )
            assert_called_once(
                mock_delete_domains,
                ANY,
                self.yandex_org_id,
            )
            org = OrganizationModel(self.main_connection).get(self.yandex_org_id)
            assert_that(
                org,
                none(),
            )
            domains = DomainModel(self.main_connection).find(
                filter_data={
                    'org_id': self.yandex_org_id,
                }
            )
            assert_that(
                domains,
                equal_to([]),
            )


class TestOrganizationWithoutDomain__post(TestCase):
    def setUp(self):
        super(TestOrganizationWithoutDomain__post, self).setUp()

        # id админа организации в паспорте (создан при регистрации через паспорт)
        self.admin_uid = 1
        # userinfo из ЧЯ
        bb_userinfo = fake_userinfo(
            uid=self.admin_uid,
            default_email='default@ya.ru',
            login='art',
            first_name='Александр',
            last_name='Артеменко',
            sex='1',
            birth_date='1980-10-05',
            country='ru',
        )
        self.mocked_blackbox.userinfo.return_value = bb_userinfo
        self.mocked_blackbox.batch_userinfo.return_value = [bb_userinfo]

        # И пресет no-owned-domain с сервисов dashboard
        PresetModel(self.meta_connection).create(
            'without-domain',
            service_slugs=[],
            settings={}
        )
        self.headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid))
        self.mocked_blackbox.reset_mock()

    def test_organization_without_domain_with_clouds(self):
        # Занулим пресет, т.к. в миграциях нет данных нет для ServiceModel
        PresetModel(self.meta_connection).update({'service_slugs': []})
        # Создаем новую организацию передавая данные облаков.
        with patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.sync_cloud_org') as sync_cloud_org:
            result = self.post_json(
                '/organization/without-domain/',
                data={
                    'clouds': ['123', '234', '567', ],
                    'preset': 'cloud',
                    'organization_type': 'cloud',
                },
                headers=self.headers,
            )

            # Ручка должна вернуть id созданной организации
            org_id = result['org_id']

            # остальные аргументы это коннекшены, легко их не пометчить
            assert_that(
                sync_cloud_org.call_args_list[0][0][0],
                equal_to(
                    org_id
                )
            )

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='unknown',
                language='ru',
                label='org-{}'.format(org_id),
                country='ru',
                tld='ru',
                preset='cloud',
                organization_type='cloud',
                clouds='123,234,567',
            )
        )

    def test_organization_without_domain_with_cloud_org_id(self):
        # Занулим пресет, т.к. в миграциях нет данных нет для ServiceModel
        PresetModel(self.meta_connection).update({'service_slugs': []})
        # Создаем новую организацию передавая cloud_org_id
        with patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.sync_cloud_org') as sync_cloud_org:
            result = self.post_json(
                '/organization/without-domain/',
                data={
                    'cloud_org_id': 'some-cloud-id',
                    'preset': 'cloud',
                    'organization_type': 'cloud',
                },
                headers=self.headers,
            )

            # Ручка должна вернуть id созданной организации
            org_id = result['org_id']

            # остальные аргументы это коннекшены, легко их не пометчить
            assert_that(
                sync_cloud_org.call_args_list[0][0][0],
                equal_to(
                    org_id
                )
            )

        assert_that(
            OrganizationMetaModel(self.meta_connection).get(org_id),
            has_entries(
                cloud_org_id='some-cloud-id',
                label=f'org-{org_id}',
            )
        )

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='unknown',
                language='ru',
                label='org-{}'.format(org_id),
                country='ru',
                tld='ru',
                preset='cloud',
                organization_type='cloud',
            )
        )

        response = self.get_json(
            f'/v6/organizations/{org_id}/?fields=id,cloud_org_id',
        )

        assert_that(
            response,
            equal_to({
                'id': org_id,
                'cloud_org_id': 'some-cloud-id',
            })
        )

        response = self.get_json(
            '/v6/organizations/?fields=id,cloud_org_id',
            headers=self.headers,
        )
        expected = {
            'result': [
                {'id': org_id, 'cloud_org_id': 'some-cloud-id'}
            ],
            'links': {}
        }
        assert response == expected

        headers = get_auth_headers(as_cloud_uid='some_uid', with_uid=False)
        response = self.get_json(
            '/v6/organizations/?fields=id,cloud_org_id',
            headers=headers,
        )

        empty_response = {'result': [], 'links': {}}
        assert response == empty_response

    def test_organization_without_domain_with_duplicate_cloud_org_id(self):
        # Занулим пресет, т.к. в миграциях нет данных нет для ServiceModel
        PresetModel(self.meta_connection).update({'service_slugs': []})
        # Создаем новую организацию передавая cloud_org_id
        with patch.object(SyncCloudOrgTask, 'delay') as delay_task:
            result = self.post_json(
                '/organization/without-domain/',
                data={
                    'cloud_org_id': 'some-cloud-id',
                    'preset': 'cloud',
                    'organization_type': 'cloud',
                },
                headers=self.headers,
                expected_code=201,
            )
            org_id = result['org_id']

            result = self.post_json(
                '/organization/without-domain/',
                data={
                    'cloud_org_id': 'some-cloud-id',
                    'preset': 'cloud',
                    'organization_type': 'cloud',
                },
                headers=self.headers,
                expected_code=400,
                expected_error_code='Conflict',
                expected_message=f'Organizations with the same cloud_org_id already exist: {org_id}',
            )

    def test_organizations_limit(self):
        settings.USER_ORGANIZATIONS_LIMIT = 6
        # Создадим пользователю 5 организаций
        for _ in range(5):
            create_organization(
                self.meta_connection,
                self.main_connection,
                label='org',
                admin_uid=self.admin_uid
            )
        # попробуем создать 6 - должно быть успешно
        self.post_json(
            '/organization/without-domain/',
            data={},
            headers=self.headers,
        )
        # при создании 7 должна быть ошибка
        self.post_json(
            '/organization/without-domain/',
            data={},
            headers=self.headers,
            expected_code=409,
        )

        # Если отключить фичу - ошибка тоже должна быть
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            MULTIORG,
            False,
        )
        self.post_json(
            '/organization/without-domain/',
            data={},
            headers=self.headers,
            expected_code=409,
        )

    def test_organization_without_domain(self):
        # Создаем новую организацию без домена.
        # Удостоверимся, что у этой организации не будет домена и ни одного сотрудника.
        result = self.post_json(
            '/organization/without-domain/',
            data={},
            headers=self.headers,
        )
        # Ручка должна вернуть id созданной организации
        org_id = result['org_id']

        # проверяем, что у организации нет ни одного домена
        assert_that(
            DomainModel(self.main_connection) \
            .filter(org_id=org_id) \
            .count(),
            equal_to(0)
        )
        # и что source c остальными параметрами сохранились в базе
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='unknown',
                language='ru',
                label='org-{}'.format(org_id),
                country='ru',
                tld='ru',
                preset='without-domain',
                organization_type='common',
            )
        )

        # И для организации должно быть отмечено, что у неё нет ни одного подтверждённого домена
        self.assertEqual(
            OrganizationModel(self.main_connection).has_owned_domains(org_id),
            False,
        )

        # админская группа должна быть с id=2
        # исторический костыль
        assert_that(
            GroupModel(self.main_connection).get_or_create_admin_group(org_id),
            has_entries(id=2)
        )

        # Доменов быть не должно ни одного.
        domains = DomainModel(self.main_connection).find({'org_id': org_id})
        assert_that(
            domains,
            empty(),
        )

        # У корневого отдела не должно быть рассылки, потому что в организации нет отдела
        root_department = DepartmentModel(self.main_connection) \
                          .filter(org_id=org_id, id=1)\
                          .one()
        assert_that(
            root_department,
            has_entries(
                label=none(),
                uid=none(),
            )
        )

        # А пользователь должен стать внутренним админом
        users = UserMetaModel(self.meta_connection) \
                .filter(org_id=org_id) \
                .all()

        assert_that(
            users,
            contains(
                has_entries(
                    id=self.admin_uid,
                    # Пользователь должен считаться внутренним, несмотря на
                    # то, что у него недоменный uid = 1
                    user_type='inner_user',
                    is_outer=False,
                )
            )
        )

        # И в шарде он тоже должен быть
        users = UserModel(self.main_connection) \
                .filter(org_id=org_id) \
                .fields('id', 'nickname', 'is_admin') \
                .all()
        assert_that(
            users,
            contains(
                has_entries(
                    id=self.admin_uid,
                    nickname='art',
                    is_admin=True,
                )
            )
        )

        # А ещё, у организации должны быть события про то, что она добавлена,
        # добавлен пользователь, отдел и тд.
        events = EventModel(self.main_connection).filter(org_id=org_id).all()

        def event(name, object_name=None):
            if object_name is not None:
                return has_entries(
                    name=name,
                    object=has_entries(
                        name=has_entries(en=object_name)
                    )
                )
            else:
                return has_entries(name=name)
        assert_that(
            events,
            contains(
                event('organization_added'),
                event('group_added', 'Organization administrator'),
                event('group_added', 'Organization deputy administrators'),
                event('group_added', 'Head of department "All employees"'),
                event('department_added', 'All employees'),
                event('group_added', 'Organization robots'),
                has_entries(
                    name='user_added',
                    object=has_entries(
                        name=has_entries(first='Vasya', last='Pupkin')
                    )
                ),
                event('department_user_added', 'All employees'),
                event('department_property_changed', 'All employees'),
            ),
        )

    def test_custom_params(self):
        # Проверим, что при создании можно переопределить все параметры
        PresetModel(self.meta_connection).create(
            'custom-preset',
            service_slugs=[],
            settings={}
        )
        result = self.post_json(
            '/organization/without-domain/',
            data=dict(
                language='en',
                tld='com',
                country='US',
                name='Рога и Копыта',
                preset='custom-preset',
                label='custom-label',
                source='custom-source',
                organization_type='education',
            ),
           headers=self.headers,
        )
        # Ручка должна вернуть id созданной организации
        org_id = result['org_id']

        # и что кастомные параметры сохранились в базе
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                name='Рога и Копыта',
                source='custom-source',
                language='en',
                label='custom-label',
                country='US',
                tld='com',
                preset='custom-preset',
                organization_type='education',
            )
        )

    def test_create_partner_organization(self):
        # Проверим, что при создании можно переопределить все параметры
        PresetModel(self.meta_connection).create(
            'custom-preset',
            service_slugs=[],
            settings={}
        )
        org_id = self.post_json(
            '/organization/without-domain/',
            data=dict(
                organization_type='partner_organization',
            ),
           headers=self.headers,
        )['org_id']

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                organization_type='partner_organization',
            )
        )

    def test_create_with_partner_id(self):
        PresetModel(self.meta_connection).create(
            'custom-preset',
            service_slugs=[],
            settings={}
        )

        partner_org_id = self.post_json(
            '/organization/without-domain/',
            data=dict(
                organization_type='partner_organization',
            ),
           headers=self.headers,
        )['org_id']
        not_partner_org_id = self.post_json(
            '/organization/without-domain/',
            data=dict(
                organization_type='common',
            ),
           headers=self.headers,
        )['org_id']

        self.post_json(
            '/organization/without-domain/',
            data=dict(
                partner_id=partner_org_id + 100,
            ),
            headers=self.headers,
            expected_code=422
        )
        self.post_json(
            '/organization/without-domain/',
            data=dict(
                partner_id=not_partner_org_id,
            ),
            headers=self.headers,
            expected_code=422
        )
        org_id = self.post_json(
            '/organization/without-domain/',
            data=dict(
                partner_id=partner_org_id,
            ),
            headers=self.headers,
        )['org_id']

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                organization_type='common',
                partner_id=partner_org_id
            )
        )

    def test_karma_and_ip_assign(self):
        # Проверим, что вьюшка создания организации проинициализирует
        # карму и сохранит пользовательский IP.
        headers = self.headers.copy()
        user_ip = '91.198.174.192'
        headers['X-User-IP'] = user_ip

        # Проверим, что при создании можно переопределить все параметры
        result = self.post_json(
            '/organization/without-domain/',
            data=dict(
                name='Рога и Копыта',
            ),
            headers=headers,
        )

        org_id = result['org_id']
        # и что кастомные параметры сохранились в базе
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                karma=50,
                ip=user_ip,
             )
        )


class TestOrganizationWithDomain__post(TestCase):
    def setUp(self):
        super(TestOrganizationWithDomain__post, self).setUp()

        # id админа организации в паспорте (создан при регистрации через паспорт)
        self.admin_uid = 1
        self.domain_name = 'test-domain.qqq'
        # домен новой организации
        self.domain = 'test-org{}'.format(app.config['DOMAIN_PART'])
        # userinfo из ЧЯ
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'admin',
            },
            'domain': 'camomile.yaserv.biz',
            'uid': self.admin_uid,
            'default_email': 'default@ya.ru',
        }

        # Сделаем сервис, который надо включить в пресет
        self.service_slug = 'the-service'
        ServiceModel(self.meta_connection).create(
            slug=self.service_slug,
            name='The Service',
        )

        # Сделаем сервис dashboard, который надо включить до подтверждения домена
        self.dashboard_slug = 'dashboard'
        ServiceModel(self.meta_connection).create(
            slug=self.dashboard_slug,
            name='Dashboard',
        )

        # Создадим пресет
        PresetModel(self.meta_connection).create(
            'only-the-service',
            service_slugs=[self.service_slug, self.dashboard_slug],
            settings={}
        )

        # И пресет no-owned-domain с сервисов dashboard
        PresetModel(self.meta_connection).create(
            'no-owned-domain',
            service_slugs=[self.dashboard_slug],
            settings={}
        )
        self.dependencies = {
            Service(self.service_slug): [
                Setting('shared-contacts', True)
            ],
        }
        self.headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid))

    def test_organizations_limits(self):
        settings.USER_ORGANIZATIONS_LIMIT = 6
        for i in range(6):
            with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                    patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):
                self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
                self.post_json(
                    '/organization/with-domain/',
                    data={
                        'domain_name': f'test-domain-{i}.qqq',
                        'preset': 'only-the-service',
                        'source': 'pdd_new_promo',
                        'tld': 'com',
                    },
                    headers=self.headers,
                )

        # упираемся в лимит теперь
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers,
                expected_code=409,
            )

    def test_organization_with_domain_with_multiorg(self):
        # создаем новую организацию с промки ПДД, при этом есть включенная фича
        org_with_feature = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            admin_uid=self.admin_uid,
            root_dep_label='all',
        )['organization']

        set_feature_value_for_organization(
            self.meta_connection,
            org_with_feature['id'],
            MULTIORG,
            True,
        )

        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'art',
                'firstname': 'Александр',
                'lastname': 'Артеменко',
                'sex': '1',
                'birth_date': '1980-10-05',
            },
            'uid': self.admin_uid,
            'domain': 'camomile.yaserv.biz',
            'default_email': 'default@ya.ru',
        }
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers,
            )
            self.assertEqual(
                self.mocked_blackbox.userinfo.call_count,
                3,
            )

        # получили id созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )
        org_id = result['org_id']
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='pdd_new_promo',
                language='ru',
                label=contains_string(self.domain_name.replace('.', '-')),
                country='ru',
                tld='com',
            )
        )

        self.assertEqual(
            OrganizationModel(self.main_connection).has_owned_domains(org_id),
            False,
        )

        # админская группа должна быть с id=2
        # исторический костыль
        assert_that(
            GroupModel(self.main_connection).get_or_create_admin_group(org_id),
            has_entries(id=2)
        )
        # корневой департамент должен быть без label и uid
        assert_that(
            DepartmentModel(self.main_connection).get(ROOT_DEPARTMENT_ID, org_id),
            has_entries(
                label=None,
                uid=None,
            )
        )

        domains = DomainModel(self.main_connection).find({'org_id': org_id})
        assert_that(
            domains,
            has_length(1),
        )
        assert_that(
            domains[0],
            has_entries(
                name=self.domain_name,
                owned=False,
                via_webmaster=True,
                master=False,
                display=False,
            )
        )
        # Проверяем, что применился пресет no-owned-domain
        # Проверяем, что применились пресеты
        org = OrganizationModel(self.main_connection).find(
            {'id': org_id},
            fields=['services.slug'],
            one=True,
        )
        assert_that(
            org['services'],
            contains(
                has_entries(
                    slug=self.dashboard_slug,
                )
            )
        )

        # Проверяем что админ добавился как обычный пользователь
        user = UserModel(self.main_connection).filter(org_id=org_id, id=self.admin_uid).all()
        self.assertEqual(len(user), 1)
        user_meta = UserMetaModel(self.meta_connection).filter(org_id=org_id, id=self.admin_uid).one()
        self.assertEqual(user_meta['user_type'], 'inner_user')

        # Проверяем, что нельзя добавить отдел с label
        headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid, org_id=org_id))
        DEPARTMENT_NAME = {
            'ru': 'Департамент',
            'en': 'Department',
        }
        department_data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        self.post_json(
            '/departments/',
            headers=headers,
            data=department_data,
            # Так как домен ещё не подтверждён, то можно попытаться добавить домен,
            # но такая попытка провалится, потому что мы задали login,
            # а рассылку ещё завести нельзя.
            # Прежде мы тут вообще отдавали 403, потому что добавление отделов было запрещено.
            expected_code=422,
            expected_error_code='unable_to_create_maillist',
            expected_message='Unable to create maillist for organization without domains',

        )

        # Подтверждаем домен
        with mocked_blackbox() as blackbox, \
            patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain:
            # Нужно чтобы ручка вернула словарь без ключа errors,
            # тогда ответ будет считаться успешным.
            self.mocked_webmaster_inner_verify.return_value = {}
            # После этого, ручка info должна вернуть, что домен подтверждён
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=True)

            response = self.post_json(
                '/domains/{0}/check-ownership/'.format(self.domain_name),
                {'verification_type': 'webmaster.dns'},
                headers=headers,
                expected_code=200,
            )

            # Подтверждение пройдет асинхронно
            assert_that(
                response,
                has_entries(
                    domain=self.domain_name,
                    owned=False,
                ),
            )

            assert_called_once(
                self.mocked_webmaster_inner_verify,
                self.domain_name,
                self.admin_uid,
                'DNS',
                ignore_errors=ANY,
            )

            assert_called_once(
                mocked_sync_single_domain,
                org_id=org_id,
                domain_name=self.domain_name,
            )

    def test_organization_with_domain(self):
        # создаем новую организацию с промки ПДД
        # проверяем что при создании оргинизации в базу добавляется неподтверждённый домен
        # Хотя домен не подтверждён, с организацией уже можно производить некоторые действия,
        # потому что у неё должна была автоматически включиться фича, расзрешающая режим
        # "Яндекс Организации": DIR-6685
        #
        # У организации в метабазе стоит осбоый признак - has_owned_domains=False
        # Подтверждаем домен
        # Признак в метабазе меняется на has_owned_domains=True
        # Появляется корневой департамент, применяются пресеты.
        # После этого для организации становятся доступными другие ручки
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers,
            )
            assert_called(
                self.mocked_blackbox.userinfo,
                3,
            )

        # получили id созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )
        org_id = result['org_id']
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='pdd_new_promo',
                language='ru',
                label=contains_string(self.domain_name.replace('.', '-')),
                country='ru',
                tld='com',
            )
        )

        self.assertEqual(
            OrganizationModel(self.main_connection).has_owned_domains(org_id),
            False,
        )

        # админская группа должна быть с id=2
        # исторический костыль
        assert_that(
            GroupModel(self.main_connection).get_or_create_admin_group(org_id),
            has_entries(id=2)
        )
        # корневой департамент должен быть без label и uid
        assert_that(
            DepartmentModel(self.main_connection).get(ROOT_DEPARTMENT_ID, org_id),
            has_entries(
                label=None,
                uid=None,
            )
        )

        domains = DomainModel(self.main_connection).find({'org_id': org_id})
        assert_that(
            domains,
            has_length(1),
        )
        assert_that(
            domains[0],
            has_entries(
                name=self.domain_name,
                owned=False,
                via_webmaster=True,
                master=False,
                display=False,
            )
        )
        # Проверяем, что применился пресет no-owned-domain
        # Проверяем, что применились пресеты
        org = OrganizationModel(self.main_connection).find(
            {'id': org_id},
            fields=['services.slug'],
            one=True,
        )
        assert_that(
            org['services'],
            contains(
                has_entries(
                    slug=self.dashboard_slug,
                )
            )
        )
        # Проверяем, что нельзя добавить отдел с label
        headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid, org_id=org_id))
        DEPARTMENT_NAME = {
            'ru': 'Департамент',
            'en': 'Department',
        }
        department_data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        self.post_json(
            '/departments/',
            headers=headers,
            data=department_data,
            # Так как домен ещё не подтверждён, то можно попытаться добавить домен,
            # но такая попытка провалится, потому что мы задали login,
            # а рассылку ещё завести нельзя.
            # Прежде мы тут вообще отдавали 403, потому что добавление отделов было запрещено.
            expected_code=422,
            expected_error_code='unable_to_create_maillist',
            expected_message='Unable to create maillist for organization without domains',

        )

        # Подтверждаем домен
        with mocked_blackbox() as blackbox, \
            patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain:
            # Нужно чтобы ручка вернула словарь без ключа errors,
            # тогда ответ будет считаться успешным.
            self.mocked_webmaster_inner_verify.return_value = {}
            # После этого, ручка info должна вернуть, что домен подтверждён
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=True)

            response = self.post_json(
                '/domains/{0}/check-ownership/'.format(self.domain_name),
                {'verification_type': 'webmaster.dns'},
                headers=headers,
                expected_code=200,
            )

            # Подтверждение пройдет асинхронно
            assert_that(
                response,
                has_entries(
                    domain=self.domain_name,
                    owned=False,
                ),
            )

            assert_called_once(
                self.mocked_webmaster_inner_verify,
                self.domain_name,
                self.admin_uid,
                'DNS',
                ignore_errors=ANY,
            )

            assert_called_once(
                mocked_sync_single_domain,
                org_id=org_id,
                domain_name=self.domain_name,
            )

    def test_if_not_outer_return_403(self):
        # Внутренний админ не может создать организацию с привязанным доменом
        org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='another',
            domain_part='another.com',
            )
        admin = org['admin_user_uid']
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
             patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):

            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=get_auth_headers(as_uid=admin),
                expected_code=403,
            )

    def test_organization_with_domain_domain_not_validated(self):
        # Cоздаем новую организацию с промки ПДД
        # проверяем что при создани организации в базу добавляется неподтверждённый домен
        # Хотя домен не подтверждён, с организацией уже можно производить некоторые действия,
        # потому что у неё должна была автоматически включиться фича, расзрешающая режим
        # "Яндекс Организации": DIR-6685
        #
        # У организации в метабазе стоит особый признак - has_owned_domains=False
        # Если домен не подтвержден,
        # Признак has_owned_domains в метабазе не меняется
        # Сервисы не появляются
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport'), \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):

            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers
            )
            assert_called(
                self.mocked_blackbox.userinfo,
                3,
            )

        # получили id созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )
        org_id = result['org_id']
        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='pdd_new_promo',
                language='ru',
                label=contains_string(self.domain_name.replace('.', '-')),
                country='ru',
                tld='com',
            )
        )

        self.assertEqual(
            OrganizationModel(self.main_connection).has_owned_domains(org_id),
            False,
        )
        domains = DomainModel(self.main_connection).find({'org_id': org_id})
        assert_that(
            domains,
            has_length(1),
        )
        assert_that(
            domains[0],
            has_entries(
                name=self.domain_name,
                owned=False,
                via_webmaster=True,
                master=False,
                display=False,
            )
        )
        # Проверяем, что применился пресет no-owned-domain
        # Проверяем, что применились пресеты
        org = OrganizationModel(self.main_connection).find(
            {'id': org_id},
            fields=['services.slug'],
            one=True,
        )
        assert_that(
            org['services'],
            contains(
                has_entries(
                    slug=self.dashboard_slug,
                )
            )
        )
        # Проверяем, что нельзя добавить отдел с label
        headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid, org_id=org_id))
        DEPARTMENT_NAME = {
            'ru': 'Департамент',
            'en': 'Department',
        }
        department_data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        self.post_json(
            '/departments/',
            headers=headers,
            data=department_data,
            # Так как домен ещё не подтверждён, то можно попытаться добавить домен,
            # но такая попытка провалится, потому что мы задали login,
            # а рассылку ещё завести нельзя.
            # Прежде мы тут вообще отдавали 403, потому что добавление отделов было запрещено.
            expected_code=422,
            expected_error_code='unable_to_create_maillist',
            expected_message='Unable to create maillist for organization without domains',
        )

        # Подтверждаем домен
        with mocked_blackbox() as blackbox, \
                patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain:
            # Нужно чтобы ручка вернула словарь без ключа errors,
            # тогда ответ будет считаться успешным.
            self.mocked_webmaster_inner_verify.return_value = {}
            # После этого, ручка info должна вернуть, что домен не подтверждён
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

            response = self.post_json(
                '/domains/{0}/check-ownership/'.format(self.domain_name),
                {'verification_type': 'webmaster.dns'},
                headers=headers,
                expected_code=200,
            )

            assert_that(
                response,
                has_entries(
                    domain=self.domain_name,
                    owned=False,
                ),
            )

            assert_called_once(
                self.mocked_webmaster_inner_verify,
                self.domain_name,
                self.admin_uid,
                'DNS',
                ignore_errors=ANY,
            )

        # не пометили домен подтвержденным в своей базе
        domain = DomainModel(self.main_connection).find(
            {'org_id': org_id, 'name': self.domain_name},
            one=True,
        )
        assert_that(
            domain,
            has_entries(
                owned=False,
                master=False,
                display=False,
            )
        )

        # Проверяем, что в базе не поменялся признак has_owned_domains
        self.assertEqual(
            OrganizationModel(self.main_connection).has_owned_domains(org_id),
            False,
        )

        # Проверяем, что пресеты не применились
        org = OrganizationModel(self.main_connection).find(
            {'id': org_id},
            fields=['services.slug'],
            one=True,
        )
        assert_that(
            org['services'],
            not_(
                contains(
                    has_entries(
                        slug=self.service_slug,
                    )
                )
            )
        )

    def test_organization_with_domain_unable_to_create_two_same_domains(self):
        # создаем новую организацию с промки ПДД
        # пытаемся создать организация с таким же доменом ещё раз
        # проверяем, что это запрещено
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):

            self.mocked_webmaster_inner_verify.side_effect = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers
            )

        # получили id созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )
        org_id = result['org_id']
        domains = DomainModel(self.main_connection).find({'org_id': org_id})
        assert_that(
            domains,
            has_length(1),
        )
        assert_that(
            domains[0],
            has_entries(
                name=self.domain_name,
                owned=False,
                via_webmaster=True,
                master=False,
                display=False,
            )
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):

            self.mocked_webmaster_inner_info = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=self.headers,
                expected_code=409,
            )
            assert_that(
                result,
                has_entries(
                    message=contains_string('Domain already added into another your organization'),
                    code='duplicate_domain',
                ),
            )

    def test_organization_with_domain_domain_not_validated_can_be_remove(self):
        # в организации без подтвержденных доменов можно удалять домены
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):

            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            result = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

        self.delete_json('/domains/%s/' % (self.domain_name), as_uid=self.admin_uid, expected_code=200)

    @responses.activate
    def test_organization_with_domain_with_domenator(self):
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        tvm.tickets['gendarme'] = 'tvm-ticket-gendarme'
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []
        from intranet.yandex_directory.src.yandex_directory.core.features import (
            set_feature_value_for_organization,
            USE_DOMENATOR,
        )
        with responses.RequestsMock() as rsps:
            rsps.add(
                responses.POST,
                'https://domenator-test.yandex.net/api/domains/sync-connect',
                body='{}',
                status=200,
                content_type='application/json'
            )
            set_feature_value_for_organization(
                self.meta_connection,
                self.organization['id'],
                USE_DOMENATOR,
                True,
            )

        responses.add(
            responses.POST,
            'https://domenator-test.yandex.net/api/domains/',
            json={},
            status=200,
        )

        self.post_json(
            '/organization/with-domain/',
            data={
                'domain_name': self.domain_name,
                'preset': 'only-the-service',
                'source': 'pdd_new_promo',
                'tld': 'com',
            },
            headers=get_auth_headers(as_uid=self.admin_uid),
        )


class TestOrganizationFeaturesByOrgIdView(TestCase):

    def test_me(self):

        response_data = self.get_json('/organizations/{}/features/'.format(self.organization['id']))
        assert_that(
            response_data,
            equal_to(
                self.features_list
            )
        )


class TestOrganizationFeaturesChangeView(TestCase):
    def test_me(self):
        feature = 'change-organization-owner'
        features = {f_name: f['enabled'] for f_name, f in self.features_list.items()}
        org_id = self.organization['id']

        def check_features():
            for f_name in features:
                assert is_feature_enabled(self.meta_connection, org_id, f_name) == features[f_name]

        check_features()

        self.post_json(f'/organizations/{org_id}/features/{feature}/enable/', data={})
        features[feature] = True
        check_features()

        self.post_json(f'/organizations/{org_id}/features/{feature}/disable/', data={})
        features[feature] = False
        check_features()


class TestOrganizationMeta(TestCase):
    services = {
        'write': None,
        'read': None,
        'none': None
    }

    def setUp(self):
        super(TestOrganizationMeta, self).setUp()
        for slug in self.services:
            self.services[slug] = ServiceModel(self.meta_connection).create(
                slug=slug,
                name=slug,
                client_id=slug + '_client_id',
                scopes=[scope.read_organization, scope.write_organization, scope.work_with_any_organization]
            )

        OrganizationMetaKeysModel(self.meta_connection).create(1, 'meta_key', [self.services['write']['id']], [self.services['read']['id']])

    def test_incorrect_key(self):
        org_id = self.organization['id']
        with auth_as(self.services['write']):
            self.post_json(f'/organizations/{org_id}/meta/invalid_key/', data={'value': "1"}, expected_code=403)
            self.get_json(f'/organizations/{org_id}/meta/invalid_key/', expected_code=403)

    def test_access(self):
        org_id = self.organization['id']
        with auth_as(self.services['none']):
            self.post_json(f'/organizations/{org_id}/meta/meta_key/', data={'value': "1"}, expected_code=403)
            self.get_json(f'/organizations/{org_id}/meta/meta_key/', expected_code=403)
        with auth_as(self.services['read']):
            self.post_json(f'/organizations/{org_id}/meta/meta_key/', data={'value': "1"}, expected_code=403)
            self.get_json(f'/organizations/{org_id}/meta/meta_key/')
        with auth_as(self.services['write']):
            self.post_json(f'/organizations/{org_id}/meta/meta_key/', data={'value': "1"})
            self.get_json(f'/organizations/{org_id}/meta/meta_key/')

    def test_simple(self):
        org_id = self.organization['id']

        def check_value(value):
            self.post_json(f'/organizations/{org_id}/meta/meta_key/', data={'value': value})
            get_value = self.get_json(f'/organizations/{org_id}/meta/meta_key/')
            assert get_value['value'] == value
            assert 'created_at' in get_value
            assert 'updated_at' in get_value

        with auth_as(self.services['write']):
            get_value = self.get_json(f'/organizations/{org_id}/meta/meta_key/')
            assert {'value': None, 'created_at': None, 'updated_at': None} == get_value

            check_value("{\"jsonkey1\":\"jsonvalue1\"}")
            check_value("{\"jsonkey1\":\"jsonvalue2\"}")
            check_value(None)


class TestOrganizationByOrgId__user_count(TestCase):
    api_version = 'v8'

    def setUp(self):
        super(TestOrganizationByOrgId__user_count, self).setUp()
        # включаем сервис, чтобы был робот
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            ready_default=True,
            robot_required=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'],
        )

    def test_get_organization_with_user_count(self):
        # проверим, что в базе админ и робот
        org_id = self.organization['id']
        users = UserModel(self.main_connection).count({'org_id': org_id})
        assert_that(
            users,
            equal_to(1)
        )

        response = self.get_json(
            '/organizations/%s/?fields=id,user_count' % org_id,
        )
        # в самом начале есть только админ
        assert_that(
            response,
            equal_to({
                'id': self.organization['id'],
                'user_count': 1,
            })
        )

        users = [self.create_user() for _ in range(2)]

        response = self.get_json(
            '/organizations/%s/?fields=id,user_count' % org_id,
        )
        # cоздали еще 2 пользователей
        assert_that(
            response,
            equal_to({
                'id': org_id,
                'user_count': 3,
            })
        )

        self.dismiss(org_id, users[0]['id'])
        response = self.get_json(
            '/organizations/%s/?fields=id,user_count' % org_id,
        )
        # одного уволили
        assert_that(
            response,
            equal_to({
                'id': org_id,
                'user_count': 2,
            })
        )


class TestOrganizationsChangeOwnerView(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestOrganizationsChangeOwnerView, self).setUp()
        self.org_id = self.organization['id']
        self.headers = get_auth_headers(as_uid=self.admin_uid)

        self.enable_owner_switching()

        self.another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

    def enable_owner_switching(self, org_id=None):
        feature = get_feature_by_slug(self.meta_connection, CHANGE_ORGANIZATION_OWNER)
        self.set_feature_value_for_organization(feature['id'], enabled=True, org_id=org_id)

    def assert_task_is_none(self):
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()
        assert task is None

    def test_change_owner_to_yandex_user(self):
        # передаем владение внешнему пользователю
        new_owner_uid = 7777
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=new_owner_uid):
            result = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': 'new-ownder@yandex.ru'},
                headers=self.headers,
                expected_code=202,
            )

        # создали задачу
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()

        assert_that(
            task['params'],
            has_entries(
                org_id=self.org_id,
                new_owner_uid=new_owner_uid
            )
        )

        assert_that(
            result,
            has_entries(
                task_id=str(task['id']),
                status='free',
            )
        )

    def test_change_owner_to_inner_user(self):
        # передаем владение пользователю в организации
        new_admin = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=new_admin['id']):
            result = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': new_admin['nickname']},
                headers=self.headers,
                expected_code=202,
            )

        # создали задачу
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()

        assert_that(
            task['params'],
            has_entries(
                org_id=self.org_id,
                new_owner_uid=new_admin['id']
            )
        )

        assert_that(
            result,
            has_entries(
                task_id=str(task['id']),
                status='free',
            )
        )

    def test_change_twice(self):
        # Проверим что в организации без домена
        # Админ 1 может передать владение другой учётке Админ 2
        # И если затем Админ 2 передатс владение Админу 1,
        # то Админ 2 перестанет быть сотрудником, а Админ 1
        # станет внешним админом.

        org_id = self.yandex_organization['id']
        org_model = OrganizationModel(self.main_connection)

        self.enable_owner_switching(org_id)

        # Благодаря миксину у нас уже есть организация без домена и с двумя сотрудниками
        admin1_id = self.yandex_admin['id']
        admin1_nickname = self.yandex_admin['nickname']
        admin2_id = self.yandex_user['id']
        admin2_nickname = self.yandex_user['nickname']

        def change_to(uid, nickname):
            with patch.object(UpdateMembersCountTask, 'delay'):
                with patch.object(SyncExternalIDS, 'delay'):
                    with patch.object(UpdateOrganizationMembersCountTask, 'delay'):
                        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                                    return_value=uid):
                            result = self.post_json(
                                '/organizations/{}/change-owner/'.format(org_id),
                                data={'login': nickname},
                                expected_code=202,
                            )
                            # Каждый раз надо убеждаться, что фоновый таск по смене владельца
                            # отработал без ошибок:
                            self.assert_no_failed_tasks()

        def check_users(*uids):
            users = UserModel(self.main_connection) \
                .filter(org_id=org_id, is_dismissed=False).fields('id') \
                .all()
            assert_that(
                users,
                contains_inanyorder(
                    *(
                        has_entries(id=uid)
                        for uid in uids
                    )
                )
            )

        def check_inner_admins(*uids):
            inner_admins = org_model.get_admins(org_id)
            if uids:
                assert_that(
                    inner_admins,
                    contains_inanyorder(
                        *(
                            has_entries(id=uid)
                            for uid in uids
                        )
                    )
                )
            else:
                assert_that(inner_admins, empty())

        def check_outer_admins(*uids):
            outer_admins = UserMetaModel(self.meta_connection).get_outer_admins(org_id)
            if uids:
                assert_that(
                    outer_admins,
                    contains_inanyorder(
                        *(
                            has_entries(id=uid)
                            for uid in uids
                        )
                    )
                )
            else:
                assert_that(outer_admins, empty())

        def check_owner_is(uid):
            admin_uid = org_model \
                .filter(id=org_id) \
                .scalar('admin_uid')[0]
            assert_that(
                admin_uid,
                equal_to(uid)
            )


        # Изначально, в организации два сотрудника, и Админ 1 является внутренним админом
        check_users(admin1_id, admin2_id)
        check_inner_admins(admin1_id)
        # внешних админов на этом этапе быть не должно
        check_outer_admins()

        # Сменим владельца
        set_auth_uid(admin1_id)
        change_to(admin2_id, admin2_nickname)

        # Оба сотрудника должны остаться в организации
        check_users(admin1_id, admin2_id)
        # Админа 2 должен добавиться, как второй внутренний админ
        check_inner_admins(admin1_id, admin2_id)

        set_auth_uid(admin2_id)
        # Теперь представим, что мы первого админа уволили,
        # а потом вдруг захотели сделать его снова владельцем
        self.dismiss(org_id, admin1_id)
        change_to(admin1_id, admin1_nickname)

        # Убедимся, что в организации сотруднимком остался лишь один Админ 2, первого то уволили
        check_users(admin2_id, admin1_id)
        # и он должен остаться внутренним админом
        check_inner_admins(admin2_id)

        # Однако владельцем организации числится Админ 1
        check_owner_is(admin1_id)

    def test_change_owner_to_user_from_other_org(self):
        # проверяем, что нельзя передавать владение пользователю из другой организации
        user_from_other_org = self.create_user(org_id=self.another_organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=user_from_other_org['id']):
            self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': user_from_other_org['nickname']},
                headers=self.headers,
                expected_code=404,
            )

        self.assert_task_is_none()

    def test_change_owner_to_nonexist_user(self):
        # проверяем, что нельзя передавать владение несуществующему пользователю
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=None):
            self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': 'some_user'},
                headers=self.headers,
                expected_code=404,
            )

        self.assert_task_is_none()

    def test_not_owner_cant_change_owner(self):
        # обычный админ не может менять владельца
        other_admin = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=other_admin['id']
        )

        self.post_json(
            '/organizations/{}/change-owner/'.format(self.org_id),
            data={'login': 'some_user@yandex.ru'},
            headers=get_auth_headers(as_uid=other_admin['id']),
            expected_code=403,
        )

        self.assert_task_is_none()

    def test_change_owner_feature_not_enabled(self):
        # владелец не может самовыпилиться, если фича не включена
        admin_uid = get_organization_admin_uid(self.main_connection, self.another_organization['id'])

        self.post_json(
            '/organizations/{}/change-owner/'.format(self.another_organization['id']),
            data={'login': 'some_user@yandex.ru'},
            headers=get_auth_headers(as_uid=admin_uid),
            expected_code=403,
        )

    def test_change_owner_in_progress(self):
        # если таск уже запущен, нельзя создать еще один
        new_admin = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=new_admin['id']):
            result = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': new_admin['nickname']},
                headers=self.headers,
                expected_code=202,
                process_tasks=False,
            )

        # создали задачу
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()

        assert_that(
            task['params'],
            has_entries(
                org_id=self.org_id,
                new_owner_uid=new_admin['id']
            )
        )

        # пытаемся дернуть ручку еще раз
        result = self.post_json(
            '/organizations/{}/change-owner/'.format(self.org_id),
            data={'login': 'new-admin@yandex.ru'},
            headers=self.headers,
            expected_code=202,
            process_tasks=False,
        )

        # в ответе id первого таска
        assert_that(
            result,
            has_entries(
                task_id=str(task['id']),
                status='free',
            )
        )

        assert_that(
            TaskModel(self.main_connection).filter(
                task_name=ChangeOrganizationOwnerTask.get_task_name(),
            ).count(),
            equal_to(1)
        )

    def test_multiple_changes(self):
        # если первый таск завершился, можно создать еще один
        new_admin = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=new_admin['id']), \
             patch('intranet.yandex_directory.src.yandex_directory.core.utils.tasks.get_domain_info_from_blackbox',
                   return_value={'admin_id': 123, 'domain_id': 123}):
            result = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': new_admin['nickname']},
                headers=self.headers,
                expected_code=202,
            )

        # задача завершилась
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()

        assert_that(
            task['state'],
            equal_to('success')
        )

        # пытаемся передать организацию еще раз
        another_admin = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=another_admin['id']):
            second_result = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': another_admin['nickname']},
                headers=get_auth_headers(as_uid=new_admin['id']),
                expected_code=202,
            )

        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).order_by('-created_at').one()

        assert str(task['id']) == second_result['task_id']

    def test_get_current_task(self):
        new_admin = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.get_user_id_from_passport_by_login',
                   return_value=new_admin['id']):
            result_post = self.post_json(
                '/organizations/{}/change-owner/'.format(self.org_id),
                data={'login': new_admin['nickname']},
                headers=self.headers,
                expected_code=202,
            )

        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()

        result_get = self.get_json(
            '/organizations/{}/change-owner/?task_id={}'.format(self.org_id, result_post['task_id']),
            headers=self.headers
        )

        assert result_get['task_id'] == str(task['id'])
        assert result_get['status'] == task['state']


class TestOrganizations___get_10(TestCase):
    api_version = 'v10'

    def test_with_x_uid_in_the_header(self):
        # в get ручке для получения списка организаций пользователя добавлена пагинация
        org_ids = []
        outer_uid = 777777

        for i in range(5):
            organization = create_organization(
                self.meta_connection,
                self.main_connection,
                label='org-{}'.format(i),
                admin_uid=outer_uid
            )['organization']
            org_ids.append(organization['id'])

        auth_headers = get_auth_headers(as_uid=outer_uid)
        response, response_headers = self.get_json(
            '/organizations/?per_page=3',
            headers=auth_headers,
            return_headers=True,
        )

        # Первая страница должна содержать только 3 организации и
        # ссылку на следующую страницу
        assert_that(
            response,
            has_entries(
                result=contains(
                    *(
                        has_entries(id=org_id)
                        for org_id in org_ids[:3]
                    )
                ),
                links=has_entries(
                    next=not_none()
                ),
                total=5,
            ),
        )

        # В ответе должна быть ссылка на следующую страницу.
        next_page = response['links']['next']

        assert_that(
            next_page,
            equal_to(
                '{}v10/organizations/?page=2&per_page=3'.format(app.config['SITE_BASE_URI'])
            )
        )

        # Так же, в заголовках ответа, должен быть соотвествующий заголовок Link
        assert_that(
            response_headers,
            has_entries(
                Link='<{next_page}>; rel="next"'.format(next_page=next_page),
            )
        )


class TestWithRegistrarCase(TestCase):
    registrar_id = 123
    pdd_id = 1
    pdd_version = 'new'

    def get_registrar_data(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None):
        return {
            'password': 'p$wd',
            'id': registrar_id or self.registrar_id,
            'pdd_id': pdd_id or self.pdd_id,
            'name': 'Name',
            'admin_id': admin_id or self.admin_uid,
            'pdd_version': pdd_version or self.pdd_version,
            'oauth_client_id': '321',
            'validate_domain_url': 'http://validate_domain_url.com/',
            'domain_added_callback_url': 'http://domain_added_callback_url.com/',
            'domain_verified_callback_url': 'http://domain_verified_callback_url.com/',
            'domain_deleted_callback_url': 'http://domain_deleted_callback_url.com/',
            'payed_url': 'http://validate_domain_url.com/',
            'added_init': 'http://domain_added_callback_url.com/',
            'added': 'http://domain_verified_callback_url.com/',
            'delete_url': 'http://domain_deleted_callback_url.com/',
        }

    def add_domenator_registrar_response(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None):
        registrar_id = registrar_id or self.registrar_id
        registrar_data = self.get_registrar_data(pdd_id, pdd_version, admin_id, registrar_id)
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            json=registrar_data,
        )

    def add_domenator_registrar_not_found_response(self, registrar_id):
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=404,
        )

    def setUp(self):
        super(TestWithRegistrarCase, self).setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        self.registrar = self.get_registrar_data()
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []


class TestOrganizationFromRegistrar__post(TestWithRegistrarCase):
    def setUp(self):
        super(TestOrganizationFromRegistrar__post, self).setUp()

        # id админа организации в паспорте (создан при регистрации через паспорт)
        self.admin_uid = 1
        self.domain_name = 'test-domain.qqq'
        # домен новой организации
        self.domain = 'test-org{}'.format(app.config['DOMAIN_PART'])
        # userinfo из ЧЯ
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'admin',
            },
            'uid': self.admin_uid,
            'default_email': 'default@ya.ru',
        }
        # Сделаем сервис dashboard, который надо включить до подтверждения домена
        self.dashboard_slug = 'dashboard'
        ServiceModel(self.meta_connection).create(
            slug=self.dashboard_slug,
            name='Dashboard',
        )

        # И пресет no-owned-domain с сервисов dashboard
        PresetModel(self.meta_connection).create(
            'no-owned-domain',
            service_slugs=[self.dashboard_slug],
            settings={}
        )

        self.headers = get_auth_headers(as_outer_admin=dict(id=self.admin_uid))

    @responses.activate
    def test_create_org_from_registrar(self):
        self.mocked_blackbox.userinfo.reset_mock()

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch.object(component_registry(), 'meta_session', Session(self.meta_connection)), \
                patch('intranet.yandex_directory.src.yandex_directory.core.registrar.tasks.webmaster.info') as webmaster_info, \
                patch('intranet.yandex_directory.src.yandex_directory.core.registrar.tasks.RegistrarClient.call_added_init_registrar_callback') as \
                        call_added_init_registrar_callback:
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

            webmaster_verification_uin = 'verification_uin_1'
            webmaster_info.return_value = {'data': {'verificationUin': webmaster_verification_uin}}

            self.add_domenator_registrar_response()

            result = self.post_json(
                '/organization/from-registrar/{}/'.format(self.registrar_id),
                data={
                    'domain_name': self.domain_name,
                    'tld': 'com',
                },
                headers=self.headers,
            )
            org_id = result['org_id']

            assert_called(
                self.mocked_blackbox.userinfo,
                3,
            )

            assert_called_once(
                self.mocked_webmaster_inner_add,
                self.domain_name,
                self.admin_uid,
                ignore_errors=ANY,
            )

            assert_called_once(
                call_added_init_registrar_callback,
                self.domain_name,
                webmaster_verification_uin,
            )

        # получили id созданной организации
        assert_that(
            result,
            has_entries(
                org_id=not_none(),
            )
        )

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                source='registrar',
                language='ru',
                label=contains_string(self.domain_name.replace('.', '-')),
                country='ru',
                tld='com',
                registrar_id=self.registrar_id,
            )
        )

        # ручка organizations тоже отдает поле registrar_id
        result_org = self.get_json(
            '/v6/organizations/{}/?fields=id,registrar_id'.format(org_id),
            headers=self.headers,
        )
        assert_that(
            result_org,
            has_entries(
                id=org_id,
                registrar_id=self.registrar_id,
            )
        )

        # записали в лог попытку подтверждения домена
        webmaster_log = WebmasterDomainLogModel(self.main_connection).filter(org_id=org_id, action='verify').one()
        assert_that(
            webmaster_log,
            has_entries(
                name=self.domain_name,
                verification_type='PDD_EMU',
                uid=self.admin_uid,
            )
        )

    @responses.activate
    def test_create_with_incorrect_registrar(self):
        self.mocked_blackbox.userinfo.reset_mock()

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

            registrar_id = 444444444
            self.add_domenator_registrar_not_found_response(registrar_id)

            result = self.post_json(
                '/organization/from-registrar/{}/'.format(registrar_id),
                data={
                    'domain_name': self.domain_name,
                    'tld': 'com',
                },
                headers=self.headers,
                expected_code=404,
            )

    @responses.activate
    def test_create_with_russian_domain(self):
        # проверяем, что дергаем колбеки с доменом в пуникоде
        self.mocked_blackbox.userinfo.reset_mock()
        russian_domain = 'капустка.рф'

        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)), \
                patch('intranet.yandex_directory.src.yandex_directory.core.registrar.tasks.webmaster.info') as webmaster_info:
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

            webmaster_verification_uin = 'verification_uin_1'
            webmaster_info.return_value = {'data': {'verificationUin': webmaster_verification_uin}}

            self.add_domenator_registrar_response()

            result = self.post_json(
                '/organization/from-registrar/{}/'.format(self.registrar_id),
                data={
                    'domain_name': russian_domain,
                    'tld': 'com',
                },
                headers=self.headers,
            )

            assert_that(
                self.mocked_zora_client.get.call_args_list[0][0],
                equal_to(
                    (self.registrar['domain_added_callback_url'],)
                )
            )
            assert_that(
                self.mocked_zora_client.get.call_args_list[0][1],
                has_entries(
                    params=has_entries(
                        domain=to_punycode(russian_domain),
                        secret_name=not_none(),
                        secret_value=not_none(),
                        sign=not_none(),
                    )
                )
            )


class TestOrganizationFromRegistrar__delete(TestWithRegistrarCase):
    def setUp(self):
        super(TestOrganizationFromRegistrar__delete, self).setUp()
        OrganizationModel(self.main_connection).filter(
            id=self.organization['id']
        ).update(
            registrar_id=self.registrar_id
        )

    @responses.activate
    def test_delete_org(self):
        # проверяем, что дергаем колбек регистратора при удалении организации
        master_domain = DomainModel(self.main_connection).get_master(self.organization['id'])
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)), \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox',
                      return_value={
                          'admin_id': self.admin_uid,
                          'master_domain': None,
                          'domain_id': 777777,
                          'blocked': False,
                      }):
            self.mocked_blackbox.account_uids.return_value = [self.admin_uid]

            registrar_data = self.get_registrar_data()
            self.add_domenator_registrar_response()

            self.delete_json(
                '/organizations/%s/' % self.organization['id'],
                expected_code=204,
            )

        assert_that(
            self.mocked_zora_client.get.call_args_list[0][0],
            equal_to(
                (registrar_data['domain_deleted_callback_url'],)
            )
        )
        assert_that(
            self.mocked_zora_client.get.call_args_list[0][1],
            has_entries(
                params=has_entries(
                    domain=master_domain['name'],
                    sign=not_none(),
                )
            )
        )


class TestOrganizationSubscriptionPersons(TestCase):
    def test_get(self):
        mocked_xmlrpc = Mock()
        expected_persons = [
            {'client_id': 34444064,
             'person_id': 8692188,
             'type': 'natural',
             'email': 'some_email@mail.com',
             'first_name': u'Иван',
             'last_name': u'Иванов',
             'middle_name': u'Иванович',
             'phone': '+79851000000'},
            {'client_id': 34444064,
             'person_id': 6533006,
             'type': 'legal',
             'inn': '7839095666',
             'kpp': '783906666',
             'bik': '044525666',
             'account': '40000810110000000489',
             'email': 'some_email@mail.com',
             'legal_address': u'г. Санкт-Петербург, 100-100, лит. Ж',
             'long_name': u'ООО "Компания"',
             'phone': '8 (800) 555-66-66',
             'postal_code': '196211',
             'postal_address': u'г. Санкт-Петербург, 100-100, лит. Ж'},
        ]
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': 11111}
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': '444444',
            'SERVICES': [9000],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetClientPersons.return_value = [
             {'CLIENT_ID': '34444064',
              'DT': '2019-10-25 18:03:28',
              'EMAIL': 'some_email@mail.com',
              'FNAME': u'Иван',
              'ID': '8692188',
              'LNAME': u'Иванов',
              'MNAME': u'Иванович',
              'NAME': u'Иванов Иван Иванович',
              'PHONE': '+79851000000',
              'REGION': '2',
              'TYPE': 'ph'},
             {'ACCOUNT': '40000810110000000489',
              'BIK': '044525666',
              'CLIENT_ID': '34444064',
              'DT': '2018-11-01 16:30:52',
              'EMAIL': 'some_email@mail.com',
              'ID': '6533006',
              'INN': '7839095666',
              'KPP': '783906666',
              'LEGALADDRESS': u'г. Санкт-Петербург, 100-100, лит. Ж',
              'LONGNAME': u'ООО "Компания"',
              'NAME': 'my cool company',
              'PHONE': '8 (800) 555-66-66',
              'POSTADDRESS': u'г. Санкт-Петербург, 100-100, лит. Ж',
              'POSTCODE': '196211',
              'TYPE': 'ur'}
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/persons/')

        assert response == expected_persons


class TestOrganizationInvoiceView(TestCase):

    def setUp(self):
        super(TestOrganizationInvoiceView, self).setUp()
        self.enable_paid_mode()
        tvm.tickets['billing_invoice'] = 'some_ticket'

    @responses.activate
    def test_get_pdf_success(self):
        invoice_id = 'hello_world'
        pdf_form = 'pdf invoice'
        responses.add(
            responses.GET,
            f'{app.config["BILLING_INVOICE_API_URL"]}documents/invoices/{invoice_id}',
            json={
                "content_type": "application/pdf",
                "filename": "\u0411-40827387-1.pdf",
                "mds_link": f"http://s3.mdst.yandex.net/balance/invoices_{invoice_id}.pdf"
            },
        )
        organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(org_id=self.organization['id'])
        mocked_xmlrpc = Mock()
        request_id = 1234
        contract_id = 123
        mocked_xmlrpc.Balance.GetRequestChoices.return_value = {
            'pcp_list': [
                {
                    'paysyses': [{
                        'payment_method_code': 'not bank',
                        'id': 6,
                    }],
                    'person': {'id': organization_billing_info['person_id']},
                    'contract': {'id': contract_id},
                },

                {
                    'paysyses': [{
                        'payment_method_code': 'bank',
                        'id': 5,
                    }],
                    'person': {'id': organization_billing_info['person_id']},
                    'contract': {'id': contract_id}
                },
            ]
        }
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'RequestID': request_id}
        mocked_xmlrpc.Balance.CreateInvoice.return_value = invoice_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with patch.object(app.billing_client, 'get_passport_by_uid') as mocked_get_passport:
                mocked_get_passport.return_value = {'ClientId': organization_billing_info['client_id']}
                with patch.object(MdsS3ApiClient, 'get_object_content') as mocked_s3api:
                    mocked_s3api.return_value = pdf_form
                    response, headers = self.post_json(
                        '/subscription/invoice/',
                        data={'amount': 100},
                        json_response=False,
                    )

                mocked_s3api.assert_called_once_with(
                    bucket='balance',
                    object_id=f'invoices_{invoice_id}.pdf'
                )

        assert_that(
            response,
            equal_to(pdf_form.encode('utf-8')),
        )

        assert_that(
            headers['Content-Disposition'],
            equal_to('attachment;filename=invoice.pdf'),
        )

        assert_that(
            headers['Content-Type'],
            equal_to('application/pdf; charset=utf-8'),
        )


class TestOrganizationProxy(TestCase):
    def setUp(self):
        super().setUp()
        self.org_admin_uid = 1112
        self.org = self.create_organization(admin_uid=self.org_admin_uid)
        self.auth_headers = get_auth_headers(as_org=self.org['id'])
        set_feature_value_for_organization(
            self.meta_connection,
            self.org['id'],
            USE_CLOUD_PROXY,
            True,
        )

    def test_disable_proxy(self):
        self.auth_headers['X-DISABLE-CLOUD-PROXY'] = 'true'
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations:
            self.get_json('v6/organizations/', headers=self.auth_headers)
            list_organizations.assert_not_called()

    def test_patch_org(self):
        OrganizationMetaModel(self.meta_connection).update(
            filter_data={'id': self.org['id']},
            update_data={'cloud_org_id': 'some_org_id'}
        )
        auth_headers = get_auth_headers(as_uid=self.org_admin_uid)
        update_data = {'description': 'some_new', 'name': 'new_name'}
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.update_organization') as update_organization:
            with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.MessageToDict', dict):
                update_organization_response = {
                    'response':{
                        'id': 'smth',
                        'name': 'new_name',
                        'description': 'some_new',
                    }
                }
                update_organization.return_value = update_organization_response

                response_data = self.patch_json(
                    '/v6/organization/',
                    headers=auth_headers,
                    data=update_data,
                )
                update_organization.assert_called_once_with(org_id='some_org_id', data=update_data)

        self.assertEqual(
            response_data,
            {'id': 'smth', 'name': 'new_name', 'description': 'some_new'}
        )
        organization = OrganizationModel(self.main_connection).get(self.org['id'], fields=['name'])
        self.assertNotEqual(organization['name'], update_data['name'])

    def test_get_org(self):
        OrganizationMetaModel(self.meta_connection).update(
            filter_data={'id': self.org['id']},
            update_data={'cloud_org_id': 'some_org_id'}
        )
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.get_organization') as get_organization:
            with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.MessageToDict', dict):
                get_organization_response = {
                            'id': 'smth',
                            'name': 'test',
                            'description': 'smth desc',
                }
                get_organization.return_value = get_organization_response

                response_data = self.get_json('v6/organizations/{}/'.format(self.org['id']), headers=self.auth_headers)
                get_organization.assert_called_once_with(org_id='some_org_id')
        self.assertEqual(
            response_data,
            {'id': 'smth', 'name': 'test', 'description': 'smth desc'}
        )

    def test_get_orgs(self):

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations:
            with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.MessageToDict', MockToDict):
                list_organizations_response = self.get_dict_object({
                    'next_page_token': None,
                    'organizations': [
                        {
                            'id': 'smth',
                            'name': 'test',
                            'description': 'smth desc',
                        },
                        {
                            'id': 'smth-1',
                            'name': 'test-1',
                            'description': 'smth desc-1',
                        }
                    ]
                })
                list_organizations.return_value = list_organizations_response

                response_data = self.get_json('v6/organizations/', headers=self.auth_headers)

        self.assertEqual(
            response_data,
            {'result': [{'id': 'smth', 'name': 'test', 'description': 'smth desc'},
                        {'id': 'smth-1', 'name': 'test-1', 'description': 'smth desc-1'}],
             'links': {}
             }
        )

    def test_get_orgs_pagination(self):
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations:
            with patch('intranet.yandex_directory.src.yandex_directory.core.views.organization.view.MessageToDict',
                       MockToDict):
                list_organizations_response = self.get_dict_object({
                    'next_page_token': None,
                    'organizations': [
                        {
                            'id': 'smth',
                            'name': 'test',
                            'description': 'smth desc',
                        },
                        {
                            'id': 'smth-1',
                            'name': 'test-1',
                            'description': 'smth desc-1',
                        }
                    ]
                })
                list_organizations.return_value = list_organizations_response

                response_data = self.get_json('v6/organizations/?per_page=1', headers=self.auth_headers)
                response_data_page2 = self.get_json('v6/organizations/?per_page=1&page=2', headers=self.auth_headers)

        self.assertEqual(
            response_data,
            {'result': [{'id': 'smth', 'name': 'test', 'description': 'smth desc'}],
             'links': {'next': 'http://dir.yd-dev.cmail.yandex.net/v6/organizations/?page=2&per_page=1'}}
        )
        self.assertEqual(
            response_data_page2,
            {'result': [{'id': 'smth-1', 'name': 'test-1', 'description': 'smth desc-1'}],
             'links': {}}
        )

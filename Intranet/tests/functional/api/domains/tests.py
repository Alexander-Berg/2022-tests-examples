# -*- coding: utf-8 -*-
import json
import responses
from hamcrest import (
    assert_that,
    has_entries,
    has_entry,
    equal_to,
    contains,
    contains_string,
    none,
    contains_inanyorder,
    has_key,
    has_length,
    not_none,
)
from unittest.mock import (
    patch,
    ANY,
    Mock,
)
from sqlalchemy.orm import Session

from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client
from intranet.yandex_directory.src.yandex_directory.core.models import (
    PresetModel, OrganizationServiceModel, DepartmentModel, ServiceModel
)
from .... import webmaster_responses
from testutils import (
    TestCase,
    get_auth_headers,
    get_oauth_headers,
    oauth_client,
    create_organization,
    create_user,
    override_settings,
    assert_not_called,
    assert_called_once,
    set_auth_uid,
    create_outer_admin,
    create_service,
    create_organization_without_domain,
    scopes,
    mocked_requests,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    lstring,
    utcnow,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    WebmasterDomainLogModel,
    OrganizationMetaModel,
    OrganizationModel,
    UserMetaModel,
)

from intranet.yandex_directory.src.yandex_directory.core.models.domain import DomainModel
from intranet.yandex_directory.src.yandex_directory.core.models.domain import domain_action
from intranet.yandex_directory.src.yandex_directory.core.registrar import PlainToken, CryptToken
from intranet.yandex_directory.src.yandex_directory.core.tasks import SyncSingleDomainTask
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    get_organization_admin_uid,
    prepare_fields,
)
from intranet.yandex_directory.src.yandex_directory.meta.repositories import DomainTokenRepository
from intranet.yandex_directory.src.yandex_directory.meta.models import DomainToken
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import (
    Command as UpdateServicesInShards
)


class TestDomainList__post(TestCase):
    # https://st.yandex-team.ru/DIR-1638

    label = 'не-тим'

    def setUp(self):
        super(TestDomainList__post, self).setUp()
        self.pdd_admin_uid = get_organization_admin_uid(
            self.main_connection,
            self.organization['id'],
        )
        self.new_domain = 'прИМЕР.рф'
        self.new_domain_lower_case = self.new_domain.lower()
        self.new_domain_in_punycode = self.new_domain_lower_case.encode('idna')
        self.master_domain = self.label + self.domain_part
        self.master_domain_in_punycode = self.master_domain.encode('idna')

        self.second_admin = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=101,
            nickname='petya',
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            org_id=self.organization['id'],
            department_id=1,
        )
        UserModel(self.main_connection).make_admin_of_organization(self.organization['id'], self.second_admin['id'])
        set_auth_uid(self.second_admin['id'])
        self.organization_without_master_domain = self.create_organization_without_master_domain()

        self.another_sso_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

    def create_organization_without_master_domain(self):
        organization_meta_instance = OrganizationMetaModel(self.meta_connection).create(
            label='org_without_domain',
            shard=1,
        )
        org_id = organization_meta_instance['id']

        # создаем внешнего админа
        UserMetaModel(self.meta_connection).create(
            id=self.outer_admin['id'],
            org_id=org_id,
        )
        # создаём организацию в базе
        OrganizationModel(self.main_connection).create(
            id=org_id,
            name='org_without_domain',
            label='org_without_domain',
            admin_uid=self.outer_admin['id'],
            language='ru',
            source='test',
            tld='ru',
            country='ru',
            maillist_type='inbox',
            preset='default',
        )
        return org_id

    def test_add_domain_verified_by_webmaster(self):
        # Проверяем, что если домен подтверждён в Вебмастере, то мы должны:
        # * добавить его в базу директории со статусом owned=True;
        # * добавить в паспорт
        data = {
            'name': self.new_domain,
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.views.create_domain_verified_via_webmaster') as mocked_create_domain:
            response = self.post_json('/domains/', data)

        assert_that(
            response,
            has_entries(
                name=self.new_domain_lower_case,
                owned=False,
            )
        )
        assert_called_once(
            mocked_create_domain,
            ANY,
            ANY,
            self.new_domain,
            self.organization['id'],
        )

    def test_add_domain_to_sso_org(self):
        domain_name = 'test.com'

        admin_uid = self.another_sso_org["admin_uid"]
        headers = get_auth_headers(as_uid=admin_uid)

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json(
                '/domains/',
                data={
                    'name': domain_name,
                },
                headers=headers,
                expected_code=403
            )

    def test_add_domain_which_already_exist_as_not_owned(self):
        # проверяем, что если домен не подтвержден, но уже добавлен какому-то админу,
        # то другой админ все еще может добавить к себе этот домен и он будет висеть в статусе not owned
        # и этот домен успешно добавится в базу директории

        data = {
            'name': self.new_domain,
        }
        admin_outer_uid1 = 11111
        admin_outer_uid2 = 22222

        organization_info1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='another',
            domain_part='another.com',
            admin_uid=admin_outer_uid1,
        )

        organization_info2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='theother',
            domain_part='theother.com',
            admin_uid=admin_outer_uid2,
        )

        org_id1 = organization_info1['organization']['id']
        org_id2 = organization_info2['organization']['id']

        headers = get_auth_headers(as_outer_admin=dict(id=admin_outer_uid1, org_id=org_id1))
        self.post_json('/domains/', data, headers=headers)

        headers = get_auth_headers(as_outer_admin=dict(id=admin_outer_uid2, org_id=org_id2))
        self.post_json('/domains/', data, headers=headers)

        domains = DomainModel(self.main_connection).find({'name': self.new_domain_lower_case})
        # проверим, что домен был добавлен в базу Директории
        assert_that(
            domains,
            contains_inanyorder(
                has_entries(
                    name=self.new_domain_lower_case,
                    owned=False,
                    org_id=org_id1,
                    validated=False,
                    master=False,
                    delegated=False,
                    mx=False,
                ),
                has_entries(
                    name=self.new_domain_lower_case,
                    owned=False,
                    org_id=org_id2,
                    validated=False,
                    master=False,
                    delegated=False,
                    mx=False,
                ),
            )
        )

    def test_add_exists_domain(self):
        # добавляем уже добавленный домен
        DomainModel(self.main_connection).create(self.new_domain, self.organization['id'])

        data = {
            'name': self.new_domain,
        }
        response = self.post_json('/domains/', data, expected_code=409)
        assert_that(
            response,
            has_entries(
                code='duplicate_domain',
                message='Domain already added',
            )
        )

    def test_long_domain_name(self):
        # имя домена слишком длинное (проблемы при переводе в pynicode)

        data = {
            'name': 'ыоваролыраоырварыоаролывраолрыволралфдраолфралофралорфолар.рф',
        }

        response = self.post_json('/domains/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='domain_too_long',
                message='Domain name is too long',
            )
        )

    def test_domain_name_without_dot(self):
        # имя домена не сожержит точку

        data = {
            'name': 'anton',
        }

        response = self.post_json('/domains/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='invalid_domain',
                message='Invalid domain',
            )
        )

    def test_add_domain_exist_in_neighbour_organization(self):
        # Кейс: админ добавил неподтвержденный домен к одной своей организации, затем пошел в другую свою
        # организацию.проверяем, что он не сможет добавить этот домен для другой своей организации.

        admin_outer_uid = 11111
        tested_domain = 'double.add.com'

        organization_info1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='another',
            domain_part='another.com',
            admin_uid=admin_outer_uid,
        )

        organization_info2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='theother',
            domain_part='theother.com',
            admin_uid=admin_outer_uid,
        )

        org_id1 = organization_info1['organization']['id']
        org_id2 = organization_info2['organization']['id']

        data = {
            'name': tested_domain
        }
        headers = get_auth_headers(as_outer_admin=dict(id=admin_outer_uid, org_id=org_id1))
        self.post_json('/domains/', data, headers=headers)

        headers = get_auth_headers(as_outer_admin=dict(id=admin_outer_uid, org_id=org_id2))
        response = self.post_json('/domains/', data, headers=headers, expected_code=409)

        assert_that(
            response,
            has_entries(
                code='duplicate_domain',
                message=contains_string('Domain already added into another your organization'),
                params=has_key('conflicting_org_id'),
            )
        )

    def test_add_domain_with_tech_part(self):
        # нельзя добавлять домены в технической доменной зоне
        data = {
            'name': 'custom-part' + app.config['DOMAIN_PART'],
        }

        response = self.post_json('/domains/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='domain_is_tech',
                message='Can\'t add domain with this name',
            )
        )

    def test_not_in_connect_but_in_passport_same_admin(self):
        # проверим, что, если домен не коннектный, но есть в паспорте у того же админа,
        # и на нем нет учеток
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            self.mocked_blackbox.account_uids.return_value = []

            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'master_domain': None,
                'aliases': [],
            }
            data = {
                'name': 'super_domain.com',
            }
            self.post_json('/domains/', data, expected_code=201)

    def test_not_in_connect_but_in_passport_same_admin_has_alias(self):
        # проверим, что, если домен не коннектный, но есть в паспорте у того же админа,
        # и у него есть алиасы
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:

            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'master_domain': None,
                'aliases': ['alias.com']
            }
            data = {
                'name': 'super_domain.com',
            }
            response = self.post_json('/domains/', data, expected_code=422)
            assert_that(
                response,
                has_entries(
                    code='domain_has_aliases',
                )
            )

    def test_not_in_connect_but_in_passport_same_admin_with_accounts(self):
        # проверим, что, если домен не коннектный, но есть в паспорте у того же админа,
        # и на нем есть учетки
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            self.mocked_blackbox.account_uids.return_value = [123]

            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'master_domain': None,
                'aliases': [],
            }
            data = {
                'name': 'super_domain.com',
            }
            response = self.post_json('/domains/', data, expected_code=422)
            assert_that(
                response,
                has_entries(
                    code='domain_has_accounts',
                )
            )

    def test_not_in_connect_but_as_alias_in_passport_same(self):
        # проверим, что, если домен не коннектный, но есть в паспорте у того же админа,
        # и это алиас
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:

            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'master_domain': 'master.com',
            }
            data = {
                'name': 'super_domain.com',
            }
            response = self.post_json('/domains/', data, expected_code=422)
            assert_that(
                response,
                has_entries(
                    code='domain_is_alias',
                )
            )

    def test_not_in_connect_but_in_passport_other_admin(self):
        # проверим, что, если домен не коннектный, но есть в паспорте у другого админа,
        # то возвращается ошибка о том, что домен уже подтвержден в паспорте
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.common.utils.safe_delete_domain_in_passport') as safe_delete_domain_in_passport:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': 0,
            }
            safe_delete_domain_in_passport.return_value = False
            data = {
                'name': 'super_domain.com',
            }
            response = self.post_json('/domains/', data, expected_code=422)
            assert_that(
                response,
                has_entries(
                    code='domain_exists_in_passport',
                )
            )


class TestDomainsSearch__get(TestCase):
    api_version = 'v11'

    def setUp(self):
        super(TestDomainsSearch__get, self).setUp()
        self.org_id = self.organization['id']
        self.domain_name = self.organization_domain

    def test_one_domain(self):
        response = self.post_json(
            '/domains/search/',
            {
                'fields': ['org_id'],
                'domains': [self.domain_name],
            },
            expected_code=200
        )

        assert response == {
            self.domain_name: [{'org_id': self.org_id}],
        }

    def test_one_domain_without_fields(self):
        response = self.post_json(
            '/domains/search/',
            {
                'domains': [self.domain_name],
            },
            expected_code=200
        )

        assert response == {
            self.domain_name: [{'org_id': self.org_id}],
        }

    def test_notexists_domain(self):
        response = self.post_json(
            '/domains/search/',
            {
                'fields': ['org_id'],
                'domains': ['notexists_domain.com'],
            },
            expected_code=200
        )

        assert response == {}

    def test_empty_domains(self):
        self.post_json(
            '/domains/search/',
            {
                'fields': ['org_id'],
                'domains': [],
            },
            expected_code=422
        )

    def test_incorrect_fields(self):
        self.post_json(
            '/domains/search/',
            {
                'fields': ['org_id', 'incorrect_fields'],
                'domains': [self.domain_name],
            },
            expected_code=422
        )


class TestDomainList__get_6(TestCase):
    api_version = 'v6'

    def setUp(self):
        super(TestDomainList__get_6, self).setUp()
        self.org_id = self.organization['id']
        self.pdd_admin_uid = get_organization_admin_uid(
            self.main_connection,
            self.org_id,
        )
        self.expected_keys = [
            'name', 'master', 'owned', 'mx', 'tech',
            'delegated', 'domain_id', 'can_users_change_password', 'pop_enabled',
            'imap_enabled', 'postmaster_uid', 'org_id', 'country',
        ]

    def test_fields_returned_by_list_view(self):
        tvm.tickets['gendarme'] = 'gendarme-ticket'

        # Проверяем, что ручка возвращает все перечисленные поля.
        fields = list(self.expected_keys)

        domain_model = DomainModel(self.main_connection)
        domain = 'хороший-домен.рф'

        domain_model.create(domain, self.org_id, owned=True)

        headers = get_auth_headers(as_org=self.org_id)

        with patch('intranet.yandex_directory.src.yandex_directory.gendarme.status') as gendarme_status:
            gendarme_status.return_value = {
                'mx': {
                    'match': True,
                },
                'ns': {
                    'match': True,
                }
            }
            response = self.get_json('/domains/',
                                     headers=headers,
                                     query_string={
                                         'fields': ','.join(map(str, fields))
                                     },
                                     )
            assert_that(
                list(response[0].keys()),
                contains_inanyorder(*fields),
            )

            # Проверяем каждое поле по отдельности.
            for field in fields:
                response = self.get_json('/domains/',
                                         headers=headers,
                                         query_string={
                                             'fields': field,
                                         })

                assert_that(response[0], has_key(field))

            # при пустом fields мы должны вернуть только name
            response = self.get_json(
                '/domains/',
                headers=headers
            )
            assert_that(
                response[0],
                has_key('name')
            )

    def test_work_with_outer_admin(self):
        domain_model = DomainModel(self.main_connection)
        domain = 'хороший-домен.рф'
        admin_uid = 111111111
        for _ in range(50):
            create_organization(
                self.meta_connection,
                self.main_connection,
                label='another',
                domain_part='another.com',
                admin_uid=admin_uid,
            )

        organization_info1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='another',
            domain_part='another.com',
            admin_uid=admin_uid,
        )
        domain_model.create(domain, organization_info1['organization']['id'], owned=True)

        headers = get_auth_headers(as_outer_admin=dict(id=admin_uid))
        response = self.get_json(
            '/domains/',
            headers=headers,
            query_string={
                'name': domain,
            },
            expected_code=200,
            check_revision=False,
        )
        self.assertEqual(response, [{'name': 'хороший-домен.рф', 'org_id': organization_info1['organization']['id']}])


    def test_wrong_fields_returned_by_list_view(self):
        # Проверяем, что ручка выдаёт ошибку при указании неподдерживаемого поля.
        # И возвращает список поддерживаемых полей.
        fields = ['wrong_field', 'name', 'tech']

        domain_model = DomainModel(self.main_connection)
        domain = 'хороший-домен.рф'

        domain_model.create(domain, self.org_id, owned=True)
        headers = get_auth_headers(as_org=self.org_id)

        response = self.get_json('/domains/',
                                 headers=headers,
                                 query_string={
                                     'fields': ','.join(map(str, fields))
                                 },
                                 expected_code=422,
                                 )

        assert_that(
            response['params']['field'],
            equal_to('wrong_field')
        )
        assert_that(
            sorted(prepare_fields(response['params']['supported_fields'], default_fields=['name'])),
            equal_to(sorted(self.expected_keys))
        )

    def test_with_domain_and_x_uid(self):
        # DIR-5369 Проверяем поиск доменов, если не известен org_id, а только domain + X-uid
        # Проверяем, что ручка возвращает все перечисленные поля.
        fields = list(self.expected_keys)

        domain_model = DomainModel(self.main_connection)
        domain = 'хороший-домен.рф'

        # создаем две организации с одним и тем же внешним админом,
        # так как если организация одна, то ее id прописывается в g.org_id
        outer_admin_uid, org_ids, _ = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=2
        )

        domain_model.create(domain, org_ids[0], owned=True)

        headers = get_auth_headers(as_uid=outer_admin_uid)

        with patch('intranet.yandex_directory.src.yandex_directory.gendarme.status') as gendarme_status:
            gendarme_status.return_value = {
                'mx': {
                    'match': True,
                },
                'ns': {
                    'match': True,
                }
            }

            # в следующих запросах не проверяем ревизию организации в заголовках ответа ручки (check_revision=False),
            # так как может возвращаться несколько организаций
            response = self.get_json('/domains/',
                                     headers=headers,
                                     query_string={
                                        'fields': ','.join(map(str, fields)),
                                        'name': domain,
                                     },
                                     check_revision=False,
                                     )
            assert_that(
                list(response[0].keys()),
                contains_inanyorder(*fields),
            )

            # Проверяем каждое поле по отдельности.
            for field in fields:
                response = self.get_json('/domains/',
                                         headers=headers,
                                         query_string={
                                             'fields': field,
                                             'name': domain,
                                         },
                                         check_revision=False,
                                         )

                assert_that(response[0], has_key(field))

            # при пустом fields мы должны вернуть только name
            response = self.get_json(
                '/domains/',
                headers=headers,
                query_string={
                    'name': domain,
                },
                check_revision=False,
            )
            assert_that(
                response[0],
                has_key('name')
            )

    def test_with_not_organization(self):
        # Если нет ни одной организации  у админа вернем пустой список

        domain = 'some.domain.com'

        headers = get_auth_headers(as_uid=88888)  # uid без организации

        response = self.get_json(
            '/domains/',
            headers=headers,
            query_string={
                'name': domain,
            },
        )
        assert_that(
            response,
            has_length(0)
        )

    def test_service_pdd_adapter(self):
        # Проверяем работу ручки, если запрос пришел от ПДД адаптера

        domain_model = DomainModel(self.main_connection)
        domain = 'хороший-домен.рф'

        # создаем две организации с одним и тем же внешним админом,
        # так как если организация одна, то ее id прописывается в g.org_id
        outer_admin_uid, org_ids, _ = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=2
        )

        domain_model.create(domain, org_ids[0], owned=True)
        service = create_service('pdd-adapter')
        headers = get_auth_headers(as_uid=outer_admin_uid, token=service['token'])

        # в следующих запросах не проверяем ревизию организации в заголовках ответа ручки (check_revision=False),
        # так как может возвращаться несколько организаций

        response = self.get_json(
            '/domains/',
            headers=headers,
            query_string={
                'name': domain,
                'fields': 'name,org_id',
            },
            check_revision=False,
        )
        assert_that(
            response,
            contains(
                has_entries(
                    org_id=org_ids[0],
                    name=domain,
                )
            )
        )

    def test_get_by_oauth(self):
        # проверим что при oauth авторизации все ok (Чиним DIR-6142)
        with oauth_client(client_id=None, uid=self.user['id'], scopes=[scope.read_domains]):
            headers = get_oauth_headers()
            response = self.get_json(
                '/domains/',
                headers=headers,
            )
            assert_that(
                response,
                contains_inanyorder(
                    has_entries(
                        name=self.organization_domain,
                    )
                )
            )


class TestDomainDetail__delete(TestCase):

    def test_404(self):
        # удаляемый домен не существует
        self.delete_json('/domains/somedomain.com/', expected_code=404)

    def test_master_domain(self):
        # удаляемый алиас это мастер домен
        result = self.delete_json('/domains/%s/' % self.organization_domain, expected_code=422)
        assert_that(
            result,
            has_entries(
                code='master_domain_cant_be_deleted',
                message='Domain {domain} can\'t be deleted because it is main domain of organization',
                params={'domain': self.organization_domain},
            )
        )

    def test_tech_domain(self):
        # успешно удаляем технический  домен
        tech_domain_name = 'custom-part' + app.config['DOMAIN_PART']
        DomainModel(self.main_connection).create(
            name=tech_domain_name,
            org_id=self.organization['id']
        )

        result = self.delete_json('/domains/%s/' % tech_domain_name, expected_code=200)

        # удалили из базы
        assert_that(
            DomainModel(self.main_connection).get(tech_domain_name, self.organization['id']),
            none()
        )
        # вернули удаленный домен
        assert_that(
            result,
            equal_to({'domain': tech_domain_name})
        )

    def test_delete_alias(self):
        # успешно удаляем алиас
        domain = 'somedomain.ru'
        DomainModel(self.main_connection).create(domain, self.organization['id'], True)

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox, \
            patch('intranet.yandex_directory.src.yandex_directory.core.models.domain.get_domain_id_from_blackbox', return_value=123):
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'domain_id': 123,
            }
            result = self.delete_json('/domains/%s/' % domain, expected_code=200)

        assert_called_once(
            self.mocked_passport.domain_alias_delete,
            ANY,
            123,
        )

        # удалили из базы
        assert_that(
            DomainModel(self.main_connection).get(domain, self.organization['id']),
            none()
        )
        # вернули удаленный домен
        assert_that(
            result,
            equal_to({'domain': domain})
        )

    def test_delete_not_alias(self):
        # успешно  удаляем  не алиас
        domain = 'somedomain.ru'
        DomainModel(self.main_connection).create(domain, self.organization['id'], True)

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'domain_id': 123,
            }
            result = self.delete_json('/domains/%s/' % domain, expected_code=200)

        # удалили из базы
        assert_that(
            DomainModel(self.main_connection).get(domain, self.organization['id']),
            none()
        )
        # вернули удаленный домен
        assert_that(
            result,
            equal_to({'domain': domain})
        )

    def test_delete_verified_via_webmaster(self):
        # успешно  удаляем  домен подтвержденный через webmaster
        domain = 'somedomain.ru'
        DomainModel(self.main_connection).create(domain, self.organization['id'], True, via_webmaster=True)

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'domain_id': 123,
            }
            result = self.delete_json('/domains/%s/' % domain, expected_code=200)

        # удалили из базы
        assert_that(
            DomainModel(self.main_connection).get(domain, self.organization['id']),
            none()
        )
        # вернули удаленный домен
        assert_that(
            result,
            equal_to({'domain': domain})
        )
        # записали в лог событие
        assert_that(
            WebmasterDomainLogModel(self.main_connection).filter(org_id=self.organization['id']).all(),
            contains(
                has_entries(
                    action=domain_action.delete,
                    name=domain,
                )
            )
        )

    def test_delete_alias_from_passport(self):
        # проверяем, что удаляем алиас домена через правильную ручку паспорта
        domain = 'somedomain.ru'
        DomainModel(self.main_connection).create(domain, self.organization['id'], True, via_webmaster=True)
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.domain.get_domain_id_from_blackbox', return_value=111), \
             patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.user['id'],
                'domain_id': 123
            }
            result = self.delete_json('/domains/%s/' % domain, expected_code=200)
            assert_called_once(
                self.mocked_passport.domain_alias_delete,
                111,
                123
            )
            # удалили из базы
            assert_that(
                DomainModel(self.main_connection).get(domain, self.organization['id']),
                none()
            )

    def test_delete_domain_with_other_admin(self):
        # проверяем, что не удаляем домен из паспорта, если он принадлежит другому админу
        domain = 'somedomain.ru'
        DomainModel(self.main_connection).create(domain, self.organization['id'], True, via_webmaster=True)
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox') as get_domain_info_from_blackbox:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': 55555555,
                'domain_id': 123
            }
            result = self.delete_json('/domains/%s/' % domain, expected_code=200)

            assert_not_called(self.mocked_passport.domain_alias_delete)
            # удалили из базы
            assert_that(
                DomainModel(self.main_connection).get(domain, self.organization['id']),
                none()
            )

    def test_simple_user(self):
        # обычный пользователь не может удалить домен организации
        new_user = self.create_user()
        headers = get_auth_headers(as_uid=new_user['id'])
        self.delete_json('/domains/%s/' % self.organization_domain, headers=headers, expected_code=403)

    def test_delete_pdd_bad_domain(self):
        # при удалении не RFC домена не должно падать, а должно удалиться в нашей базе
        domain_name = '1.1'
        DomainModel(self.main_connection).create(domain_name, self.organization['id'], owned=False)

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_info_from_blackbox', return_value=None):
            self.delete_json('/domains/%s/' % domain_name, expected_code=200)

        domain = DomainModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'name': domain_name,
            }
        )
        assert not domain


class TestDomainCheckOwnership(TestCase):
    api_version = 'v7'

    def setUp(self):
        super(TestDomainCheckOwnership, self).setUp()
        self.second_admin = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=101,
            nickname='petya',
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            org_id=self.organization['id'],
            department_id=1,
        )
        UserModel(self.main_connection).make_admin_of_organization(self.organization['id'], self.second_admin['id'])

    def test_webmaster_verification(self):
        # Проверим, что если домен добавлен в режиме подтверждения через Вебмастер,
        # то мы дернем ручку verify вебмастера от имени главного админа
        org_id = self.organization['id']
        domain_model = DomainModel(self.main_connection)
        domain = domain_model.create(
            org_id=org_id,
            name='example.com',
            via_webmaster=True,
        )

        set_auth_uid(self.second_admin['id'])

        domain_id = 123
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_id_from_blackbox', return_value=domain_id), \
             patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain,\
            patch('intranet.yandex_directory.src.blackbox_client.BlackboxClient.hosted_domains') as hosted_domains:
            # замокаем блекбокс, чтобы он не ломился в паспорт
            hosted_domains.return_value = {
                "hosted_domains": [
                    {
                        "domain": self.organization_domain,
                        "domid": "1",
                    },
                    {
                        "domain": 'example.com',
                        "domid": "2",
                    }
                ]
            }

            # Нужно чтобы ручка вернула словарь без ключа errors,
            # тогда ответ будет считаться успешным.
            self.mocked_webmaster_inner_verify.return_value = {
            }

            self.post_json(
                '/domains/{0}/check-ownership/'.format(domain['name']),
                {'verification_type': 'webmaster.dns'},
                expected_code=200,
            )
            self.mocked_webmaster_inner_verify.assert_called_once_with(
                domain['name'],
                self.admin_uid,
                'DNS',
                ignore_errors=ANY,
            )

            # ставим заадчу на проверку в очередь
            assert_called_once(
                mocked_sync_single_domain,
                org_id=org_id,
                domain_name=domain['name'],
            )

        # записали в лог событие
        assert_that(
            WebmasterDomainLogModel(self.main_connection).filter(org_id=self.organization['id']).all(),
            contains(
                has_entries(
                    action=domain_action.verify,
                    name=domain['name'],
                    verification_type='dns',
                )
            )
        )

    def test_webmaster_verification_dns_delegation(self):
        # Проверим, что при подтверждении домена через делегирование,
        # закрепляется домен за пользователем на двое суток
        domain = DomainModel(self.main_connection).create(
            org_id=self.organization['id'],
            name='example.com',
            via_webmaster=True,
        )
        self.mocked_webmaster_inner_lock_dns_delegation.return_value = {}
        self.mocked_webmaster_inner_verify.return_value = {}

        self.post_json(
            '/domains/{0}/check-ownership/'.format(domain['name']),
            {'verification_type': 'webmaster.dns_delegation'},
            expected_code=200,
        )

        duration_minutes = app.config['LOCK_DNS_DELEGATION_DURATION'] / 60
        assert_called_once(
            self.mocked_webmaster_inner_lock_dns_delegation,
            domain['name'],
            self.admin_uid,
            duration_minutes,
        )

    def test_webmaster_verification_when_domain_already_verified(self):
        # проверяем ситуацию, что домен уже подтверждён в Вебмастере.
        # В этом случае его ручка /verify/ возвращает ошибку, и её надо игнорировать
        org_id = self.organization['id']
        domain = DomainModel(self.main_connection).create(
            org_id=org_id,
            name='example.com',
            via_webmaster=True,
        )

        with patch.object(SyncSingleDomainTask, 'delay'):
            # Нужно чтобы ручка вернула словарь с ошибкой, как будто домен уже подтверждён
            self.mocked_webmaster_inner_verify.return_value = {
                "errors": [
                    {
                        "code": "VERIFY_HOST__ALREADY_VERIFIED",
                    }
                ]
            }
            # После этого, ручка info должна вернуть, что домен подтверждён
            self.mocked_webmaster_inner_info.return_value = {
                'data': {'verificationStatus': 'VERIFIED'},
            }

            self.post_json(
                '/domains/{0}/check-ownership/'.format(domain['name']),
                {'verification_type': 'webmaster.dns'},
                # Но несмотря на ошибку, ручка директории должна завершиться успешно
                expected_code=200,
            )


class TestOwnershipInfoCase(TestCase):
    api_version = 'v7'

    def setUp(self):
        super(TestOwnershipInfoCase, self).setUp()
        self.second_admin = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=101,
            nickname='petya',
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            org_id=self.organization['id'],
            department_id=1,
        )
        UserModel(self.main_connection).make_admin_of_organization(self.organization['id'], self.second_admin['id'])

    def test_ownership_info_for_owned_domain(self):
        # Проверяем, что если домен уже подтвержден,
        # то мы отдадим статус подтверждения owned

        # Сделаем вид, что домен owned
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=True)

        response = self.get_json('/domains/{0}/ownership-info/'.format(domain_name))

        assert_that(
            response,
            has_entries(
                status='owned',
                domain=domain_name,
            )
        )

    def test_ownership_info_for_not_owned_domain(self):
        # Проверяем, что если домен ещё не подтверждён
        # то мы отдадим статус подтверждения need-validation

        # Сделаем вид, что домен owned=False
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        set_auth_uid(self.second_admin['id'])
        # Сделаем вид, что запрос в вебмастер вернул список возможных способов подтверждения
        with patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_list_applicable') \
                as webmaster_list_applicable, \
                patch('intranet.yandex_directory.src.yandex_directory.webmaster._inner_info') \
                        as webmaster_info:
            webmaster_list_applicable.side_effect = webmaster_responses.applicable()
            webmaster_info.side_effect = webmaster_responses.info(uin='100500')

            response = self.get_json('/domains/{0}/ownership-info/'.format(domain_name))
            expected_methods = [
                {
                    'method': 'webmaster.whois',
                    'weight': 0,
                    'code': '100500',
                },
                {
                    'method': 'webmaster.dns',
                    'weight': 0,
                    'code': '100500',
                },
                {
                    'method': 'webmaster.html_file',
                    'weight': 0,
                    'code': '100500',
                },
                {
                    'method': 'webmaster.meta_tag',
                    'weight': 0,
                    'code': '100500',
                },
            ]
            assert_that(
                response,
                has_entries(
                    status='need-validation',
                    methods=contains_inanyorder(*expected_methods),
                    preferred_host=domain_name,
                )
            )

            webmaster_info.assert_called_once_with(
                domain_name,
                self.admin_uid,
                ignore_errors=ANY,
            )
            webmaster_list_applicable.assert_called_once_with(
                domain_name,
                self.admin_uid,
            )

    def test_ownership_info_for_owned_in_webmaster(self):
        # Проверим, что если домен подтвержден через Вебмастер, а в директории нет
        # то подтвердим его в директории
        org_id = self.organization['id']
        domain_model = DomainModel(self.main_connection)
        domain = domain_model.create(
            org_id=org_id,
            name='example.com',
            via_webmaster=True,
        )
        set_auth_uid(self.second_admin['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_domain_id_from_blackbox', return_value=123), \
             patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain:
            # Нужно чтобы ручка вернула словарь без ключа errors,
            # тогда ответ будет считаться успешным.
            self.mocked_webmaster_inner_verify.return_value = {}
            # После этого, ручка info должна вернуть, что домен подтверждён
            self.mocked_webmaster_inner_info.return_value = {
                'data': {'verificationStatus': 'VERIFIED', 'verificationUin': 123},
            }
            self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()

            response = self.get_json(
                '/domains/{0}/ownership-info/'.format(domain['name']),
                expected_code=200,
            )
            assert_called_once(
                mocked_sync_single_domain,
                org_id=self.organization['id'],
                domain_name=domain['name'].lower()
            )

            # Сама ручка должна отдать в ответ имя домена и признак, что он подтверждён
            assert_that(
                response,
                has_entries(
                    domain=domain['name'],
                    status='in-progress',
                )
            )

    def test_ownership_info_when_verification_failed(self):
        # Проверим, что если процедуру подтверждения запустили, то ручка
        # отдаст способ подтверждения, выбранный пользователем и дату
        # последней проверки.

        # Сделаем вид, что домен owned=False
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        # Сделаем вид, что запрос в вебмастер вернул список возможных способов подтверждения
        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        # После нескольких попыток подтвердить через DNS не удалось, и
        # нужно чтобы пользователь снова нажал кнопку.
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_failed()

        response = self.get_json('/domains/{0}/ownership-info/'.format(domain_name))

        assert_that(
            response,
            has_entries(
                status='need-validation',
                last_check=has_entries(
                    method='webmaster.dns',
                    date='2018-03-02T13:40:10.167Z',
                    fail_type='dns_record_not_found',
                    fail_reason='some_error',
                ),
            )
        )

    def test_ownership_info_when_verification_in_progress(self):
        # Проверим, что если процедуру подтверждения запустили, но она еще не закончена

        # Сделаем вид, что домен owned=False
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        # Сделаем вид, что запрос в вебмастер вернул список возможных способов подтверждения
        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        # После нескольких попыток подтвердить через DNS не удалось, и
        # нужно чтобы пользователь снова нажал кнопку.
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_in_progress()

        response = self.get_json('/domains/{0}/ownership-info/'.format(domain_name))

        assert_that(
            response,
            has_entries(
                status='in-progress',
            )
        )

    def test_ownership_info_removed_from_webmaster_domain(self):
        # Сначала делаем вид, что в вебмастере домена нет
        # Проверем, что мы добавили домен в вебмастер и повторно запросили info
        org_id = self.organization['id']
        domain_model = DomainModel(self.main_connection)
        domain = domain_model.create(
            org_id=org_id,
            name='example.com',
            via_webmaster=True,
        )

        r1 = {
            'errors': [{'code': 'USER__HOST_NOT_ADDED'}]
        }
        r2 = {
            "data": {
                "verificationStatus": "VERIFIED",
                "verificationUin": 123,
            }
        }
        webmaster_info_responses = [r1, r2]
        m = Mock(side_effect=webmaster_info_responses)
        self.mocked_webmaster_inner_info.side_effect = m
        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        with patch.object(SyncSingleDomainTask, 'delay') as mocked_sync_single_domain:
            response = self.get_json(
                '/domains/{0}/ownership-info/'.format(domain['name']),
                expected_code=200,
            )

        assert_called_once(
            self.mocked_webmaster_inner_add,
            domain['name'].lower(),
            self.admin_uid,
            ignore_errors=ANY,
        )
        assert_called_once(
            mocked_sync_single_domain,
            org_id=self.organization['id'],
            domain_name=domain['name'].lower(),
        )
        assert_that(
            response,
            has_entries(
                status='in-progress',
                domain=domain['name'],
                methods=not_none(),
            )
        )

class TestDomainConnectView__post(TestCase):
    def setUp(self):
        super(TestDomainConnectView__post, self).setUp()
        self.admin_outer_uid = 11111
        self.label = 'another'
        self.domain_part = 'another.com'

        organization_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label=self.label,
            domain_part=self.domain_part,
            admin_uid=self.admin_outer_uid,
        )

        self.org_id = organization_info['organization']['id']
        self.admin_uid = get_organization_admin_uid(
            self.main_connection,
            self.org_id,
        )
        self.new_domain = 'test.ws.autotest.yandex.ru'
        self.new_domain_lower_case = self.new_domain.lower()
        self.new_domain_in_punycode = self.new_domain_lower_case.encode('idna')
        self.master_domain = self.label + self.domain_part
        self.master_domain_in_punycode = self.master_domain.encode('idna')

        self.second_admin = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=101,
            nickname='petya',
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            org_id=self.org_id,
            department_id=1,
        )
        UserModel(self.main_connection).make_admin_of_organization(self.org_id, self.second_admin['id'])
        set_auth_uid(self.second_admin['id'])

    def test_add_connect_domain(self):
        # Проверяем, что коннектный домен добавляется как алиас сразу подтвержденным
        data = {
            'name': self.new_domain,
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.views.create_domain_in_passport') as mocked_create_domain:
            mocked_create_domain.return_value = False
            response = self.post_json('/domains/connect/', data)

        assert_that(
            response,
            has_entries(
                name=self.new_domain_lower_case,
                owned=True,
            )
        )

        domains = DomainModel(self.main_connection).find({'org_id': self.org_id})

        assert_that(
            domains,
            contains_inanyorder(
                has_entries(
                    name=self.master_domain,
                    owned=True,
                    master=True,
                ),
                has_entries(
                    name=self.new_domain_lower_case,
                    owned=True,
                    master=False,
                )
            )
        )

        # мы должны были добавить домен в паспорт
        assert_called_once(
            mocked_create_domain,
            ANY,
            self.org_id,
            self.new_domain,
            self.admin_uid,
        )

    def test_add_incorrect_domain(self):
        # через эту ручку не технические домены добавлять нельзя
        data = {
            'name': 'some_domain.ru',
        }
        response = self.post_json('/domains/connect/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='invalid_domain',
            )
        )

    def test_add_second_tech_domain(self):
        # нельзя добавить технический домен, если такой уже существует
        data = {
            'name': self.new_domain,
        }

        # добавляем в дефолтную организацию, у которой есть технический домен
        response = self.post_json(
            '/domains/connect/',
            data,
            headers=get_auth_headers(as_uid=self.organization_info['admin_user']['id']),
            expected_code=422,

        )
        assert_that(
            response,
            has_entries(
                code='tech_domain_already_exists',
            )
        )


class TestDomainDetail__blocked(TestCase):
    def setUp(self):
        super(TestDomainDetail__blocked, self).setUp()
        self.org_id = self.organization['id']
        DomainModel(self.main_connection).create(
            name='yet-another-domain',
            org_id=self.org_id,
            owned=True
        )

    def test_delete_blocked(self):
        DomainModel(self.main_connection).update(
            {'blocked_at': utcnow()},
            {'org_id': self.org_id, 'owned': True}
        )

        result = self.delete_json('/domains/%s/' % 'yet-another-domain', expected_code=422)
        assert_that(
            result,
            has_entries(
                code='blocked_domain_cant_be_modified',
                message='Domain {domain} is blocked',
                params={'domain': 'yet-another-domain'},
            )
        )

    def test_change_master(self):
        # нельзя сменить мастер домен, если текущий мастер заблокирован
        DomainModel(self.main_connection).update(
            {'blocked_at': utcnow()},
            {'org_id': self.org_id, 'master': True}
        )

        result = self.patch_json(
            '/organization/',
            {'master_domain': 'yet-another-domain'},
            expected_code=422
        )
        assert_that(
            result,
            has_entries(
                code='blocked_domain_cant_be_modified',
            )
        )

        # нельзя сменить мастер домен, если новый мастер заблокирован
        DomainModel(self.main_connection).update(
            {'blocked_at': utcnow()},
            {'org_id': self.org_id, 'name': 'yet-another-domain'}
        )
        DomainModel(self.main_connection).update(
            {'blocked_at': None},
            {'org_id': self.org_id, 'master': True},
            force=True
        )
        self.patch_json(
            '/organization/',
            {'master_domain': 'yet-another-domain'},
            expected_code=422
        )


class TestDomainAutoapprove(TestCase):
    def test_domain_with_special_suffix_is_autoapproved(self):
        # Проверяем, что домены с определённым именем будут автоподтверждаться.
        # Это нужно для облегчения тестирования ассесорами.
        new_domain = 'blah.auto.connect-tk.tk'
        data = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
           preset='default',
        )
        admin_uid = data['admin_user_uid']
        org_id = data['organization']['id']

        ServiceModel(self.meta_connection).create(slug='abook2', name='Адресная книга')
        UpdateServicesInShards().try_run()
        preset = PresetModel(self.meta_connection).get('enable-maillist')
        if not preset:
            PresetModel(self.meta_connection).create('enable-maillist', ['maillist', 'abook2'], {})

        def get_org():
            return OrganizationModel(self.main_connection).filter(id=org_id).fields('has_owned_domains').one()

        # До добавления домена признак в метабазе должен быть False
        assert_that(
            get_org(),
            has_entries(
                has_owned_domains=False,
            )
        )

        headers = get_auth_headers(as_uid=admin_uid)
        self.post_json('/domains/', dict(name=new_domain), headers=headers, process_tasks=True)

        domains = DomainModel(self.main_connection) \
            .filter(org_id=org_id) \
            .fields('name', 'owned') \
            .all()

        assert_that(
            domains,
            contains(
                has_entries(
                    owned=True,
                    name=new_domain,
                ),
            )
        )
        # После добавления домена мы должны зафиксировать в метабазе, что домены есть
        assert_that(
            get_org(),
            has_entries(
                has_owned_domains=True,
            )
        )

        # Также нужно проверить, что сервис рассылок включился
        assert OrganizationServiceModel(self.main_connection).is_service_enabled(org_id, 'maillist')
        assert OrganizationServiceModel(self.main_connection).is_service_enabled(org_id, 'abook2')


class TestAddDomainAsOwned(TestCase):
    def test_add_domain_as_owned(self):
        domain_name = 'test.com'

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json('/proxy/domains/', data={
                'name': domain_name,
            })

        domain_model = DomainModel(self.main_connection).get(domain_name=domain_name, org_id=self.organization['id'])
        assert_that(
            domain_model,
            not_none()
        )
        assert_that(
            domain_model['owned'],
            equal_to(True)
        )

    def test_add_domain_to_sso_org(self):
        domain_name = 'test.com'

        another_sso_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

        admin_uid = another_sso_org["admin_uid"]
        headers = get_auth_headers(as_uid=admin_uid)

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json(
                '/proxy/domains/',
                data={
                    'name': domain_name,
                },
                headers=headers,
                expected_code=403
            )

    def test_add_duplicated_domain_in_same_organizations(self):
        domain_name = 'test.com'

        DomainModel(self.main_connection).create(
            name=domain_name,
            org_id=self.organization['id'],
        )

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json('/proxy/domains/', data={
                'name': domain_name,
            })

        domain_model = DomainModel(self.main_connection).get(domain_name=domain_name)
        assert_that(
            domain_model['org_id'],
            equal_to(self.organization['id'])
        )
        assert_that(
            domain_model['owned'],
            equal_to(True)
        )

    def test_add_duplicated_owned_domain_in_same_organization(self):
        domain_name = 'test.com'

        DomainModel(self.main_connection).create(
            name=domain_name,
            org_id=self.organization['id'],
            owned=True,
        )

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json('/proxy/domains/', data={
                'name': domain_name,
            }, expected_code=409)

    def test_add_duplicated_domain_in_another_organization(self):
        domain_name = 'test.com'

        another_org_id = 123

        OrganizationModel(self.main_connection).create(
            id=another_org_id,
            name='another_org_with_domain',
            language='ru',
            label='another_org_with_domain',
            admin_uid=321,
        )
        DomainModel(self.main_connection).create(
            name=domain_name,
            org_id=another_org_id,
        )

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json('/proxy/domains/', data={
                'name': domain_name,
            })

        domain_model = DomainModel(self.main_connection).get(domain_name=domain_name, org_id=self.organization['id'])

        assert_that(
            domain_model,
            not_none()
        )
        assert_that(
            domain_model['owned'],
            equal_to(True)
        )

    def test_add_duplicated_owned_domain_in_another_organization(self):
        domain_name = 'test.com'

        another_org_id = 123

        OrganizationModel(self.main_connection).create(
            id=another_org_id,
            name='another_org_with_domain',
            language='ru',
            label='another_org_with_domain',
            admin_uid=321,
        )
        DomainModel(self.main_connection).create(
            name=domain_name,
            org_id=another_org_id,
            owned=True,
        )

        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(scope.pdd_proxy, *standard_scopes):
            self.post_json('/proxy/domains/', data={
                'name': domain_name,
            }, expected_code=409)

    def test_without_pdd_proxy_scope(self):
        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]
        with scopes(*standard_scopes):
            self.post_json(
                '/proxy/domains/',
                data={'name': 'test.com'},
                expected_code=403,
            )


class TestGetDomainAndAdminUidView(TestCase):
    def test_get_404(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            self.get_json(
                '/domain-token/{token}/{pdd_version}/'.format(token='invalid-token', pdd_version='old'),
                expected_code=404,
            )

    def test_get_200_old_pdd(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)) as session:
            pdd_version = 'old'
            admin_id = 123
            domain = 'test.com'
            token = 'token1'

            serialized_token = PlainToken(component_registry().domain_token_repository).serialize_token(token)

            domain_token = DomainToken(
                pdd_version=pdd_version,
                admin_id=admin_id,
                domain=domain,
                token=serialized_token
            )
            session.add(domain_token)
            session.flush()

            response = self.get_json(
                '/domain-token/{token}/{pdd_version}/'.format(token=token, pdd_version=pdd_version)
            )

            assert_that(
                response,
                has_entries(
                    uid=admin_id,
                    domain=domain,
                )
            )

    def test_get_200_new_pdd(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)) as session:
            pdd_version = 'new'
            admin_id = 123
            domain = 'test.com'
            token = 't' * 52

            serialized_token = CryptToken(component_registry().domain_token_repository).serialize_token(token)

            domain_token = DomainToken(
                pdd_version=pdd_version,
                admin_id=admin_id,
                domain=domain,
                token=serialized_token,
            )
            session.add(domain_token)
            session.flush()

            response = self.get_json(
                '/domain-token/{token}/{pdd_version}/'.format(token=token, pdd_version=pdd_version)
            )

            assert_that(
                response,
                has_entries(
                    uid=admin_id,
                    domain=domain,
                )
            )

    def test_get_404_on_invalid_token(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            self.get_json(
                '/domain-token/{token}/{pdd_version}/'.format(token='BULLSHIT', pdd_version='new'),
                expected_code=404,
            )


class TestGetDomainToken(TestCase):
    def test_gen_domain_token_old_pdd(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            admin_id = 123
            domain = 'test.com'
            pdd_version = 'old'

            response = self.post_json('/domain-token/{uid}/{domain}/{pdd_version}'.format(
                uid=admin_id,
                domain=domain,
                pdd_version=pdd_version,
            ), {}, expected_code=200)

            assert_that(
                response,
                has_key('token'),
            )

            domain_token_repository = component_registry().domain_token_repository  # type: DomainTokenRepository
            serialized_token = PlainToken(domain_token_repository).serialize_token(response['token'])

            domain_token = domain_token_repository.find_one_by_token_and_pdd_version(serialized_token, pdd_version)
            assert_that(domain_token is not None)

    def test_gen_domain_token_new_pdd(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            admin_id = 123
            domain = 'test.com'
            pdd_version = 'new'

            response = self.post_json('/domain-token/{uid}/{domain}/{pdd_version}'.format(
                uid=admin_id,
                domain=domain,
                pdd_version=pdd_version,
            ), {}, expected_code=200)

            assert_that(
                response,
                has_key('token'),
            )

            domain_token_repository = component_registry().domain_token_repository  # type: DomainTokenRepository
            serialized_token = CryptToken(domain_token_repository).serialize_token(response['token'])

            domain_token = domain_token_repository.find_one_by_token_and_pdd_version(serialized_token, pdd_version)
            assert_that(domain_token is not None)


class TestDeleteDomainToken(TestCase):
    def test_delete_not_existent_domain_token(self):
        admin_id = 123
        domain = 'test.com'
        pdd_version = 'old'

        self.delete_json('/domain-token/{uid}/{domain}/{pdd_version}'.format(
            uid=admin_id,
            domain=domain,
            pdd_version=pdd_version,
        ), expected_code=404)

    def test_delete_domain_token(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)) as session:
            admin_id = 123
            domain = 'test.com'
            pdd_version = 'old'

            domain_token = DomainToken(
                pdd_version=pdd_version,
                admin_id=admin_id,
                domain=domain,
                token='serialized_token',
            )
            session.add(domain_token)
            session.flush()

            self.delete_json('/domain-token/{uid}/{domain}/{pdd_version}'.format(
                uid=admin_id,
                domain=domain,
                pdd_version=pdd_version,
            ), expected_code=204)

            domain_token_repository = component_registry().domain_token_repository  # type: DomainTokenRepository
            domain_token = domain_token_repository.find_one_by_pdd_info(admin_id, domain, pdd_version)

            assert_that(
                domain_token,
                none(),
            )


class TestDomenatorProxyForDomainToken(TestCase):

    def setUp(self):
        super().setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []

    @responses.activate
    @override_settings(DOMENATOR_DOMAIN_TOKEN_PROXY=True)
    def test_delete_domain_token_proxy(self):
        admin_id = 123
        domain = 'test.com'
        pdd_version = 'old'

        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/domain-token/{admin_id}/{domain}/{pdd_version}/',
            status=204,
        )

        self.delete_json(
            f'/domain-token/{admin_id}/{domain}/{pdd_version}',
            expected_code=204
        )

    @responses.activate
    @override_settings(DOMENATOR_DOMAIN_TOKEN_PROXY=True)
    def test_delete_domain_token_proxy_fail(self):
        admin_id = 123
        domain = 'test.com'
        pdd_version = 'old'

        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/domain-token/{admin_id}/{domain}/{pdd_version}/',
            status=404,
        )

        self.delete_json(
            f'/domain-token/{admin_id}/{domain}/{pdd_version}',
            expected_code=404
        )

    @responses.activate
    @override_settings(DOMENATOR_DOMAIN_TOKEN_PROXY=True)
    def test_gen_domain_token_old_pdd_proxy(self):
        admin_id = 123
        domain = 'test.com'
        pdd_version = 'old'

        responses.add(
            responses.POST,
            f'https://domenator-test.yandex.net/api/domain-token/{admin_id}/{domain}/{pdd_version}/',
            json={'token': 'my_token'},
            status=201,
        )

        response = self.post_json(
            f'/domain-token/{admin_id}/{domain}/{pdd_version}',
            {},
            expected_code=200
        )
        assert response['token'] == 'my_token'

    @responses.activate
    @override_settings(DOMENATOR_DOMAIN_TOKEN_PROXY=True)
    def test_get_200_new_pdd_proxy(self):
        pdd_version = 'new'
        admin_id = 123
        domain = 'test.com'
        token = 't' * 52

        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/domain-token/{token}/{pdd_version}/',
            json={'uid': admin_id, 'domain': domain},
            status=200,
        )

        response = self.get_json(
            f'/domain-token/{token}/{pdd_version}/'
        )

        assert_that(
            response,
            has_entries(
                uid=admin_id,
                domain=domain,
            )
        )



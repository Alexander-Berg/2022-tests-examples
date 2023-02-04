# -*- coding: utf-8 -*-
import contextlib
from unittest import skip
from unittest.mock import patch

import json

import pytest
from hamcrest import (
    empty,
    assert_that,
    equal_to,
    calling,
    has_entries,
    has_length,
    has_properties,
    none,
    contains_string,
    contains,
    instance_of,
)
from werkzeug.datastructures import Headers
from intranet.yandex_directory.src.blackbox_client import odict
from testutils import TestCase as BaseTestCase
from testutils import (
    create_outer_admin,
    override_settings,
    raises,
    mocked_blackbox,
    create_organization,
    MockToDict,
    fake_userinfo,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.user import User, TeamUser
from intranet.yandex_directory.src.yandex_directory.auth.service import Service
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.auth.middlewares import (
    authorize,
    authenticate_by_token,
    authenticate_by_oauth,
    authenticate_by_tvm2,
    get_user_from_headers,
    get_authorization_tokens,
    AuthMiddleware,
)
from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    AuthenticationError,
    AuthorizationError,
    OrganizationNotReadyError,
    APIError,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    OrganizationDeleted,
    OrganizationUnknown,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationMetaModel,
    UserMetaModel,
    UserModel,
    ServiceModel,
    OrganizationServiceModel,
    RobotServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import INTERNAL_ADMIN_SERVICE_SLUG


class TestCase(BaseTestCase):
    @contextlib.contextmanager
    def run(self, *args, **kwargs):
        with app.test_request_context(), \
             super(TestCase, self).run(*args, **kwargs):
            yield


class TestAuthenticationUtils(TestCase):
    def test_auth_headers_parse(self):
        # Проверим, как парсятся Авторизационные заголовки

        # В Ticket можно передавать TVM тикет
        with app.test_request_context('/', headers={'Ticket': 'blah'}):
            tokens = list(get_authorization_tokens())
            assert_that(
                tokens,
                contains(('tvm', 'blah'))
            )

        # Так же, TVM тикет пока можно передать в Authorization
        with app.test_request_context('/', headers={'Authorization': 'TVM blah'}):
            tokens = list(get_authorization_tokens())
            assert_that(
                tokens,
                contains(('tvm', 'blah'))
            )

        # OAuth токен можно передать в Authorization
        with app.test_request_context('/', headers={'Authorization': 'oauth blah'}):
            tokens = list(get_authorization_tokens())

            assert_that(
                tokens,
                contains(('oauth', 'blah'))
            )

        # В заголовках могут быть разные токены и функция вернёт их все в таком
        # порядке, что сначала будет идти TVM 2.0, потом обычный TVM, и только
        # затем oauth.
        headers = {
            'X-Ya-Service-Ticket': '1',
            'Ticket': '2',
            'Authorization': 'oauth 3',
        }
        with app.test_request_context('/', headers=headers):
            tokens = list(get_authorization_tokens())

            assert_that(
                tokens,
                contains(
                    ('tvm2', '1'),
                    ('tvm', '2'),
                    ('oauth', '3'),
                )
            )

    def test_with_uid_without_ip_raises(self):
        # X-UID должен всегда передаваться вместе с X-User-IP
        headers = Headers({
            'X-UID': self.user['id'],
            # IP бэкенда сервиса должен присутствовать обязательно
            'X-Real-IP': '127.0.0.1',
        })
        assert_that(
            calling(get_user_from_headers).with_args(headers),
            raises(
                APIError,
                status_code=400,
                message='Header "{header}" is required.',
            )
        )

    def test_with_uid_and_ip_is_ok(self):
        headers = Headers({
            'X-UID': self.user['id'],
            'X-User-IP': '130.14.12.42',
            # IP бэкенда сервиса должен присутствовать обязательно
            'X-Real-IP': '127.0.0.1',
        })

        response = get_user_from_headers(headers)
        assert_that(
            response,
            has_properties(
                passport_uid=self.user['id'],
                ip='130.14.12.42',
            )
        )


class TestAuthenticationByToken(TestCase):
    service_data = {
        'name': 'Autotest',
        'identity': 'autotest',
        'token': '12345',
        'scopes': ['foo:bar', 'blah:minor'],
    }

    @override_settings(
        INTERNAL_SERVICES_BY_TOKEN={service_data['token']: service_data}
    )
    def test_full_case(self):
        # Сервисы, приходящие с захардкоженным токеном,
        # могут передавать X-Org-ID и X-UID + X-User-IP
        headers = Headers({
            # На этапе аутентификации никак не проверятся,
            # есть ли такая организация.
            'X-Org-ID': 42,
            'X-UID': self.user['id'],
            'X-User-IP': '1.2.3.4',
            # IP бэкенда сервиса
            'X-Real-IP': '127.0.0.1',
        })

        response = authenticate_by_token('12345', headers, True)
        assert_that(
            response,
            has_entries(
                # сервис определился
                service=has_properties(
                    name='Autotest',
                    identity='autotest',
                    is_internal=True,
                    ip='127.0.0.1',
                ),
                scopes=['foo:bar', 'blah:minor'],
                # поле org_id заполнено тем id, что был передан
                org_id=42,
                user=has_properties(
                    passport_uid=self.user['id'],
                    ip='1.2.3.4'
                ),
            )
        )

    def test_with_bad_token_raises(self):
        # Если токен не известен, то 401 ошибка
        headers = Headers({
            # IP бэкенда сервиса должен присутствовать обязательно
            'X-Real-IP': '127.0.0.1',
        })
        assert_that(
            calling(authenticate_by_token).with_args('unknown-token', headers, True),
            raises(
                AuthenticationError,
                status_code=401,
                message='Bad credentials',
            )
        )

    def test_with_bad_org_id_raises(self):
        # Если в качестве Org-ID передано что-то, что нельзя привести к int,
        # то это 400 Bad Request.
        bad_org_id = 'blah'

        headers = Headers({
            'X-Org-ID': bad_org_id,
            # IP бэкенда сервиса должен присутствовать обязательно
            'X-Real-IP': '127.0.0.1',
        })
        assert_that(
            calling(authenticate_by_token).with_args('12345', headers, True),
            raises(
                APIError,
                status_code=400,
                message='Header "{header}" should be a positive integer.',
            )
        )

    @override_settings(INTERNAL=False)
    def test_is_not_internal_with_token(self):
        # Проверяем что нельзя аутентифицироваться с помощью токена, если инстанс внешний.
        headers = Headers({
            # IP бэкенда сервиса должен присутствовать обязательно
            'X-Real-IP': '127.0.0.1',
        })
        assert_that(
            calling(authenticate_by_token).with_args('12345', headers, True),
            raises(
                AuthenticationError,
                status_code=401,
                message='Our API requires OAuth authentication',
            )
        )

    @override_settings(
        INTERNAL_SERVICES_BY_TOKEN={service_data['token']: service_data}
    )
    def test_if_old_service_can_be_linked_to_db_service(self):
        # Проверим, что если сервис прописан в базе в таблице services,
        # но пришёл со старым токеном, то мы возьмём его id и скоупы
        # из базы, а не из конфига.
        headers = Headers({
            # На этапе аутентификации никак не проверятся,
            # есть ли такая организация.
            'X-Org-ID': 42,
            # 'X-UID': self.user['id'],
            # 'X-User-IP': '1.2.3.4',
            # IP бэкенда сервиса
            'X-Real-IP': '127.0.0.1',
        })

        service_from_db = ServiceModel(self.meta_connection).create(
            slug=self.service_data['identity'],
            name=self.service_data['name'],
            scopes=['scope:from-db'],
        )

        response = authenticate_by_token(self.service_data['token'], headers, True)
        assert_that(
            response,
            has_entries(
                # сервис определился
                service=has_properties(
                    identity='autotest',
                    is_internal=True,
                    # нам важно, что у него есть id
                    id=service_from_db['id'],
                ),
                # и скоупы не ['foo:bar', 'blah:minor']
                scopes=['scope:from-db'],
            )
        )


class TestAuthorization(TestCase):
    def setUp(self):
        super(TestAuthorization, self).setUp()

        self.service_from_db = ServiceModel(self.meta_connection).create(
            slug='good-service',
            name='Good Service',
            # Сейчас не важно какой тут id.
            client_id=42,
        )
        self.service = Service(
            id=self.service_from_db['id'],
            name=self.service_from_db['name'],
            identity=self.service_from_db['slug'],
            is_internal=False,
            ip='127.0.0.1',
        )

    def test_if_user_does_not_exist_directory_db_org_ids_will_be_empty(self):
        # Если известен пользователь, но он не существовует в базе Директории,
        # то в результате авторизации у него не заполняется атрибут org_ids.
        # Такое может быть, если в Директорию пришёл запрос от имени сервиса
        # с указанием X-UID какого-то пользователя Яндекса, которого нет
        # в Директории. Сервис может интересоваться списком организаций
        # этого пользователя, к примеру. В этом случае, мы отдадим ему пустой
        # список, а не ошибку.

        unknown_uid = 12345
        user = User(unknown_uid, '127.0.0.1')
        # Дёрнем авторизацию с пользователем, которого в Директории нет
        authorize(
            self.meta_connection,
            user=user,
        )
        # Убедимся, что его нет ни в одной организации
        assert_that(
            user,
            has_properties(
                org_ids=empty(),
            )
        )

    def test_if_cloud_user_does_not_exist_it_will_be_created(self):
        # Если известен облачный пользователь, но он не существовует в базе Директории,
        # при этом он существует в базе Я.Организаций, то создадим его на лету.
        unknown_uid = 'hello-world'
        user = User(cloud_uid=unknown_uid, ip='127.0.0.1', is_cloud=True)
        org1_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
            cloud_org_id='smth-1'
        )['organization_meta_instance']['id']
        org2_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
            cloud_org_id='smth-2'
        )['organization_meta_instance']['id']

        # Дёрнем авторизацию с пользователем, которого в Директории нет
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.MessageToDict', MockToDict):

            list_organizations_response = self.get_dict_object({
                'next_page_token': None,
                'organizations': [
                    {
                        'id': 'smth-1',
                        'name': 'test',
                        'description': 'smth desc',
                    },
                    {
                        'id': 'smth-2',
                        'name': 'test-1',
                        'description': 'smth desc-1',
                    }
                ]
            })
            list_organizations.return_value = list_organizations_response
            mock_cloud_blackbox_instance.userinfo.return_value = {
                'uid': 9000000000000100,
                'claims': {
                    'given_name': 'user given name',
                    'family_name': 'user family name',
                    'preferred_username': 'username',
                    'email': 'username@example.com'
                }
            }

            authorize(
                self.meta_connection,
                user=user,
            )

        # Убедимся, что пользователь создан и находится в нужных организациях
        assert_that(
            user,
            has_properties(
                org_ids=[org1_id, org2_id],
            )
        )
        user_main = UserModel(self.main_connection).get(user_id=9000000000000100, org_id=org1_id)
        assert_that(
            user_main,
            has_entries(
                email='username@example.com',
                nickname='username',
                name={'last': 'user family name', 'first': 'user given name'},
                cloud_uid='hello-world',
            )
        )

    def test_if_passport_user_does_not_exist_it_will_be_created(self):
        # Если известен паспортный пользователь, но он не существовует в базе Директории,
        # при этом он существует в базе Я.Организаций, то создадим его на лету.
        unknown_uid = 4242424242
        unknown_cloud_uid = 'hello-world'
        user = User(passport_uid=unknown_uid, ip='127.0.0.1', cloud_uid=unknown_cloud_uid, is_cloud=True)
        org1_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
            cloud_org_id='smth-1'
        )['organization_meta_instance']['id']
        org2_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
            cloud_org_id='smth-2'
        )['organization_meta_instance']['id']

        # Дёрнем авторизацию с пользователем, которого в Директории нет
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations, \
            patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.MessageToDict', MockToDict), \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:

            list_organizations_response = self.get_dict_object({
                'next_page_token': None,
                'organizations': [
                    {
                        'id': 'smth-1',
                        'name': 'test',
                        'description': 'smth desc',
                    },
                    {
                        'id': 'smth-2',
                        'name': 'test-1',
                        'description': 'smth desc-1',
                    }
                ]
            })
            list_organizations.return_value = list_organizations_response
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(uid=unknown_uid)
            mock_blackbox_instance.batch_userinfo.return_value = [fake_userinfo(uid=unknown_uid)]

            authorize(
                self.meta_connection,
                user=user,
            )

            # Убедимся, что пользователь создан и находится в нужных организациях
            assert_that(
                user,
                has_properties(
                    org_ids=[org1_id, org2_id],
                )
            )
            user_main = UserModel(self.main_connection).get(user_id=unknown_uid, org_id=org1_id)
            assert_that(
                user_main,
                has_entries(
                    email='user@yandex.ru',
                    name={'last': 'Pupkin', 'first': 'Vasya'},
                    cloud_uid=unknown_cloud_uid,
                )
            )

    def test_org_id_autofilled_if_only_one_organization(self):
        # Если известен пользователь, и он состоит только в одной организации

        # удостоверимся, что пользователь только в одной организации
        meta_users = UserMetaModel(self.meta_connection).find({'id': self.user['id']})
        assert_that(meta_users, has_length(1))

        user = User(passport_uid=self.user['id'], ip='127.0.0.1')

        # org_id будет вычислен в результате выполнения Авторизации.
        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=None,
            user=user,
        )
        assert_that(
            org_id,
            equal_to(meta_users[0]['org_id'])
        )

    @pytest.mark.skip('multiorg enabled')
    def test_org_id_is_none_if_only_many_organization(self):
        # Если известен пользователь, и он состоит нескольких
        # организациях, то org_id остаётся None.

        # создадим внешнего админа
        uid, org_ids, revisions = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=2
        )

        # удостоверимся, что он действительно в нескольких организациях
        meta_users = UserMetaModel(self.meta_connection).find({'id': uid})
        assert_that(meta_users, has_length(2))

        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=None,
            user=User(passport_uid=uid, ip='127.0.0.1')
        )
        # org_id должен остаться None
        assert_that(org_id, none())

    def test_org_id_is_other_then_one_of_users_org(self):
        # Если явно указан X-Org-ID и он не соответствует
        # ни одной из организаций пользователя, то должна
        # быть 403 ошибка.

        # создадим внешнего админа
        uid, org_ids, revisions = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=2
        )

        # удостоверимся, что он действительно в нескольких организациях
        meta_users = UserMetaModel(self.meta_connection).find({'id': uid})
        assert_that(meta_users, has_length(2))

        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                # пользователь в этой организации не состоит
                org_id=self.organization['id'],
                user=User(passport_uid=uid, ip='127.0.0.1')
            ),
            raises(
                AuthorizationError,
                status_code=403,
                message='Wrong organization (org_id: {}, user.uid: {})'.format(self.organization['id'], uid),
            )
        )

    def test_organizations_not_ready(self):
        # Если организация не домигрировала, то считаем, что её нет

        # Сделаем вид, что миграция ещё не завершилась
        OrganizationMetaModel(self.meta_connection).update(
            {'ready': False},
            {'id': self.organization['id']}
        )

        # Попробуем получить доступ и org_id не обязателен,
        # функция должна отработать без проблем и вернуть
        # вместо org_id None.
        assert_that(
            authorize(
                self.meta_connection,
                org_id=self.organization['id'],
                require_org_id=False,  # <- Org_id не обязателен
            ),
            equal_to((None, None, None))
        )

        # Но если вью требует наличия org_id,
        # то будет ошибка
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                org_id=self.organization['id'],
                require_org_id=True,  # <- Org_id обязателен
            ),
            raises(
                OrganizationNotReadyError,
                status_code=403,
                message='Organization is not ready yet',
            )
        )

    def test_no_user_given_but_org_id_does(self):
        # Проверяем, что если сервис не указал пользователя,
        # но указал X-Org-ID, и эта организация есть в базе,
        # то функция вернёт её id
        org_id, shard, revision = authorize(
            self.meta_connection,
            # Эта организация есть в базе
            org_id=self.organization['id'],
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_no_user_given_and_organization_does_not_exist(self):
        # Проверяем, что если сервис не указал пользователя,
        # но указал X-Org-ID, и этой организации нет в базе и никогда не было,
        # то будет ошибка OrganizationUnknown

        org_id = OrganizationMetaModel(self.meta_connection).get_max_org_id() + 10
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                # Такой организации в базе нет и не было
                org_id=org_id,
            ),
            raises(
                OrganizationUnknown,
                status_code=404,
                code='unknown_organization',
                message='Unknown organization',
            )
        )

    def test_no_user_given_and_organization_was_deleted(self):
        # Проверяем, что если сервис не указал пользователя,
        # но указал X-Org-ID, и этой организации нет в базе,
        # но ее id меньше максимального, то будет ошибка OrganizationDeleted

        org_id = OrganizationMetaModel(self.meta_connection).get_max_org_id() - 10
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                # Такой организации в базе нет и ее id < максимального
                org_id=org_id,
            ),
            raises(
                OrganizationDeleted,
                status_code=404,
                code='organization_deleted',
                message='Organization was deleted',
            )
        )

    def test_if_service_can_work_with_organization_where_it_is_enabled(self):
        # Проверим, что сервис может работать с той организацией, для
        # которой он подключен.

        # Подключим сервис для организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service_from_db['id'],
        )
        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            service=self.service,
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_if_service_can_work_with_any_organization_if_have_special_scope(self):
        # Проверим, что сервис может работать с организацией, в которой
        # не подключен, если имеет специальный scope

        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            # Этот сервис не подключен.
            service=self.service,
            # Но имеет специальный scope
            scopes=[scope.work_with_any_organization],
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_error_if_service_has_no_special_scope_but_tries_to_work_with_any_user(self):
        # Проверим, что если сервис пытается работать от имени обычного
        # пользователя не имея специального скоупа, то будет 403 ошибка

        # Подключим сервис для организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service_from_db['id'],
        )

        # Несмотря на то, что сервис включён, он не должен иметь
        # возможности представляться пользователем без особого на то
        # разрешения.
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                org_id=self.organization['id'],
                # Этот сервис не подключен.
                service=self.service,
                user=User(passport_uid=self.user['id'], ip='127.0.0.1'),
            ),
            raises(
                AuthorizationError,
                status_code=403,
                message='Unable to work on behalf of the user',
            )
        )

    def test_service_can_work_with_any_user_if_have_special_scope(self):
        # Если сервис включён для организации и имеет специальный scope,
        # то может представляться любым пользователем.

        # Подключим сервис для организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service_from_db['id'],
        )

        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            service=self.service,
            user=User(passport_uid=self.user['id'], ip='127.0.0.1'),
            scopes=[scope.work_on_behalf_of_any_user],
        )

        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_service_can_work_with_his_robot(self):
        # Если сервис включён для организации и пытается
        # работать от имени своего робота, то это норм.

        # Подключим сервис для организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=self.service.id,
        )

        # Сделаем вид, что self.user это робот сервиса
        RobotServiceModel(self.main_connection).create(
            org_id=self.user['org_id'],
            uid=self.user['id'],
            service_id=self.service.id,
            slug=self.service.identity,
        )

        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            # Этот сервис не подключен.
            service=self.service,
            # Специального скоупа нет,
            scopes=[],
            # но этот пользователь – робот сервиса
            user=User(passport_uid=self.user['id'], ip='127.0.0.1'),
        )

        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_organization_required(self):
        # Если org_id не известен, а ручке он требуется, то это 403 ошибка
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                org_id=None,
                require_org_id=True,
            ),
            raises(
                AuthorizationError,
                status_code=403,
                message='Organization is required for this operation',
            )
        )

        # Если org_id известен, то всё хорошо
        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            require_org_id=True,
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_user_required(self):
        # Если пользователь неизвестен, но он требуется ручке, то это 403 ошибка
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                user=None,
                require_user=True,
            ),
            raises(
                AuthorizationError,
                status_code=403,
                message='User is required for this operation',
            )
        )

        # Если известен, то всё хорошо.
        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            user=User(
                passport_uid=self.user['id'],
                ip='127.0.0.1',
            ),
            require_user=True,
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )

    def test_internal_only(self):
        # Если ручка доступна только для внутренних сервисов а пришел внешний
        # или пользователь сам постучался, то отдаём 404, будто её и нет.
        assert_that(
            calling(authorize).with_args(
                self.meta_connection,
                service=Service('Foo', 'foo', is_internal=False),
                internal_only=True,
            ),
            raises(
                APIError,
                status_code=404,
                message='This route is internal',
            )
        )

        # А если пришел внутренний, то всё хорошо.
        org_id, shard, revision = authorize(
            self.meta_connection,
            org_id=self.organization['id'],
            service=Service('Disk', 'disk', is_internal=True),
            internal_only=True,
            # Чтобы проверка сработала, надо либо включить сервис
            # для организации, либо задать этот scope
            scopes=[scope.work_with_any_organization],
        )
        assert_that(
            org_id,
            equal_to(self.organization['id'])
        )


class TestAuthenticationByOAuth(TestCase):
    def setUp(self):
        super(TestAuthenticationByOAuth, self).setUp()

        self.service_client_id = 'e8c97c9c7e424f9e85385a895493c123'

        # зарегистрируем новый сервис
        ServiceModel(self.meta_connection).create(
            slug='good-service',
            name='Good Service',
            client_id=self.service_client_id,
        )

        self.valid_oauth_info = odict({
            "status": "VALID",
            "domain": None,
            "bruteforce_policy": {
                "captcha": False,
                "level": None,
                "password_expired": False
            },
            "login_status": None,
            "uid": None,
            "new_sslsession": None,
            "oauth": {
                "uid": None,
                "client_icon": None,
                "expire_time": "2017-12-22 13:38:42",
                "meta": None,
                "client_id": "e8c97c9c7e424f9e85385a895493c4fc",
                "token_id": "383396583",
                "client_is_yandex": "0",
                "device_id": None,
                "is_ttl_refreshable": "1",
                "ctime": "2016-12-21 18:11:30",
                "client_name": "Test directory",
                "client_ctime": "2016-12-21 11:07:34",
                "device_name": None,
                "issue_time": "2016-12-22 13:38:42",
                "scope": "market:partner-api tv:use display:all cloud_api.data:app_data bsapi:access",
                "client_homepage": None
            },
            "default_email": None,
            "lite_uid": None,
            "secure": False,
            "redirect": False,
            "fields": {
                "social_aliases": None,
                "social": None,
                "display_name": None,
                "aliases": None
            },
            "display_name": None,
            "password_status": None,
            "hosted_domains": None,
            "emails": None,
            "valid": True,
            "new_session": None,
            "karma": "0",
            "error": "OK",
            "attributes": {}
        })

    def test_bad_token(self):
        # Авторизуемся с невалидным OAuth токеном.

        with mocked_blackbox() as blackbox:
            blackbox.oauth.return_value = odict({
                'status': 'INVALID',
                'error': 'Error message'
            })

            headers = Headers({
                'X-Real-IP': '127.0.0.1',
            })
            assert_that(
                calling(authenticate_by_oauth).with_args('bad_token_very_bad', headers, True),
                raises(
                    AuthenticationError,
                    status_code=401,
                    message='Bad credentials',
                )
            )

    def test_unknown_service_unknown_user(self):
        # Авторизуемся сервисом не добавленным в Директорию
        # с токеном, не привязанным к пользователю.

        with mocked_blackbox() as blackbox:
            oauth_info = odict(self.valid_oauth_info.copy())
            oauth_info['oauth']['client_id'] = 'not_registered_client_id'
            blackbox.oauth.return_value = oauth_info

            headers = Headers({
                'X-Real-IP': '127.0.0.1',
            })

            # Если сервис не известен, и токен не привязан к
            # пользователю, то считаем такой запрос неаутентифицированным и кидаем
            # ошибку 401.
            assert_that(
                calling(authenticate_by_oauth).with_args('valid_token', headers, True),
                raises(
                    AuthenticationError,
                    status_code=401,
                    message='No user and service',
                )
            )

    def test_unknown_service_known_user(self):
        # Авторизуемся сервисом не добавленным в Директорию
        # с токеном, привязанным к пользователю.

        with mocked_blackbox() as blackbox:
            oauth_info = odict(self.valid_oauth_info.copy())
            oauth_info['oauth']['client_id'] = 'not_registered_client_id'
            # Те скоупы, которые тут перечислены, должны быть
            # присвоены атрибуту scopes
            oauth_info['oauth']['scope'] = 'foo:bar blah:minor'
            # Функция не должна ничего делать с этим uid
            # проверка будет осуществляться на этапе авторизации.
            oauth_info['uid'] = 1234567
            blackbox.oauth.return_value = oauth_info

            headers = Headers({
                'X-Real-IP': '127.0.0.1',
            })
            result = authenticate_by_oauth('valid_token', headers, True)

            # В этом случае, сервис неизвестен, так как
            # client_id не зарегистрирован в базе Директории.
            assert_that(
                result,
                has_entries(
                    auth_type='oauth',
                    service=none(),
                    org_id=none(),
                    user=has_properties(
                        passport_uid=1234567,
                        ip='127.0.0.1',
                    ),
                    # Скоупы должны трансфорироваться в список.
                    scopes=['foo:bar', 'blah:minor'],
                )
            )

    def test_org_id_header_is_allowed(self):
        # При использовании OAuth, можно явно указать id организации
        # через заголовок X-Org-ID. Это может быть полезно тогда,
        # когда пользователь внешний админ, и у него несколько организаций
        # или если это Сервис и ему надо выбрать с какой организацией
        # работать.
        # На этапе аутентификации нет никакой проверки, что id
        # организации существует или что сервис или пользователь
        # может работать с ней.
        #
        # Так же, тут проверяется, что когда в Директорию
        # приходит известный ей Сервис, то заполняется поле 'service'.
        # и оно содержит те скоупы, которые были получены от oauth.yandex.ru.
        # + те скоупы, которые были прописаны для самого сервиса в Директории
        headers = Headers({
            'X-Org-ID': '42',
            'x-real-ip': '127.0.0.1',
        })

        ServiceModel(self.meta_connection).update(
            {'scopes': ['dir-scope']},
            {'slug': 'good-service'},
        )

        with mocked_blackbox() as blackbox:
            oauth_info = odict(self.valid_oauth_info.copy())
            oauth_info['oauth']['client_id'] = self.service_client_id
            oauth_info['oauth']['scope'] = 'foo:bar blah:minor'
            blackbox.oauth.return_value = oauth_info

            result = authenticate_by_oauth('valid_token', headers, True)
            assert_that(
                result,
                has_entries(
                    auth_type='oauth',
                    service=has_properties(
                        identity='good-service',
                        is_internal=True,
                    ),
                    user=none(),
                    org_id=42,  # организация указана через X-Org-Id
                    # Скоупы должны трансфорироваться в список и сложиться из
                    # тех, что отдал oauth.yandex.ru и тех, что были прописаны
                    # в базе Директории.
                    scopes=['foo:bar', 'blah:minor', 'dir-scope'],
                )
            )


class FakeTVMData(object):
    def __init__(self, client_id, uids=None):
        self.client_ids = [client_id]
        self.uids = uids or []


class FakeTVM2Data(object):
    def __init__(self, client_id):
        self.src = client_id


class FakeTVM2UserData(object):
    def __init__(self, uids=None, default_uid=None):
        self.uids = uids or []
        self.default_uid = default_uid


class TestAuthenticationByTVM2(TestCase):
    def setUp(self):
        super(TestAuthenticationByTVM2, self).setUp()

        self.tvm2_client_id = 42
        self.mocked_tvm2_client.parse_service_ticket.return_value = FakeTVM2Data(self.tvm2_client_id)

        ServiceModel(self.meta_connection).create(
            slug='test-service',
            name='Test Service',
            tvm2_client_ids=[self.tvm2_client_id],
        )

    def set_service_scopes(self, *scopes):
        ServiceModel(self.meta_connection) \
            .filter(slug='test-service') \
            .update(scopes=list(scopes))


    def test_good_ticket(self):
        # Проверяем, что если в заголовке правильный тикет,
        # от обычного сервиса, то валидация пройдёт,
        # но user будет None
        headers = Headers({
            # Нам обязательно нужен IP клиента от Qloud
            'X-Real-IP': '127.0.0.1',
        })
        self.set_service_scopes('directory:some-scope')

        # Так как мы замокали parse_ticket, то тут всё равно какое
        # значение мы передаём.
        result = authenticate_by_tvm2('some_ticket', headers, True)

        assert_that(
            result,
            has_entries(
                auth_type='tvm2',
                service=has_properties(
                    is_internal=True,
                    identity='test-service',
                ),
                # Так как организация не передана,
                # то тут все поля про неё - None.
                org_id=none(),
                user=none(),
                scopes=['directory:some-scope'],
            )
        )

    def test_good_admin_ticket(self):
        # Проверяем, что если тикет соответствует внутренней админке
        # Коннекта (INTERNAL_ADMIN_SERVICE_SLUG), и uid пользователя начинается
        # с 112000..., то в результатах будет TeamUser.
        user_id = 1120000000075307
        user_ip = '192.168.1.1'

        headers = Headers({
            # Нам обязательно нужен IP клиента от Qloud
            'X-Real-IP': '127.0.0.1',
            'X-User-IP': user_ip,
            'X-Ya-User-Ticket': 'tttt',
        })
        dashboard_client_id = 100500
        ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[dashboard_client_id],
            scopes=[scope.internal_admin],
        )

        self.set_service_scopes(scope.internal_admin)

        # Сделаем вид, что тикет расшифровался и соответствует
        # сервису с tvm_client_id 42, про который директория знает
        self.mocked_tvm2_client.parse_service_ticket.return_value = FakeTVM2Data(dashboard_client_id)
        self.mocked_tvm2_client.parse_user_ticket.return_value = FakeTVM2UserData([user_id], user_id)

        # Так как мы замокали parse_ticket, то тут всё равно какое
        # значение мы передаём.
        result = authenticate_by_tvm2('some_ticket', headers, True)
        assert_that(
            result,
            has_entries(
                auth_type='tvm2',
                service=has_properties(
                    is_internal=True,
                    identity=INTERNAL_ADMIN_SERVICE_SLUG,
                ),
                org_id=none(),
                user=instance_of(TeamUser),
                scopes=[scope.internal_admin],
            )
        )


class TestAuthMiddleware(TestCase):
    def test_error_during_auth(self):
        # если в middleware случится неизвестное исключение то мы вернем json c ошибкой
        # в ожидаемом формате, а не стандартную страницу от werkzeug

        with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.get_view_function', side_effect=Exception), \
             app.test_request_context('/'):
            response = AuthMiddleware(app).authenticate_and_authorize()
            assert_that(
                response.status_code,
                equal_to(500)
            )
            json_response = json.loads(response.data)
            assert_that(
                json_response,
                has_entries(
                    code='unhandled_exception',
                    message=contains_string('Unhandled exception:')
                )
            )

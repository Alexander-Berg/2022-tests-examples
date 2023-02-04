# coding: utf-8

import hashlib
import math
from datetime import datetime

from hamcrest import (
    assert_that,
    has_entries,
    not_none,
    none,
    equal_to,
)

from testutils import (
    assert_called_once,
    TestCase,
    get_auth_headers,
    create_department,
    create_service,
    create_outer_admin,
    TestOrganizationWithoutDomainMixin,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ServiceModel,
    ServicesLinksModel,
    UserModel,
    UserMetaModel,
    InviteModel,
    DepartmentModel,
    OrganizationModel,
    UsedInviteModel,
    DomainModel,
    OrganizationMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    add_existing_user,
)
from intranet.yandex_directory.src.yandex_directory.core.features import (
    MULTIORG,
    is_feature_enabled,
    set_feature_value_for_organization,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    generate_secret_key_for_invitation,
)


class TestUseInviteView(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestUseInviteView, self).setUp()
        # id пользователя в паспорте (создан при регистрации через паспорт)
        self.user_uid = 10
        # userinfo из ЧЯ
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': self.user_uid,
            'default_email': 'default@ya.ru',
        }
        self.dep_id = 5
        self.org_id = self.yandex_organization['id']
        create_department(
            self.main_connection,
            org_id=self.org_id,
            dep_id=self.dep_id,
        )
        self.headers = get_auth_headers(as_uid=self.user_uid)
        self.default_counter = app.config['DEFAULT_INVITE_LIMIT']
        ServicesLinksModel(self.meta_connection).create(
            slug='portal',
            data_by_tld={
                'ru': {'url': 'https://portal.yandex.ru'},
                'com': {'url': 'https://portal.yandex.com'}
            },
        )
        self.portal = ServiceModel(self.meta_connection).create(
            slug='portal',
            name='Portal',
        )
        self.tracker = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='Tracker',
            redirect_tld={
                'ru': {'url': 'https://tracker.yandex.ru'},
                'com': {'url': 'https://tracker.yandex.com'}
            },
        )
        self.create_invite()

    def create_invite(self, service='portal', wait=5):
        self.wait = wait
        self.code = InviteModel(self.meta_connection).create(
            self.org_id,
            self.dep_id,
            self.admin_uid,
            wait=self.wait,
            service_slug=service,
        )
        self.url = '/invites/{}/use/'.format(self.code)

    def test_add_user(self):
        # Пользователь не состоит ни в одной организации.
        # Проверяем, что он добавится в организацию в указанный отдел,
        # и что инвайт код будет помечен как использованный.
        # Проверяем, что обновилось количество пользователей в отделе и в организаци.
        response = self.post_json(
            self.url,
            data=None, # Эта ручка должна уметь принимать POST без тела
            headers=self.headers,
        )
        org_id = self.yandex_organization['id']
        sk = generate_secret_key_for_invitation(self.user_uid)

        assert_that(
            response,
            has_entries(
                org_id=org_id,
                wait=self.wait,
                # по умолчанию сервис - portal, а для него всегда редиректим на фиксированный URL
                redirect_to='/portal/context?org_id={}&mode=portal&retpath=%2Fportal%2Fhome&sk={}'.format(org_id, sk),
            )
        )
        assert_that(
            UserModel(self.main_connection).get(self.user_uid),
            has_entries(
                org_id=self.org_id,
                department_id=self.dep_id,
                role='user',
            )
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.code),
            has_entries(
                last_use=not_none(),
                enabled=True,
                counter=self.default_counter-1,
            )
        )
        assert_that(
            UsedInviteModel(self.meta_connection).get(self.code, self.user_uid),
            has_entries(
                user_id=self.user_uid,
                created_at=not_none(),
                org_id=self.org_id,
                code=self.code,
            )
        )
        assert_that(
            DepartmentModel(self.main_connection).get(self.dep_id, self.org_id)['members_count'],
            equal_to(1)
        )
        assert_that(
            OrganizationModel(self.main_connection).get(self.org_id)['user_count'],
            equal_to(3)
        )
        assert_called_once(
            self.mocked_passport.set_organization_ids,
            self.user_uid,
            [self.org_id],
        )

    def test_use_custom_tld(self):
        # Если в ручку передан TLD, то урл для редиректа будет выбран
        # в соответствии с политикой fallback. Например если запрашивается
        # com.tr, а сервис есть только на com, то будет отдан .com url.
        self.create_invite(service='tracker', wait=60)

        response = self.post_json(
            self.url,
            data={'tld': 'com.tr'},
            headers=self.headers,
        )
        org_id = self.yandex_organization['id']

        sk = generate_secret_key_for_invitation(self.user_uid)

        assert_that(
            response,
            has_entries(
                org_id=org_id,
                wait=60,
                redirect_to='/portal/context?org_id={}&retpath=https%3A%2F%2Ftracker.yandex.com&sk={}'.format(org_id, sk),
            )
        )

    def test_additional_keys_in_body_should_be_ignored(self):
        # Не знаю почему, но фронт зачем-то отправляет нам в body
        # ключи code и uid, и из-за этого у нас даже сломался релиз,
        # так как эти дополнительные ключи схемой считались невалидными:
        # https://st.yandex-team.ru/DIR-6975#5cf92aa9162e62001e053a71
        response = self.post_json(
            self.url,
            data={'code': self.code, 'uid': self.user['id']},
            headers=self.headers,
        )
        org_id = self.yandex_organization['id']

        sk = generate_secret_key_for_invitation(self.user_uid)

        assert_that(
            response,
            has_entries(
                org_id=org_id,
                wait=self.wait,
                # по умолчанию сервис - portal, а для него всегда редиректим на фиксированный URL
                redirect_to='/portal/context?org_id={}&mode=portal&retpath=%2Fportal%2Fhome&sk={}'.format(org_id, sk),
            )
        )

    def test_add_user_removed_department(self):
        # Пользователь не состоит ни в одной организации.
        # Отдел, который был указан при генерации ивайт кода, больше не существует.
        # Проверяем, чтоб добавиться в организацию в корневой отдел,
        # и что инвайт код будет помечен как использованный
        DepartmentModel(self.main_connection).\
            filter(id=self.dep_id,org_id=self.org_id).\
            delete()
        response = self.post_json(self.url,
                       data={},
                       headers=self.headers,
                       )
        assert_that(
            response,
            has_entries(
                org_id=self.yandex_organization['id'],
            )
        )
        assert_that(
            UserModel(self.main_connection).get(self.user_uid),
            has_entries(
                org_id=self.org_id,
                department_id=ROOT_DEPARTMENT_ID,
                role='user',
            )
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.code),
            has_entries(
                last_use=not_none(),
                enabled=True,
                counter=self.default_counter-1,
            )
        )
        assert_that(
            UsedInviteModel(self.meta_connection).get(self.code, self.user_uid),
            has_entries(
                user_id=self.user_uid,
                created_at=not_none(),
                org_id=self.org_id,
                code=self.code,
            )
        )

    def test_add_user_not_found_in_blackbox(self):
        # Пользователь не найден в ЧЯ.
        # Проверяем, что возвращается ошибка
        self.mocked_blackbox.userinfo.return_value['uid'] = None
        response = self.post_json(self.url,
                       data={},
                       headers=self.headers,
                       expected_code=422,
                       )
        assert_that(
            response,
            has_entries(
                code='user_does_not_exist',
                message='User does not exist',
            )
        )

    def test_add_user_already_member_of_organization(self):
        # Пользователь уже состоит в какой-то организации.
        # Проверяем, что возвращается ошибка
        add_existing_user(
            self.meta_connection,
            self.main_connection,
            self.org_id,
            self.user_uid
        )
        response = self.post_json(self.url,
                       data={},
                       headers=self.headers,
                       expected_code=409,
                       )
        assert_that(
            response,
            has_entries(
                code='has_organization',
                message='User already a member of some organization',
            )
        )

    def test_outer_admin_can_accept_invite(self):
        # Внейшний админ может принять инвайт и состоять в одной организации как пользователь
        uid, org_ids, revisions = create_outer_admin(self.meta_connection, self.main_connection)
        headers = get_auth_headers(as_uid=uid)

        response = self.post_json(
            self.url,
            data={},
            headers=headers,
        )
        assert_that(
            response,
            has_entries(
                org_id=self.yandex_organization['id'],
            )
        )
        assert_that(
            UserModel(self.main_connection).get(uid),
            has_entries(
                org_id=self.org_id,
                department_id=self.dep_id,
                role='user',
            )
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.code),
            has_entries(
                last_use=not_none(),
                enabled=True,
                counter=self.default_counter-1,
            )
        )
        assert_that(
            UsedInviteModel(self.meta_connection).get(self.code, uid),
            has_entries(
                user_id=uid,
                created_at=not_none(),
                org_id=self.org_id,
                code=self.code,
            )
        )
        assert_that(
            DepartmentModel(self.main_connection).get(self.dep_id, self.org_id)['members_count'],
            equal_to(1)
        )
        assert_that(
            OrganizationModel(self.main_connection).get(self.org_id)['user_count'],
            equal_to(3)
        )
        assert_called_once(
            self.mocked_passport.set_organization_ids,
            uid,
            [self.org_id],
        )

    def test_add_user_organization_not_exists(self):
        # Организация из инвайт кода больше не существует.
        # Проверяем, что возвращается ошибка
        OrganizationModel(self.main_connection).remove_all_data_for_organization(self.org_id)
        self.post_json(self.url,
                       data={},
                       headers=self.headers,
                       expected_code=404,
                       )

    def test_add_user_invite_code_not_exists(self):
        # Инвайт код не найден.
        # Проверяем, что возвращается ошибка
        InviteModel(self.meta_connection).delete_one(self.code)
        self.post_json(self.url,
                       data={},
                       headers=self.headers,
                       expected_code=404,
                       )

    def test_add_portal_user_same_nickname_like_existing_feature_enabled(self):
        # В организации есть доменный аккаунт и фича "MULTIORG" включена.
        # Пытаемся добавить портальный аккаунт с таким же никнеймом
        # Не должно быть ошибки

        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        DomainModel(self.main_connection).create('forest.com', self.org_id, owned=True)
        self.create_user(1, nickname='mr-fox', org_id=self.org_id, email='mr-fox@forest.com')
        self.post_json(
            self.url,
            data={},
            headers=self.headers,
            expected_code=201,
        )
        assert_that(
            UserMetaModel(self.meta_connection).filter(org_id=self.org_id, id=self.user_uid).one(),
            not_none(),
        )
        assert_that(
            UserModel(self.main_connection).filter(org_id=self.org_id, id=self.user_uid).one(),
            not_none(),
        )

    def test_check_redirect_from_business_mail(self):
        org_id = self.yandex_organization['id']
        # создадим сервис с redirect_tld
        slug = 'service_with_redirect_tld'
        redirect_tld = {'ru': {'url': 'https://example.ru'}, 'com': {'url': 'https://example.com'}}
        ServiceModel(self.meta_connection).create(
            slug=slug,
            name='Сервис с редиректом',
            redirect_tld=redirect_tld,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            slug,
        )

        self.create_invite(service=slug, wait=60)
        response = self.post_json(
            self.url,
            data={'tld': 'ru'},
            headers=self.headers,
        )

        sk = generate_secret_key_for_invitation(self.user_uid)
        assert_that(
            response,
            has_entries(
                org_id=org_id,
                wait=60,
                redirect_to='/portal/context?org_id={}&retpath=https%3A%2F%2Fexample.ru&sk={}'.format(org_id, sk),
            )
        )

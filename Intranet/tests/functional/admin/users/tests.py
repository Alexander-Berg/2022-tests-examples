# -*- coding: utf-8 -*-
from unittest.mock import patch

from hamcrest import (
    assert_that,
    has_entries,
    contains_inanyorder,
    equal_to,
)

from functional.api.users.tests import BaseMixin
from testutils import (
    TestCase,
    tvm2_auth,
    create_organization,
    create_user,
    tvm2_auth_success,
)
from intranet.yandex_directory.src.yandex_directory.auth.service import Service
from intranet.yandex_directory.src.yandex_directory.core.idm import ADMIN_ROLES
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    ServiceModel,
    OrganizationServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import INTERNAL_ADMIN_SERVICE_SLUG
from intranet.yandex_directory.src.yandex_directory.core.utils import prepare_user
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope


class TestAdminUserDetailView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminUserDetailView, self).setUp(*args, **kwargs)
        tvm2_client_id = 42

        self.organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )

        self.user = self.create_user(
            nickname='tester',
            name={'first': 'Petya', 'last': 'Ivanov'},
            email='tester@test.com',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            self.service['id'],
            True,
        )

        self.tvm2_service = Service(
                    id=self.service['id'],
                    name=self.service['name'],
                    identity=self.service['slug'],
                    is_internal=True,
                    ip='127.0.0.1',
                )

    def test_should_return_user_detail_info(self):
        # проверим, что ручка отдает детальную информацию о пользователе
        # UserMetaModel(self.meta_connection).create(100700, self.organization['id'])
        TVM2_HEADERS = {
            'X-Ya-Service-Ticket': 'qqq',
        }
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            response = self.get_json(
                '/admin/users/%s/' % self.user['id'],
                TVM2_HEADERS,
            )

        user = UserModel(self.main_connection).find(filter_data={'id': self.user['id']}, one=True)

        exp_response = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )

        assert_that(response, equal_to(exp_response))


class TestUserContactsView_test(BaseMixin, TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestUserContactsView_test, self).setUp()
        tvm2_client_id = 42
        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )
        self.tvm2_service = Service(
            id=self.service['id'],
            name=self.service['name'],
            identity=self.service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_contacts(self):
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.app.blackbox_instance.userinfo') as mock_account:
            user = {'account_uids': [],
                    'bruteforce_policy': {'captcha': False,
                                          'level': None,
                                          'password_expired': False},
                    'default_email': 'user@pilesos.com',
                    'domain': None,
                    'elem_count': None,
                    'emails': [{"address": "krutoy_paren1999@pilesos.com",
                                "born-date": "007-08-09 13:37:00",
                                "default": False,
                                "native": False,
                                "rpop": False,
                                "silent": False,
                                "unsafe": True,
                                "validated": True
                                }, ],
                    'error': None,
                    'fields': {'aliases': [('1', 'kek'), ('13', 'mem')],
                               'display_name': 'best_programmer',
                               'social': None,
                               'social_aliases': None,
                               'phones': ['88005553535']},
                    'display_name': {
                        'name': 'best_programmer',
                        'avatar': {
                            'default': '31523715',
                            'empty': False,
                        }
                    },
                    'hosted_domains': None,
                    'karma': '0',
                    'lite_uid': None,
                    'login_status': None,
                    'oauth': None,
                    'password_status': None,
                    'status': None,
                    'total_count': None,
                    'uid': '1337228'
                    }
            mock_account.return_value = user

            userinfo = self.get_json('/admin/users/%s/contacts/' % (123456))

            ans = {
                'uid': '1337228',
                'phone_number': '88005553535',
                'email': ["krutoy_paren1999@pilesos.com"]
            }
            assert (userinfo == ans)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_no_email(self):
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.app.blackbox_instance.userinfo') as mock_account:
            user = {'account_uids': [],
                    'bruteforce_policy': {'captcha': False,
                                          'level': None,
                                          'password_expired': False},
                    'default_email': 'user@pilesos.com',
                    'domain': None,
                    'elem_count': None,
                    'emails': [{"address": "krutoy_paren1999@pilesos.com",
                                "born-date": "007-08-09 13:37:00",
                                "default": False,
                                "native": False,
                                "rpop": True,
                                "silent": False,
                                "unsafe": True,
                                "validated": True
                                }, ],
                    'error': None,
                    'fields': {'aliases': [('1', 'kek'), ('13', 'mem')],
                               'display_name': 'best_programmer',
                               'social': None,
                               'social_aliases': None,
                               'phones': ['88005553535']},
                    'display_name': {
                        'name': 'best_programmer',
                        'avatar': {
                            'default': '31523715',
                            'empty': False,
                        }
                    },
                    'hosted_domains': None,
                    'karma': '0',
                    'lite_uid': None,
                    'login_status': None,
                    'oauth': None,
                    'password_status': None,
                    'status': None,
                    'total_count': None,
                    'uid': '1337228'
                    }
            mock_account.return_value = user

            userinfo = self.get_json('/admin/users/%s/contacts/' % (123456))

            ans = {
                'uid': '1337228',
                'phone_number': '88005553535',
                'email': []
            }
            assert (userinfo == ans)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_no_number(self):
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.app.blackbox_instance.userinfo') as mock_account:
            user = {'account_uids': [],
                    'attributes': {},
                    'bruteforce_policy': {'captcha': False,
                                          'level': None,
                                          'password_expired': False},
                    'default_email': 'user@pilesos.com',
                    'domain': None,
                    'elem_count': None,
                    'emails': [{"address": "krutoy_paren1999@pilesos.com",
                                "born-date": "007-08-09 13:37:00",
                                "default": False,
                                "native": False,
                                "rpop": True,
                                "silent": False,
                                "unsafe": True,
                                "validated": True
                                }, ],
                    'error': None,
                    'fields': {'aliases': [('1', 'kek'), ('13', 'mem')],
                               'display_name': 'best_programmer',
                               'social': None,
                               'social_aliases': None,
                               'phones': []},
                    'display_name': {
                        'name': 'best_programmer',
                        'avatar': {
                            'default': '31523715',
                            'empty': False,
                        }
                    },
                    'hosted_domains': None,
                    'karma': '0',
                    'lite_uid': None,
                    'login_status': None,
                    'oauth': None,
                    'password_status': None,
                    'status': None,
                    'total_count': None,
                    'uid': '1337228'
                    }
            mock_account.return_value = user

            userinfo = self.get_json('/admin/users/%s/contacts/' % (123456))

            ans = {
                'uid': '1337228',
                'phone_number': None,
                'email': []
            }
            assert (userinfo == ans)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_not_found_uid(self):
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.app.blackbox_instance.userinfo') as mock_account:
            user = None
            mock_account.return_value = user

            userinfo = self.get_json('/admin/users/%s/contacts/' % (123456),
                                     expected_code=404)


class TestAdminAdminDetailView(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_user_roles(self):
        TVM2_HEADERS = {'X-Ya-Service-Ticket': 'qqq'}
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.users.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = [ADMIN_ROLES.support, ADMIN_ROLES.admin]
            response = self.get_json(
                '/admin/user/',
                headers=TVM2_HEADERS,
            )
        assert_that(
            response,
            has_entries(
                roles=contains_inanyorder(ADMIN_ROLES.support, ADMIN_ROLES.admin)
            )
        )

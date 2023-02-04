# coding: utf-8

import json
import requests
import unittest.mock
from unittest.mock import (
    patch,
    Mock,
    ANY)
from hamcrest import *
from werkzeug.datastructures import FileMultiDict
from intranet.yandex_directory.src.yandex_directory.common.utils import get_localhost_ip_address

from testutils import (
    TestCase,
    mocked_requests,
    assert_called_once,
    assert_not_called,
    fake_userinfo,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import url_join
from intranet.yandex_directory.src.yandex_directory.core.utils import build_email
from intranet.yandex_directory.src.yandex_directory.passport.client import (
    PassportApiClient,
    raise_exception_if_errors,
)
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    BirthdayInvalid,
    LoginLong,
    PassportUnavailable,
    DomainAliasExists,
    DomainAlreadyExists,
    DomainInvalid,
    DomainNotFound,
)


class TestPassportApiClient(TestCase):
    def setUp(self):
        super(TestPassportApiClient, self).setUp()
        self.passport_client = PassportApiClient()

    def test_account_add_pass(self):
        domain = b'domain.com'
        user_data = {
            'login': 'login',
            'password': 'password',
            'firstname': 'firstname',
            'lastname': 'lastname',
            'birthday': '01-12-1992',
            'gender': 'm',
            'language': 'ru',
        }

        with mocked_requests() as requests:
            self.passport_client.account_add(domain=domain, user_data=user_data)

            url = url_join(
                app.config['PASSPORT_API'], '/1/bundle/account/register/pdd/',
                query_params={'consumer': 'directory'},
            )
            user_data['domain'] = domain
            expected_call = unittest.mock.call(url=url, data=user_data, headers=self.passport_client.headers)
            self.assertEqual(requests.post.call_args, expected_call)

    def test_account_add_hash(self):
        domain = b'domain.com'
        user_data = {
            'login': 'login',
            'password_hash': 'hash',
            'firstname': 'firstname',
            'lastname': 'lastname',
            'birthday': '01-12-1992',
            'gender': 'm',
            'language': 'ru',
        }

        with mocked_requests() as requests:
            self.passport_client.account_add(domain=domain, user_data=user_data)

            url = url_join(
                app.config['PASSPORT_API'], '/1/bundle/account/register/pdd/',
                query_params={'consumer': 'directory'},
            )
            user_data['domain'] = domain
            expected_call = unittest.mock.call(data=user_data, url=url, headers=self.passport_client.headers)

            self.assertEqual(requests.post.call_args, expected_call)

    def test_account_edit(self):
        user_data = {
            'password': 'password',
            'firstname': 'firstname',
            'lastname': 'lastname',
            'birthday': '01-12-1992',
            'gender': 'male',
        }
        with mocked_requests() as requests:
            self.passport_client.account_edit(user_data=user_data)

            url = url_join(
                app.config['PASSPORT_API'], '/1/bundle/account/person/',
                query_params={'consumer': 'directory'},
            )
            expected_call = unittest.mock.call(data=user_data, url=url, headers=self.passport_client.headers)

            self.assertEqual(requests.post.call_args, expected_call)

    def test_change_password_user_should_call_passport_api_with_change_password_request(self):
        user_uid = 123
        new_password = 'new_password'
        track_id = 321
        with mocked_requests() as requests:
            requests.post.return_value.json.side_effect = [
                # Сначала делается вызов change_passport_submit,
                # и он должен вернуть trackid
                {'track_id': track_id, 'status': 'ok'},
                # а последующий change_password_commit просто должен вернуть ok
                {'status': 'ok'},
            ]

            self.passport_client.change_password(uid=user_uid, new_password=new_password)
            expected_call = unittest.mock.call(
                url=url_join(
                    app.config['PASSPORT_API'], '/1/account/%s/flush_pdd/commit/' % user_uid,
                    query_params={'consumer': 'directory'},
                ),
                data={'password': new_password,
                 'track_id': track_id},
                headers=self.passport_client.headers,
            )
            self.assertEqual(requests.post.call_count, 2)
            self.assertEqual(requests.post.call_args, expected_call)

    def test_change_password_should_not_log_password(self):
        user_uid = 123

        with mocked_requests() as requests, \
                patch.object(PassportApiClient, '_write_log', Mock()) as mocked_write_log:
            requests.post.return_value.json.side_effect = [
                {'track_id': 312, 'status': 'ok'},
                {'status': 'ok'}
            ]
            self.passport_client.change_password(uid=user_uid, new_password='new_password')
            url_commit = url_join(
                app.config['PASSPORT_API'], '/1/account/%s/flush_pdd/commit/' % user_uid,
                query_params={'consumer': 'directory'},
            )
            self.assertEqual(mocked_write_log.call_count, 2)

            expected_call = (url_commit, '[REMOVED]')
            msg = 'Не нужно логировать пароль пользователя при запросе в апи паспорта'
            self.assertEqual(mocked_write_log.call_args[0][:2], expected_call, msg=msg)

    def test_block_user_should_call_passport_api_with_block_request(self):
        user_uid = 111 * 10 ** 13
        with mocked_requests() as requests:
            self.passport_client.block_user(uid=user_uid)
            assert_called_once(
                requests.post,
                url=url_join(
                    app.config['PASSPORT_API'], '/1/account/%s/' % user_uid,
                    query_params={'consumer': 'directory'}
                ),
                data={'is_enabled': False},
                headers=self.passport_client.headers,
            )

    def test_raise_exception_if_errors_should_raise_birthday_invalid_exception(self):
        passport_response = {
            'status': 'error',
            'errors': ['birthday.invalid'],
        }

        with self.assertRaises(BirthdayInvalid):
            raise_exception_if_errors(json_response=passport_response)

    def test_raise_exception_if_errors_should_raise_domain_invalid_exception(self):
        passport_response = {
            'status': 'error',
            'errors': ['domain.invalid'],
        }

        with self.assertRaises(DomainInvalid):
            raise_exception_if_errors(json_response=passport_response)

    def test_unblock_user_should_call_passport_api_with_block_request(self):
        user_uid = 111 * 10 ** 13 + 1
        with mocked_requests() as requests:
            self.passport_client.unblock_user(uid=user_uid)
            assert_called_once(
                requests.post,
                url=url_join(
                    app.config['PASSPORT_API'], '/1/account/%s/' % user_uid,
                    query_params={'consumer': 'directory'},
                ),
                data={'is_enabled': True},
                headers=self.passport_client.headers,
            )

    def test_change_avatar_by_file(self):
        # обрачиваем изображение в тип werkzeug.datastructures.FileStorage
        # и передаем в ф-цию change_avatar
        user_uid = 123
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/passport/data/scream.jpg'
        )
        img = open(file_path, 'rb')
        filesdict.add_file('avatar_file', img, filename='scream.jpg')
        file_img = filesdict.get('avatar_file')

        with mocked_requests() as requests:
            requests.post.return_value.json.return_value = {'status': 'ok'}
            self.passport_client.change_avatar(uid=user_uid, file_img=file_img)
            assert_called_once(
                requests.post,
                url=url_join(
                    app.config['PASSPORT_API'], '/2/change_avatar/',
                    query_params={'consumer': 'directory'},
                ),
                data={'default': True, 'uid': user_uid},
                files={
                    'file': (file_img.filename, file_img.stream, file_img.mimetype)
                },
                headers={
                    'Ya-Client-Host': 'yandex.ru',
                    'Ya-Consumer-Client-Ip': get_localhost_ip_address(),
                    'Ya-Consumer-Client-Scheme': 'https',
                },
            )

    def test_change_avatar_by_url(self):
        # проверяем, что url отправляется post-data
        user_uid = 123
        img_url = 'ya.ru/pic'

        with mocked_requests() as requests:
            requests.post.return_value.json.return_value = {'status': 'ok'}
            self.passport_client.change_avatar(uid=user_uid, url=img_url)
            assert_called_once(
                requests.post,
                url=url_join(
                    app.config['PASSPORT_API'], '/2/change_avatar/',
                    query_params={'consumer': 'directory'},
                ),
                data={'default': True, 'uid': user_uid, 'url': img_url},
                files=None,
                headers={
                    'Ya-Client-Host': 'yandex.ru',
                    'Ya-Consumer-Client-Ip': get_localhost_ip_address(),
                    'Ya-Consumer-Client-Scheme': 'https',
                },
            )

    def test_add_alias(self):
        user_uid = 123
        alias = 'new_alias'
        with mocked_requests() as requests:
            requests.post.return_value.json.return_value = {'status': 'ok'}
            self.passport_client.alias_add(user_uid, alias)
            url_template = '/1/account/{uid}/alias/pddalias/{alias}/'.format(uid=user_uid, alias=alias)
            assert_called_once(
                requests.post,
                url=url_join(
                    app.config['PASSPORT_API'],
                    url_template,
                    query_params={'consumer': 'directory'},
                ),
                data={},
                headers=self.passport_client.headers,
            )

    def test_alias_delete(self):
        user_uid = 4034386806
        alias = 'new_alias'
        with mocked_requests() as requests:
            requests.delete.return_value.json.return_value = {'status': 'ok'}
            self.passport_client.alias_delete(user_uid, alias)

            url_template = '/1/account/{uid}/alias/pddalias/{alias}/'.format(uid=user_uid, alias=alias)
            assert_called_once(
                requests.delete,
                url=url_join(
                    app.config['PASSPORT_API'],
                    url_template,
                    query_params={'consumer': 'directory'}
                ),
                headers=self.passport_client.headers,
            )

    def test_account_delete(self):
        with mocked_requests() as requests:
            outer_uid = 123
            self.passport_client.account_delete(outer_uid)
            assert_not_called(requests.delete)

            inner_uid = 111 * 10 ** 13 + 1
            self.passport_client.account_delete(inner_uid)
            url_template = '/1/bundle/account/{uid}/'.format(uid=inner_uid)

            assert_called_once(
                requests.delete,
                url=url_join(
                    app.config['PASSPORT_API'],
                    url_template,
                    query_params={'consumer': 'directory'}
                ),
                headers=self.passport_client.headers,
            )

    def test_ignore_account_not_found_exception(self):
        with mocked_requests() as requests:
            requests.post.return_value.json.return_value = {
                'status': 'error',
                'errors': ['account.not_found']
            }
            status = self.passport_client.change_password(123, 'password')
            self.assertEqual(status, True)

    def test_validate_login(self):
        login = 'cool_guy@company.cccp'
        track_id = 321

        with mocked_requests() as requests, \
                self.assertRaises(LoginLong):
            requests.post.return_value.json.side_effect = [
                # validate_login сначала создаёт track
                {'id': track_id, 'status': 'ok'},
                # а затем использует его для проверки логина
                # тут то мы ему ошибку и покажем
                {'status': 'error', 'errors': ['login.long']},
            ]
            self.passport_client.validate_login(login)

    def test_client_should_return_503_on_network_errors(self):
        # Если во время запроса в паспорт случилась какая-то сетевая ошибка,
        # то нужно вернуть 503 код с помощь исключения PassportUnavailable
        self.stop_patchers()

        for exc in (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
            with mocked_requests() as requests_lib:
                requests_lib.post.side_effect = exc

                with self.assertRaises(PassportUnavailable):
                    self.passport_client._make_post_request('fake-url', {})

    def test_ignore_domain_already_in_passport_exception(self):
        admin_uid = 123
        domain_name = 'domain.com'
        with mocked_requests() as requests:

            requests.post.return_value.json.return_value = {
                'status': 'error',
                'errors': ['domain.already_exists']
            }

            # проверим, что исключения не будет,
            # если домен мастер и принадлежит этому же админу
            self.mocked_blackbox.hosted_domains.return_value = {
                "hosted_domains": [{
                    'admin': admin_uid,
                    'born_date': '2018-01-01 13:22:25',
                    'domid': 666,
                    'master_domain': None,
                    'mx': 0,
                }]
            }
            self.passport_client.domain_add(domain_name, admin_uid)

            # вернем исключение, если домен принадлежит другому админу
            self.mocked_blackbox.hosted_domains.return_value = {
                "hosted_domains": [{
                    'admin': 1111,
                    'born_date': '2018-01-01 13:22:25',
                    'domid': 666,
                    'master_domain': None,
                    'mx': 0,
                }]
            }
            with self.assertRaises(DomainAlreadyExists):
                self.passport_client.domain_add(domain_name, admin_uid)

            # вернем исключение, если домен вляется алиасом
            self.mocked_blackbox.hosted_domains.return_value = {
                "hosted_domains": [{
                    'admin': admin_uid,
                    'born_date': '2018-01-01 13:22:25',
                    'domid': 666,
                    'master_domain': 'some_domain.com',
                    'mx': 0,
                }]
            }
            with self.assertRaises(DomainAlreadyExists):
                self.passport_client.domain_add(domain_name, admin_uid)

    def test_ignore_alias_already_in_passport_exception(self):
        admin_uid = 666
        alias_name = 'domain.com'
        master_domain_id = 123
        with mocked_requests() as requests:

            requests.post.return_value.json.return_value = {
                'status': 'error',
                'errors': ['domain_alias.exists']
            }

            # проверим, что исключения не будет,
            # если домен уже алиас этого мастер домена
            self.mocked_blackbox.hosted_domains.side_effect = [
                {
                    "hosted_domains": [{
                        'admin': admin_uid,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': 666,
                        'master_domain': 'some_domain.com',
                        'mx': 0,
                    }]
                },
                {
                    "hosted_domains": [{
                        'admin': admin_uid,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': master_domain_id,
                        'master_domain': None,
                        'mx': 0,
                    }]
                },
            ]
            self.passport_client.domain_alias_add(master_domain_id, alias_name)

            # вернем исключение, если алиас принадлежит другому мастер домену
            self.mocked_blackbox.hosted_domains.side_effect = [
                {
                    "hosted_domains": [{
                        'admin': admin_uid,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': 666,
                        'master_domain': 'some_domain.com',
                        'mx': 0,
                    }]
                },
                {
                    "hosted_domains": [{
                        'admin': admin_uid,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': 7777,
                        'master_domain': None,
                        'mx': 0,
                    }]
                },
            ]
            with self.assertRaises(DomainAliasExists):
                self.passport_client.domain_alias_add(master_domain_id, alias_name)

    def test_ignore_alias_not_found(self):
        with mocked_requests() as requests:
            requests.delete.return_value.json.return_value = {
                'status': 'error',
                'errors': ['alias.not_found']
            }
            status = self.passport_client.alias_delete(123, 'alias.com')
            self.assertEqual(status, True)

    def test_ignore_domain_alias_not_found(self):
        with mocked_requests() as requests:
            requests.delete.return_value.json.return_value = {
                'status': 'error',
                'errors': ['domain_alias.not_found']
            }
            status = self.passport_client.domain_alias_delete(123, 'alias.com')
            self.assertEqual(status,  {'status': 'ok'})

    def test_domain_edit_dont_override_explicit(self):
        with mocked_requests() as requests:
            self.mocked_blackbox.hosted_domains.return_value = {
                "hosted_domains": [
                    {
                        'admin': 100500,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': 100500,
                        'master_domain': None,
                        'mx': 0,
                        'ena': 1,
                        'default_uid': self.user['id'],
                        'options': json.dumps({
                            "organization_name": "FooBar",
                            "can_users_change_password": "1",
                        }),
                    },
                ],
            }
            requests.post.return_value.json.return_value = {'status': 'ok'}

            status = self.passport_client.domain_edit(
                100500,
                {
                    'mx': 1,
                    'enabled': 0,
                    'admin_uid': 100501,
                    'default': 'test-test-test',
                    'organization_name': 'FooBaz',
                    'can_users_change_password': 0,
                }
            )

            assert_that(status, True)
            assert_that(
                requests.post,
                has_properties(
                    called=True,
                    call_count=1,
                    call_args=has_item(
                        has_entries(
                            data={
                                'mx': 1,
                                'enabled': 0,
                                'admin_uid': 100501,
                                'default': 'test-test-test',
                                'organization_name': 'FooBaz',
                                'can_users_change_password': 0,
                            },
                        ),
                    ),
                ),
            )

    def test_domain_edit_with_absent_bb_options(self):
        with mocked_requests() as requests:
            self.mocked_blackbox.hosted_domains.return_value = {
                "hosted_domains": [
                    {
                        'admin': 100500,
                        'born_date': '2018-01-01 13:22:25',
                        'domid': 100500,
                        'master_domain': None,
                        'mx': 0,
                        'ena': 1,
                        'default_uid': self.user['id'],
                    },
                ],
            }
            requests.post.return_value.json.return_value = {'status': 'ok'}

            status = self.passport_client.domain_edit(100500, {})

            assert_that(status, True)
            assert_that(
                requests.post,
                has_properties(
                    called=True,
                    call_count=1,
                    call_args=not_(
                        has_item(
                            has_item(
                                has_entries(
                                    organization_name=ANY,
                                    can_users_change_password=ANY,
                                ),
                            ),
                        ),
                    ),
                ),
            )

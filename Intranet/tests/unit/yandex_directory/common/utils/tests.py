# -*- coding: utf-8 -*-
import datetime
import json
import time
from operator import attrgetter
import unittest.mock
import psycopg2
import pytest
import pytz
from concurrent.futures import TimeoutError
from flask import (
    g,
)
from hamcrest import (
    assert_that,
    has_entries,
    is_,
    equal_to,
    none,
    calling,
    raises,
    contains,
    contains_inanyorder,
    not_none,
)
from unittest.mock import (
    patch,
    Mock,
    ANY,
)

from testutils import (
    TestCase,
    create_user,
    create_department,
    create_group,
    assert_not_called,
    create_organization,
    mocked_requests,
    assert_called_once,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.auth.user import User
from intranet.yandex_directory.src.yandex_directory.common.exceptions import ImmediateReturn, DomainNotFound, MasterDomainNotFound
from intranet.yandex_directory.src.yandex_directory.core.features import (
    DOMAIN_AUTO_HANDOVER,
    MULTIORG,
    is_feature_enabled,
    set_feature_value_for_organization,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    force_text,
    force_utf8,
    make_simple_strings,
    make_internationalized_strings,
    covert_keys_with_dots_to_items,
    format_date,
    parse_birth_date,
    url_join,
    check_permissions,
    format_datetime,
    check_label_or_nickname_or_alias_is_uniq_and_correct,
    build_order_by,
    find_domid,
    first_or_none,
    get_boolean_param,
    get_environment,
    stopit,
    multikeysort,
    utcnow,
    hide_sensitive_params,
    find_sensitive_params,
    SENSITIVE_DATA_PLACEHOLDER,
    mask_email,
    create_domain_in_passport,
)
from intranet.yandex_directory.src.yandex_directory.core.mailer.tasks import SendEmailToAllTask
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    EventModel,
    OrganizationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    global_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.domain import disable_domain_in_organization
from intranet.yandex_directory.src.yandex_directory.passport import PassportApiClient
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    LoginLong,
    BirthdayInvalid,
)


class Test_covert_keys_with_dots_to_items(TestCase):
    create_organization = False

    def test_experiments(self):
        experiments = [
            {
                'data': {
                    'hello': 'world',
                    'user.name.first.ru': 'Gena',
                    'user.name.last.ru': 'Chibisov',
                    'user.age': 25,
                },
                'expected': {
                    'hello': 'world',
                    'user': {
                        'name': {
                            'first': {
                                'ru': 'Gena'
                            },
                            'last': {
                                'ru': 'Chibisov'
                            }
                        },
                        'age': 25
                    }
                }
            },
            {
                'data': {
                    'hello': 'world',
                    'user.name.first.ru': 'Gena',
                    'user.name.last.ru': 'Chibisov',
                    'department.id': 1,
                    'department.name': 'business',
                },
                'expected': {
                    'hello': 'world',
                    'user': {
                        'name': {
                            'first': {
                                'ru': 'Gena'
                            },
                            'last': {
                                'ru': 'Chibisov'
                            }
                        }
                    },
                    'department': {
                        'id': 1,
                        'name': 'business',
                    }
                }
            },
            {
                'data': {
                    'department.id': 168,
                    'department.parent.id': None,
                },
                'expected': {
                    'department': {
                        'id': 168,
                        'parent': {
                            'id': None
                        }
                    }
                }
            },
            {
                'data': {
                    'department.id': 168,
                    'department.parent.id': 1508,
                },
                'expected': {
                    'department': {
                        'id': 168,
                        'parent': {
                            'id': 1508
                        }
                    }
                }
            }
        ]

        for exp in experiments:
            self.assertEqual(
                covert_keys_with_dots_to_items(exp['data']),
                exp['expected']
            )


class Test_convert_format_date__and__parse_date(TestCase):
    def test_me(self):
        experiments = [
            {
                'date': datetime.date(day=2, month=3, year=1990),
                'string': '1990-03-02'
            },
            {
                'date': datetime.date(day=11, month=12, year=2010),
                'string': '2010-12-11'
            },
        ]

        for exp in experiments:
            self.assertEqual(format_date(exp['date']), exp['string'])
            self.assertEqual(parse_birth_date(exp['string']), exp['date'])

    def test_incorrect_data(self):
        today = utcnow()
        # Возьмем дату в будущем
        next_month_date = (datetime.date.today() + datetime.timedelta(42)).strftime('%Y-%m-%d')
        long_time_ago = datetime.date(today.year - 101, today.month, 1).strftime('%Y-%m-%d')

        experiments = [
            next_month_date,
            long_time_ago,
            '2000-12-40',
            '&5-7y-%',
        ]
        for exp in experiments:
            with pytest.raises(BirthdayInvalid) as err:
                parse_birth_date(exp)
                self.assertEqual(err.value.code, 'invalid_birthday')


def test_url_join():
    assert_that(url_join('http://example.com', '/blah'),
                is_('http://example.com/blah')
    )
    assert_that(url_join('http://example.com', '/blah',
                         query_params={'minor': 'again'}),
                is_('http://example.com/blah?minor=again')
    )
    assert_that(url_join('http://example.com', '/blah',
                         query_params={'minor': 'again'},
                         force_trailing_slash=True),
                is_('http://example.com/blah/?minor=again')
    )
    assert_that(url_join('http://example.com', '/blah?and=again',
                         query_params={'minor': 'again'},
                         force_trailing_slash=True),
                is_('http://example.com/blah/?and=again&minor=again')
    )


class Test_check_permissions(TestCase):
    def test_global(self):
        g.user = User(
            passport_uid=self.user['id'],
            ip='127.0.0.1',
        )
        g.auth_type = 'token'
        g.org_id = self.organization['id']
        result = check_permissions(
            self.meta_connection,
            self.main_connection,
            [global_permissions.add_groups]
        )
        assert_that(result, equal_to(None))

    def test_oauth(self):
        # при авторизации по oauth если нет пользователя то не проверяем права
        g.user = None
        g.auth_type = 'oauth'
        result = check_permissions(
            self.meta_connection,
            self.main_connection,
            [global_permissions.add_groups]
        )
        assert_that(result, equal_to(None))


class Test_format_datetime(TestCase):
    def test_fixed_offset_none(self):
        # В базе данные с временем храняться с time-зоной. Поэтому utcoffset()
        # возвращает не None, а psycopg2.tz.FixedOffsetTimezone
        fixed_offset = psycopg2.tz.FixedOffsetTimezone(offset=0, name=None)
        d_now = utcnow()
        d_with_null_offset = d_now.replace(tzinfo=fixed_offset)
        iso_native_date = format_datetime(d_with_null_offset)
        # проверяем формат '%Y-%m-%dT%H:%M:%S.%fZ'
        check_format = datetime.datetime.strptime(iso_native_date, '%Y-%m-%dT%H:%M:%S.%fZ')
        # Тут надо убрать UTC таймзону с d_now, потому что strptime
        # возвращает datetime без таймзоны
        self.assertEqual(check_format, d_now.replace(tzinfo=None))
        self.assertEqual(check_format.hour, d_with_null_offset.hour)
        self.assertEqual(check_format.minute, d_with_null_offset.minute)
        self.assertEqual(check_format.second, d_with_null_offset.second)

    def test_fixed_offset_delta(self):
        # Проверяем формат, когда время прилетает с offset timezone =
        # psycopg2.tz.FixedOffsetTimezone c некоторым смещение.
        offset_min = 60
        offset = psycopg2.tz.FixedOffsetTimezone(offset=offset_min, name='Custom')
        offset_delta_sec = datetime.timedelta(minutes=offset_min)
        date_now = utcnow()
        # формируем время UTC+1
        date_now_changed = date_now + offset_delta_sec
        date_now_offset_fixed = date_now_changed.replace(tzinfo=offset)
        iso_native_date = format_datetime(date_now_offset_fixed)
        check_format = datetime.datetime.strptime(iso_native_date, '%Y-%m-%dT%H:%M:%S.%fZ')
        # Тут надо убрать UTC таймзону с date_now, потому что strptime
        # возвращает datetime без таймзоны
        self.assertEqual(check_format, date_now.replace(tzinfo=None))
        self.assertNotEqual(check_format.hour, date_now_offset_fixed.hour)
        self.assertEqual(check_format.minute, date_now_offset_fixed.minute)
        self.assertEqual(check_format.second, date_now_offset_fixed.second)

    def test_pytz_moscow_date(self):
        # UTC+3
        moscow_date = datetime.datetime.now(pytz.timezone('Europe/Moscow'))
        utc_date = utcnow()
        iso_native_date = format_datetime(moscow_date)
        check_format = datetime.datetime.strptime(iso_native_date, '%Y-%m-%dT%H:%M:%S.%fZ')
        self.assertEqual(check_format.hour, utc_date.hour)
        self.assertNotEqual(check_format.hour, moscow_date.hour)
        self.assertEqual(check_format.hour, utc_date.hour)

    def test_pytz_alaska_date(self):
        # формируем время UTC-8 (-9)
        alaska_date = datetime.datetime.now(pytz.timezone('US/Alaska'))
        utc_date = utcnow()
        iso_native_date = format_datetime(alaska_date)
        check_format = datetime.datetime.strptime(iso_native_date, '%Y-%m-%dT%H:%M:%S.%fZ')
        self.assertNotEqual(check_format.hour, alaska_date.hour)
        self.assertEqual(check_format.hour, utc_date.hour)

    def test_time_native(self):
        # Проверяем формат, когда время прилетает с offset=None
        dnow = utcnow()
        iso_native_date = format_datetime(dnow)
        check_format = datetime.datetime.strptime(iso_native_date, '%Y-%m-%dT%H:%M:%S.%fZ')
        self.assertEqual(check_format.hour, dnow.hour)


class Test_check_label_or_nickname_or_alias_is_uniq_and_correct(TestCase):
    def setUp(self):
        super(Test_check_label_or_nickname_or_alias_is_uniq_and_correct, self).setUp()

        self.org_id = self.organization['id']
        self.second_org_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='new-org',
        )['organization']['id']

        self.alias_user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=1,
            nickname='nickname',
            name=self.name,
            email='email@yandex.ru',
            org_id=self.org_id,
            aliases=['nickname1', 'nickname2'],
        )
        self.second_user = self.create_user(nickname='test-1', org_id=self.org_id, )

        self.alias_group = create_group(
            self.main_connection,
            org_id=self.org_id,
            label='group',
            aliases=['group1', 'group2'],
        )

        self.alias_department = create_department(
            self.main_connection,
            org_id=self.org_id,
            label='department',
            aliases=['department1', 'department2'],
        )

    def test_uniq_user_nickname(self):
        self.assertIsNone(
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'unique',
                self.org_id
            )
        )

    def test_not_uniq_user_nickname_in_the_organization_with_feature_off(self):
        # проверяем, что нельзя добавить пользователя с никнеймом в организацию,
        # где есть доменный пользователь с таким же никнеймом и фича "MULTIORG" выключена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            False,
        )
        with self.assertRaises(ImmediateReturn):
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-1',
                self.org_id,
            )

    def test_not_uniq_user_nickname_in_the_organization_with_feature_enabled(self):
        # проверяем, что нельзя добавить пользователя с никнеймом в организацию,
        # где есть доменный пользователь с таким же никнеймом и фича "MULTIORG" включена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        with self.assertRaises(ImmediateReturn):
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-1',
                self.org_id,
            )

    def test_not_uniq_user_nickname_in_the_organization_with_feature_enabled_for_cloud(self):
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        check_label_or_nickname_or_alias_is_uniq_and_correct(
            self.main_connection,
            'test-1',
            self.org_id,
            is_cloud=True,
        )

    def test_not_uniq_user_nickname_in_the_organization_this_user_with_feature_off(self):
        # проверяем, что если пользователь состоит в организауии и в user_id
        # передать uid портальной учетки, то функция вернет ошибку, если фича MULTIORG выключена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            False,
        )

        portal_user = self.create_portal_user(
            org_id=self.org_id,
            login='test-nickname',
            email='test-nickname@yandex.ru',
        )
        with self.assertRaises(ImmediateReturn):
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-nickname',
                self.org_id,
                user_id=portal_user['id'],
            )

    def test_not_uniq_user_nickname_in_the_organization_this_user_with_feature_enabled(self):
        # проверяем, что если пользователь состоит в организауии и в user_id
        # передать uid портальной учетки, то функция вернет ошибку, если фича MULTIORG включена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        portal_user = self.create_portal_user(
            org_id=self.org_id,
            login='test-nickname',
            email='test-nickname@yandex.ru',
        )
        with self.assertRaises(ImmediateReturn):
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-nickname',
                self.org_id,
                user_id=portal_user['id'],
            )

    def test_not_uniq_user_nickname_in_different_organization_for_user_id_with_feature_off(self):
        # пользователь состоит в другой огранизации, проверим, что его можно
        # добавить в эту организацию, если фича MULTIORG выключена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            False,
        )
        user = self.create_user(
            org_id=self.second_org_id,
            nickname='test-nickname',
            email='test-nickname@domain.ru',
        )
        check_label_or_nickname_or_alias_is_uniq_and_correct(
            self.main_connection,
            'test-nickname',
            self.org_id,
            user_id=user['id'],
        )

    def test_not_uniq_user_nickname_in_different_organization_for_user_id_with_feature_enabled(self):
        # пользователь состоит в другой огранизации, проверим, что его можно
        # добавить в эту организацию, если фича MULTIORG включена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        user = self.create_user(
            org_id=self.second_org_id,
            nickname='test-nickname',
            email='test-nickname@domain.ru',
        )
        check_label_or_nickname_or_alias_is_uniq_and_correct(
            self.main_connection,
            'test-nickname',
            self.org_id,
            user_id=user['id'],
        )

    def test_not_uniq_user_nickname_in_different_organization_with_feature_off(self):
        # проверяем, что можно добавить пользователя с никнеймом в организацию,
        # если есть пользователь с таким же никнеймом в другой организации, если фича MULTIORG выключена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            False,
        )
        self.assertIsNone(
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-1',
                self.second_org_id,
            )
        )

    def test_not_uniq_user_nickname_in_different_organization_with_feature_enabled(self):
        # проверяем, что можно добавить пользователя с никнеймом в организацию,
        # если есть пользователь с таким же никнеймом в другой организации, если фича MULTIORG включена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )

        self.assertIsNone(
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                'test-1',
                self.second_org_id,
            )
        )

    def test_long_label(self):
        # если label больше 30ти символов - кинем исключение
        default_limit = 100
        assert_that(
            calling(check_label_or_nickname_or_alias_is_uniq_and_correct).with_args(
                self.main_connection,
                's' * (default_limit + 1),
                self.org_id,
            ),
            raises(LoginLong)
        )

    def test_not_uniq(self):
        from intranet.yandex_directory.src.yandex_directory.common.exceptions import ImmediateReturn
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            False,
        )
        not_uniq = ['nickname', 'nickname1', 'group', 'group1', 'department', 'department1']
        for name in not_uniq:
            with self.assertRaises(ImmediateReturn):
                check_label_or_nickname_or_alias_is_uniq_and_correct(
                    self.main_connection,
                    name,
                    self.org_id
                )

    def test_yandex_team(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.is_yandex_team_org_id', return_value=True), \
             patch.object(PassportApiClient, 'validate_login') as mp:
            team_login = 'login@team-domainc.com'
            check_label_or_nickname_or_alias_is_uniq_and_correct(
                self.main_connection,
                team_login,
                self.organization['id'],
            )
            assert_not_called(mp)


class Test__build_order_by(TestCase):
    def test_order_by_should_return_order_by_string_with_asc(self):
        order_by = ['name', 'id']
        result = build_order_by(fields=order_by)
        exp_result = 'ORDER BY name ASC, id ASC'
        self.assertEqual(result, exp_result)

    def test_order_by_with_empty_fields_should_return_empty_string(self):
        for exp in [None, '', []]:
            result = build_order_by(fields=exp)
            self.assertEqual(result, '')


class Test_find_domid(TestCase):
    def setUp(self):
        self.latin_domain = 'DOMAIN.ru'
        self.latin_domain_domid = '1'
        self.cyrillic = 'ДОМЕН.рф'
        self.cyrillic_punycode = 'xn--d1acufc.xn--p1ai'
        self.cyrillic_domain_domid = '2'
        # интересующая нас часть ответа паспорта на ручку hosted_domains
        self.hosted_domain_response = {
            'hosted_domains': [{
                'domain': self.cyrillic_punycode,
                'domid': self.cyrillic_domain_domid,
            }, {
                'domain': self.latin_domain,
                'domid': self.latin_domain_domid,
            }]
        }

    def test_find_domid(self):
        # поиск домена латиницей
        assert_that(
            find_domid(self.hosted_domain_response, self.latin_domain),
            equal_to(self.latin_domain_domid)
        )

        # поиск домена кириллицей
        assert_that(
            find_domid(self.hosted_domain_response, self.cyrillic),
            equal_to(self.cyrillic_domain_domid)
        )

        # поиск домена в punycode
        assert_that(
            find_domid(self.hosted_domain_response, self.cyrillic_punycode),
            equal_to(self.cyrillic_domain_domid)
        )

    def test_no_domain(self):
        # домена нет в ответе
        assert_that(
            find_domid(self.hosted_domain_response, 'other.doman.com'),
            none()
        )


class Test_first_or_none(TestCase):
    def test_me(self):
        assert_that(
            first_or_none([1,2,3,4,5]),
            equal_to(1)
        )
        assert_that(
            first_or_none(set([1, 2, 3, 4, 5])),
            equal_to(1)
        )
        assert_that(
            first_or_none([]),
            equal_to(None)
        )
        assert_that(
            first_or_none(item for item in range(10)),
            equal_to(0)
        )

        assert_that(
            first_or_none(range(1, 10)),
            equal_to(1)
        )
        with self.assertRaises(ValueError):
            first_or_none(None)


class Test_get_boolean_param(TestCase):
    def test_no_required(self):
        # нет обязательного параметра

        assert_that(
            calling(get_boolean_param).with_args({}, 'required', True),
            raises(ImmediateReturn)
        )

    def test_valid_values(self):
        # значение параметров приводимых к boolean 'true' или 'false'

        assert_that(
            get_boolean_param({'bool': 'tRue'}, 'bool'),
            equal_to(True)
        )

        assert_that(
            get_boolean_param({'bool': 'FalsE'}, 'bool'),
            equal_to(False)
        )

    def test_invalid_values(self):
        # значение параметров приводимых к boolean не равны 'true' или 'false'

        assert_that(
            calling(get_boolean_param).with_args({'bool': 'some-value'}, 'bool'),
            raises(ImmediateReturn)
        )


from intranet.yandex_directory.src.yandex_directory.common import schemas

class TestInternationalizedString(TestCase):
    def test_i18n_string(self):
        # Если схема требует интернационализированную строку, то обычная строка
        # должна быть преобразована к интернационализированной
        result = make_internationalized_strings(
            'бар',
            schema=schemas.I18N_STRING
        )
        assert_that(
            result,
            has_entries(
                ru='бар',
            )
        )

    def test_i18n_string_or_none(self):
        # Если схема требует интернационализированную строку или None,
        # то обычная строка должна быть преобразована к
        # интернационализированной, а None оставлено как есть
        result = make_internationalized_strings(
            'бар',
            schema=schemas.I18N_STRING_OR_NULL
        )
        assert_that(
            result,
            has_entries(
                ru='бар',
            )
        )

        # Проверим, что None утилита не тронет
        result = make_internationalized_strings(
            None,
            schema=schemas.I18N_STRING_OR_NULL
        )
        assert_that(
            result,
            none()
        )

    def test_string_property(self):
        # Если property – строка, то она должна остаться как есть
        result = make_internationalized_strings(
            {
                'foo': 'бар',
            },
            schema={
                'type': 'object',
                'properties': {
                    'foo': schemas.STRING
                }
            }
        )
        # Так как схема требует обычную строку, то результат
        # должен остаться неизменным
        assert_that(
            result,
            has_entries(
                foo='бар'
            )
        )

    def test_string_or_null_property(self):
        # Если property – может быть строкой или нуллом,
        # то значение должно остаться как есть
        result = make_internationalized_strings(
            {
                'foo': None,
            },
            schema={
                'type': 'object',
                'properties': {
                    'foo': schemas.STRING_OR_NULL
                }
            }
        )
        # Так как схема требует обычную строку, то результат
        # должен остаться неизменным
        assert_that(
            result,
            has_entries(
                foo=None,
            )
        )

    def test_i18n_string_property(self):
        # Если property – должно быть интернационализированной строкой,
        # а на входе подана обычная, то она должна быть преобразована
        result = make_internationalized_strings(
            {
                'foo': 'бар'
            },
            schema={
                'type': 'object',
                'properties': {
                    'foo': schemas.I18N_STRING
                }
            }
        )
        assert_that(
            result,
            has_entries(
                foo=has_entries(
                    ru='бар',
                )
            )
        )

    def test_nested_i18n_string_property(self):
        # То же самое, что и предыдущий тест, но при
        # более глубоком уровне вложенности
        result = make_internationalized_strings(
            {
                'blah': {'foo': 'бар'}
            },
            schema={
                'type': 'object',
                'properties': {
                    'blah': {
                        'type': 'object',
                        'properties': {
                            'foo': schemas.I18N_STRING
                        }
                    }
                }
            }
        )
        assert_that(
            result,
            has_entries(
                blah=has_entries(
                    foo=has_entries(
                        ru='бар',
                    )
                )
            )
        )

    def test_objects_in_list(self):
        # Если property – должно быть интернационализированной строкой,
        # а на входе подана обычная, то она должна быть преобразована
        result = make_internationalized_strings(
            [
                {
                    'foo': 'блах'
                },
                {
                    'foo': 'минор'
                },
            ],
            schema={
                'type': 'array',
                'items': {
                    'type': 'object',
                    'properties': {
                        'foo': schemas.I18N_STRING
                    }
                }
            }
        )
        assert_that(
            result,
            contains(
                has_entries(
                    foo=has_entries(
                        ru='блах',
                    )
                ),
                has_entries(
                    foo=has_entries(
                        ru='минор',
                    )
                )
            )
        )

    def test_make_simple_strings(self):
        assert_that(
            make_simple_strings('фуу'),
            equal_to('фуу'),
        )

        assert_that(
            make_simple_strings({'ru': ''}),
            equal_to(''),
        )

        assert_that(
            make_simple_strings(
                {'ru': 'фуу'},
            ),
            equal_to('фуу'),
        )

        assert_that(
            make_simple_strings(
                {'en': 'foo'},
            ),
            equal_to('foo'),
        )

        assert_that(
            make_simple_strings(
                {'ru': 'фуу', 'en': 'foo'},
            ),
            equal_to('фуу'),
        )


        assert_that(
            make_simple_strings(
                {
                    'nickname': 'art',
                    'name': {'ru': 'Саша'},
                }
            ),
            has_entries(
                nickname='art',
                name='Саша',
            ),
        )

        # Теперь тот же объект, но внутри списка
        assert_that(
            make_simple_strings(
                [
                    {
                        'nickname': 'art',
                        'name': {'ru': 'Саша'},
                    }
                ]
            ),
            contains(
                has_entries(
                    nickname='art',
                    name='Саша',
                ),
            )
        )

        # А теперь проверим, что оно работает и для большей глубины вложенности
        assert_that(
            make_simple_strings(
                {
                    'response': [
                        {
                            'nickname': 'art',
                            'name': {'ru': 'Саша'},
                        }
                    ]
                }
            ),
            has_entries(
                response=contains(
                    has_entries(
                        nickname='art',
                        name='Саша',
                    ),
                ),
            )
        )


class Test__get_environment(TestCase):
    def test_get_environment(self):
        # проверим, что get_environment вовзращает в lowercase текущее окружение
        mocked_os_environ_get = Mock(return_value='tEsTing')
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.os.environ.get', mocked_os_environ_get):
            result = get_environment()

        assert_that(result, equal_to('testing'))

        mocked_os_environ_get = Mock(return_value=None)
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.os.environ.get', mocked_os_environ_get):
            result = get_environment()

        assert_that(result, equal_to(None))


class Test__stopit(TestCase):
    def test_stopit_should_return_empty_list_if_function_raised_timeout_error(self):
        # в случае таймаута функция должна вернуть default если raise_timeout=False
        timeout = 0.01

        def func():
            time.sleep(0.05)
            return True

        exc_result = []

        result = stopit(func, timeout=timeout, default=exc_result, raise_timeout=False)()
        assert_that(result, equal_to(exc_result))

    def test_stopit_should_raise_timeout_if_function_raised_timeout_error(self):
        # в случае таймаута функция должна кинуть исключение если raise_timeout=True
        timeout = 0.01

        def func():
            time.sleep(0.05)
            return True

        with self.assertRaises(TimeoutError):
            stopit(func, timeout=timeout)()

    def test_stopit_should_return_result_if_function_executed_successfully(self):
        # если таймаута не было, функция должна вернуть свой результат
        timeout = 0.01

        def func():
            return True

        result = stopit(func, timeout=timeout)()
        assert_that(result, equal_to(True))


class TestReplaceI18NStrings(TestCase):
    def test_replace_in_schema(self):
        # Проверим на одной их реальных схем, что функция
        # replace_i18n_strings_in_schema заменит все типа строк с
        # интернационализацией на обычные строки.
        # Это нам нужно для того, чтобы в доке для ручек начиная с 6
        # версии API показывать только обычные строки.
        from intranet.yandex_directory.src.yandex_directory.core.views.groups import GROUP_SCHEMA, STRING
        from intranet.yandex_directory.src.yandex_directory.common.schemas import replace_i18n_strings_in_schema

        the_copy = GROUP_SCHEMA
        result = replace_i18n_strings_in_schema(GROUP_SCHEMA)

        # проверим, что функция не меняет входные данные
        assert GROUP_SCHEMA == the_copy

        # удостоверимся, что в результате больше нет интернациональных строк
        assert_that(
            result['properties'],
            has_entries(
                name=STRING,
                description=STRING,
            )
        )


class TestTextConversion(TestCase):
    create_organization = False

    def test_force_utf8(self):
        assert_that(
            force_utf8('привет'),
            equal_to(b'\xd0\xbf\xd1\x80\xd0\xb8\xd0\xb2\xd0\xb5\xd1\x82'),
        )

    def test_force_text(self):
        assert_that(
            force_text('привет'),
            equal_to('привет'),
        )
        assert_that(
            force_text('привет'),
            equal_to('привет')
        )


class SomeTestObject(object):
    def __init__(self, name, age):
        self.name = name
        self.age = age


class TestMultikeysort(TestCase):
    def setUp(self):
        self.list_of_dicts = [
            {'name': 'Name02', 'age': 10},
            {'name': 'Name01', 'age': 30},
            {'name': 'Name02', 'age': 20},
        ]
        self.list_of_objects = [
            SomeTestObject('Name02', 10),
            SomeTestObject('Name01', 30),
            SomeTestObject('Name02', 20),
        ]

    def test_sort_dicts_name_asc_age_desc(self):
        expected = [
            self.list_of_dicts[1],
            self.list_of_dicts[2],
            self.list_of_dicts[0],
        ]
        assert_that(
            multikeysort(self.list_of_dicts, ['name', '-age']),
            equal_to(expected),
        )

    def test_sort_objects_name_asc_age_desc(self):
        expected = [
            self.list_of_objects[1],
            self.list_of_objects[2],
            self.list_of_objects[0],
        ]
        assert_that(
            multikeysort(self.list_of_objects, ['name', '-age'], getter=attrgetter),
            equal_to(expected),
        )

    def test_sort_dicts_name_desc_age_asc(self):
        expected = [
            self.list_of_dicts[0],
            self.list_of_dicts[2],
            self.list_of_dicts[1],
        ]
        assert_that(
            multikeysort(self.list_of_dicts, ['-name', 'age']),
            equal_to(expected),
        )

    def test_sort_objects_name_desc_age_asc(self):
        expected = [
            self.list_of_objects[0],
            self.list_of_objects[2],
            self.list_of_objects[1],
        ]
        assert_that(
            multikeysort(self.list_of_objects, ['-name', 'age'], getter=attrgetter),
            equal_to(expected),
        )

    def test_sort_dicts_name_asc_age_asc(self):
        expected = [
            self.list_of_dicts[1],
            self.list_of_dicts[0],
            self.list_of_dicts[2],
        ]
        assert_that(
            multikeysort(self.list_of_dicts, ['name', 'age']),
            equal_to(expected),
        )

    def test_sort_objects_name_desc_age_desc(self):
        expected = [
            self.list_of_objects[2],
            self.list_of_objects[0],
            self.list_of_objects[1],
        ]
        assert_that(
            multikeysort(self.list_of_objects, ['-name', '-age'], getter=attrgetter),
            equal_to(expected),
        )

    def test_sort_no_columns(self):
        expected = self.list_of_objects
        assert_that(
            multikeysort(self.list_of_objects, getter=attrgetter),
            equal_to(expected),
        )


class TestDisableDomain(TestCase):
    def setUp(self):
        super(TestDisableDomain, self).setUp()

        # обновим имя мастер домена, так как автоматически создается .ws.autotest.yandex.ru
        # и он считаестся техническим в автотестах
        self.organization_domain = 'test.ru'
        DomainModel(self.main_connection) \
            .filter(master=True, org_id=self.organization['id']) \
            .update(name=self.organization_domain)

        # В эту организацию мы будем передавать домен
        self.new_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='new-org'
        )['organization']
        self.set_feature_value_for_organization(DOMAIN_AUTO_HANDOVER, True, org_id=self.new_organization['id'])


    def get_org_domains(self, organization):
        return DomainModel(self.main_connection) \
            .filter(master=True, org_id=organization['id']) \
            .scalar('name')

    def test_disable_alias(self):
        # проверим случай, когда домен организации, который мы хотим открепить, является алиасом

        # добавим алиас
        alias = 'alias.com'
        DomainModel(self.main_connection).create(
            name=alias,
            org_id=self.organization['id'],
            owned=True,
            master=False,
        )

        now = utcnow()
        with patch.object(SendEmailToAllTask, 'place_into_the_queue') as place_into_the_queue, \
                patch.object(DomainModel, 'update') as mock_update_domain, \
                patch.object(DomainModel, 'delete_domain') as mock_delete_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.utcnow', return_value=now):

            disable_domain_in_organization(alias, self.new_organization['id'], 123)

            # сначала блокируем все домены, затем разблокируем
            assert_that(
                mock_update_domain.call_count,
                equal_to(2)
            )
            expected_call_args_list = [
                unittest.mock.call(
                    update_data={'blocked_at': now},
                    filter_data={
                        'org_id': self.organization['id'],
                        'owned': True
                    }
                ),
                unittest.mock.call(
                    update_data={'blocked_at': None},
                    filter_data={'org_id': self.organization['id']},
                    force=True
                )
            ]
            assert_that(
                mock_update_domain.call_args_list,
                contains(*expected_call_args_list)
            )

            mock_delete_domain.assert_called_once_with(
                alias,
                self.organization['id'],
                ANY,
                delete_blocked=True
            )

            # проверим, что отправляем письмо админу организации
            place_into_the_queue.assert_called_once_with(
                ANY,
                None,
                campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['DISABLE_DOMAIN_EMAIL'],
                org_id=self.organization['id'],
                domain=alias,
                new_master=None,
                tech=False,
            )

    def test_disable_master(self):
        # проверим случай, когда домен организации, который мы хотим открепить, является мастером
        # и в этой организации есть подтвержденные алиасы, тогда один из них должен быть сделан новым мастером

        # добавим организации подтвержденный алиас
        owned_alias = 'owned_alias.com'
        DomainModel(self.main_connection).create(
            name=owned_alias,
            org_id=self.organization['id'],
            owned=True,
            master=False,
        )

        now = utcnow()
        filter_data = {
            'org_id': self.organization['id'],
            'owned': True
        }
        with patch.object(SendEmailToAllTask, 'place_into_the_queue') as place_into_the_queue, \
                patch.object(DomainModel, 'update') as mock_update_domain, \
                patch.object(DomainModel, 'delete_domain') as mock_delete_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.utcnow', return_value=now):
            disable_domain_in_organization(owned_alias, self.new_organization['id'], 123)

            # сначала блокируем все домены, затем разблокируем
            assert_that(
                mock_update_domain.call_count,
                equal_to(2)
            )
            expected_call_args_list = [
                unittest.mock.call(
                    update_data={'blocked_at': now},
                    filter_data={
                        'org_id': self.organization['id'],
                        'owned': True
                    }
                ),
                unittest.mock.call(
                    update_data={'blocked_at': None},
                    filter_data={'org_id': self.organization['id']},
                    force=True
                )
            ]
            assert_that(
                mock_update_domain.call_args_list,
                contains(*expected_call_args_list)
            )

            mock_delete_domain.assert_called_once_with(
                owned_alias,
                self.organization['id'],
                ANY,
                delete_blocked=True
            )

            # проверим, что отправляем письмо админу организации
            place_into_the_queue.assert_called_once_with(
                ANY,
                None,
                campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['DISABLE_DOMAIN_EMAIL'],
                org_id=self.organization['id'],
                domain=owned_alias,
                new_master=None,
                tech=False,
            )

    def test_disable_portal_domain_prohibited(self):
        portal = create_organization(
            self.meta_connection,
            self.main_connection,
            label='portal'
        )['organization']
        OrganizationModel(self.main_connection).change_organization_type(portal['id'], 'portal')
        portal_domain = DomainModel(self.main_connection).get_master(portal['id'])
        with self.assertRaises(RuntimeError):
            disable_domain_in_organization(portal_domain['name'], self.new_organization['id'], 123)

    def test_dont_disable_domain_in_same_organization(self):
        # Убедимся, что домен в той же самой организации не будет оторван.
        # Функция должна отработать, но ничего не сделать.
        disable_domain_in_organization(self.organization_domain, self.organization['id'], 123)
        # Убедимся, что ничего не оторвалось
        domains = self.get_org_domains(self.organization)
        assert_that(
            domains,
            contains(self.organization_domain)
        )

    def test_disable_master_generate_tech(self):
        # проверим случай, когда домен организации, который мы хотим открепить, является мастером
        # и в этой организации нет подтвержденных алиасов, тогда новым мастером будет сгенерированный технический домен

        with patch.object(SendEmailToAllTask, 'place_into_the_queue') as place_into_the_queue, \
             patch.object(DomainModel, 'change_master_domain') as mock_change_master, \
             patch.object(DomainModel, 'delete_domain') as mock_delete_domain, \
             patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.create_domain_in_passport') as mock_create_domain_in_passport:

            self.clean_actions_and_events()

            disable_domain_in_organization(self.organization_domain, self.new_organization['id'], 123)

            # проверим, что добавился технический домен
            tech_domain_name = 'test-ru-{org_id}.ws.autotest.yandex.ru'.format(
                org_id=self.organization['id']
            )
            tech_domain = DomainModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'owned': True,
                    'name': tech_domain_name,
                }
            )
            assert tech_domain

            # проверим, что отправляем письмо админу организации
            place_into_the_queue.assert_called_once_with(
                ANY,
                None,
                campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['DISABLE_DOMAIN_EMAIL'],
                org_id=self.organization['id'],
                domain=self.organization_domain,
                new_master=tech_domain_name,
                tech=True,
            )
            # добаляем тех домен в паспорт
            mock_create_domain_in_passport.assert_called_once_with(
                ANY,
                org_id=self.organization['id'],
                punycode_name=tech_domain_name,
                admin_uid=ANY,
            )
            # меняемем мастер домен
            mock_change_master.assert_called_once_with(
                org_id=self.organization['id'],
                domain_name=tech_domain_name,
                force=True
            )

            # удаляем страый мастер
            mock_delete_domain.assert_called_once_with(
                self.organization_domain,
                self.organization['id'],
                ANY,
                delete_blocked=True,
            )

            # проверим, что отправляются нужные events
            events = [e['name'] for e in EventModel(self.main_connection).find()]
            assert_that(
                events,
                contains_inanyorder(
                    'domain_added',
                )
            )


class TestHideSensitiveKeys(TestCase):
    def test_no_password(self):
        # В данных, где нет поля password ничего не должно меняться
        data = {'status': 'ready', 'warnings': ['yamb_lost', 'head_maillist_lost']}
        assert(hide_sensitive_params(data) is False)
        assert(data == {'status': 'ready',
                        'warnings': ['yamb_lost', 'head_maillist_lost']
                        }
               )

    def test_password1(self):
        # В данных, где есть поле password, оно должно полностью закрываться, независимо от типа данных, который там лежит
        data = [{'passord': 'kek',
                 'password': [{'kek': 'newpassword'},
                              {"oldpassword": "mem",
                               "oldpasswordnew": 'kek',
                               'jin': [{"password": "kek",
                                        6: 1,
                                        "fds": 5
                                        }]
                               },
                               {}
                              ]
                 },
                 1
                 ]
        assert(hide_sensitive_params(data) is True)
        assert(data == [{'passord': 'kek', 'password': SENSITIVE_DATA_PLACEHOLDER}, 1])

    def test_password2(self):
        data = [{'lol': 'kek',
                 'password': [{'kek': 'newpassword'},
                              {"oldpassword": "mem",
                               "oldpasswordnew": 'kek',
                               'jin': [{"password": "kek",
                                        6: 1,
                                        "fds": 5}
                                       ]
                               },
                              {}
                              ]
                 },
                1
                ]
        assert(hide_sensitive_params(data) is True)
        assert(data == [{'lol': 'kek', 'password': SENSITIVE_DATA_PLACEHOLDER}, 1])

    def test_password3(self):
        data = [{'lol': 'kek',
                 'passwasdord': [{'kek': 'newpassword'},
                                 {"oldpassword": "mem",
                                  "oldpasswordnew": 'kek',
                                  'jin': [{"password": "kek",
                                           6: 1,
                                           "fds": 5
                                           }
                                          ]
                                  },
                                 {}
                                 ]
                 },
                1
                ]
        assert(hide_sensitive_params(data) is True)
        assert(data == [{'passwasdord': [{'kek': 'newpassword'}, {'jin': [{'fds': 5, 6: 1, 'password': SENSITIVE_DATA_PLACEHOLDER}], 'oldpassword': SENSITIVE_DATA_PLACEHOLDER, 'oldpasswordnew': SENSITIVE_DATA_PLACEHOLDER}, {}], 'lol': 'kek'}, 1])

    def test_invite_link(self):
        data = {
            'mail_args': {
                'ping': 'pong',
                'invite_link': 'test_link'
            }
        }
        assert(hide_sensitive_params(data) is True)
        assert(data == {'mail_args': {'ping': 'pong', 'invite_link': SENSITIVE_DATA_PLACEHOLDER}})


class TestfindSensitiveKeys(TestCase):
    def test_password(self):
        data = [{'password': 'kek', 'password': [{'kek': 'newpassword'}, {"oldpassword": "mem", "oldpasswordnew": 'kek','jin': [{"password": "kek", 6: 1, "fds": 5}]},{}]},1]
        assert(find_sensitive_params(data) is True)

    def test_no_password(self):
        data = {'status': 'ready', 'warnings': ['yamb_lost', 'head_maillist_lost']}
        assert(find_sensitive_params(data) is False)

    def test_hidden_password(self):
        data = [{'pasword': 'kek', 'password': SENSITIVE_DATA_PLACEHOLDER}, 1]
        assert(find_sensitive_params(data) is False)

    def test_invite_link(self):
        data = {'mail_args': {'invite_link': 'link', 'other_params': 1} }
        assert(find_sensitive_params(data) is True)


class Test_mask_email(TestCase):
    def test_email(self):
        test_cases = [
            ('admin@yandex.ru', 'a***n@yandex.ru'),
            ('admin@domain.com', 'a***n@domain.com'),
            ('lol@domain.com', 'l***l@domain.com'),
            ('ab@domain.com', '***@domain.com'),
            ('admin', 'a***n@yandex.ru'),
        ]
        for case in test_cases:
            assert mask_email(case[0]) == case[1], case

    def test_domain(self):
        test_cases = [
            ('admin@yandex.ru', 'a***n@y***.ru'),
            ('admin@domain', 'a***n@***'),
            ('admin@domain.yandex.ru', 'a***n@d***.ru'),
            ('admin@y.com', 'a***n@***.com'),
            ('admin@yt.com', 'a***n@y***.com'),
            ('admin@yt.yandex.com', 'a***n@y***.com'),
        ]
        for case in test_cases:
            assert mask_email(case[0], mask_domain=True) == case[1], case


class TestDomainNotFoundError(TestCase):
    def test_with_domain_parameter(self):
        e = DomainNotFound(domain='domain.ru')
        assert e.message == 'Domain {domain} not found'

    def test_with_org_id_parameter(self):
        e = MasterDomainNotFound(org_id=123)
        assert e.message == 'Domain for organization {org_id} not found'


class TestCreateDomainInPassportAndPdd(TestCase):

    def test_with_disabled_pdd_client_should_call_gendarme_and_fouras(self):
        tvm.tickets['gendarme'] = 'gendarme-tvm-ticket'
        domain_name = 'test.com'

        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.app.passport'), \
                patch('intranet.yandex_directory.src.yandex_directory.core.tasks.tasks.fouras.get_or_gen_domain_key') as get_or_gen_domain_key, \
                mocked_requests() as requests:

            requests.post.return_value.status_code = 200
            requests.post.return_value.json = lambda: {'status': 'ok', 'response': 'response_content'}

            create_domain_in_passport(
                main_connection=self.main_connection,
                org_id=self.organization['id'],
                punycode_name=domain_name,
                admin_uid=self.organization['admin_uid'],
            )
            self.process_tasks()
            request_url = requests.post.call_args_list[0][1]['url']
            request_data = json.loads(requests.post.call_args_list[0][1]['data'])

            assert_that(
                '/domain/recheck' in request_url,
            )

            assert_that(
                request_data['name'],
                equal_to(domain_name),
            )

            assert_that(
                request_data['sync'],
                equal_to(False),
            )

            assert_called_once(get_or_gen_domain_key, domain_name)

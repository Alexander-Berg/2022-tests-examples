# -*- coding: utf-8 -*-
import pytest

from django.test import TestCase
from django.conf import settings
from django.test.utils import override_settings
from django.db import models
from django.contrib.contenttypes.models import ContentType
from events.common_app.mixins import PublishedMixin
from events.surveyme.factories import SurveyFactory

from unittest.mock import patch, MagicMock

from events.accounts.models import User
from events.common_app import utils
from events.common_app.utils import (
    get_upload_to_translified_func_for_path,
    get_first_object,
    add_model_prefix,
    is_duplicate_entry_error,
    get_duplicate_entry_error_model,
    get_localhost_ip_address,
    get_user_ip_address,
    get_backoffice_url_for_obj,
    re_escape,
    class_localcache,
    get_tld,
)


class TestGetUploadToTranslifiedPath(TestCase):
    def setUp(self):
        self.upload_to = get_upload_to_translified_func_for_path('form/')

    def test_get_upload_to_translified_func(self):
        path_to_file = self.upload_to(None, 'файл')
        self.assertEqual(path_to_file, 'form/fajl')

        upload_to = get_upload_to_translified_func_for_path('form')
        path_to_file = upload_to(None, 'файл')
        self.assertEqual(path_to_file, 'form/fajl')

    def test_slugify_with_two_extensions(self):
        path_to_file = self.upload_to(None, 'файл с пробелами.tar.gz')
        self.assertEqual(path_to_file, 'form/fajl_s_probelami.tar.gz')


class BigCarManager(models.Manager):
    @get_first_object
    def get_first_car(self):
        return self.model.objects.order_by('ordering')


class BigCar(PublishedMixin, models.Model):
    name = models.CharField(max_length=100)
    ordering = models.PositiveIntegerField(default=1)

    objects = BigCarManager()

    class Meta:
        app_label = 'common_app'


class TestUtils(TestCase):
    def test_get_first_object_decorator_raises_exception(self):
        try:
            BigCar.objects.get_first_car()
            is_raised = False
        except BigCar.DoesNotExist:
            is_raised = True

        self.assertTrue(is_raised, 'т.к. объект не найден, то должно возбудиться исключение DoesNotExist')

    def test_get_first_object_decorator_returns_object(self):
        big_car = BigCar.objects.create(name='bmv')
        try:
            found_car = BigCar.objects.get_first_car()
            is_raised = False
        except BigCar.DoesNotExist:
            found_car = None
            is_raised = True

        self.assertFalse(is_raised, 'должен быть найден объект')
        self.assertEqual(found_car, big_car, 'найденный объект должен быть инстансом модели')

    def test_add_model_prefix(self):
        experiments = [
            {
                'prefix': None,
                'to': 'some_value',
                'expected': 'some_value',
                'msg': 'если не задан prefix, то должно вернуться чистое to'
            },
                {
                'prefix': 'prefix',
                'to': 'some_value',
                'expected': 'prefix__some_value',
                'msg': 'если задан prefix, то к префиксу должно конкатенироваться значение to, а между ними два подчеркивания'
            }
        ]

        for exp in experiments:
            response = add_model_prefix(exp['prefix'], exp['to'])
            self.assertEqual(response, exp['expected'], msg=exp['msg'])

    def test_clean_escaped_quotes(self):
        for i in range(10):
            amps = 'amp;' * i
            quot = '&{amps}quot;'.format(amps=amps)
            text = 'ООО {quot}Одуванчик{quot}'.format(quot=quot)
            response = utils.clean_escaped_quotes(text=text)
            expected = 'ООО "Одуванчик"'
            self.assertEqual(response, expected)


class Man_tests(models.Model):
    name = models.CharField(max_length=100)

    class Meta:
        app_label = 'common_app'

    def __str__(self):
        return self.name


CAR_PRICE_CHOICES = (
    ('low', 'Low text'),
    ('medium', 'Medium text'),
    ('high', 'High text'),
)


class Car_tests(models.Model):
    fans = models.ManyToManyField(Man_tests)
    price = models.CharField(max_length=10, choices=CAR_PRICE_CHOICES)

    class Meta:
        app_label = 'common_app'


class Test_is_duplicate_entry_error(TestCase):
    def test_without_args_attr(self):
        self.assertFalse(is_duplicate_entry_error(object))

    def test_with_empty_args(self):
        self.assertFalse(is_duplicate_entry_error(Exception()))

    def test_with_first_arg_not_equal_properly_mysql_error_code(self):
        self.assertFalse(is_duplicate_entry_error(Exception(1000)))

    def test_with_first_arg_equal_properly_mysql_error_code(self):
        self.assertTrue(is_duplicate_entry_error(Exception(1062)))


class Test_get_duplicate_entry_error_model(TestCase):
    def test_without_args_attr(self):
        self.assertIsNone(get_duplicate_entry_error_model(object))

    def test_with_empty_args(self):
        self.assertIsNone(get_duplicate_entry_error_model(Exception()))

    def test_with_one_arg(self):
        self.assertIsNone(get_duplicate_entry_error_model(Exception('hello')))

    def test_with_two_args__and_second_string_is_in_bad_format(self):
        self.assertIsNone(get_duplicate_entry_error_model(Exception('hello', 'world')))

    def test_with_string_is_in_good_format_and_app_has_underscore_in_name(self):
        exc = Exception('hello', "Duplicate entry '1-hello' for key 'common_app_bigcar_survey_id_7654a1549b1373ab_uniq'")
        response = get_duplicate_entry_error_model(exc)
        self.assertEqual(response, BigCar)

    def test_with_string_is_in_good_format_and_app_has_not_underscores_in_name(self):
        exc = Exception('hello', "Duplicate entry '1-hello' for key 'accounts_user_survey_id_7654a1549b1373ab_uniq'")
        response = get_duplicate_entry_error_model(exc)
        self.assertEqual(response, User)


class Test_update_by_format(TestCase):
    def test_basestring(self):
        self.assertEqual(
            '/forms_int/my.log',
            utils.update_by_format('/{app_type}/my.log', {'app_type': 'forms_int'}),
        )
        self.assertEqual(
            '/forms_ext/my.log',
            utils.update_by_format('/forms_ext/my.log', {'app_type': 'forms_int'}),
        )
        self.assertEqual(
            42,
            utils.update_by_format(42, {'app_type': 'forms_int'}),
        )
        self.assertIsNone(
            utils.update_by_format(None, {'app_type': 'forms_int'}),
        )

    def test_lists(self):
        self.assertEqual(
            ['/forms_int/my.log', '/forms_ext/my.log'],
            utils.update_by_format(['/{app_type}/my.log', '/forms_ext/my.log'],
                                   {'app_type': 'forms_int'}),
        )

    def test_dict(self):
        self.assertEqual(
            {'files': ['/forms_int/my.log', '/forms_ext/my.log']},
            utils.update_by_format({'files': ['/{app_type}/my.log', '/forms_ext/my.log']},
                                   {'app_type': 'forms_int'}),
        )


class TestLocalhostIPAddress(TestCase):
    @override_settings(IS_TEST=True)
    def test_unittest_case(self):
        self.assertIsNotNone(get_user_ip_address())

    @override_settings(IS_TEST=True)
    @patch('socket.getaddrinfo')
    def test_mock_unittest_case(self, getaddrinfo):
        get_user_ip_address()
        getaddrinfo.assert_not_called()

    @override_settings(IS_TEST=False)
    def test_ipv6_case(self):
        self.assertIsNotNone(get_user_ip_address())


class TestBackofficeUrl(TestCase):

    def setUp(self):
        self.survey = SurveyFactory()

    @override_settings(APP_TYPE='forms_biz', BACKOFFICE_DOMAIN="connect-test.ws.yandex.ru")
    def test_get_backoffice_url_for_obj_biz_success(self):
        fmt = f'https://connect-test.ws.yandex.ru/forms/admin/{self.survey.pk}/edit'
        self.assertEqual(get_backoffice_url_for_obj(self.survey), fmt)

    @override_settings(APP_TYPE='forms_biz', BACKOFFICE_DOMAIN="connect-test.ws.yandex.ru")
    def test_get_backoffice_url_for_obj_biz_with_action_success(self):
        fmt = f'https://connect-test.ws.yandex.ru/forms/admin/{self.survey.pk}/publish'
        self.assertEqual(get_backoffice_url_for_obj(self.survey, action_text=settings.MESSAGE_CLOSED_BY_TIME), fmt)

    def test_get_backoffice_url_for_obj(self):
        content_type = ContentType.objects.get_for_model(self.survey)
        fmt = f'https://admin-ext-forms.local.yandex-team.ru/#/redirect?contentType={content_type.pk}&objectId={self.survey.pk}'
        self.assertEqual(get_backoffice_url_for_obj(self.survey, action_text=settings.MESSAGE_CLOSED_BY_TIME), fmt)


@pytest.mark.parametrize('text,result', [
    ('', ''),
    ('London', 'London'),
    ('Москва', 'Москва'),
    ('^(){}[]\\.+*?$', '\\^\\(\\)\\{\\}\\[\\]\\\\\\.\\+\\*\\?\\$'),
    ('London(1)+Москва[2]', 'London\\(1\\)\\+Москва\\[2\\]'),
])
def test_re_escape(text, result):
    assert re_escape(text) == result


class TestClassLocalCache(TestCase):
    def test_local_cache(self):
        class Something:
            def get_value(self):
                pass

            @property
            @class_localcache
            def value(self):
                return self.get_value()

        something = Something()
        something.get_value = MagicMock(return_value=42)

        self.assertEqual(something.value, 42)
        self.assertEqual(something.value, 42)

        something.get_value.assert_called_once()


class TestUserIpAddress(TestCase):
    def test_should_return_user_ip_from_context(self):
        from ylog.context import put_to_context
        put_to_context('user_ip', '192.168.1.101')
        with patch('events.common_app.utils.get_localhost_ip_address') as mock_get_localhost_ip_address:
            user_ip = get_user_ip_address()
        self.assertEqual(user_ip, '192.168.1.101')
        mock_get_localhost_ip_address.assert_not_called()

    def test_should_return_user_ip_from_localhost(self):
        with patch('events.common_app.utils.get_context_ip_address') as mock_get_context_ip_address:
            mock_get_context_ip_address.return_value = None
            user_ip = get_user_ip_address()
        self.assertEqual(user_ip, get_localhost_ip_address())
        mock_get_context_ip_address.assert_called_once_with()


class TestRenderToPdf(TestCase):
    def test_should_render_pdf_file(self):
        text = (
            '# Chapter 1\n'
            'Some text\n'
            '# Глава 2\n'
            'Какой-то текст\n'
        )
        html_text = utils.render_markdown(text, with_new_line_break=True)
        pdf = utils.render_to_pdf(html_text)
        self.assertIsNotNone(pdf)
        pdf_data = pdf.getvalue()
        self.assertTrue(len(pdf_data) > 4)
        self.assertEqual(pdf_data[:4], b'%PDF')


class TestTld(TestCase):
    def test_should_return_valid_tld(self):
        self.assertEqual(get_tld('http://yastatic.ru/favicon.ico'), '.ru')
        self.assertEqual(get_tld('https://forms-test.awacs-b.yandex.net/v1/'), '.net')
        self.assertEqual(get_tld('https://forms.yandex.com/u/1234567/'), '.com')
        self.assertEqual(get_tld('http://kdunaev-dev.sas.yp-c.yandex.net:8080/v1/surveys/1234567/'), '.net')
        self.assertEqual(get_tld('https://forms.yandex.com.tr/survey/1234567/?iframe=1'), '.com.tr')
        self.assertEqual(get_tld('https://forms.yandex-team.ru/admin/1234567'), '.ru')

    def test_should_return_none(self):
        self.assertIsNone(get_tld('not a valid url'))
        self.assertIsNone(get_tld(''))
        self.assertIsNone(get_tld(None))

# -*- coding: utf-8 -*-
import base64
import hashlib
import urllib.parse

from django.test import TestCase
from django.utils.http import urlquote
from django.utils.encoding import force_str

from events.surveyme_integration.filters import (
    MD5Filter,
    Base64Filter,
    LowerFilter,
    UpperFilter,
    URLEncodeFilter,
    PunycodeFilter,
    LoginFilter,
    HeaderSanitizeFilter,
    JsonFilter,
)


class BaseFilterMixin:
    filter_class = None

    def setUp(self):
        self.experiments = [
            {
                'value': None,
                'expected': self.get_expected_value('')
            },
            {
                'value': {},
                'expected': self.get_expected_value('')
            },
            {
                'value': {'is': True},
                'expected': self.get_expected_value("{'is': True}")
            },
            {
                'value': 12345,
                'expected': self.get_expected_value('12345')
            },
            {
                'value': '12345',
                'expected': self.get_expected_value('12345')
            },
            {
                'value': 'ы',
                'expected': self.get_expected_value('ы')
            },
            {
                'value': 'ы',
                'expected': self.get_expected_value('ы')
            },
            {
                'value': 'https://www.привет.рф/',
                'expected': self.get_expected_value('https://www.привет.рф/')
            },
        ]

    def get_expected_value(self, value):
        raise NotImplementedError

    def test_me(self):
        for exp in self.experiments:
            self.assertEqual(self.filter_class().apply_filter(exp['value']), exp['expected'])


class TestMD5Filter(BaseFilterMixin, TestCase):
    filter_class = MD5Filter

    def get_expected_value(self, value):
        return hashlib.md5(force_str(value).encode('utf-8')).hexdigest()


class TestBase64Filter(BaseFilterMixin, TestCase):
    filter_class = Base64Filter

    def get_expected_value(self, value):
        return force_str(base64.b64encode(force_str(value).encode('utf-8')))


class TestLowerFilter(BaseFilterMixin, TestCase):
    filter_class = LowerFilter

    def get_expected_value(self, value):
        return force_str(value).lower()


class TestUpperFilter(BaseFilterMixin, TestCase):
    filter_class = UpperFilter

    def get_expected_value(self, value):
        return force_str(value).upper()


class TestURLEncodeFilter(BaseFilterMixin, TestCase):
    filter_class = URLEncodeFilter

    def get_expected_value(self, value):
        return urlquote(force_str(value))


class TestPunycodeFilter(BaseFilterMixin, TestCase):
    filter_class = PunycodeFilter

    def setUp(self, *args, **kwargs):
        super().setUp(*args, **kwargs)
        new_experiments = [
            {
                'value': 'https://www.привет.рф:8080/this-is-a-long-path?and_get_params=True',
                'expected': 'https://www.xn--b1agh1afp.xn--:8080-uye2a/this-is-a-long-path?and_get_params=True'
            },
            {
                'value': 'https://www.alliancefrançaise.nu',
                'expected': 'https://www.xn--alliancefranaise-npb.nu',
            },
        ]
        self.experiments += new_experiments

    def get_expected_value(self, value):
        parsed_url = urllib.parse.urlparse(value)
        if parsed_url.netloc:
            parsed_url = parsed_url._replace(netloc=force_str(force_str(parsed_url.netloc).encode('idna')))
            return force_str(parsed_url.geturl())
        return force_str(force_str(value).encode('idna'))


class TestLoginFilter(BaseFilterMixin, TestCase):
    filter_class = LoginFilter

    def setUp(self, *args, **kwargs):
        super().setUp(*args, **kwargs)
        self.experiments = [
            {
                'value': 'Трушкина (Рудакова) Александра (avrudakova)',
                'expected': 'avrudakova',
            },
            {
                'value': 'Трушкина (Рудакова) Александра (avrudakova), Дунаев Кирилл (kdunaev)',
                'expected': 'avrudakova, kdunaev',
            },
            {
                'value': 'Trushkina (Rudakova) Aleksandra (avrudakova), Dunaev Kirill',
                'expected': 'avrudakova, dunaev kirill',
            },
            {
                'value': 'Name Surname (login)',
                'expected': 'login',
            },
            {
                'value': '(login) Name Surname',
                'expected': '(login) name surname',
            },
            {
                'value': 'Name Surname',
                'expected': 'name surname',
            },
            {
                'value': '',
                'expected': '',
            },
        ]

    def get_expected_value(self, value):
        value = force_str(value)
        try:
            left_paren = value.rindex('(')
            right_paren = value.index(')', left_paren + 1)
            return value[left_paren + 1:right_paren]
        except ValueError:
            return value


class TestSanitizeFilter(BaseFilterMixin, TestCase):
    filter_class = HeaderSanitizeFilter

    def setUp(self, *args, **kwargs):
        self.experiments = [
            {
                'value': None,
                'expected': '',
            },
            {
                'value': {},
                'expected': '',
            },
            {
                'value': {'is': True},
                'expected': 'is True',
            },
            {
                'value': 12345,
                'expected': '12345',
            },
            {
                'value': '12345',
                'expected': '12345',
            },
            {
                'value': 'ы',
                'expected': 'ы',
            },
            {
                'value': 'ы',
                'expected': 'ы',
            },
            {
                'value': 'https://www.привет.рф/',
                'expected': 'https www.привет.рф',
            },
        ]


class TestJsonFilter(BaseFilterMixin, TestCase):
    filter_class = JsonFilter

    def setUp(self):
        self.experiments = [
            {
                'value': '',
                'expected': '',
            },
            {
                'value': '?',
                'expected': '?',
            },
            {
                'value': 'one line',
                'expected': 'one line',
            },
            {
                'value': 'multi\nline',
                'expected': 'multi\\nline',
            },
            {
                'value': 'symbols \n"\'\\',
                'expected': 'symbols \\n\\"\'\\\\',
            },
        ]

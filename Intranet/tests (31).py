from django import db
from django.db import connection
from django.test import TestCase
from django.http import HttpRequest, HttpResponse, QueryDict
from django.contrib.auth.models import User

from .decorators import (
    check_referer,
    token_auth,
    passport_or_token_auth,
)
from .models import Token
from .utils import cache


class TokenTest(TestCase):
    def setUp(self):
        self.token = Token(
            token='killa_gorilla',
            ips='192.193.194.195 |196.197.198.199',
            hostnames='killa.ru\r\ngorilla.ru\n godzilla.ru',
        )
        self.token.save()
        cache.clear()

    def test_ips_list(self):
        self.assertEqual(
            set(self.token.ips_list),
            {'192.193.194.195', '196.197.198.199'}
        )

    def test_hostnames_list(self):
        self.assertEqual(
            set(self.token.hostnames_list),
            {'killa.ru', 'gorilla.ru', 'godzilla.ru'}
        )

    def test_changing_token_and_refreshing_cache(self):
        cache.set('killa_gorilla', '192.193.194.196', False)
        cache.set('killa_gorilla', '192.193.194.195', True)
        self.token.ips = '192.193.194.196'
        self.token.save()
        self.assertEqual(
            cache.get('killa_gorilla', '192.193.194.196'), None
        )
        self.assertEqual(
            cache.get('killa_gorilla', '192.193.194.195'), None
        )


class TokenViewTest(TestCase):
    def setUp(self):
        self.req = HttpRequest()
        self.req.POST = QueryDict('')
        # очистка кэша от предыдущего запуска теста.
        cache.clear()

        @token_auth
        def view(request):
            return HttpResponse('ok')

        self.view = view

        self.token = Token(
            token='killa_gorilla',
            ips='192.193.194.195|196.197.198.199'
        )
        self.token.save()

    def test_empty_token(self):
        self.assertEqual(self.view(self.req).status_code, 403)

    def test_wrong_token(self):
        self.req.POST = QueryDict('token=oioioioi')
        self.req.META['REMOTE_ADDR'] = '192.193.194.195'
        self.assertEqual(self.view(self.req).status_code, 403)

    def test_wrong_ip(self):
        self.req.META['REMOTE_ADDR'] = '192.193.194.196'
        self.req.POST = QueryDict('token=killa_gorilla')
        self.assertEqual(self.view(self.req).status_code, 403)

    def test_access_allowed(self):
        self.req.META['REMOTE_ADDR'] = '192.193.194.195'
        self.req.POST = QueryDict('token=killa_gorilla')
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_ipv6_ipv4_mapped_ip_adress(self):
        self.req.META['REMOTE_ADDR'] = '::ffff:196.197.198.199'
        self.req.POST = QueryDict('token=killa_gorilla')
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_access_allowed_from_cache(self):
        db.reset_queries()
        self.view(self.req)
        self.assertEqual(connection.queries, [])

    def test_access_denied_from_cache(self):
        db.reset_queries()
        self.req.META['REMOTE_ADDR'] = '192.193.194.196'
        self.req.POST = QueryDict('token=killa_gorilla')
        self.assertEqual(self.view(self.req).status_code, 403)
        self.assertEqual(connection.queries, [])


class RefererViewTest(TestCase):
    def setUp(self):
        self.req = HttpRequest()
        self.req.POST = QueryDict('')

        @check_referer
        def view(request):
            return HttpResponse('ok')

        self.view = view

    def test_access_denied(self):
        self.assertEqual(self.view(self.req).status_code, 403)

    def test_access_allowed(self):
        self.req.META['HTTP_REFERER'] = 'http://killa.yandex-team.ru/oioioi'
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_https_refferer(self):
        self.req.META['HTTP_REFERER'] = (
            'https://calendar.yandex-team.ru/invite'
        )
        self.assertEqual(self.view(self.req).status_code, 200)


class AllChecksTest(TestCase):
    def setUp(self):
        self.req = HttpRequest()
        self.req.POST = QueryDict('')
        # очистка кэша от предыдущего запуска теста.
        cache.clear()

        class View(object):
            @passport_or_token_auth
            def __call__(self, request):
                return HttpResponse('ok')
        self.view = View()

        self.token = Token(
            token='killa_gorilla',
            ips='192.193.194.195'
        )
        self.token.save()

    def test_access_denied(self):
        self.assertEqual(self.view(self.req).status_code, 403)

    def test_access_allowed_by_token(self):
        self.req.META['REMOTE_ADDR'] = '192.193.194.195'
        self.req.POST = QueryDict('token=killa_gorilla')
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_access_allowed(self):
        self.req.META['HTTP_REFERER'] = 'http://killa.yandex-team.ru/oioioi'
        self.req.user = User(username='killa')
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_access_allowed_with_oauth(self):
        self.req.META['HTTP_AUTHORIZATION'] = (
            "OAuth aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        self.req.user = User(username='killa')
        self.assertEqual(self.view(self.req).status_code, 200)

    def test_access_denied_with_oauth(self):
        self.req.META['HTTP_AUTHORIZATION'] = (
            "OAuth aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        self.assertEqual(self.view(self.req).status_code, 403)

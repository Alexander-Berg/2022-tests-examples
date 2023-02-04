# -*- coding: utf-8 -*-

from testutils import TestCase, get_auth_headers


class TestSwagger(TestCase):
    def test_redirect(self):
        # проверяем что система версий api не ломает документацию

        response = self.client.get('/docs/')
        self.assertEqual(response.status_code, 302)

    def test_no_content_type(self):
        # Если не передан Content-Type: application/json, API должно игнорировать
        # тело запроса и считать, что на вход пришёл пустой словарь.
        # Если схема ручки содержит обязательные поля, то это должно приводить
        # к 422 ошибке.
        response = self.client.post(
            '/v6/users/',
            data='{"nickname": "vasily", "name": {"first": "Василий", "last": "Петров"}}',
            headers=get_auth_headers(),
        )
        self.assertEqual(response.status_code, 422)

    def test_content_type_charset(self):
        response = self.client.post(
            '/v6/users/',
            data='{"nickname": "vasily", "name": {"first": "Василий", "last": "Петров"}}',
            content_type='application/json; charset=UTF-8',
            headers=get_auth_headers(),
        )
        self.assertEqual(response.status_code, 422)

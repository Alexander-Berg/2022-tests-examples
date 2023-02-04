"""
Данный модуль проверяет корректность обработки запросов от IDM
"""

import json
import http

from django.urls import reverse
from django.conf import settings
from django.test import modify_settings


class TestIDM:
    def test_info(self, client):
        """
        Проверяем получение информациии структуре ролей в системе
        """

        info_uri = reverse('django_idm_api:info')
        response = client.get(info_uri)
        assert response.status_code == http.HTTPStatus.OK, response.content

        expected = {
            'code': 0,
            'roles': {
                'slug': 'role',
                'name': 'роль',
                'values': {
                    'superuser': 'суперпользователь',
                    'group-1': 'внут. контроль',
                    'group-2': 'представитель сервиса',
                },
            },
        }
        result = response.json()
        assert result == expected

    def test_add_role(self, client):
        """
        Проверяем корректность обработки присвоения роли
        """

        add_role_uri = reverse('django_idm_api:add-role')

        data = {
            'login': 'anybody',
            'role': json.dumps({'role': 'superuser'}),
        }

        # Даже если роль уже присвоена, ответ должен быть 200 OK
        for _ in range(2):
            response = client.post(add_role_uri, data)
            assert response.status_code == http.HTTPStatus.OK, response.content

            expected = {'code': 0}
            result = response.json()
            assert result == expected

    def test_remove_role(self, client):
        """
        Проверяем корректность обработки отвязывания роли
        """

        remove_role_uri = reverse('django_idm_api:remove-role')

        data = {
            'login': 'anybody',
            'role': json.dumps({'role': 'superuser'}),
            'path': '/role/superuser',
        }

        # Даже если роль уже отвязана, ответ должен быть 200 OK
        for _ in range(2):
            response = client.post(remove_role_uri, data)
            assert response.status_code == http.HTTPStatus.OK, response.content

            expected = {'code': 0}
            result = response.json()
            assert result == expected

    def test_get_all_roles(self, client):
        """
        Проверяем корректность вывода всех пользователей и их ролей
        """

        add_role_uri = reverse('django_idm_api:add-role')
        get_all_roles_uri = reverse('django_idm_api:get-all-roles')

        data = {
            'login': 'anybody',
            'role': json.dumps({'role': 'superuser'}),
        }

        response = client.post(add_role_uri, data)
        assert response.status_code == http.HTTPStatus.OK, response.content

        response = client.get(get_all_roles_uri)
        assert response.status_code == http.HTTPStatus.OK, response.content

        expected = {'code': 0, 'users': [{'login': 'anybody', 'roles': [{'role': 'superuser'}]}]}
        result = response.json()
        assert result == expected

    def test_tvm(self, client):
        """
        Проверяем, что ручки idm/ корректно закрываются TVM-авторизацией при включении
        """

        with modify_settings(MIDDLEWARE={'append': settings.IDM_API_TVM_MIDDLEWARE}):
            info_uri = reverse('django_idm_api:info')
            response = client.get(info_uri)
            assert response.status_code == http.HTTPStatus.FORBIDDEN, response

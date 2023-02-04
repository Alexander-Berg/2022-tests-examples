"""
Тест работы /api/v1/groups
"""
import json
import http

import pytest

from django.urls import reverse
from django.conf import settings
from django.contrib.auth.models import User

from rest_framework.status import (
    HTTP_200_OK,
)

from billing.dcsaap.backend.project.const import SU_GROUP_NAME


class TestAPIUserGroups:
    @pytest.fixture(autouse=True)
    def setup(self, db, api_client):
        self.setup_user(api_client, settings.YAUTH_TEST_USER)

    def setup_user(self, client, user_login):
        add_role_uri = reverse('django_idm_api:add-role')

        data = {
            'login': user_login,
            'role': json.dumps({'role': 'group-2'}),
        }

        response = client.post(add_role_uri, data)
        assert response.status_code == http.HTTPStatus.OK, response.content

    def test_user_groups(self, api_client):
        response = api_client.get(reverse('user_groups'))
        assert response.status_code == HTTP_200_OK
        expected = [{'id': 2, 'name': 'Представитель сервиса'}]
        assert list(response.data['groups']) == expected


class TestAPIUserGroupsUserNotExists:
    def test_user_groups(self, api_client):
        response = api_client.get(reverse('user_groups'))
        assert response.status_code == HTTP_200_OK
        assert list(response.data['groups']) == []


class TestAPIUserGroupsUserNotExists2:
    def test_user_groups(self, api_client):
        User.objects.filter(username=settings.YAUTH_TEST_USER).delete()

        response = api_client.get(reverse('user_groups'))
        assert response.status_code == HTTP_200_OK
        assert list(response.data['groups']) == []


class TestAPIUserGroupsSuperUser:
    @pytest.fixture(autouse=True)
    def setup(self, db, api_client):
        user_login = settings.YAUTH_TEST_USER
        add_role_uri = reverse('django_idm_api:add-role')
        data = {
            'login': user_login,
            'role': json.dumps({'role': 'superuser'}),
        }

        response = api_client.post(add_role_uri, data)
        assert response.status_code == http.HTTPStatus.OK, response.content

    def test_user_groups(self, api_client):
        response = api_client.get(reverse('user_groups'))
        assert response.status_code == HTTP_200_OK
        assert list(response.data['groups']) == [{"id": 0, "name": SU_GROUP_NAME}]


class TestAPIUserGroupsAllRoles:
    @pytest.fixture(autouse=True)
    def setup(self, db, api_client):
        user_login = settings.YAUTH_TEST_USER
        add_role_uri = reverse('django_idm_api:add-role')
        data = {'login': user_login, 'role': json.dumps({'role': 'superuser'})}
        response = api_client.post(add_role_uri, data)
        assert response.status_code == http.HTTPStatus.OK, response.content
        data = {'login': user_login, 'role': json.dumps({'role': 'group-2'})}
        response = api_client.post(add_role_uri, data)
        assert response.status_code == http.HTTPStatus.OK, response.content

    def test_user_groups(self, api_client):
        response = api_client.get(reverse('user_groups'))
        assert response.status_code == HTTP_200_OK
        expected = [
            {'id': 2, 'name': 'Представитель сервиса'},
            {"id": 0, "name": SU_GROUP_NAME},
        ]
        assert list(response.data['groups']) == expected

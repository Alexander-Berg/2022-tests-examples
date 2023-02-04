# coding: utf-8

import json
from contextlib import contextmanager
from datetime import timedelta
from typing import Sequence

import pytest
import responses
from django.conf import settings
from django.middleware.csrf import _get_new_csrf_token
from django.utils.timezone import now
from pkg_resources import resource_string
from rest_framework.test import APIClient


def assert_status(response, status_code):

    try:
        content = response.json()
    except (TypeError, ValueError):
        content = response.content

    if response.status_code != status_code:
        pytest.fail(
            f'Expected {status_code}, got {response.status_code}\n{content}'
        )


def prepare_user(client, *, username: str, roles: Sequence = (), csrf=False):

    if client.env == 'internal':

        client.cookies['Session_id'] = sid = f'|login:{username}|'
        client.cookies['sessionid2'] = sid

        from procu.api.models import User

        try:
            user = User.objects.get(username=username)
            user.set_roles(*roles)

        except User.DoesNotExist:
            pass

    elif client.env == 'external':
        client.login(username=username, password='123456')

    if csrf:
        client.cookies['csrftoken'] = _get_new_csrf_token()


def date_in_future(**kwargs):

    if not kwargs:
        kwargs['days'] = 7

    t = now()
    t += timedelta(**kwargs)

    return t


class Client(APIClient):
    env = None

    def _parse_json(self, response, **extra):

        ct = response.get('Content-Type')
        if 'application/json' not in ct:
            raise ValueError(
                f'Content-Type header is "{ct}" not "application/json"'
            )

        extra['encoding'] = 'utf-8'
        return json.loads(response.content.decode('utf-8'), **extra)


class ClientInternal(APIClient):
    env = 'internal'

    def request(self, **kwargs):
        return super().request(**dict(**kwargs, **{'SERVER_NAME': 'procu.int'}))


class ClientExternal(APIClient):
    env = 'external'

    def request(self, **kwargs):
        return super().request(**dict(**kwargs, **{'SERVER_NAME': 'procu.ext'}))


def _fixture(name):
    return resource_string('procu', f'fixtures/tracker/{name}.json')


@contextmanager
def mock_get_issue(key):
    with responses.RequestsMock() as rsps:

        mocks = [
            ('fields/', 'fields'),
            ('statuses/1', 'status_open'),
            ('users/1120000000048460', 'user_robot_procu'),
            ('priorities/2', 'priority_normal'),
            (f'issues/{key}', 'single_issue'),
        ]

        for path, fx in mocks:
            rsps.add(
                rsps.GET,
                f'{settings.STARTREK_API_URL}/v2/{path}',
                body=_fixture(fx),
                status=200,
                content_type='application/json',
            )

        yield


@contextmanager
def mock_find_tickets():
    with responses.RequestsMock() as rsps:

        mocks = [
            (rsps.GET, 'fields/', 'fields'),
            (rsps.GET, 'statuses/1', 'status_open'),
            (rsps.GET, 'priorities/2', 'priority_normal'),
            (rsps.POST, 'issues/_search', 'found_issues'),
        ]

        for method, path, fx in mocks:
            rsps.add(
                method,
                f'{settings.STARTREK_API_URL}/v2/{path}',
                body=_fixture(fx),
                status=200,
                content_type='application/json',
            )

        yield


@contextmanager
def mock_phone(username, number=None):

    content = []

    if number is not None:
        content = [{'number': number}]

    with responses.RequestsMock() as rsps:

        mock = {
            'links': {},
            'page': 1,
            'limit': 50,
            'result': [{'phones': content}],
            'total': 1,
            'pages': 1,
        }

        rsps.add(
            rsps.GET,
            f'https://staff-api.test.yandex-team.ru/v3/persons'
            f'?login={username}&_fields=phones.number',
            body=json.dumps(mock),
            status=200,
            content_type='application/json',
        )

        yield


@contextmanager
def mock_wf(html, cfg='intranet'):

    with responses.RequestsMock() as rsps:
        rsps.add(
            rsps.POST,
            f'https://wf.yandex-team.ru/v5/html?cfg={cfg}',
            body=html,
            status=200,
            content_type='text/html',
        )

        yield


def parametrize(params, *fields):
    argnames = ','.join(fields)
    argvalues = []

    if len(fields) == 1:
        for data in params:
            argvalues.append(data[fields[0]])

    else:
        for data in params:
            argvalues.append([data[f] for f in fields])

    return pytest.mark.parametrize(argnames, argvalues)


def get_file_content(file_id):
    return f'FILE-CONTENT-{file_id}'.encode()

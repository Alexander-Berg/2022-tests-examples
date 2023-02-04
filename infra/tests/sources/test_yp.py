import pytest
import mock
import logging

import requests

from infra.cauth.server.master.importers.servers.sources import YpGroups, GroupResult
from __tests__.utils import Response

pytestmark = pytest.mark.django_db

hosts_answer = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<domain name="yp-sas">
<group name="test-vmproxy-allocated">
<host name="test-vmproxy-allocated.sas.yp-c.yandex.net">
<type>server</type>
</host>
<host name="test-vmproxy-allocated-new.sas.yp-c.yandex.net"/>
</group>
<group name="test-vmproxy-allocated-2">
<host name="test-vmproxy-allocated-2.sas.yp-c.yandex.net">
<type>dev-vm</type>
</host>
</group>
</domain>'''

owners_answer = '''test-vmproxy-allocated-2	frolstas,max7255,alonger,romanovich
test-vmproxy-allocated	frolstas,max7255,alonger,romanovich
unknown-group uruz,lavrukov'''


def mock_answers(method, url, *args, **kwargs):
    if 'sas' not in url:
        raise requests.HTTPError('Invalid status code')

    if 'owners' in url:
        response = Response(owners_answer)
    elif 'domain' in url:
        response = Response(hosts_answer)
    else:
        raise requests.HTTPError('Invalid status code')

    return response


def test_import():
    expected = {
        'yp.test-vmproxy-allocated': GroupResult(
            source='yp',
            name='yp.test-vmproxy-allocated',
            responsibles={'alonger', 'frolstas', 'max7255', 'romanovich'},
            hosts=[
                GroupResult.Host(hostname='test-vmproxy-allocated.sas.yp-c.yandex.net'),
                GroupResult.Host(hostname='test-vmproxy-allocated-new.sas.yp-c.yandex.net'),
            ],
        ),
        'yp.test-vmproxy-allocated-2': GroupResult(
            source='yp',
            name='yp.test-vmproxy-allocated-2',
            responsibles={'alonger', 'frolstas', 'max7255', 'romanovich'},
            hosts=[
                GroupResult.Host(hostname='test-vmproxy-allocated-2.sas.yp-c.yandex.net', type='dev-server'),
            ],
        ),
    }
    groups = YpGroups(logger=logging.getLogger('testlogger'))
    with mock.patch('requests.sessions.Session.request') as mocked:
        mocked.side_effect = mock_answers
        groups.fetch()
        assert groups._groups_map == expected

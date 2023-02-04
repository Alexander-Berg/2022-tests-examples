import pytest
import mock
import logging

from infra.cauth.server.master.importers.servers.sources import GolemServers, ServerResult
from __tests__.utils import Response

pytestmark = pytest.mark.django_db

golem_answer = b'''hello.yandex.net	regular,host
vm-e2e-head1-network-1521137421-trk-1.pre-df.cloud.yandex.net	cloud,host
sas1-79818febe86d.qloud-c.yandex.net	funny,qloud,host
vs_golem-group  monitoring,group
    some,empty,name
host.yandex.net_1   strange,host,clone
bye.yandex.net	another,regular,host'''


def mock_golem_answer(method, url, *args, **kwargs):
    return Response(golem_answer)


def test_golem_import():
    expected = [
        ServerResult(
            source='golem',
            name='hello.yandex.net',
            type='server',
            responsibles={'host', 'regular'},
        ),
        ServerResult(
            source='golem',
            name='vm-e2e-head1-network-1521137421-trk-1.pre-df.cloud.yandex.net',
            type='server',
            responsibles={'cloud', 'host'},
        ),
        ServerResult(
            source='golem',
            name='bye.yandex.net',
            type='server',
            responsibles={'another', 'host', 'regular'},
        ),
    ]

    servers = GolemServers(logger=logging.getLogger('testlogger'))
    with mock.patch('requests.sessions.Session.request') as mocked:
        mocked.side_effect = mock_golem_answer

        assert servers.fetch() == expected

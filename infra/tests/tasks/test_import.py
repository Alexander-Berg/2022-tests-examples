import itertools
import json
from mock import mock
import pytest
import requests
from waffle.testutils import override_switch

from django.utils import timezone
from django.test import override_settings

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import IDM_STATUS
from infra.cauth.server.common.models import ServerGroup, Server, Source, User, ServerResponsible
from infra.cauth.server.master.api.models import DNS_STATUS
from infra.cauth.server.master.importers.management.commands.poke_hanging_hosts import (
    Command as PokeHostsCommand
)
from infra.cauth.server.master.importers.servers.sources import (
    GolemServers, ConductorGroups, GencfgGroups, WalleProjects, BotAbcGroups, HdAbcGroups
)
from infra.cauth.server.master.importers.tasks import run_import
from __tests__.utils import Response, create_user
from infra.cauth.server.master.utils.subtasks import SubtaskPool

pytestmark = pytest.mark.django_db


old_hosts_answer = b''

golem_hosts_answer = b'''
host1.yandex.net\tuser1,user2
host2.yandex.net\tuser3
'''

CONDUCTOR_AND_BOT_FQDN = 'bot_conductor_fqdn.yandex.next'
conductor_hosts_answer = (
    'conductor-group:host3.sas.yp-c.yandex.net,conductor-host.yandex.net,server1.yandex.net,qloud-host.qloud-c.yandex.net,{fqdn}:::\n'  # noqa
    'qloud-hosts-group-only:qloud-host2.qloud-c.yandex.net,qloud-host3.qloud-c.yandex.net:::'
    .format(fqdn=CONDUCTOR_AND_BOT_FQDN)
).encode()
conductor_responsibles_answer = json.dumps([
    {
        'name': 'conductor-group',
        'admins': ['user1', 'user2'],
    },
]).encode()

gencfg_hosts_answer = b'''<?xml version="1.0" encoding="UTF-8"?>
<domain id="10" name="search">
</domain>'''

gencfs_owners_answer = b''

yp_hosts_answer = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<domain name="yp-sas">
<group name="yp-1">
<host name="host1.sas.yp-c.yandex.net">
<type>server</type>
</host>
<host name="host2.sas.yp-c.yandex.net"/>
</group>
<group name="yp-2">
<host name="host3.sas.yp-c.yandex.net">
<type>dev-vm</type>
</host>
</group>
</domain>'''

yp_owners_answer = b'''yp-1\tuser4,user5
yp-2\tuser4,user6
unknown-group\tuser5,user6
'''

walle_answer = b'''walle-project1:host1.yandex.net,walle-specific-host1.yandex.net
walle-project2:host2.yandex.net,walle-specific-host2.yandex.net
walle-project3:
'''

walle_responsibles_answer = json.dumps(
    {
        "walle-project1": ["walle-user1", "walle-user2"],
        "walle-project2": ["walle-user2"]
    })

walle_settings_answer = json.dumps(
    {
        "walle-project1": {},
        "walle-project2": {
            "flow": "backend_sources",
            "trusted_sources": "walle,conductor",
            "key_sources": "staff,secure,insecure",
            "krl_url": "https://skotty/krl/all.zst",
            "insecure_ca_list_url": "https://skotty/pub/insecure",
            "secure_ca_list_url": "https://skotty/pub/secure",
            "sudo_ca_list_url": "https://skotty/pub/sudo",
        },
    })


BOT_ABC_ID = 1
HD_ABC_ID = 2
BOT_FQDN = 'bot_fqdn.yandex.next'
HD_FQDN = 'hd_fqdn.yandex.next'
BOT_RESP = 'bot_resp'
HD_RESP = 'hd_resp'
HD_ABC_SLUG = 'hd'
HD_IDS = ['hd_id']
BOT_ABC_SLUG = 'abc_slug'
BOT_ANSWER = (
    '362600	{fqdn1}	OPERATION	RU	IVA	IVNIT	IVA-3	60	45	-	0025906C0036	0025906C0037			90E2BAEABF06	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC	DP/SM/SYS6017RNTF/4T3.5/1U/1P	XEONE5-2660	SERVERS	SRV	X9DRW-IF	0025906FF783	{abc_id}	\n'  # noqa
    '362600	{fqdn2}	OPERATION	RU	IVA	IVNIT	IVA-3	60	45	-	0025906C0036	0025906C0037			90E2BAEABF06	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC	DP/SM/SYS6017RNTF/4T3.5/1U/1P	XEONE5-2660	SERVERS	SRV	X9DRW-IF	0025906FF783	{abc_id}	'  # noqa
    .format(fqdn1=BOT_FQDN, fqdn2=CONDUCTOR_AND_BOT_FQDN, abc_id=BOT_ABC_ID).encode()
)
ABC_ANSWER = [
    {
        "person": {
            "login": BOT_RESP,
        },
        "service": {
            "id": BOT_ABC_ID,
            "slug": BOT_ABC_SLUG,
        },
        "role": {
            "id": BotAbcGroups.HARDWARE_MANAGER_ID,
        }
    }
]

ABC_ANSWER_HD = [
    {
        "person": {
            "login": HD_RESP,
        },
        "service": {
            "id": HD_ABC_ID,
            "slug": HD_ABC_SLUG,
        },
        "role": {
            "id": HdAbcGroups.HARDWARE_MANAGER_ID,
        },
        "resource": {
            "external_id": HD_IDS[0]
        }
    }]

HD_ANSWER = """[
    {{
      \"fqdn\" : \"{first_fqdn}\",
      \"login\":  \"{first_login}\"
    }}]""".format(first_fqdn=HD_FQDN, first_login=HD_IDS[0])


CONS_ANSWER = [
    {
        'resource': {'external_id': HD_IDS[0]},
        'service': {'id': HD_ABC_ID}
    }]


def mock_answers(url, *args, **kwargs):
    if '.cloud.yandex-team.ru' in url:
        response = Response(old_hosts_answer)
    elif url == GolemServers.URL:
        response = Response(golem_hosts_answer)
    elif url == ConductorGroups.HOSTS_URL:
        response = Response(conductor_hosts_answer)
    elif url == ConductorGroups.RESPONSIBLES_URL:
        response = Response(conductor_responsibles_answer)
    elif url.startswith(GencfgGroups.SANDBOX_PROXY_URL + 'GENCFG_CAUTH_EXPORT_HOSTS'):
        response = Response(gencfg_hosts_answer)
    elif url.startswith(GencfgGroups.SANDBOX_PROXY_URL + 'GENCFG_CAUTH_EXPORT_OWNERS'):
        response = Response(gencfs_owners_answer)
    elif url.startswith('https://yp-cauth') and 'owners' in url:
        if 'sas' not in url:
            raise requests.HTTPError('Invalid status code')
        response = Response(yp_owners_answer)
    elif url.startswith('https://yp-cauth') and 'domain' in url:
        if 'sas' not in url:
            raise requests.HTTPError('Invalid status code')
        response = Response(yp_hosts_answer)
    elif url == WalleProjects.SOURCE_URL:
        response = Response(walle_answer)
    elif url == WalleProjects.RESPONSIBLES_URL:
        response = Response(walle_responsibles_answer)
    elif url == WalleProjects.SETTINGS_URL:
        response = Response(walle_settings_answer)
    else:
        raise requests.HTTPError('INVALID TEST URL')

    return response


def empty_answers(url, *args, **kwargs):
    empty_text_answer = b''
    empty_xml_yp_answer = b'<?xml version="1.0" encoding="UTF-8"?><domain name="yp-sas"></domain>'
    empty_json_answer = '[]'
    empty_dict_answer = '{}'

    if '.cloud.yandex-team.ru' in url:
        response = Response(empty_text_answer)
    elif url == GolemServers.URL:
        response = Response(empty_text_answer)
    elif url == ConductorGroups.HOSTS_URL:
        response = Response(empty_json_answer)
    elif url == ConductorGroups.RESPONSIBLES_URL:
        response = Response(empty_json_answer)
    elif url.startswith(GencfgGroups.SANDBOX_PROXY_URL + 'GENCFG_CAUTH_EXPORT_HOSTS'):
        response = Response(gencfg_hosts_answer)
    elif url.startswith(GencfgGroups.SANDBOX_PROXY_URL + 'GENCFG_CAUTH_EXPORT_OWNERS'):
        response = Response(gencfs_owners_answer)
    elif url.startswith('https://yp-cauth') and 'owners' in url:
        response = Response(empty_xml_yp_answer)
    elif url.startswith('https://yp-cauth') and 'domain' in url:
        response = Response(empty_xml_yp_answer)
    elif url == WalleProjects.SOURCE_URL:
        response = Response(empty_text_answer)
    elif url == WalleProjects.RESPONSIBLES_URL:
        response = Response(empty_dict_answer)
    elif url == WalleProjects.SETTINGS_URL:
        response = Response(empty_dict_answer)
    else:
        raise requests.HTTPError('INVALID TEST URL')

    return response


class MockedPool(SubtaskPool):
    def _update_subtask(self, *args, **kwargs):
        pass

    def join(self):
        for task in self.tasks:
            task.task.apply(task.args, task.kwargs)


@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_import_servers(mocked, client, mock_bot_https, mock_hd_https):
    for obj in itertools.chain(Server.query.all(), ServerGroup.query.all(), User.query.all(), Source.query.all()):
        Session.delete(obj)
    source = Source(
        name='default',
        is_default=True,
        last_update=timezone.now(),
    )
    Session.add(source)
    for i in range(1, 7):
        username = 'user{}'.format(i)
        create_user(i, username, username)
    create_user(10, BOT_RESP, BOT_RESP)
    for i in range(1, 3):
        username = 'walle-user{}'.format(i)
        create_user(i+100, username, username)
    for source_name in ['golem', 'conductor', 'cms', 'yp', 'ycloud', 'walle', 'bot', 'hd']:
        source = Source.query.filter_by(name=source_name).first()
        if source is None:
            source = Source(name=source_name, is_default=False)
        if source_name == 'walle':
            source.is_modern = True
        Session.add(source)

    assert Server.query.count() == 0
    assert ServerGroup.query.count() == 0

    ycloud_source = Source.query.filter_by(name='ycloud').first()
    yc_server1 = Server(
        fqdn='server1.yandex.net',
        type='server',
        client_version='cauth/100',
        idm_status=IDM_STATUS.ACTUAL,
    )
    yc_server2 = Server(
        fqdn='server2.yandex.net',
        type='server',
        client_version='cauth/100',
        idm_status=IDM_STATUS.ACTUAL,
    )
    Session.add(yc_server1)
    Session.add(yc_server2)
    yc_server1.sources = [ycloud_source]
    yc_server2.sources = [ycloud_source]

    mock_bot_https(BOT_ANSWER, ABC_ANSWER)
    mock_hd_https(HD_ANSWER, ABC_ANSWER_HD, CONS_ANSWER)
    with mock.patch('infra.cauth.server.master.api.tasks.push_idm_update'), \
        mock.patch('infra.cauth.server.master.api.tasks.push_idm_object'), \
        mock.patch('requests.sessions.Session.get') as session_get, \
        mock.patch('infra.cauth.server.master.importers.base.SubtaskPool', new=MockedPool), \
            mock.patch('infra.cauth.server.master.utils.tasks.SubtaskPool', new=MockedPool):
        session_get.side_effect = mock_answers
        run_import.apply(args=['servers'])

    def new_hosts():
        return {
            host.fqdn: {
                'hostname': host.fqdn,
                'type': host.type,
                'idm_status': host.idm_status,
                'sources': sorted([
                    src.name for src in host.sources
                ]),
                'is_baremetal': host.is_baremetal,
            } for host in Server.query.all()
        }

    expected_hosts = {
        BOT_FQDN: {
            'hostname': BOT_FQDN, 'type': 'server',
            'is_baremetal': True,
            'idm_status': 'dirty',
            'sources': ['bot'],
        },
        CONDUCTOR_AND_BOT_FQDN: {
            'hostname': CONDUCTOR_AND_BOT_FQDN, 'type': 'server',
            'is_baremetal': True,
            'idm_status': 'dirty',
            'sources': ['bot', 'conductor'],
        },
        'host1.yandex.net': {
            'hostname': 'host1.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['golem', 'walle'],
        },
        'host2.yandex.net': {
            'hostname': 'host2.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['golem', 'walle'],
        },
        'hd_fqdn.yandex.next': {
            'hostname': 'hd_fqdn.yandex.next',
            'idm_status': 'dirty',
            'is_baremetal': False,
            'sources': ['hd'],
            'type': 'server'
        },
        'host1.sas.yp-c.yandex.net': {
            'hostname': 'host1.sas.yp-c.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['yp'],
        },
        'host2.sas.yp-c.yandex.net': {
            'hostname': 'host2.sas.yp-c.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['yp'],
        },
        'host3.sas.yp-c.yandex.net': {
            'hostname': 'host3.sas.yp-c.yandex.net', 'type': 'dev-server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['conductor', 'yp'],
        },
        'conductor-host.yandex.net': {
            'hostname': 'conductor-host.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['conductor'],
        },
        'walle-specific-host1.yandex.net': {
            'hostname': 'walle-specific-host1.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['walle'],
        },
        'walle-specific-host2.yandex.net': {
            'hostname': 'walle-specific-host2.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['walle'],
        },
        'server1.yandex.net': {
            'hostname': 'server1.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'dirty',
            'sources': ['conductor', 'ycloud'],
        },
        'server2.yandex.net': {
            'hostname': 'server2.yandex.net', 'type': 'server',
            'is_baremetal': False,
            'idm_status': 'actual',  # сервер не был затронут в ходе синка
            'sources': ['ycloud'],
        }
    }
    assert new_hosts() == expected_hosts

    def new_server_resps():
        return {
            (resp.server.fqdn, resp.user.login, resp.source.name)
            for resp in ServerResponsible.query.all()
        }

    expected_server_resps = {
        ('bot_conductor_fqdn.yandex.next', 'bot_resp', 'bot'),
        ('bot_conductor_fqdn.yandex.next', 'user1', 'conductor'),
        ('bot_conductor_fqdn.yandex.next', 'user2', 'conductor'),
        ('bot_fqdn.yandex.next', 'bot_resp', 'bot'),
        ('conductor-host.yandex.net', 'user1', 'conductor'),
        ('conductor-host.yandex.net', 'user2', 'conductor'),
        ('host1.sas.yp-c.yandex.net', 'user4', 'yp'),
        ('host1.sas.yp-c.yandex.net', 'user5', 'yp'),
        ('host1.yandex.net', 'user1', 'golem'),
        ('host1.yandex.net', 'user2', 'golem'),
        ('host1.yandex.net', 'walle-user1', 'walle'),
        ('host1.yandex.net', 'walle-user2', 'walle'),
        ('host2.sas.yp-c.yandex.net', 'user4', 'yp'),
        ('host2.sas.yp-c.yandex.net', 'user5', 'yp'),
        ('host2.yandex.net', 'user3', 'golem'),
        ('host2.yandex.net', 'walle-user2', 'walle'),
        ('host3.sas.yp-c.yandex.net', 'user1', 'conductor'),
        ('host3.sas.yp-c.yandex.net', 'user2', 'conductor'),
        ('host3.sas.yp-c.yandex.net', 'user4', 'yp'),
        ('host3.sas.yp-c.yandex.net', 'user6', 'yp'),
        ('server1.yandex.net', 'user1', 'conductor'),
        ('server1.yandex.net', 'user2', 'conductor'),
        ('walle-specific-host1.yandex.net', 'walle-user1', 'walle'),
        ('walle-specific-host1.yandex.net', 'walle-user2', 'walle'),
        ('walle-specific-host2.yandex.net', 'walle-user2', 'walle'),
    }
    assert new_server_resps() == expected_server_resps

    def new_groups():
        return {
            group.name: {
                'idm_status': group.idm_status,
                'responsibles': {user.login for user in group.responsible_users},
                'hosts': {host.fqdn for host in group.servers},
                'flow': group.flow,
                'trusted_sources': {s.name for s in group.trusted_sources},
                'key_sources': set(group.key_sources.split(',')) if group.key_sources else set(),
                'secure_ca_list_url': group.secure_ca_list_url,
                'insecure_ca_list_url': group.insecure_ca_list_url,
                'krl_url': group.krl_url,
                'sudo_ca_list_url': group.sudo_ca_list_url,
            }
            for group in ServerGroup.query.all()
        }

    expected_groups = {
        'bot.' + BOT_ABC_SLUG: {
            'idm_status': 'dirty',
            'responsibles': {BOT_RESP},
            'hosts': {BOT_FQDN, CONDUCTOR_AND_BOT_FQDN},
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'yp.yp-1': {
            'idm_status': 'dirty',
            'responsibles': {'user4', 'user5'},
            'hosts': {'host1.sas.yp-c.yandex.net', 'host2.sas.yp-c.yandex.net'},
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'yp.yp-2': {
            'idm_status': 'dirty',
            'responsibles': {'user4', 'user6'},
            'hosts': {'host3.sas.yp-c.yandex.net'},
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'conductor.conductor-group': {
            'idm_status': 'dirty',
            'responsibles': {'user1', 'user2'},
            'hosts': {
                'host3.sas.yp-c.yandex.net',
                'conductor-host.yandex.net',
                'server1.yandex.net',
                CONDUCTOR_AND_BOT_FQDN,
            },
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'conductor.qloud-hosts-group-only': {
            'idm_status': 'dirty',
            'hosts': set(),
            'responsibles': set(),
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'walle.walle-project1': {
            'idm_status': 'dirty',
            'responsibles': {"walle-user1", "walle-user2"},
            'hosts': {'host1.yandex.net', 'walle-specific-host1.yandex.net'},
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'walle.walle-project2': {
            'idm_status': 'dirty',
            'responsibles': {"walle-user2"},
            'hosts': {'host2.yandex.net', 'walle-specific-host2.yandex.net'},
            'flow': 'backend_sources',
            'trusted_sources': {'walle', 'conductor'},
            'key_sources': {'staff', 'secure', 'insecure'},
            'krl_url': 'https://skotty/krl/all.zst',
            'insecure_ca_list_url': 'https://skotty/pub/insecure',
            'secure_ca_list_url': 'https://skotty/pub/secure',
            'sudo_ca_list_url': 'https://skotty/pub/sudo',
        },
        'walle.walle-project3': {
            'idm_status': 'dirty',
            'responsibles': set(),
            'hosts': set(),
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
        'hd.hd': {
            'hosts': {'hd_fqdn.yandex.next'},
            'idm_status': 'dirty',
            'responsibles': set([]),
            'flow': 'classic',
            'trusted_sources': set(),
            'key_sources': set(),
            'secure_ca_list_url': None,
            'insecure_ca_list_url': None,
            'krl_url': None,
            'sudo_ca_list_url': None,
        },
    }
    assert new_groups() == expected_groups

    with mock.patch('infra.cauth.server.master.importers.management.commands.poke_hanging_hosts.get_queued_dsts') as get_queue,\
            mock.patch('infra.cauth.server.master.api.idm.update.IdmClient.perform_batch') as perform_batch,\
            mock.patch('infra.cauth.server.master.api.idm.update.create_dst_requests') as create_requests:  # noqa
        get_queue.return_value = set()  # никаких тасок в очереди нет
        create_requests.return_value = [1, 1, 1]  # какие-то три запроса
        with override_switch('cauth.poke_dirty_hosts', active=True):
            PokeHostsCommand()._handle()
        assert len(perform_batch.call_args_list) == len(new_groups()) + len(new_hosts()) - 1  # один сервер актуальный

    assert new_hosts() == expected_hosts
    assert new_groups() == expected_groups

    with mock.patch('infra.cauth.server.master.importers.management.commands.poke_hanging_hosts.get_queued_dsts') as get_queue,\
            mock.patch('infra.cauth.server.master.api.idm.update.IdmClient.perform_batch') as perform_batch,\
            mock.patch('infra.cauth.server.master.api.idm.update.create_dst_requests') as create_requests:  # noqa
        get_queue.return_value = {'host2.yandex.net'}  # в очереди есть таска на один хост
        create_requests.return_value = [1, 1, 1]  # какие-то три запроса
        with override_switch('cauth.poke_dirty_hosts', active=True):
            PokeHostsCommand()._handle()
        # один сервер актуальный, другой уже есть в очереди
        assert len(perform_batch.call_args_list) == len(new_groups()) + len(new_hosts()) - 2

    assert new_hosts() == expected_hosts
    assert new_groups() == expected_groups

    with mock.patch('infra.cauth.server.master.importers.management.commands.poke_hanging_hosts.get_queued_dsts') as get_queue, \
            mock.patch('infra.cauth.server.master.api.idm.update.IdmClient.perform_batch') as perform_batch, \
            mock.patch('infra.cauth.server.master.api.idm.update.create_dst_requests') as create_requests:  # noqa
        get_queue.return_value = set()  # никаких тасок в очереди нет
        create_requests.return_value = []  # По диффу определили, что запросов делать не надо
        with override_switch('cauth.poke_dirty_hosts', active=True):
            PokeHostsCommand()._handle()
        assert len(perform_batch.call_args_list) == 0

    for host in list(expected_hosts.values()):
        host['idm_status'] = 'actual'
    for group in list(expected_groups.values()):
        group['idm_status'] = 'actual'
    assert new_hosts() == expected_hosts
    assert new_groups() == expected_groups

    # Источники SUSPENDED_SOURCES исключены из синхронизации
    golem = Source.get_one(name='golem')
    assert len(golem.servers) == 2

    mock_bot_https(b'', [])
    mock_hd_https('[]', [], [])
    with mock.patch('infra.cauth.server.master.api.tasks.push_idm_update'),\
            mock.patch('requests.sessions.Session.get') as session_get,\
            mock.patch('infra.cauth.server.master.importers.base.SubtaskPool', new=MockedPool),\
            mock.patch('infra.cauth.server.master.utils.tasks.SubtaskPool', new=MockedPool),\
            override_settings(SUSPENDED_SOURCES=['golem']),\
            override_settings(CAUTH_SKIP_REMOVING_ON_IMPORT_HOURS=0):
        session_get.side_effect = empty_answers
        run_import.apply(args=['servers'])

    golem = Source.get_one(name='golem')
    assert len(golem.servers) == 2

    with mock.patch('infra.cauth.server.master.api.tasks.push_idm_update'),\
            mock.patch('requests.sessions.Session.get') as session_get,\
            mock.patch('infra.cauth.server.master.importers.base.SubtaskPool', new=MockedPool),\
            mock.patch('infra.cauth.server.master.utils.tasks.SubtaskPool', new=MockedPool),\
            override_settings(CAUTH_SKIP_REMOVING_ON_IMPORT_HOURS=0):
        session_get.side_effect = empty_answers
        run_import.apply(args=['servers'])

    golem = Source.get_one(name='golem')
    assert len(golem.servers) == 0

    # Проверим, что синк не удаляет свежие сервера
    request_data = {
        'srv': 'conductor-fresh-host.yandex.net',
        'resp': 'user1',
    }
    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT='/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=ITO'
                                      '/CN=client.c.yandex-team.ru'
                                      '/emailAddress=rccs-admin@yandex-team.ru',
        )
        assert response.status_code == 200

    fresh_server = {
        'is_baremetal': False,
        'sources': ['conductor'],
        'hostname': 'conductor-fresh-host.yandex.net',
        'type': 'server',
        'idm_status': 'dirty',
    }
    assert fresh_server == new_hosts()['conductor-fresh-host.yandex.net']

    with mock.patch('infra.cauth.server.master.api.tasks.push_idm_update'),\
            mock.patch('requests.sessions.Session.get') as session_get,\
            mock.patch('infra.cauth.server.master.importers.base.SubtaskPool', new=MockedPool),\
            mock.patch('infra.cauth.server.master.utils.tasks.SubtaskPool', new=MockedPool):
        session_get.side_effect = mock_answers
        run_import.apply(args=['servers'])

    assert fresh_server == new_hosts()['conductor-fresh-host.yandex.net']

    with mock.patch('infra.cauth.server.master.api.tasks.push_idm_update'),\
            mock.patch('requests.sessions.Session.get') as session_get,\
            mock.patch('infra.cauth.server.master.importers.base.SubtaskPool', new=MockedPool),\
            mock.patch('infra.cauth.server.master.utils.tasks.SubtaskPool', new=MockedPool),\
            override_settings(CAUTH_SKIP_REMOVING_ON_IMPORT_HOURS=0):
        session_get.side_effect = mock_answers
        run_import.apply(args=['servers'])

    assert 'conductor-fresh-host.yandex.net' not in new_hosts()

    User.query.delete()
    Source.query.delete()
    Server.query.delete()
    Session.flush()

# coding: utf-8
import pytest
import inject
import mock
import logging

from sepelib.core import config as appconfig

from infra.swatlib import metrics
from awacs.lib import idmclient
from awacs.model.cron.webauthrolesidmsyncer import (WebauthIdmRole, ParseIdmJsonToRoleError, WEBAUTH_SYSTEM, Node,
                                                    WebauthRolesIdmSyncer)
from infra.awacs.proto import model_pb2
from awtest.mocks.idm_client import MockIDMClient
from awtest import wait_until_passes, check_log


INSTALLATION = 'test-awacs'
NS_ID = 'adm-nanny.yandex-team.ru'
NS_ID2 = 'ferenets.yandex.net'

LOGIN = 'ferenets'

op_log = logging.getLogger('test-op-log')


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(idmclient.IIDMClient, MockIDMClient('idm-test.yandex-team.ru', 'XXX'))
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def test_webauth_idm_role():
    role = WebauthIdmRole(op_log, INSTALLATION, NS_ID, responsible={LOGIN, })

    assert role.installation == INSTALLATION
    assert role.namespace_id == NS_ID
    assert role.domain_id is None
    assert role.slug_path == '/installation/{}/namespace/{}/'.format(INSTALLATION, NS_ID)
    assert role.role_slug_path == '/installation/{}/namespace/{}/role_type/user/'.format(INSTALLATION, NS_ID)
    assert role.responsible == {LOGIN, }
    assert role.get_responsible() == [{'username': LOGIN, 'notify': True}]

    assert role.nodes() == [
        ('/', 'installation', 'Installation', u'Инсталляция'),
        ('/installation/', INSTALLATION, INSTALLATION, INSTALLATION),
        ('/installation/test-awacs/', 'namespace', 'Namespace', u'Неймспейс'),
        ('/installation/test-awacs/namespace/', NS_ID, NS_ID, NS_ID),
        ('/installation/test-awacs/namespace/adm-nanny.yandex-team.ru/', 'role_type', 'Type', u'Тип'),
        ('/installation/test-awacs/namespace/adm-nanny.yandex-team.ru/role_type/', 'user', 'User', u'Пользователь')
    ]

    idm_client = idmclient.IIDMClient.instance()

    role.update()
    assert idm_client.last_update_call_args == (WEBAUTH_SYSTEM, role.role_slug_path, role.get_responsible())

    role.remove()
    assert idm_client.last_remove_call_args == (WEBAUTH_SYSTEM, role.slug_path)

    role.add(slug_paths_to_be_add=[])
    assert idm_client.last_batch_call_args is None

    role.add(slug_paths_to_be_add=[
        '/installation/test-awacs/namespace/adm-nanny.yandex-team.ru/',
        '/installation/test-awacs/namespace/adm-nanny.yandex-team.ru/role_type/',
        '/installation/test-awacs/namespace/adm-nanny.yandex-team.ru/role_type/user/'
    ])

    assert idm_client.total_batch_calls_count == 1
    subrequests = idm_client.last_batch_call_args[0]
    assert len(subrequests) == 3
    assert not subrequests[0]['body'].get('responsibilities')
    assert not subrequests[1]['body'].get('responsibilities')
    assert subrequests[2]['body'].get('responsibilities') == role.get_responsible()

    idm_json = {'slug_path': role.role_slug_path}
    idm_role = WebauthIdmRole.from_idm_json(op_log, idm_json)

    assert idm_role != role
    assert idm_role.key() == role.key()

    idm_json = {'slug_path': '/installation/'}
    with pytest.raises(ParseIdmJsonToRoleError):
        WebauthIdmRole.from_idm_json(op_log, idm_json)


def test_nodes():
    root = Node(slug_path='/')
    assert root.list_roles() == set()

    root.ensure_by_path(slugs=['installation', INSTALLATION])
    assert root.list_roles() == set()

    root.ensure_by_path(slugs=['installation', INSTALLATION, 'namespace'], op_id='1')
    assert 'namespace' in root.children['installation'].children[INSTALLATION].children
    root.revert(op_id='2')
    assert 'namespace' in root.children['installation'].children[INSTALLATION].children
    root.revert(op_id='1')
    assert 'namespace' not in root.children['installation'].children[INSTALLATION].children

    role = WebauthIdmRole(op_log, INSTALLATION, NS_ID, responsible={LOGIN, })
    role2 = WebauthIdmRole(op_log, INSTALLATION, NS_ID2)
    root.add(role)
    assert root.list_roles() == {role, }
    assert root.children['installation'].list_roles() == {role, }

    root.add(role2)
    assert root.list_roles() == {role, role2}


def test_webauth_roles_idm_syncer(zk_storage, cache, caplog):
    def create_namespace(namespace_id):
        ns_pb = model_pb2.Namespace()
        ns_pb.meta.id = namespace_id
        ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
        ns_pb.meta.auth.staff.owners.logins.append(LOGIN)
        ns_pb.meta.webauth.responsible.logins.append(LOGIN)
        ns_pb.order.status.status = 'FINISHED'
        zk_storage.create_namespace(namespace_id, ns_pb)
        wait_until_passes(lambda: cache.must_get_namespace(namespace_id))

    create_namespace(NS_ID)
    syncer = WebauthRolesIdmSyncer(metrics.Registry(), op_log)
    with pytest.raises(appconfig.MissingConfigOptionError, match="Configuration option 'run.installation' is missing."):
        syncer.process()

    appconfig.set_value('run.installation', 'awacs')
    appconfig.set_value('run.debug', True)
    with pytest.raises(AssertionError, match='"run.installation" can not be set to "awacs" if "run.debug" is True'):
        syncer.process()

    appconfig.set_value('run.installation', INSTALLATION)
    syncer.process()
    idm_client = idmclient.IIDMClient.instance()

    assert idm_client.last_update_call_args is None
    assert idm_client.last_remove_call_args is None
    assert idm_client.total_batch_calls_count == 1
    subrequests = idm_client.last_batch_call_args[0]
    assert len(subrequests) == 6
    role = WebauthIdmRole(op_log, INSTALLATION, NS_ID, responsible={LOGIN, })
    assert subrequests[5]['body'].get('responsibilities') == role.get_responsible()

    roles = []
    for i in range(1000):
        namespace_id = 'ns_{}'.format(i)
        if i >= 10:
            roles.append(WebauthIdmRole(op_log, INSTALLATION, namespace_id, responsible={LOGIN, }))
        if i < 50:
            create_namespace(namespace_id)

    # Now we have 51 namespace in awacs
    with mock.patch.object(idm_client, 'iterate_system_roles', return_value=[
        {'slug_path': role.role_slug_path} for role in roles
    ]):
        syncer.process()

    assert idm_client.total_batch_calls_count == 12
    assert idm_client.total_update_calls_count == 40
    assert idm_client.total_remove_calls_count == 0
    with check_log(caplog, clear=False) as log:
        assert 'An attempt to remove more than 30% roles from IDM - will not remove anything' in log.records_text()

    with mock.patch.object(idm_client, 'iterate_system_roles', return_value=[
        {'slug_path': role.role_slug_path} for role in roles[:50]
    ]):
        syncer.process()

    assert idm_client.total_remove_calls_count == 10

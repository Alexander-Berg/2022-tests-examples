import json
import logging
import mock

import datetime
import freezegun
import pytest
from infra.qyp.qdm.src.server.evoq.common import DC

from infra.qyp.qdm.src.server.evoq.manager import EvoqManager
from infra.qyp.qdm.src.server.evoq.process import EvoqProcess
from infra.qyp.qdm.src.server.model import Evoq


class Response(object):
    def __init__(self, data, status_code=None):
        self.status_code = status_code or 200
        self.text = json.dumps(data)
        self.headers = {'Content-Type': 'application/json'}


def deblocker_apply(handler, *args, **kwargs):
    return handler(*args, **kwargs)


@pytest.fixture
def wait_session_kit():
    session = 'infra.qyp.qdm.src.server.evoq.process.Session'
    session_obj_mock = mock.Mock()
    session_obj_mock.state = 'archive'
    session_obj_mock.key = '1'

    revision = 'infra.qyp.qdm.src.server.evoq.process.StorageRevision'
    revision_obj_mock = mock.Mock()
    revision_obj_mock.state = 'active'
    revision_obj_mock.rev_id = 1
    revision_obj_mock.key = '1'

    with mock.patch(session, return_value=session_obj_mock):
        with mock.patch(revision, return_value=revision_obj_mock):
            yield


@pytest.fixture
def evoq_process():
    cluster = 'test_sas'
    vm_id = 'my-shiny-vm'
    node_id = 'sas0-0000'
    log = logging.getLogger('evoqproc')
    db_mock = mock.Mock()
    qnotifier_mock = mock.Mock()
    vmproxy_mock = mock.Mock()
    yp_mock = mock.Mock()
    evoq = Evoq(db=db_mock, vm_id='{}.{}'.format(vm_id, cluster), node_id=node_id)
    evoq.run_cnt = 0
    evoq_process = EvoqProcess(
        log=log,
        db=db_mock,
        key=None,
        evoq=evoq,
        qnotifier_client=qnotifier_mock,
        vmproxy_client=vmproxy_mock,
        yp_client=yp_mock,
        disable_vm_leaving=False
    )
    return evoq_process


@freezegun.freeze_time(datetime.datetime(2001, 1, 1))
@mock.patch('infra.qyp.qdm.src.server.evoq.manager.Deblock.apply', side_effect=deblocker_apply)
def test_evoq_planner(mock_apply):
    now = int((datetime.datetime(2001, 1, 1) - datetime.datetime(1970, 1, 1)).total_seconds() * 10 ** 6)
    long_ago = int((datetime.datetime(2000, 1, 1) - datetime.datetime(1970, 1, 1)).total_seconds() * 10 ** 6)
    db_mock = mock.Mock()
    yp_mock = mock.Mock()
    evoq_mngr = EvoqManager(
        db=db_mock,
        dbt=None,
        qnotifier_client=mock.Mock(),
        yp_client=yp_mock,
        vmproxy_client=mock.Mock(),
        max_active_jobs=0,
    )
    all_pods = [
        {
            'id': 'vm1',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': now,
            'maintenance': None,
        },
        {
            'id': 'vm2',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': long_ago,
            'maintenance': None
        },
        {
            'id': 'vm3',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'hfsm'},
            'maintenance': {'state': 'acknowledged'},
        },
        {
            'id': 'vm4',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'hfsm'},
            'maintenance': {'state': 'requested'},
        },
        {
            'id': 'vm5',
            'node_id': 'sas0-0000',
            'eviction': None,
            'maintenance': {'state': 'requested'},
        }
    ]
    yp_mock.select_pods.side_effect = all_pods, [], [], [], [], []

    with mock.patch('infra.qyp.qdm.src.server.evoq.manager.Evoq') as MockEvoq:
        evoq_mngr.scheduler()

    assert len(MockEvoq.call_args_list) == 3
    MockEvoq.assert_any_call(db_mock, 'vm2.test_sas', 'sas0-0000')
    MockEvoq.assert_any_call(db_mock, 'vm4.test_sas', 'sas0-0000')
    MockEvoq.assert_any_call(db_mock, 'vm5.test_sas', 'sas0-0000')


@freezegun.freeze_time(datetime.datetime(2001, 1, 1))
@mock.patch('infra.qyp.qdm.src.server.evoq.manager.Deblock.apply', side_effect=deblocker_apply)
def test_ignore_evictions_from_hs(mock_apply):
    now = int((datetime.datetime(2001, 1, 1) - datetime.datetime(1970, 1, 1)).total_seconds() * 10 ** 6)
    long_ago = int((datetime.datetime(2000, 1, 1) - datetime.datetime(1970, 1, 1)).total_seconds() * 10 ** 6)
    day_ago = int((datetime.datetime(2000, 12, 31) - datetime.datetime(1970, 1, 1)).total_seconds() * 10 ** 6)
    db_mock = mock.Mock()
    yp_mock = mock.Mock()
    evoq_mngr = EvoqManager(
        db=db_mock,
        dbt=None,
        qnotifier_client=mock.Mock(),
        yp_client=yp_mock,
        vmproxy_client=mock.Mock(),
        max_active_jobs=0,
    )
    sas_pods = [
        {
            'id': 'vm1-sas',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': now,
            'maintenance': None,
        },
        {
            'id': 'vm2-sas',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': long_ago,
            'maintenance': None,
        },
        {
            'id': 'vm3-sas',
            'node_id': 'sas0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': day_ago,
            'maintenance': None,
        },
    ]
    iva_pods = [
        {
            'id': 'vm1-iva',
            'node_id': 'iva0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': now,
            'maintenance': None,
        },
        {
            'id': 'vm2-iva',
            'node_id': 'iva0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': long_ago,
            'maintenance': None,
        },
        {
            'id': 'vm3-iva',
            'node_id': 'iva0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': day_ago,
            'maintenance': None,
        },
        {
            'id': "dev-xafster-win",
            'node_id': 'iva0-0000',
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'scheduling_last_updated': day_ago,
            'maintenance': None,
        }
    ]
    yp_mock.select_pods.side_effect = [], [], sas_pods, [], iva_pods
    with mock.patch('infra.qyp.qdm.src.server.evoq.manager.Evoq') as MockEvoq:
        evoq_mngr.scheduler()

    assert len(MockEvoq.call_args_list) == 3
    MockEvoq.assert_any_call(db_mock, 'vm2-sas.sas', 'sas0-0000')
    MockEvoq.assert_any_call(db_mock, 'vm2-iva.iva', 'iva0-0000')
    MockEvoq.assert_any_call(db_mock, 'vm3-iva.iva', 'iva0-0000')


@mock.patch('infra.qyp.qdm.src.server.evoq.process.Deblock.apply', side_effect=deblocker_apply)
def test_vm_evicted(mock_apply, wait_session_kit, evoq_process):
    vm_id = evoq_process.pod_id
    evoq_process.vmproxy_client.api_call.side_effect = [
        # first check
        Response({'state': {'type': 'STOPPED'}}),
        # backup call
        Response({}),
        # eviction ack call
        Response({'result': 'DONE_EVICTION'}),
        # last check
        Response({'state': {'type': 'RUNNING'}}),
    ]
    evoq_process.yp_client.select_pods.return_value = [
        {
            'id': vm_id,
            'node_id': evoq_process.evoq.node_id,
            'fqdn': ['1', '2'],
            'eviction': {'state': 'requested', 'reason': 'scheduler'},
            'maintenance': None,
            'owners': {'users': ['user'], 'groups': ['group']},
            'labels': {'vmagent_version': '0.34'},
            'maintenance_ts': 0,
        }
    ]
    evoq_process.run()
    vmproxy_url = DC[evoq_process.dc]['vmproxy']
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/MakeAction/',
        {
            'vm_id': {'pod_id': vm_id},
            'action': 'QDMUPLOAD',
            'qdmreq': {'key': '1'}
        }
    )
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/AcknowledgeEviction/',
        {
            'vm_id': vm_id,
            'qdm_res_id': 'qdm:1',
            'use_evict': False
        }
    )


@mock.patch('infra.qyp.qdm.src.server.evoq.process.Deblock.apply', side_effect=deblocker_apply)
def test_disabled_leaving(mock_apply, wait_session_kit, evoq_process):
    evoq_process.disable_vm_leaving = True
    vm_id = evoq_process.pod_id
    evoq_process.vmproxy_client.api_call.side_effect = [
        # first check
        Response({'state': {'type': 'STOPPED'}}),
        # backup call
        Response({}),
        # eviction ack call
        Response({'result': 'DONE_EVICTION'}),
        # last check
        Response({'state': {'type': 'RUNNING'}}),
    ]
    evoq_process.yp_client.select_pods.return_value = [
        {
            'id': vm_id,
            'node_id': evoq_process.evoq.node_id,
            'fqdn': ['1', '2'],
            'eviction': {'state': 'requested', 'reason': 'hfsm'},
            'maintenance': {'state': 'requested', 'info': {'kind': 'reboot'}},
            'owners': {'users': ['user'], 'groups': ['group']},
            'labels': {'vmagent_version': '0.34'},
            'maintenance_ts': 0,
        }
    ]
    evoq_process.run()
    vmproxy_url = DC[evoq_process.dc]['vmproxy']
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/MakeAction/',
        {
            'vm_id': {'pod_id': vm_id},
            'action': 'QDMUPLOAD',
            'qdmreq': {'key': '1'}
        }
    )
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/AcknowledgeEviction/',
        {
            'vm_id': vm_id,
            'qdm_res_id': 'qdm:1',
            'use_evict': False
        }
    )


@mock.patch('infra.qyp.qdm.src.server.evoq.process.Deblock.apply', side_effect=deblocker_apply)
def test_destructive_maintenance(mock_apply, wait_session_kit, evoq_process):
    vm_id = evoq_process.pod_id
    evoq_process.vmproxy_client.api_call.side_effect = [
        # first check
        Response({'state': {'type': 'STOPPED'}}),
        # backup call
        Response({}),
        # eviction ack call
        Response({'result': 'DONE_EVICTION'}),
        # last check
        Response({'state': {'type': 'RUNNING'}}),
    ]
    evoq_process.yp_client.select_pods.return_value = [
        {
            'id': vm_id,
            'node_id': evoq_process.evoq.node_id,
            'fqdn': ['1', '2'],
            'eviction': {'state': 'requested', 'reason': 'hfsm'},
            'maintenance': {'state': 'requested', 'info': {'kind': 'redeploy'}},
            'owners': {'users': ['user'], 'groups': ['group']},
            'labels': {'vmagent_version': '0.34'},
            'maintenance_ts': 0,
        }
    ]
    evoq_process.run()
    vmproxy_url = DC[evoq_process.dc]['vmproxy']
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/MakeAction/',
        {
            'vm_id': {'pod_id': vm_id},
            'action': 'QDMUPLOAD',
            'qdmreq': {'key': '1'}
        }
    )
    evoq_process.vmproxy_client.api_call.assert_any_call(
        vmproxy_url + '/api/AcknowledgeEviction/',
        {
            'vm_id': vm_id,
            'qdm_res_id': 'qdm:1',
            'use_evict': False
        }
    )


@mock.patch('infra.qyp.qdm.src.server.evoq.process.Deblock.apply', side_effect=deblocker_apply)
def test_non_destructive_maintenance(mock_apply, evoq_process):
    vm_id = evoq_process.pod_id
    evoq_process.vmproxy_client.api_call.side_effect = [
        Response({'state': {'type': 'RUNNING'}}),
    ]
    evoq_process.yp_client.select_pods.return_value = [
        {
            'id': vm_id,
            'node_id': evoq_process.evoq.node_id,
            'fqdn': ['1', '2'],
            'eviction': {'state': 'requested', 'reason': 'hfsm'},
            'maintenance': {'state': 'requested', 'info': {'kind': 'reboot'}},
            'owners': {'users': ['user'], 'groups': ['group']},
            'labels': {'vmagent_version': '0.34'},
            'maintenance_ts': 0,
        }
    ]
    evoq_process.run()
    evoq_process.vmproxy_client.api_call.assert_not_called()

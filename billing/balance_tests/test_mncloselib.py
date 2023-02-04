# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime
import xml.etree.ElementTree as et
import socket

import xmlrpclib
import mock
import pytest


from balance import mncloselib, muzzle_util
from balance.constants import NirvanaTaskStatus
from tests import object_builder as ob

NOW = datetime.datetime.now()
MONTH = datetime.datetime(NOW.year, NOW.month, 1)

NIRVANA_TASK_STATUS = {
    'task_new_openable': NirvanaTaskStatus.TASK_STATUS_NEW_OPENABLE,
    'task_stalled': NirvanaTaskStatus.TASK_STATUS_STALLED,
    'task_new_unopenable': NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE,
    'task_solved_reopenable': NirvanaTaskStatus.TASK_STATUS_RESOLVED,
}


def parse_xmlrpc_call(call_descr):
    root = et.fromstring(call_descr)
    return root.findtext('methodName'), [el.text for el in root.findall('params/param/value/./')]


@pytest.fixture
def xmlrpclib_call_mock():
    with mock.patch('xmlrpclib.Transport.single_request') as mock_obj:
        yield mock_obj


@pytest.fixture
def mnclose(request):
    return mncloselib.MNClose(
        'http://user:666@greed-666.paysys.yandex.ru:6666/be/',
        'user',
        getattr(request, 'param', None),
        use_medium=True
    )


class TestItasks(object):
    @pytest.fixture
    def mock_get_itasks(self, xmlrpclib_call_mock):
        def call_(host, handler, request_body, verbose=0):
            result = {
                'scheme_version': 0,
            }
            method_name, args = parse_xmlrpc_call(request_body)
            if method_name == 'NirvanaMnCloseTasks.get_status':
                if args[0] in NIRVANA_TASK_STATUS:
                    result['status'] = NIRVANA_TASK_STATUS[args[0]]
                else:
                    result['error'] = 'task not found'
                    result['error_type'] = mncloselib.UNKNOWN_TASK_ERROR_TYPE
                return (result, )
            else:
                raise ValueError

        xmlrpclib_call_mock.side_effect = call_
        return xmlrpclib_call_mock

    def test_get(self, mnclose, mock_get_itasks):
        now = datetime.datetime.now()
        itask = mnclose.itask('task_new_openable')
        assert 'task_new_openable' == itask.name
        assert MONTH == itask.inst_dt
        assert 'new' == itask.status
        assert ['open'] == itask.available_actions

        assert 1 == mock_get_itasks.call_count
        call_descr = mock_get_itasks.call_args[0][2]
        rpc_method_name, rpc_args = parse_xmlrpc_call(call_descr)
        assert 'NirvanaMnCloseTasks.get_status' == rpc_method_name
        assert now.strftime('%y-%m') == rpc_args[1]

    def test_get_unknown(self, mnclose, mock_get_itasks):
        now = datetime.datetime.now()
        with pytest.raises(mncloselib.UnknownTask):
            mnclose.itask('task_something_or_other')

        assert 1 == mock_get_itasks.call_count
        call_descr = mock_get_itasks.call_args[0][2]
        rpc_method_name, rpc_args = parse_xmlrpc_call(call_descr)
        assert 'NirvanaMnCloseTasks.get_status' == rpc_method_name
        assert now.strftime('%y-%m') == rpc_args[1]


class TestIaction(object):
    @pytest.fixture
    def mock_iaction(self, request, xmlrpclib_call_mock):
        ans = {
            'scheme_version': 0,
            'status': 'resolved'
        }
        param = getattr(request, 'param', None)
        if isinstance(param, dict):
            ans.update(param)

        if isinstance(param, BaseException):
            def raiser(*args, **kwargs):
                raise param

            xmlrpclib_call_mock.side_effect = raiser
        else:
            xmlrpclib_call_mock.return_value = (ans, )

        return xmlrpclib_call_mock

    @pytest.fixture
    def itask(self):
        return mncloselib.ITask(
            666, MONTH, 'task_god',
            'ascended_to_godhood',
            ['open', 'stall', 'resolve', 'reopen']
        )

    @pytest.mark.parametrize(
        'iaction', ['open', 'resolve', 'stall', 'reopen']
    )
    def test_ok(self, mnclose, itask, iaction, mock_iaction):
        now = datetime.datetime.now()
        getattr(mnclose, iaction)(itask)

        assert 1 == mock_iaction.call_count
        call_descr = mock_iaction.call_args[0][2]
        rpc_method_name, rpc_args = parse_xmlrpc_call(call_descr)

        assert 'NirvanaMnCloseTasks.set_status' == rpc_method_name
        assert ['task_god', now.strftime('%y-%m'), mncloselib._get_nirvana_status(iaction)] == rpc_args[:3]

    @pytest.mark.parametrize(
        'mnclose',
        [
            {
                'stall': {'num': 2, 'wait_min': 1, 'wait_max': 3},
                'resolve': {'num': 3, 'wait_min': 1, 'wait_max': 3},
            }
        ],
        indirect=['mnclose'],
        ids=['mnclose']
    )
    @pytest.mark.parametrize(
        'iaction, exc, retry_count, mock_iaction',
        [
            (
                    'open', mncloselib.MNCloseException, 1,
                    {'error': 'some error happened', 'error_type': 'some_error'}
            ),
            (
                    'stall', mncloselib.MNCloseException, 2,
                    {'error': 'some error happened', 'error_type': 'some_error'}
            ),
            (
                    'resolve', mncloselib.MNCloseException, 3,
                    {'error': 'some error happened', 'error_type': 'some_error'}
            ),
            (
                    'resolve', xmlrpclib.ProtocolError, 3, xmlrpclib.ProtocolError(None, None, None, None)
            ),
            (
                    'resolve', socket.error, 3, socket.error()
            ),
        ],
        indirect=['mock_iaction']
    )
    def test_fail(self, mnclose, itask, iaction, exc, retry_count, mock_iaction):
        now = datetime.datetime.now()
        with pytest.raises(exc):
            getattr(mnclose, iaction)(itask)

        assert retry_count == mock_iaction.call_count
        for call_args in mock_iaction.call_args_list:
            call_descr = call_args[0][2]
            rpc_method_name, rpc_args = parse_xmlrpc_call(call_descr)

            assert 'NirvanaMnCloseTasks.set_status' == rpc_method_name
            assert ['task_god', now.strftime('%y-%m'), mncloselib._get_nirvana_status(iaction)] == rpc_args[:3]


class TestMNCloseProcedure(object):
    @pytest.fixture
    def mock_mnclose_call(self, request, xmlrpclib_call_mock):
        ans = {
            'scheme_version': 0,
            'flash': 'ляляля',
            'ret_code': 200
        }
        param = getattr(request, 'param', {})
        ans.update(param)

        def call_(host, handler, request_body, verbose=0):
            method_name, args = parse_xmlrpc_call(request_body)
            result = {
                'scheme_version': 0,
            }
            if method_name == 'NirvanaMnCloseTasks.get_status':
                result['status'] = NIRVANA_TASK_STATUS[args[0]]
                return (result, )
            elif method_name == 'NirvanaMnCloseTasks.set_status':
                result['status'] = NIRVANA_TASK_STATUS[args[0]]
                return (result, )
            else:
                raise ValueError

        xmlrpclib_call_mock.side_effect = call_
        return xmlrpclib_call_mock

    @pytest.mark.parametrize('task_name', ['task_new_openable', 'task_stalled'])
    def test_openable(self, mnclose, task_name, mock_mnclose_call):

        @mncloselib.mnclose_procedure(task_name)
        def func(mnclose, *args, **kwargs):
            assert (1, 2, 3) == args
            assert {'a': 4, 'b': 5} == kwargs
            return 666

        res = func(mnclose, 1, 2, 3, a=4, b=5)
        assert 666 == res

        assert 3 == mock_mnclose_call.call_count
        call_args_index, call_args_open, call_args_resolve = mock_mnclose_call.call_args_list

        index_method_name, index_args = parse_xmlrpc_call(call_args_index[0][2])
        assert 'NirvanaMnCloseTasks.get_status' == index_method_name

        open_method_name, open_args = parse_xmlrpc_call(call_args_open[0][2])
        assert 'NirvanaMnCloseTasks.set_status' == open_method_name
        assert NirvanaTaskStatus.TASK_STATUS_OPENED == open_args[2]

        resolve_method_name, resolve_args = parse_xmlrpc_call(call_args_resolve[0][2])
        assert 'NirvanaMnCloseTasks.set_status' == resolve_method_name
        assert NirvanaTaskStatus.TASK_STATUS_RESOLVED == resolve_args[2]

    @pytest.mark.parametrize('task_name', ['task_new_unopenable', 'task_solved_reopenable'])
    def test_unopenable(self, mnclose, task_name, mock_mnclose_call):

        @mncloselib.mnclose_procedure(task_name)
        def func(mnclose):
            return 666

        res = func(mnclose)
        assert res is None

        assert 1 == mock_mnclose_call.call_count
        call_args_index, = mock_mnclose_call.call_args_list

        index_method_name, index_args = parse_xmlrpc_call(call_args_index[0][2])
        assert 'NirvanaMnCloseTasks.get_status' == index_method_name

    def test_exception(self, mnclose, mock_mnclose_call):

        @mncloselib.mnclose_procedure('task_new_openable')
        def func(mnclose):
            assert 'War' is 'Peace'

        with pytest.raises(AssertionError):
            func(mnclose)

        assert 3 == mock_mnclose_call.call_count
        call_args_index, call_args_open, call_args_stall = mock_mnclose_call.call_args_list

        index_method_name, index_args = parse_xmlrpc_call(call_args_index[0][2])
        assert 'NirvanaMnCloseTasks.get_status' == index_method_name

        open_method_name, open_args = parse_xmlrpc_call(call_args_open[0][2])
        assert 'NirvanaMnCloseTasks.set_status' == open_method_name
        assert NirvanaTaskStatus.TASK_STATUS_OPENED == open_args[2]

        stall_method_name, stall_args = parse_xmlrpc_call(call_args_stall[0][2])
        assert 'NirvanaMnCloseTasks.set_status' == stall_method_name
        assert NirvanaTaskStatus.TASK_STATUS_STALLED == stall_args[2]


class TestDataBaseRequests(object):
    def test_get_task_last_status(self, session):
        task = ob.NirvanaMnCloseSyncBuilder().build(session).obj

        status = mncloselib.get_task_last_status(session, task.task_id, task.dt)

        assert task.status == status

    def test_get_task_last_status_for_old_period(self, session):
        # Тест месяца, которого нет в t_nirvana_mnclose_sync

        status = mncloselib.get_task_last_status(session, 'dcs_bua', datetime.datetime(2019, 1, 1))

        assert status == NirvanaTaskStatus.TASK_STATUS_RESOLVED

    def test_get_mnclose_all_tasks_statuses(self, session):
        first_task = ob.NirvanaMnCloseSyncBuilder(task_id='first_task').build(session).obj
        second_task = ob.NirvanaMnCloseSyncBuilder(task_id='second_task').build(session).obj

        statuses = mncloselib.get_mnclose_all_tasks_statuses(session)

        assert statuses[first_task.task_id] == first_task.status
        assert statuses[second_task.task_id] == second_task.status

    def test_get_first_non_closed_dt_when_opened(self, session):
        current_inst_dt = MONTH
        previous_inst_dt = muzzle_util.add_months_to_date(current_inst_dt, -1)

        ob.NirvanaMnCloseSyncBuilder(
            task_id='monthly_limits',
            dt=previous_inst_dt,
            status=NirvanaTaskStatus.TASK_STATUS_RESOLVED
        ).build(session)

        ob.NirvanaMnCloseSyncBuilder(
            task_id='monthly_limits',
            dt=current_inst_dt,
            status=NirvanaTaskStatus.TASK_STATUS_OPENED
        ).build(session)

        dt = mncloselib.get_first_non_closed_dt(session)

        assert previous_inst_dt == dt

    def test_get_first_non_closed_dt_when_resolved(self, session):
        current_inst_dt = MONTH
        previous_inst_dt = muzzle_util.add_months_to_date(current_inst_dt, -1)

        ob.NirvanaMnCloseSyncBuilder(
            task_id='monthly_limits',
            dt=previous_inst_dt,
            status=NirvanaTaskStatus.TASK_STATUS_RESOLVED
        ).build(session)

        ob.NirvanaMnCloseSyncBuilder(
            task_id='monthly_limits',
            dt=current_inst_dt,
            status=NirvanaTaskStatus.TASK_STATUS_RESOLVED
        ).build(session)

        dt = mncloselib.get_first_non_closed_dt(session)

        assert current_inst_dt == dt


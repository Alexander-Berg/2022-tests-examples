import os
import time
import mock

from infra.ya_salt.lib import initial_setup
from infra.ya_salt.lib import pbutil
from infra.ya_salt.proto import ya_salt_pb2


def test_is_not_ready_too_long():
    status = ya_salt_pb2.HostmanStatus()

    assert initial_setup.Solver.is_not_ready_too_long(status) is False

    pbutil.false_cond(status.initial_setup_passed, 'In test')

    # Check too long
    def time_func():
        return status.initial_setup_passed.transition_time.seconds + 10 * 24 * 3600

    assert initial_setup.Solver.is_not_ready_too_long(status, time_func=time_func) is True

    # Check not too long
    def time_func():
        return status.initial_setup_passed.transition_time.seconds + 1 * 3600

    assert initial_setup.Solver.is_not_ready_too_long(status, time_func=time_func) is False


def test_kernel_is_ready_or_need_reboot():
    s = ya_salt_pb2.HostmanStatus()
    s.kernel.ready.status = 'True'
    assert initial_setup.Solver.kernel_is_ready_or_need_reboot(s.kernel)
    s.kernel.ready.Clear()
    s.kernel.need_reboot.status = 'True'
    assert initial_setup.Solver.kernel_is_ready_or_need_reboot(s.kernel)
    s.kernel.need_reboot.status = 'False'
    assert not initial_setup.Solver.kernel_is_ready_or_need_reboot(s.kernel)


def test_ready_err():
    sp = ya_salt_pb2.HostmanSpec()
    st = ya_salt_pb2.HostmanStatus()
    st.initial_setup_passed.status = 'True'
    ready_err = initial_setup.Solver.ready_err
    assert ready_err(sp, st) is None
    st.Clear()
    assert ready_err(sp, st) == 'empty environment in salt'
    sp.salt.environment = 'search_runtime'
    st.salt.execution_ok.status = 'False'
    assert ready_err(sp, st) == 'salt execution failed'
    st.salt.execution_ok.status = 'True'
    assert ready_err(sp, st) == 'no salt states executed'
    st.salt_components.add().salt.state_results.add().ok = False
    assert ready_err(sp, st) == 'some salt states are failed'
    del st.salt_components[:]
    st.salt_components.add().salt.state_results.add().ok = True
    st.initial_setup_passed.status = 'mock'
    st.initial_setup_passed.transition_time.FromSeconds(long(time.time() - initial_setup.Solver.BROKEN_GRACE_PERIOD - 10))
    assert ready_err(sp, st) == 'failed to pass initial setup for too long'
    st.initial_setup_passed.transition_time.FromSeconds(long(time.time() - initial_setup.Solver.BROKEN_GRACE_PERIOD + 10))
    st.kernel.need_reboot.status = 'False'
    assert ready_err(sp, st) == 'kernel not ready and not ready for reboot'
    st.kernel.ready.status = 'True'
    pbutil.unkn_cond(st.hostctl.ok, 'checking on you')
    assert ready_err(sp, st) == 'hostctl execution failed: checking on you'
    st.hostctl.ok.status = 'True'
    assert ready_err(sp, st) is None


def test_infer_status(monkeypatch):
    sp = ya_salt_pb2.HostmanSpec()
    st = ya_salt_pb2.HostmanStatus()
    sp.need_initial_setup = False
    assert initial_setup.infer_status(sp, st) == initial_setup.Status.PASSED
    sp.need_initial_setup = True
    st.initial_setup.boot_id = 'mock'
    st.node_info.boot_id = 'mock2'
    assert initial_setup.infer_status(sp, st) == initial_setup.Status.REBOOT_DONE
    st.initial_setup.Clear()
    monkeypatch.setattr(initial_setup.Solver, 'ready_err', classmethod(lambda *args, **kwargs: None))
    assert initial_setup.infer_status(sp, st) == initial_setup.Status.NEEDS_REBOOT
    monkeypatch.setattr(initial_setup.Solver, 'ready_err', classmethod(lambda *args, **kwargs: 'mock'))
    assert initial_setup.infer_status(sp, st) == initial_setup.Status.NOT_READY


def test_fsm_passed():
    sp = ya_salt_pb2.HostmanSpec()
    st = ya_salt_pb2.HostmanStatus()
    assert initial_setup.fsm_passed(None, sp, st) is None
    assert st.initial_setup_passed.status == "True"


def test_fsm_not_passed(monkeypatch):
    sp = ya_salt_pb2.HostmanSpec()
    st = ya_salt_pb2.HostmanStatus()
    monkeypatch.setattr(initial_setup.Solver, 'ready_err', classmethod(lambda *args, **kwargs: 'mock'))
    initial_setup.fsm_not_passed(None, sp, st)
    assert st.initial_setup.need_reboot.status == 'False'
    assert st.initial_setup.need_reboot.message == 'mock'
    assert st.initial_setup_passed.status == 'False'
    assert st.initial_setup_passed.message == 'mock'


def test_fsm_needs_reboot():
    st = ya_salt_pb2.HostmanStatus()
    st.node_info.boot_id = 'mock'
    initial_setup.fsm_needs_reboot(None, None, st)
    assert st.initial_setup.boot_id == 'mock'
    assert st.initial_setup.need_reboot.status == 'True'
    assert st.initial_setup.need_reboot.message == 'To finish initial setup'
    assert st.initial_setup_passed.status == 'False'
    assert st.initial_setup_passed.message == 'Awaiting reboot'


def test_fsm_reboot_done(monkeypatch):
    ctx = initial_setup.Ctx('mock')
    st = ya_salt_pb2.HostmanStatus()
    sp = ya_salt_pb2.HostmanSpec()
    unlink = mock.Mock()
    monkeypatch.setattr(os, 'unlink', unlink)
    initial_setup.fsm_reboot_done(ctx, sp, st)
    assert st.initial_setup.need_reboot.status == 'False'
    assert st.initial_setup.need_reboot.message == 'Reboot done'
    assert sp.need_initial_setup is False
    assert st.initial_setup_passed.status == 'True'
    unlink.assert_called_once_with('mock')

    unlink.reset_mock()
    unlink.side_effect = IOError('error mock')
    st.Clear()
    sp.Clear()
    initial_setup.fsm_reboot_done(ctx, sp, st)
    assert st.initial_setup.need_reboot.status == 'False'
    assert st.initial_setup.need_reboot.message == 'Reboot done'
    assert st.initial_setup_passed.status == 'False'

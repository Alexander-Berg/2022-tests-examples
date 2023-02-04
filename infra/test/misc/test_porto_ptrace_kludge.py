import pytest
import errno
import kern
import os


def check_ptrace(pid):
    ret = kern.ptrace(kern.PTRACE_ATTACH, pid, 0, 0)
    if ret == 0:
        _pid, status = os.waitpid(pid, 0)
        assert _pid == pid and os.WIFSTOPPED(status)
        assert kern.ptrace(kern.PTRACE_DETACH, pid, 0, 0) == 0
    return ret


def test_porto_ptrace_kludge(make_task):
    assert kern.prctl(kern.PR_GET_DUMPABLE) == 1

    pidns = kern.Namespace('pid', unshare=True)
    init_task = make_task(namespaces=[pidns])
    assert init_task.call_func(os.getpid) == 1
    pidns = kern.Namespace('pid', init_task.pid)

    probe = make_task(namespaces=[pidns])
    assert probe.call_func(os.getpid) == 2
    assert probe.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 1

    if kern.prctl(kern.PR_SET_DUMPABLE_INIT_NS) != 0:
        pytest.xfail("prctl(PR_SET_DUMPABLE_INIT_NS) is not supported")

    assert kern.prctl(kern.PR_GET_DUMPABLE) == 3

    dummy = make_task(namespaces=[pidns])
    assert dummy.call_func(os.getpid) == 3
    assert dummy.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 3

    assert check_ptrace(dummy.pid) == 0
    assert probe.call_func(check_ptrace, 3) == -errno.EPERM

    assert probe.call_func(kern.prctl, kern.PR_SET_DUMPABLE_INIT_NS) == -errno.EPERM
    assert probe.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 1

    assert dummy.call_func(kern.prctl, kern.PR_SET_DUMPABLE_INIT_NS) == -errno.EPERM
    assert dummy.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 3

    dummy.call_func(os.setuid, 1)
    assert dummy.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 3

    assert dummy.call_func(kern.prctl, kern.PR_SET_DUMPABLE, 1) == 0
    assert dummy.call_func(kern.prctl, kern.PR_GET_DUMPABLE) == 1

    assert check_ptrace(dummy.pid) == 0
    assert probe.call_func(check_ptrace, 3) == 0

    if kern.prctl(kern.PR_TRANSLATE_PID, 1, 0, 0) == 1:
        assert kern.prctl(kern.PR_TRANSLATE_PID, dummy.pid, 0, probe.pid) == 3
        assert kern.prctl(kern.PR_TRANSLATE_PID, 3, probe.pid, 0) == dummy.pid
        assert kern.prctl(kern.PR_TRANSLATE_PID, 1, probe.pid, dummy.pid) == 1

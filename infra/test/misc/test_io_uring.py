import pytest
import os

try:
    import infra.kernel.test.misc.kern as kern
    import infra.kernel.test.misc.util as util
except ImportError:
    from . import kern
    from . import util


_MARKS = {
    '35fa71a030ca-test': [pytest.mark.skip(reason="infinite loop by design")],
    '232c93d07b74-test': [pytest.mark.xfail(reason="socket sigsegv", strict=False)],
    'a4c0b3decb33-test': [pytest.mark.skip(reason="infinite loop by design")],
    'd4ae271dfaae-test': [pytest.mark.xfail(not kern.kernel_in('5.6'), reason="bug in backlogged CQ ring fixed in d4ae271dfaae")],
    'accept': [pytest.mark.xfail(reason="socket sigsegv", strict=False)],
    'accept-link': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="IORING_OP_ACCEPT not implemented")],
    'accept-test': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="IORING_OP_ACCEPT not implemented")],
    'across-fork': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="depends on https://lore.kernel.org/io-uring/20191203025444.29344-1-axboe@kernel.dk/")],
    'close-opath': [pytest.mark.xfail(not kern.kernel_in('5.6'), reason="IORING_OP_CLOSE not implemented")],
    'connect': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="IORING_OP_CONNECT not implemented")],
    'cq-overflow-peek': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="backlogged CQ ring not implemented")],
    'lfs-openat': [pytest.mark.xfail(not kern.kernel_in('5.6'), reason="IORING_OP_OPENAT not implemented")],
    'lfs-openat-write': [pytest.mark.xfail(not kern.kernel_in('5.6'), reason="IORING_OP_OPENAT not implemented")],
    'link-timeout': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="IORING_OP_LINK_TIMEOUT not implemented")],
    'poll-cancel': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="depends on b0dd8a412699")],
    'poll-link': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="IORING_OP_LINK_TIMEOUT not implmeneted")],
    'read-write': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="read/write not implemented")],
    'submit-reuse': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="depends on https://lore.kernel.org/io-uring/20191203025444.29344-1-axboe@kernel.dk/", strict=False)],
    'link_drain': [pytest.mark.xfail(not kern.kernel_in('5.5'), reason="depends on drain behaivor implmented in 1b4a51b6d03d", strict=False)],
    'send_recvmsg': [pytest.mark.xfail(reason="ub: use of stack variable after function return", strict=False)],
    'socket-rw': [pytest.mark.xfail(reason="socket sigsegv", strict=False)],
    'file-register': [pytest.mark.xfail(reason="ub: dereferencing invalid address", strict=False)],
    'io-cancel': [pytest.mark.skip(reason="not fixed")],
    'defer': [pytest.mark.skip(reason="not fixed")],
    'sq-space_left': [pytest.mark.xfail(not kern.kernel_in('5.7'), reason="early submission fail implemented in 1d4240cc9e7bb")],
    'file-update': [pytest.mark.xfail(not kern.kernel_in('5.4.110-13'), reason="not implemented")],
    'open-close': [pytest.mark.xfail(not kern.kernel_in('5.4.110-13'), reason="EINVAL does not return on unknown opcode")],
    'openat2': [pytest.mark.xfail(not kern.kernel_in('5.4.110-13'), reason="EINVAL does not get returned on unknown opcode")],
    'splice': [pytest.mark.xfail(not kern.kernel_in('5.4.110-13'), reason="EINVAL does not get returned on unknown opcode")],
    'timeout': [pytest.mark.xfail(not kern.kernel_in('5.4.110-13'), reason="EINVAL does not get returned on unknown opcode")],
}


def _list_tests():
    tests_dir = util.arcadia_build_path('contrib/libs/liburing/test')
    for name in os.listdir(tests_dir):
        test_dir = os.path.join(tests_dir, name)
        if os.path.isdir(test_dir):
            test_bin = os.path.join(tests_dir, name, name)
            marks = _MARKS.get(name, [])
            yield pytest.param(test_bin, marks=marks)


def id_func(path):
    return os.path.relpath(path, util.arcadia_build_path(''))


def prepare_io_uring_perm(sysctl):
    # Newer kernels has restricted io_uring by default, enable it before test run
    if "kernel.io_uring_perm" in sysctl:
        sysctl["kernel.io_uring_perm"] = 1


@pytest.mark.skipif(not kern.kernel_in("5.4"), reason="not implemented")
@pytest.mark.skip(reason="no support for IORING_SETUP_CQSIZE flag used in fio-3.28")
def test_registerfiles(make_fio, make_temp_file, sysctl, logger):
    prepare_io_uring_perm(sysctl)
    f = make_temp_file(size=(1 << 27))  # 128MiB
    fio = make_fio(filename=f.name,
                   ioengine='io_uring',
                   bs='4k',
                   direct=1,
                   rw='randread',
                   time_based=1,
                   runtime=10,
                   group_reporting=1,
                   registerfiles=1,
                   fixedbufs=1,
                   numjobs=4,
                   iodepth=128,
                   logger=logger)
    job = fio.run()
    assert job and job['error'] == 0


@pytest.mark.skipif(not kern.kernel_in('5.4'), reason="not implemented")
@pytest.mark.parametrize('test_bin', list(_list_tests()), ids=id_func)
def test_liburing(test_bin, run, sysctl, logger):
    prepare_io_uring_perm(sysctl)
    run([test_bin], timeout=30)


@pytest.mark.skipif(not kern.kernel_in("5.4.134-19.1", "5.4.148-24"), reason='not implemented')
def test_perm_sysctl(run, sysctl, logger):
    tests_dir = util.arcadia_build_path('contrib/libs/liburing/test')
    test_bin = os.path.join(tests_dir, 'probe/probe')

    assert run([test_bin], check=False, timeout=30).returncode != 0
    prepare_io_uring_perm(sysctl)
    assert run([test_bin], check=False, timeout=30).returncode == 0

import pytest
import kern


@pytest.mark.skipif(not kern.module_exists('test_lockup'), reason='no module test_lockup')
def test_remount_s_umount(tmp_path_factory, run, dmesg, kernel_extra_modules):
    src = str(tmp_path_factory.mktemp('src'))
    dst = str(tmp_path_factory.mktemp('dst'))

    commands = [
        ['mount', '--bind', src, dst],
        ['mount', '--make-private', dst],
        ['mount', '-o', 'remount,bind,ro', dst],
        ['umount', dst],
    ]

    mod = kern.ModuleLoader('test_lockup', file_path=src, lock_sb_umount='Y', time_secs=10, state='S')

    # must not depends on sb->s_umount
    with mod:
        dmesg.match('test_lockup: lock rw_semaphore .*', timeout=10)
        for cmd in commands:
            with kern.Timer(max_time=1):
                run(cmd)
    dmesg.match('test_lockup: unlock rw_semaphore .*', timeout=10)


@pytest.mark.skipif(not kern.module_exists('test_lockup'), reason='no module test_lockup')
def test_remount_namespace_sem(tmp_path_factory, run, dmesg, kallsyms, kernel_extra_modules):
    src = str(tmp_path_factory.mktemp('src'))
    dst = str(tmp_path_factory.mktemp('dst'))

    commands = [
        ['mount', '--bind', src, dst],
        ['mount', '--make-private', dst],
        ['mount', '-o', 'remount,bind,ro', dst],
        ['umount', dst],
    ]

    mod = kern.ModuleLoader('test_lockup', lock_rwsem_ptr=kallsyms['namespace_sem'], time_secs=1, state='S')

    # must depends on namespace_sem
    for cmd in commands:
        with mod:
            dmesg.match('test_lockup: lock rw_semaphore .*', timeout=10)
            with kern.Timer(min_time=1):
                run(cmd)
        dmesg.match('test_lockup: unlock rw_semaphore .*', timeout=10)

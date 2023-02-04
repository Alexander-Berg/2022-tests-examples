from __future__ import unicode_literals
import os
import time
import utils


def test_coredump_probability_0(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '0')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'True')

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == '0'


def test_coredump_probability_100(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '100')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'False')

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == 'unlimited'


def test_always_coredump_false(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '100')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'False')
    # Sleep 2 seconds to make core mtime be greater than binary mtime
    time.sleep(2)
    cwd.join('test_coredumps_dir').ensure(dir=True)
    cwd.join('test_coredumps_dir', 'func_test_binary_core').write('1')

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == '0'


def test_always_coredump_true(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '100')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'True')

    cwd.join('test_coredumps_dir').ensure(dir=True)
    cwd.join('test_coredumps_dir', 'func_test_binary_core').write('1')

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == 'unlimited'


def test_coredumps_filemask_limit_reached(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '100')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'False')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredumps_filemask', '*')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredumps_count_limit', '1')
    cwd.join('test_coredumps_dir').ensure(dir=True)
    cwd.join('test_coredumps_dir', 'some-random-file-name').write('1')
    # Sleep 2 seconds to make binary mtime to be greater than core mtime
    time.sleep(2)
    # Bump binary mtime
    os.utime(cwd.join('func_test_binary').strpath, None)

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == '0'


def test_coredumps_filemask_limit_not_reached(cwd, ctl, patch_loop_conf, ctl_environment, request):
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredump_probability', '100')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'always_coredump', 'False')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredumps_filemask', '*')
    utils.set_loop_conf_parameter(cwd.join('loop.conf').strpath,
                                  'test_coredump_limits', 'coredumps_count_limit', '2')
    cwd.join('test_coredumps_dir').ensure(dir=True)
    cwd.join('test_coredumps_dir', 'some-random-file-name').write('1')
    # Sleep 2 seconds to make binary mtime to be greater than core mtime
    time.sleep(2)
    # Bump binary mtime
    os.utime(cwd.join('func_test_binary').strpath, None)

    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert cwd.join('result.txt').read().rstrip() == 'unlimited'

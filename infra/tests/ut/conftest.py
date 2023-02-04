import os
import subprocess

import pytest
import yatest.common


built_porto = None
test_dir = None


def run(args, **kwargs):
    print("+ '" + "' '".join(args) + "'", flush=True)
    return subprocess.check_call(args, **kwargs)


def run_test_part(number, test_dir_path):
    log = yatest.common.test_output_path('test_output.txt')
    cmd = ['ctest', '-V', '-R', '^part{}$'.format(number), '--output-log', log]
    print('Run {}'.format(" ".join(cmd)), flush=True)
    ret = subprocess.call(cmd, cwd=test_dir_path)
    if ret:
        subprocess.call(['tail', '-n100', '/var/log/portod.log'])
        pytest.fail('test part{} fail, see logs: {}'.format(number, log))


@pytest.fixture(scope='session')
def prepare_env():
    global built_porto
    global test_dir

    built_porto = yatest.common.binary_path('infra/porto/build/porto.tar.gz')
    test_dir = "/tmp/build_porto"

    run(['mkdir', test_dir])


@pytest.fixture(scope='session')
def prepare_porto(prepare_env):
    run(['tar', '--strip-components=2', '-xf', os.path.abspath(built_porto)], cwd=test_dir)
    run(['chmod', '0755', '.'], cwd=test_dir)


@pytest.fixture(scope='session', autouse=True)
def prepare_tests(prepare_porto):
    run(['cp', './portoctl', '/usr/sbin/portoctl'], cwd=test_dir)
    run(['./portod', 'start'], cwd=test_dir)
    # add layers with necessary packets baked in
    run(['./portoctl', 'layer', '-I', 'ubuntu-precise', yatest.common.runtime.work_path('ubuntu-precise.tgz')], cwd=test_dir)
    run(['./portoctl', 'layer', '-I', 'ubuntu-xenial', yatest.common.runtime.work_path('ubuntu-xenial.tar.zst')], cwd=test_dir)
    run(['./portoctl', 'layer', '-I', 'docker-xenial', yatest.common.runtime.work_path('docker-xenial.tar.gz')], cwd=test_dir)

    # add needed users
    run(['./scripts/prepare_test'], cwd=test_dir)

    # for test #17 devices
    run(['modprobe', 'brd'])

    # for test #25 net-sched
    run(['modprobe', 'dummy'])
    run(['ip', 'link', 'del', 'dummy0'])

    # restart porto to reload info about new users
    run(['./portod', 'stop'], cwd=test_dir)


@pytest.fixture()
def test_dir_path(scope='function'):
    return test_dir

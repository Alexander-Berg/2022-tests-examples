import glob
import logging
import os
import os.path
import pytest
import requests
import shutil
import socketserver
import stat
import time
import tempfile
import threading
import yatest.common
from yatest.common import network, execute, work_path

from .util import Checker, DNSResolverHTTPSAdapter

log = logging.getLogger('test')

TESTDATA_PATH = 'infra/awacs/awacslet/test/testdata'
PORTO_SOCKET = '/run/portod.socket'
CONF_LOCAL_PATH = 'Conf.local'


def prepare_conf_local(env={}, store=False):
    # Wrapper for environment variables. If store is set variables will
    # be stored in Conf.local file, a returned map will be empty. Otherwise
    # Conf.local will be empty and variables will be placed on to returned map
    cwd = work_path()
    if 'PWD' in env:
        cwd = env['PWD']

    cwd = os.path.join(cwd, CONF_LOCAL_PATH)

    try:
        os.unlink(cwd)
    except FileNotFoundError:
        pass

    if not store:
        return dict(env)

    with open(cwd, "w") as fd:
        for key, value in env.items():
            fd.write("ENV_{0}={1}\n".format(key, value))

    nenv = {}
    if 'PWD' in env:
        nenv['PWD'] = env['PWD']
    return nenv


def get_testdata(name):
    return yatest.common.source_path(os.path.join(TESTDATA_PATH, name))


def log_balancer_logs(logs_dir):
    log.debug('=== Balancer logs after test:')
    for path in glob.glob(os.path.join(logs_dir, 'current-*')):
        with open(path, 'r') as f:
            data = f.read()
            log.debug('* %s:', os.path.basename(path))
            log.debug(data)
            log.debug('')


@pytest.fixture
def instance(tmpdir):
    sdstub_path = yatest.common.binary_path('infra/awacs/awacslet/test/sdstub/sdstub')
    with network.PortManager() as port_manager:
        sd_port = port_manager.get_port()
        sdstub = execute([sdstub_path, str(sd_port)], wait=False)
        instance = Instance.setup(
            instance_dir=tmpdir,
            sd_port=sd_port,
            port_manager=port_manager,
            config_name='http_config.lua'
        )
        log.debug('instance.admin_port: %d', instance.admin_port)
        log.debug('instance.balancer_port: %d', instance.balancer_port)
        yield instance
        sdstub.kill()
    time.sleep(3)  # let logs be written and flushed
    log_balancer_logs(instance.logs_dir)


@pytest.fixture
def https_instance(tmpdir):
    sdstub_path = yatest.common.binary_path('infra/awacs/awacslet/test/sdstub/sdstub')
    with network.PortManager() as port_manager:
        sd_port = port_manager.get_port()
        sdstub = execute([sdstub_path, str(sd_port)], wait=False)
        instance = Instance.setup(
            instance_dir=tmpdir,
            sd_port=sd_port,
            port_manager=port_manager,
            config_name='https_config.lua'
        )
        log.debug('instance.admin_port: %d', instance.admin_port)
        log.debug('instance.balancer_port: %d', instance.balancer_port)
        yield instance
        sdstub.kill()
    time.sleep(3)  # let logs be written and flushed
    log_balancer_logs(instance.logs_dir)


class MockPortodHandler(socketserver.BaseRequestHandler):
    def handle(self):
        while True:
            recv = self.request.recv(1024)
            if b'absolute_name' in recv:
                self.request.sendall(b']\10\0\22\0\"P\nN/porto/ISS-AGENT--21902/21902_rtc_balancer_knoss_morda_testing_man_iwaKjTDUPjH\300>\245\275\266\205\6')
            elif b'cpu_limit' in recv:
                self.request.sendall(b'\27\10\0\22\0\"\n\n\0101.97989c\300>\245\275\266\205\6')
            else:
                try:
                    self.request.sendall(b'failed')
                except BrokenPipeError:
                    pass

            if self.server.ev.wait(0.1):
                break


class MockPortodServer(socketserver.ThreadingMixIn, socketserver.UnixStreamServer):
    pass


class Instance:
    LOCAL_DEV_SHM_DIR_PATH = './dev-shm'
    LOCAL_LOGS_DIR_PATH = './place/db/www/logs'

    def __init__(self, instance_dir, logs_dir, dev_shm_dir, config_lua, admin_port, balancer_port, sd_port, env):
        self.dir = instance_dir
        self.logs_dir = logs_dir
        self.config_lua = config_lua
        self.hostname = 'localhost'
        self.admin_port = admin_port
        self.balancer_port = balancer_port
        self.sd_port = sd_port
        self.env = env

        self.logs_dir = logs_dir
        self.dev_shm_dir = dev_shm_dir
        self.awacslet_path = os.path.join(self.dir, 'awacslet')

    def read_config_lua(self):
        with open(os.path.join(self.dir, 'config.lua'), 'r') as f:
            return f.read()

    def write_config_lua(self, lua):
        with open(os.path.join(self.dir, 'config.lua'), 'w') as f:
            f.write(lua)

    @classmethod
    def setup(cls, instance_dir, port_manager, config_name, sd_port):
        assert config_name in ('http_config.lua', 'https_config.lua')
        awacslet_path = yatest.common.binary_path('infra/awacs/awacslet/cmd/awacslet/awacslet')
        balancer_path = yatest.common.binary_path('balancer/daemons/balancer/balancer')
        awacslet_get_workers_provider_lua_path = get_testdata('awacslet_get_workers_provider.lua')

        shutil.copy2(awacslet_path, os.path.join(instance_dir, 'awacslet'))
        shutil.copy2(balancer_path, os.path.join(instance_dir, 'balancer'))
        shutil.copy2(awacslet_get_workers_provider_lua_path,
                     os.path.join(instance_dir, 'awacslet_get_workers_provider.lua'))
        dev_shm_dir_path = os.path.join(instance_dir, cls.LOCAL_DEV_SHM_DIR_PATH)
        os.makedirs(dev_shm_dir_path)
        logs_dir_path = os.path.join(instance_dir, cls.LOCAL_LOGS_DIR_PATH)
        os.makedirs(logs_dir_path)

        balancer_port = port_manager.get_port()
        admin_port = port_manager.get_port()

        http_config_lua_path = get_testdata(config_name)
        with open(http_config_lua_path, 'r') as f:
            config_lua = (f.read()
                          .replace('/dev/shm', cls.LOCAL_DEV_SHM_DIR_PATH)
                          .replace('/place/db/www/logs', cls.LOCAL_LOGS_DIR_PATH)
                          .replace('__BALANCER_PORT__', str(balancer_port))
                          .replace('__SD_PORT__', str(sd_port)))
        with open(os.path.join(instance_dir, 'config.lua'), 'w') as f:
            log.debug(config_lua)
            f.write(config_lua)

        env = {
            'PATH': instance_dir,
            'AWACSLET_DEV_SHM_DIR_PATH': cls.LOCAL_DEV_SHM_DIR_PATH,
            'AWACSLET_LOGS_DIR_PATH': cls.LOCAL_LOGS_DIR_PATH,
            'AWACSLET_GRACEFUL_SHUTDOWN_TIMEOUT': '10s',
            'AWACSLET_USE_SUDO_FOR_SHAWSHANK': '0',

            'AWACS_X': '31337',
            'AWACS_Y': '31338',

            'CPU_LIMIT': '1c',
            'BSCONFIG_IPORT': str(admin_port),
            'BSCONFIG_IDIR': '.',
            'a_dc': 'sas',
        }

        return Instance(instance_dir=instance_dir,
                        logs_dir=logs_dir_path,
                        dev_shm_dir=dev_shm_dir_path,
                        config_lua=config_lua,
                        admin_port=admin_port,
                        balancer_port=balancer_port,
                        sd_port=sd_port,
                        env=env)


@pytest.mark.parametrize('use_conf_local', [
    False,
    True
], ids=[
    'ordinary',
    'conf_local'
])
def test_initialize(tmpdir, use_conf_local):
    cwd = str(tmpdir)
    awacslet_path = yatest.common.binary_path('infra/awacs/awacslet/cmd/awacslet/awacslet')

    global_env = {'PWD': cwd}
    env = prepare_conf_local(global_env, use_conf_local)

    global_env['BSCONFIG_IPORT'] = '80'
    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=False, wait=True, cwd=cwd)
    assert 'failed to initialize: failed to read "BSCONFIG_IPORT" env var' in str(p.stderr)
    assert p.returncode == 1

    global_env['BSCONFIG_IPORT'] = '80'
    env = prepare_conf_local(global_env, use_conf_local)
    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=False, wait=True, cwd=cwd, env=env)
    assert p.returncode == 1
    assert 'failed to initialize: failed to read "BSCONFIG_IDIR" env var' in str(p.stderr)

    global_env['BSCONFIG_IDIR'] = '.'
    env = prepare_conf_local(global_env, use_conf_local)
    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=False, wait=True, cwd=cwd, env=env)
    assert p.returncode == 1
    assert 'failed to initialize: failed to read "a_dc" env var' in str(p.stderr)

    global_env['a_dc'] = 'sas'
    global_env['PORTO_SOCKET'] = '/var/socket/not_exists'
    env = prepare_conf_local(global_env, use_conf_local)
    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=False, wait=True, cwd=cwd, env=env)
    assert p.returncode == 1
    assert 'failed to get root container name for current porto container' in str(p.stderr)

    global_env['CPU_LIMIT'] = '123'
    env = prepare_conf_local(global_env, use_conf_local)
    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=False, wait=True, cwd=cwd, env=env)
    assert p.returncode == 1
    assert ('failed to initialize: failed to get workers count: '
            'CPU_LIMIT env var value does not have suffix "c"') in str(p.stderr)

    global_env['CPU_LIMIT'] = '2c'
    env = prepare_conf_local(global_env, use_conf_local)
    p = execute([awacslet_path], check_exit_code=False, wait=True, cwd=cwd, env=env)
    assert p.returncode == 1
    assert ('expected command: "version", "start", "prepare", "notify", '
            '"status", "stop", "reopenlog", "get_workers_count"') in str(p.stderr)

    p = execute([awacslet_path, 'get_workers_count'], check_exit_code=True, wait=True, cwd=cwd, env=env)
    assert b'2' == p.stdout

    # The server instance uses the full path to create a socket file.
    # cwd parameter and PWD environment variable provide a test working
    # directory for the spawned process. A full path in X variable binary
    # will raise 'AF_UNIX path too long' because of max path length 104 symbols.
    with tempfile.TemporaryDirectory(suffix='portod_test') as portod_test_dir:
        portod_socket = os.path.join(portod_test_dir, 'portod.socket')
        server = MockPortodServer(portod_socket, MockPortodHandler)
        with server:
            server.ev = threading.Event()
            server_thread = threading.Thread(target=server.serve_forever)
            server_thread.daemon = True
            server_thread.start()

            del global_env['CPU_LIMIT']
            global_env['PORTO_SOCKET'] = portod_socket
            env = prepare_conf_local(global_env, use_conf_local)
            p = execute([awacslet_path, 'get_workers_count'], check_exit_code=True, wait=True, cwd=cwd, env=env)
            assert b'2' == p.stdout

            server.ev.set()
            server.shutdown()


def test_basics(instance):
    awacslet_path = instance.awacslet_path

    execute_kwargs = {
        'cwd': instance.dir,
    }
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    # Make sure we stashed env variables whose names have "AWACS" prefix (see SWAT-5502)
    with open(os.path.join(instance.dev_shm_dir, 'env_save'), 'r') as f:
        assert f.read().strip() == 'AWACS_X=31337\nAWACS_Y=31338'

    # Make sure we can run prepare twice and everything is OK
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    awacslet_start = execute([awacslet_path, 'start', 'balancer'], check_exit_code=False, wait=False, env=instance.env,
                             **execute_kwargs)
    time.sleep(3)
    assert awacslet_start.running

    for a in Checker(timeout=10):
        with a:
            execute([awacslet_path, 'status'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    resp = requests.get('http://{}:{}/helloworld'.format(instance.hostname, instance.balancer_port))
    assert resp.status_code == 200
    assert resp.text == 'Hey! I am powered by awacslet :) My AWACS_X = 31337 and AWACS_Y = 31338'

    # Change config.lua...
    lua = instance.read_config_lua()
    instance.write_config_lua(lua.replace(':)', ':-)'))

    # ...and also emulate feature-bug described in https://st.yandex-team.ru/SWAT-5502:
    # make notify hook unsee env var required by balancer lua configuration
    notify_env = dict(instance.env)
    del notify_env['AWACS_X']
    del notify_env['AWACS_Y']
    execute([awacslet_path, 'notify', 'notify_action', '!config.lua'],
            check_exit_code=True, wait=True, env=notify_env, **execute_kwargs)

    # Make sure we see the resulf of reloaded config and that AWACS_X (which is read from env var)
    # is still there
    resp = requests.get('http://{}:{}/helloworld'.format(instance.hostname, instance.balancer_port))
    assert resp.status_code == 200
    assert resp.text == 'Hey! I am powered by awacslet :-) My AWACS_X = 31337 and AWACS_Y = 31338'

    # Call stop hook
    execute([awacslet_path, 'stop'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    # Wait for balancer to stop...
    awacslet_start.wait(check_exit_code=True, timeout=10)

    awacslet_status = execute([awacslet_path, 'status'], check_exit_code=False, wait=True, env=instance.env,
                              **execute_kwargs)
    assert awacslet_status.returncode == 1

    assert os.path.exists(os.path.join(instance.dir, 'sd_cache'))


def test_https(https_instance):
    instance = https_instance

    awacs_dir_path = os.path.join(instance.dir, 'awacs')
    os.mkdir(awacs_dir_path)
    instance.env['AWACSLET_AWACS_DIR_PATH'] = awacs_dir_path

    awacslet_path = instance.awacslet_path
    secrets_1 = os.path.join(instance.dir, 'secrets_x')
    os.makedirs(secrets_1)
    shutil.copy2(get_testdata('secrets.tgz'), secrets_1)

    execute_kwargs = {
        'cwd': instance.dir,
    }
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)
    awacslet_start = execute([awacslet_path, 'start', 'balancer'], check_exit_code=False, wait=False, env=instance.env,
                             **execute_kwargs)
    time.sleep(3)
    assert awacslet_start.running

    for a in Checker(timeout=10):
        with a:
            execute([awacslet_path, 'status'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    base_url = 'https://{}:{}/'.format(instance.hostname, instance.balancer_port)
    s = requests.Session()
    s.mount(base_url, DNSResolverHTTPSAdapter('romanovich.awacslet.yandex.net', '::1'))
    resp = s.get('https://{}:{}/helloworld'.format(instance.hostname, instance.balancer_port), headers={
        'Host': 'romanovich.awacslet.yandex.net',
    }, verify=get_testdata('rootCA.pem'))
    assert resp.status_code == 200
    assert resp.text == 'Hey! I am powered by awacslet!'

    execute([awacslet_path, 'stop'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)
    awacslet_start.wait(check_exit_code=True, timeout=10)

    awacslet_status = execute([awacslet_path, 'status'], check_exit_code=False, wait=True, env=instance.env,
                              **execute_kwargs)
    assert awacslet_status.returncode == 1

    assert not os.path.exists(os.path.join(instance.dir, 'sd_cache'))
    assert os.path.exists(os.path.join(awacs_dir_path, 'sd_cache'))


def test_prepare_w_one_cert(instance):
    awacslet_path = instance.awacslet_path
    secrets_1 = os.path.join(instance.dir, 'secrets_x')
    os.makedirs(secrets_1)
    shutil.copy2(get_testdata('secrets.tgz'), secrets_1)

    execute_kwargs = {
        'cwd': instance.dir,
    }
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    certs_dir = os.path.join(instance.dev_shm_dir, 'balancer')
    assert os.path.exists(certs_dir)
    assert set(os.listdir(certs_dir)) == {'allCAs-_.awacslet.yandex.net.pem', 'priv'}
    priv_dir = os.path.join(certs_dir, 'priv')
    assert set(os.listdir(priv_dir)) == {'1st._.awacslet.yandex.net.key',
                                         '2nd._.awacslet.yandex.net.key',
                                         '3rd._.awacslet.yandex.net.key',
                                         '_.awacslet.yandex.net.pem'}


def test_prepare_w_conflicting_certs(instance):
    secrets_1 = os.path.join(instance.dir, 'secrets_x')
    os.makedirs(secrets_1)
    shutil.copy2(get_testdata('secrets.tgz'), os.path.join(secrets_1, 'secrets.tgz'))

    awacslet_path = instance.awacslet_path
    secrets_2 = os.path.join(instance.dir, 'secrets_y')
    os.makedirs(secrets_2)
    shutil.copy2(get_testdata('secrets-2.tgz'), os.path.join(secrets_2, 'secrets.tgz'))

    execute_kwargs = {
        'cwd': instance.dir,
    }
    awacslet_prepare = execute([awacslet_path, 'prepare'], check_exit_code=False, wait=True,
                               env=instance.env, **execute_kwargs)
    assert awacslet_prepare.returncode == 1
    assert 'failed to prepare certs: failed to dump cert secrets_y/secrets.tgz' in str(awacslet_prepare.stderr)


def test_start_w_incorrect_config(instance):
    awacslet_path = instance.awacslet_path

    execute_kwargs = {
        'cwd': instance.dir,
    }
    # Make config.lua invalid
    lua = instance.read_config_lua()
    instance.write_config_lua(lua.replace('status = 204', 'status = -1'))

    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    awacslet_start = execute([awacslet_path, 'start', 'balancer'], check_exit_code=False, wait=True, env=instance.env,
                             **execute_kwargs)
    assert awacslet_start.returncode == 1
    assert 'Unexpected symbol "-" at pos 0 in string "-1"' in str(awacslet_start.stderr)
    assert 'start failed: balancer config test failed' in str(awacslet_start.stderr)


def test_reload_w_incorrect_config(instance):
    awacslet_path = instance.awacslet_path

    execute_kwargs = {
        'cwd': instance.dir,
    }
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    awacslet_start = execute([awacslet_path, 'start', 'balancer'], check_exit_code=False, wait=False, env=instance.env,
                             **execute_kwargs)
    time.sleep(3)
    assert awacslet_start.running

    # Make config.lua invalid
    lua = instance.read_config_lua()
    instance.write_config_lua(lua.replace('status = 204', 'status = -1'))

    awacslet_notify = execute([awacslet_path, 'notify', 'notify_action', '!config.lua'],
                              check_exit_code=False, wait=True, env=instance.env, **execute_kwargs)
    assert awacslet_notify.returncode == 1
    assert 'Unexpected symbol "-" at pos 0 in string "-1"' in str(awacslet_notify.stderr)
    assert 'notify failed: balancer config test failed' in str(awacslet_notify.stderr)


def test_shawshank(instance):
    awacslet_path = os.path.join(instance.dir, 'awacslet')
    spec_path = os.path.join(instance.dir, 'awacs-balancer-container-spec.pb.json')
    with open(spec_path, 'w') as f:
        f.write('{}')

    execute_kwargs = {
        'cwd': instance.dir,
    }
    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)

    with open(spec_path, 'w') as f:
        f.write('{"requirements": XXX}')

    p = execute([awacslet_path, 'prepare'], check_exit_code=False, wait=True, env=instance.env, **execute_kwargs)
    assert ('prepare failed: '
            'failed to read awacs-balancer-container-spec.pb.json: '
            'failed to unmarshal awacs-balancer-container-spec.pb.json') in str(p.stderr)

    with open(spec_path, 'w') as f:
        f.write('{"requirements": ["upyachka.exe"]}')

    p = execute([awacslet_path, 'prepare'], check_exit_code=False, wait=True, env=instance.env, **execute_kwargs)
    assert ('prepare failed: '
            'failed to read awacs-balancer-container-spec.pb.json: '
            'failed to unmarshal awacs-balancer-container-spec.pb.json') in str(p.stderr)

    with open(spec_path, 'w') as f:
        f.write('{"requirements": [{"name": "upyachka.exe"}]}')

    p = execute([awacslet_path, 'prepare'], check_exit_code=False, wait=True, env=instance.env, **execute_kwargs)
    assert 'prepare failed: unknown requirement "upyachka.exe"' in str(p.stderr)

    with open(spec_path, 'w') as f:
        f.write('{"requirements": [{"name": "shawshank"}]}')

    p = execute([awacslet_path, 'prepare'], check_exit_code=False, wait=True, env=instance.env, **execute_kwargs)
    assert ('prepare failed: failed to locate shawshank binary: '
            'exec: "shawshank": executable file not found in $PATH') in str(p.stderr)

    shawshank_path = os.path.join(instance.dir, 'shawshank')
    with open(shawshank_path, 'w') as f:
        f.write('#!/bin/bash\necho "1" > ./shawshank-has-been-executed')
    st = os.stat(shawshank_path)
    os.chmod(shawshank_path, st.st_mode | stat.S_IEXEC)

    execute([awacslet_path, 'prepare'], check_exit_code=True, wait=True, env=instance.env, **execute_kwargs)
    assert os.path.exists(os.path.join(instance.dir, 'shawshank-has-been-executed'))

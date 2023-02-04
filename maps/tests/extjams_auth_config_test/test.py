import ipaddress
import logging
import os
import pytest
import re
import requests
import tarfile
import tempfile
import time

import yatest.common as yc
from yatest.common import network


logger = logging.getLogger('test_logger')


class Nginx:
    def __init__(self, configuration, ping_port):
        self.root_dir: str = yc.runtime.work_path('dir/')
        nginx_tar = tarfile.open(os.path.join(self.root_dir, 'pack.tar.gz'), 'r:gz')
        nginx_tar.extractall(self.root_dir)

        self.executable: str = os.path.join(self.root_dir, 'nginx')

        os.makedirs(os.path.join(self.root_dir, 'conf.d'))
        self.configuration_dir: str = os.path.join(self.root_dir, 'conf.d')
        self.nginx_conf = os.path.join(self.root_dir, 'nginx.conf')
        self.access_log_file = yc.output_path('access-log.txt')
        self.error_log_file = yc.output_path('error-log.txt')
        self.pid_file = os.path.join(self.root_dir, 'pid')

        with open(yc.source_path('maps/analyzer/services/jams_analyzer/modules/dispatcher/tests/extjams_auth_config_test/data/nginx.conf')) as nginx_conf_file:
            nginx_conf = nginx_conf_file.read() \
                .replace('#PID_FILE#', self.pid_file) \
                .replace('#ACCESS_LOG_PATH#', self.access_log_file) \
                .replace('#ERROR_LOG_PATH#', self.error_log_file) \
                .replace('#CLIENT_BODY#', tempfile.mkdtemp(prefix=self.root_dir)) \
                .replace('#PROXY#', tempfile.mkdtemp(prefix=self.root_dir)) \
                .replace('#FASTCGI#', tempfile.mkdtemp(prefix=self.root_dir)) \
                .replace('#UWSGI#', tempfile.mkdtemp(prefix=self.root_dir)) \
                .replace('#SCGI#', tempfile.mkdtemp(prefix=self.root_dir)) \
                .replace('#PING_PORT#', str(ping_port))
            with open(self.nginx_conf, 'w', encoding='utf-8') as nginx_conf_out:
                nginx_conf_out.write(nginx_conf)

        self.ping_port = ping_port
        self.test_conf = configuration['text']
        self.ports = configuration['ports']
        with open(os.path.join(self.configuration_dir, 'test.conf'), 'w', encoding='utf-8') as test_configuration_file:
            test_configuration_file.write(self.test_conf)

        self.process = None

    def start(self):
        self.process = yc.execute([self.executable, '-c', self.nginx_conf, '-p', self.root_dir], wait=False,
                                  check_exit_code=True)
        logger.info('Waiting for nginx to start ...')
        i = 0
        while i < 10:
            i += 1
            time.sleep(1)
            try:
                response = requests.get(f'http://localhost:{self.ping_port}/ping')
                if response.status_code == 200:
                    logger.info(f'Nginx ping OK: {response}')
                    return
            except Exception as err:
                response = err
            logger.info(f'Nginx ping ERROR: {response}')
        raise Exception('Nginx failed to start!')

    def stop(self):
        self.process.kill()


@pytest.fixture(scope='class')
def port_manager(request):
    """
    Prepares ports
    :param request:
    :return:
    """
    port_manager = network.PortManager()
    request.addfinalizer(port_manager.release)
    return port_manager


@pytest.fixture(scope='class')
def nginx(configuration, port_manager):
    """
    Starts nginx process
    :param ports:
    :return:
    """
    nginx = Nginx(configuration=configuration, ping_port=port_manager.get_port())
    nginx.start()
    return nginx


source = yc.source_path('maps/analyzer/services/jams_analyzer/docker/install/etc/nginx/auth/extjams.auth')
conf_file = open(source, 'r').read()


def test_same_ip():
    lines = conf_file.split('\n')
    ip_addresses = []
    ok = True
    for line in lines:
        res = re.match(r'\s*allow\s+[a-z0-9./:]+;\s*#*', line)
        if res is not None:
            addr = re.search(r'[a-z0-9./:]+;', line).group(0)[:-1]
            try:
                ip_addr = ipaddress.IPv6Network(addr)
            except:
                if addr.find('/') != -1:
                    net = int(re.search(r'/[0-9]*', addr).group(0)[1:])
                    addr = re.search(r'[0-9.]*/', addr).group(0)[:-1]
                else:
                    net = 32
                ip_addr = ipaddress.IPv6Network('::ffff:{:02x}{:02x}:{:02x}{:02x}/{}'.format(*list(map(int, addr.split('.'))), net + 96))
            for ip in ip_addresses:
                if ip_addr.overlaps(ip[0]):
                    ok = False
                    logger.info('Same ip or one network is a subnetwork of other')
                    logger.info(f'{addr} {ip[1]}')
            ip_addresses.append([ip_addr, addr])
    assert ok, 'Same ip detected. Check logs'


@pytest.fixture(scope='class')
def configuration(port_manager):
    '''
    Provides nginx configuration for test
    :return: ports and configuration string
    '''
    port_1 = port_manager.get_port()
    text = f'''
    server {{
        listen 127.0.0.1:{port_1};
        listen [::1]:{port_1};
        {conf_file}
        return 204;
    }}
    '''
    return {
        'text': text,
        'ports': {
            'port_1': port_1
        }
    }


def test_nginx_configuration(nginx: Nginx):
    port = nginx.ports['port_1']
    response = requests.get(f'http://localhost:{port}')
    assert response.status_code == 204
    logger.info(response)

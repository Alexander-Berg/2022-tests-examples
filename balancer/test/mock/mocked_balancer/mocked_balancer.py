import json
import os
import requests
import subprocess
import time
from yatest.common import network
from yatest.common import binary_path
from yatest.common import source_path
from yatest.common import test_output_path

from balancer.test.mock.mock_config_variables.mock_config_variables import generate_mock_data
from balancer.test.mock.mock_sd import MockSD


BALANCER_STDERR_FILENAME = "balancer_stderr.txt"
BALANCER_STDOUT_FILENAME = "balancer_stdout.txt"


class BalancerWithMockedConfig(object):
    def ping(self, address):
        url = "{addr}".format(addr=address)
        resp = requests.get(url, timeout=0.1)
        if resp.status_code == 200:
            ready, total = resp.content.strip().split('/')
            self._tries.append([ready, total])
            return ready == total
        return False

    def __init__(
        self,
        subnet,
        no_start=True,
        config_file_path=None,
        balancer_path=None,
        certs_path=None,
        limit_backends_instances=None,
        pytest_mod=True
    ):
        self._tries = []
        self._pm = network.PortManager()
        self._config_file_path = config_file_path
        if config_file_path is None:
            configs = os.listdir(binary_path('balancer/config/build'))
            if len(configs) != 1:
                raise Exception('Expected one config, got {}'.format(configs))
            self._project = configs[0]
            self._config_file_path = binary_path('balancer/config/build/{cfg}/{cfg}.cfg'.format(cfg=configs[0]))
        self._mocked_data, self._sd_backends = generate_mock_data(self._config_file_path, self._pm, subnet, limit_backends_instances)
        self._pytest_mod = pytest_mod
        self._proc = None

        if pytest_mod:
            self.weights_dir = test_output_path()
            self.mock_path = test_output_path('mock.json')
            self.work_dir = test_output_path()
            self.balancer_path = binary_path('balancer/daemons/balancer/balancer')
        else:
            self.weights_dir = os.path.abspath('.')
            self.mock_path = os.path.abspath('mock.json')
            self.work_dir = os.path.abspath('.')
            self.balancer_path = balancer_path

        with open(self.mock_path, 'w') as fd:
            json.dump(self._mocked_data, fd, indent=4)

        if certs_path is None:
            certs_path = source_path('balancer/test/plugin/certs/data')

        self._sd_port = self._pm.get_port()
        self._sd_mock = None

        self._command = [
            self.balancer_path,
            self._config_file_path,
            "-V", "WeightsDir={}".format(self.weights_dir),
            "-V", "LogDir={}".format(self.work_dir),
            "-V", "instance_state_directory={}/balancer-state".format(self.work_dir),
            "-V", "yandex_public_cert={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_private_cert={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_any_custom_yandex_rsa_public={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_any_custom_yandex_rsa_private={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_any_custom_afisha_rsa_public={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_any_custom_afisha_rsa_private={certs}/default.key".format(certs=certs_path),
            "-V", "public_any_custom_other_rsa={certs}/default.crt".format(certs=certs_path),
            "-V", "private_any_custom_other_rsa={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_eu_public={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_eu_private={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_fi_public={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_fi_private={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_pl_public={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_pl_private={certs}/default.key".format(certs=certs_path),
            "-V", "yandex_secondary_cert_certum={certs}/default.crt".format(certs=certs_path),
            "-V", "yandex_secondary_priv_certum={certs}/default.key".format(certs=certs_path),
            "-V", "ClickDaemonKeys={certs}/clickdaemon.keys".format(certs=certs_path),
            "-V", "ClickDaemonJsonKeys={certs}/clickdaemon.json_keys".format(certs=certs_path),
            "-V", "globals_replace_map={}".format(self.mock_path),
            "-V", "sd_cache={}/sd_cache".format(self.work_dir),
            "-V", "sd_host=127.0.0.1", "-V", "sd_port={}".format(self._sd_port),
            "-V", "workers=2",
            "-V", "start_without_special_children=true",
            "-V", "coro_pool_stacks_per_chunk=256",
            "-C", "1048576",
        ]

        if not no_start:
            self.start()

    def _start_sd(self):
        backends = {}

        for backend in self._sd_backends:
            backends[(backend[0], backend[1])] = [
                {
                    'ip': backend_config['cached_ip'],
                    'port': backend_config['port'],
                } for backend_config in self._mocked_data['backends_{}#{}'.format(backend[0], backend[1])]
            ]

        self._sd_mock = MockSD(self._sd_port, backends)

    def _stop_sd(self):
        if not self._sd_mock:
            return

        self._sd_mock.stop()
        self._sd_mock = None

    def _get_file_path(self, filename):
        if self._pytest_mod:
            return test_output_path(filename)
        else:
            return filename

    def _get_formatted_stdout_stderr(self):
        stderr = ""
        stdout = ""

        try:
            with open(self._get_file_path(BALANCER_STDERR_FILENAME)) as f:
                stderr = "".join(f.readlines()[-10:])
        except IOError:
            pass

        try:
            with open(self._get_file_path(BALANCER_STDOUT_FILENAME)) as f:
                stdout = "".join(f.readlines()[-10:])
        except IOError:
            pass

        res = ""
        if len(stderr) > 0:
            res += "== stderr [last 10 lines] ==\n" + stderr

        if len(stdout) > 0:
            if len(res) > 0:
                res += "\n\n"
            res += "== stdout [last 10 lines] ==\n" + stdout

        return res

    def start(self, pytest_mod=True):
        self._start_sd()

        addr = self._mocked_data["instance_admin_addrs"][0]
        cmd_path = self._get_file_path("balancer_start.sh")
        cfg_file_path = self._get_file_path("balancer_{}.cfg".format(addr["port"]))

        with open(cmd_path, "w") as f:
            f.write(" ".join(self._command))

        with open(cfg_file_path, 'w') as cfg_file:
            cfg_file.write(open(self._config_file_path, 'r').read())

        with open(self._get_file_path(BALANCER_STDERR_FILENAME), "w") as stderr:
            with open(self._get_file_path(BALANCER_STDOUT_FILENAME), "w") as stdout:
                self._proc = subprocess.Popen(
                    args=self._command,
                    stdout=stdout,
                    stderr=stderr,
                )

        time.sleep(1)

        tries = 200
        for i in xrange(tries):
            if self._proc.poll() is not None:
                raise Exception("Balancer crashed with exit code {}, stderr = {}".format(
                    self._proc.returncode, self._get_formatted_stdout_stderr()
                ))

            try:
                time.sleep(1)
                if self.ping("http://{host}:{port}/admin?action=dbg_workers_ready".format(
                    host=addr['ip'],
                    port=addr['port']
                )):
                    break
            except requests.exceptions.ConnectionError:
                continue
            except requests.exceptions.Timeout:
                continue
        else:
            if self._proc.poll() is None:
                self._proc.kill()
                self._proc.wait()
            raise Exception(
                "Balancer failed to start after {} tries. Exit code = {}, attempts = {}, stderr = {}".format(
                    tries,
                    self._proc.returncode,
                    self._tries,
                    self._get_formatted_stdout_stderr()
                ))

    def stop(self):
        self._stop_sd()

        if not self._proc:
            return

        addr = self._mocked_data['instance_admin_addrs'][0]
        try:
            resp = requests.get("http://{host}:{port}/admin?action=shutdown".format(
                host=addr['ip'],
                port=addr['port']
            ))
            assert resp.status_code == 200
            shutdown_successful = True
        except requests.exceptions.RequestException:
            shutdown_successful = False

        self._proc.wait()

        if not shutdown_successful or \
                self._proc.returncode != 0 or \
                os.path.getsize(self._get_file_path(BALANCER_STDOUT_FILENAME)) > 0 or \
                os.path.getsize(self._get_file_path(BALANCER_STDERR_FILENAME)) > 0:
            raise Exception('Shutdown successful = {}, exit code = {}\n{}'.format(
                shutdown_successful,
                self._proc.returncode,
                self._get_formatted_stdout_stderr()
            ))

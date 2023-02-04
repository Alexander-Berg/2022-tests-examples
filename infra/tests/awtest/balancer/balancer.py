from __future__ import absolute_import

import contextlib
import errno
import glob
import shutil
import tempfile

import os
import pexpect
import psutil
import ptyprocess
import pytest
import requests
import six
import time

from awacs import yamlparser
from awacs.model.balancer import generator
from awacs.model.balancer.vector import BalancerVersion, DomainVersion, UpstreamVersion, BackendVersion, CertVersion
from awacs.wrappers.base import Holder, ANY_MODULE, ValidationCtx
from awacs.wrappers import rps_limiter_settings
from awtest import t
from awtest.core import wait_until_passes
from awtest.https_adapter import get_https_session
from awtest.mocks.ports import MAGIC_SDSTUB_PORT, MAGIC_HTTP_PORT, MAGIC_HTTPS_PORT
from awtest.network import is_ipv4_only, get_local_ip, get_free_port, worker_id_to_offset
from infra.awacs.proto import model_pb2, modules_pb2


ROOT_CA_PEM = u'awtest/balancer/config/certs/rootCA.pem'
IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ


class Balancer(object):
    BASE_PORT = 16000
    WORKER_RANGE = 200
    WORKER_SEQ = 0

    def __init__(self, ctx, tag, balancer_executable_path, namespace_id, fixture_dir, sd_port=8080):
        self.config_dir = t(u'awtest/balancer/config/')
        self.config_tag = tag
        self.namespace_id = namespace_id
        namespace_pb = model_pb2.Namespace()
        namespace_pb.meta.id = namespace_id
        self.namespace_pb = namespace_pb
        self.balancer_executable_path = balancer_executable_path
        self.fixture_dir = fixture_dir
        self.balancer_id = u'balancer'
        self.balancer_version = None
        self.balancer_spec_pb = None
        self.domain_spec_pbs = {}
        self.upstream_spec_pbs = {}
        self.backend_spec_pbs = {}
        self.cert_spec_pbs = {}
        self.ctx = ctx

        self.env_dir = os.path.abspath(os.path.join(self.config_dir, u'env'))
        IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ
        if IS_ARCADIA:
            from yatest import common
            from infra.swatlib.logutil import rndstr
            writable_env_dir = common.output_path('env' + ((self.config_tag or '') + rndstr()))
            shutil.copytree(self.env_dir, writable_env_dir)
            self.env_dir = writable_env_dir

        self.certs_dir = os.path.join(self.config_dir, u'certs')
        self.root_ca_pem = os.path.join(self.certs_dir, u'rootCA.pem')
        self.sd_port = sd_port
        self.http_port = None
        self.https_port = None
        self.access_log_path = None

        self._prepare()

    def _prepare(self):
        if not os.path.exists(self.balancer_executable_path):
            pytest.fail('{} does not exist'.format(self.balancer_executable_path))

        self.read_balancer_config(os.path.join(self.config_dir, u'balancer.yml'))
        self.read_domain_configs()
        self.read_upstream_configs(os.path.join(self.config_dir, u'upstreams'))
        self.read_backend_configs()

        lua = self.render()
        with open(os.path.join(self.config_dir, u'config.lua')) as f:
            expected_lua = f.read()

        new_lua_path = os.path.join(self.config_dir, u'config.lua.new')
        if lua != expected_lua:
            with open(new_lua_path, u'w') as f:
                f.write(lua)
            raise AssertionError(u'Base balancer lua does not match expected config')
        else:
            if os.path.exists(new_lua_path):
                os.remove(new_lua_path)

    @staticmethod
    def parse(yml, cls=modules_pb2.Holder):
        return yamlparser.parse(cls, yml, ensure_ascii=True)

    def read_balancer_config(self, balancer_yml_path):
        self.balancer_version = BalancerVersion(
            ctime=u'2018-02-02t09:27:31.216017z',
            balancer_id=(self.namespace_id, self.balancer_id),
            version=u'1')
        with open(balancer_yml_path, u'r') as f:
            yaml = f.read()
            self.balancer_spec_pb = model_pb2.BalancerSpec()
            self.balancer_spec_pb.type = model_pb2.YANDEX_BALANCER
            self.balancer_spec_pb.yandex_balancer.yaml = yaml
            self.balancer_spec_pb.yandex_balancer.config.CopyFrom(self.parse(yaml))

    def read_domain_configs(self):
        for upstream_id in (u'flat', u'by_dc'):
            domain_spec_pb = model_pb2.DomainSpec()
            domain_spec_pb.type = model_pb2.YANDEX_BALANCER
            config_pb = domain_spec_pb.yandex_balancer.config
            config_pb.protocol = config_pb.HTTP_ONLY
            config_pb.fqdns.append(u'{}.easy-mode.yandex.net'.format(upstream_id))
            config_pb.include_upstreams.filter.id = upstream_id
            domain_version = DomainVersion(
                ctime=u'2018-02-02t09:27:31.216017z',
                domain_id=(self.namespace_id, u'{}.easy-mode.yandex.net'.format(upstream_id)),
                version=u'1',
                deleted=False,
                incomplete=False)
            self.domain_spec_pbs[domain_version] = domain_spec_pb

    def read_upstream_configs(self, upstreams_dir_path):
        upstream_pbs = {}
        for yml_path in glob.iglob(os.path.join(upstreams_dir_path, u'*.yml')):
            with open(yml_path, u'r') as f:
                upstream_id, _ = os.path.splitext(os.path.basename(yml_path))
                pb = model_pb2.Upstream()
                pb.meta.id = upstream_id
                pb.meta.namespace_id = self.namespace_id
                pb.spec.type = model_pb2.YANDEX_BALANCER
                pb.spec.yandex_balancer.yaml = f.read()

                section_pb = self.parse(pb.spec.yandex_balancer.yaml)
                section = Holder(section_pb)
                section.validate(preceding_modules=[ANY_MODULE],
                                 ctx=ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM,
                                                   namespace_id=self.namespace_id))

                pb.spec.yandex_balancer.config.CopyFrom(section_pb)
                pb.spec.labels[u'order'] = u'1'
                upstream_pbs[(self.namespace_id, upstream_id)] = pb
        self.upstream_spec_pbs = {
            UpstreamVersion(
                ctime=u'2018-02-02t09:27:31.216017z',
                upstream_id=upstream_id,
                version=u'1',
                deleted=False): upstream_pb.spec
            for upstream_id, upstream_pb in six.iteritems(upstream_pbs)}

    @contextlib.contextmanager
    def update_balancer_config(self):
        yield self.balancer_spec_pb.yandex_balancer.config

    @contextlib.contextmanager
    def update_l7_macro(self):
        """
        :rtype: modules_pb2.L7Macro
        """
        yield self.balancer_spec_pb.yandex_balancer.config.l7_macro

    @contextlib.contextmanager
    def update_upstream_config(self, upstream_id):
        found_upstream_spec_pb = None
        for upstream_version, upstream_spec_pb in six.iteritems(self.upstream_spec_pbs):
            if upstream_version.upstream_id[1] == upstream_id:
                found_upstream_spec_pb = upstream_spec_pb
                break
        if found_upstream_spec_pb is not None:
            yield found_upstream_spec_pb.yandex_balancer.config

    @contextlib.contextmanager
    def update_domain_config(self, domain_id):
        found_domain_spec_pb = None
        for domain_version, domain_spec_pb in six.iteritems(self.domain_spec_pbs):
            if domain_version.domain_id[1] == domain_id:
                found_domain_spec_pb = domain_spec_pb
                break
        if found_domain_spec_pb is not None:
            yield found_domain_spec_pb.yandex_balancer.config

    def read_backend_configs(self):
        backend_spec_pbs = {}
        for loc in (u'sas', u'man', u'vla'):
            backend_version = BackendVersion(
                ctime=u'2018-02-02t09:27:31.216017z',
                backend_id=(self.namespace_id, u'httpbin-{}'.format(loc)),
                version=u'1',
                deleted=False)
            backend_spec_pb = model_pb2.BackendSpec()
            backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
            backend_spec_pb.selector.yp_endpoint_sets.add(cluster=loc, endpoint_set_id=u'httpbin')
            backend_spec_pbs[backend_version] = backend_spec_pb
        self.backend_spec_pbs = backend_spec_pbs

    def add_antirobot_backends(self):
        for loc in (u'sas', u'man', u'vla'):
            backend_version = BackendVersion(
                ctime=u'2018-02-02t09:27:31.216017z',
                backend_id=(u'common-antirobot', u'antirobot_{}_yp'.format(loc)),
                version=u'1',
                deleted=False)
            backend_spec_pb = model_pb2.BackendSpec()
            backend_spec_pb.is_global.value = True
            backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
            backend_spec_pb.selector.yp_endpoint_sets.add(cluster=loc, endpoint_set_id=u'httpbin')
            self.backend_spec_pbs[backend_version] = backend_spec_pb

    def add_rps_limiter_backends(self, installation=u''):
        inst = rps_limiter_settings.get_installation(installation)
        for loc, backend_id in six.iteritems(inst.get_full_backend_ids()):
            backend_version = BackendVersion(
                ctime=u'2018-02-02t09:27:31.216017z',
                backend_id=backend_id,
                version=u'1',
                deleted=False)
            backend_spec_pb = model_pb2.BackendSpec()
            backend_spec_pb.is_global.value = True
            backend_spec_pb.selector.type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
            backend_spec_pb.selector.yp_endpoint_sets.add(cluster=loc, endpoint_set_id=u'httpbin')
            self.backend_spec_pbs[backend_version] = backend_spec_pb

    def add_cert(self, cert_id, deleted=False, incomplete=False, is_ecc=False):
        cert_spec_pb = model_pb2.CertificateSpec()
        if is_ecc:
            cert_spec_pb.fields.public_key_info.algorithm_id = u'ec'
        cert_version = CertVersion(
            ctime=u'2018-02-02t09:27:31.216017z',
            cert_id=(self.namespace_id, cert_id),
            version=u'1',
            deleted=deleted,
            incomplete=incomplete)
        self.cert_spec_pbs[cert_version] = cert_spec_pb

    def add_domain(self, fqdns,
                   cert_id=None,
                   secondary_cert_id=None,
                   domain_type=model_pb2.DomainSpec.Config.COMMON,
                   protocol=model_pb2.DomainSpec.Config.HTTP_ONLY,
                   deleted=False,
                   incomplete=False):
        domain_spec_pb = model_pb2.DomainSpec()
        domain_spec_pb.type = model_pb2.YANDEX_BALANCER
        config_pb = domain_spec_pb.yandex_balancer.config
        config_pb.type = domain_type
        config_pb.protocol = protocol
        config_pb.fqdns.extend(fqdns)
        config_pb.include_upstreams.filter.id = u'flat'
        if cert_id:
            config_pb.cert.id = cert_id
        if secondary_cert_id:
            config_pb.secondary_cert.id = secondary_cert_id
        domain_version = DomainVersion(
            ctime=u'2018-02-02t09:27:31.216017z',
            domain_id=(
                self.namespace_id, fqdns[0] if domain_type != model_pb2.DomainSpec.Config.WILDCARD else u'wildcard'
            ),
            version=u'1',
            deleted=deleted,
            incomplete=incomplete)
        self.domain_spec_pbs[domain_version] = domain_spec_pb
        return domain_spec_pb

    def remove_domain(self, domain_id):
        for domain_version in list(self.domain_spec_pbs):
            if domain_version.domain_id == (self.namespace_id, domain_id):
                del self.domain_spec_pbs[domain_version]
                return

    def _get_http_port(self):
        ports = self.balancer_spec_pb.yandex_balancer.config.l7_macro.http.ports
        if ports:
            return ports[0]
        else:
            return 80

    def render(self):
        balancer = generator.validate_config(
            namespace_pb=self.namespace_pb,
            namespace_id=self.namespace_id,
            balancer_version=self.balancer_version,
            balancer_spec_pb=self.balancer_spec_pb,
            upstream_spec_pbs=self.upstream_spec_pbs,
            backend_spec_pbs=self.backend_spec_pbs,
            endpoint_set_spec_pbs={},
            domain_spec_pbs=self.domain_spec_pbs,
            cert_spec_pbs=self.cert_spec_pbs,
        ).balancer
        ctx = ValidationCtx(
            domain_config_pbs={version.domain_id: pb.yandex_balancer.config
                               for version, pb in six.iteritems(self.domain_spec_pbs)},
            sd_client_name=u'awacs-l7-balancer({}:{})'.format(self.namespace_id, self.balancer_id),
            namespace_id=self.namespace_id)
        config = (balancer.module or balancer.chain).to_config(ctx=ctx)
        lua = config.to_top_level_lua()
        return lua

    def inject_certs(self, lua, certs_dirs):
        if not self.cert_spec_pbs:
            return lua
        src_certs_dir = os.path.join(self.config_dir, u'certs')
        for filename in os.listdir(src_certs_dir):
            if filename.endswith(u'.pem'):
                shutil.copyfile(os.path.join(src_certs_dir, filename), os.path.join(certs_dirs, filename))
        for cert_version in self.cert_spec_pbs:
            cert_id = cert_version.cert_id[1]  # without the namespace part
            lua = lua.replace('get_private_cert_path("{}.pem", "/dev/shm/balancer/priv");'.format(cert_id),
                              'get_private_cert_path("{}.pem", "{}");'.format(cert_id, certs_dirs))
            lua = lua.replace('get_public_cert_path("allCAs-{}.pem", "/dev/shm/balancer");'.format(cert_id),
                              'get_public_cert_path("allCAs-{}.pem", "{}");'.format(cert_id, certs_dirs))
        lua = lua.replace('"/dev/shm/balancer/priv"', '"{}"'.format(certs_dirs))
        return lua

    @staticmethod
    def inject_config_tag(lua, config_tag):
        return lua.replace('instance = {', 'instance = { config_tag = "' + config_tag + '"; ')

    @staticmethod
    def disable_reuse_port(lua):
        return lua.replace('enable_reuse_port = true', 'enable_reuse_port = false')

    @staticmethod
    def enable_debug_log(lua):
        return lua.replace('log_level = "ERROR";', 'log_level = "DEBUG";')

    @staticmethod
    def rewrite_sd_host(lua):
        return lua.replace('sd.yandex.net', '127.0.0.1' if is_ipv4_only() else '::1')

    @staticmethod
    def rewrite_http_port(lua, new_port):
        return lua.replace(str(MAGIC_HTTP_PORT), new_port)

    @staticmethod
    def rewrite_https_port(lua, new_port):
        return lua.replace(str(MAGIC_HTTPS_PORT), new_port)

    def rewrite_port_sd(self, lua):
        return lua.replace('port = {};'.format(MAGIC_SDSTUB_PORT), 'port = {};'.format(self.sd_port))

    def get_next_port(self, local_ip, worker_id):
        if worker_id.startswith('gw'):
            self.WORKER_SEQ += 1
            return self.BASE_PORT + self.WORKER_RANGE * worker_id_to_offset(worker_id) + self.WORKER_SEQ
        else:
            return get_free_port(local_ip)

    def check_lua(self, lua, path_to_expected_lua):
        if path_to_expected_lua is None:
            return
        lua_path = os.path.join(self.fixture_dir, path_to_expected_lua)
        if not os.path.exists(lua_path):
            if not os.path.exists(os.path.dirname(lua_path)):
                os.makedirs(os.path.dirname(lua_path))
            with open(lua_path, mode='w') as fw:
                fw.write(lua)
            raise AssertionError(u"Expected lua didn't exist, saved it to {}".format(lua_path))
        with open(lua_path) as f:
            expected_lua = f.read().strip()
        new_lua_path = lua_path + u'.new'
        if expected_lua != lua.strip():
            with open(new_lua_path, mode='w') as fw:
                fw.write(lua)
            raise AssertionError(u'lua does not match expected config, saved new version to {}'.format(new_lua_path))
        else:
            if os.path.exists(new_lua_path):
                os.remove(new_lua_path)

    @contextlib.contextmanager
    def access_log(self):
        with open(self.access_log_path) as af:
            yield af

    @contextlib.contextmanager
    def start_pginx(self, path_to_expected_lua=None, worker_id=''):
        log_dir = tempfile.mkdtemp()
        certs_dir = tempfile.mkdtemp()
        lua = self.render()
        self.check_lua(lua, path_to_expected_lua)
        # Assign config_tag to check it after starting the balancer and make sure that
        # we are running the right configuration file.
        lua = self.inject_config_tag(lua, self.config_tag)
        # SO_REUSEPORT allows multiple processes to bind on the same port.
        # When our tests don't kill a balancer process from a previous test (e.g. due to a bug),
        # test requests get distributed among older and newer balancer processes, which
        # leads to very unexpected flaps and errors.
        # To not deal with them, let's disable SO_REUSEPORT.
        lua = self.disable_reuse_port(lua)

        prev_lua = lua
        lua = self.inject_certs(lua, certs_dir)
        certs_injected = lua != prev_lua

        lua = self.enable_debug_log(lua)
        lua = self.rewrite_sd_host(lua)
        local_ip = get_local_ip()

        self.http_port = self.get_next_port(local_ip, worker_id)
        self.https_port = self.get_next_port(local_ip, worker_id)
        assert self.http_port != self.https_port
        lua = self.rewrite_http_port(lua, str(self.http_port))
        lua = self.rewrite_https_port(lua, str(self.https_port))

        lua = self.rewrite_port_sd(lua)
        with tempfile.NamedTemporaryFile(suffix=u'.{}.lua'.format(self.config_tag)) as f:
            self.access_log_path = os.path.join(log_dir, u'current-access_log-balancer-{}'.format(self.http_port))
            try:
                f.truncate()
                f.write(lua.encode('utf8'))
                f.flush()
                if not os.path.exists(self.env_dir):
                    os.mkdir(self.env_dir)
                args = [
                    f.name,
                    u'-Vlog_dir={}'.format(log_dir),
                    u'-Vport={}'.format(self.http_port),
                    u'-Vget_workers_provider=./dump_json_get_workers_provider.lua',
                ]

                timeout = 10
                with safe_balancer_process(self.ctx, self.balancer_executable_path,
                                           args, self.namespace_id, timeout, self.env_dir, env_vars={},
                                           config_tag=self.config_tag) as balancer_process:
                    balancer_started = wait_for_balancer_start(self.ctx, balancer_process, timeout, log_dir)
                    if not balancer_started:
                        pytest.fail('balancer did not start')

                    url = u'http://[{}]:{}'.format(local_ip, self.http_port)
                    self.ctx.log.info(
                        u'balancer ({}) started on {}, config_tag: {}, ports {} {}'.format(args, url, self.config_tag,
                                                                                           self.http_port,
                                                                                           self.https_port))
                    self.ctx.log.info(u'waiting for successful health check from balancer...')

                    def hc_200():
                        admin_url = u'http://[{}]:{}/admin?action=show_tag'.format(u'::1', self.http_port)
                        assert requests.get(admin_url).text == self.config_tag
                        resp = requests.get(url + u'/awacs-balancer-health-check')
                        assert resp.status_code == 200

                        if certs_injected:
                            s, https_url = get_https_session(self, url, 'test', verify=False)
                            resp = s.get(u'{}/awacs-balancer-health-check'.format(https_url), verify=t(ROOT_CA_PEM))
                            assert resp.status_code == 200

                    try:
                        wait_until_passes(hc_200, timeout=30, interval=1)
                        self.ctx.log.info(u'health check: success'.format(args, url, self.config_tag))
                    except:
                        childs_log_path = get_childs_log_path(log_dir)
                        if childs_log_path:
                            with open(childs_log_path) as bf:
                                content = bf.read()
                            self.ctx.log.info(u'childs_log content:')
                            self.ctx.log.info(content)
                        self.ctx.log.info(u'\nbalancer process stdout:')
                        try:
                            stdout = balancer_process.stdout.read()
                        except pexpect.exceptions.TIMEOUT:
                            stdout = u'<TIMEOUT>'
                        self.ctx.log.info(stdout)
                        raise
                    try:
                        yield url
                    except:
                        raise
                    finally:
                        for action in (u'reopenlog', u'shutdown'):
                            admin_url = u'http://[{}]:{}/admin?action={}'.format(u'::1', self.http_port, action)
                            try:
                                resp = requests.get(admin_url)
                            except Exception as e:
                                self.ctx.log.info(u'Failed to {}: {!r}'.format(action, e))
                            else:
                                if resp.status_code != 200:
                                    self.ctx.log.info(u'Failed to {}: {!r}'.format(action, resp.text))
                        time.sleep(3)
            except Exception as e:
                self.ctx.log.info(e)
                with open(self.access_log_path, u'r') as af:
                    self.ctx.log.info(u'accesslog:')
                    self.ctx.log.info(u'\n'.join(af.read().splitlines()[-5:]))
                with open(os.path.join(log_dir, u'current-error_log-balancer-{}'.format(self.http_port)), u'r') as ef:
                    self.ctx.log.info(u'errorlog:')
                    self.ctx.log.info(u'\n'.join(ef.read().splitlines()[-5:]))

                raise
            finally:
                for temp_dir in (log_dir, certs_dir):
                    try:
                        shutil.rmtree(temp_dir)  # delete directory
                    except OSError as exc:
                        if exc.errno != errno.ENOENT:  # ENOENT - no such file or directory
                            raise  # re-raise exception
            self.access_log_path = None


def get_childs_log_path(dir_path):
    for filename in os.listdir(dir_path):
        if filename.startswith(u'current-childs_log-balancer-'):
            return os.path.join(dir_path, filename)


def wait_for_balancer_start(ctx, balancer_process, timeout, log_dir, cert_is_expired=False):
    elapsed = 0
    started = False
    childs_log_path = None
    while (balancer_process.running if IS_ARCADIA else balancer_process.isalive()) and elapsed < timeout:
        childs_log_path = get_childs_log_path(log_dir)
        if childs_log_path:
            try:
                with open(childs_log_path) as bf:
                    content = bf.read()
                    if 'Server started' in content or 'Master process started' in content:
                        started = True
                        break
            except:
                pass
        time.sleep(.1)
        elapsed += .1
    if started:
        ctx.log.info(u'balancer started successfully')
        return True
    if cert_is_expired:
        if IS_ARCADIA:
            assert balancer_process.returncode == 1
        else:
            output = balancer_process.read()
            if six.PY3:
                assert b'Certificate is expired' in output
            else:
                assert 'Certificate is expired' in output
        ctx.log.info(u'balancer failed successfully')
        return True
    ctx.log.info(u'balancer failed to start')
    try:
        if childs_log_path:
            with open(childs_log_path) as bf:
                content = bf.read()
            ctx.log.info(u'childs_log content:')
            ctx.log.info(content)
        if IS_ARCADIA:
            output = balancer_process.stdout.read()
        else:
            output = balancer_process.read()
        ctx.log.info(u'balancer process stdout:')
        ctx.log.info(output)
    except:
        pass
    return False


@contextlib.contextmanager
def safe_balancer_process(ctx, balancer_executable_path, balancer_args, fixture_dirname, timeout,
                          env_dir, env_vars, config_tag=None):
    ctx.log.info(u'starting balancer \n{} {}\n cwd {}\n env {}'.format(
        balancer_executable_path, ' '.join(balancer_args), env_dir, env_vars))
    if IS_ARCADIA:
        from yatest import common
        balancer_process = common.execute([balancer_executable_path] + balancer_args,
                                          cwd=env_dir, env=env_vars, wait=False,
                                          close_fds=True)
    else:
        balancer_process = pexpect.spawn(balancer_executable_path, balancer_args, timeout=timeout,
                                         cwd=env_dir, env=env_vars, echo=False)
    if IS_ARCADIA:
        pid = balancer_process.process.pid
    else:
        pid = balancer_process.pid
    ctx.log.info(u'balancer process started, pid = {}'.format(pid))
    try:
        yield balancer_process
    finally:
        time.sleep(1)  # sleep a little to let balancer write its logs
        ctx.log.info(u'stopping balancer')
        try:
            if IS_ARCADIA:
                balancer_process.terminate()
            else:
                balancer_process.close(force=True)
        except ptyprocess.PtyProcessError as e:
            ctx.log.info(u'Could not terminate the balancer: {}, {}'.format(fixture_dirname, e))
            if six.text_type(e) == u'Could not terminate the balancer.':
                # Sometimes happens in Sandbox
                pass
            else:
                raise
        finally:
            pids_to_kill = set()
            if psutil.pid_exists(pid):
                pids_to_kill.add(pid)
            if config_tag is not None:
                for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                    if config_tag in u' '.join(proc.info[u'cmdline']):
                        ctx.log.info(u'Found a process to kill: {}'.format(proc.info))
                        pids_to_kill.add(proc.info[u'pid'])

            for pid in pids_to_kill:
                # try really hard to kill the process
                ctx.log.info(u'killing balancer pid = {}'.format(pid))
                try:
                    p = psutil.Process(pid)
                except psutil.NoSuchProcess:
                    ctx.log.info(u'balancer process (pid %d) not found', pid)
                else:
                    p.terminate()
                    for attempt in range(10):
                        try:
                            p.wait(timeout=5)
                        except psutil.TimeoutExpired:
                            continue
                        else:
                            if not p.is_running():
                                break
                            ctx.log.info(u'attempt {}, balancer pid = {} still exists'.format(attempt, pid))
                            p.kill()
                    ctx.log.info(u'balancer stopped')

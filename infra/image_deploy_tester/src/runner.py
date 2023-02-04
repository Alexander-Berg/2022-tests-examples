import logging
import socket
import time

import getpass
from gevent import threadpool
from infra.qyp.image_deploy_tester.src.lib import vmproxy_client
from infra.qyp.image_deploy_tester.src.lib import yp_client
from infra.qyp.proto_lib import vmagent_pb2

import nanny_rpc_client
import paramiko
import paramiko.ssh_exception
from infra.qyp.image_deploy_tester.src.lib.yasmutil import push_to_yasm
from sepelib.core import config


class StatusFail(Exception):
    pass


class SshFail(Exception):
    pass


class SshCmdFail(Exception):
    pass


class VmNameConflict(Exception):
    pass


class Job(object):
    DEFAULT_VM_NAME_PREFIX = 'test-qyp-'
    DEFAULT_TIMEOUT = 600

    def __init__(self, client, name, image_url):
        super(Job, self).__init__()
        self.client = client
        self.name = name
        self.image_url = image_url
        self.started = False

        job_config = config.get_value('app.job_config')
        vm_name_prefix = job_config.get('vm_name_prefix', self.DEFAULT_VM_NAME_PREFIX)
        self.pod_id = '{}{}'.format(vm_name_prefix, self.name)

        app_timeouts = job_config['timeouts']
        self.iteration_timeout = app_timeouts.get('iteration')
        self.status_timeout = app_timeouts.get('status', self.DEFAULT_TIMEOUT)
        self.ssh_timeout = app_timeouts.get('ssh', self.DEFAULT_TIMEOUT)
        self.boot_timeout = app_timeouts.get('boot', 0)
        self.ssh_cmd_timeout = app_timeouts.get('ssh_cmd', self.DEFAULT_TIMEOUT)
        self.log = logging.getLogger('job-{}'.format(self.pod_id))
        self.cmd = job_config.get('cmd', None)
        self.vm_label = job_config.get('vm_label')
        self.reuse_vms = job_config.get('reuse_vms', False)

    def _make_result(self, status_fail, ssh_fail, status_ts=None, ssh_ts=None, ssh_cmd_ts=None,
                     ssh_cmd_exit_code=None):
        """
        :type status_fail: int
        :type ssh_fail: int
        :type status_ts: int | NoneType
        :type ssh_ts: int | NoneType
        :type ssh_cmd_ts: int | NoneType
        :type ssh_cmd_exit_code: int | NoneType
        :rtype: dict[str, int]
        """
        result = {
            'status_fail_teet': status_fail,
            'ssh_fail_teet': ssh_fail,
        }
        if status_ts is not None:
            result['status_ts_tvvv'] = status_ts
        if ssh_ts is not None:
            result['ssh_timeout_tvvv'] = ssh_ts
        if ssh_cmd_ts is not None:
            result['ssh_cmd_ts_tvvv'] = ssh_cmd_ts
        if ssh_cmd_exit_code is not None:
            result['ssh_cmd_exit_code_txxt'] = ssh_cmd_exit_code
        return result

    def _handle(self, **kwargs):
        """
        :rtype: dict[str, int]
        """
        node_id = kwargs.get('node_id', None)
        self.prepare(node_id=node_id)
        self.started = True
        self.log.info('Wait till vm change status to RUNNING...')
        status, status_ts = self.wait_status()
        self.log.info('Vmagent started in {}sec'.format(status_ts))

        addr = None
        for ip in status.ip_allocations:
            if ip.vlan_id == 'backbone' and ip.owner == 'vm':
                addr = ip.address
                break

        if self.boot_timeout:
            self.log.info('Wait {} seconds for VM to boot'.format(self.boot_timeout))
            time.sleep(self.boot_timeout)

        self.log.info('Trying to connect {} via ssh'.format(addr))
        ssh_ts, ssh_cmd_ts, ssh_cmd_exit_code = self.wait_ssh(addr)
        self.log.info('Login via ssh in {}sec'.format(ssh_ts))
        if self.cmd:
            self.log.info('Command run finished in : {}, with exit code {}'.format(
                ssh_cmd_ts, ssh_cmd_exit_code
            ))
        self.log.info('Successfully connected')
        return self._make_result(False, False, status_ts, ssh_ts, ssh_cmd_ts, ssh_cmd_exit_code)

    def run(self, **kwargs):
        """
        :rtype: dict[str, int]
        """
        try:
            return self._handle(**kwargs)
        except StatusFail:
            self.log.info('Status time limit exceeded')
            return self._make_result(True, False)
        except SshFail:
            self.log.info('Ssh time limit exceeded')
            return self._make_result(False, True)
        except SshCmdFail:
            self.log.info('Ssh command time limit exceeded')
            return self._make_result(True, True)
        except Exception as e:
            self.log.error('Failed: {}'.format(e))
            return self._make_result(True, True)
        finally:
            self.cleanup()

    def wait_status(self):
        """
        :rtype: (vmagent_pb2.VMStatus, int)
        """
        time_start = time.time()
        while time.time() - time_start < self.status_timeout:
            try:
                status = self.client.get_vm(self.pod_id).vm.status
            except nanny_rpc_client.exceptions.BadRequestError:
                # When instance not ready, backend responds 400
                time.sleep(self.iteration_timeout)
            else:
                if status.state.type == vmagent_pb2.VMState.RUNNING:
                    status_ready_time = time.time() - time_start
                    return status, status_ready_time

        raise StatusFail()

    def wait_ssh(self, addr):
        """
        :type addr: str
        :rtype: (int, int, int)
        """
        key_filename = config.get_value('auth.key_filename', default=None)
        pkey = paramiko.RSAKey.from_private_key_file(key_filename) if key_filename else None
        username = config.get_value('auth.username', default=None) or getpass.getuser()
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

        time_start = time.time()
        while time.time() - time_start < self.ssh_timeout:
            try:
                ssh.connect(addr, username=username, pkey=pkey, timeout=60)
                ssh_success_time = time.time() - time_start
                if self.cmd:
                    ssh_cmd_exit_code = self.run_ssh_command(ssh)
                    ssh_cmd_time = time.time() - ssh_success_time - time_start
                    return ssh_success_time, ssh_cmd_time, ssh_cmd_exit_code
                else:
                    return ssh_success_time, None, None
            except (paramiko.ssh_exception.SSHException, socket.error):
                time.sleep(self.iteration_timeout)
            finally:
                ssh.close()

        raise SshFail()

    def run_ssh_command(self, ssh):
        """
        :type ssh: paramiko.SSHClient
        :rtype: int
        """
        time_start = time.time()
        self.log.info('Running {}'.format(self.cmd))
        s = ssh.get_transport().open_session()
        paramiko.agent.AgentRequestHandler(s)
        s.exec_command(self.cmd)
        while time.time() - time_start < self.ssh_cmd_timeout:
            if s.exit_status_ready():
                return s.recv_exit_status()
            time.sleep(60)

        raise SshCmdFail()

    def prepare(self, node_id):
        """
        :type node_id: str | NoneType
        """
        vm = None
        force_delete = False
        try:
            vm = self.client.get_vm(self.pod_id).vm
        except nanny_rpc_client.exceptions.NotFoundError:
            pass
        except nanny_rpc_client.exceptions.BadRequestError:
            force_delete = True

        if force_delete or vm and self.vm_label in vm.spec.labels:
            if self.reuse_vms:
                self.log.info('Using previously created vm')
                return

            self.log.info('Remove previously created VM')
            self.client.remove_vm(self.pod_id)
            self.log.info('Create new vm')
            if node_id:
                self.log.info('Will place VM on {}'.format(node_id))
            self.client.create_vm(self.pod_id, self.image_url, self.vm_label, node_id)
            return

        if vm is None:
            self.log.info('Create new vm')
            if node_id:
                self.log.info('Will place VM on {}'.format(node_id))
            self.client.create_vm(self.pod_id, self.image_url, self.vm_label, node_id)
            return

        self.log.error('VM with this name already exists')
        raise VmNameConflict()

    def cleanup(self):
        if not self.started:
            self.log.info('Job has not been started, nothing to clean')
            return
        if self.reuse_vms:
            self.log.info('Reusing VMs enabled, do not remove this VM')
            return

        self.log.info('Remove vm')
        try:
            self.client.remove_vm(self.pod_id)
        except Exception as e:
            self.log.error('Remove failed: {}'.format(e))


class Runner(object):
    def __init__(self):
        self.log = logging.getLogger('runner')
        self.run_interval = config.get_value('app.run_interval')
        default_cluster = config.get_value('vmproxy.default_cluster')
        for backend in config.get_value('vmproxy.backends'):
            if backend['cluster'] == default_cluster:
                url = backend.get('url')
                break
        else:
            raise ValueError('Cannot find default cluster in backends list')
        client = vmproxy_client.VmproxyClient(url=url, token=config.get_value('vmproxy.token'))

        yp_config = config.get_value('yp')
        yp_default_cluster = yp_config['default_cluster']
        for cluster in yp_config['clusters']:
            if cluster['cluster'] == yp_default_cluster:
                url = cluster.get('url')
                break
        else:
            raise ValueError('Cannot find default cluster YP url')
        self.yp_client = yp_client.YpClient(yp_default_cluster, url, yp_config['token'])

        self.enable_yasm = config.get_value('yasm.enable', False)
        self.iterate_nodes = config.get_value('app.iterate_nodes', False)
        self.node_filter = config.get_value('app.node_filter', None)
        self._all_nodes = None
        self.yasm_conf = config.get_value('yasm')

        self.jobs = []
        for job in config.get_value('jobs'):
            job_count = job.get('count', 1)
            for index in range(1, job_count + 1):
                name = '{}-{}'.format(job['name'], index)
                self.jobs.append(Job(client, name, job['image_url']))

    @property
    def all_nodes(self):
        if self._all_nodes is None:
            self._all_nodes = self._get_next_node()
        return self._all_nodes

    def _get_next_node(self):
        """
        :rtype: str
        """
        all_nodes = self.yp_client.get_nodes_from_dev(self.node_filter)
        while True:
            for node in all_nodes:
                yield node

    def run(self):
        pool = threadpool.ThreadPool(len(self.jobs))
        while True:
            self.log.info('Start new iteration')
            seen_nodes = set()
            for job in self.jobs:
                if self.iterate_nodes:
                    node_id = next(self.all_nodes)
                    if node_id in seen_nodes:
                        self.log.warn('Node {} already occupied, skip job {}'.format(node_id, job.name))
                        continue
                    seen_nodes.add(node_id)
                else:
                    node_id = None
                pool.spawn(self._single_run, job=job, node_id=node_id)
            pool.join()
            self.log.info('Wait {}secs before new iteration'.format(self.run_interval))
            time.sleep(int(self.run_interval))

    def _single_run(self, job, node_id):
        """
        :type job: Job
        :type node_id: str
        """
        result = job.run(node_id=node_id)
        if self.enable_yasm:
            tags = self.yasm_conf['tags'].copy()
            tags['prj'] = job.name
            push_to_yasm(
                url=self.yasm_conf['url'],
                values=result,
                tags=tags,
                ttl=self.yasm_conf['ttl']
            )

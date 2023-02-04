import json
import os
import requests
import time
import uuid

import google.protobuf.json_format as json_format
import google.protobuf.text_format as text_format

import yatest
import yatest.common.network as network

import infra.callisto.protos.deploy.tables_pb2 as tables  # noqa
import infra.callisto.deploy.deployer.config.config_pb2 as config_pb2  # noqa
import search.plutonium.deploy.proto.sources_pb2 as sources  # noqa


# Shorter intervals (0.1 s) to help tests to converge faster.
DEFAULT_CONFIG = config_pb2.TConfig(Execution=config_pb2.TExecution(DownloadInterval=100,
                                                                    UpdateTargetInterval=100))


class Helper(object):
    def __init__(self):
        self.pod_id = str(uuid.uuid4())
        self.namespace = str(uuid.uuid4())
        self.targets = {}  # Dict: LocalPath -> TPodTarget
        self.process = None
        self.workload = None
        self._config = DEFAULT_CONFIG
        self._use_target_table = False
        with network.PortManager() as port_manager:  # todo: release ports
            self.workload_port = port_manager.get_port()
            self.deployer_port = port_manager.get_port()

    def add_target(self, target):
        assert target.LocalPath not in self.targets
        self.targets[target.LocalPath] = target

    def add_static_target(self, path, content):
        self.add_target(tables.TPodTarget(
            PodId=self.pod_id,
            Namespace=self.namespace,
            LocalPath=path,
            ResourceSpec=sources.TSource(Static=sources.TStaticSource(Content=content))
        ))

    def add_notification(self, path, extended=False):
        self.targets[path].Notification.CopyFrom(
            tables.TNotification(HttpRequest=tables.THttpRequest(Method='GET',
                                                                 Port=self.workload_port,
                                                                 Path='/'),
                                 Extended=extended))

    def configure(self, config):
        self._config = config

    def run_deployer(self, wait_until_ready=True):
        with open(self.target_file, 'w') as f:
            json.dump([json_format.MessageToDict(target) for target in self.targets.values()], f, indent=4)

        with open(self.config_file, 'w') as f:
            f.write(text_format.MessageToString(self._config))

        with open(self.notifications_config_file, 'w') as f:
            config = '''
                Notifications {{
                    Namespace: "{}"
                    Cooldown: 0
                }}
            '''.format(self.namespace)
            f.write(config)

        target_options = ['--target-file', self.target_file] if not self._use_target_table else \
            ['--target-table', '//home/target-table', '--yt-proxy', 'non-existent.bad-domain.yandex.net']
        self.process = yatest.common.execute([
            yatest.common.binary_path('infra/callisto/deploy/deployer/python/deployer'),
            '--pod-id', self.pod_id,
            '--storage-root', self.storage_root,
            '--log-dir', self.logs_dir,
            '--status-file', self.status_file,
            '--unistat-port', str(self.deployer_port),
            '--config', self.config_file,
            '--notifications-config', self.notifications_config_file] + target_options,
            wait=False,
        )

        if wait_until_ready:
            # The dynamic table client (re)tries for 5 seconds.
            self.wait_until_ready(seconds=10)

        # Main deployer intervals are as large as 0.1 second both,
        # so 5 seconds must be enough for deployer loops to converge
        # even in presence of chaotic failures.
        # Chaotic failures are configured via CHAOS_FAILURE_PROBABILITY in ya.make/ENV section.
        # Keep CHAOS_FAILURE_PROBABILITY variable in sync with deployer intervals and the timeout below.
        time.sleep(5)
        try:
            self.process.kill()
        except yatest.common.InvalidExecutionStateError:
            pass

    def run_workload(self, active=True, extended=False):
        command = [yatest.common.binary_path('infra/callisto/deploy/deployer/testing/workload/workload')]
        command += ['--port', str(self.workload_port)]
        if extended:
            command += ['--extended']
        if active:
            command += ['--active']
        self.workload = yatest.common.execute(command, wait=False)

    def wait_until_ready(self, seconds):
        step = 0.1
        for _ in range(int(seconds / step)):
            try:
                requests.get('http://localhost:{}/unistat'.format(self.deployer_port)).json()
                return
            except IOError:
                time.sleep(step)
        raise RuntimeError('Deployer did not start')

    def get_status(self, path):
        with open(self.status_file) as f:
            statuses = json.load(f)
            for status in statuses:
                status = json_format.ParseDict(status, tables.TPodStatus())
                if status.LocalPath == path:
                    return status.ResourceState.Status

    def get_content(self, path):
        with open(os.path.join(self.storage_root, 'tree', self.namespace, '__resources__', path)) as f:
            return f.read()

    def use_target_table(self):
        self._use_target_table = True

    @staticmethod
    def path(subpath):
        return yatest.common.test_output_path(subpath)

    @property
    def storage_root(self):
        return self.path('storage_root')

    @property
    def logs_dir(self):
        return self.path('logs')

    @property
    def target_file(self):
        return self.path('targets.json')

    @property
    def status_file(self):
        return self.path('status.json')

    @property
    def config_file(self):
        return self.path('config.cfg')

    @property
    def notifications_config_file(self):
        return self.path('notifications_config.cfg')

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.process:
            try:
                self.process.kill()
            except yatest.common.InvalidExecutionStateError:
                pass

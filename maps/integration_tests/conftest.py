import asyncio
import json
import mock
import os
import pytest
import signal
import time
import logging

import yatest
from maps.infra.ecstatic.common.worker.lib.paths import StoragePath
from maps.infra.ecstatic.common.worker.lib.worker import EcstaticWorker
from maps.infra.ecstatic.common.experimental_worker.lib.worker import EcstaticWorker as AsyncWorker
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import coordinator, mongo, mongo_reinit  # noqa
from maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api import EcstaticAPI, setup_logger
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Coordinator as CoordinatorApi
from maps.pylibs.utils.lib.filesystem import temporary_directory
from bson import ObjectId
from multiprocessing import Process

logger = logging.getLogger()


class MockCoordinatorApi(CoordinatorApi):
    def _make_auth_header(self):
        return {'X-Ya-Src-Tvm-Id': '1'}

    def _deduce_hostnames(self):
        return 'host', 'host'


class MockEcstaticApi(EcstaticAPI):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.coordinator = MockCoordinatorApi(*args, **kwargs)


@pytest.fixture(scope='function', params=['sync', 'async'])
def storage(coordinator, mongo, request):  # noqa: F811
    mongo.reconfigure(storages_config=json.dumps([{'hosts': ['localhost', "storage12", "storage11"]}]))
    mongo.reconfigure(storages_config=json.dumps([{'hosts': ['localhost', "storage11"]}]))
    mongo.reconfigure(storages_config=json.dumps([{'hosts': ['localhost']}]))
    mongo.db['host_dc'].insert({'host': 'localhost', 'dc': 'dca', 'group': 'rtc:maps_core_ecstatic_storage_unstable',
                                'stamp': ObjectId()})
    coordinator.http_post('/debug/UpdateHostInfoCache')

    process = Storage(coordinator.url[len('http://'):], request.param == 'sync')
    coordinator.http_post('/debug/UpdateStorages')
    yield process
    process.stop()


@pytest.fixture(scope='function')
def ecstatic_tool(coordinator, storage):  # noqa: F811
    os.environ["unittest_hostname"] = 'host'
    yield MockEcstaticApi(coordinator_host=coordinator.url[len('http://'):])
    del os.environ["unittest_hostname"]


@pytest.fixture(scope='function')
def coordinator_api(coordinator):  # noqa: F811
    return MockCoordinatorApi(coordinator_host=coordinator.url[len('http://'):])


class Storage:
    def __init__(self, coordinator_host, sync=True):
        self.coordinator_host = coordinator_host
        self.sync = sync
        self.worker_cmd = ''
        with temporary_directory(keep=True) as tmp:
            self.root = tmp

        self.state_dir = os.path.join(self.root, 'state')
        self.conf_dir = os.path.join(self.root, 'conf')
        self.storage_dir = os.path.join(self.root, 'storage')
        self.grpc_socket = f'unix:{self.state_dir}/ymtorrent.sock'
        self.status_file = os.path.join(self.state_dir, 'ymtorrent.status')
        self.storage_path = StoragePath(self.storage_dir)
        for subdir in [
            self.storage_path.torrents,
            self.storage_path.versions,
            self.storage_path.content,
            self.storage_path.data,
            self.storage_path.preserved_data,
            self.state_dir,
            self.conf_dir
        ]:
            os.makedirs(subdir)

        self.worker_process = None
        self.start()

    @mock.patch('maps.infra.ecstatic.tool.client_interface.local_lock.LocalLockContextDecorator.__enter__', mock.Mock())
    @mock.patch('maps.infra.ecstatic.tool.client_interface.local_lock.LocalLockContextDecorator.__exit__', mock.Mock())
    @mock.patch('maps.infra.ecstatic.tool.ecstatic_api.coordinator.Coordinator._read_config')
    def start_worker(self, read_config_patch):
        def mock_read_config(*args, **kwargs):
            return {'coordinator': self.coordinator_host, 'coordinator_tvm_id': 12345}

        read_config_patch.side_effect = mock_read_config

        if self.sync:
            self.worker_process = Process(target=self._run_sync_worker)
        else:
            self.worker_process = Process(target=self._run_async_worker)
        self.worker_process.start()
        logger.info(f'Worker pid: {self.worker_process.pid}')

    @mock.patch('maps.infra.ecstatic.tool.ecstatic_api.coordinator.Coordinator._make_optional_auth_header',
                mock.Mock(return_value={'X-Ya-Src-Tvm-Id': '12345'}))
    def _run_sync_worker(self):
        os.environ['ECSTATIC_HOST'] = 'localhost'
        setup_logger('ecstatic', debug=True)

        def signal_handler(*args, **kwargs):
            os._exit(0)
        signal.signal(signal.SIGTERM, signal_handler)
        while True:
            try:
                worker = EcstaticWorker(conf_dir=self.conf_dir, status_dir=self.state_dir,
                                        storages=[self.storage_path], storage_mode=True, socket_path=self.grpc_socket,
                                        hook_timeouts={
                                            'check': 300, 'remove': 300, 'postdl': 300, 'switch': 600}
                                        )
                worker.run(switch_polling_interval=None)
            except Exception:
                pass
            time.sleep(1)
        os._exit(0)

    @mock.patch('maps.infra.ecstatic.tool.ecstatic_api.coordinator.Coordinator._make_optional_auth_header',
                mock.Mock(return_value={'X-Ya-Src-Tvm-Id': '12345'}))
    def _run_async_worker(self):
        os.environ['ECSTATIC_HOST'] = 'localhost'
        setup_logger('ecstatic', debug=True)
        worker = AsyncWorker(
            conf_dir=self.conf_dir, status_dir=self.state_dir,
            storages=[self.storage_path], storage_mode=True, socket_path=self.grpc_socket,
            hook_timeouts={
                'check': 300, 'remove': 300, 'postdl': 300, 'switch': 600}
        )
        asyncio.run(worker.run(switch_polling_interval=None))
        os._exit(0)

    def start_ymtorrent(self):
        env = os.environ.copy()
        env['unittest_hostname'] = 'localhost'
        self.ymtorrent_process = yatest.common.execute(
            [yatest.common.binary_path('maps/infra/ecstatic/ymtorrent/bin/ymtorrent'), '--grpc-socket',
             self.grpc_socket, '-d',
             self.storage_path.torrents, '-S', self.status_file],
            wait=False, env=env)
        time.sleep(10)

    def start(self):
        self.start_ymtorrent()
        self.start_worker()

    def stop(self):
        self.worker_process.terminate()
        self.worker_process.join()
        assert self.ymtorrent_process.running
        self.ymtorrent_process.terminate()
        self.ymtorrent_process.wait()
        assert self.ymtorrent_process.exit_code == 0

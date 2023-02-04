import asyncio
import hashlib
import json
import logging
import os

import pymongo
import pytest
import requests
import time
import typing as tp
import yatest
from yatest.common import network

from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import (
    ForbiddenError, BadRequestError, UnauthorizedError, GoneError, UnprocessableEntityError, LockedError, ServerError,
    NotFoundError
)
from maps.infra.ecstatic.mongo.lib.mongo_init import init_mongo
from maps.infra.ecstatic.proto import coordinator_pb2
from maps.infra.ecstatic.sandbox.reconfigurer.lib.mock_group_resolver import FileMockGroupResolver
from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import run_reconfiguration, ReconfigurationStage
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import TorrentsCollection, Dataset
from maps.pylibs.local_mongo import MongoServer

logger = logging.getLogger("test_logger")

DB_NAME = 'ecstatic'

DEFAULT_STORAGES_CONFIG = '[{"hosts": ["storage11", "storage12", "storage13"]}]'

CONFIG_COLLECTIONS = [
    "acls",
    "dataset_priority",
    "dataset_state",
    "dataset_ttl",
    "deploy_groups",
    "deploy_index",
    "lock_config"]

RUNTIME_COLLECTIONS = [
    "branch_deploys",
    "cookies",
    "holds",
    "host_dc",
    "locks",
    "postdl",
    "storage_assignments",
    "torrents",
    "tracker",
    "versions"]


class TestingTorrentsCollection(TorrentsCollection):
    @property
    def all_torrents(self) -> set[str]:
        return set(self._content.keys())

    @property
    def all_datasets(self) -> set[Dataset]:
        res = set()
        for datasets in self.datasets.values():
            res |= datasets
        return res

    @property
    def all_dataset_names(self) -> set[str]:
        return {dataset.name for dataset in self.all_datasets}


@pytest.fixture(scope='session')
def mongo() -> 'Mongo':
    return Mongo()


@pytest.fixture(autouse=True, scope='function')
def mongo_reinit(coordinator: 'Coordinator', mongo: 'Mongo'):
    mongo.reset()
    coordinator.reconfigure()


@pytest.fixture(scope='session')
def coordinator(mongo: 'Mongo'):
    with network.PortManager() as pm:
        process = Coordinator(pm.get_port(), mongo)
        yield process
        process.shutdown()


class Mongo(object):
    def __init__(self):
        self.mongo_server = MongoServer.create_instance()
        self.heartbeat_script_path = yatest.common.test_source_path('data/init-heartbeat.js')
        self.db = pymongo.MongoClient(f'{self.mongo_server.uri}/{DB_NAME}').get_database()

        for tries_left in reversed(range(20)):
            time.sleep(1)
            try:
                # required for coordinator start
                self.reset()
                break
            except Exception as e:
                if not tries_left:
                    raise RuntimeError('cannot connect to mongo: ' + str(e))

    def reset(self) -> None:
        self.mongo_server.clear()
        init_mongo(self.mongo_server.uri)
        self.reconfigure()

    def drop_database(self) -> None:
        self.mongo_server.clear()

    def get_list_of_storages(self) -> list[str]:
        res = []
        for shard in self.db['storages'].find({}, {'hosts': 1}):
            if shard['_id'] != '__SENTINEL__':
                res.append(set(shard['hosts']))
        return sorted(res)

    def _get_yav_mock(self, storages_config: str):
        yav_response = {'value': {'config': storages_config}}

        class Mocker:
            def __init__(self, *args, **kwargs):
                pass

            def get_version(self, *args, **kwargs):
                return yav_response

        return Mocker

    def reconfigure(
            self,
            storages_config: str = DEFAULT_STORAGES_CONFIG,
            hosts_config: str = 'data/host-groups.conf',
            config: str = 'data/ecstatic.conf',
            installation: str = 'unstable',
            wait_for_quorums_calculation: bool = False,
            config_revision: int = 1) -> None:
        run_reconfiguration(
            stage=ReconfigurationStage.UPLOAD,
            sharding_config=storages_config,
            mongo_uri=f'{self.mongo_server.uri}/{DB_NAME}',
            installation=installation,
            stub_hosts_file=yatest.common.test_source_path(hosts_config),
            files=[yatest.common.test_source_path(config)],
            wait_for_quorums_calculation=wait_for_quorums_calculation,
            revision=config_revision,
        )
        run_reconfiguration(
            stage=ReconfigurationStage.UPLOAD,
            sharding_config=storages_config,
            mongo_uri=f'{self.mongo_server.uri}/{DB_NAME}',
            installation=installation,
            stub_hosts_file=yatest.common.test_source_path(hosts_config),
            files=None,
            wait_for_quorums_calculation=wait_for_quorums_calculation,
            revision=None,
        )

        for entry in self.db['host_dc'].find({}):
            self.db['heartbeat'].update_one({'host': entry['host']},
                                            {'$set': {'group': entry['group'], 'lastSeen': int(time.time())}},
                                            upsert=True)
        for shard in self.db['storages'].find({}):
            logging.info(shard)
            for host in shard.get('hosts', []):
                self.db['heartbeat'].update_one({'host': host},
                                                {'$set': {'group': 'storage', 'lastSeen': int(time.time()),
                                                          'isStorage': True}},
                                                upsert=True)

    def add_dead_hosts(self, dead_hosts: list[str]) -> None:
        self.db['heartbeat'].delete_many({'host': {'$in': dead_hosts}})


def check_status_code(response: requests.Response) -> None:
    if response.status_code == 400:
        raise BadRequestError(response.url)
    elif response.status_code == 401:
        raise UnauthorizedError(response.url)
    elif response.status_code == 403:
        raise ForbiddenError(response.url)
    elif response.status_code == 410:
        raise GoneError(response.url)
    elif response.status_code == 422:
        raise UnprocessableEntityError(response.url)
    elif response.status_code == 423:
        raise LockedError(response.url)
    elif response.status_code == 500 or response.status_code == 503:
        raise ServerError(response.url)
    elif response.status_code == 404:
        raise NotFoundError(response.url)
    else:
        response.raise_for_status()


class Coordinator(object):
    def __init__(self, port: int, mongo: Mongo):
        self.url = 'http://localhost:' + str(port)
        self.port = port
        self.mongo = mongo
        env = os.environ.copy()
        env['YCR_MODE'] = 'http:' + str(port)
        env['MONGO_CONFIG'] = json.dumps({'uri': f'{self.mongo.mongo_server.uri}/ecstatic'})
        env['YCR_THREADS'] = '3'

        bin_path = yatest.common.binary_path(
            'maps/infra/ecstatic/coordinator/bin/ecstatic-coordinator')
        self.process = yatest.common.execute(
            [bin_path, '--enable-testing'],
            wait=False,
            env=env)

        for tries_left in reversed(range(20)):
            time.sleep(1)
            try:
                self.http_post('/debug/UpdateHostInfoCache')
                self.ping()
                break
            except Exception as e:
                if not tries_left:
                    raise RuntimeError('cannot ping coordinator: ' + str(e))

        self.http_post('/yacare/loglevel?level=debug')

    def shutdown(self):
        self.process.terminate()
        assert self.process.process.wait() == 0

    def restart(self):
        self.shutdown()
        self.__init__(self.port, self.mongo)

    def enable_bg_jobs(self):
        self.http_post('/debug/startBgJobs')

    def http_get(self, path: str,
                 tvm_id: tp.Optional[int] = None,
                 **kwargs) -> requests.Response:
        headers = {}
        if tvm_id is not None:
            headers.update({"X-Ya-Src-Tvm-Id": str(tvm_id)})
        response = requests.get(
            self.url + path,
            params=dict((k, v) for k, v in kwargs.items() if v is not None),
            headers=headers,
            timeout=60)
        check_status_code(response)
        return response

    def http_post(self, path: str,
                  data: tp.Optional[bytes] = None,
                  tvm_id: tp.Optional[int] = None,
                  **kwargs) -> requests.Response:
        headers = {"Content-Type": "application/octet-stream"}
        # Mock tvm authentication in tests: no service tickets, tvm-id passed as plain header
        if tvm_id:
            headers.update({"X-Ya-Src-Tvm-Id": str(tvm_id)})
        response = requests.post(
            self.url + path,
            params=dict((k, v) for k, v in kwargs.items() if v is not None),
            headers=headers,
            data=data,
            timeout=60)
        check_status_code(response)
        return response

    def ping(self) -> None:
        self.http_get('/ping')

    def upload(self, dataset: str,
               version: str,
               host: str,
               data: str = '',
               branch: str = '',
               disk_usage: int = 1,
               **kwargs) -> str:
        if not data:
            data = dataset + '_' + version

        pieces = '0:' if disk_usage == 0 else "20:AAAAAAAAAAAAAAAAAAAA"
        info = 'd5:filesld6:lengthi{0}e4:pathl{1}:{2}eee4:name1:_12:piece ' \
               'lengthi65536e6:pieces{3}e'.format(disk_usage, len(data), data, pieces)
        torrent = 'd8:announce16:http://localhost4:info' + info + "e"

        self.http_post('/upload',
                       dataset=dataset,
                       version=version,
                       host=host,
                       data=torrent,
                       **kwargs)

        if branch:
            self.step_in(dataset, version, host, branch=branch, **kwargs)

        return hashlib.sha1(info.encode('utf-8')).hexdigest()

    def announce(self,
                 info_hash: str,
                 hosts: tp.Union[str, list[str]],
                 left: int = 0,
                 peer_id: str = None,
                 port: int = 0,
                 **kwargs) -> str:
        if isinstance(hosts, str):
            hosts = [hosts]

        if peer_id and len(hosts) > 1:
            raise ValueError('cannot use custom peer_id for multiple hosts')

        byte_marker = '__'
        escaped_hash = ''.join(
            byte_marker + info_hash[i:i + 2] for i in range(0, len(info_hash), 2))

        # avoid hash urlescape
        class NoQuotedCommasSession(requests.Session):
            def send(self, *args, **kwargs):
                # args[0] is prepared request
                args[0].url = args[0].url.replace(byte_marker, '%')
                return requests.Session.send(self, *args, **kwargs)

        s = NoQuotedCommasSession()

        output = ''
        for host in hosts:
            args = {
                'info_hash': escaped_hash,
                'peer_id': peer_id or host,
                'ip': host,
                'port': port,
                'left': left}
            args.update(kwargs)

            response = s.get(self.url + '/tracker/announce',
                             params=args)
            check_status_code(response)
            output += response.text

        return output

    def postdl(self, dataset: str, version: str, hosts: tp.Union[str, list[str]]) -> None:
        if isinstance(hosts, str):
            hosts = [hosts]

        for h in hosts:
            self.http_post('/postdl',
                           host=h,
                           data='{0}\t{1}\n'.format(dataset, version))

    def current_versions(self, **kwargs) -> None:
        self.http_post('/current_versions', **kwargs)

    def versions(self, host: str, **kwargs) -> requests.Response:
        self.http_post('/debug/UpdateQuorums')
        return self.http_get('/versions', host=host, **kwargs)

    def report_versions(self, hosts: tp.Union[str, list[str]]) -> None:
        if isinstance(hosts, str):
            hosts = [hosts]

        for h in hosts:
            for line in self.versions(h).text.splitlines():
                if '__CURRENT__' not in line and '__NONE__' not in line:
                    self.current_versions(host=h, data=line)

    def get_postdl(self, host: str, **kwargs) -> requests.Response:
        return self.http_get('/postdl', host=host, **kwargs)

    def require_version(self, dataset: str, version: str, hosts: tp.Union[str, list[str]]) -> None:
        pattern = dataset + "\t" + (version or '')
        if isinstance(hosts, str):
            hosts = [hosts]
        for h in hosts:
            response = self.versions(h)
            assert pattern in response.text

    def step_in(self, dataset: str, version: str, host: str, branch: str = 'stable', **kwargs) -> requests.Response:
        return self.http_post(
            '/step_in',
            dataset=dataset,
            version=version,
            host=host,
            branch=branch,
            **kwargs)

    def retire(self, dataset: str, version: str, host: str, branch: str = 'stable', **kwargs) -> requests.Response:
        return self.http_post(
            '/retire',
            dataset=dataset,
            version=version,
            host=host,
            branch=branch,
            **kwargs)

    def torrents(self, **kwargs) -> TestingTorrentsCollection:
        self.http_post('/debug/UpdateStorages')
        return TestingTorrentsCollection(self.http_get('/torrents', **kwargs).content)

    def torrents_raw(self, **kwargs) -> requests.Response:
        self.http_post('/debug/UpdateStorages')
        return self.http_get('/torrents', **kwargs)

    def check_exists(self, dataset: str, version: str, tag: tp.Optional[str] = None, existency: bool = True) -> None:
        pattern = 'yes' if existency else 'no'
        assert pattern in self.http_get(
            '/exists',
            dataset=dataset,
            version=version,
            tag=tag).text

    def switch_failed(self, dataset: str, version: str, host: str, **kwargs) -> requests.Response:
        return self.http_post(
            '/switch_failed',
            dataset=dataset,
            version=version,
            host=host,
            **kwargs)

    def postdl_failed(self, dataset: str, version: str, host: str, **kwargs) -> requests.Response:
        return self.http_post(
            '/postdl_failed',
            dataset=dataset,
            version=version,
            host=host,
            **kwargs)

    def postdl_started(self, dataset: str, version: str, host: str, **kwargs) -> requests.Response:
        return self.http_post(
            '/postdl_started',
            dataset=dataset,
            version=version,
            host=host,
            **kwargs
        )

    def remove(self, dataset: str, version: str, host: str, **kwargs) -> set[Dataset]:
        resp = coordinator_pb2.RemoveResponse()
        resp.ParseFromString(self.http_post(
            '/remove',
            dataset=dataset,
            version=version,
            host=host,
            **kwargs).content)
        return {Dataset(f'{elem.dataset}{elem.tag}', elem.version) for elem in resp.removed}

    def reset_errors(self, dataset: str, version: str, group: str) -> requests.Response:
        return self.http_post(
            '/reset_errors',
            dataset=dataset,
            version=version,
            group=group)

    def move(self, dataset: str, version: str, branch: str, host: str, tvm_id: int) -> None:
        cmd = "/step_in" if branch[0] == '+' else "/retire"
        self.http_post(cmd, dataset=dataset, version=version, branch=branch[1:], host=host, tvm_id=tvm_id)

    def check_adopted(self, torrent_hash: str, check: bool = True) -> None:
        assert self.http_get('/is_adopted', info_hash=torrent_hash).text == ('yes' if check else 'no') + '\n'

    def list_status(self, dataset: str, branch: tp.Optional[str] = None) -> requests.Response:
        return self.http_get('/list_status', dataset=dataset, branch=branch)

    def list_versions(self, dataset: str, branch: tp.Optional[str] = None) -> requests.Response:
        return self.http_get('/list_versions', dataset=dataset, branch=branch)

    def is_adopted(self, torrent_hash: str) -> requests.Response:
        return self.http_get('/is_adopted', info_hash=torrent_hash)

    def get_lock(self, name: str, who: str, till: int, deadline: int) -> str:
        result = self.http_post(
            '/locks/get', name=name, who=who, till=till, deadline=deadline).text.split('\n')
        assert len(result) == 1
        return result[0]

    def release_lock(self, lock_id: str) -> requests.Response:
        return self.http_post('/locks/release', id=lock_id)

    def list_locks(self, host: str, age_limit: int = 0) -> coordinator_pb2.Locks:
        lock_collection = coordinator_pb2.Locks()
        lock_collection.ParseFromString(self.http_get('/locks/list', age_limit=age_limit, who=host).content)
        return lock_collection

    def replication_config(self) -> dict:
        return json.loads(self.http_get('/replication_config').text)

    def reconfigure(self, *args, **kwargs) -> None:
        self.mongo.reconfigure(*args, **kwargs)
        self.http_post('/debug/UpdateHostInfoCache')

    def download(self, dataset: str, version: str, tvm_id: tp.Optional[int] = None) -> None:
        self.http_get('/download', dataset=dataset, version=version, tvm_id=tvm_id)

    def list_datasets(self) -> coordinator_pb2.ListDatasetsResponse:
        resp = coordinator_pb2.ListDatasetsResponse()
        resp.ParseFromString(self.http_get("/dataset/list").content)
        return resp

    def dataset_versions(self, dataset: str) -> coordinator_pb2.ListVersionsResponse:
        resp = coordinator_pb2.ListVersionsResponse()
        resp.ParseFromString(self.http_get("/dataset/versions", dataset=dataset).content)
        return resp


def resolve_hosts(groups: tp.Union[str, list[str]]) -> list[str]:
    if isinstance(groups, str):
        groups = [groups]

    resolver = FileMockGroupResolver(
        yatest.common.test_source_path('data/host-groups.conf'))
    resolved = asyncio.run(resolver.resolve_groups(groups))
    return [h.fqdn for info in resolved.values() for h in info.hosts]

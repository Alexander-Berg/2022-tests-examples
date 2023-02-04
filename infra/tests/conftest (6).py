import os


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ

if not IS_ARCADIA:
    # import awacs first to let it call gevent monkeypatch
    import awacs  # noqa
    from awacs.model.objects import L7HeavyConfig

    # then hack threading module to prevent the following error when importing pdb or ipython:
    # The 'save_flag' trait of a HistoryManager instance must be a _Event or None, but a value of class 'gevent.event.Event'
    import threading


    threading._Event = threading.Event  # noqa

    # rewrite asserts in utility functions, need to call this before import
    import pytest


    pytest.register_assert_rewrite('awtest')  # noqa

    import socket
    import time
    import logging
    import copy
    import pexpect
    import six
    from kazoo.exceptions import NoNodeError
    from sepelib.core import config as app_config
    from infra.swatlib.cmdutil import setup_logging, setup_logging_to_stdout
    from infra.swatlib import metrics
    from awacs.app.base import ApplicationBase
    from awacs.lib import zookeeper_client, context, nannyclient, mongo, racktables
    from awacs.lib.models import classes as model_classes
    from awacs.model import db, cache as c, apicache as c2, storage_modern as zks
    from awacs.model.dao import Dao, IDao
    from awacs.model.zk import ZkStorage, IZkStorage
    from awacs.web.app import create_app
    from awtest.network import socket_allow_hosts, get_local_ip_v4, get_local_ip_v6, worker_id_to_offset
    from awtest.mocks.httpbin_server import Httpbin
    from awtest.mocks.racktables import RacktablesMockClient
    from awtest.mocks.yp_sd import SdStub
    from awtest.api import create_namespace
    from awtest.core import Checker
    from awtest import xdist
    from awtest.mocks import mongo as mongomock, zookeeper as zookeepermock
    from awtest.ctl_runner import CtlRunner
    from awtest.mocks.abc_client import AbcMockClient
    from awtest.mocks.dns_manager_client import DnsResolverMock, DnsManagerClientMock
    from awtest.mocks.l3mgr_client import L3MgrMockClient
    from awtest.mocks.nanny_client import NannyMockClient
    from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
    from awtest.mocks.yp_lite_client import YpLiteMockClient


    def pytest_sessionstart(session):
        if session.config.getoption(u'--vcr-record-mode') in (u'all', u'new_episodes'):
            return
        allowed = session.config.getoption(u'--allow-hosts')
        hosts = set()
        if allowed:
            hosts |= {host.strip() for host in allowed.split(u',')}
        if u'local_ip_v4' in hosts:
            hosts.discard(u'local_ip_v4')
            local_ip_v4 = get_local_ip_v4()
            if local_ip_v4:
                hosts.add(local_ip_v4)
        if u'local_ip_v6' in hosts:
            hosts.discard(u'local_ip_v6')
            local_ip_v6 = get_local_ip_v6()
            if local_ip_v6:
                hosts.add(local_ip_v6)
        socket_allow_hosts(hosts)


    if not IS_ARCADIA:
        def monkeypatch_execnet():
            # allows us to avoid errors like
            # Exception KeyError: KeyError(140283530475792,) in <module 'threading' from '/skynet/python/lib/python2.7/threading.pyc'> ignored  # noqa
            # see https://stackoverflow.com/questions/8774958/keyerror-in-module-threading-after-a-successful-py-test-run for details  # noqa
            import execnet.gateway_io

            patch_string = 'import gevent.monkey;gevent.monkey.patch_all(Event=True);'
            if not execnet.gateway_io.popen_bootstrapline.startswith(patch_string):
                execnet.gateway_io.popen_bootstrapline = patch_string + execnet.gateway_io.popen_bootstrapline


        def pytest_cmdline_main(config):  # noqa
            if config.option.numprocesses:
                monkeypatch_execnet()
                config.option.tx = ['popen//execmodel=gevent'] * config.option.numprocesses


    def pytest_addoption(parser):
        parser.addoption('--zookeeper', help='path to ZooKeeper directory', default='/opt/zookeeper-3.4.14/')
        parser.addoption('--config', '--cfg', help='path to awacs config')
        parser.addoption('--balancer', help='path to balancer executable')
        parser.addoption('--runslow', action='store_true', default=False, help='run slow tests')
        parser.addoption(
            '--allow-hosts',
            dest='allow_hosts',
            metavar='ALLOWED_HOSTS_CSV',
            help='Only allow specified hosts through socket.socket.connect((host, port)).',
            default='127.0.0.1,::1,local_ip_v4,local_ip_v6,localhost'
        )


    if IS_ARCADIA:
        @pytest.fixture(scope='module')
        def vcr_cassette_dir(request):
            from yatest import common
            return common.source_path('infra/awacs/vendor/awacs/tests/cassettes')


    def pytest_configure(config):
        config.option.verbose = True
        config.addinivalue_line("markers", "allow_hosts([hosts]): Restrict socket connection to defined list of hosts")
        config.addinivalue_line("markers", "slow: marks tests as slow")
        if IS_ARCADIA:
            from yatest import common
            config.option.vcr_record = common.get_param('vcr-record-mode')


    def pytest_ignore_collect(path, config):
        path_str = six.text_type(path)
        return 'tests/fixtures' in path_str or 'tests/cassettes' in path_str


    def pytest_collection_modifyitems(config, items):
        if config.getoption("--runslow"):
            # --runslow given in cli: do not skip slow tests
            return
        skip_slow = pytest.mark.skip(reason='need --runslow option to run')
        for item in items:
            if 'slow' in item.keywords:
                item.add_marker(skip_slow)


    def pytest_assertrepr_compare(op, left, right):
        class_name = left.__class__.__name__
        if class_name not in ('L7State', 'L3State', 'DnsRecordState'):
            return
        if right.__class__.__name__ != class_name or op != '==':
            return
        return ['Comparing State instances:'] + str(left).splitlines() + ['------'] + str(right).splitlines()


    if IS_ARCADIA:
        @pytest.fixture(scope='session')
        def worker_id():
            return 'master'


    @pytest.fixture(scope='session')
    def sd_stub(tmpdir_factory, worker_id):
        offset = worker_id_to_offset(worker_id)
        sd_stub = SdStub(port=SdStub.port + offset, httpbin_port=Httpbin.port + offset)
        sd_stub.start()
        yield sd_stub
        sd_stub.terminate()


    @pytest.fixture(scope='session')
    def httpbin(tmpdir_factory, worker_id, sd_stub):
        offset = worker_id_to_offset(worker_id)
        hb = Httpbin(port=Httpbin.port + offset)
        hb.start()
        yield hb
        hb.terminate()


    @pytest.fixture(scope='session')
    def vcr_config():
        # http://pytest-vcr.readthedocs.io/en/latest/#filtering-saved-requestresponse
        return {
            # Replace the Authorization request header with "DUMMY" in cassettes
            'filter_headers': [('authorization', 'DUMMY')],
        }


    @pytest.fixture(scope='session')
    def test_config_path(request):
        if IS_ARCADIA:
            return 'deps/cfg_test.yml'
        if request.config.option.config:
            return request.config.option.config
        else:
            return './cfg_test.yml'


    @pytest.fixture(scope='session')
    def balancer_executable_path(request):
        if IS_ARCADIA:
            from yatest import common
            return common.build_path('infra/awacs/vendor/awacs/tests/deps/balancer')
        return request.config.option.balancer or pexpect.which('balancer')


    @pytest.fixture(scope='session')
    def zk_prefix(worker_id):
        return '/awacs_test_{}/'.format(worker_id)


    @pytest.fixture(scope='session')
    def initial_test_config(test_config_path):
        if IS_ARCADIA:
            path = 'deps/cfg_default.yml'
        else:
            from infra.swatlib.cmdutil import DEFAULT_CONFIG_PATH
            path = DEFAULT_CONFIG_PATH
        app_config.load(test_config_path, defaults=path)
        app_config.set_value('run.auth', False)
        return copy.deepcopy(app_config._CONFIG)


    @pytest.fixture(autouse=True)
    def test_config(initial_test_config):
        app_config._CONFIG = copy.deepcopy(initial_test_config)


    @pytest.fixture(scope='session')
    def port_manager(request):
        if IS_ARCADIA:
            from yatest.common import network
            yield network.PortManager()
        else:
            yield None


    @pytest.fixture(scope='session')
    def zk_session_connection(request, tmpdir_factory, worker_id, zk_prefix, initial_test_config, port_manager):
        if IS_ARCADIA:
            from yatest import common
            import os.path
            import tarfile
            import hashlib

            def untar(file_path, path='.'):
                tar = tarfile.open(file_path)
                tar.extractall(path)
                tar.close()

            uniq = hashlib.md5(os.getcwd().encode("utf-8")).hexdigest()
            # zookeeper_dir_path = os.path.join(os.getcwd(), uniq)
            zookeeper_dir_path = common.output_path(uniq)
            try:
                os.makedirs(zookeeper_dir_path)
            except OSError:
                pass
            zookeeeper_tgz_path = common.build_path('infra/awacs/vendor/awacs/tests/deps/zookeeper.tar.gz')
            untar(zookeeeper_tgz_path, path=zookeeper_dir_path)
            zookeeper_dir_path = os.path.join(zookeeper_dir_path, 'zookeeper-3.4.6')
        else:
            assert request.config.option.zookeeper
            zookeeper_dir_path = request.config.option.zookeeper
        with xdist.run_once(tmpdir_factory, worker_id,
                            config_name='zk_config.json',
                            config=dict(app_config.get_value('coord'))) as (zk_config, already_running):
            if not already_running:
                server = zookeepermock.ZooKeeper(zookeeper_dir_path, port_manager=port_manager)
                xdist.register_cleanup(request, worker_id, server.disconnect)
                zk_config['hosts'] = server.hosts
        zk_config['zk_root'] = zk_prefix
        zk_identifier = '{}:{}:{}'.format(socket.gethostname(), app_config.get_value('web.http.port'), worker_id)
        zk_client = zookeeper_client.ZookeeperClient(zk_config, identifier=zk_identifier,
                                                     metrics=metrics.ROOT_REGISTRY)
        zk_client.start().wait()
        kazoo_logger = zk_client.client.logger
        kazoo_logger.setLevel(logging.WARN)
        yield zk_client
        zk_client.stop()
        zk_client.close()


    @pytest.fixture
    def zk(zk_session_connection):
        try:
            children = zk_session_connection.get_children('/')
        except NoNodeError:
            pass
        else:
            for child in children:
                zk_session_connection.delete_file(child, recursive=True)
        yield zk_session_connection


    @pytest.fixture
    def cache(zk, zk_prefix):
        L7HeavyConfig.cache.clear()
        rv = c.AwacsCache(zk_client=zk, path=zk_prefix, enable_extended_signals=True, proxy_counters_cache_ttl=0,
                          structure=zks.construct_full_zk_structure())
        rv.start()
        yield rv
        rv.stop()


    @pytest.fixture
    def apicache(zk):
        return c2.AwacsApiCache()


    @pytest.fixture
    def mongo_storage(mongo_connection):
        return db.MongoStorage()


    @pytest.fixture
    def zk_storage(zk, zk_prefix):
        return ZkStorage(zk, prefix=zk_prefix)


    @pytest.fixture(scope='session')
    def mongo_session_connection(request, tmpdir_factory, worker_id):
        if IS_ARCADIA:
            from yatest import common
            os.environ['PATH'] += os.pathsep + common.build_path('infra/awacs/vendor/awacs/tests/deps/')
        with xdist.run_once(tmpdir_factory, worker_id,
                            config_name='mongodb_config.json',
                            config={}) as (mongodb_config, already_running):
            if not already_running:
                mongodb = mongomock.MongoDb()
                xdist.register_cleanup(request, worker_id, mongodb.kill)
                mongodb_config['host'] = mongodb.host
        yield mongo.connect(worker_id, host=mongodb_config['host'])
        mongo.disconnect()


    @pytest.fixture
    def mongo_connection(worker_id, mongo_session_connection):
        connection = mongo_session_connection
        mongodb = connection[worker_id]
        for collection_name in mongodb.collection_names(include_system_collections=False):
            mongodb[collection_name].remove({})
        yield connection


    @pytest.fixture
    def dao(zk_storage, mongo_storage, cache):
        return Dao(zk_storage, mongo_storage, cache)


    @pytest.fixture(scope='session', autouse=True)
    def log():
        console_handler = setup_logging_to_stdout(ApplicationBase.LOG_FORMAT)
        setup_logging({'loglevel': 'DEBUG'}, console_handler, console=1,
                      fmt=ApplicationBase.LOG_FORMAT, filter_=ApplicationBase.LOG_FILTER)
        yield
        logging.getLogger().removeHandler(console_handler)


    @pytest.fixture
    def create_default_namespace(zk_storage, cache):
        return lambda namespace_id: create_namespace(zk_storage, cache, namespace_id)


    @pytest.fixture
    def nanny_rpc_mock_client():
        return NannyRpcMockClient()


    @pytest.fixture
    def yp_lite_mock_client():
        return YpLiteMockClient()


    @pytest.fixture
    def abc_client():
        return AbcMockClient()


    @pytest.fixture
    def l3_mgr_client():
        return L3MgrMockClient()


    @pytest.fixture
    def dns_resolver_mock():
        return DnsResolverMock()


    @pytest.fixture
    def dns_manager_client_mock():
        return DnsManagerClientMock()


    @pytest.fixture
    def binder(mongo_storage, zk_storage, zk, zk_prefix, cache, apicache, dao, log):
        def configure(b):
            model_classes.ModelZkClient.awtest_set_zk_prefix(zk_prefix.rstrip('/'))
            b.bind(db.IMongoStorage, mongo_storage)
            b.bind(zookeeper_client.IZookeeperClient, zk)
            b.bind(IZkStorage, zk_storage)
            b.bind(c.IAwacsCache, cache)
            b.bind(c2.IAwacsApiCache, apicache)
            b.bind(IDao, dao)
            b.bind(racktables.IRacktablesClient, RacktablesMockClient())

        return configure


    @pytest.fixture
    def binder_with_nanny_client(binder):
        def configure(b):
            b.bind(nannyclient.INannyClient, NannyMockClient(url='https://nanny.yandex-team.ru/v2/', token='DUMMY'))
            binder(b)

        return configure


    @pytest.fixture
    def ctlrunner(zk):
        r = CtlRunner(zk)
        yield r
        r.stop()


    @pytest.fixture
    def ctx():
        return context.BackgroundCtx().with_op(op_id='test-op', log=logging.getLogger('awacs-tests'))


    @pytest.fixture(scope='session', autouse=True)
    def flask_app(initial_test_config):
        app = create_app(name='awacsd',
                         hostname='localhost',
                         version='0.0.1',
                         version_timestamp=int(time.time()))
        with app.test_request_context():
            yield app


    @pytest.fixture(autouse=True)
    def clean_metrics_registry():
        metrics.ROOT_REGISTRY.clear()


    @pytest.fixture
    def enable_auth():
        prev_value = app_config.get_value('run.auth', False)
        app_config.set_value('run.auth', True)
        yield
        app_config.set_value('run.auth', prev_value)


    _checker = Checker()


    @pytest.fixture
    def checker():
        return _checker

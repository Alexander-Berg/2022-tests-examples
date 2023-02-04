import pymongo
import pytest
import yatest

from maps.infra.ecstatic.mongo.lib.mongo_init import init_mongo
from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import run_reconfiguration, ReconfigurationStage
from maps.pylibs.local_mongo import MongoServer


@pytest.fixture(scope='function')
def mongo():
    server = MongoServer.create_instance()
    init_mongo(mongo_uri=server.uri)
    yield server
    server.clear()


@pytest.fixture(scope='function')
def mongo_client(mongo):
    return pymongo.MongoClient(f'{mongo.uri}/ecstatic').get_database()


@pytest.fixture(scope='function', autouse=True)
def autoreconfigure(reconfigurer):
    reconfigurer()


@pytest.fixture(scope='function')
def reconfigurer(mongo):
    def reconfigure(
            storages_config='[{"hosts": ["storage11", "storage12", "storage13"]}]',
            hosts_config='data/host-groups.json',
            config='data/ecstatic.conf',
            installation='unstable',
            config_revision=1,
            reconfiguration_stage=ReconfigurationStage.UPLOAD):
        run_reconfiguration(
            stage=reconfiguration_stage,
            sharding_config=storages_config,
            mongo_uri=f'{mongo.uri}/ecstatic',
            installation=installation,
            stub_hosts_file=yatest.common.test_source_path(hosts_config),
            files=[yatest.common.test_source_path(config)],
            wait_for_quorums_calculation=False,
            revision=config_revision,
        )

    return reconfigure

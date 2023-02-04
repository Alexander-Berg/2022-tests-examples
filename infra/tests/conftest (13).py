import mock
import mongomock
import pytest
import yaml
import yatest.common

import infra.dist.cacus.lib.common
from infra.dist.cacus.lib import loader
from infra.dist.cacus.lib.dbal import mongo_connection


# initialize config
@pytest.fixture(autouse=True)
def mock_common(request, monkeypatch):
    import infra.dist.cacus.lib.common
    with open(yatest.common.source_path('infra/dist/cacus/tests/cacus.yaml')) as f:
        infra.dist.cacus.lib.common.config = yaml.safe_load(f)

    monkeypatch.setattr(loader, 'get_plugin',
                        mock.Mock(side_effect=RuntimeError('MDS access should be overridden in tests locally')))

    def _fin():
        infra.dist.cacus.lib.common.config = None

    request.addfinalizer(_fin)


@pytest.fixture(autouse=True)
def mongo_mock(request):
    mongo_connection.connections[mongo_connection.CACUS] = mongo_connection.MongoDatabaseConnection(
        mongo_connection.CACUS,
        'cacus',
        'uri-mock',
        mongomock.MongoClient
    )
    mongo_connection.connections[mongo_connection.REPOS] = mongo_connection.MongoDatabaseConnection(
        mongo_connection.REPOS,
        'repos',
        'uri-mock',
        mongomock.MongoClient
    )
    mongo_connection.connections[mongo_connection.HISTORY] = mongo_connection.MongoDatabaseConnection(
        mongo_connection.HISTORY,
        'cacus_history',
        'uri-mock',
        mongomock.MongoClient,
        config=infra.dist.cacus.lib.common.config['metadb'][mongo_connection.HISTORY]
    )

    def _fin():
        mongo_connection.connections = {}

    request.addfinalizer(_fin)

import copy
import pytest
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import deleted_key


KEYS = [
    "1008557/yandex-cloud/testing/all/Packages.bz2_41ac76607b93c9e00ee13bb23d894838",
    "2493082/verstka/testing/all/Packages.gz_316114652817dbd11658a0b956230db8",
    "741928/mdb-bionic/stable/source/Sources.bz2_6ee9666e5b39f3a4e955fe7854aa1239",
    "741928/mail-trusty/unstable/amd64/Packages.bz2_5169e1510c110a6739a1e7caecf35e28",
    "1392806/mail-trusty/unstable/source/Sources.bz2_9e335b3da301f7c15841c6e5963865a3"
]


@pytest.fixture
def mock_db():
    mongo_connection.cacus()['__to_delete__'].insert_one({'name': 'failed_to_delete', 'keys': copy.deepcopy(KEYS)})


@pytest.mark.usefixtures("mock_db")
def test_all():
    rv = deleted_key.default_store.all()
    assert len(rv) == len(KEYS)
    assert sorted(rv) == sorted(KEYS)


@pytest.mark.usefixtures("mock_db")
def test_push():
    deleted_key.default_store.push("mock")
    assert "mock" in deleted_key.default_store.all()


@pytest.mark.usefixtures("mock_db")
def test_pop():
    deleted_key.default_store.pop(KEYS[0])
    assert KEYS[0] not in deleted_key.default_store.all()

from intranet.search.core.storages import StageStatusStorage, LocalStageStatusStorage
from intranet.search.core.utils import generate_prefix, parse_prefix
from intranet.search.core.models import StageStatus

import pytest


pytestmark = pytest.mark.django_db


@pytest.fixture
def global_storage():
    return StageStatusStorage(1, 1)


@pytest.fixture
def local_storage():
    return LocalStageStatusStorage(1, 1)


def global_status_state(global_storage):
    global_storage.create('setup', 'global')
    global_storage.create('walk', 'global')
    global_storage.create('walk', 'global')
    global_storage.create('walk', 'global', 'in progress')
    global_storage.create('walk', 'global', 'done')
    global_storage.create('walk', 'global', 'fail')


@pytest.fixture(name='global_status_state')
def global_status_state_fixture(global_storage):
    return global_status_state(global_storage)


@pytest.fixture
def local_status_state(local_storage):
    local_storage.create('fetch', 'local')
    local_storage.create('create', 'local')
    local_storage.create('store', 'local')
    local_storage.create('store', 'local')
    local_storage.create('store', 'local', 'in progress')
    local_storage.create('store', 'local', 'done')
    local_storage.create('store', 'local', 'cancel')


def test_generate_prefix():
    assert '1:2:3' == generate_prefix(':', ['one', 'two', 'three'], one=1, two=2, three=3)
    assert '1:3' == generate_prefix(':', ['one', 'two', 'three'], one=1, three=3)

    with pytest.raises(AssertionError):
        generate_prefix(':', ['one', 'two'], one=1, three=3)


def test_parse_prefix():
    assert {'one': '1', 'two': '2', 'three': '3'} == parse_prefix(':', ['one', 'two', 'three'], '1:2:3')


def test_global_create_status(global_storage):
    id_ = global_storage.create('store', 'global')

    assert id_.startswith('1:1:store:')


def test_global_get_indexations():
    StageStatusStorage(1, 2).create('walk', 'global')
    StageStatusStorage(1, 3).create('walk', 'global')
    StageStatusStorage(2, 4).create('walk', 'global')
    indexations = StageStatusStorage.get_indexations()

    assert indexations == {('1', '2'), ('1', '3'), ('2', '4')}


def test_global_get_stats(global_storage, global_status_state):
    assert StageStatus.objects.count() == 6

    stats = global_storage.get_stats()

    for key, value in [(('setup', 'new'), 1),
                       (('walk', 'new'), 2),
                       (('walk', 'in progress'), 1),
                       (('walk', 'done'), 1),
                       (('walk', 'fail'), 1)]:
        assert key in stats
        assert stats[key] == value, key

    # долны остаться записи про статусы не в терминальных состояниях
    assert StageStatus.objects.count() == 4


def test_global_get_stats_collapse(global_storage):
    id_ = global_storage.create('walk', 'global')
    global_storage.start(id_)
    global_storage.succeed(id_)

    id_ = global_storage.create('walk', 'global')
    global_storage.start(id_)
    global_storage.fail(id_)

    stats = global_storage.get_stats()

    for key, value in [(('walk', 'fail'), 1),
                       (('walk', 'done'), 1)]:
        assert key in stats
        assert stats[key] == value, key


def test_local_create(local_storage):
    id_ = local_storage.create('store', 'local')

    assert id_.startswith('store.')


def test_local_get_stats(local_storage, local_status_state, mock_redis):
    assert mock_redis.llen(local_storage.key) == 7

    stats = local_storage.get_stats()

    for key, value in [(('create', 'new'), 1),
                       (('fetch', 'new'), 1),
                       (('store', 'done'), 1),
                       (('store', 'in progress'), 1),
                       (('store', 'new'), 2),
                       (('store', 'cancel'), 1)]:
        assert key in stats
        assert stats[key] == value, key

    assert mock_redis.llen(local_storage.key) == 5


def test_local_get_stats_with_global(local_storage, local_status_state):
    global_status_state(local_storage)

    stats = local_storage.get_stats(with_global=True)

    for key, value in [(('setup', 'new'), 1),
                       (('walk', 'new'), 2),
                       (('walk', 'in progress'), 1),
                       (('walk', 'done'), 1),
                       (('walk', 'fail'), 1),
                       (('create', 'new'), 1),
                       (('fetch', 'new'), 1),
                       (('store', 'done'), 1),
                       (('store', 'in progress'), 1),
                       (('store', 'new'), 2),
                       (('store', 'cancel'), 1)]:
        assert key in stats
        assert stats[key] == value, key


def test_local_get_stats_collapse(local_storage):
    id_ = local_storage.create('store', 'local')
    local_storage.start(id_)
    local_storage.succeed(id_)

    id_ = local_storage.create('store', 'local')
    local_storage.start(id_)
    local_storage.fail(id_)

    stats = local_storage.get_stats()

    for key, value in [(('store', 'fail'), 1),
                       (('store', 'done'), 1)]:
        assert key in stats
        assert stats[key] == value, key


def test_local_get_indexations():
    LocalStageStatusStorage(1, 2).create('store', 'local')
    LocalStageStatusStorage(1, 3).create('store', 'local')
    LocalStageStatusStorage(2, 4).create('store', 'local')
    indexations = LocalStageStatusStorage.get_indexations()

    assert indexations == {(1, 2), (1, 3), (2, 4)}

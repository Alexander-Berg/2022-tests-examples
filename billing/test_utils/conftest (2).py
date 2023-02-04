import pytest

from billing.library.python.yt_utils.test_utils.utils import (
    create_yt_client,
    fix_yt_logger,
)


__all__ = [
    'yt_client',
    'yt_root_fixture',
    'yt_transaction',
]


@pytest.fixture(scope='session', autouse=True)
def fix_yt_logger_fixture():
    fix_yt_logger()


@pytest.fixture(scope='session')
def yt_client():
    return create_yt_client()


@pytest.fixture(name='yt_root')
def yt_root_fixture(yt_client):
    # Все создаваемые в YT объекты сохраняются между запусками тестов, и,
    # скорее всего, все параллельные запуски тестов смотрят в один инстанс yt.
    # Поэтому создаем для каждого теста уникальную директорию, и все объекты
    # в тесте создаем относительно нее.
    path = yt_client.find_free_subpath('//')
    yt_client.create('map_node', path)
    return path


@pytest.fixture
def yt_transaction(yt_client):
    with yt_client.Transaction(ping=False) as tx:
        yield tx

import pytest

from maps_adv.billing_proxy.lib.data_manager.base import BaseDataManager
from maps_adv.billing_proxy.tests.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]

_returned_object = object()


@pytest.fixture
def db_mock():
    class DB:
        acquire = coro_mock()
        release = coro_mock()

    db = DB()
    db.acquire.coro.return_value = _returned_object

    return db


@pytest.fixture
def dm(db_mock):
    return BaseDataManager(db_mock)


async def test_acquires_connection(dm, db_mock):
    async with dm.connection(type="some") as con:
        assert con is _returned_object
        db_mock.acquire.assert_called_with("some")


async def test_releases_connection_if_no_exception_raised(dm, db_mock):
    async with dm.connection() as con:
        pass

    db_mock.release.assert_called_with(con)


async def test_releases_connection_if_exception_raised(dm, db_mock):
    with pytest.raises(Exception):
        async with dm.connection() as con:
            raise Exception

    db_mock.release.assert_called_with(con)


async def test_not_acquires_connection_if_parameter_passed(dm, db_mock):
    local_con = object()

    async with dm.connection(local_con) as con:
        assert con is local_con
        assert not db_mock.acquire.called


async def test_not_releases_connection_if_parameter_passed_and_no_exception_raised(
    dm, db_mock
):
    local_con = object()

    async with dm.connection(local_con):
        pass

    assert not db_mock.release.called


async def test_not_releases_connection_if_parameter_passed_and_exception_raised(
    dm, db_mock
):
    local_con = object()

    with pytest.raises(Exception):
        async with dm.connection(local_con):
            raise Exception

    assert not db_mock.release.called

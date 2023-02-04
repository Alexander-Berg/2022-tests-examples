import pytest

from intranet.trip.src.unit_of_work import UnitOfWork
from intranet.trip.src.db.gateways.base import RecordNotFound


pytestmark = pytest.mark.asyncio


class SomeException(Exception):
    pass


async def _create_holding(uow, holding_id: int):
    await uow.holdings.create(holding_id=holding_id)


async def test_uow_without_with(db_engine):
    holding_id = 1
    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        await _create_holding(uow, holding_id)

    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        holding = await uow.holdings.get_holding(holding_id)
        assert holding_id == holding.holding_id
        await uow.holdings.delete(holding_id)


async def test_uow_with_with(db_engine):
    holding_id = 1
    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        async with uow:
            await _create_holding(uow, holding_id)

    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        holding = await uow.holdings.get_holding(holding_id)
        assert holding_id == holding.holding_id
        await uow.holdings.delete(holding_id)


async def test_uow_rollback(db_engine):
    holding_id = 1
    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        try:
            async with uow:
                await _create_holding(uow, holding_id)
                raise SomeException
        except SomeException:
            pass

    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        with pytest.raises(RecordNotFound):
            await uow.holdings.get_holding(holding_id)


async def test_uow_nested_with_rollback(db_engine):
    holding_id = 1
    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)

        try:
            async with uow:
                async with uow:
                    await _create_holding(uow, holding_id)
                raise SomeException
        except SomeException:
            pass

    async with db_engine.acquire() as conn:
        uow = UnitOfWork(conn)
        with pytest.raises(RecordNotFound):
            await uow.holdings.get_holding(holding_id)

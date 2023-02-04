import asyncio
import json
import sys
import threading
import uuid

import pytest

from sendr_aiopg.engine.single import create_engine

from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.storage import Storage


@pytest.fixture(scope='session', autouse=True)
def db_event_loop():
    """
    Запустим event loop во вспомогательном потоке,
    чтобы генерировать session фикстуры для базы через нашу связку engine + storage.
    """

    def thread_logic(event_loop):
        asyncio.set_event_loop(event_loop)
        event_loop.run_until_complete(wait())

    loop = asyncio.new_event_loop()
    if sys.version_info >= (3, 10):
        stop_event = asyncio.Event()
    else:
        stop_event = asyncio.Event(loop=loop)

    async def wait():
        await stop_event.wait()

    thread = threading.Thread(target=thread_logic, args=(loop,), daemon=True)
    thread.start()

    yield loop

    def stop():
        stop_event.set()

    loop.call_soon_threadsafe(stop)

    thread.join(timeout=15)
    assert not thread.is_alive()


@pytest.fixture(scope='session')
def run_in_db_loop(db_event_loop):
    def run(coroutine):
        task = asyncio.run_coroutine_threadsafe(coroutine, loop=db_event_loop)

        event = threading.Event()
        task.add_done_callback(lambda fut: event.set())
        event.wait()

        return task.result()

    return run


@pytest.fixture(scope='session', autouse=True)
def db_engine(db_conn_params, run_in_db_loop):
    async def create():
        return await create_engine(**db_conn_params)

    db_engine = run_in_db_loop(create())

    yield db_engine

    db_engine.close()
    run_in_db_loop(db_engine.wait_closed())


@pytest.fixture(scope='session')
def db_conn_params():
    with open('pg_recipe.json') as f:
        return json.loads(f.read())


@pytest.fixture(scope='session')
def merchant_id():
    return uuid.uuid4()


@pytest.fixture(scope='session')
def psp_external_id():
    return 'test_psp'


@pytest.fixture(scope='session', autouse=True)
def merchant(db_engine, run_in_db_loop, merchant_id) -> Merchant:
    async def create():
        async with db_engine.acquire() as conn:
            storage = Storage(conn)
            return await storage.merchant.create(Merchant(merchant_id=merchant_id, name='some name'))

    return run_in_db_loop(create())


@pytest.fixture(autouse=True, scope='session')
def psp(db_engine, run_in_db_loop, psp_external_id):
    async def create():
        async with db_engine.acquire() as conn:
            storage = Storage(conn)
            return await storage.psp.create(PSP(
                psp_external_id=psp_external_id,
                public_key='bla',
                public_key_signature='bla',
                psp_id=uuid.uuid4(),
            ))

    return run_in_db_loop(create())


@pytest.fixture(autouse=True, scope='session')
def merchant_origin(db_engine, run_in_db_loop, merchant_id):
    canonized_origin = 'https://domain.ru:443'

    async def create():
        async with db_engine.acquire() as conn:
            storage = Storage(conn)
            return await storage.merchant_origin.create(MerchantOrigin(
                merchant_id=merchant_id,
                origin=canonized_origin,
            ))

    return run_in_db_loop(create())

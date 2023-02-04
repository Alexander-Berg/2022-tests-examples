import pytest
import asyncio


from ads_pytorch.tools.aioretry import aioretry


@pytest.mark.asyncio
async def test_catch_retry():
    counter = 0

    @aioretry(ValueError, retries=5, cooldown=0)
    async def _foo():
        nonlocal counter
        counter += 1
        if counter < 2:
            raise ValueError

    await _foo()
    assert counter == 2


@pytest.mark.asyncio
async def test_missing_exception():
    counter = 0

    @aioretry(KeyError, retries=5, cooldown=0)
    async def _foo():
        nonlocal counter
        counter += 1
        if counter < 2:
            raise ValueError

    with pytest.raises(ValueError):
        await _foo()
    assert counter == 1


@pytest.mark.asyncio
async def test_retry_count_exceeced():
    counter = 0

    @aioretry(ValueError, retries=5, cooldown=0)
    async def _foo():
        nonlocal counter
        counter += 1
        raise ValueError

    with pytest.raises(ValueError):
        await _foo()

    # one call + 5 retries
    assert counter == 6


class MyExc(Exception):
    pass


class MyBaseExc(BaseException):
    pass


@pytest.mark.asyncio
async def test_default_exceptions_retry():
    base_counter = 0
    usual_counter = 0

    @aioretry(retries=5, cooldown=0)
    async def _base_foo():
        nonlocal base_counter
        base_counter += 1
        raise MyBaseExc

    @aioretry(retries=5, cooldown=0)
    async def _usual_foo():
        nonlocal usual_counter
        usual_counter += 1
        raise MyExc

    with pytest.raises(BaseException):
        await _base_foo()
    with pytest.raises(BaseException):
        await _usual_foo()

    assert base_counter == 1
    assert usual_counter == 6


@pytest.mark.asyncio
async def test_cancellation():
    locked = 0

    async def external_foo():
        @aioretry(ValueError, retries=5, cooldown=0)
        async def inner_foo():
            nonlocal locked
            try:
                locked = 1
                await asyncio.sleep(100500)
            finally:
                locked = 2
        return await inner_foo()

    future = asyncio.create_task(external_foo())
    await asyncio.sleep(0.01)
    assert not future.done()
    future.cancel()
    await asyncio.sleep(0.01)

    with pytest.raises(asyncio.CancelledError):
        await future

    assert locked == 2


@pytest.mark.asyncio
async def test_callback():
    counter = 0

    def callback(ex: ValueError):
        return "RETRY" in str(ex)

    @aioretry(ValueError, retries=5, cooldown=0, callback=callback)
    async def _foo():
        nonlocal counter
        counter += 1
        if counter < 2:
            raise ValueError("RETRY")

    await _foo()
    assert counter == 2

    counter = 0

    @aioretry(ValueError, retries=5, cooldown=0, callback=callback)
    async def _foo():
        nonlocal counter
        counter += 1
        if counter < 2:
            raise ValueError("OPS")

    with pytest.raises(ValueError):
        await _foo()
    assert counter == 1

import asyncio
import datetime
import typing as tp


class RetryError(BaseException):
    pass


async def poll(awaitable: tp.Callable[[], tp.Awaitable], interval_seconds: float, timeout_seconds: float) -> tp.Any:
    now = start_time = datetime.datetime.now()
    while (now - start_time).seconds < timeout_seconds:
        try:
            return await awaitable()
        except RetryError:
            pass

        await asyncio.sleep(interval_seconds)
        now = datetime.datetime.now()
    raise TimeoutError("exceeded polling timeout")

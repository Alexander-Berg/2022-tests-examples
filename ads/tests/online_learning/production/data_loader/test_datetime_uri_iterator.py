from typing import List

import pytest
import asyncio
import os
import itertools
import datetime
import contextlib

from ads_pytorch.online_learning.production.data_loader.datetime_uri_iterator import (
    DatetimeUriIterable,
    DatetimeURI
)
from ads_pytorch.online_learning.production.data_loader.metrics_interface import (
    AbstractDatetimeUtiIterableMetricsSender
)
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock


@contextlib.asynccontextmanager
async def make_folder(start: datetime.datetime, count: int):
    adapter = CypressAdapterMock()
    datetime_mask = "%Y-%m-%d-%H"
    async with adapter.temporary_directory() as tmp:
        for i in range(count):
            date_str = (start + datetime.timedelta(hours=i)).strftime(datetime_mask)
            await adapter.create_table(adapter.path_join(tmp, date_str))

        yield tmp, datetime_mask


##################################################
#                  DEATH TESTS                   #
##################################################


@pytest.mark.asyncio
async def test_scheduler_died_with_filled_cache():
    count = 48
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01
        )
        iterable.__aiter__()
        [await iterable.__anext__() for _ in range(count // 2)]
        iterable._task.cancel()
        await asyncio.wait([iterable._task])
        await asyncio.sleep(0.02)
        with pytest.raises(asyncio.CancelledError):
            await iterable.__anext__()


@pytest.mark.asyncio
async def test_scheduler_dies_empty_cache():
    count = 48
    adapter = CypressAdapterMock()
    async with adapter.temporary_directory() as folder:
        datetime_mask = "%Y-%m"
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01
        )
        iterable.__aiter__()
        task = asyncio.create_task(iterable.__anext__())
        await asyncio.sleep(0.02)
        assert not task.done()
        iterable._task.cancel()
        await asyncio.wait([iterable._task])
        await asyncio.sleep(0.02)
        assert task.done()
        with pytest.raises(asyncio.CancelledError):
            await task


@pytest.mark.asyncio
async def test_scheduler_dies_filled_cache_middle_iteration():
    count = 48
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01
        )
        iterable.__aiter__()
        [await iterable.__anext__() for _ in range(count // 2)]

        iterable._task.cancel()
        await asyncio.wait([iterable._task])
        await asyncio.sleep(0.02)
        with pytest.raises(asyncio.CancelledError):
            await iterable.__anext__()


@pytest.mark.asyncio
async def test_scheduler_dies_filled_cache_waiting_for_hole():
    count = 48
    adapter = CypressAdapterMock()
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 11) - datetime.timedelta(hours=1),
            scheduler_frequency=0.01,
            uri_timedelta=datetime.timedelta(hours=1)
        )
        iterable.__aiter__()
        [await iterable.__anext__() for _ in range(count)]

        # This will create a small hole
        next_date = datetime.datetime(2019, 9, 14)
        assert datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=1) * count < next_date < datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=1) * (count + count * 2)
        date_str = next_date.strftime(datetime_mask)
        await adapter.create_table(path=adapter.path_join(folder, date_str))

        task = asyncio.create_task(iterable.__anext__())
        await asyncio.sleep(0.02)
        assert not task.done()
        iterable._task.cancel()
        await asyncio.wait([iterable._task])
        await asyncio.sleep(0.02)
        assert task.done()
        with pytest.raises(asyncio.CancelledError):
            await task


############################################################
#                       ITERATION TESTS                    #
############################################################


@pytest.mark.asyncio
async def test_skip_until_date_with_hole():
    count = 48
    uri_timedelta = datetime.timedelta(hours=1)
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01,
            uri_timedelta=uri_timedelta
        )
        iterable.__aiter__()
        task = asyncio.create_task(iterable.__anext__())
        await asyncio.sleep(0.02)
        assert not task.done()

        # Fill the hole and check we started iteration
        adapter = CypressAdapterMock()
        for i in range(24):
            date_str = (datetime.datetime(2019, 9, 10) + datetime.timedelta(hours=i)).strftime(datetime_mask)
            await adapter.create_table(adapter.path_join(folder, date_str))
        await asyncio.sleep(0.02)
        assert task.done()
        res = await task
        date = datetime.datetime(2019, 9, 10, 1)
        assert res.uri == DatetimeURI(
            uri=adapter.path_join(folder, date.strftime(datetime_mask)),
            date=date
        )


@pytest.mark.asyncio
async def test_start_skip():
    count = 48
    uri_timedelta = datetime.timedelta(hours=1)
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 11, 23),
            scheduler_frequency=0.01,
            uri_timedelta=uri_timedelta
        )
        iterable.__aiter__()
        uris = [await iterable.__anext__() for _ in range(24)]
        for i, uri in enumerate(uris):
            assert uri.uri.date == datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=i + 24)


@pytest.mark.asyncio
async def test_force_skip():
    count = 48
    uri_timedelta = datetime.timedelta(hours=1)
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=24,
            log_skip_limit=24,
            skip_until_date=datetime.datetime(2019, 9, 10, 23),
            scheduler_frequency=0.01,
            uri_timedelta=uri_timedelta
        )
        iterable.__aiter__()
        uris = [await iterable.__anext__() for _ in range(48)]
        for i, uri in enumerate(uris):
            if i <= 24:
                assert uri.force_skip
            else:
                assert not uri.force_skip
            assert uri.uri.date == datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=i)


@pytest.mark.asyncio
async def test_force_skip_hole():
    # We have to check that uri which will fill our hole won't be yielded
    # after we run in future
    uri_timedelta = datetime.timedelta(hours=1)
    adapter = CypressAdapterMock()
    async with make_folder(datetime.datetime(2019, 9, 11), 4) as (folder, datetime_mask):
        # make a hole for 3 hours
        for i in range(10):
            date_str = (datetime.datetime(2019, 9, 11, 7 + i)).strftime(datetime_mask)
            await adapter.create_table(adapter.path_join(folder, date_str))

        iterable = DatetimeUriIterable(
            fs=adapter,
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=2,
            log_skip_limit=2,
            skip_until_date=datetime.datetime(2019, 9, 10, 23),
            scheduler_frequency=0.01,
            uri_timedelta=uri_timedelta
        )
        iterable.__aiter__()
        uris = [await iterable.__anext__() for _ in range(14)]
        for i, uri in enumerate(uris[:4]):
            assert uri.force_skip
            assert uri.uri.date == datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=i)

        for i, uri in enumerate(uris[4:]):
            assert uri.uri.date == datetime.datetime(2019, 9, 11, 7) + datetime.timedelta(hours=i)

        # okay, we've successfully skipped the hole. Let's fill it and see whether we yield it

        for i in range(3):
            date_str = (datetime.datetime(2019, 9, 11, 4 + i)).strftime(datetime_mask)
            await adapter.create_table(adapter.path_join(folder, date_str))

        new_uri_task = asyncio.create_task(iterable.__anext__())
        await asyncio.sleep(0.02)
        assert not new_uri_task.done()
        last_date = datetime.datetime(2019, 9, 11, 17)
        date_str = last_date.strftime(datetime_mask)
        await adapter.create_table(adapter.path_join(folder, date_str))
        await asyncio.sleep(0.02)

        new_uri = await new_uri_task
        assert new_uri.uri == DatetimeURI(
            uri=adapter.path_join(folder, date_str),
            date=last_date
        )
        assert not new_uri.force_skip


def log_date_iter(start_log_date):
    while True:
        start_log_date += datetime.timedelta(hours=1)
        yield start_log_date


@pytest.mark.parametrize('log_skip_limit', [1, 3, 5])
@pytest.mark.asyncio
async def test_log_skip_limit(log_skip_limit):
    uri_timedelta = datetime.timedelta(hours=1)
    adapter = CypressAdapterMock()
    async with make_folder(datetime.datetime(2019, 9, 11), 4) as (folder, datetime_mask):
        iterable = DatetimeUriIterable(
            fs=adapter,
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=100,
            log_skip_limit=log_skip_limit,
            skip_until_date=datetime.datetime(2019, 9, 10, 23),
            scheduler_frequency=0.01,
            uri_timedelta=uri_timedelta
        )
        iterable.__aiter__()

        [await iterable.__anext__() for _ in range(4)]
        assert len(iterable._cache) == 0

        current_last_log = log_date_iter(datetime.datetime(2019, 9, 11, 3))

        # 1 hour hole
        next(current_last_log)
        for i in range(1, log_skip_limit - 1):  # add (log_skip_limit - 2) logs after hole of size 1 (it should not be processed right now)
            await adapter.create_table(adapter.path_join(folder, next(current_last_log).strftime(datetime_mask)))
            with pytest.raises(asyncio.TimeoutError):
                await asyncio.wait_for(iterable.__anext__(), timeout=0.1)
            assert len(iterable._cache) == i

        # add one more log, last log will be at log_skip_limit distance from skip_until_date and should be processed
        await adapter.create_table(adapter.path_join(folder, next(current_last_log).strftime(datetime_mask)))
        [await asyncio.wait_for(iterable.__anext__(), timeout=0.1) for _ in range(log_skip_limit - 1)]

        # hole of size log_skip_limit
        [next(current_last_log) for _ in range(log_skip_limit)]
        await adapter.create_table(adapter.path_join(folder, next(current_last_log).strftime(datetime_mask)))
        await asyncio.wait_for(iterable.__anext__(), timeout=0.1)

        # hole of size 10 * log_skip_limit
        [next(current_last_log) for _ in range(10 * log_skip_limit)]
        await adapter.create_table(adapter.path_join(folder, next(current_last_log).strftime(datetime_mask)))
        await asyncio.wait_for(iterable.__anext__(), timeout=0.1)


###########################################
#              REAL ITERATION             #
###########################################

# complex tests with different cases in sequence


@pytest.mark.asyncio
async def test_iteration_with_newly_added_data():
    count = 48
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        adapter = CypressAdapterMock()
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01
        )

        async def _add_values(new_count):
            start = datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=count)
            for i in range(new_count):
                date_str = (start + datetime.timedelta(hours=i)).strftime(datetime_mask)
                await adapter.create_table(adapter.path_join(folder, date_str))
                await asyncio.sleep(0.03)

        async def _get_values(total_count):
            counter = 0
            async for x in iterable:
                yield x
                counter += 1
                if counter == total_count:
                    break

        new_count = 50
        asyncio.create_task(_add_values(new_count))
        uris = []
        async for x in _get_values(count + new_count):
            uris.append(x)

        for i, uri in enumerate(uris):
            assert uri.uri.date == datetime.datetime(2019, 9, 11) + datetime.timedelta(hours=i)


############################################################
#                        METRICS SENDER                    #
############################################################


class RememberSender(AbstractDatetimeUtiIterableMetricsSender):
    def __init__(self):
        self.call_update_uri = []
        self.call_yield_new = []
        self.call_scheduler_died = []
        self.call_waiting = []

    async def update_uri(self, new_uri: List[DatetimeURI]):
        self.call_update_uri.append(new_uri)

    async def yield_new(self, prod_uri: ProdURI):
        self.call_yield_new.append(prod_uri)

    async def scheduler_died(self, ex: BaseException):
        self.call_scheduler_died.append(ex)

    async def waiting(self, lag: datetime.timedelta):
        self.call_waiting.append(lag)


@pytest.mark.asyncio
async def test_scheduler_died_metrics():
    count = 48
    async with make_folder(datetime.datetime(2019, 9, 11), count) as (folder, datetime_mask):
        metrics_sender = RememberSender()
        iterable = DatetimeUriIterable(
            fs=CypressAdapterMock(),
            folder=folder,
            datetime_regex=r"REGEX_WHICH_DEFINETELY_FAIL",
            datetime_mask=datetime_mask,
            force_skip_limit=count * 2,
            log_skip_limit=count * 2,
            skip_until_date=datetime.datetime(2019, 9, 10),
            scheduler_frequency=0.01,
            metrics_sender=metrics_sender
        )

        with pytest.raises(RuntimeError):
            async for _ in iterable:
                pass

        assert len(metrics_sender.call_scheduler_died) == 1
        assert isinstance(metrics_sender.call_scheduler_died[0], RuntimeError)


@pytest.mark.asyncio
async def test_scheduler_wait_data_metrics():
    metrics_sender = RememberSender()

    lag = datetime.timedelta(hours=5)
    current_date = datetime.datetime.now() - lag

    async def _iter():
        # 0 uris to enforce iterable to wait
        async with make_folder(current_date + datetime.timedelta(days=1), 0) as (folder, datetime_mask):
            iterable = DatetimeUriIterable(
                fs=CypressAdapterMock(),
                folder=folder,
                datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
                datetime_mask=datetime_mask,
                force_skip_limit=5,
                log_skip_limit=5,
                skip_until_date=current_date,
                scheduler_frequency=0.01,
                metrics_sender=metrics_sender
            )

            async for _ in iterable:
                pass

    loop = asyncio.get_running_loop()
    loop.create_task(_iter())

    await asyncio.sleep(0.1)

    assert len(metrics_sender.call_waiting) > 9  # 0.1 sleep and 0.01 frequency
    assert all([x >= lag for x in metrics_sender.call_waiting])

    # We did not yield anything - test it in metrics
    assert len(metrics_sender.call_yield_new) == 0

    # We did not receive new uris - test it
    assert all(x == [] for x in metrics_sender.call_update_uri)


@pytest.mark.parametrize('force_skip', [True, False], ids=['ForceSkip', 'Usual'])
@pytest.mark.asyncio
async def test_scheduler_wait_and_yield_one_metrics(force_skip):
    metrics_sender = RememberSender()

    lag = datetime.timedelta(hours=5)
    current_date = datetime.datetime.now() - lag

    async with make_folder(current_date + datetime.timedelta(days=1), 0) as (folder, datetime_mask):
        fs = CypressAdapterMock()
        iterable = DatetimeUriIterable(
            fs=fs,
            folder=folder,
            datetime_regex=r"\d{4}-\d{2}-\d{2}-\d{2}",
            datetime_mask=datetime_mask,
            force_skip_limit=(0 if force_skip else 10),
            log_skip_limit=(0 if force_skip else 10),
            skip_until_date=current_date,
            scheduler_frequency=0.01,
            metrics_sender=metrics_sender
        )

        async def _iter():
            async for _ in iterable:
                pass

        loop = asyncio.get_running_loop()
        loop.create_task(_iter())

        await asyncio.sleep(0.03)

        assert len(metrics_sender.call_yield_new) == 0
        assert all(x == [] for x in metrics_sender.call_update_uri)

        newdate = datetime.datetime.now()
        newpath = fs.path_join(folder, newdate.strftime(datetime_mask))
        newuri = DatetimeURI(uri=newpath, date=datetime.datetime.strptime(newdate.strftime(datetime_mask), datetime_mask))
        await fs.create_table(path=newpath)
        await asyncio.sleep(0.03)

        assert len(metrics_sender.call_yield_new) == 1
        assert metrics_sender.call_yield_new[0] == ProdURI(uri=newuri, force_skip=force_skip)
        assert sum(x == [newuri] for x in metrics_sender.call_update_uri) == 1
        assert sum(x == [] for x in metrics_sender.call_update_uri) == len(metrics_sender.call_update_uri) - 1

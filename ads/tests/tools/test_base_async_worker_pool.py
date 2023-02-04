import asyncio

import pytest
import dataclasses
from ads_pytorch.tools.async_worker_pool import (
    BaseAsyncWorkerPool,
    BaseAsyncWorker,
    DeadPool,
    WorkerPoolHasNotStarted,
    WorkerPoolHasAlreadyStarted
)


class MySimpleWorker(BaseAsyncWorker):
    def __init__(self, recorder, rank):
        super(MySimpleWorker, self).__init__(rank)
        self.call_counter = 0
        self.recorder = recorder

    async def _do_job(self, x):
        self.call_counter += 1
        self.recorder.append(x)
        return x


class MyWorkPool(BaseAsyncWorkerPool):
    def __init__(self, recorder, workers_count=1):
        self.recorder = recorder
        super(MyWorkPool, self).__init__(workers_count=workers_count)

    def create_new_worker(self, new_rank: int):
        return MySimpleWorker(self.recorder, new_rank)


class ToCatch(Exception):
    pass


#########################################################
#           API EXCEPTIONS: NOT STARTED / DEAD          #
#########################################################


@pytest.mark.asyncio
async def test_api_not_started():
    pool = MyWorkPool(list(), 5)
    with pytest.raises(WorkerPoolHasNotStarted):
        await pool.assign_job(1)
    with pytest.raises(WorkerPoolHasNotStarted):
        await pool.kill(RuntimeError())
    with pytest.raises(WorkerPoolHasNotStarted):
        await pool.join()
    with pytest.raises(WorkerPoolHasNotStarted):
        await pool.terminate_jobs()
    with pytest.raises(WorkerPoolHasNotStarted):
        await pool.ensure_set_worker_count(5)


@pytest.mark.asyncio
async def test_api_dead():
    pool = MyWorkPool(list(), 5)

    async def _helper():
        async with pool:
            pool.kill(ToCatch())
            with pytest.raises(DeadPool):
                await pool.assign_job(1)
            with pytest.raises(DeadPool):
                await pool.kill(RuntimeError())
            with pytest.raises(DeadPool):
                await pool.join()
            with pytest.raises(DeadPool):
                await pool.terminate_jobs()
            with pytest.raises(DeadPool):
                await pool.ensure_set_worker_count(5)

    with pytest.raises(ToCatch):
        await _helper()


class MyCreateWorkerThrowsPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        raise RuntimeError


@pytest.mark.asyncio
async def test_create_worker_throws():
    pool = MyCreateWorkerThrowsPool(10)
    with pytest.raises(RuntimeError):
        async with pool:
            pass

    assert pool.is_dead()
    assert pool.workers_count == 0


@pytest.mark.asyncio
async def test_double_enter():
    pool = MyWorkPool(list(), 5)
    async with pool:
        with pytest.raises(WorkerPoolHasAlreadyStarted):
            async with pool:
                pass

    assert not pool.is_dead()


#########################################################
#           NORMAL EXECUTION TESTS: NO ERRORS           #
#########################################################


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'job_count,item_count',
    [
        (10, 5),  # overcommit
        (5, 10),  # undercommit
        (5, 5),  # exact
        (10, 0),  # no jobs
        (1, 10)  # 1 worker case
    ]
)
async def test_all_processed(job_count, item_count):
    recorder = []
    done_recorder = []

    pool = MyWorkPool(recorder, job_count)
    # pool.add_done_callback(lambda x: done_recorder.append(x))
    async with pool as pool:
        for x in range(item_count):
            future = await pool.assign_job(x)
            future.add_done_callback(lambda future: done_recorder.append(future.result()))

    reference_items = set(range(item_count))
    assert set(recorder) == reference_items
    assert set(done_recorder) == reference_items


@pytest.mark.asyncio
async def test_all_workers_have_been_called():
    pool = MyWorkPool([], 10)
    async with pool as pool:
        for x in range(100):
            await pool.assign_job(x)
    assert all(x.call_counter for x in pool.all_workers)
    assert sum(x.call_counter for x in pool.all_workers) == 100


@pytest.mark.asyncio
async def test_multiple_enter_context():
    pool = MyWorkPool([], 10)
    for _ in range(3):
        assert set(pool._free_workers.get_workers().values()) == set(pool.all_workers)
        assert pool._running_workers == {}
        async with pool as pool:
            for x in range(100):
                await pool.assign_job(x)


#########################################################
#       NORMAL EXECUTION TESTS: DYNAMIC SIZE CHANGE     #
#########################################################


@pytest.mark.asyncio
async def test_proper_ranks_on_size_change():
    pool = MyWorkPool([], 5)
    assert pool.workers_count == 5
    assert not len(pool.all_workers)  # lazy initialization
    pool.workers_count = 10
    assert pool.workers_count == 10
    assert not len(pool.all_workers)  # lazy initialization

    # Check initialized workers
    async with pool:
        assert len(pool.all_workers) == 10
    assert pool.workers_count == 10
    assert len(pool.all_workers) == 10

    # Enhance
    pool.workers_count = 15
    assert pool.workers_count == 15
    async with pool:
        assert len(pool.all_workers) == 15
    assert len(pool.all_workers) == 15

    # shrink
    pool.workers_count = 5
    assert pool.workers_count == 5
    async with pool:
        assert pool.workers_count == 5


class MySimpleWorkerRankRecorder(BaseAsyncWorker):
    def __init__(self, recorder, rank_recorder, rank):
        super(MySimpleWorkerRankRecorder, self).__init__(rank)
        self.call_counter = 0
        self.recorder = recorder
        self.rank_recorder = rank_recorder

    async def _do_job(self, x):
        self.rank_recorder.add(self.rank)
        self.call_counter += 1
        self.recorder.append(x)
        return x


class MyWorkPoolRankRecorder(BaseAsyncWorkerPool):
    def __init__(self, recorder, rank_recorder, workers_count=1):
        self.recorder = recorder
        self.rank_recorder = rank_recorder
        self.assign_rank_recorder = set()
        super(MyWorkPoolRankRecorder, self).__init__(workers_count=workers_count)

    def _assign_job_to_worker(self, cur_worker, *args, **kwargs):
        self.assign_rank_recorder.add(cur_worker.rank)
        return super(MyWorkPoolRankRecorder, self)._assign_job_to_worker(cur_worker, *args, **kwargs)

    def create_new_worker(self, new_rank: int):
        return MySimpleWorkerRankRecorder(self.recorder, self.rank_recorder, new_rank)


@pytest.mark.asyncio
async def test_decrease_pool_size_between_launches():
    recorder = []
    ranks_recorder = set()
    pool = MyWorkPoolRankRecorder(recorder, ranks_recorder, 10)
    iterable = list(range(100))
    async with pool:
        for r in iterable:
            await pool.assign_job(r)
    assert ranks_recorder == set(range(10))
    assert set(recorder) == set(iterable)

    pool.workers_count = 5
    recorder.clear()
    ranks_recorder.clear()
    async with pool:
        for r in iterable:
            await pool.assign_job(r)
    assert ranks_recorder == set(range(5))
    assert set(recorder) == set(iterable)


@pytest.mark.asyncio
async def test_increase_pool_size_between_launches():
    recorder = []
    ranks_recorder = set()
    pool = MyWorkPoolRankRecorder(recorder, ranks_recorder, 5)
    iterable = list(range(100))
    async with pool:
        for r in iterable:
            await pool.assign_job(r)
    assert ranks_recorder == set(range(5))
    assert set(recorder) == set(iterable)

    pool.workers_count = 10
    recorder.clear()
    ranks_recorder.clear()
    async with pool:
        for r in iterable:
            await pool.assign_job(r)
    assert ranks_recorder == set(range(10))
    assert set(recorder) == set(iterable)


@pytest.mark.asyncio
@pytest.mark.parametrize('new_count', [5, 15])
async def test_dynamic_size_change(new_count):
    recorder = []
    ranks_recorder = set()
    pool = MyWorkPoolRankRecorder(recorder, ranks_recorder, 10)
    iterable = list(range(1000))

    s = int(len(iterable) / 2)
    first_half = iterable[:s]
    second_half = iterable[s:]
    async with pool:
        for r in first_half:
            await pool.assign_job(r)
        old_count = pool.workers_count
        old_free_workers = len(pool._free_workers.get_workers())
        pool.workers_count = new_count
        if new_count > old_count:
            assert len(pool._free_workers.get_workers()) - old_free_workers == new_count - old_count
        pool.assign_rank_recorder.clear()
        for r in second_half:
            await pool.assign_job(r)

    assert pool.assign_rank_recorder == set(range(new_count))


#########################################################
#                   TERMINATION TESTS                   #
#########################################################


class MyInfiniteWorker(BaseAsyncWorker):
    async def _do_job(self, x):
        await asyncio.sleep(100000)
        return x


class MyInfiniteWorkPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return MyInfiniteWorker(new_rank)


@pytest.mark.asyncio
async def test_terminate_jobs():
    futures = []
    async with MyInfiniteWorkPool(10) as pool:
        for i in range(7):
            future = await pool.assign_job(i)
            futures.append(future)
        pool.terminate_jobs()

    for future in futures:
        with pytest.raises(asyncio.CancelledError):
            await future


@pytest.mark.asyncio
async def test_pool_survive_after_termination():
    async with MyInfiniteWorkPool(10) as pool:
        for i in range(7):
            await pool.assign_job(i)
        assert len(pool._free_workers.get_workers()) == 3
        pool.terminate_jobs()
        assert len(pool._free_workers.get_workers()) == 10
        # Try again after termination
        pool.workers_count = 15
        for i in range(15):
            await pool.assign_job(i)
        assert len(pool._free_workers.get_workers()) == 0
        pool.terminate_jobs()
        assert len(pool._free_workers.get_workers()) == 15


@pytest.mark.asyncio
async def test_pool_terminate_jobs_in_case_of_exception_on_exit():
    futures = []
    with pytest.raises(RuntimeError):
        async with MyInfiniteWorkPool(10) as pool:
            for i in range(7):
                futures.append(await pool.assign_job(i))
            raise RuntimeError

    for f in futures:
        with pytest.raises(asyncio.CancelledError):
            await f


#########################################################
#                       KILL TESTS                      #
#########################################################


@pytest.mark.asyncio
async def test_kill_pool():
    futures = []
    pool = MyInfiniteWorkPool(10)
    with pytest.raises(ToCatch):
        async with pool:
            for i in range(7):
                future = await pool.assign_job(i)
                futures.append(future)
            assert len(pool._free_workers.get_workers()) == 3
            pool.kill(ToCatch("SomeWeirdException"))

            # test on join to pool
            await asyncio.wait(futures)

            assert len(pool._free_workers.get_workers()) == 0
            assert pool.workers_count == 0

            for future in futures:
                with pytest.raises((ToCatch, asyncio.CancelledError)):
                    await future

            with pytest.raises(ToCatch):
                pool.workers_count = 1

            with pytest.raises(ToCatch):
                await pool.assign_job(5)

            with pytest.raises(ToCatch):
                await pool.join()

            with pytest.raises(ToCatch):
                await pool.terminate_jobs()

    with pytest.raises(ToCatch):
        async with pool:
            pass


@pytest.mark.asyncio
async def test_kill_twice_autodie_propagate_exception():
    pool = MyInfiniteWorkPool(10)
    with pytest.raises(ToCatch):
        async with pool:
            for i in range(7):
                await pool.assign_job(i)
            ex = ToCatch("SomeWeirdException")
            pool.kill(ex)
            with pytest.raises(ToCatch):
                pool.kill(RuntimeError("oops"))
            assert pool._ex is ex


@pytest.mark.asyncio
async def test_kill_twice():
    pool = MyInfiniteWorkPool(10)
    with pytest.raises(ToCatch):
        async with pool:
            for i in range(7):
                await pool.assign_job(i)
            ex = ToCatch("SomeWeirdException")
            pool.kill(ex)
            with pytest.raises(ToCatch):
                pool.kill(RuntimeError("oops"))
            assert pool._ex is ex


# Pool robustness to any error tests
class MyErrorProneWorker(BaseAsyncWorker):
    async def _do_job(self, x):
        if x % 2:
            return x
        raise ToCatch("Oops...")


class MyErrorPronePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return MyErrorProneWorker(new_rank)


class MyErrorProneAutoTerminatedPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count=1):
        super(MyErrorProneAutoTerminatedPool, self).__init__(workers_count=workers_count)

    def create_new_worker(self, new_rank: int):
        return MyErrorProneWorker(new_rank)


#########################################################
#                      AUTODIE TESTS                    #
#########################################################


@pytest.mark.asyncio
async def test_autoterminated_pool():
    pool = MyErrorProneAutoTerminatedPool(10)

    with pytest.raises(ToCatch):
        async with pool:
            for i in range(1, 10, 2):
                await pool.assign_job(i)
            # First future assigns without any problem
            future = await pool.assign_job(2)
            # new die assignments: check that pool finishes correctly in case of multiple kill commands
            new_die_assignments = [asyncio.ensure_future(pool.assign_job(i)) for i in range(0, 10, 2)]
            # ensure that we have terminated the pool
            with pytest.raises(ToCatch):
                await future
            new_assignments = [asyncio.ensure_future(pool.assign_job(i)) for i in range(11, 100, 2)]
            await asyncio.wait(new_assignments)
            await asyncio.wait(new_die_assignments)
            # For new assignments, we must always obtain CancelledError on assign job
            for new_future in new_assignments:
                with pytest.raises(asyncio.CancelledError):
                    await new_future
            for new_die_future in new_die_assignments:
                try:
                    future = await new_die_future
                    with pytest.raises(ToCatch):
                        await future
                except:
                    with pytest.raises(asyncio.CancelledError):
                        raise
            assert pool.is_dead()
            with pytest.raises(ToCatch):
                pool.workers_count = 1
            with pytest.raises(ToCatch):
                await pool.assign_job(5)

    with pytest.raises(ToCatch):
        async with pool:
            pass


@pytest.mark.asyncio
async def test_pool_throws_error_autodie_mode():
    pool = MyErrorPronePool(10)
    with pytest.raises(ToCatch):
        async with pool:
            # this will actually die on exit
            await pool.assign_job(2)


@dataclasses.dataclass
class CallCounter:
    x: int = 0
    max_call_counter: int = 3


class DelayedExceptionWorker(BaseAsyncWorker):
    def __init__(self, call_counter, new_rank):
        super(DelayedExceptionWorker, self).__init__(new_rank)
        self.call_counter = call_counter

    async def _do_job(self, x):
        self.call_counter.x += 1
        if self.call_counter.x > self.call_counter.max_call_counter:
            raise ToCatch("Oops...")
        return x


class DelayedExceptionWorkerPool(BaseAsyncWorkerPool):
    def __init__(self, call_counter, workers_count):
        super(DelayedExceptionWorkerPool, self).__init__(workers_count=workers_count)
        self.call_counter = call_counter

    def create_new_worker(self, new_rank: int):
        return DelayedExceptionWorker(self.call_counter, new_rank)


@pytest.mark.asyncio
async def test_propagate_innermost_exception():
    call_counter = CallCounter(max_call_counter=3)
    with pytest.raises(ToCatch):
        async with DelayedExceptionWorkerPool(call_counter, 5) as pool:
            for i in range(10):
                await pool.assign_job(3)


@pytest.mark.asyncio
@pytest.mark.parametrize("inner_exc", [DeadPool("ahaha"), RuntimeError("BOOM")], ids=['DeadPool', 'RuntimeError'])
async def test_do_not_substitute_foreign_exception(inner_exc):
    call_counter = CallCounter(max_call_counter=3)
    with pytest.raises(type(inner_exc)) as exc:
        async with DelayedExceptionWorkerPool(call_counter, 5) as pool:
            await pool.assign_job(3)
            await pool.assign_job(3)
            raise inner_exc
    assert exc.value is inner_exc


@pytest.mark.asyncio
async def test_nested_die_pools():
    call_counter = CallCounter(max_call_counter=3)
    with pytest.raises(ToCatch) as exc:
        async with DelayedExceptionWorkerPool(call_counter, 5) as pool:
            async with DelayedExceptionWorkerPool(call_counter, 5) as pool2:
                await pool.assign_job(3)
                await pool.assign_job(3)
                for i in range(10):
                    await pool2.assign_job(3)
    assert exc.value is pool2._ex


@pytest.mark.asyncio
async def test_nested_infinite_pools_propagate_termination():
    inner_pool = MyInfiniteWorkPool(10)
    outer_pool = MyInfiniteWorkPool(10)
    with pytest.raises(ToCatch):
        async with inner_pool:
            async with outer_pool:
                for i in range(5):
                    await inner_pool.assign_job(1)
                    await outer_pool.assign_job(1)
                await asyncio.sleep(0.01)
                raise ToCatch("OOPS")

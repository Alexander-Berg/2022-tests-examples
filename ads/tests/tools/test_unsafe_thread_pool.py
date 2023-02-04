import multiprocessing
from ads_pytorch.tools.unsafe_thread_pool import UnsafeThreadPoolExecutor, wait, ThreadPoolExecutor


def infinite_foo():
    event = multiprocessing.Event()
    event.wait()


def run_infinite_foo():
    pool = UnsafeThreadPoolExecutor(1)
    pool.submit(infinite_foo)


# With usual ThreadPoolExecutor inside run_infinite_foo, this test gets stuck
# But Unsafe executor allows us just to forget about this threads
def test_pool_exits():
    ctx = multiprocessing.get_context('spawn')
    p = ctx.Process(target=run_infinite_foo)
    p.start()
    p.join()


# sanity check that thread pools work equally
def test_pools_are_same():
    def _do_job(x):
        return x

    def _check(safe_pool, unsafe_pool, size):
        safe_futures = []
        unsafe_futures = []

        for i in range(size):
            safe_futures.append(safe_pool.submit(_do_job, i))
            unsafe_futures.append(unsafe_pool.submit(_do_job, i))

        wait(safe_futures)
        wait(unsafe_futures)

        for f1, f2 in zip(safe_futures, unsafe_futures):
            assert f1.result() == f2.result()

    with ThreadPoolExecutor(10) as safe_pool, UnsafeThreadPoolExecutor(10) as unsafe_pool:
        _check(safe_pool, unsafe_pool, 1)
        _check(safe_pool, unsafe_pool, 5)
        _check(safe_pool, unsafe_pool, 100)
        _check(safe_pool, unsafe_pool, 5)

from infra.orly.lib import limited_semaphore


def test_acquire_release():
    sem = limited_semaphore.LimitedQueueSemaphore(1)
    assert sem.acquire() is None
    assert sem.acquire() == limited_semaphore.LimitedQueueSemaphore.ERR_CONCURRENT_WAITERS_LIMIT_REACHED
    sem.release()
    sem = limited_semaphore.LimitedQueueSemaphore(2)
    assert sem.acquire() is None
    assert sem.acquire(0.01) == limited_semaphore.LimitedQueueSemaphore.ERR_TIMEOUT_REACHED

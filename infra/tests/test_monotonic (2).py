"""Tests generation of monotonically increasing numbers."""

from __future__ import unicode_literals

import threading

from sepelib.mongo.monotonic import _MonotonicNumbers, get_next
import six


def test_generation(db):
    assert [get_next("first") for i in six.moves.xrange(10)] == list(range(0, 10))
    assert [get_next("second") for i in six.moves.xrange(5)] == list(range(0, 5))
    assert [obj.to_mongo() for obj in _MonotonicNumbers.objects.order_by("id")] == [
        _MonotonicNumbers(id="first", next=10).to_mongo(), _MonotonicNumbers(id="second", next=5).to_mongo()]


def test_concurrency(db):
    count = 100
    thread_num = 10

    numbers = []
    lock = threading.Lock()

    def worker():
        for i in six.moves.xrange(count):
            number = get_next("test")
            with lock:
                numbers.append(number)

    threads = [threading.Thread(target=worker) for i in six.moves.xrange(thread_num)]

    try:
        for thread in threads:
            thread.start()
    finally:
        for thread in threads:
            thread.join()

    expected_numbers = list(range(0, count * thread_num))
    assert sorted(numbers) == expected_numbers
    assert numbers != expected_numbers

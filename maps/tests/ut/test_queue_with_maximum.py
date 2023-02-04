from ya_courier_backend.util.queue_with_maximum import QueueWithMaximum
from ya_courier_backend.util.position import Position
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
import pytest


@skip_if_remote
def test_queue_workflow():
    queue = QueueWithMaximum()
    assert queue.is_empty()
    queue.push(1)
    assert len(queue) == 1

    queue.push(3)
    queue.push(2)
    assert queue.maximum() == 3
    assert len(queue) == 3

    assert queue.front() == 1
    assert queue.back() == 2
    assert queue[1] == 3

    obj = queue.pop()
    assert obj == 1
    assert len(queue) == 2
    assert queue.front() == 3

    obj = queue.pop()
    assert queue.maximum() == 2
    obj = queue.pop()
    assert queue.is_empty()

    queue.push(1)
    queue.push(1)
    queue.push(1)
    assert len(queue) == 3
    queue.clear()
    assert queue.is_empty()


@skip_if_remote
def test_queue_errors():
    queue = QueueWithMaximum()

    with pytest.raises(IndexError):
        _ = queue.front()
    with pytest.raises(IndexError):
        _ = queue.back()

    queue.push(1)
    queue.push(2)
    assert len(queue) == 2
    with pytest.raises(IndexError):
        _ = queue[2]
    with pytest.raises(TypeError):
        _ = queue['41']


@skip_if_remote
def test_queue_with_comparator():
    queue = QueueWithMaximum(key=lambda x: x[0])
    queue.push((100, Position(lon=0, lat=0, time=1e9+1)))
    queue.push((300, Position(lon=0, lat=0, time=1e9+20)))
    queue.push((300, Position(lon=0, lat=0, time=1e9+10)))
    assert queue.maximum() == queue.back()
    queue.push((200, Position(lon=0, lat=0, time=1e9+10)))
    assert queue.maximum() == queue[2]

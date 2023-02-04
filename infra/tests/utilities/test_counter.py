import dataclasses

from walle.util import counter


@dataclasses.dataclass
class TestItem:
    counter: int


def test_counter_checker():
    checker = counter.CounterChecker("test")
    item = TestItem(counter=0)
    item_id = 0

    assert not checker.check_for_fresh(item_id, item)
    assert not checker.check_for_fresh(item_id, item)
    item.counter = 1
    assert checker.check_for_fresh(item_id, item)
    item.counter = 2
    assert checker.check_for_fresh(item_id, item)
    assert not checker.check_for_fresh(item_id, item)


def test_counter_checker_existed_item():
    checker = counter.CounterChecker("test")
    item = TestItem(counter=10)
    item_id = 0

    assert not checker.check_for_fresh(item_id, item)
    assert not checker.check_for_fresh(item_id, item)
    item.counter += 1
    assert checker.check_for_fresh(item_id, item)

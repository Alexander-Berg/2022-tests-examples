import pytest
import dataclasses
import contextlib
from typing import List
from pytorch_embedding_model import CppYtClient
from pytorch_embedding_model.yt_trainer.transaction_context import (
    TransactionActionsStack,
    LEAST_POSSIBLE_PRIORITY,
    ITransactionAction
)


@dataclasses.dataclass
class CppYtClientMock:
    committed: bool = dataclasses.field(init=False, default=False)
    aborted: bool = dataclasses.field(init=False, default=False)

    def StartTransaction(self, parent_tx):
        return self

    def CommitTransaction(self):
        self.committed = True

    def AbortTransaction(self):
        self.aborted = True


def test_empty_context():
    client = CppYtClientMock()
    with TransactionActionsStack(yt_client=client) as stack:
        pass

    assert client.committed
    assert not client.aborted


@dataclasses.dataclass
class DummyAction(ITransactionAction):
    finished: bool = dataclasses.field(init=False, default=False)
    yielded: bool = dataclasses.field(init=False, default=False)

    @contextlib.contextmanager
    def transaction_ctx(self, transaction: CppYtClient):
        try:
            yield
        finally:
            self.yielded = True

    def finish(self):
        self.finished = True


@pytest.mark.parametrize(
    "priority",
    [LEAST_POSSIBLE_PRIORITY, LEAST_POSSIBLE_PRIORITY + 1],
    ids=["equal", "greater"]
)
def test_forbidden_priorities(priority):
    client = CppYtClientMock()
    with pytest.raises(ValueError):
        with TransactionActionsStack(yt_client=client) as stack:
            stack.push(DummyAction(), priority=priority)


@pytest.mark.parametrize(
    "priority",
    [LEAST_POSSIBLE_PRIORITY - 1, 1, 0, -1 -LEAST_POSSIBLE_PRIORITY, -LEAST_POSSIBLE_PRIORITY * 2]
)
def test_ok_priorities(priority):
    client = CppYtClientMock()
    action = DummyAction()
    with TransactionActionsStack(yt_client=client) as stack:
        stack.push(action, priority=priority)
    assert action.finished
    assert action.yielded
    assert client.committed
    assert not client.aborted


def test_throw_exception():
    client = CppYtClientMock()
    action = DummyAction()
    try:
        with TransactionActionsStack(yt_client=client) as stack:
            stack.push(action, priority=0)
            raise ValueError
    except ValueError:
        pass
    assert not action.finished
    assert action.yielded
    assert not client.committed
    assert client.aborted


@dataclasses.dataclass
class ActionWithPriority(DummyAction):
    priority: int
    priority_finish_ref: List[int]
    priority_yield_ref: List[int]

    def finish(self):
        self.priority_finish_ref.append(self.priority)

    @contextlib.contextmanager
    def transaction_ctx(self, transaction: CppYtClient):
        try:
            with super(ActionWithPriority, self).transaction_ctx(transaction):
                yield
        finally:
            self.priority_yield_ref.append(self.priority)


@pytest.mark.parametrize(
    "priority_order",
    [
        list(range(10)),
        list(reversed(range(10))),
        [5, 9, 1, 4, 2, 6, 3, 7, 8, 0]
    ],
    ids=[
        "sequential",
        "reversed",
        "random"
    ]
)
def test_different_priorities(priority_order):
    client = CppYtClientMock()
    finish_lst = []
    yield_lst = []
    with TransactionActionsStack(yt_client=client) as stack:
        # reverse push order to test we really yield in proper order
        for i in priority_order:
            stack.push(
                ActionWithPriority(
                    priority=i,
                    priority_finish_ref=finish_lst,
                    priority_yield_ref=yield_lst
                ),
                priority=i)
    assert finish_lst == list(range(10))
    assert yield_lst == list(range(10))


def test_different_priorities_exception():
    client = CppYtClientMock()
    finish_lst = []
    yield_lst = []
    try:
        with TransactionActionsStack(yt_client=client) as stack:
            # reverse push order to test we really yield in proper order
            for i in reversed(range(10)):
                stack.push(
                    ActionWithPriority(
                        priority=i,
                        priority_finish_ref=finish_lst,
                        priority_yield_ref=yield_lst
                    ),
                    priority=i)
            raise ValueError
    except ValueError:
        pass
    assert finish_lst == []
    assert yield_lst == list(range(10))

import pytest

from sendr_utils import get_subclasses

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.base import BaseAsyncDBAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.base import ProcessingAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.taskq.app import ActionWorker


@pytest.mark.parametrize(
    'action_cls', set(get_subclasses(BaseAsyncDBAction, '.test_')) - {ProcessingAsyncableAction}
)
def test_async_action_is_registered(action_cls):
    action_worker_classes = get_subclasses(ActionWorker) + [ActionWorker]
    is_member_of_any = any(action_cls in cls.actions for cls in action_worker_classes)
    assert is_member_of_any, f'{action_cls} is not member of any ActionWorker subclasses'

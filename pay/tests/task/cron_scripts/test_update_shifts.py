from datetime import timedelta
from contextlib2 import nullcontext as does_not_raise
import pytest

from yb_darkspirit.task.cron_scripts.exceptions import TimeoutException
from yb_darkspirit.task.cron_scripts.update_shifts import update_shifts_for_ws


@pytest.mark.parametrize(
    'timeout, raises',
    [
        (timedelta(hours=1), does_not_raise()), (timedelta(hours=-1), pytest.raises(TimeoutException)),
    ]
)
def test_update_shifts_for_ws_timeout_raise(
        session, cr_wrapper, disabled_metrics_subprocess_collect, timeout, raises,
):
    with raises:
        update_shifts_for_ws(session, cr_wrapper.cash_register.whitespirit, timeout)

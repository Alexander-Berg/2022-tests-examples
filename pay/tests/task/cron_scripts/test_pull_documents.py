import pytest
from datetime import timedelta
from contextlib2 import nullcontext as does_not_raise

from yb_darkspirit.task.cron_scripts.exceptions import TimeoutException
from yb_darkspirit.task.cron_scripts.pull_documents import pull_documents_for_ws


@pytest.mark.parametrize(
    'timeout, raises',
    [
        (timedelta(hours=1), does_not_raise()), (timedelta(hours=-1), pytest.raises(TimeoutException)),
    ]
)
def test_pull_missing_documents_for_ws_timeout_raise(
        session, cr_wrapper, pull_document, get_missing_document_numbers, disabled_metrics_subprocess_collect,
        timeout, raises,
):
    get_missing_document_numbers.return_value = [10, 22]
    with raises:
        pull_documents_for_ws(session, cr_wrapper.cash_register.whitespirit, timeout)

import pytest

from maps.poi.pylibs.util.yt_util import run_retryable_yt_operation
from maps.pylibs.utils.lib.common import RetryFailedException
from yt.wrapper.errors import YtResponseError


class ExceptionsMaker(object):
    def __init__(self, exception, n_exceptions):
        self.exception = exception
        self.n_exceptions = n_exceptions
        self.n_queries = 0

    def request(self):
        n_current = self.n_queries
        self.n_queries += 1
        if n_current < self.n_exceptions:
            raise self.exception


def test_retry():
    yt_error = YtResponseError({'message': 'msg'})  # yes, such a format

    exc_maker = ExceptionsMaker(yt_error, 1)
    run_retryable_yt_operation(exc_maker.request, tries=2)

    exc_maker = ExceptionsMaker(yt_error, 5)
    with pytest.raises(RetryFailedException):
        run_retryable_yt_operation(exc_maker.request, tries=3, delay=1)

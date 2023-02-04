import pytest

pytest.register_assert_rewrite("ads.quality.metric_eval.tests.lib.check_result")

from .check_result import check_result  # noqa

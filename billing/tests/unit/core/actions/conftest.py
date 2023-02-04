import pytest

from billing.hot.faas.python.faas.core.actions.base import BaseAction
from billing.hot.faas.python.faas.utils.registry import registry


@pytest.fixture(autouse=True)
def action_context_setup(test_logger, function_registry, rands):
    BaseAction.context.logger = test_logger
    BaseAction.context.request_id = rands()
    BaseAction.context.function_registry = function_registry
    BaseAction.context.registry = registry

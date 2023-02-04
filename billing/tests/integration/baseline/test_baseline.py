import typing as tp

import pytest

from billing.hot.tests.lib.state.builder import ExtendedBuilder
from billing.hot.tests.lib.templates.processor import BaseProcessorRenderer
from billing.hot.tests.lib.test_case.config import TestCase
from billing.hot.tests.lib.test_case.base_namespace_test import BaseNamespaceTest
from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.base_client import Client as ProcessorClient
from billing.hot.tests.lib.test_case.test_case_loader import TestConfigLoader


class TestBaseline(BaseNamespaceTest):
    baseline_cases: list[tuple[str, tp.Optional[str], TestCase]] = TestConfigLoader.load_baseline_cases()

    @pytest.mark.parametrize("namespace,description,case", baseline_cases)
    @pytest.mark.asyncio
    async def test_baseline(
        self,
        state_builder: ExtendedBuilder,
        create_client: tp.Callable[[tp.Type[BaseProcessorRenderer]], ProcessorClient],
        accounts_client: AccountsClient,
        namespace: str,
        description: str,
        case: TestCase
    ):
        await self._baseline(
            processor_client=create_client(case.processor.input.renderer_type),
            accounts_client=accounts_client,
            state_builder=state_builder,
            case=case,
        )

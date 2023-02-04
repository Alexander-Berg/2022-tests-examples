import logging
import typing as tp
from abc import ABC
from datetime import datetime

import hamcrest as hm
import pytest

from billing.hot.tests.config.config import Config
from billing.hot.tests.lib.matchers.base import (
    success_processor_response_entries, success_accounts_read_batch_response,
    error_processor_response_entries, error_accounts_read_batch_response
)
from billing.hot.tests.lib.state import builder, state
from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.base_client import Client as ProcessorClient
from billing.hot.tests.clients.yt.client import Client as YtClient
from billing.hot.tests.lib.state.builder import ExtendedBuilder
from billing.hot.tests.lib.templates import yt as yt_template
from billing.hot.tests.lib.test_case import config as test_config
from billing.library.python.calculator.values import PersonType


def prepare_state_builder(
    state_builder: ExtendedBuilder,
    data: test_config.ProcessorTestCaseInput
) -> None:
    state_builder.with_template(template=data.template)
    state_builder.with_service_id(service_id=data.service_id)

    if data.client_id:
        state_builder.with_client_id(client_id=data.client_id)
    if data.firm_id:
        state_builder.with_firm(firm_id=data.firm_id)
    if data.person_type:
        state_builder.with_person_type(person_type=PersonType(data.person_type))
    if data.contract:
        state_builder.with_contract_params(contract_params=data.contract)
    if data.service_code:
        state_builder.with_service_code(service_code=data.service_code)
    if data.event_params:
        state_builder.with_event_params(event_params=data.event_params)
    if data.trust_event:
        event = data.trust_event

        state_builder.with_event_currency(event_currency=event.currency)
        state_builder.with_rows(rows=event.rows)
        state_builder.with_refunds(refunds=event.refunds)

        if event.products:
            state_builder.with_products_params(products_params=event.products)
        if event.payment_method_id:
            state_builder.with_payment_method_id(payment_method_id=event.payment_method_id)


class BaseNamespaceTest(ABC):

    @property
    def now_ts(self) -> int:
        return int(datetime.now().timestamp())

    @property
    def logger(self) -> logging.Logger:
        return logging.getLogger(__name__)

    @pytest.fixture
    def state_builder(self, yt_lib_client: YtClient, config: Config) -> ExtendedBuilder:
        yt_renderer = yt_template.YtRenderer(yt_template.YtLoader(config.state_builder.yt_data_dir))

        return ExtendedBuilder(yt_lib_client, yt_renderer)

    @staticmethod
    def _prepare_state_builder(
        state_builder: builder.ExtendedBuilder, case: test_config.TestCase
    ) -> state.ExtendedPipelineState:
        state_builder.with_namespace(namespace=case.namespace)
        state_builder.with_endpoint(endpoint=case.endpoint)

        prepare_state_builder(state_builder=state_builder, data=case.processor.input)

        state_builder.fill_contracts(
            contracts=[case.processor.input.contract_type.generate()],
            namespace=case.namespace,
            dry_run=case.processor.input.dry_run or False,
        )

        return state_builder.built_state()

    @staticmethod
    async def _make_processor_request(
        method: tp.Callable[[...], tp.AsyncContextManager],
        *method_args: tp.Any,
        expected_response: test_config.ExpectedResponse,
        **method_kwargs: tp.Any,
    ) -> None:
        status = expected_response.status
        async with method(*method_args, **method_kwargs) as response:
            hm.assert_that(response.status, hm.equal_to(status))
            if expected_response.data:
                matcher = success_processor_response_entries if status < 300 else error_processor_response_entries
                hm.assert_that(await response.json(), matcher(expected_response.data))

    @staticmethod
    async def _make_accounts_request(
        method: tp.Callable[[...], tp.AsyncContextManager],
        *method_args: tp.Any,
        expected_response: test_config.ExpectedResponse,
        **method_kwargs: tp.Any,
    ) -> None:
        status = expected_response.status
        async with method(*method_args, **method_kwargs) as response:
            hm.assert_that(response.status, hm.equal_to(status))
            if expected_response.data:
                matcher = success_accounts_read_batch_response if status < 300 else error_accounts_read_batch_response
                hm.assert_that(
                    await response.json(), matcher(expected_response.data, strict=method_kwargs.get('strict', False))
                )

    async def _baseline(
        self,
        processor_client: ProcessorClient,
        accounts_client: AccountsClient,
        state_builder: ExtendedBuilder,
        case: test_config.TestCase,
    ) -> None:
        """
        Make processor request and check balances in accounts
        """
        self.logger.info("namespace=%s case=%s", case.namespace, case)

        st = self._prepare_state_builder(state_builder=state_builder, case=case)

        await self._make_processor_request(
            processor_client.post, st,
            expected_response=case.processor.expected.response,
        )

        if case.accounts:
            await self._make_accounts_request(
                accounts_client.read_balances, st, self.now_ts, case.accounts.input.accounts, case.namespace,
                expected_response=case.accounts.expected.response,
            )

        state_builder.clear()

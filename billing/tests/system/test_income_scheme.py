import asyncio
import itertools
import json
import os
import typing as tp
from datetime import date

import pytest
import yql.api.v1.client as yql
import yt.wrapper as yt
from billing.hot.nirvana.act_o_tron import act_o_tron

from billing.hot.tests.config import config as core_config
from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.logbroker import logbroker
from billing.hot.tests.clients.yt.client import Client as YtClient
from billing.hot.tests.lib.date import timestamp
from billing.hot.tests.lib.schema import obfuscation
from billing.hot.tests.lib.test_case import config as test_cfg
from billing.hot.tests.clients.processor.base_client import Client as ProcessorClient
from billing.hot.tests.lib.test_case.base_namespace_test import prepare_state_builder
from billing.hot.tests.lib.test_case.test_case_loader import NamespaceTestLoader, TestConfigLoader
from billing.hot.tests.lib.state import builder, contract as ct, state
from billing.hot.tests.lib.templates import processor as pt
from billing.hot.tests.lib.util import deco


@pytest.fixture
def system_test_bundles(config: core_config.Config) -> dict[str, test_cfg.SystemTestBundleConfig]:
    return {
        namespace: NamespaceTestLoader(namespace).system_test_bundle
        for namespace in config.testing_namespaces.get('system')
    }


@pytest.fixture()
def yt_root(yt_lib_client: YtClient):
    path = yt_lib_client.client.find_free_subpath('//')
    yt_lib_client.client.create('map_node', path)
    yield path
    yt_lib_client.client.remove(path, recursive=True, force=True)


@pytest.fixture
def yql_client():
    yield yql.YqlClient(
        server='localhost',
        port=int(os.environ['YQL_PORT']),
        db='plato'
    )


@pytest.fixture
def bundles(
    system_test_bundles: dict[str, test_cfg.SystemTestBundleConfig],
    create_client: tp.Callable[[tp.Type[pt.BaseProcessorRenderer]], ProcessorClient],
) -> dict[str, tp.Any]:
    def get_obfuscator(cfg: test_cfg.ObfuscationConfig) -> obfuscation.Obfuscator:
        return obfuscation.Obfuscator(
            obfuscate_term=cfg.obfuscate_term,
            processor_result_paths=cfg.processor_result_paths,
            accounts_balance_paths=cfg.accounts_balance_paths,
            accounts_event_paths=cfg.accounts_event_paths,
            act_rows_paths=cfg.act_rows_paths,
            acted_events_paths=cfg.acted_events_paths
        )

    return {
        namespace: {
            'namespace': namespace,
            'endpoint': bundle_cfg.endpoint,
            'accounts': bundle_cfg.accounts,
            'firm_id': bundle_cfg.processor_input.firm_id,
            'client_id': bundle_cfg.processor_input.client_id,
            'extract_external_ids': bundle_cfg.extract_external_ids_func,
            'loc_kwargs': bundle_cfg.loc_kwargs,
            'act_o_tron_loc_attrs': bundle_cfg.act_o_tron_loc_attrs,
            'processor_data': bundle_cfg.processor_input,
            'client': create_client(bundle_cfg.processor_input.renderer_type),
            'obfuscator': get_obfuscator(bundle_cfg.obfuscation),
            'contract': bundle_cfg.processor_input.contract_type.generate(),
        }
        for namespace, bundle_cfg in system_test_bundles.items()
    }


class TestProcessorToActotron:
    testcases = TestConfigLoader.load_testing_namespaces()['system']

    @pytest.fixture(params=testcases)
    def bundle(self, request, bundles: dict[str, tp.Any]) -> dict[str, tp.Any]:
        return bundles[request.param]

    @pytest.fixture
    def namespace(self, bundle: dict[str, tp.Any]) -> str:
        return bundle['namespace']

    @pytest.fixture
    def endpoint(self, bundle: dict[str, tp.Any]) -> tp.Optional[str]:
        return bundle.get('endpoint')

    @pytest.fixture
    def contract(self, bundle: dict[str, tp.Any]) -> ct.Contract:
        return bundle['contract']

    @pytest.fixture
    def accounts(self, bundle: dict[str, tp.Any]) -> list[str]:
        return bundle['accounts']

    @pytest.fixture
    def firm_id(self, bundle: dict[str, tp.Any]) -> tp.Optional[int]:
        return bundle.get('firm_id')

    @pytest.fixture
    def client_id(self, bundle: dict[str, tp.Any]) -> tp.Optional[int]:
        return bundle.get('client_id')

    @pytest.fixture
    def client(
        self,
        bundle: dict[str, tp.Any]
    ) -> ProcessorClient:
        return bundle['client']

    @pytest.fixture
    def obfuscator(self, bundle: dict[str, tp.Any]) -> obfuscation.Obfuscator:
        return bundle['obfuscator']

    @pytest.fixture
    def extract_external_ids(
        self,
        bundle: dict[str, tp.Any],
    ) -> tp.Callable[[dict[str, tp.Any]], list[tp.Union[str, int]]]:
        return bundle['extract_external_ids']

    @pytest.fixture
    def loc_kwargs(self, bundle: dict[str, tp.Any]) -> dict[str, tp.Any]:
        return bundle['loc_kwargs']

    @pytest.fixture
    def act_o_tron_loc_attrs(self, bundle: dict[str, tp.Any]) -> list[str]:
        return bundle['act_o_tron_loc_attrs']

    @pytest.fixture
    def processor_data(self, bundle: dict[str, tp.Any]) -> tp.Optional[test_cfg.ProcessorTestCaseInput]:
        return bundle.get('processor_data')

    @pytest.fixture
    def firm_tax_static(self, yt_lib_client: YtClient, yt_root: str) -> str:
        schema = [
            {'name': 'id', 'type': 'int64'},
            {'name': 'tax_policies', 'type_v3': {'type_name': 'optional', 'item': 'yson'}},
        ]
        path = yt.ypath_join(yt_root, 'firm_tax')
        yt_lib_client.client.create('table', path, recursive=True, attributes={'schema': schema})

        yield path

        yt_lib_client.client.remove(path, force=True)

    @pytest.fixture
    def accrualer_events(self, yt_lib_client: YtClient, yt_root: str) -> str:
        path = yt.ypath_join(yt_root, 'accrualer_events')
        schema = [
            {'name': 'data', 'type': 'string'},
        ]
        yt_lib_client.client.create('table', path, recursive=True, attributes={'schema': schema})

        yield path

        yt_lib_client.client.remove(path, force=True)

    @pytest.fixture
    def output_unacted_events(self, yt_root: str):
        return yt.ypath_join(yt_root, 'output_unacted_events')

    @pytest.fixture
    def output_acted_events(self, yt_root: str):
        return yt.ypath_join(yt_root, 'output_acted_events')

    @pytest.fixture
    def output_act_rows(self, yt_root: str):
        return yt.ypath_join(yt_root, 'output_act_rows')

    @pytest.fixture
    def built_state(
        self,
        state_builder: builder.ExtendedBuilder,
        namespace: str,
        contract: ct.Contract,
        firm_id: tp.Optional[int],
        client_id: tp.Optional[int],
        endpoint: str,
        processor_data: tp.Optional[test_cfg.ProcessorTestCaseInput],
    ):
        if firm_id is not None:
            state_builder.with_firm(firm_id)

        if client_id is not None:
            state_builder.with_client_id(client_id)

        if processor_data:
            state_builder.with_namespace(namespace=namespace)
            state_builder.with_endpoint(endpoint=endpoint)
            prepare_state_builder(state_builder=state_builder, data=processor_data)

        state_builder.fill_contracts(
            namespace=namespace,
            contracts=[contract],
            dry_run=False,
        )

        return state_builder.built_state()

    @deco.asynctest
    async def test_happy_path(
        self,
        built_state: state.ExtendedPipelineState,
        client: ProcessorClient,
        accounts_client: AccountsClient,
        accrualer_logbroker_client: logbroker.LogBrokerAPI,
        yql_client: yql.YqlClient,
        yt_lib_client: YtClient,
        firm_tax_static: str,
        accrualer_events: str,
        output_unacted_events: str,
        output_acted_events: str,
        output_act_rows: str,
        act_o_tron_loc_attrs: list[str],
        namespace: str,
        accounts: list[str],
        contract: ct.Contract,
        loc_kwargs: dict[str, tp.Any],
        obfuscator: obfuscation.Obfuscator,
        extract_external_ids: tp.Callable[[dict[str, tp.Any]], list[tp.Union[str, int]]],
    ):
        responses = {}

        async with client.post(built_state) as response:
            processor_result = await response.json()
            responses['processor_result'] = obfuscator.obfuscate_processor_response(processor_result)

        async with accounts_client.read_balances(
            built_state,
            timestamp.now_dt_second(),
            accounts,
            namespace
        ) as response:
            accounts_balances = await response.json()
            responses['accounts_balances'] = obfuscator.obfuscate_accounts_balances(accounts_balances)

        accounts_events = await self.get_accounts_events(
            built_state,
            accounts_client,
            external_ids=extract_external_ids(processor_result),
            namespace=namespace,
            accounts=accounts,
            client_id=built_state.client_id,
            contract_id=contract.id,
            **loc_kwargs,
        )
        self.fill_accounts_events_seq_id(accounts_events)
        responses['accounts_events'] = obfuscator.obfuscate_accounts_events(accounts_events)

        self.write_export_events_to_lb(accrualer_logbroker_client, accounts_events)
        agent_acts = self.read_agent_acts_from_lb(accrualer_logbroker_client)
        responses['agent_acts'] = obfuscator.obfuscate_agent_acts(agent_acts)

        return self.run_act_o_tron(
            yql_client=yql_client,
            yt_lib_client=yt_lib_client,
            obfuscator=obfuscator,
            agent_acts=agent_acts,
            loc_attrs=act_o_tron_loc_attrs,
            namespace=namespace,
            accrualer_events=accrualer_events,
            firm_tax=firm_tax_static,
            output_unacted_events=output_unacted_events,
            output_acted_events=output_acted_events,
            output_act_rows=output_act_rows,
        ) | responses

    async def get_accounts_events(
        self,
        st: state.ExtendedPipelineState,
        ac: AccountsClient,
        external_ids: list[str],
        namespace: str,
        accounts: list[str],
        client_id: int,
        contract_id: int,
        **kwargs,
    ) -> list[dict[str, tp.Any]]:
        kwargs.update({
            'external_ids': external_ids,
            'namespace': namespace,
            'client_id': client_id,
            'contract_id': contract_id,
        })

        all_events = await asyncio.gather(
            *[
                self._events_by_account(st, ac, acc, **kwargs)
                for acc in accounts
            ]
        )

        return list(itertools.chain(*all_events))

    @staticmethod
    async def _events_by_account(
        st: state.ExtendedPipelineState,
        accounts_client: AccountsClient,
        account: str,
        **kwargs,
    ) -> list[dict[str, tp.Any]]:
        async with accounts_client.get_exported_events(
            st,
            account=account,
            **kwargs,
        ) as response:
            body = await response.json()
            return body['data']

    @staticmethod
    def fill_accounts_events_seq_id(events: list[dict[str, tp.Any]]):
        for event in events:
            event['seq_id'] = event['id']

    @staticmethod
    def write_export_events_to_lb(
        accrualer_lb: logbroker.LogBrokerAPI,
        events: list[dict[str, tp.Any]],
    ):
        for event in events:
            accrualer_lb.producers['accounts-events'].write(event)

    @staticmethod
    def read_agent_acts_from_lb(
        accrualer_lb: logbroker.LogBrokerAPI,
    ) -> list[dict[str, tp.Any]]:
        all_messages = []
        while True:
            messages, found_any = accrualer_lb.consumers['agent-acts'].read()
            if not found_any:
                break
            all_messages.extend(messages)

        return [json.loads(m.data.decode('utf-8')) for m in all_messages]

    @staticmethod
    def run_act_o_tron(
        yql_client: yql.YqlClient,
        yt_lib_client: YtClient,
        obfuscator: obfuscation.Obfuscator,
        agent_acts: list[dict[str, tp.Any]],
        namespace: str,
        loc_attrs: list[str],
        accrualer_events: str,
        firm_tax: str,
        output_unacted_events: str,
        output_acted_events: str,
        output_act_rows: str,
    ) -> dict[str, tp.Any]:
        yt_lib_client.client.write_table(firm_tax, [
            {'id': f['id'], 'tax_policies': f['tax_policies']} for
            f in yt_lib_client.client.select_rows(f'* FROM [{yt_lib_client.tables["firm_tax"]}]', format='yson').rows
        ])
        yt_lib_client.client.write_table(accrualer_events, [{'data': json.dumps(a)} for a in agent_acts])

        with yt_lib_client.client.Transaction() as transaction:
            act_o_tron.run_yql(
                yql_client=yql_client,
                accrualer_events_subquery=f'''
                    $accrualer_events =
                    SELECT * FROM `{accrualer_events}`;
                ''',
                reference_firm_tax=firm_tax,
                act_dt=date.today().isoformat(),
                namespace=namespace,
                dry_run=False,
                location_attrs_config=loc_attrs,
                prev_unacted_events=accrualer_events,
                output_unacted_events=output_unacted_events,
                output_acted_events=output_acted_events,
                output_act_rows=output_act_rows,
                transaction=transaction,
            )

        output_unacted_events = list(yt_lib_client.client.read_table(output_unacted_events, format='yson'))
        output_acted_events = list(yt_lib_client.client.read_table(output_acted_events, format='yson'))
        output_act_rows = list(yt_lib_client.client.read_table(output_act_rows, format='yson'))

        return {
            'output_unacted_events': output_unacted_events,
            'output_acted_events': obfuscator.obfuscate_acted_events(output_acted_events),
            'output_act_rows': obfuscator.obfuscate_act_rows(output_act_rows),
        }

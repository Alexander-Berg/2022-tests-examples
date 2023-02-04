import os
import datetime
import typing as tp

import pytest

import yt.wrapper as yt_lib

from billing.hot.tests.lib.templates.processor import BaseProcessorRenderer
from library.python import resource

from billing.hot.tests.clients import base
from billing.hot.tests.clients.accounts import client as accounts
from billing.hot.tests.clients.database import (
    client as db_client, sharded as sharded_db_client
)
from billing.hot.tests.clients.database.accounts.db import AccountsDB
from billing.hot.tests.clients.diod import client as diod
from billing.hot.tests.clients.logbroker import logbroker
from billing.hot.tests.clients.payout import client as payout
from billing.hot.tests.clients.processor import (
    actotron_act_rows_client as actotron_act_rows,
    bnpl_client as bnpl,
    bnpl_income_client as bnpl_income,
    client as processor,
    oplata_client as oplata,
    taxi_client as taxi,
    taxi_light_client as taxi_light,
)
from billing.hot.tests.clients.processor.base_client import Client as BaseProcessorClient
from billing.hot.tests.clients.tvm import client as tvm
from billing.hot.tests.clients.yt.client import Client as YtClient
from billing.hot.tests.config import config as core_config
from billing.hot.tests.lib.oebs import oebs
from billing.hot.tests.lib.state import builder
from billing.hot.tests.lib.templates import (
    accounts as accounts_template,
    oebs as oebs_template,
    yt as yt_template,
    processor as processor_template,
)
from billing.hot.tests.lib.testgen import runner
from billing.hot.tests.lib.test_case.balance_loader import BalancesLoader


@pytest.fixture(scope='session')
def config() -> core_config.Config:
    return core_config.Config.from_yaml(resource.find('config'))


@pytest.fixture(scope='session')
def test_on_recipes(config):
    return not bool(os.getenv('NON_RECIPE_TESTS'))


@pytest.fixture(scope='module')
def tvm_client(config, test_on_recipes):
    return None if test_on_recipes else tvm.get_tvm_client(config.tvm)


@pytest.fixture
def yandex_firm_id() -> int:
    return 1


@pytest.fixture(scope='session')
def balances() -> dict[str, list[accounts_template.Balance]]:
    balances = BalancesLoader.load_balances()
    return {
        namespace: [
            accounts_template.Balance(acc.account, acc.analytic)
            for acc in accounts_analytics
        ]
        for namespace, accounts_analytics in balances.items()
    }


@pytest.fixture(scope='session')
def accounts_renderer(
    balances: dict[str, list[accounts_template.Balance]]
) -> accounts_template.AccountsReadBatchRenderer:
    return accounts_template.AccountsReadBatchRenderer(balances)


@pytest.fixture(scope='module')
def accounts_writebatch_renderer(config) -> accounts_template.AccountsWriteBatchRenderer:
    return accounts_template.AccountsWriteBatchRenderer(config.accounts.template_dir)


@pytest.fixture
@pytest.mark.asyncio
async def base_client(config, tvm_client):
    client = base.BaseClient(tvm_client)
    try:
        yield client
    finally:
        await client.close()


@pytest.fixture
def payout_client(base_client, config) -> payout.Client:
    return payout.Client(base_client, config.payout)


@pytest.fixture
@pytest.mark.asyncio
async def payout_db(config) -> db_client.Client:
    client = db_client.Client(config.payout.db)
    await client.connect()
    try:
        yield client
    finally:
        await client.close()


@pytest.fixture
def processor_client(base_client, config) -> processor.Client:
    return processor.Client(base_client, config.processor)


@pytest.fixture
def actotron_act_rows_client(base_client, config) -> actotron_act_rows.Client:
    return actotron_act_rows.Client(base_client, config.processor)


@pytest.fixture
def taxi_client(base_client, config) -> taxi.Client:
    return taxi.Client(base_client, config.processor)


@pytest.fixture
def taxi_light_client(base_client, config) -> taxi_light.Client:
    return taxi_light.Client(base_client, config.processor)


@pytest.fixture
def oplata_client(base_client, config) -> oplata.Client:
    return oplata.Client(base_client, config.processor)


@pytest.fixture
def bnpl_client(base_client, config) -> bnpl.Client:
    return bnpl.Client(base_client, config.processor)


@pytest.fixture
def diod_client(base_client, config) -> diod.Client:
    return diod.Client(base_client, config.diod)


@pytest.fixture
def bnpl_income_client(base_client, config) -> bnpl_income.Client:
    return bnpl_income.Client(base_client, config.processor)


@pytest.fixture
def accounts_client(base_client, config, accounts_renderer, accounts_writebatch_renderer) -> accounts.Client:
    return accounts.Client(base_client, config.accounts, accounts_renderer, accounts_writebatch_renderer)


@pytest.fixture
@pytest.mark.asyncio
async def accounts_db(config):
    client = sharded_db_client.ShardedClient(config.accounts_db)
    await client.connect()
    try:
        yield AccountsDB(client)
    finally:
        await client.close()


@pytest.fixture
def yt_lib_client(config, yt_client, test_on_recipes) -> YtClient:
    if test_on_recipes:
        client = yt_client
    else:
        yt_config = yt_lib.default_config.get_default_config()
        yt_config['dynamic_table_retries']['total_timeout'] = datetime.timedelta(seconds=30)
        client = yt_lib.YtClient(proxy=config.yt.proxy, config=yt_config)
    yield YtClient(yt_client=client, cfg=config.yt)


@pytest.fixture(scope='module')
def accrualer_logbroker_client(config, tvm_client) -> tp.Optional[logbroker.LogBrokerAPI]:
    yield from _logbroker_client(config.accrualer.log_broker, tvm_client)


@pytest.fixture(scope='module')
def taxi_logbroker_client(config, tvm_client) -> tp.Optional[logbroker.LogBrokerAPI]:
    if not tvm_client:
        yield None
    else:
        yield from _logbroker_client(config.oebs.log_broker['taxi'], tvm_client)


@pytest.fixture(scope='module')
def oplata_logbroker_client(config, tvm_client) -> tp.Optional[logbroker.LogBrokerAPI]:
    if not tvm_client:
        yield None
    else:
        yield from _logbroker_client(config.oebs.log_broker['oplata'], tvm_client)


@pytest.fixture(scope='module')
def bnpl_logbroker_client(config, tvm_client) -> tp.Optional[logbroker.LogBrokerAPI]:
    if not tvm_client:
        yield None
    else:
        yield from _logbroker_client(config.oebs.log_broker['bnpl'], tvm_client)


def _logbroker_client(logbroker_config, tvm_client) -> tp.Optional[logbroker.LogBrokerAPI]:
    client = logbroker.LogBrokerAPI(tvm_client, logbroker_config)
    client.start()
    try:
        yield client
    finally:
        client.stop()


@pytest.fixture(scope='module')
def taxi_oebs_client(config, taxi_logbroker_client) -> oebs.OEBS:
    return oebs.OEBS(
        taxi_logbroker_client,
        oebs_template.OEBSRenderer(oebs_template.OEBSLoader(config.oebs.template_dir)),
    )


@pytest.fixture(scope='module')
def oplata_oebs_client(config, oplata_logbroker_client) -> oebs.OEBS:
    return oebs.OEBS(
        oplata_logbroker_client,
        oebs_template.OEBSRenderer(oebs_template.OEBSLoader(config.oebs.template_dir)),
    )


@pytest.fixture(scope='module')
def bnpl_oebs_client(config, bnpl_logbroker_client) -> oebs.OEBS:
    return oebs.OEBS(
        bnpl_logbroker_client,
        oebs_template.OEBSRenderer(oebs_template.OEBSLoader(config.oebs.template_dir)),
    )


@pytest.fixture
def taxi_oebs_pipeline(config, tvm_client, taxi_oebs_client) -> oebs.OEBSPipeline:
    return oebs.OEBSPipeline(taxi_oebs_client)


@pytest.fixture
def oplata_oebs_pipeline(config, tvm_client, oplata_oebs_client) -> oebs.OEBSPipeline:
    return oebs.OEBSPipeline(oplata_oebs_client)


@pytest.fixture
def bnpl_oebs_pipeline(config, tvm_client, bnpl_oebs_client) -> oebs.OEBSPipeline:
    return oebs.OEBSPipeline(bnpl_oebs_client)


@pytest.fixture
def create_state_builder(yt_lib_client, config):
    contracts = yt_template.YtRenderer(yt_template.YtLoader(config.state_builder.yt_data_dir))

    def _create_state_builder(st=None):
        return builder.Builder(yt_lib_client, contracts, st)

    return _create_state_builder


@pytest.fixture
def processor_loader(config) -> processor_template.ProcessorLoader:
    return processor_template.ProcessorLoader(config.processor.template_dir)


@pytest.fixture
def bnpl_income_renderer(processor_loader) -> processor_template.ProcessorBnplIncomeRenderer:
    return processor_template.ProcessorBnplIncomeRenderer(processor_loader)


@pytest.fixture
def trust_renderer(processor_loader) -> processor_template.ProcessorTrustRenderer:
    return processor_template.ProcessorTrustRenderer(processor_loader)


@pytest.fixture
def yt_loader(config) -> yt_template.YtLoader:
    return yt_template.YtLoader(config.state_builder.yt_data_dir)


@pytest.fixture
def yt_renderer(yt_loader) -> yt_template.YtRenderer:
    return yt_template.YtRenderer(yt_loader)


@pytest.fixture
def state_builder(yt_lib_client, yt_renderer):
    return builder.ExtendedBuilder(yt_lib_client, yt_renderer)


@pytest.fixture
def create_client(
    base_client: base.BaseClient,
    config: core_config.Config
) -> tp.Callable[[tp.Type[BaseProcessorRenderer]], BaseProcessorClient]:
    def _create_client(renderer_cls: tp.Type[BaseProcessorRenderer]) -> BaseProcessorClient:
        return BaseProcessorClient(base_client, config.processor, renderer_cls)
    return _create_client


@pytest.fixture
def processor_clients(
    taxi_client,
    taxi_light_client,
    oplata_client,
    bnpl_client,
) -> dict[str, processor.Client]:
    return {
        'taxi': taxi_client,
        'taxi_light': taxi_light_client,
        'oplata': oplata_client,
        'bnpl': bnpl_client,
    }


@pytest.fixture
def scenario_runner(
    request,
    create_state_builder,
    payout_client,
    accounts_client,
    processor_clients,
) -> runner.TestRunner:
    namespace = request.param
    processor_client = processor_clients[namespace]
    return runner.TestRunner(payout_client, accounts_client, processor_client, create_state_builder)


@pytest.fixture(scope="session")
def yt_tables():
    return core_config.YtTables.from_yaml(resource.find('yt_tables_config'))


@pytest.fixture(scope='session', autouse=True)
def create_yt_tables(yt_client, yt_tables, test_on_recipes):
    if not test_on_recipes:
        yield None
    else:
        def create_yt_table(table: core_config.YtTable) -> None:
            table_attrs = {
                "atomicity": table.atomicity,
                "dynamic": True,
                "schema": table.schema
            }
            yt_client.create('table', table.path, recursive=True, force=True, attributes=table_attrs)
            yt_client.mount_table(table.path, sync=True)

        def remove_yt_table(table: core_config.YtTable) -> None:
            yt_client.unmount_table(table.path, sync=True)
            yt_client.remove(table.path)

        for yt_table in yt_tables.defined_tables:
            create_yt_table(yt_table)

        yield yt_client

        for yt_table in yt_tables.defined_tables:
            remove_yt_table(yt_table)

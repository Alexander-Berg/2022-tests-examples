import pytest

from asyncpg import Connection
from contextlib import asynccontextmanager

from maps_adv.billing_proxy.lib.data_manager.clients import (
    AbstractClientsDataManager,
    ClientsDataManager,
)
from maps_adv.billing_proxy.lib.data_manager.orders import (
    AbstractOrdersDataManager,
    OrdersDataManager,
)
from maps_adv.billing_proxy.lib.data_manager.products import (
    AbstractProductsDataManager,
    ProductsDataManager,
)

from .helpers import coro_mock


@pytest.fixture
def clients_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_clients_dm")
    return request.getfixturevalue("_clients_dm")


@pytest.fixture
def _clients_dm(db):
    return ClientsDataManager(db)


@pytest.fixture
def _mock_clients_dm():
    class MockDm(AbstractClientsDataManager):
        client_exists = coro_mock()
        agency_exists = coro_mock()
        find_client_locally = coro_mock()
        upsert_client = coro_mock()
        list_client_ids = coro_mock()
        sync_client_contracts = coro_mock()
        insert_client = coro_mock()
        set_account_manager_for_client = coro_mock()
        set_representatives_for_client = coro_mock()
        list_agencies = coro_mock()
        list_agency_clients = coro_mock()
        list_client_ids = coro_mock()
        add_clients_to_agency = coro_mock()
        remove_clients_from_agency = coro_mock()
        client_is_in_agency = coro_mock()
        find_contract = coro_mock()
        list_contacts_by_client = coro_mock()
        list_account_manager_clients = coro_mock()
        set_client_has_accepted_offer = coro_mock()
        list_clients = coro_mock()
        list_client_representatives = coro_mock()
        sync_clients_contracts = coro_mock()
        list_clients_with_agencies = coro_mock()
        list_clients_with_orders_with_agency = coro_mock()

        @asynccontextmanager
        async def connection(self, con: Connection = None):
            yield None

    return MockDm()


@pytest.fixture
def products_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_products_dm")
    return request.getfixturevalue("_products_dm")


@pytest.fixture
def _products_dm(db):
    return ProductsDataManager(db)


@pytest.fixture
def _mock_products_dm():
    class MockDm(AbstractProductsDataManager):
        find_product = coro_mock()
        list_products = coro_mock()
        list_by_params = coro_mock()
        find_product_active_version = coro_mock()
        list_clients_bound_to_product = coro_mock()
        bind_client_to_product = coro_mock()
        unbind_client_from_product = coro_mock()
        create_product = coro_mock()
        update_product = coro_mock()

    return MockDm()


@pytest.fixture
def orders_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_orders_dm")
    return request.getfixturevalue("_orders_dm")


@pytest.fixture
def _orders_dm(db, config, balance_client, geoproduct_client):
    return OrdersDataManager(
        db,
        balance_client=balance_client,
        geoproduct_client=geoproduct_client,
        geoproduct_operator_id=config["BALANCE_OPERATOR_UID"],
        skip_balance_api_call_on_orders_charge=config[
            "SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE"
        ],
        use_recalculate_statistic_mode=config["USE_RECALCULATE_STATISTIC_MODE"],
        yt_cluster=config["YT_CLUSTER"],
        yt_token=config["YT_TOKEN"],
        reconciliation_report_dir=config["YT_RECOINCILIATION_REPORT_DIR"],
    )


@pytest.fixture
def _mock_orders_dm():
    class MockDm(AbstractOrdersDataManager):
        list_inexistent_order_ids = coro_mock()
        create_order = coro_mock()
        find_order = coro_mock()
        find_order_by_external_id = coro_mock()
        update_order = coro_mock()
        find_orders = coro_mock()
        list_agency_orders = coro_mock()
        list_client_orders = coro_mock()
        retrieve_order_ids_for_account = coro_mock()
        list_orders_stats = coro_mock()
        list_positive_balance_orders = coro_mock()
        lock_and_return_orders_balance = coro_mock()
        list_orders_debits_for_billed_due_to = coro_mock()
        charge_orders = coro_mock()
        update_orders_limits = coro_mock()
        build_reconciliation_report = coro_mock()
        retrieve_order_id_by_external_id = coro_mock()
        load_geoprod_reconciliation_report = coro_mock()
        list_orders_debits = coro_mock()

    return MockDm()

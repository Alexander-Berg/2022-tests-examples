import pytest

from textwrap import dedent

from payplatform.spirit.match_receipts.lib.base import Cluster, PLATO, HAHN
from payplatform.spirit.match_receipts.lib.datatables.base import (
    DataTable, Field, BouncedField, CollectableDataTable
)
from payplatform.spirit.match_receipts.lib.datatables.yt_types import YtInt64, YtDouble, YtString
from payplatform.spirit.match_receipts.lib.datatables.utils import cast_to_string


class TestPayments(DataTable):
    def __init__(self, cluster: Cluster, table_name: str):
        self.payment_id = Field(YtInt64)
        self.client_id = Field(YtInt64)
        self.total = Field(YtDouble)

        super().__init__(cluster, table_name)


class TestClients(DataTable):
    def __init__(self, cluster: Cluster, table_name: str):
        self.id = Field(YtInt64)
        self.region = Field(YtInt64)

        super().__init__(cluster, table_name)


class TestData(CollectableDataTable):
    def __init__(self, cluster: Cluster, table_name: str, payments: TestPayments, clients: TestClients):
        self.__payments__ = payments
        self.__clients__ = clients

        self.id = BouncedField(payments.payment_id)
        self.sum = BouncedField(payments.total)
        self.casted_sum = BouncedField(payments.total, cast_to_string, YtString)
        self.client_region = BouncedField(clients.region)

        super().__init__(cluster, table_name)

    def source_tables_join(self, cluster: Cluster):
        pass

    def query_text(self) -> str:
        return f"{self.__cluster__.yql_header} insert into {self.path()} select * from {self.__clients__.path(self.__cluster__)};"


test_clients = TestClients(PLATO, 'test_clients')
test_payments = TestPayments(PLATO, 'test_payments')
test_data = TestData(PLATO, 'test_data', test_payments, test_clients)
test_data_hahn = TestData(HAHN, 'test_data_hahn', test_payments, test_clients)


def test_select_strings():
    expected = dedent(
        '''\
        test_payments.`payment_id` as id,
        test_payments.`total` as sum,
        Cast(test_payments.`total` as String) as casted_sum,
        test_clients.`region` as client_region'''
    )
    assert test_data.list_for_select() == expected


@pytest.mark.parametrize('field,string', [
    (test_data.sum, 'test_data.`sum`'),
    (test_data.client_region, 'test_data.`client_region`'),
])
def test_field_name(field, string):
    assert str(field) == string


@pytest.mark.parametrize('field,expected_string', [
    (test_data.casted_sum, 'Cast(test_payments.`total` as String)'),
    (test_data.client_region, 'test_clients.`region`')
])
def test_field_collection(field, expected_string):
    assert field.field_collect_string() == expected_string


@pytest.mark.parametrize('cluster,path_string', [
    (None, '//home_ofd/execution_history/time_string/test_data'),
    (HAHN, '//home_hahn/execution_history/time_string/test_data'),
    (PLATO, '//home_ofd/execution_history/time_string/test_data'),
])
def test_table(cluster: Cluster, path_string):
    assert test_data.path(cluster) == path_string, test_data.path(cluster)
    assert test_data.from_expression(cluster) == f'`{path_string}` as test_data', test_data.from_expression(cluster)

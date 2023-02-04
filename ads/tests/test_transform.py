import pytest
from yt import wrapper as yt

from ads.bsyeti.caesar.tools.yt_sync.commands.main import parse_args
from ads.bsyeti.caesar.tools.yt_sync.commands.tables import ensure_table

from ads.bsyeti.caesar.tools.yt_sync.test.libs.helpers import get_common_args

from ads.bsyeti.caesar.tools.yt_sync.benchmark.libs import benchmark
from ads.bsyeti.caesar.tools.yt_sync.benchmark.libs import transform

TABLE = "AdDomains"


@pytest.fixture
def table():
    return TABLE


def get_key_column():
    return {
        "name": "TestField",
        "type": "uint64",
        "sort_order": "ascending",
    }


def get_ensure_args(common_args, table_desc):
    return parse_args(table_desc, {}, ["table"] + common_args + ["--commit"])


def get_benchmark_args(args, replica, tables_desc):
    # fmt: off
    return benchmark.parse_args(
        [
            "transform",
            "--clusters", replica,
            "--keep-temp-table",
            "--commit",
        ]
        + args,
        tables_desc=tables_desc,
    )
    # fmt: on


def test_transform(table_desc, table, path, clients, clusters):
    primary_client, replica_client = clients
    primary, replica = clusters

    settings = table_desc[TABLE]["test"]
    settings["master"]["cluster"] = primary.get_uri()
    settings["sync_replicas"]["cluster"] = [replica.get_uri()]

    key_column = get_key_column()
    key_column["expression"] = "farm_hash(Domain) % 10"

    schema_generator = table_desc[TABLE]["schema_generator"]

    def new_schema_generator(*args, **kwargs):
        return [key_column] + schema_generator(*args, **kwargs)

    table_desc[TABLE]["schema_generator"] = new_schema_generator

    args = get_common_args(table, path)
    ensure_table(get_ensure_args(args, table_desc))

    rows = [{"Domain": "domain1"}, {"Domain": "domain2"}]
    with yt.Transaction():
        primary_client.insert_rows(path, rows, require_sync_replica=True)

    key_column["expression"] = "farm_hash(Domain)"
    args = get_benchmark_args(args, replica.get_uri(), table_desc)
    transform.run(args)

    table_rows = list(replica_client.read_table("%s.tmp" % path))
    assert sorted([x["Domain"] for x in table_rows]) == [x["Domain"] for x in rows]

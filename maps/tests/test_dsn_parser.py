import pytest

from maps_adv.common.helpers.dsn_parser import parse


@pytest.mark.parametrize(
    "database_url", ("clickhouse://user:pass@hostname:1234/database",)
)
def test_parse_with_one_host(database_url):
    got = parse(database_url)

    assert got == dict(
        user="user",
        password="pass",
        hosts=[{"host": "hostname", "port": 1234}],
        database="database",
        secure=False,
        ca_certs=None,
    )


@pytest.mark.parametrize(
    "database_url, expected_hosts",
    (
        [
            "clickhouse://user:pass@host0:1234,host1:5678/database",
            [{"host": "host0", "port": 1234}, {"host": "host1", "port": 5678}],
        ],
        [
            "clickhouse://user:pass@host0:1234,host1:5678,host2/database",
            [
                {"host": "host0", "port": 1234},
                {"host": "host1", "port": 5678},
                {"host": "host2", "port": 9000},
            ],
        ],
    ),
)
def test_parse_many_hosts(database_url, expected_hosts):
    got = parse(database_url)

    assert got == dict(
        user="user",
        password="pass",
        hosts=expected_hosts,
        database="database",
        secure=False,
        ca_certs=None,
    )


@pytest.mark.parametrize(
    ["database_url", "expected"],
    (
        ["clickhouse://user:pass@host0:1234,host1:5678/database", False],
        ["clickhouse://user:pass@host0:1234/database?secure=true", True],
        ["clickhouse://user:pass@host0:1234/database?secure=false", False],
        ["clickhouse://user:pass@host0:1234/database?secure=1", True],
        ["clickhouse://user:pass@host0:1234/database?secure=0", False],
        ["clickhouse://user:pass@host0:1234/database?secure=", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=true", True],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=false", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=t", True],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=f", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=yes", True],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=no", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=y", True],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=n", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=1", True],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=0", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=", False],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?secure=keklol", False],
    ),
)
def test_will_parse_secure_param(database_url, expected):
    got = parse(database_url)

    assert got["secure"] == expected


@pytest.mark.parametrize(
    ["database_url", "expected"],
    (
        ["clickhouse://user:pass@host0:1234,host1:5678/database", None],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?ca_certs=", None],
        ["clickhouse://user:pass@host0:1234,host1:5678/database?ca_certs=lol", "lol"],
    ),
)
def test_will_parse_ca_certs_param(database_url, expected):
    got = parse(database_url)

    assert got["ca_certs"] == expected


def test_raises_for_unknown_dsn():
    with pytest.raises(ValueError, match="Invalid DSN: prostgresql"):
        parse("prostgresql://user:pass@hostname:1234/database?sslmode=verify-full")

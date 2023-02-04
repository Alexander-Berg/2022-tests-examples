import pytest

from infra.analytics.research.replicas_number_research.compute_sla_resources import compute_replicas_number, \
    compute_shards_number


default_infra = 0.96


def test_compute_replics_number():

    params = [
        (
            3, 89, 98, 6
        ),
        (
            1500, 90, 99.9999, 1819
        ),
        (
            1, 85, 99.9999, 9
        )
    ]

    for par in params:
        assert par[3] == compute_replicas_number(par[0], par[1], par[2], default_infra)


def test_compute_shards_number():

    params = [
        (
            7, 95, 1400, 87, 1722
        ),
        (
            12, 97, 10, 92, 16
        ),
        (
            4, 96, 5, 85, 10
        )
    ]

    for par in params:
        assert par[4] == compute_shards_number(par[0], par[1], par[2], par[3], default_infra)

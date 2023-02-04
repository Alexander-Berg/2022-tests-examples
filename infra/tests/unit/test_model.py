import pytest

from infra.mc_rsc.src import yputil


@pytest.fixture
def mc_rs_with_disabled_clusters(mc_rs, second_cluster):
    mc_rs._disabled_clusters = {second_cluster}
    return mc_rs


@pytest.fixture
def mc_rs_with_disabled_clusters_and_zero_replica_count(mc_rs_with_disabled_clusters):
    for c in mc_rs_with_disabled_clusters.mcrs.spec.clusters:
        c.spec.replica_count = 0
    return mc_rs_with_disabled_clusters


def test_max_unavailable(mc_rs):
    assert mc_rs.max_unavailable() == 2
    # chech that cache mc_rs._max_unavailable works
    assert mc_rs.max_unavailable() == 2


def test_max_unavailable_with_disabled_clusters(mc_rs_with_disabled_clusters):
    assert mc_rs_with_disabled_clusters.max_unavailable() == 1
    # chech that cache mc_rs._max_unavailable works
    assert mc_rs_with_disabled_clusters.max_unavailable() == 1

    mc_rs_with_disabled_clusters.spec.deployment_strategy.max_unavailable = 0
    assert mc_rs_with_disabled_clusters.max_unavailable() == 0


def test_max_unavailable_with_mc_rs_with_disabled_clusters_and_zero_replica_count(mc_rs_with_disabled_clusters_and_zero_replica_count):
    assert mc_rs_with_disabled_clusters_and_zero_replica_count.max_unavailable() == 2


def test_rs_with_disabled_cluster(rs):
    yputil.set_label(rs.labels, 'deploy', {'disabled_clusters': ['ams']})
    assert rs.disabled_clusters() == ['ams']

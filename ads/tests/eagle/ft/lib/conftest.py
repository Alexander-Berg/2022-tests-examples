# -*- coding: utf-8 -*-
import logging

import pytest
from ads.bsyeti.libs.py_testlib import tables
from ads.bsyeti.libs.py_testlib.conftest import *  # noqa
from ads.bsyeti.tests.test_lib.eagle_daemon import launch_eagle, wait_eagle_started
from yatest.common.network import PortManager

from contextlib import contextmanager
from .test_environment import TestEnvironment


@pytest.yield_fixture(scope="session")
def yt_tables(yt_cluster_family):
    return {
        "profiles": tables.ProfilesTable(yt_cluster_family),
        "vulture": tables.VultureCryptaTable(yt_cluster_family),
        "cookies": tables.CookiesTable(yt_cluster_family),
        "search_profiles": tables.SearchPersTable(yt_cluster_family),
        "user_shows": tables.UserShowsTable(yt_cluster_family),
    }


@contextmanager
def test_environment_base(
    fully_ready_yt,
    yt_tables,
    enable_profiles_loader=True,
    enable_kv_saas=False,
    enable_market_dj=False,
    uid_start=100000000,
):
    yt_cluster = fully_ready_yt["clusters"]

    with PortManager() as port_manager:
        eagle_port = port_manager.get_port()
        apphost_port = port_manager.get_port()
        kv_saas_port = port_manager.get_port()
        market_dj_port = port_manager.get_port()
        logging.info("starting eagle on http_port=%d, apphost_port=%d...", eagle_port, apphost_port)
        env = {"YTRPC_CLUSTERS_CONFIG": fully_ready_yt["yt_rpc_proxy_conf_path"], "YT_USER": "root"}
        # just enable models which are used in tests
        enabled_dssm = {
            "FuturePrGGDssmModel",
            "ABDssmModel",
            "JamshidSearchDssmModel",
            "OrganicConvSearchDssmModel",
            "QueryBannerDssmModel",
            "TorchV2ModelFloat16",
        }
        with launch_eagle(
            eagle_port,
            apphost_port=apphost_port,
            kv_saas_port=kv_saas_port,
            market_dj_port=market_dj_port,
            yt_cluster=yt_cluster.replica.get_proxy_address(),
            env=env,
            filter_dssm=enabled_dssm,
            enable_profiles_loader=enable_profiles_loader,
            enable_kv_saas=enable_kv_saas,
            enable_market_dj=enable_market_dj,
        ) as proc:
            wait_eagle_started(eagle_port, proc)
            logging.info("started eagle on http_port=%d, apphost_port=%d", eagle_port, apphost_port)
            yield TestEnvironment(
                eagle_port=eagle_port,
                apphost_port=apphost_port,
                yt_tables=yt_tables,
                kv_saas_port=kv_saas_port,
                market_dj_port=market_dj_port,
                start=uid_start,
            )


@pytest.yield_fixture(scope="session")
def test_environment(fully_ready_yt, yt_tables):
    with test_environment_base(fully_ready_yt, yt_tables, uid_start=100000000) as result:
        yield result


@pytest.yield_fixture(scope="session")
def test_environment_with_kv_saas(fully_ready_yt, yt_tables):
    with test_environment_base(fully_ready_yt, yt_tables, enable_kv_saas=True, uid_start=200000000) as result:
        yield result


@pytest.yield_fixture(scope="session")
def test_environment_with_market_dj(fully_ready_yt, yt_tables):
    with test_environment_base(fully_ready_yt, yt_tables, enable_market_dj=True, uid_start=300000000) as result:
        yield result


@pytest.yield_fixture(scope="session")
def test_environment_with_disabled_profiles_loader(fully_ready_yt, yt_tables):
    with test_environment_base(fully_ready_yt, yt_tables, enable_profiles_loader=False, uid_start=400000000) as result:
        yield result

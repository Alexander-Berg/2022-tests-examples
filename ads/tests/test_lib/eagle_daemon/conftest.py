import logging
import os
from contextlib import contextmanager

import pytest
import yatest.common
from yatest.common.network import PortManager

from .bindings import from_storage_uid, prefixes  # noqa
from .services import launch_balancer, launch_eagle, wait_eagle_started  # noqa
from .yt_tables import *  # noqa

# == DIRS AND FILES ==


@pytest.fixture(scope="module")
def lb_producers_dir():
    dir_name = os.path.join(yatest.common.output_path(), "lb_data")
    if not os.path.exists(dir_name):
        os.mkdir(dir_name)
    return dir_name


@pytest.fixture(scope="module")
def balancer_logs_dir():
    dir_name = os.path.join(yatest.common.output_path(), "balancer_logs")
    if not os.path.exists(dir_name):
        os.makedirs(os.path.join(dir_name, "logs"))
    return dir_name


# == SERVICES ==
@contextmanager
def eagle_core(fully_ready_yt, remote_profiles=False, filter_dssm=None):
    replica_cluster = fully_ready_yt["clusters"][1]

    with PortManager() as port_manager:
        eagle_port = port_manager.get_port()
        apphost_port = port_manager.get_port()
        kv_saas_port = port_manager.get_port()
        market_dj_port = port_manager.get_port()
        logging.info("started eagle on port %d", eagle_port)
        env = {
            "YTRPC_CLUSTERS_CONFIG": fully_ready_yt["yt_rpc_proxy_conf_path"],
        }
        with launch_eagle(
            port=eagle_port,
            apphost_port=apphost_port,
            kv_saas_port=kv_saas_port,
            market_dj_port=market_dj_port,
            yt_cluster=replica_cluster.get_proxy_address(),
            env=env,
            filter_dssm=filter_dssm,
            remote_profiles=remote_profiles
        ) as proc:
            wait_eagle_started(eagle_port, proc)
            yield {"EAGLE_PORT": eagle_port}


@pytest.yield_fixture(scope="module")
def eagle_module(fully_ready_yt):
    with eagle_core(fully_ready_yt) as result:
        yield result


def get_enabled_models():
    return {
        "ABDssmModel",
        "AlbFreshDssmModel",
        "BasylBCRsyaDssmModel",
        "BasylPClickRsyaDssmModel",
        "FuturePrGGDssmModel",
        "JamshidDssmModel",
        "PytorchTsarModel",
        "TorchV2ModelFloat16",
        "TsarDiFactoCompressedUserModelPath",
    }


@pytest.yield_fixture(scope="module")
def eagle_module_with_only_rsya_tsar(fully_ready_yt):
    """launch eagle w/o search tsar modules"""

    with eagle_core(fully_ready_yt, filter_dssm=get_enabled_models()) as result:
        yield result


@pytest.yield_fixture(scope="module")
def eagle_module_without_tsar(fully_ready_yt):
    """
    launch eagle with custom select_type.json file
    IMPORTANT: all Tsars models disabled!
    """
    with eagle_core(fully_ready_yt, filter_dssm=set()) as result:
        yield result


@pytest.yield_fixture(scope="module")
def eagle_remote_module_without_tsar(fully_ready_yt):
    with eagle_core(fully_ready_yt, filter_dssm=set(), remote_profiles=True) as result:
        yield result


@pytest.yield_fixture(scope="module")
def eagle_balancer_module(eagle_module_without_tsar, balancer_logs_dir):
    """
    launch balancer above eagle
    IMPORTANT: all Tsars models disabled!
    """
    with PortManager() as port_manager:
        balancer_port = port_manager.get_port()
        stat_port = port_manager.get_port()
        dg_port = port_manager.get_port()
        admin_port = port_manager.get_port()
        eagle_port = eagle_module_without_tsar["EAGLE_PORT"]

        with launch_balancer(
            balancer_port,
            admin_port,
            stat_port,
            dg_port,
            [("localhost", "::1")],
            eagle_port,
            logs_path=balancer_logs_dir,
        ):
            yield {"BALANCER_PORT": balancer_port}


@pytest.yield_fixture(scope="module")
def eagle_remote_balancer_module(eagle_remote_module_without_tsar, balancer_logs_dir):
    """
    launch balancer above eagle
    IMPORTANT: all Tsars models disabled!
    """
    with PortManager() as port_manager:
        balancer_port = port_manager.get_port()
        stat_port = port_manager.get_port()
        dg_port = port_manager.get_port()
        admin_port = port_manager.get_port()
        eagle_port = eagle_remote_module_without_tsar["EAGLE_PORT"]

        with launch_balancer(
            balancer_port,
            admin_port,
            stat_port,
            dg_port,
            [("localhost", "::1")],
            eagle_port,
            logs_path=balancer_logs_dir,
        ):
            yield {"BALANCER_PORT": balancer_port}

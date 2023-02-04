from infra.unbound.instance import UnboundInstance

import yp.tests.helpers.conftest as yp_conftest

import yt.wrapper as yt

from yatest.common import network
import yatest.common

import logging
import pytest


logging.basicConfig(level=logging.DEBUG)


DEFAULT_UNBOUND_CONFIG = {
    "server": {
        "chroot": "",
        "directory": "",
        "verbosity": 5,
        "log-time-ascii": True,
        "log-local-actions": True,
        "log-queries": True,
        "log-replies": True,
        "log-tag-queryreply": True,
        "log-servfail": True,
        "do-not-query-localhost": False,
    },
    "remote-control": {
        "control-enable": True,
        "control-use-cert": False,
    },
}

DEFAULT_YP_DNS_CONFIG = {
    "LoggerConfig": {
        "Path": "current-eventlog",
        "Level": "DEBUG",
    },
    "BackupLoggerConfig": {
        "Path": "current-replica-eventlog",
        "Level": "DEBUG",
    },
    "ConfigUpdatesLoggerConfig": {
        "Path": "current-config-updates-eventlog",
        "Level": "DEBUG",
    },
    "DynamicZonesConfig": {
        "Enabled": True,
        "ReplicaLoggerConfig": {
            "Path": "current-replica-dns-zones-eventlog",
            "Level": "DEBUG",
        },
        "YPReplicaConfig": {
            "StorageConfig": {
                "ValidationConfig": {"MaxAge": "86400s"},
                "Path": "dns_zones_storage",
                "EnableCache": True,
                "AgeAlertThreshold": "60s",
            },
            "BackupConfig": {
                "Path": "dns_zones_backup",
                "BackupFrequency": "300s",
                "BackupLogFiles": False,
                "MaxBackupsNumber": 5,
            },
            "UseCache": True,
            "ChunkSize": 2000,
            "GetUpdatesMode": "WATCH_UPDATES",
            "WatchObjectsEventCountLimit": 2000,
            "WatchObjectsTimeLimit": "15s",
        },
    },
    "YPReplicaConfig": {
        "StorageConfig": {
            "ValidationConfig": {"MaxAge": "86400s"},
            "Path": "dns_record_sets_storage",
            "AgeAlertThreshold": "60s",
        },
        "BackupConfig": {
            "Path": "dns_record_sets_backup",
            "BackupFrequency": "60s",
            "BackupLogFiles": False,
            "MaxBackupsNumber": 50,
        },
        "ChunkSize": 25000,
        "GetUpdatesMode": "WATCH_UPDATES",
        "WatchObjectsEventCountLimit": 25000,
        "WatchObjectsTimeLimit": "5s",
    },
}

DEFAULT_UNBOUND_ENV_CONFIG = {
    "start_unbound": True,
    "wait_for_yp_dns_start": False,
    "wait_for_yp_dns_readiness": False,
}


class UnboundTestEnvironment(object):
    def __init__(
        self, *,
        unbound_config=None,
        yp_dns_config=None,
        start=True,
        wait_for_yp_dns_start=False,
        wait_for_yp_dns_readiness=False,
    ):
        unbound_config = yt.common.update(DEFAULT_UNBOUND_CONFIG, unbound_config)
        yp_dns_config = yt.common.update(DEFAULT_YP_DNS_CONFIG, yp_dns_config)

        self._workdir = yatest.common.output_path(path='unbound_test_env')

        self.unbound_instance = UnboundInstance(
            workdir=self._workdir,
            unbound_config=unbound_config,
            yp_dns_config=yp_dns_config,
        )

        if start:
            self.start()

            if wait_for_yp_dns_start:
                self.unbound_instance.wait_for_yp_dns_start()

            if wait_for_yp_dns_readiness:
                self.unbound_instance.wait_for_yp_dns_readiness()

    def start(self, *args, **kwargs):
        try:
            self.unbound_instance.start(*args, **kwargs)
        except:
            logging.exception("Failed to start Unbound test environment")
            raise

    def stop(self):
        try:
            self.unbound_instance.stop()
        except:
            logging.exception("Failed to stop Unbound test environment")
            raise


def fill_yp(yp_env, content):
    yp_client = yp_env.yp_instance.create_client()
    for object_type, values in content.items():
        object_type = object_type.removesuffix('s')
        for value in values:
            yp_client.create_object(object_type, attributes=value)


@pytest.fixture(scope="class")
def test_environment(request):
    yp_environments = {}
    yp_grpc_addresses = {}

    def add_yp_env(cluster):
        start_yp = getattr(request.cls, "START", True)
        if cluster not in yp_environments:
            yp_grpc_addresses[cluster] = None

            if not start_yp:
                yp_master_config = getattr(request.cls, "YP_MASTER_CONFIG", {})
                yp_grpc_addresses[cluster] = f"localhost:{network.PortManager().get_port()}"
                yp_master_config.setdefault("client_grpc_server", {})["addresses"] = [
                    {"address": yp_grpc_addresses[cluster]},
                ]
                setattr(request.cls, "YP_MASTER_CONFIG", yp_master_config)

            yp_environments[cluster] = request.getfixturevalue('test_environment_configurable')

            if yp_grpc_addresses[cluster] is None:
                yp_grpc_addresses[cluster] = yp_environments[cluster].yp_instance.yp_client_grpc_address

        return yp_environments[cluster], yp_grpc_addresses[cluster]

    yp_dns_config = getattr(request.cls, "YP_DNS_CONFIG", None)
    cluster_names = set([])
    if yp_dns_config is not None:
        # YP instance for each cluster set in YP DNS config
        cluster_names |= set(map(lambda cluster_config: cluster_config["Name"], yp_dns_config.get("YPClusterConfigs", [])))

        # YP instance for cluster with dynamic zones
        if yp_dns_config.get("DynamicZonesConfig", {}).get("Enabled", DEFAULT_YP_DNS_CONFIG["DynamicZonesConfig"]["Enabled"]):
            cluster_names.add(yp_dns_config["DynamicZonesConfig"]["YPClusterConfig"]["Name"])

    for cluster_name in cluster_names:
        add_yp_env(cluster_name)

    return yp_environments, yp_grpc_addresses


@pytest.fixture(scope="function")
def unbound_env(request, test_environment):
    yp_environments, yp_grpc_addresses = test_environment

    for yp_environment in yp_environments.values():
        yp_conftest.test_method_setup(yp_environment)
        request.addfinalizer(lambda: yp_conftest.test_method_teardown(yp_environment))

    yp_content = getattr(request.cls, "YP_CONTENT", {})
    # TODO: check that YP instances are started
    for cluster, content in yp_content.items():
        fill_yp(yp_environments[cluster], content)

    yp_dns_config = getattr(request.cls, "YP_DNS_CONFIG", None)
    if yp_dns_config is not None:
        for cluster_config in yp_dns_config.get("YPClusterConfigs", []):
            cluster_config["Address"] = yp_grpc_addresses[cluster_config["Name"]]

        # init YP instance for cluster with dynamic zones
        if yp_dns_config.get("DynamicZonesConfig", {}).get("Enabled", DEFAULT_YP_DNS_CONFIG["DynamicZonesConfig"]["Enabled"]):
            yp_dns_config.setdefault("DynamicZonesConfig", {}).setdefault("YPClusterConfig", {})["Address"] = \
                yp_grpc_addresses[yp_dns_config["DynamicZonesConfig"]["YPClusterConfig"]["Name"]]

    env_config = DEFAULT_UNBOUND_ENV_CONFIG | getattr(request.cls, "UNBOUND_ENV_CONFIG", {})

    unbound_environment = UnboundTestEnvironment(
        unbound_config=getattr(request.cls, "UNBOUND_CONFIG", None),
        yp_dns_config=yp_dns_config,
        start=env_config["start_unbound"],
        wait_for_yp_dns_start=env_config["wait_for_yp_dns_start"],
        wait_for_yp_dns_readiness=env_config["wait_for_yp_dns_readiness"],
    )

    request.addfinalizer(lambda: unbound_environment.stop())

    return unbound_environment, yp_environments

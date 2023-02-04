import json
import local_yp
import logging
import pytest
import yatest

from library.python import resource

from yatest.common import network

from infra.yp_dns.daemon import YpDns

from yp.common import YtResponseError
from yp.logger import logger

from yt.wrapper.common import generate_uuid
from yt.wrapper.retries import run_with_retries


logger.setLevel(logging.DEBUG)


OBJECT_TYPES = [
    "dns_record_set",
]

CONFIG_RESOURCE_NAME = "/proto_config.json"

MASTER_NAMES = ["master1"]


def test_method_teardown(yp_client):
    for object_type in OBJECT_TYPES:
        def do():
            for object_ids in yp_client.select_objects(object_type, selectors=["/meta/id"]):
                yp_client.remove_object(object_type, object_ids[0])

        run_with_retries(do, exceptions=(YtResponseError,))


@pytest.fixture(scope="session")
def session_yp_env(request):
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(),
        'yp_{}'.format(generate_uuid()),
        start_proxy=True,
    )
    yp_instance.start()

    config = json.loads(resource.find(CONFIG_RESOURCE_NAME))
    config.update({
        "YPClusterConfigs": [
            {
                "Name": name,
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False,
                "UpdatingFrequency": "5s",
                "Timeout": "5s",
                "Balancing": True,
                "CacheDiscoveryResult": True,
                "UseMasterIpAddress": True,
            } for name in MASTER_NAMES
        ],
    })
    for zone in config["Zones"]:
        zone["YPClusters"] = MASTER_NAMES

    for logger_path in ("BackupLoggerConfig", "YtLoggerConfig", "ConfigUpdatesLoggerConfig"):
        config[logger_path]["DuplicateToUnifiedAgent"] = False
        config[logger_path]["DuplicateToErrorBooster"] = False

    port_manager = network.PortManager()
    port = port_manager.get_port()

    pdns_args = {
        'local-port': port,
        'service-port': port + 1,
        'local-address': '127.0.0.1',
        'cache-ttl': 30,
        'query-cache-ttl': 30,
        'negquery-cache-ttl': 30,
        'soa-minimum-ttl': 600,
        'udp-truncation-threshold': 1232,
    }

    yp_client = yp_instance.create_client()
    yp_dns = YpDns(config, pdns_args)

    request.addfinalizer(lambda: yp_dns.stop())
    request.addfinalizer(lambda: yp_instance.stop())
    return yp_client, yp_dns


@pytest.fixture(scope="function")
def yp_env(request, session_yp_env):
    request.addfinalizer(lambda: test_method_teardown(session_yp_env[0]))
    return session_yp_env

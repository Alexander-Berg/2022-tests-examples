import logging
import pytest

from infra.yp_dns.libs.config.protos import config_pb2

from library.python import resource

import dns.resolver

from google.protobuf import json_format


CONFIG_RESOURCE_NAME = "/proto_config.json"
PROD_CONFIG_RESOURCE_NAME = "/proto_prod_config.json"


def resolve_ip6_address(domain):
    answer = dns.resolver.resolve(domain, "AAAA")
    return map(
        lambda rdata: rdata.address,
        answer
    )


def get_config(resource_name):
    config = config_pb2.TConfig()
    json_format.Parse(resource.find(resource_name), config)
    return config


@pytest.mark.parametrize("resource_name", [CONFIG_RESOURCE_NAME, PROD_CONFIG_RESOURCE_NAME])
def test_clusters_config(resource_name):
    config = get_config(resource_name)
    for idx, cluster in enumerate(config.YPClusterConfigs):
        name = cluster.Name
        logging.info(f"Checking config for cluster {name}...")
        logging.info(f"Config:\n{cluster}")

        assert cluster.Balancing, \
            f"Cluster {name}: YP client balancing is disabled" \
            ", but expected to be enabled"

        assert cluster.CacheDiscoveryResult, \
            f"Cluster {name}: caching of GetMasters results is disabled" \
            ", but expected to be enabled"

        assert cluster.UseMasterIpAddress, \
            f"Cluster {name}: using of YP master ip address instead of fqdn is disabled" \
            ", but expected to be enabled"

        assert len(cluster.TargetFqdn) > 0, \
            f"Cluster {name}: target fqdn is not set" \
            f", it should be set to fqdn of YP master L3: {name}.yp.yandex-team.ru"

        possible_target_fqdns = [
            f"{name}.yp.yandex-team.ru",
            f"{name}.yp.yandex.net",
        ]
        assert cluster.TargetFqdn in possible_target_fqdns, \
            f"Cluster {name}: target fqdn should be set to" \
            f" one of this {' '.join(possible_target_fqdns)}"

        possible_addresses = list(map(
            lambda ip6_address: f"[{ip6_address}]:8090",
            resolve_ip6_address(cluster.TargetFqdn)
        ))
        assert cluster.Address in possible_addresses, \
            f"Cluster {name}: address should be set to" \
            f" one of this {' '.join(possible_addresses)}"

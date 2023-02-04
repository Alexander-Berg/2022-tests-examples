import pytest
import time
import timeout_decorator

import helpers


@pytest.mark.usefixtures("unbound_env")
@pytest.mark.timeout(5)
class TestStart(object):
    def test_start(self, unbound_env):
        pass


@pytest.mark.usefixtures("unbound_env")
class TestStartWithYP(object):
    # YP params
    START = False

    # Unbound params
    UNBOUND_CONFIG = {
        "server": {
            "run-yp-dns": True,
        },
        "auth-zone": {
            "name": "yandex.net",
            "for-downstream": True,
            "for-upstream": True,
            "backend-type": "yp",
            "fallback-enabled": False,
        },
    }

    YP_DNS_CONFIG = {
        "YPClusterConfigs": [
            {
                "Name": "test-cluster",
                "EnableSsl": False,
                "Timeout": "15s",
                "UpdatingFrequency": "0s",
            },
        ],
        "DynamicZonesConfig": {
            "YPClusterConfig": {
                "Name": "test-cluster",
                "EnableSsl": False,
                "Timeout": "15s",
                "UpdatingFrequency": "0s",
            },
        },
        "Zones": [
            {
                "Name": "yandex.net",
                "Nameservers": ["ns1.yp-dns.yandex.net"],
                "YPClusters": ["test-cluster"],
            },
        ],
    }

    UNBOUND_ENV_CONFIG = {
        "wait_for_yp_dns_start": True,
        "wait_for_yp_dns_readiness": False,
    }

    def test_ready_sensor(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        for _ in range(10):
            sensors = unbound_instance.yp_dns_sensors()
            assert helpers.get_sensor_value(sensors, sensor="unbound.yp_dns_service.ready") == 0
            time.sleep(0.05)

        for yp_environment in yp_environments.values():
            yp_environment.start()

        @timeout_decorator.timeout(10, exception_message="YP DNS service is not ready after 10s")
        def wait_for_sensor_ready():
            while True:
                sensors = unbound_instance.yp_dns_sensors()
                if helpers.get_sensor_value(sensors, sensor="unbound.yp_dns_service.ready") == 1:
                    break

        wait_for_sensor_ready()

        for _ in range(10):
            sensors = unbound_instance.yp_dns_sensors()
            assert helpers.get_sensor_value(sensors, sensor="unbound.yp_dns_service.ready") == 1
            time.sleep(0.05)

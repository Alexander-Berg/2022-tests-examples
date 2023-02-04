import logging
import pytest
import time

from textwrap import dedent

import dns.edns
import dns.exception
import dns.message
import dns.query

from infra.unbound.helpers import Args


@pytest.mark.parametrize("run_yp_dns", [True, False])
@pytest.mark.usefixtures("unbound_env")
class TestReload(object):
    # Unbound params
    DEFAULT_LOCAL_ZONES = [
        Args(["test.", "transparent"]),
    ]

    UNBOUND_CONFIG = {
        "server": {
            "run-yp-dns": True,
        },
        "auth-zone": {
            "name": "yandex.test",
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
                "Name": "yandex.test",
                "Nameservers": ["ns1.yp-dns.yandex.net"],
                "YPClusters": ["test-cluster"],
            },
        ],
    }

    UNBOUND_ENV_CONFIG = {
        "wait_for_yp_dns_readiness": True,
    }

    DATAS = [
        f"2a02:6b8:c1b:2a8c:0:696:54cd:{idx:x}"
        for idx in range(5)
    ]

    YP_CONTENT = {
        "test-cluster": {
            "dns_record_sets": [
                {
                    "meta": {"id": "abc.yandex.test"},
                    "spec": {"records": [{"type": "AAAA", "data": DATAS[0]}]},
                },
            ],
        },
    }

    @classmethod
    @pytest.fixture(scope='function', autouse=True)
    def setup(cls, run_yp_dns):
        if run_yp_dns:
            cls.UNBOUND_CONFIG["server"]["run-yp-dns"] = True
            cls.UNBOUND_ENV_CONFIG["wait_for_yp_dns_start"] = True
            cls.UNBOUND_ENV_CONFIG["wait_for_yp_dns_readiness"] = True

            cls.UNBOUND_CONFIG["server"]["local-zone"] = cls.DEFAULT_LOCAL_ZONES
            cls.UNBOUND_CONFIG["server"]["local-data"] = None
            cls.UNBOUND_CONFIG["auth-zone"]["backend-type"] = "yp"
        else:
            cls.UNBOUND_CONFIG["server"]["run-yp-dns"] = False
            cls.UNBOUND_ENV_CONFIG["wait_for_yp_dns_start"] = False
            cls.UNBOUND_ENV_CONFIG["wait_for_yp_dns_readiness"] = False

            cls.UNBOUND_CONFIG["server"]["local-zone"] = cls.DEFAULT_LOCAL_ZONES + [
                Args(["yandex.test.", "static"]),
            ]
            cls.UNBOUND_CONFIG["server"]["local-data"] = [
                "yandex.test. 600 IN NS ns1.yp-dns.yandex.net",
                f"abc.yandex.test. 600 IN AAAA {cls.DATAS[0]}",
            ]
            cls.UNBOUND_CONFIG["auth-zone"]["backend-type"] = "unbound"

    def test_reload(self, unbound_env, run_yp_dns):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        yp_client = yp_environments["test-cluster"].yp_instance.create_client()

        record_set = self.YP_CONTENT["test-cluster"]["dns_record_sets"][0]

        def check_record(expected_data):
            q = dns.message.make_query(record_set["meta"]["id"], "AAAA")
            logging.info(f"Query:\n{q.to_text()}")

            r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
            logging.info(f"Response:\n{r.to_text()}")

            assert r == dns.message.from_text(dedent(f"""\
                id {q.id}
                opcode QUERY
                rcode NOERROR
                flags QR AA RD RA
                ;QUESTION
                {record_set["meta"]["id"]}. IN AAAA
                ;ANSWER
                {record_set["meta"]["id"]}. 600 IN AAAA {expected_data}
                ;AUTHORITY
                ;ADDITIONAL
            """))

        def reload_unbound_and_update_record(data):
            if run_yp_dns:
                new_unbound_config = None
            else:
                new_unbound_config = unbound_instance.unbound_config
                new_unbound_config["server"]["local-data"] = [
                    "yandex.test. 600 IN NS ns1.yp-dns.yandex.net",
                    f"{record_set['meta']['id']}. 600 IN AAAA {data}",
                ]

            unbound_instance.reload(new_unbound_config)

            if run_yp_dns:
                assert not unbound_instance.is_yp_dns_ready(), "YP DNS seems to be not stopped after reload command"
                unbound_instance.wait_for_yp_dns_readiness()

                yp_client.update_object("dns_record_set", record_set["meta"]["id"], set_updates=[
                    {
                        "path": "/spec/records/0/data",
                        "value": data,
                    },
                ])
                # wait for update to be received by YP DNS
                time.sleep(5)

        check_record(self.DATAS[0])
        for data in self.DATAS[1:]:
            reload_unbound_and_update_record(data)
            check_record(data)

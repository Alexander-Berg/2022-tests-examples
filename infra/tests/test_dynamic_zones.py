import logging
import pytest
import time

from textwrap import dedent

import dns.message
import dns.name
import dns.query
import dns.rcode


@pytest.mark.usefixtures("unbound_env")
class TestYpDynamicZones(object):
    UNBOUND_CONFIG = {
        "server": {
            "run-yp-dns": True,
        },
        "auth-zone": [
            {
                "name": "dzone1.yandex.net",
                "for-downstream": True,
                "for-upstream": True,
                "backend-type": "yp",
                "fallback-enabled": False,
            },
            {
                "name": "dzone2.yandex.net",
                "for-downstream": True,
                "for-upstream": True,
                "backend-type": "yp",
                "fallback-enabled": False,
            },
        ],
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
        ],
    }

    UNBOUND_ENV_CONFIG = {
        "wait_for_yp_dns_readiness": True,
    }

    DEFAULT_ZONE_SPEC = {
        "config": {
            "store_config": {"clusters": ["test-cluster"], "algorithm": "merge"},
            "default_soa_record": {"SOA": {"primary_nameserver": "ns9.yandex.net"}, "class": "IN"},
            "default_ns_records": [
                {"NS": {"nameserver": "ns8.yp-dns.yandex.net"}, "class": "IN"},
                {"NS": {"nameserver": "ns9.yp-dns.yandex.net"}, "class": "IN"},
            ]
        }
    }

    YP_CONTENT = {
        "test-cluster": {
            "dns_zones": [
                {
                    "meta": {"id": "dzone1.yandex.net"},
                    "spec": DEFAULT_ZONE_SPEC,
                    "labels": {"yp_dns": {"enable": True}},
                },
            ],
            "dns_record_sets": [
                {
                    "meta": {"id": "lupa.dzone1.yandex.net"},
                    "spec": {"records": [{"type": "AAAA", "data": "2a02:6b8:c1b:2a8c:0:696:54cd:0"}]},
                },
                {
                    "meta": {"id": "pupa.dzone2.yandex.net"},
                    "spec": {"records": [{"type": "AAAA", "data": "2a02:6b8:c18:d1c:0:4e84:acfc:0"}]},
                },
            ],
        },
    }

    def test_dynamic_zones(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        yp_client = yp_environments["test-cluster"].yp_instance.create_client()

        def check_zone(domain, **expected_data):
            q = dns.message.make_query(domain, "AAAA")
            logging.info(f"Query:\n{q.to_text()}")

            r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
            logging.info(f"Response:\n{r.to_text()}")

            if expected_data["rcode"] == "NOERROR":
                assert r == dns.message.from_text(dedent(f"""\
                    id {q.id}
                    opcode QUERY
                    rcode NOERROR
                    flags QR AA RD RA
                    ;QUESTION
                    {domain}. IN AAAA
                    ;ANSWER
                    {domain}. 600 IN AAAA {expected_data["data"]}
                    ;AUTHORITY
                    ;ADDITIONAL
                """))
            elif expected_data["rcode"] == "SERVFAIL":
                assert r == dns.message.from_text(dedent(f"""\
                    id {q.id}
                    opcode QUERY
                    rcode SERVFAIL
                    flags QR AA RD RA
                    ;QUESTION
                    {domain}. IN AAAA
                    ;ANSWER
                    ;AUTHORITY
                    ;ADDITIONAL
                """))
            else:
                assert False, "Assert is not implementted"

        # dzone1.yandex.net - in configuration, dzone2.yandex.net - not
        check_zone("lupa.dzonE1.YAndex.net", rcode="NOERROR", data="2a02:6b8:c1b:2a8c:0:696:54cd:0")
        check_zone("puPA.DZONe2.yanDEx.net", rcode="SERVFAIL")

        # add dzone2.yandex.net
        yp_client.create_object(
            "dns_zone",
            attributes={
                "meta": {"id": "dzone2.yandex.net"},
                "spec": self.DEFAULT_ZONE_SPEC,
                "labels": {"yp_dns": {"enable": True}},
            }
        )

        # TODO: use more proper condition to check whether the confiration is updated
        time.sleep(10)

        check_zone("LUpa.dzone1.yandex.nEt", rcode="NOERROR", data="2a02:6b8:c1b:2a8c:0:696:54cd:0")
        check_zone("pUPA.dzoNe2.yanDEx.net", rcode="NOERROR", data="2a02:6b8:c18:d1c:0:4e84:acfc:0")

        # remove zone dzone1.yandex.net
        yp_client.remove_object("dns_zone", "dzone1.yandex.net")
        time.sleep(10)

        check_zone("lupa.dzone1.YAndex.Net", rcode="SERVFAIL")
        check_zone("pupa.DZOne2.yaNDex.net", rcode="NOERROR", data="2a02:6b8:c18:d1c:0:4e84:acfc:0")

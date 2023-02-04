import logging
import pytest

from functools import partial
from textwrap import dedent

import dns.message
import dns.name
import dns.query
import dns.rcode

import helpers


@pytest.mark.usefixtures("unbound_env")
class TestAuthWildcardsWithYP(object):
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
                "Nameservers": ["ns1.yp-dns.yandex.net", "ns2.yp-dns.yandex.net"],
                "YPClusters": ["test-cluster"],
            },
        ],
    }

    UNBOUND_ENV_CONFIG = {
        "wait_for_yp_dns_readiness": True,
    }

    YP_CONTENT = {
        "test-cluster": {
            "dns_record_sets": [
                {
                    "meta": {"id": "yandex.net"},
                    "spec": {"records": [
                        {"type": "SOA", "data": "ns1.yandex.ru. sysadmin.yandex.ru. 2017854118 900 600 3600000 300"},
                        {"type": "NS", "data": "ns1.yandex.ru."},
                        {"type": "NS", "data": "ns2.yandex.ru."},
                    ]}
                },
                {
                    "meta": {"id": "*.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": "this is a wildcard"},
                        {"type": "MX", "data": "10 host1.yandex.net."},
                    ]}
                },
                {
                    "meta": {"id": "sub.*.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": "this is not a wildcard"},
                    ]}
                },
                {
                    "meta": {"id": "host1.yandex.net"},
                    "spec": {"records": [
                        {"type": "A", "data": "192.0.2.1"},
                    ]}
                },
                {
                    "meta": {"id": "_ssh_._tcp.host1.yandex.net"},
                    "spec": {"records": [
                        {"type": "SRV", "data": "0 0 0 myt6-6605ace04563.qloud-c.yandex.net."},
                    ]}
                },
                {
                    "meta": {"id": "_ssh_._tcp.host2.yandex.net"},
                    "spec": {"records": [
                        {"type": "SRV", "data": "0 0 0 iva5-8ee83844a936.qloud-c.yandex.net."},
                    ]}
                },
                {
                    "meta": {"id": "subdel.yandex.net"},
                    "spec": {"records": [
                        {"type": "NS", "data": "ns.yandex.net.ru"},
                        {"type": "NS", "data": "ns.yandex.net.com"},
                    ]}
                },
                # DISCOVERY-212 case below
                {
                    "meta": {"id": "*.wild.yandex.net"},
                    "spec": {"records": [
                        {"type": "CNAME", "data": "wild.yandex.net"},
                    ]}
                }
            ],
        },
    }

    def test_wildcards(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        get_soa_serial = partial(helpers.get_soa_serial, section='AUTHORITY')

        # case 1
        q = dns.message.make_query("host3.yandex.net", "MX")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            host3.yandex.net. IN MX
            ;ANSWER
            host3.yandex.net. 600 IN MX 10 host1.yandex.net.
            ;AUTHORITY
            ;ADDITIONAL
        """))

        # case 2
        q = dns.message.make_query("host3.yandex.net", "A")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            host3.yandex.net. IN A
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

        # case 3
        q = dns.message.make_query("foo.bar.yandex.net", "TXT")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            foo.bar.yandex.net. IN TXT
            ;ANSWER
            foo.bar.yandex.net. 600 IN TXT "this is a wildcard"
            ;AUTHORITY
            ;ADDITIONAL
        """))

        # case 4
        q = dns.message.make_query("host1.yandex.net", "MX")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            host1.yandex.net. IN MX
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

        # case 5
        q = dns.message.make_query("sub.*.yandex.net", "MX")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            sub.*.yandex.net. IN MX
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

        # case 6
        q = dns.message.make_query("_telnet._tcp.host1.yandex.net", "SRV")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NXDOMAIN
            flags QR AA RD RA
            ;QUESTION
            _telnet._tcp.host1.yandex.net. IN SRV
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

        # case 7
        q = dns.message.make_query("host.subdel.yandex.net", "A")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR RD RA
            ;QUESTION
            host.subdel.yandex.net. IN A
            ;ANSWER
            ;AUTHORITY
            subdel.yandex.net. 600 IN NS ns.yandex.net.ru.
            subdel.yandex.net. 600 IN NS ns.yandex.net.com.
            ;ADDITIONAL
        """))

        # case 8
        q = dns.message.make_query("ghost.*.yandex.net", "MX")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NXDOMAIN
            flags QR AA RD RA
            ;QUESTION
            ghost.*.yandex.net. IN MX
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

        # case 9 DISCOVERY-212
        q = dns.message.make_query("wild.yandex.net", "A")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            wild.yandex.net. IN A
            ;ANSWER
            ;AUTHORITY
            yandex.net. 300 IN SOA ns1.yandex.ru. sysadmin.yandex.ru. {get_soa_serial(r)} 900 600 3600000 300
            ;ADDITIONAL
        """))

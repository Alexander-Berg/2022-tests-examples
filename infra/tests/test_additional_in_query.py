import logging
import pytest

from textwrap import dedent

import dns.message
import dns.name
import dns.query
import dns.rcode
import dns.rdata

import helpers


@pytest.mark.parametrize("allow_unknown_rrs", [True, False])
@pytest.mark.usefixtures("unbound_env")
@pytest.mark.ticket('DISCOVERY-106')
class TestQueryWithAdditional(object):
    UNBOUND_CONFIG = {
        "server": {
            "run-yp-dns": True,
            # next option forces Unbound to skip unknown RRs in ADDITIONAL section
            "skip-unknown-additional-rrs": True,
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
                        {"type": "AAAA", "data": "2a02:6b8::242"},
                    ]}
                },
            ],
        },
    }

    @classmethod
    @pytest.fixture(scope='function', autouse=True)
    def setup(cls, allow_unknown_rrs):
        cls.UNBOUND_CONFIG["server"]["skip-unknown-additional-rrs"] = allow_unknown_rrs

    @pytest.mark.parametrize("use_edns", [False, True])
    def test_query_with_unknown_rr(self, unbound_env, allow_unknown_rrs, use_edns):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        q = dns.message.make_query("yandex.net", "AAAA", use_edns=use_edns)
        q.additional.append(dns.rrset.from_text(".", 0, "IN", 65517, "\\# 4 00000000"))

        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        if allow_unknown_rrs:
            assert r == dns.message.from_text(dedent(f"""\
                id {q.id}
                opcode QUERY
                rcode NOERROR
                flags QR AA RD RA
                ;QUESTION
                yandex.net. IN AAAA
                ;ANSWER
                yandex.net. 600 IN AAAA 2a02:6b8::242
                ;AUTHORITY
                ;ADDITIONAL
            """))
        else:
            if use_edns:
                assert r == dns.message.from_text(dedent(f"""\
                    id {q.id}
                    opcode QUERY
                    rcode FORMERR
                    flags QR RD
                    edns 0
                    payload 1232
                    ;QUESTION
                    yandex.net. IN AAAA
                    ;ANSWER
                    ;AUTHORITY
                    ;ADDITIONAL
                    . 0 IN TYPE65517 {helpers.BACKSLASH_CHAR}# 4 00000000
                """))
            else:
                assert r == dns.message.from_text(dedent(f"""\
                    id {q.id}
                    opcode QUERY
                    rcode FORMERR
                    flags QR RD RA
                    edns 0
                    payload 1232
                    ;QUESTION
                    yandex.net. IN AAAA
                    ;ANSWER
                    ;AUTHORITY
                    ;ADDITIONAL
                """))

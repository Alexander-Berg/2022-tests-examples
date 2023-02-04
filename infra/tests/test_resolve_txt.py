import logging
import pytest

from textwrap import dedent

import dns.message
import dns.query


@pytest.mark.usefixtures("unbound_env")
class TestResolveTXTWithYP(object):
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
                    "meta": {"id": "semicolon.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": 'included"; not included, ":" starts comment here'},
                        {"type": "TXT", "data": 'text\"; this text is included since quotes are escaped'},
                        {"type": "TXT", "data": 'v=DKIM1; g=*; k=rsa; p=MIGfMA0G+W5S3w0bof/p4zHpcIzoscQihz0P'},
                        {"type": "TXT", "data": 'v=rsa;p=MIGfMA0GCSqGSIb/LrBJnk9IjZSzN'},
                        {"type": "TXT", "data": 'v=DMARC1;p=none;rua=mailto:it.foodfox@yandex.ru;ruf=mailto:it.foodfox@yandex.ru;fo=s'},
                        {"type": "TXT", "data": 't=s; o=~;'},
                    ]}
                },
                {
                    "meta": {"id": "onestring.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": ''},
                        {"type": "TXT", "data": '63297a737e744f95b8237b2a7939b245a6789e74ea9ae200ef8425b45307491'},
                        {"type": "TXT", "data": '77 77 asdd qwd'},
                        {"type": "TXT", "data": 'google-site-verification=xcWRAUm33oFckPO6feOjKJ2z6KWrokJkdh-9MQ9N4Ws'},
                        {"type": "TXT", "data": 'v=spf1 include:_spf.yandex.ru ip4:127.0.0.1 -all'},
                    ]}
                },
                {
                    "meta": {"id": "manystrings.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": 'Temporary" "record" "for" "check" "is" "creation" "is" "allowed'},
                        {"type": "TXT", "data": '" "v=DKIM1'},
                        {"type": "TXT", "data": '66" "66'},
                        {"type": "TXT", "data": '121" "12312'},
                    ]}
                },
            ],
        },
    }

    def test_resolve_semicolon_in_data(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        q = dns.message.make_query("semicolon.yandex.net", "TXT")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            semicolon.yandex.net. IN TXT
            ;ANSWER
            semicolon.yandex.net. 600 IN TXT "included"
            semicolon.yandex.net. 600 IN TXT "text\"; this text is included since quotes are escaped"
            semicolon.yandex.net. 600 IN TXT "v=DKIM1; g=*; k=rsa; p=MIGfMA0G+W5S3w0bof/p4zHpcIzoscQihz0P"
            semicolon.yandex.net. 600 IN TXT "v=rsa;p=MIGfMA0GCSqGSIb/LrBJnk9IjZSzN"
            semicolon.yandex.net. 600 IN TXT "v=DMARC1;p=none;rua=mailto:it.foodfox@yandex.ru;ruf=mailto:it.foodfox@yandex.ru;fo=s"
            semicolon.yandex.net. 600 IN TXT "t=s; o=~;"
            ;AUTHORITY
            ;ADDITIONAL
        """))

    def test_resolve_one_string(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        q = dns.message.make_query("onestring.yandex.net", "TXT")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            onestring.yandex.net. IN TXT
            ;ANSWER
            onestring.yandex.net. 600 IN TXT ""
            onestring.yandex.net. 600 IN TXT "63297a737e744f95b8237b2a7939b245a6789e74ea9ae200ef8425b45307491"
            onestring.yandex.net. 600 IN TXT "77 77 asdd qwd"
            onestring.yandex.net. 600 IN TXT "google-site-verification=xcWRAUm33oFckPO6feOjKJ2z6KWrokJkdh-9MQ9N4Ws"
            onestring.yandex.net. 600 IN TXT "v=spf1 include:_spf.yandex.ru ip4:127.0.0.1 -all"
            ;AUTHORITY
            ;ADDITIONAL
        """))

    def test_resolve_many_strings(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        q = dns.message.make_query("manystrings.yandex.net", "TXT")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        assert r == dns.message.from_text(dedent(f"""\
            id {q.id}
            opcode QUERY
            rcode NOERROR
            flags QR AA RD RA
            ;QUESTION
            manystrings.yandex.net. IN TXT
            ;ANSWER
            manystrings.yandex.net. 600 IN TXT "Temporary" "record" "for" "check" "is" "creation" "is" "allowed"
            manystrings.yandex.net. 600 IN TXT "" "v=DKIM1"
            manystrings.yandex.net. 600 IN TXT "66" "66"
            manystrings.yandex.net. 600 IN TXT "121" "12312"
            ;AUTHORITY
            ;ADDITIONAL
        """))

import logging
import pytest

from textwrap import dedent

import dns.flags
import dns.message
import dns.query

from infra.unbound.helpers import Args


@pytest.mark.parametrize("dns64_check_cd", [True, False])
@pytest.mark.usefixtures("unbound_env")
@pytest.mark.ticket('DNS-624')
class TestDns64CheckingDisabled(object):
    UNBOUND_CONFIG = {
        "server": {
            "module-config": "dns64 iterator",
            "dns64-check-cd": False,
            "local-zone": Args(["test.", "typetransparent"]),
            "local-data": [
                "test. 20 IN NS localhost.",
                "test. 20 IN SOA localhost. nobody.invalid. 1 7200 900 1209600 86400",
                "fqdn.test 600 IN A 140.82.121.4",
            ],
        },
        "stub-zone": {
            "name": "test.",
            "stub-addr": "::1@{unbound_port}"
        },
    }

    @classmethod
    @pytest.fixture(scope='function', autouse=True)
    def setup(cls, dns64_check_cd):
        cls.UNBOUND_CONFIG["server"]["dns64-check-cd"] = dns64_check_cd

    @pytest.mark.parametrize("send_cd", [True, False])
    def test_query_with_cd(self, unbound_env, dns64_check_cd, send_cd):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        q = dns.message.make_query("fqdn.test", "AAAA")
        if send_cd:
            q.flags |= dns.flags.CD
        else:
            q.flags &= ~dns.flags.CD

        logging.info(f"Query flags:\n{dns.flags.to_text(q.flags)}")
        logging.info(f"Query:\n{q.to_text()}")

        r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
        logging.info(f"Response:\n{r.to_text()}")

        if dns64_check_cd and send_cd:
            assert r == dns.message.from_text(dedent(f"""\
                id {q.id}
                opcode QUERY
                rcode SERVFAIL
                flags QR RD RA{' CD' if send_cd else ''}
                ;QUESTION
                fqdn.test. IN AAAA
                ;ANSWER
                ;AUTHORITY
                ;ADDITIONAL
            """))
        else:
            assert r == dns.message.from_text(dedent(f"""\
                id {q.id}
                opcode QUERY
                rcode NOERROR
                flags QR RD RA{' CD' if send_cd else ''}
                ;QUESTION
                fqdn.test. IN AAAA
                ;ANSWER
                fqdn.test. 600 IN AAAA 64:ff9b::8c52:7904
                ;AUTHORITY
                ;ADDITIONAL
            """))

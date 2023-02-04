import logging
import pytest
import time

from functools import partial
from textwrap import dedent

import dns.message
import dns.name
import dns.query
import dns.rcode
import dns.rdata

import helpers


@pytest.mark.parametrize("serial_gen_mode", [None, 'ORIGINAL', 'YP_TIMESTAMP_BASED'])
@pytest.mark.usefixtures("unbound_env")
class TestSOASerialSubstWithYP(object):
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

    ZONECFG_SOA_PRIMARY_NAMESERVER = "ns2.yandex.net."
    ZONECFG_SOA_HOSTMASTER = "sysadmin.yandex.net."
    ZONECFG_SOA_TTL = 555
    ZONECFG_SOA_SERIAL = 1448
    ZONECFG_SOA_REFRESH = 1000
    ZONECFG_SOA_RETRY = 666
    ZONECFG_SOA_EXPIRE = 604800
    ZONECFG_SOA_NEGATIVE_TTL = 123

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
                "PrimaryNameserver": ZONECFG_SOA_PRIMARY_NAMESERVER,
                "Hostmaster": ZONECFG_SOA_HOSTMASTER,
                "SOARecordTtl": ZONECFG_SOA_TTL,
                "SOASerial": ZONECFG_SOA_SERIAL,
                "SOARefresh": ZONECFG_SOA_REFRESH,
                "SOARetry": ZONECFG_SOA_RETRY,
                "SOARecordMinimumTtl": ZONECFG_SOA_NEGATIVE_TTL,
            },
        ],
    }

    UNBOUND_ENV_CONFIG = {
        "wait_for_yp_dns_readiness": True,
    }

    SOA_SERIAL = 1337
    SOA_DATA = "ns1.yandex.ru. sysadmin.yandex.ru. {serial} 900 600 3600000 300"
    YP_CONTENT = {
        "test-cluster": {
            "dns_record_sets": [
                {
                    "meta": {"id": "yandex.net"},
                    "spec": {"records": [
                        {"type": "SOA", "data": SOA_DATA.format(serial=SOA_SERIAL)},
                        {"type": "NS", "data": "ns1.yandex.ru."},
                        {"type": "NS", "data": "ns2.yandex.ru."},
                    ]}
                },
            ],
        },
    }

    @classmethod
    @pytest.fixture(scope='function', autouse=True)
    def setup(cls, serial_gen_mode):
        if serial_gen_mode is not None:
            cls.YP_DNS_CONFIG["Zones"][0]["SerialGenerateMode"] = serial_gen_mode
        elif "SerialGenerateMode" in cls.YP_DNS_CONFIG["Zones"][0]:
            cls.YP_DNS_CONFIG["Zones"][0].pop("SerialGenerateMode")

        if serial_gen_mode is None or serial_gen_mode == 'YP_TIMESTAMP_BASED':
            cls.ZONECFG_SOA_DATA = ' '.join(map(str, [
                cls.ZONECFG_SOA_PRIMARY_NAMESERVER,
                cls.ZONECFG_SOA_HOSTMASTER,
                "{serial}",
                cls.ZONECFG_SOA_REFRESH,
                cls.ZONECFG_SOA_RETRY,
                cls.ZONECFG_SOA_EXPIRE,
                cls.ZONECFG_SOA_NEGATIVE_TTL,
            ]))
        elif serial_gen_mode == 'ORIGINAL':
            cls.ZONECFG_SOA_DATA = ' '.join(map(str, [
                cls.ZONECFG_SOA_PRIMARY_NAMESERVER,
                cls.ZONECFG_SOA_HOSTMASTER,
                cls.ZONECFG_SOA_SERIAL,
                cls.ZONECFG_SOA_REFRESH,
                cls.ZONECFG_SOA_RETRY,
                cls.ZONECFG_SOA_EXPIRE,
                cls.ZONECFG_SOA_NEGATIVE_TTL,
            ]))

    def test_soa_serial_substitution(self, unbound_env, serial_gen_mode):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        yp_client = yp_environments["test-cluster"].yp_instance.create_client()

        get_soa_serial = partial(helpers.get_soa_serial, section='ANSWER')

        def check_substitution(expected_soa_pattern, ttl=600):
            # check that:
            # 1. serial number in SOA from record set in YP is substituted with actual serial
            # 2. serial number is non-descreasing number
            first_serial = None
            last_serial = None

            attempts = 10
            for attempt in range(attempts):
                q = dns.message.make_query("yandex.net", "SOA")
                logging.info(f"Query {attempt}:\n{q.to_text()}")

                r = dns.query.udp(q, where=unbound_instance.unbound_host, port=unbound_instance.unbound_port)
                logging.info(f"Response {attempt}:\n{r.to_text()}")

                soa_serial = get_soa_serial(r)
                # assert soa_serial > self.SOA_SERIAL_FAKE
                assert soa_serial >= (last_serial or 0)

                expected_soa = dns.rdata.from_text('IN', 'SOA', expected_soa_pattern.format(serial=soa_serial))
                assert r == dns.message.from_text(dedent(f"""\
                    id {q.id}
                    opcode QUERY
                    rcode NOERROR
                    flags QR AA RD RA
                    ;QUESTION
                    yandex.net. IN SOA
                    ;ANSWER
                    yandex.net. {ttl} IN SOA {expected_soa}
                    ;AUTHORITY
                    ;ADDITIONAL
                """))

                first_serial = first_serial or soa_serial
                last_serial = soa_serial

                if attempt + 1 != attempts:
                    time.sleep(0.5)

            if serial_gen_mode == 'ORIGINAL':
                assert first_serial == last_serial
            else:
                assert first_serial < last_serial

        # check for SOA record when it presents in YP dns record set
        check_substitution(self.SOA_DATA)

        # check for SOA record when there are no SOA record in record sets
        yp_client.remove_object("dns_record_set", "yandex.net")
        # TODO: use more proper condition to check whether the confiration is updated
        time.sleep(10)
        check_substitution(self.ZONECFG_SOA_DATA, ttl=self.ZONECFG_SOA_TTL)

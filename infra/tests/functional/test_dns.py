import pytest
import json

import dns.flags
import dns.rdatatype

from library.python import spack

DEFAULT_TTL = 3600

QUESTION = ';QUESTION'
ANSWER = ';ANSWER'
AUTHORITY = ';AUTHORITY'
ADDITIONAL = ';ADDITIONAL'

SECTIONS = [
    QUESTION,
    ANSWER,
    AUTHORITY,
    ADDITIONAL,
]

AUTH_NAMESERVERS = [
    'ns1.yp-dns.yandex.net',
    'ns2.yp-dns.yandex.net',
]

YP_C_ZONE = 'yp-c.yandex.net.'
SAS_TEST_ZONE = 'sas-test.{}'.format(YP_C_ZONE)

AUTH_YP_ZONES = [
    YP_C_ZONE,
    SAS_TEST_ZONE,
]


NON_ZERO_SENSORS = [
    'yp_dns.pdns.backend.YP_DNS.requests',
    'yp_dns.pdns.key-cache-size',
    'yp_dns.pdns.meta-cache-size',
    'yp_dns.pdns.packetcache-hit',
    'yp_dns.pdns.packetcache-miss',
    'yp_dns.pdns.packetcache-size',
    'yp_dns.pdns.query-cache-hit',
    'yp_dns.pdns.query-cache-miss',
    'yp_dns.pdns.query-cache-size',
    'yp_dns.pdns.rd-queries',
    'yp_dns.pdns.real-memory-usage',
    'yp_dns.pdns.tcp-answers',
    'yp_dns.pdns.tcp-answers-bytes',
    'yp_dns.pdns.tcp-queries',
    'yp_dns.pdns.tcp4-answers',
    'yp_dns.pdns.tcp4-answers-bytes',
    'yp_dns.pdns.tcp4-queries',
    'yp_dns.pdns.udp-answers',
    'yp_dns.pdns.udp-answers-bytes',
    'yp_dns.pdns.udp-queries',
    'yp_dns.pdns.udp4-answers',
    'yp_dns.pdns.udp4-answers-bytes',
    'yp_dns.pdns.udp4-queries',
]

ZERO_SENSORS = [
    'yp_dns.pdns.corrupt-packets',
    'yp_dns.pdns.deferred-cache-inserts',
    'yp_dns.pdns.deferred-cache-lookup',
    'yp_dns.pdns.deferred-packetcache-inserts',
    'yp_dns.pdns.deferred-packetcache-lookup',
    'yp_dns.pdns.dnsupdate-answers',
    'yp_dns.pdns.dnsupdate-changes',
    'yp_dns.pdns.dnsupdate-queries',
    'yp_dns.pdns.dnsupdate-refused',
    'yp_dns.pdns.incoming-notifications',
    'yp_dns.pdns.overload-drops',
    'yp_dns.pdns.qsize-q',
    'yp_dns.pdns.recursing-answers',
    'yp_dns.pdns.recursing-questions',
    'yp_dns.pdns.recursion-unanswered',
    'yp_dns.pdns.servfail-packets',
    'yp_dns.pdns.signature-cache-size',
    'yp_dns.pdns.signatures',
    'yp_dns.pdns.tcp6-answers',
    'yp_dns.pdns.tcp6-answers-bytes',
    'yp_dns.pdns.tcp6-queries',
    'yp_dns.pdns.udp-do-queries',
    'yp_dns.pdns.udp-recvbuf-errors',
    'yp_dns.pdns.udp-sndbuf-errors',
    'yp_dns.pdns.udp6-answers',
    'yp_dns.pdns.udp6-answers-bytes',
    'yp_dns.pdns.udp6-queries',
    'yp_dns.yp_replica.backup.age_indicator',
    'yp_dns.yp_replica.fails_in_a_row',
    'yp_dns.yp_replica.master.chunk_error',
    'yp_dns.yp_replica.master.chunk_failure',
    'yp_dns.yp_replica.master.failures',
]


BLAH_SAS_TEST = 'blah.{}'.format(SAS_TEST_ZONE)


@pytest.mark.usefixtures("yp_env")
class TestDNS(object):
    def make_dns_record_set(self, domain, addresses, rdtype, rdclass='IN'):
        return {
            "meta": {"id": domain},
            "spec": {
                "records": list(map(lambda address: {"class": rdclass, "type": rdtype, "data": address}, addresses))
            }
        }

    def make_dns_record_sets(self, domain_to_addresses, rdtype, rdclass='IN'):
        return tuple(map(
            lambda kv: ("dns_record_set", self.make_dns_record_set(kv[0], kv[1], rdtype, rdclass)),
            domain_to_addresses.items()
        ))

    def response_to_text(self, resp, skip=['id', 'rcode']):
        skip = tuple(skip)
        result = []
        section_name = None
        section = []

        def add_section_to_result(result):
            if section_name is not None:
                result.append(section_name)
                result += sorted(section)

        for data in resp.answer + resp.authority:
            for rd in data:
                if rd.rdtype == dns.rdatatype.SOA:
                    # serial number is not canonizable
                    data.add(rd.replace(serial=0))

        for line in resp.to_text().split('\n'):
            if line.startswith(skip):
                continue
            if line in SECTIONS:
                add_section_to_result(result)
                section_name = line
                section = []
                continue
            if section_name is None:
                result.append(line)
            else:
                section.append(line)
        add_section_to_result(result)

        return '\n'.join(result)

    def add_result(self, result, key, response):
        assert key not in result
        result[key] = self.response_to_text(response)

    def test_NS(self, yp_env):
        _, yp_dns = yp_env

        result = {}

        for zone in AUTH_YP_ZONES:
            resp = yp_dns.udp_tcp(zone, 'NS')
            self.add_result(result, zone, resp)

        return result

    def test_AAAA(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        domain_to_addresses = {
            "d2j7yhdpj5hctkxe.sas-test.yp-c.yandex.net": [
                "2a02:6b8:c0c:b02:100:0:5d8a:0",
                "2a02:6b8:fc08:b02:100:0:56ad:0",
            ],
            "rypj4ada6l2ojkvx.sas-test.yp-c.yandex.net": [
                "a02:6b8:fc10:298:10d:b9c5:cbf5:0",
                "2a02:6b8:c00:298:10d:b9c5:1abb:0",
            ],
        }

        # answer is empty
        for domain, addresses in domain_to_addresses.items():
            resp = yp_dns.udp_tcp(domain, 'AAAA')
            self.add_result(result, f'empty_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # create records
        dns_record_sets = self.make_dns_record_sets(domain_to_addresses, 'AAAA')
        object_ids = yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        for domain, addresses in domain_to_addresses.items():
            resp = yp_dns.udp_tcp(domain, 'AAAA')
            self.add_result(result, f'answer_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # remove records
        for domain in object_ids:
            yp_client.remove_object("dns_record_set", domain)
        yp_dns.wait_update()

        # answer is empty
        for domain, addresses in domain_to_addresses.items():
            resp = yp_dns.udp_tcp(domain, 'AAAA')
            self.add_result(result, f'empty_after_clear_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # answer is empty for a random domain
        resp = yp_dns.udp_tcp(BLAH_SAS_TEST, 'AAAA')
        self.add_result(result, f'empty_{BLAH_SAS_TEST}', resp)
        assert yp_dns.get_answer(resp, BLAH_SAS_TEST, 'AAAA') is None

        return result

    def test_SOA(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        # SOA records in ANSWER section for queries with qname=<zone>
        for zone in AUTH_YP_ZONES:
            resp = yp_dns.udp_tcp(zone, 'SOA')
            self.add_result(result, f'zone_has_answer_{zone}', resp)
            assert yp_dns.get_answer(resp, zone, 'SOA') is not None

        # SOA record in AUTHORITY section for domains with authoritative zone
        resp = yp_dns.udp_tcp(BLAH_SAS_TEST, 'AAAA')
        self.add_result(result, f'has_auth_{BLAH_SAS_TEST}', resp)
        assert yp_dns.get_authority(resp, SAS_TEST_ZONE, 'SOA') is not None

        domain_to_addresses = {
            "5koe4oh4ew6j36vk.sas-test.yp-c.yandex.net": [
                "2a02:6b8:fc08:b02:100:0:d605:0",
                "2a02:6b8:c0c:b02:100:0:bfc3:0",
            ],
            "cderrgym6suzbnij.sas-test.yp-c.yandex.net": [
                "2a02:6b8:fc08:b02:10d:b9c5:2652:0",
                "2a02:6b8:c0c:b02:10d:b9c5:593e:0",
            ],
        }

        dns_record_sets = self.make_dns_record_sets(domain_to_addresses, 'AAAA')
        yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        for domain, addresses in domain_to_addresses.items():
            resp = yp_dns.udp_tcp(domain, 'SOA')
            self.add_result(result, domain, resp)

            assert yp_dns.get_answer(resp, domain, 'SOA') is None
            assert yp_dns.get_authority(resp, domain, 'SOA') is None
            assert yp_dns.get_authority(resp, SAS_TEST_ZONE, 'SOA') is not None

        return result

    def test_SRV(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        base_domain = 'von-dyck-trendbox.von-dyck.education.stable.qloud-d.yandex.net'
        domain_to_srv_data = {
            "_id_.{}".format(base_domain): {
                "0 0 0 latest-master-1.latest-master.von-dyck-trendbox.von-dyck.education.stable.qloud-d.yandex.net",
                "0 0 0 latest-master-2.latest-master.von-dyck-trendbox.von-dyck.education.stable.qloud-d.yandex.net",
            },
            "_ip_.{}".format(base_domain): {
                "0 0 0 2a02:06b8:0c00:3b27:0000:4173:e159:3dec",
                "0 0 0 2a02:06b8:0c0c:731a:0000:4173:3797:9e7a",
            },
            "_host_.{}".format(base_domain): {
                "0 0 0 myt2-e1593dec455b.qloud-c.yandex.net",
                "0 0 0 iva7-37979e7a59f1.qloud-c.yandex.net",
            },
        }

        ask_domains = list(domain_to_srv_data.keys()) + [base_domain]

        # answer is empty
        for domain in ask_domains:
            resp = yp_dns.udp_tcp(domain, 'SRV')
            self.add_result(result, f'empty_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'SRV') is None

        # create records
        dns_record_sets = self.make_dns_record_sets(domain_to_srv_data, 'SRV')
        object_ids = yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        for domain in ask_domains:
            yp_dns.udp_tcp(domain, 'AAAA')
            resp = yp_dns.udp_tcp(domain, 'SRV')
            self.add_result(result, f'answer_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'SRV') is not None

        # remove records
        for domain in object_ids:
            yp_client.remove_object("dns_record_set", domain)
        yp_dns.wait_update()

        # answer is empty
        for domain in ask_domains:
            resp = yp_dns.udp_tcp(domain, 'SRV')
            self.add_result(result, f'empty_after_clear_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'SRV') is None

        # answer is empty for a random domain
        resp = yp_dns.udp_tcp(BLAH_SAS_TEST, 'SRV')
        self.add_result(result, f'empty_{BLAH_SAS_TEST}', resp)
        assert yp_dns.get_answer(resp, BLAH_SAS_TEST, 'SRV') is None

        return result

    def test_CNAME(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        domain = 'd2j7yhdpj5hctkxe.sas-test.yp-c.yandex.net'
        www_domain = 'www.{}'.format(domain)
        domain_to_addresses = {
            domain: [
                "2a02:6b8:c0c:b02:100:0:5d8a:0",
                "2a02:6b8:fc08:b02:100:0:56ad:0",
            ],
        }

        # answer is empty
        for name in [domain, www_domain]:
            resp = yp_dns.udp_tcp(name, 'CNAME')
            self.add_result(result, f'empty_{name}', resp)
            for lookup_name in [domain, www_domain]:
                assert yp_dns.get_answer(resp, lookup_name, 'CNAME') is None
                assert yp_dns.get_answer(resp, lookup_name, 'AAAA') is None

        # create records
        dns_record_sets = self.make_dns_record_sets(domain_to_addresses, 'AAAA')
        dns_record_sets += self.make_dns_record_sets({www_domain: [domain]}, 'CNAME')
        yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        # check CNAME
        resp = yp_dns.udp_tcp(www_domain, 'CNAME')
        self.add_result(result, f'answer_{www_domain}_CNAME', resp)
        assert yp_dns.get_answer(resp, www_domain, 'CNAME') is not None
        assert yp_dns.get_answer(resp, www_domain, 'AAAA') is None
        assert yp_dns.get_answer(resp, domain, 'CNAME') is None
        assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # check AAAA
        resp = yp_dns.udp_tcp(www_domain, 'AAAA')
        self.add_result(result, f'answer_{www_domain}_AAAA', resp)
        assert yp_dns.get_answer(resp, www_domain, 'CNAME') is not None
        assert yp_dns.get_answer(resp, www_domain, 'AAAA') is None
        assert yp_dns.get_answer(resp, domain, 'CNAME') is None
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # remove AAAA records
        yp_client.remove_object("dns_record_set", domain)
        yp_dns.wait_update()

        resp = yp_dns.udp_tcp(www_domain, 'AAAA')
        self.add_result(result, f'answer_after_AAAA_clear_{www_domain}_AAAA', resp)
        assert yp_dns.get_answer(resp, www_domain, 'CNAME') is not None
        assert yp_dns.get_answer(resp, www_domain, 'AAAA') is None
        assert yp_dns.get_answer(resp, domain, 'CNAME') is None
        # assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # remove CNAME record
        yp_client.remove_object("dns_record_set", www_domain)
        yp_dns.wait_update()

        for rdtype in ['AAAA', 'CNAME']:
            resp = yp_dns.udp_tcp(www_domain, rdtype)
            self.add_result(result, f'answer_after_clear_{www_domain}_{rdtype}', resp)
            for lookup_name in [www_domain, domain]:
                assert yp_dns.get_answer(resp, lookup_name, 'CNAME') is None
                assert yp_dns.get_answer(resp, lookup_name, 'AAAA') is None

        return result

    def test_ANY(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        domain_to_addresses = {
            "bwxouz2ew6kakhjb.sas-test.yp-c.yandex.net": [
                "2a02:6b8:c0c:a82:100:0:5412:0",
                "2a02:6b8:fc08:a82:100:0:446d:0",
            ],
            "d6wg3nyyebigfz5f.sas-test.yp-c.yandex.net": [
                "2a02:6b8:fc00:1411:10d:b9c5:87f4:0",
                "2a02:6b8:c08:1491:10d:b9c5:4204:0",
            ],
        }

        # answer is empty
        for domain in domain_to_addresses.keys():
            resp = yp_dns.tcp(domain, 'ANY')
            self.add_result(result, f'empty_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # create records
        dns_record_sets = self.make_dns_record_sets(domain_to_addresses, 'AAAA')
        object_ids = yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        for domain in domain_to_addresses.keys():
            resp = yp_dns.tcp(domain, 'ANY')
            self.add_result(result, domain, resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # remove records
        for domain in object_ids:
            yp_client.remove_object("dns_record_set", domain)
        yp_dns.wait_update()

        # answer is empty
        for domain, addresses in domain_to_addresses.items():
            resp = yp_dns.tcp(domain, 'ANY')
            self.add_result(result, f'empty_after_clear_{domain}', resp)
            assert yp_dns.get_answer(resp, domain, 'AAAA') is None

        # answer is empty for a random domain
        resp = yp_dns.tcp(BLAH_SAS_TEST, 'ANY')
        self.add_result(result, f'empty_{BLAH_SAS_TEST}', resp)
        assert yp_dns.get_answer(resp, BLAH_SAS_TEST, 'AAAA') is None
        assert yp_dns.get_authority(resp, SAS_TEST_ZONE, 'SOA') is not None

        return result

    def test_truncate(self, yp_env):
        yp_client, yp_dns = yp_env

        result = {}

        domain_to_addresses = {
            "small.sas-test.yp-c.yandex.net": [
                "2a02:6b8:c0c:a82:100:0:5412:0",
            ],
            "too-long-for-udp-query.sas-test.yp-c.yandex.net": [
                "2a02:6b8:c0c:a82:100:0:5412:{}".format(i) for i in range(20)
            ],
            "too-long-for-edns-query.sas-test.yp-c.yandex.net": [
                "2a02:6b8:c0c:a82:100:0:5412:{}".format(i) for i in range(50)
            ],
        }

        dns_record_sets = self.make_dns_record_sets(domain_to_addresses, 'AAAA')
        yp_client.create_objects(dns_record_sets)
        yp_dns.wait_update()

        # small answer, no TC flag
        domain = "small.sas-test.yp-c.yandex.net"
        resp = yp_dns.udp(domain, 'AAAA')
        assert resp.flags & dns.flags.TC == 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # TC flag in udp response, full answer in TCP
        domain = "too-long-for-udp-query.sas-test.yp-c.yandex.net"
        resp = yp_dns.udp(domain, 'AAAA')
        self.add_result(result, f'udp_tc=1_{domain}', resp)
        assert resp.flags & dns.flags.TC != 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        resp = yp_dns.tcp(domain, 'AAAA')
        assert resp.flags & dns.flags.TC == 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # no TC flag with edns (bigger payload)
        domain = "too-long-for-udp-query.sas-test.yp-c.yandex.net"
        resp = yp_dns.udp(domain, 'AAAA', use_edns=0)
        self.add_result(result, f'edns_tc=0_{domain}', resp)
        assert resp.flags & dns.flags.TC == 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        # TC flag with edns
        domain = "too-long-for-edns-query.sas-test.yp-c.yandex.net"
        resp = yp_dns.udp(domain, 'AAAA', use_edns=0)
        self.add_result(result, f'edns_tc=1_{domain}', resp)
        assert resp.flags & dns.flags.TC != 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        resp = yp_dns.tcp(domain, 'AAAA')
        assert resp.flags & dns.flags.TC == 0
        assert yp_dns.get_answer(resp, domain, 'AAAA') is not None

        return result

    def test_sensors(self, yp_env):
        yp_client, yp_dns = yp_env
        attempt_count = 2048
        for i in range(attempt_count):
            yp_dns.ping()
            yp_dns.sensors()
            yp_dns.reopen_log()

        str_data = spack.to_json(yp_dns.sensors().content)
        sensors_json = json.loads(str_data)["sensors"]
        result = []

        for sensor in sensors_json:
            if sensor['kind'] == 'HIST_RATE':
                s = sensor['labels']['sensor']
                if len(s) >= 4 and s[-4:] == 'time':
                    result.append([s, sum(sensor['hist']['buckets']) + sensor['hist']['inf']])
            else:
                name = sensor['labels']['sensor']
                if name in NON_ZERO_SENSORS:
                    assert sensor['value'] > 0, 'sensor: {}'.format(name)
                elif name in ZERO_SENSORS:
                    assert sensor['value'] == 0

        for sensor in result:
            sensor_name = sensor[0].split('.')
            if sensor_name[1] != 'pdns':
                continue
            if sensor_name[2] == 'backend' or sensor_name[2] == 'shutdown_response_time':
                continue
            assert int(sensor[1]) >= attempt_count, 'sensor: {}'.format(sensor_name)

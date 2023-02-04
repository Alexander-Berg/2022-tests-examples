import unittest
import yatest.common as ytc
import os
import re
import netaddr
import logging

from dns import resolver
from dns import reversename

hbf_data_path = ytc.source_path('infra/rtc/packages/yandex-hbf-agent/share/rules.d')
etc_data_path = ytc.source_path('infra/rtc/packages/yandex-hbf-agent/rtc/etc/yandex-hbf-agent/rules.d/')
config_spec_data_path = ytc.source_path('infra/rtc/packages/yandex-hbf-agent/share/')

config_sepc_path = os.path.join(config_spec_data_path, 'yandex-hbf-agent.configspec')

hbf_v4_rules_path = os.path.join(hbf_data_path, '05-hbf-server.v4')
hbf_v6_rules_path = os.path.join(hbf_data_path, '05-hbf-server.v6')

dns_cache_v4_rules_path = os.path.join(hbf_data_path, '10-dns.v4')
dns_cache_v6_rules_path = os.path.join(hbf_data_path, '10-dns.v6')

its_v4_rules_path = os.path.join(etc_data_path, '15-its.v4')
its_v6_rules_path = os.path.join(etc_data_path, '15-its.v6')

scanners_v4_rules_path = os.path.join(etc_data_path, '40-yascan.v4')
scanners_v6_rules_path = os.path.join(etc_data_path, '40-yascan.v6')

hbf_servers = (
    'hbf.yandex.net',
    'new-man-hbf.yandex.net',
    'new-sas-hbf.yandex.net',
    'new-vla-hbf.yandex.net',
    'new-vlx-hbf.yandex.net',
    'new-iva-hbf.yandex.net',
    'new-myt-hbf.yandex.net',
)

its_servers = (
    'its.yandex-team.ru',
)

dns_cache_servers = (
    'ns-cache.yandex.net',
    'dns-cache.yandex.net',
)

scanners = (
    'yascan-1.yandex.net',
)

append_rule_re = re.compile(r'^\-A\s+\w+\s+(-p\s+\w+\s+)?\-[ds]\s+(?P<ip>[a-fA-F\d\.\:]+).*$')
insert_rule_re = re.compile(r'^\-I\s+\w+\s+\d+\s+(-p\s+\w+\s+)?\-[ds]\s+(?P<ip>[a-fA-F\d\.\:]+).*$')
server_ips_re = re.compile(r'^server_ips\s+=\s+.*$')

# We need to manually set dns servers, because ns64-cache.yandex.net don't answers with proper A records.
RES = resolver.Resolver()
RES.nameservers = ['2a02:6b8::1:1', '2a02:6b8:0:3400::1']


def resolve_fqdn_to_netaddr(fqdn):
    results = set()
    for rec_type in ('A', 'AAAA'):
        try:
            answers = RES.query(fqdn, rec_type)
        except resolver.NoAnswer:
            answers = ()
        results |= {netaddr.IPAddress(rdata.address) for rdata in answers}
    return results


def resolve_ptr(ip):
    # Use resolver for consistency
    addr = reversename.from_address(str(ip))
    try:
        return str(resolver.query(addr, "PTR")[0])
    except Exception:
        return ""


def parse_ips_from_rule_file(rules_file):
    res = set()
    with open(rules_file, 'r') as f:
        for line in f:
            for rule_variation in [append_rule_re, insert_rule_re]:
                try:
                    res.add(netaddr.IPAddress(rule_variation.match(line).group('ip')))
                except AttributeError:
                    pass
    return res


def parse_server_ips_from_configspec(configspec_file):
    res = set()
    with open(configspec_file, 'r') as f:
        for line in f:
            found = server_ips_re.match(line)
            if found:
                logging.info("found: {}".format(found.string))
                ips = re.findall(r'("[A-Za-z0-9:.]+")', found.string)
                logging.info("ips: {}".format(ips))
                for i in ips:
                    res.add(netaddr.IPAddress(i.strip('"')))
    return res


def resolve_ips(servers):
    resolved_ips = set()
    for h in servers:
        resolved_ips |= resolve_fqdn_to_netaddr(h)
    return resolved_ips


def get_ips_from_rules(rules_files):
    ips_from_rules = set()
    for r in rules_files:
        ips_from_rules |= parse_ips_from_rule_file(r)
    return ips_from_rules


class TestServerIPsChanged(unittest.TestCase):
    def test_hbf_server(self):
        resolved_ips = resolve_ips(hbf_servers)
        ips_from_rules = get_ips_from_rules((hbf_v4_rules_path, hbf_v6_rules_path))
        assert resolved_ips == ips_from_rules

    def test_its(self):
        resolved_ips = resolve_ips(its_servers)
        ips_from_rules = get_ips_from_rules((its_v4_rules_path, its_v6_rules_path))
        assert resolved_ips == ips_from_rules

    def test_ns_caches(self):
        resolved_ips = resolve_ips(dns_cache_servers)
        ips_from_rules = get_ips_from_rules((dns_cache_v4_rules_path, dns_cache_v6_rules_path))
        assert resolved_ips == ips_from_rules

    def test_scanners(self):
        resolved_ips = resolve_ips(scanners)
        ips_from_rules = get_ips_from_rules((scanners_v4_rules_path, scanners_v6_rules_path))
        print ips_from_rules
        assert resolved_ips == ips_from_rules

    def test_find_config_ips(self):
        server_ips = parse_server_ips_from_configspec(config_sepc_path)
        for i in server_ips:
            assert resolve_ptr(i) in hbf_servers

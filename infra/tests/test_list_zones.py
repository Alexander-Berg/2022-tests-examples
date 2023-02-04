import pytest


@pytest.mark.usefixtures("unbound_env")
class TestListYpZones(object):
    UNBOUND_CONFIG = {
        "server": {
            "run-yp-dns": True,
        },
        "auth-zone": [
            {
                "name": "dynamic.yandex.net",
                "for-downstream": True,
                "for-upstream": True,
                "backend-type": "yp",
                "fallback-enabled": False,
            },
            {
                "name": "static.yandex.net",
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
            {
                "Name": "static.yandex.net",
                "PrimaryNameserver": "ns9.yandex.net",
                "Nameservers": [
                    "ns8.yp-dns.yandex.net",
                    "ns9.yp-dns.yandex.net",
                ],
                "IsAuthoritative": True,
                "YPClusters": ["test-cluster"],
            },
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
                    "meta": {"id": "dynamic.yandex.net"},
                    "spec": DEFAULT_ZONE_SPEC,
                    "labels": {"yp_dns": {"enable": True}},
                },
            ],
            "dns_record_sets": [
                {
                    "meta": {"id": "lupa.dynamic.yandex.net"},
                    "spec": {"records": [{"type": "AAAA", "data": "2a02:6b8:c1b:2a8c:0:696:54cd:0"}]},
                },
                {
                    "meta": {"id": "txt.dynamic.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": "v=DKIM1; k=rsa; t=s; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GN"},
                        {"type": "TXT", "data": 'first" "second" "third'},
                    ]},
                },
                {
                    "meta": {"id": "pupa.static.yandex.net"},
                    "spec": {"records": [{"type": "AAAA", "data": "2a02:6b8:c18:d1c:0:4e84:acfc:0"}]},
                },
                {
                    "meta": {"id": "txt.static.yandex.net"},
                    "spec": {"records": [
                        {"type": "TXT", "data": "v=DKIM1; k=rsa; t=f; p=MIGfMA0GCSqGSIb3dqebaquaa4GN"},
                        {"type": "TXT", "data": '1st" "2nd" "3rd'},
                    ]},
                },
            ],
        },
    }

    def get_soa_serial(self, zone_records):
        for record in zone_records:
            if 'SOA' in record:
                tokens = record.split()
                return int(tokens[tokens.index('SOA') + 3])

    def check_zone_data(self, unbound_instance, zone, expected_status, expected_records, mode='json', replace_soa_serial=True):
        resp = unbound_instance.list_zone_data(zone, mode=mode)
        resp_status = resp['status'] if mode == 'json' else 'OK'
        resp_data = (resp['data'] if mode == 'json' else resp.decode('utf-8')).splitlines()

        if replace_soa_serial:
            serial = self.get_soa_serial(resp_data)
            assert isinstance(serial, int) and serial > 0
            for idx, record in enumerate(expected_records):
                if 'SOA' in record:
                    expected_records[idx] = record.format(serial=serial)

        assert resp_status == expected_status
        assert resp_data == expected_records

    def test_list_zones_data(self, unbound_env):
        unbound_environment, yp_environments = unbound_env
        unbound_instance = unbound_environment.unbound_instance

        for mode in ('json', 'raw'):
            self.check_zone_data(
                unbound_instance,
                zone='static.yandex.net',
                expected_status='OK',
                expected_records=[
                    'pupa.static.yandex.net. 600 IN AAAA 2a02:6b8:c18:d1c:0:4e84:acfc:0',
                    'txt.static.yandex.net. 600 IN TXT "v=DKIM1; k=rsa; t=f; p=MIGfMA0GCSqGSIb3dqebaquaa4GN"',
                    'txt.static.yandex.net. 600 IN TXT "1st" "2nd" "3rd"',
                    'static.yandex.net. 3600 IN SOA ns9.yandex.net. sysadmin.yandex-team.ru. {serial} 900 600 604800 120',
                    'static.yandex.net. 3600 IN NS ns8.yp-dns.yandex.net',
                    'static.yandex.net. 3600 IN NS ns9.yp-dns.yandex.net',
                ],
                mode=mode,
            )

            self.check_zone_data(
                unbound_instance,
                zone='dynamic.yandex.net',
                expected_status='OK',
                expected_records=[
                    'lupa.dynamic.yandex.net. 600 IN AAAA 2a02:6b8:c1b:2a8c:0:696:54cd:0',
                    'txt.dynamic.yandex.net. 600 IN TXT "v=DKIM1; k=rsa; t=s; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GN"',
                    'txt.dynamic.yandex.net. 600 IN TXT "first" "second" "third"',
                    'dynamic.yandex.net. 3600 IN SOA ns9.yandex.net. sysadmin.yandex-team.ru. {serial} 900 600 604800 120',
                    'dynamic.yandex.net. 3600 IN NS ns8.yp-dns.yandex.net',
                    'dynamic.yandex.net. 3600 IN NS ns9.yp-dns.yandex.net',
                ],
                mode=mode,
            )

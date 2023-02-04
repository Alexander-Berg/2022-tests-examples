import json
import local_yp
import logging
import pytest
import yatest

import dns.rdtypes

from infra.yp_dns.daemon import YpDns

from yp.local import reset_yp
from yp.logger import logger
from yt.wrapper.common import generate_uuid

from yatest.common import network

from library.python import resource


logger.setLevel(logging.DEBUG)

YP_DNS_CONFIG_RESOURCE_NAME = "/proto_config.json"

ZONE1 = "zone1.yandex.net"

REVERSE_ZONE1 = "8.b.6.0.2.0.a.2.ip6.arpa"

AUTH_ZONES = [
    ZONE1,

    REVERSE_ZONE1,
]


def unpack(record):
    if isinstance(record, dns.rdtypes.IN.AAAA.AAAA):
        rdclass = dns.rdataclass.to_text(record.rdclass)
        rdtype = dns.rdatatype.to_text(record.rdtype)
        data = record.address
    elif isinstance(record, dns.rdtypes.ANY.PTR.PTR):
        rdclass = dns.rdataclass.to_text(record.rdclass)
        rdtype = dns.rdatatype.to_text(record.rdtype)
        data = record.target.to_text(omit_final_dot=True)
    else:
        raise Exception("'unpack' is not implemented for record type '{}'".format(type(record)))

    return rdclass, rdtype, data


class AxfrServer:
    def __init__(self, yp_instance, yp_dns):
        self._yp_client = yp_instance.create_client()
        self._yp_dns = yp_dns

    @property
    def address(self):
        return self._yp_dns.address

    @property
    def port(self):
        return self._yp_dns.port

    def resume(self):
        self._yp_dns.resume()

    def suspend(self):
        self._yp_dns.suspend()

    def add_record(self, domain, record):
        yp_client = self._yp_client
        rdclass, rdtype, data = unpack(record)

        dns_record_set = yp_client.get_object(
            "dns_record_set", domain,
            selectors=["/spec/records"],
            options={"ignore_nonexistent": True}
        )
        records = None if dns_record_set is None else dns_record_set[0]

        if records is None:
            yp_client.create_object("dns_record_set", attributes={
                "meta": {"id": domain},
                "spec": {"records": []},
            })
            records = []

        records.append({
            "type": rdtype,
            "data": data,
            "class": rdclass,
        })

        yp_client.update_object("dns_record_set", domain, set_updates=[
            {
                "path": "/spec/records",
                "value": records,
            }
        ])

        self._yp_dns.wait_update()

    def remove_record(self, domain, record):
        yp_client = self._yp_client
        rdclass, rdtype, data = unpack(record)

        records = yp_client.get_object(
            "dns_record_set", domain,
            selectors=["/spec/records"],
            options={"ignore_nonexistent": True}
        )[0]

        if records is None:
            return

        records = list(filter(
            lambda record: record["type"] != rdtype or record["class"] != rdclass or record["data"] != data,
            records
        ))

        yp_client.update_object("dns_record_set", domain, set_updates=[
            {
                "path": "/spec/records",
                "value": records,
            }
        ])

        self._yp_dns.wait_update()

    def remove_record_set(self, domain):
        yp_client = self._yp_client

        yp_client.remove_object("dns_record_set", domain)

        self._yp_dns.wait_update()


def get_yp_instance():
    uuid = generate_uuid()
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(uuid),
        'yp_{}'.format(uuid),
        start_proxy=True,
    )
    yp_instance.start()
    return yp_instance


def get_yp_dns(yp_instance):
    config = json.loads(resource.find(YP_DNS_CONFIG_RESOURCE_NAME))
    config.update({
        "LoggerConfig": {
            "Backend": "FILE",
            "Level": "DEBUG",
        },
        "BackupLoggerConfig": {
            "Backend": "FILE",
            "Level": "DEBUG",
        },
        "YtLoggerConfig": {
            "Backend": "FILE",
            "Level": "DEBUG",
        },
        "ConfigUpdatesLoggerConfig": {
            "Backend": "FILE",
            "Level": "DEBUG",
        },
        "YPClusterConfigs": [
            {
                "Name": "master1",
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False,
                "UpdatingFrequency": "0s",
                "Timeout": "5s",
            },
        ],
        "Zones": [
            {
                "Name": zone,
                "PrimaryNameserver": "ns1.yp-dns.yandex.net",
                "Nameservers": [
                    "ns1.yp-dns.yandex.net",
                    "ns2.yp-dns.yandex.net",
                ],
                "IsAuthoritative": True,
                "YPClusters": [
                    "master1",
                ]
            } for zone in AUTH_ZONES
        ],
    })

    port_manager = network.PortManager()
    port = port_manager.get_port()

    config["LoggerConfig"]["Path"] = 'yp_dns_{}/eventlog'.format(port)
    config["BackupLoggerConfig"]["Path"] = 'yp_dns_{}/backup_eventlog'.format(port)
    config["YtLoggerConfig"]["Path"] = 'yp_dns_{}/yt_client_eventlog'.format(port)
    config["ConfigUpdatesLoggerConfig"]["Path"] = 'yp_dns_{}/config_updates_eventlog'.format(port)
    config["YPReplicaConfig"]["StorageConfig"]["Path"] = 'yp_dns_{}/storage'.format(port)
    config["YPReplicaConfig"]["BackupConfig"]["Path"] = 'yp_dns_{}/backup'.format(port)

    pdns_args = {
        'local-port': port,
        'service-port': port + 1,
        'local-address': '127.0.0.1',
        'cache-ttl': 30,
        'query-cache-ttl': 30,
        'negquery-cache-ttl': 30,
        'soa-minimum-ttl': 600,
        'udp-truncation-threshold': 1232,
        'socket-dir': 'yp_dns_{}'.format(port),
    }

    return YpDns(config, pdns_args)


@pytest.fixture(scope="session")
def session_ctl_env(request):
    yp_instance = get_yp_instance()
    yp_dns = get_yp_dns(yp_instance)

    yp_instance_for_axfr_server = get_yp_instance()
    yp_dns_for_axfr = get_yp_dns(yp_instance_for_axfr_server)

    request.addfinalizer(lambda: yp_instance.stop())
    request.addfinalizer(lambda: yp_dns.stop())
    request.addfinalizer(lambda: yp_instance_for_axfr_server.stop())
    request.addfinalizer(lambda: yp_dns_for_axfr.stop())

    return yp_instance, yp_dns, yp_instance_for_axfr_server, yp_dns_for_axfr


def test_teardown(yp_instance):
    reset_yp(yp_instance.create_client())


@pytest.fixture(scope="function")
def ctl_env(request, session_ctl_env):
    yp_instance, yp_dns, yp_instance_for_axfr_server, yp_dns_for_axfr = session_ctl_env
    request.addfinalizer(lambda: test_teardown(yp_instance))
    request.addfinalizer(lambda: test_teardown(yp_instance_for_axfr_server))
    return yp_instance, yp_dns, AxfrServer(yp_instance_for_axfr_server, yp_dns_for_axfr)

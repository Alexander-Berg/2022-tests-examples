import os
import pytest
import yatest

import dns.name
import dns.rdataclass
import dns.rdatatype
import dns.rdtypes.ANY.PTR
import dns.rdtypes.IN.AAAA
import dns.set

from conftest import ZONE1, REVERSE_ZONE1, AUTH_ZONES, unpack

from infra.yp_yandex_dns_export.controller import StandaloneController


CREATED_BY_YP_DNS_EXPORT = "yp_dns_export"

DOMAIN1 = "domain1.{}".format(ZONE1)
DOMAIN2 = "domain2.{}".format(ZONE1)
REVERSE_DOMAIN1 = "6.9.6.0.0.0.0.0.a.a.5.e.0.0.c.f.{}".format(REVERSE_ZONE1)

RECORDS = {
    DOMAIN1: [
        dns.rdtypes.IN.AAAA.AAAA(
            rdclass=dns.rdataclass.from_text("IN"),
            rdtype=dns.rdatatype.from_text("AAAA"),
            address=address
        ) for address in [
            "2a02:6b8:c08:9b94:0:43db:a51e:0",
            "2a02:6b8:c08:58a5:0:44a0:8c45:0",
        ]
    ],
    DOMAIN2: [
        dns.rdtypes.IN.AAAA.AAAA(
            rdclass=dns.rdataclass.from_text("IN"),
            rdtype=dns.rdatatype.from_text("AAAA"),
            address=address
        ) for address in [
            "2a02:6b8:c08:9b94:0:43db:a51e:1",
            "2a02:6b8:c08:58a5:0:44a0:8c45:1",
        ]
    ],
    REVERSE_DOMAIN1: [
        dns.rdtypes.ANY.PTR.PTR(
            rdclass=dns.rdataclass.from_text("IN"),
            rdtype=dns.rdatatype.from_text("PTR"),
            target=dns.name.Name(domain.split('.'))
        ) for domain in [
            "domain1-1.yandex.net.",
            "domain1-2.yandex.net.",
        ]
    ]
}


def create_controller(yp_instance, axfr_server, zone_config={}):
    controller_dir = yatest.common.output_path("yp_dns_export")
    os.makedirs(controller_dir, exist_ok=True)
    config = {
        "Controller": {
            "YpClient": {
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False,
            },
            "LeadingInvader": {
                "Path": "//home",
                "Proxy": yp_instance.create_yt_client().config["proxy"]["url"],
            },
            "Logger": {
                "Path": os.path.join(controller_dir, "controller.log"),
                "Level": "RESOURCES",
            },
        },
        "YpAddresses": [
            yp_instance.yp_client_grpc_address
        ],
        "ExportConfig": {
            "Sources": [
                {
                    "Name": "source-{}".format(zone),
                    "Retrieve": {
                        "Axfr": {
                            "Address": "::",
                            "Port": axfr_server.port,
                            "Zone": zone,
                        }
                    }
                } for zone in AUTH_ZONES
            ],
            "Zones": [
                {
                    "Name": zone,
                    "Sources": [
                        "source-{}".format(zone),
                    ]
                } | zone_config for zone in AUTH_ZONES
            ],
        },
    }
    return StandaloneController(config)


def equal_items(rrset, *items):
    assert rrset is not None
    if len(items) == 1 and isinstance(items[0], list):
        items = items[0]
    return dns.set.Set(rrset.items) == dns.set.Set(items)


def make_record(record):
    rdclass, rdtype, data = unpack(record)
    return {
        "data": data,
        "class": rdclass,
        "type": rdtype,
    }


@pytest.mark.usefixtures("ctl_env")
def test_core_functionality(ctl_env):
    yp_instance, yp_dns, axfr_server = ctl_env
    controller = create_controller(yp_instance, axfr_server)

    # no records
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # add record
    axfr_server.add_record(DOMAIN1, RECORDS[DOMAIN1][0])

    # still no records
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # sync, now we have record
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])

    # add one more record, but still one record
    axfr_server.add_record(DOMAIN1, RECORDS[DOMAIN1][1])
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])

    # sync, now we have two records
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1])

    # remove one record, but still have it
    axfr_server.remove_record(DOMAIN1, RECORDS[DOMAIN1][0])
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1])

    # sync, one record left
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][1])

    # remove whole record set, but one record still in answer
    axfr_server.remove_record_set(DOMAIN1)
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][1])

    # sync, no records
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None


@pytest.mark.usefixtures("ctl_env")
def test_reverse_records(ctl_env):
    yp_instance, yp_dns, axfr_server = ctl_env
    controller = create_controller(yp_instance, axfr_server)

    # no records
    resp = yp_dns.udp(REVERSE_DOMAIN1, 'PTR', wait_update=True)
    assert yp_dns.get_answer(resp, REVERSE_DOMAIN1, 'PTR') is None

    # add records
    for record in RECORDS[REVERSE_DOMAIN1]:
        axfr_server.add_record(REVERSE_DOMAIN1, record)

    # sync, now we have record
    controller.sync()
    resp = yp_dns.udp(REVERSE_DOMAIN1, 'PTR', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, REVERSE_DOMAIN1, 'PTR'), RECORDS[REVERSE_DOMAIN1])


@pytest.mark.usefixtures("ctl_env")
def test_uncontrolled_records(ctl_env):
    yp_instance, yp_dns, axfr_server = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance, axfr_server)

    # no records
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    yp_client.create_object("dns_record_set", attributes={
        "meta": {"id": DOMAIN1},
        "spec": {"records": [make_record(RECORDS[DOMAIN1][0])]},
    })

    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])

    # controller must not remove record
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])

    # add record, but contoller must not add record
    axfr_server.add_record(DOMAIN1, RECORDS[DOMAIN1][1])
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])

    # clear records, but dns_record_set still exists
    yp_client.update_object("dns_record_set", DOMAIN1, set_updates=[
        {
            "path": "/spec/records",
            "value": []
        }
    ])
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # now remove dns_record_set
    yp_client.remove_object("dns_record_set", DOMAIN1)
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # now sync and new second record must appear
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][1])


@pytest.mark.usefixtures("ctl_env")
def test_fault_tolerance(ctl_env):
    yp_instance, yp_dns, axfr_server = ctl_env
    controller = create_controller(yp_instance, axfr_server)

    # no records
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # add record, but not synchronized
    axfr_server.add_record(DOMAIN1, RECORDS[DOMAIN1][0])
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    axfr_server.suspend()

    # sync must fail
    assert not controller.safe_sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # restart AXFR server
    axfr_server.resume()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert yp_dns.get_answer(resp, DOMAIN1, 'AAAA') is None

    # successful sync
    controller.sync()
    resp = yp_dns.udp(DOMAIN1, 'AAAA', wait_update=True)
    assert equal_items(yp_dns.get_answer(resp, DOMAIN1, 'AAAA'), RECORDS[DOMAIN1][0])


@pytest.mark.usefixtures("ctl_env")
def test_set_sources_label(ctl_env):
    yp_instance, yp_dns, axfr_server = ctl_env
    yp_client = yp_instance.create_client()
    controller = create_controller(yp_instance, axfr_server, zone_config={'SetSourcesLabel': True})

    yp_client.create_object("dns_record_set", attributes={
        "meta": {"id": DOMAIN1},
        "spec": {"records": [make_record(RECORDS[DOMAIN1][0])]},
        "labels": {"created_by": CREATED_BY_YP_DNS_EXPORT},
    })

    axfr_server.add_record(DOMAIN1, RECORDS[DOMAIN1][0])
    axfr_server.add_record(DOMAIN2, RECORDS[DOMAIN2][0])

    # set /labels/sources for DOMAIN1 (exists) and DOMAIN2 (creating)
    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set", selectors=["/meta/id", "/labels/sources"])
    assert len(record_sets) == 2
    for id, sources in record_sets:
        assert sources == [
            "source-{}".format(ZONE1),
        ]

    axfr_server.remove_record_set(DOMAIN2)

    # disabling SetSourcesLabel must not delete label
    controller = create_controller(yp_instance, axfr_server, zone_config={'SetSourcesLabel': False})
    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set", selectors=["/meta/id", "/labels/sources"])
    assert len(record_sets) == 1
    for id, sources in record_sets:
        assert sources == [
            "source-{}".format(ZONE1),
        ]

    # create record set, but do not set label
    axfr_server.add_record(DOMAIN2, RECORDS[DOMAIN2][0])
    controller.sync()

    record_sets = yp_client.select_objects("dns_record_set", selectors=["/meta/id", "/labels/sources"], limit=3)
    assert record_sets == [
        [DOMAIN1, ["source-{}".format(ZONE1)]],
        [DOMAIN2, None],
    ]

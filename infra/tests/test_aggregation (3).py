# coding: utf-8
from __future__ import print_function

import time
import json
import itertools

import requests
import pytest


@pytest.mark.xfail
def test_netmon(netmon_aggregator_url):
    hosts = [
        "jmon-test.search.yandex.net",
        "jmon-unstable.search.yandex.net"
    ]
    fb_hosts = [
        "fb-jmon-test.search.yandex.net",
        "fb-jmon-unstable.search.yandex.net"
    ]

    scores = [(i, 10 - i) for i in xrange(11)]
    rtt = [i * 10.0 for i in xrange(1, 11)]

    for i in xrange(1000):
        hosts.extend((
            "sas1-{}.search.yandex.net".format(i + 1000),
            "man1-{}.search.yandex.net".format(i + 1000)
        ))

        fb_hosts.extend((
            "fb-sas1-{}.search.yandex.net".format(i + 1000),
            "fb-man1-{}.search.yandex.net".format(i + 1000)
        ))

    ping_url = "{}/api/v1/ping".format(netmon_aggregator_url)
    send_url = "{}/api/client/v1/send_reports".format(netmon_aggregator_url)
    create_expression_url = "{}/api/expression/v1/upsert".format(netmon_aggregator_url)
    fetch_expression_url = "{}/api/expression/v1/get".format(netmon_aggregator_url)

    expression_name = "test"
    requests.post(create_expression_url, data=json.dumps({"expression": "group=G@ALL_RUNTIME", "id": expression_name})).raise_for_status()
    is_aggregated = lambda: requests.post(fetch_expression_url, data=json.dumps({"id": expression_name})).json()["expression"]["aggregated"]

    probe_list = []
    tcs = [0, 96, 128]
    fb_tcs = [0, 32, 64]

    host_iter = itertools.cycle(iter(hosts))
    fb_host_iter = itertools.cycle(iter(fb_hosts))

    scores_iter = itertools.cycle(iter(scores))
    rtt_iter = itertools.cycle(iter(rtt))

    deadline = time.time() + 600
    for i in itertools.count():
        received, lost = scores_iter.next()

        src = host_iter.next()
        tgt = host_iter.next()

        probe_list.append({
            "Source": src,
            "Target": tgt,
            "Family": 30,
            "Protocol": 1,
            "Received": received,
            "Lost": lost,
            "RoundTripTimeAverage": int(rtt_iter.next() * 1000000),
            "Generated": int(time.time() * 1000000),
            "TypeOfService": 0
        })

        for tc in tcs:
            probe_list.append({
                "Source": src,
                "Target": tgt,
                "Family": 30,
                "Protocol": 1,
                "Received": 10,
                "Lost": 0,
                "RoundTripTimeAverage": int(rtt_iter.next() * 1000000),
                "Generated": int(time.time() * 1000000),
                "TypeOfService": tc
            })

        src = fb_host_iter.next()
        tgt = fb_host_iter.next()

        for tc in fb_tcs:
            probe_list.append({
                "Source": src,
                "Target": tgt,
                "Family": 30,
                "Protocol": 1,
                "Received": 10,
                "Lost": 0,
                "RoundTripTimeAverage": int(rtt_iter.next() * 1000000),
                "Generated": int(time.time() * 1000000),
                "TypeOfService": tc
            })

        if i % 10000 == 0:
            requests.post(send_url, data=json.dumps({"Reports": probe_list})).raise_for_status()
            probe_list = []

            if time.time() > deadline:
                break

            if requests.get(ping_url).status_code == 200:
                if is_aggregated():
                    break

    requests.get(ping_url).raise_for_status()

    assert is_aggregated()

    views = requests.post(fetch_expression_url, data=json.dumps({"id": expression_name})).json()["expression"]["views"]

    network_types = {v["network"] for v in views}

    assert "fb6" in network_types
    assert "bb6" in network_types
    assert "fb-cs1" in network_types
    assert "fb-cs2" in network_types
    assert "bb-cs3" in network_types
    assert "bb-cs4" in network_types

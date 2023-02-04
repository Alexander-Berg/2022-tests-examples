# coding: utf-8
import pytest
import mock

from infra.swatlib.auth import gencfg
from infra.awacs.proto import internals_pb2
from awacs import resolver
from awacs.lib import nannyclient


@pytest.mark.vcr
def test_nanny_resolver(mongo_storage):
    nanny_client = nannyclient.NannyClient(url='https://nanny.yandex-team.ru/v2')
    gencfg_client = resolver.GencfgClient(gencfg.GencfgClient(url='http://api.gencfg.yandex-team.ru/'))
    c = resolver.NannyClient(
        nanny_client=nanny_client,
        gencfg_client=gencfg_client,
        yp_client_factory=mock.Mock()
    )

    instances = c.list_nanny_snapshot_instances(service_id='production_awacs',
                                                snapshot_id='a1a845bee08b275b94894e1aa442aa33777b3529',
                                                use_mtn=False)
    expected_instances = [
        internals_pb2.Instance(
            host='vla1-3634.search.yandex.net',
            port=23830,
            ipv6_addr='2a02:6b8:c0e:23:0:604:db7:a07e',
            weight=160
        ),
        internals_pb2.Instance(
            host='sas1-6027.search.yandex.net',
            port=23830,
            ipv6_addr='2a02:6b8:b000:164:428d:5cff:fe34:f932',
            weight=160
        ),
        internals_pb2.Instance(
            host='vla1-5979.search.yandex.net',
            port=23830,
            ipv6_addr='2a02:6b8:c0e:12d:0:604:5e18:da6',
            weight=160
        ),
        internals_pb2.Instance(
            host='sas1-6201.search.yandex.net',
            port=23830,
            ipv6_addr='2a02:6b8:b000:11b:215:b2ff:fea7:7908',
            weight=160
        ),
        internals_pb2.Instance(
            host='man1-0139.search.yandex.net',
            port=23830,
            ipv6_addr='2a02:6b8:b000:601c:f652:14ff:fe8c:2a80',
            weight=160
        ),
    ]
    assert sorted(instances, key=lambda pb: pb.host) == sorted(expected_instances, key=lambda pb: pb.host)

    cache = resolver.NannyInstancesCache(mem_maxsize=100)
    c = resolver.NannyClient(
        nanny_client=nanny_client,
        gencfg_client=gencfg_client,
        yp_client_factory=mock.Mock(),
        cache=cache
    )

    instances = c.list_nanny_snapshot_instances(service_id='production_awacs',
                                                snapshot_id='a1a845bee08b275b94894e1aa442aa33777b3529',
                                                use_mtn=False)
    assert sorted(instances, key=lambda pb: pb.host) == sorted(expected_instances, key=lambda pb: pb.host)

    cached_instances = cache.get_nanny_instances(service_id='production_awacs',
                                                 snapshot_id='a1a845bee08b275b94894e1aa442aa33777b3529',
                                                 use_mtn=False)
    assert sorted(cached_instances, key=lambda pb: pb.host) == sorted(expected_instances, key=lambda pb: pb.host)

    cache.mem_cache.clear()
    cached_instances = cache.get_nanny_instances(service_id='production_awacs',
                                                 snapshot_id='a1a845bee08b275b94894e1aa442aa33777b3529',
                                                 use_mtn=False)
    assert sorted(cached_instances, key=lambda pb: pb.host) == sorted(expected_instances, key=lambda pb: pb.host)

import pytest
import json
import grpc
import time
import os
import sys

import grut.libs.object_api.proto.object_api_service_pb2 as object_api_service_pb2
import grut.libs.object_api.proto.object_api_service_pb2_grpc as object_api_service_pb2_grpc

from ads.bsyeti.big_rt.py_test_lib import make_namedtuple
from ads.bsyeti.caesar.tests.lib.b2b import helpers, schema, utils
from ads.bsyeti.caesar.tests.ft.common import get_yt_cluster_from_docker


@pytest.fixture()
def stand(request, config_test_default_enabled, port_manager):

    file = open('../configs/ports_config.json', 'r')
    configs_json = json.load(file)

    os.environ["YT_PROXY"] = 'localhost:' + str(configs_json['yt_http_proxy'])
    os.environ["YT_PROXY_PRIMARY"] = os.environ["YT_PROXY"]
    os.environ["OBJECT_API_ADDRESS_0"] = 'localhost:' + str(configs_json['object_api_port'])
    os.environ["GRUT_ADDRESS"] = os.environ["OBJECT_API_ADDRESS_0"]

    # use generate_timestamp handler to understand if grut is up

    STOP = False
    response = object_api_service_pb2.TRspGenerateTimestamp()

    cnt = 0

    while STOP is False:
        try:
            with grpc.insecure_channel(os.environ['OBJECT_API_ADDRESS_0']) as channel:
                stub = object_api_service_pb2_grpc.ObjectApiServiceStub(channel)
                response = stub.GenerateTimestamp(object_api_service_pb2.TReqGenerateTimestamp())
                if response.timestamp != 0:
                    STOP = True
        except:
            if cnt >= 180:
                sys.stderr.write("Can't connect to grut\n")
                assert 'connection failed' == ''
            cnt += 5
            time.sleep(5)

    shards_count = 1
    yt_cluster = get_yt_cluster_from_docker()
    yt_client = yt_cluster.primary_cluster.get_yt_client()
    yt_client.set("//sys/accounts/tmp/@resource_limits/tablet_count", 100500)
    queue_names = []
    queues = {}
    caesar_workers = []

    for dest in utils.get_destinations():
        if "Writer" in dest:
            name = utils.get_profile_type(dest["DestinationName"])
            worker = utils.make_caesar_worker(name, dest)
            caesar_workers.append(worker)
            queue_names.append(worker.input_queue)

    helpers.create_profile_tables(yt_cluster, caesar_workers)

    queues = {}
    with utils.threadpool() as pool:
        for qname in queue_names:
            queues[qname] = pool.submit(utils.make_queue, yt_client, qname, shards_count)

    for qname in queue_names:
        queues[qname] = queues[qname].result()

    for path, attributes in schema.TABLES.items():
        yt_client.create("table", path, attributes=attributes)
        yt_client.mount_table(path)

    return make_namedtuple(
        "Stand",
        yt_cluster=yt_cluster,
        yt_client=yt_client,
        port_manager=port_manager,
        source2queue=None,
        queues=queues,
        caesar_workers=caesar_workers,
        now=utils.get_worker_dt(),
        resharder_port=None,
        caesar_port=port_manager.get_port(),
    )

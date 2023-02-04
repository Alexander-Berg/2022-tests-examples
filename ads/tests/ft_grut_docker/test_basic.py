import json
import pytest
import sys
import os

from ads.bsyeti.caesar.tests.ft.common import import_all_cases
from ads.bsyeti.caesar.tests.ft.common import run_test_case

import grpc
import time

import grut.libs.object_api.proto.object_api_service_pb2 as object_api_service_pb2
import grut.libs.object_api.proto.object_api_service_pb2_grpc as object_api_service_pb2_grpc


@pytest.mark.parametrize("case_class", import_all_cases("ads/bsyeti/caesar/tests/ft_grut/cases"))
def test_basic(request, case_class, port_manager, config_test_default_enabled):
    file = open('../configs/ports_config.json', 'r')
    configs_json = json.load(file)

    os.environ["YT_PROXY"] = 'localhost:' + str(configs_json['yt_http_proxy'])
    os.environ["OBJECT_API_ADDRESS_0"] = 'localhost:' + str(configs_json['object_api_port'])

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

    # grut is up, ready to test
    run_test_case(request, case_class, port_manager, from_docker=True)

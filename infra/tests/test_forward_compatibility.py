import logging
import pytest
from yp.client import YpClient
from yp.client import find_token
from yp_proto.yp.client.api.proto import object_service_pb2
import yp.data_model as data_model

import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-full-cycle-stage-' + suffix_fixture


@pytest.mark.skip(reason="skipped due to DEPLOY-3654")
def test_forward_compatibility(stage_name, stage_fixture, yp_xdc_client_fixture, yp_xdc_cluster):
    logging.info('----forward_compatibility----')
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)

    client = YpClient(
        transport='grpc',
        address=yp_xdc_cluster,
        config={
            'token': find_token(),
            'grpc_channel_options': {
                'max_receive_message_length': 8000000
            }
        },
    )
    stub = client.create_grpc_object_stub()
    req = object_service_pb2.TReqGetObject()
    req.object_id = stage_name
    req.object_type = data_model.OT_STAGE
    req.format = object_service_pb2.EPayloadFormat.Value("PF_PROTOBUF")
    req.selector.paths.append("/status")
    stub.GetObject(req)

    logging.info('----forward_compatibility end----')

# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import AntirobotWrapper
from awacs.wrappers.errors import ValidationError


def test_antirobot_wrapper():
    pb = modules_pb2.AntirobotWrapperModule()
    antirobot_wrapper = AntirobotWrapper(pb)

    pb.cut_request_bytes = -1
    antirobot_wrapper.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        antirobot_wrapper.validate()
    e.match('cut_request_bytes: must be non-negative')

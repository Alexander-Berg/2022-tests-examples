# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Shared
from awacs.wrappers.errors import ValidationError


def test_exp_getter():
    pb = modules_pb2.SharedModule()

    shared = Shared(pb)

    with pytest.raises(ValidationError) as e:
        shared.validate()
    e.match('uuid.*is required')

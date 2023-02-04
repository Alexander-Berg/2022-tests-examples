import os

import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment
from ya_courier_backend.util.tvm import Tvm

TVM_ENABLED = os.environ.get("YA_COURIER_TVM_ENABLED", "YES")


@skip_if_remote
@pytest.mark.skipif(not TVM_ENABLED, reason='Tvm disabled')
def test_valid_tvm_ids_in_config(env: Environment):
    for alias in Tvm.destination:
        Tvm.get_tvm_headers(alias)

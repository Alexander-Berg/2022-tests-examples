# -*- coding: utf-8 -*-

import time


def test_no_segfault_wo_tvm(test_environment):
    now = int(time.time())
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": now - 10, "uint_values": [15]}]
            },
        }
    )
    response = test_environment.raw_request(
        client="yabs", ids={"bigb-uid": id1}, test_time=now, tvm_service_ticket="secret"
    )
    # because there is no tvm, just check that eagle does not segfault
    # In new runtime we return 400 in case of misssed TVM
    assert 400 == response.status_code

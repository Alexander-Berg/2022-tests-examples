import yatest.common
import test_with_local_yp

import infra.libs.yp_replica.test.scenario.scenario as scenario


def test():
    test_with_local_yp.run_test(yatest.common.output_path(), scenario)

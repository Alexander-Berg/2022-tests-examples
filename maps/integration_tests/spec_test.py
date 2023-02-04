import json
import os

import yatest.common


def test_example_app_full_spec():
    return json.loads(yatest.common.execute(
        command=[yatest.common.binary_path('maps/infra/pycare/example/bin/example')],
        env={'PYCR_GENCONFIG': '1', **os.environ},
    ).std_out)

import os
import tarfile

import yatest

from ads.bsyeti.big_rt.py_test_lib import make_json_file
from ads.bsyeti.caesar.tests.lib.b2b import utils
from ads.bsyeti.caesar.tests.lib.b2b.b2b_scenario import get_profiles

DIFF_TOOL = "ads/bsyeti/caesar/tests/lib/b2b/compare/b2b-compare.tar"
DIFF_TOOL_TIMEOUT = 120


def test_b2b(stand):
    trim_data = False
    profiles = get_profiles(stand, problems_interval_seconds=None, problems_max_count=None, trim_data=trim_data)
    tarfile.TarFile(yatest.common.binary_path(DIFF_TOOL)).extractall()
    diff_tool = [
        yatest.common.java_bin(),
        "-cp",
        os.path.join(os.getcwd(), "*"),
        "bsyeti.json.compare.B2BCompare",
    ]
    with utils.measure_time("making canonical file"):
        return {
            k: yatest.common.canonical_file(
                make_json_file(v, name_template="{}.json".format(k)),
                diff_tool=diff_tool,
                diff_tool_timeout=DIFF_TOOL_TIMEOUT,
            )
            for k, v in profiles.items()
        }

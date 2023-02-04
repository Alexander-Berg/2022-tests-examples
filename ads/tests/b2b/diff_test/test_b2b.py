import ujson as json

import yatest
from library.python.sanitizers import asan_is_on

from ads.bsyeti.caesar.tests.lib.b2b.b2b_scenario import get_profiles


def test_b2b(stand):
    profiles = get_profiles(stand, problems_interval_seconds=None, problems_max_count=None, trim_data=asan_is_on())

    with open(yatest.common.output_path("profiles.json"), "w") as f:
        f.write(json.dumps(profiles, indent=4, sort_keys=True))

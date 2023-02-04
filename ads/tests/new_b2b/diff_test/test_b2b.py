import ujson as json

import yatest
from library.python.sanitizers import asan_is_on

from ads.bsyeti.caesar.tests.lib.b2b.b2b_scenario import get_profiles


def test_b2b(stand):
    # add --test-param upload_to_yt=True --test-param yt_profiles_path='//sample/path'
    upload_to_yt = yatest.common.get_param("upload_to_yt")
    profiles = get_profiles(stand, problems_interval_seconds=None, problems_max_count=None, trim_data=asan_is_on(), resharder_enabled=False, upload_to_yt=upload_to_yt)

    with open(yatest.common.output_path("profiles.json"), "w") as f:
        f.write(json.dumps(profiles, indent=4, sort_keys=True))

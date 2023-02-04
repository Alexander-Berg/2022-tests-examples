import logging

from ads.bsyeti.big_rt.py_test_lib import make_json_file
from ads.bsyeti.caesar.tests.lib.b2b import utils
from ads.bsyeti.caesar.tests.lib.b2b.b2b_scenario import get_profiles
from ads.bsyeti.caesar.tests.lib.b2b.helpers import (
    create_profile_tables,
    drop_profile_tables,
    unfreeze_queues,
)

INTERVAL = 15
PROBLEMS_MAX_COUNT = 9999


def do_test(stand, is_stable):
    label = "stable" if is_stable else "unstable"
    interval = None if is_stable else INTERVAL
    problems_max_count = None if is_stable else PROBLEMS_MAX_COUNT
    trim_data = True

    with utils.measure_time(f"getting profiles {label}"):
        profiles = get_profiles(
            stand,
            problems_interval_seconds=interval,
            problems_max_count=problems_max_count,
            trim_data=trim_data,
        )
    with utils.measure_time(f"saving data to disk {label}"):
        make_json_file(profiles, name_template=f"{label}.json")

    return {"data": profiles, "lens": {k: len(v) for k, v in profiles.items()}}


def test_b2b(stand):
    unstable = do_test(stand, is_stable=False)
    logging.info(unstable["lens"])

    with utils.measure_time("unfreezing"):
        unfreeze_queues(stand, stand.caesar_workers)

    drop_profile_tables(stand.yt_client, stand.caesar_workers)
    create_profile_tables(stand.yt_cluster, stand.caesar_workers)

    stable = do_test(stand, is_stable=True)

    assert stable["lens"] == unstable["lens"]

    with utils.measure_time("asserting equality"):
        assert stable["data"] == unstable["data"]

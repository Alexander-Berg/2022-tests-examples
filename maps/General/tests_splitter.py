from maps.garden.sdk.core import Task
from maps.garden.sdk.yt.utils import get_yt_client_from_environment_settings

import yt.wrapper as yt

import logging
import json

YT_SERVER = "hahn"


def read_toponyms_yt(yt_client, geosrc_path):
    toponyms = yt_client.read_table(geosrc_path + '/toponyms', format=yt.format.JsonFormat())

    toponym_to_geoid = dict()
    for t in toponyms:
        if t["is_geo_id_owner"]:
            toponym_to_geoid[str(t["toponym_id"])] = str(t["geo_id"])
    return toponym_to_geoid


def read_hierarchy_yt(yt_client, geosrc_path, toponym_to_geoid, pov, cache_geoids):
    hierarchy = yt_client.read_table(geosrc_path + f"/pov/{pov}/hierarchy", format=yt.format.JsonFormat())

    geoid_branch = {}
    for h in hierarchy:
        child_id = str(h["toponym_id"])
        if child_id in toponym_to_geoid:  # == is geoid owner
            for ancestor_id in h["ancestor_ids"]:
                ancestor_id = str(ancestor_id)
                if ancestor_id in toponym_to_geoid and toponym_to_geoid[ancestor_id] in cache_geoids:  # geoid owner and have cache for this region
                    geoid_branch.setdefault(toponym_to_geoid[child_id], set()).add(toponym_to_geoid[ancestor_id])

    result = {}
    for geoid in geoid_branch:
        result[geoid] = list(item for item in geoid_branch[geoid])
        result[geoid].sort()
    return result


class BuildGeoidTreesTask(Task):
    '''
    Prepare geoids dependencies for each pov filtered by cache geoids of caches to be tested.
    '''
    def __init__(self):
        super().__init__()

    def __call__(self, geosrc, geoid_trees, **cache_file_resources):
        cache_geoids = set()
        povs = set()
        for name, cache_file_resource in cache_file_resources.items():
            lang, pov, geoid = name.split("_")[4:7]  # search_2_cache_file_{locale}_{geoid}
            cache_geoids.add(geoid)
            povs.add(pov)

        yt_client = get_yt_client_from_environment_settings(self._env, YT_SERVER)

        logging.info(f"Reading toponyms from {geosrc.path}")
        toponym_to_geoid = read_toponyms_yt(yt_client, geosrc.path)
        logging.info("Done.")

        logging.info(f"Reading hierarcheis from {geosrc.path}")
        geoids = dict(
            (pov, read_hierarchy_yt(yt_client, geosrc.path, toponym_to_geoid, pov, cache_geoids))
            for pov in povs)  # geoid -> [geoid]

        with open(geoid_trees.path(), "w") as out:
            json.dump(geoids, out)

        logging.info("Done.")

    def load_environment_settings(self, environment_settings):
        self._env = environment_settings


def split_testset_by_geoid_and_locale(testset, geoid_trees):
    single_cache_tests = dict()

    for test in testset["tests"]:
        test_geoid = str(test["expected"]["geoid"])
        locale = test["request"]["lang"]
        lang, pov = locale.split("_")

        if pov in geoid_trees:
            for geoid in geoid_trees[pov].get(test_geoid, []):
                single_cache_tests.setdefault(geoid, dict()).setdefault(locale, list()).append(test)

    return single_cache_tests

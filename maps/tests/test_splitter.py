import pytest
import yatest.common
import json

import yt.wrapper as yt

from maps.garden.sdk.test_utils import data

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FileResource
from maps.garden.sdk.yt import YtDirectoryResource

from maps.garden.modules.offline_search_cache_validator.lib import tests_splitter
from maps.garden.modules.offline_search_cache_validator.lib import offline_search_cache_validator
from maps.garden.modules.offline_search_cache_validator.lib.resource_names import GEOID_TREES_BY_POVS

VERSION = "09.02.22-0"


def _create_tables(yt_client, geosrc_dir_resource, data_path):

    def _create_table(path, schema):
        yt_table_path = yt.ypath_join(geosrc_dir_resource.path, path)
        yt_client.create("table", yt_table_path, attributes={"schema": schema}, recursive=True)
        yt_client.write_table(yt_table_path, data.read_table_from_file(data_path + "/input", path))

    toponyms_schema = data.read_table_from_file(data_path + "/schema", "toponyms")
    hierarchy_schema = data.read_table_from_file(data_path + "/schema", "hierarchy")

    _create_table("toponyms", toponyms_schema)
    _create_table("pov/RU/hierarchy", hierarchy_schema)
    _create_table("pov/UA/hierarchy", hierarchy_schema)


def _create_geosrc_dir_resource(environment_settings):
    GEOCODER_DATA_PATH = yatest.common.test_source_path("data")
    geosrc_dir_resource = YtDirectoryResource(
        name=offline_search_cache_validator.GEOSRC_YT,
        filename_template="//tmp/geocoder_export",
        server="hahn")
    geosrc_dir_resource.version = Version(properties={"release": VERSION})
    geosrc_dir_resource.load_environment_settings(environment_settings)
    yt_client = geosrc_dir_resource.get_yt_client()
    try:
        _create_tables(yt_client, geosrc_dir_resource, GEOCODER_DATA_PATH)
    except BaseException:  # yt.common.YtResponseError():
        pass
    return geosrc_dir_resource


@pytest.mark.use_local_yt("hahn")
def test_load_toponyms_hierarchy(environment_settings):
    geosrc_dir_resource = _create_geosrc_dir_resource(environment_settings)
    yt_client = geosrc_dir_resource.get_yt_client()

    # YT data dependent tests. See input/pov/*/hierarchy
    toponym_to_geoid = tests_splitter.read_toponyms_yt(yt_client, geosrc_dir_resource.path)
    assert toponym_to_geoid == {"1": "1", "3": "10", "4": "12"}
    assert len(toponym_to_geoid) == 3
    assert toponym_to_geoid["1"] == "1"
    assert toponym_to_geoid["3"] == "10"
    assert toponym_to_geoid["4"] == "12"

    cache_geoids = ["1", "10"]  # Imagine we have only these two caches

    ru_hierarchy = tests_splitter.read_hierarchy_yt(yt_client, geosrc_dir_resource.path, toponym_to_geoid, "RU", cache_geoids)
    assert set(ru_hierarchy["1"]) == set("1")
    assert set(ru_hierarchy["10"]) == set(["1", "10"])
    assert set(ru_hierarchy["12"]) == set(["1", "10"])

    all_values = set()
    for geoids in ru_hierarchy.values():
        for geoid in geoids:
            all_values.add(geoid)
    assert all_values == set(cache_geoids)  # All caches present and No extra caches left

    ua_hierarchy = tests_splitter.read_hierarchy_yt(yt_client, geosrc_dir_resource.path, toponym_to_geoid, "UA", cache_geoids)
    assert set(ua_hierarchy["1"]) == set("1")
    assert set(ua_hierarchy["10"]) == set(["10"])
    assert set(ua_hierarchy["12"]) == set(["10"])

    all_values = set()
    for geoids in ua_hierarchy.values():
        for geoid in geoids:
            all_values.add(geoid)
    assert all_values == set(cache_geoids)


def test_split_testset():
    testset = {"tests": [
        {"expected": {"geoid": 12}, "request": {"lang": "ru_RU", "geocode": "test1"}},  # Should be tested on geoid 12 and all its RU ancestors (1, 10)
        {"expected": {"geoid": 12}, "request": {"lang": "uk_UA", "geocode": "test2"}},  # Should be tested on geoid 12 and all its UA ancestors (10)
        {"expected": {"geoid": 10}, "request": {"lang": "ru_RU", "geocode": "test3"}},
        {"expected": {"geoid": 10}, "request": {"lang": "uk_UA", "geocode": "test4"}},
        {"expected": {"geoid": 1}, "request": {"lang": "ru_RU", "geocode": "test5"}},
        {"expected": {"geoid": 1}, "request": {"lang": "uk_UA", "geocode": "test6"}},
        {"expected": {"geoid": 1}, "request": {"lang": "tr_TR", "geocode": "test7"}},  # No cache for TR to be tested on
    ]}

    geoid_trees = {  # geoid -> [ancestors and self]
        "RU": {"1": ["1"], "10": ["1", "10"], "12": ["1", "10", "12"]},
        "UA": {"1": ["1"], "10": ["10"], "12": ["10", "12"]},
    }

    split_tests = tests_splitter.split_testset_by_geoid_and_locale(testset, geoid_trees)

    def expect(split, expected):
        split_tests = set()
        for test in split:
            split_tests.add(test["request"]["geocode"])

        assert split_tests == set(expected)

    expect(split_tests["1"]["ru_RU"], ["test1", "test3", "test5"])  # All ru_RU tests related to geoid 1 or any of its RU children (10, 12)
    expect(split_tests["10"]["ru_RU"], ["test1", "test3"])
    expect(split_tests["12"]["ru_RU"], ["test1"])
    expect(split_tests["1"]["uk_UA"], ["test6"])
    expect(split_tests["10"]["uk_UA"], ["test2", "test4"])
    expect(split_tests["12"]["uk_UA"], ["test2"])


@pytest.mark.use_local_yt("hahn")
def test_BuildGeoidTreesTask(environment_settings):  # All the tests as above combined to a Task
    path, content = ("fake_cache_file", b"fake cache data")
    name = "fake_cache"
    cache_resource = FileResource(name, path)
    cache_resource.version = Version(properties={"release": VERSION})
    cache_resource.load_environment_settings(environment_settings)

    with cache_resource.open("wb") as file:
        file.write(content)

    cache_resource.logged_commit()
    assert cache_resource.physically_exists

    caches = {
        "search_2_cache_file_ru_RU_1_single_file": cache_resource,
        "search_2_cache_file_ru_RU_10_single_file": cache_resource,
        "search_2_cache_file_uk_UA_1_single_file": cache_resource,
        "search_2_cache_file_uk_UA_10_single_file": cache_resource
    }

    # Create output resource
    geoid_trees_resource = FileResource(GEOID_TREES_BY_POVS, "geoid_trees.json")
    geoid_trees_resource.version = Version(properties={"release": VERSION})
    geoid_trees_resource.load_environment_settings(environment_settings)

    geosrc_dir_resource = _create_geosrc_dir_resource(environment_settings)

    # Run task
    task = tests_splitter.BuildGeoidTreesTask()
    task.load_environment_settings(environment_settings)
    task(geoid_trees=geoid_trees_resource, geosrc=geosrc_dir_resource, **caches)

    with open(geoid_trees_resource.path()) as res:
        trees = json.load(res)

    assert trees == {
        "RU": {"1": ["1"], "10": ["1", "10"], "12": ["1", "10"]},
        "UA": {"1": ["1"], "10": ["10"], "12": ["10"]},
    }

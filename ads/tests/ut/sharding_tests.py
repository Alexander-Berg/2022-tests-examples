import os
import pytest
import tempfile

import yatest.common

from ads.targeting.lm_dumps_generator.lib.sharded_binary_dump_generator import ShardedBinaryDumpsGenerator
from ads.targeting.make_lm_sharding_base.lib.sharder import dump_pi

from ads.emily.storage.transport.indexer.converters.lm.lib import ModelValidatorError


VWDUMP = yatest.common.binary_path("ads/libs/vw_lib/vwdump/vwdump")
RV_NAMESPACES = {"CTR", "Relevance", "SQValue1", "SQValue2", "SQValue3", "SQValue4"}


SHARDING_MAP_FOR_COMPLEX_FEATURES_WITH_RV = {
    "BannerID": {
        252391316: 1,
        252391310: 2,
        302640480: 4,
        338725838: 3
    }
}


SHARDING_MAP_WITH_NOT_INTERCEPTING_FEATURES = {
    "UserGroupID": {
        36: 1,
        47: 2,
        33: 3
    },
    "BannerID": {
        200826279: 5,
        57385345: 6,
        43206200: 7
    }
}


SHARDING_MAP = {
    "BMCategoryID": {
        252391316: 3,
        252391310: 2,
        302640480: 1
    }
}


def get_base_conf(linear_model_path):
    return {
        "linear_model_id": 1,
        "dump_type": "vw",
        "task_id": "lm_dump",
        "linear_model_path": linear_model_path,
        "sharding_bases_map": {},
        "destination_path": ".",
        "location": "stat",
        "total_shards": 1,
        "stat_model_shard_file_pattern": "lm_dump_%d_shard_%d.bin",
        "meta_model_file_pattern": "lm_dump_%d.bin",
        "truncate_options": {},
        "validation_options": {},
        "correction_options": {},
    }


def get_base_validation_options():
    return {
        "BMCategoryID": {
            "min_num_features": 2
        },
        "BannerID": {
            "min_num_features": 3
        },
        "BannerID,CTR": {
            "min_num_features": 3
        },
        "BannerID,CTR,CTR,Relevance": {
            "min_num_features": 3
        },
        "DeviceType,BannerID,SQValue4": {
            "min_num_features": 5
        },
        "UserGroupID,BannerID": {
            "min_num_features": 3
        },
        "UserGroupID,BannerID,BannerCategoryID": {
            "min_num_features": 3
        },
        "UserGroupID,BannerID,BannerCategoryID,Relevance": {
            "min_num_features": 3
        },
        "UserGroupID,BannerID,BannerCategoryID,Relevance,Relevance,CTR": {
            "min_num_features": 3
        },
        "CTR,DeviceType,CTR,BannerGroupID,BannerCategoryID": {
            "min_num_features": 4
        },
        "DeviceType,BannerCategoryID,CTR": {
            "min_num_features": 4
        },
        "CTR,Relevance": None,
        "CTR": None,
        "SQValue1,SQValue2,SQValue3": None
    }


@pytest.fixture()
def cleandir():
    newpath = tempfile.mkdtemp()
    os.chdir(newpath)


def conf_with_real_sharding(dump, sharding_map):
    conf = get_base_conf(dump)

    conf["sharding_bases_map"] = {}

    for key, value in sharding_map.iteritems():
        _file = tempfile.NamedTemporaryFile(delete=False)
        _file.close()
        dump_pi(value.iteritems(), _file.name)
        conf["sharding_bases_map"][key] = _file.name

    return conf


def conf_default(dump):
    return get_base_conf(dump)


def conf_with_truncate_options(dump):
    conf = get_base_conf(dump)
    conf["truncate_options"] = {
        "default": {
            "keep_only": [
                "CTR", "UserGroupID,BannerID", "UserGroupID,BannerID,BannerCategoryID"
            ],
            "truncate_all": 1,
            "alg": 3,
            "num_features": 10
        }
    }
    return conf


def conf_with_correction_options(dump):
    conf = get_base_conf(dump)
    conf["correction_options"] = {
        "UserGroupID,BannerID": {
            "11,2": 1000.0
        }
    }
    return conf


def conf_with_validation_options(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    return conf


def conf_with_validation_options_min_num_features_exception(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    conf["validation_options"]["BannerID"] = {
        "min_num_features": 10
    }
    return conf


def conf_with_validation_options_learn_min_llp(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    conf["validation_options"]['learn'] = {
        "ll_p": {
            "min": 0.15
        }
    }
    return conf


def conf_with_validation_options_learn_min_llp_exception(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    conf["validation_options"]['learn'] = {
        "ll_p": {
            "min": 0.17
        }
    }
    return conf


def conf_with_validation_options_test_min_llp(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    conf["validation_options"]['test'] = {
        "ll_p": {
            "min": 0.15
        }
    }
    return conf


def conf_with_validation_options_test_min_llp_exception(dump):
    conf = get_base_conf(dump)
    conf["validation_options"] = get_base_validation_options()
    conf["validation_options"]['test'] = {
        "ll_p": {
            "min": 0.17
        }
    }
    return conf


dumps = [
    yatest.common.source_path("ads/targeting/lm_dumps_generator/tests/ut/fixture/old_style_dump"),
    yatest.common.source_path("ads/targeting/lm_dumps_generator/tests/ut/fixture/new_style_dump")
]

dump_ids = [
    "old_style_dump",
    "new_style_dump"
]


options_resources = [
    conf_default,
    conf_with_truncate_options,
    conf_with_correction_options,
]

options_ids = [
    "that_empty_options_do_not_change_dump",
    "that_truncation_options_change_dump_correctly",
    "that_correction_options_change_dump_correctly",
]


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
@pytest.mark.parametrize("conf", options_resources, ids=options_ids)
def test_dump_manipulations(dump, conf):
    gen = ShardedBinaryDumpsGenerator.load(conf(dump), rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP)
    dump_path = yatest.common.test_output_path('new_dump.txt')
    gen.dumphandler.write_dump(dump_path, binary=False)
    return yatest.common.canonical_file(dump_path)


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options(dump):
    conf = conf_with_validation_options(dump)
    ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options_min_num_features_exception(dump):
    conf = conf_with_validation_options_min_num_features_exception(dump)
    with pytest.raises(ModelValidatorError):
        ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options_learn_llp_min(dump):
    conf = conf_with_validation_options_learn_min_llp(dump)
    ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options_learn_llp_min_exception(dump):
    conf = conf_with_validation_options_learn_min_llp_exception(dump)
    with pytest.raises(ModelValidatorError):
        ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options_test_llp_min(dump):
    conf = conf_with_validation_options_test_min_llp(dump)
    ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_validation_options_test_llp_min_exception(dump):
    conf = conf_with_validation_options_test_min_llp_exception(dump)
    with pytest.raises(ModelValidatorError):
        ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("shards", [1, 18])
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_sharding_on_stat(shards, dump):
    conf = get_base_conf(dump)
    conf["total_shards"] = shards
    ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()
    expected_dumps = [conf["stat_model_shard_file_pattern"] % (conf["linear_model_id"], i)
                      for i in xrange(1, shards + 1)]
    assert sorted(expected_dumps) == sorted(os.listdir(conf["destination_path"]))
    files = [{
        "dump_%s" % i: yatest.common.canonical_file(dump_path)
        for i, dump_path in enumerate(expected_dumps, 1)
    }]

    return files


sharding_maps = [
    SHARDING_MAP,
    SHARDING_MAP_FOR_COMPLEX_FEATURES_WITH_RV,
    SHARDING_MAP_WITH_NOT_INTERCEPTING_FEATURES
]


sharding_maps_ids = [
    "simple_feature",
    "feature_in_namespace_with_rv",
    "not_intercepting_features"
]


@pytest.mark.usefixtures("cleandir")
@pytest.mark.parametrize("shards", [1, 2, 3, 18])
@pytest.mark.parametrize("sharding_map", sharding_maps, ids=sharding_maps_ids)
@pytest.mark.parametrize("dump", dumps, ids=dump_ids)
def test_sharding_on_stat_with_sharding_conf(shards, dump, sharding_map):
    conf = conf_with_real_sharding(dump, sharding_map)
    conf["total_shards"] = shards
    ShardedBinaryDumpsGenerator.load(conf, rv_namespaces=RV_NAMESPACES, vwdump_binary_path=VWDUMP).validate_and_generate()
    expected_dumps = [conf["stat_model_shard_file_pattern"] % (conf["linear_model_id"], i)
                      for i in xrange(1, shards + 1)]
    assert sorted(expected_dumps) == sorted(os.listdir(conf["destination_path"]))
    files = [{
        "dump_%s" % i: yatest.common.canonical_file(dump_path)
        for i, dump_path in enumerate(expected_dumps, 1)
    }]

    return files

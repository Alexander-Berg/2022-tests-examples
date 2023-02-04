import yatest.common
import os
import yt.wrapper as yt
import pytest
import sys
import logos.libs.testy as testy  # noqa
from ads.pytorch.lib.ci_lib import prepare_environment

TO_INSTALL = [
    "ads/pytorch/packages/ads_pytorch",
    "ads/pytorch/packages/deploy",
    "dj/torch/djtorch",
]

DSSM_RUNNER = os.path.abspath(os.path.realpath(yatest.common.binary_path(
    "dj/torch/tools/runner/djtorch_runner"
)))
# left here binary_path not to carry whole pytorch_dssm in data section
SCRIPT_PATH = os.path.join(os.path.abspath(os.path.realpath(yatest.common.source_path(
    "ads/pytorch/ads_pytorch_dj_tests/dj_fixture"))), "multi_trainer.py")

CONFIG_PATH = os.path.join(os.path.abspath(os.path.realpath(yatest.common.source_path(
    "ads/pytorch/ads_pytorch_dj_tests/dj_fixture"))), "config_pytorch_dssm.json")
FACTORS_CONFIG_PATH = os.path.join(os.path.abspath(os.path.realpath(yatest.common.source_path(
    "ads/pytorch/ads_pytorch_dj_tests/dj_fixture"))), "factors_config_pytorch_dssm.json")
FACTORS_PATH = os.path.join(os.path.abspath(os.path.realpath(yatest.common.source_path(
    "ads/pytorch/ads_pytorch_dj_tests/dj_fixture"))), "factors_pytorch_dssm.json")
FEATURES = ["like.length", "own_play$track", "track.length", "track", "own_play.length",
            "like$track", "like$track.length", "own_play$track.length"]
TARGETS = ["target", "weight", "group_id"]

YT_ROOT = "//home/ads/tzar/ci/dj"

TRAIN_ATTRIBUTES = {
    "_tensor_serializer": "JsonTensorSerializer(compression='brotli')",
    "_source_fields_schema": '[{"type": "uint64", "name": "__row_index_4reduce__", "type_v3": {"item": "uint64'
                             '", "type_name": "optional"}}, {"type": "uint64", "name": "__row_index_4group__",'
                             ' "type_v3": {"item": "uint64", "type_name": "optional"}}, {"type": "uint64", "na'
                             'me": "__row_index_4join__", "type_v3": {"item": "uint64", "type_name": "optional'
                             '"}}, {"type": "string", "name": "action_namespace", "type_v3": {"item": "string"'
                             ', "type_name": "optional"}}, {"type": "uint32", "name": "action_type", "type_v3"'
                             ': {"item": "uint32", "type_name": "optional"}}, {"type": "double", "name": "base'
                             'line", "type_v3": {"item": "double", "type_name": "optional"}}, {"type": "string'
                             '", "name": "context_profile", "type_v3": {"item": "string", "type_name": "option'
                             'al"}}, {"type": "string", "name": "dataset", "type_v3": {"item": "string", "type'
                             '_name": "optional"}}, {"type": "string", "name": "features", "type_v3": {"item":'
                             ' "string", "type_name": "optional"}}, {"type": "double", "name": "group_weight",'
                             ' "type_v3": {"item": "double", "type_name": "optional"}}, {"type": "string", "na'
                             'me": "namespace", "type_v3": {"item": "string", "type_name": "optional"}}, {"typ'
                             'e": "string", "name": "payload", "type_v3": {"item": "string", "type_name": "opt'
                             'ional"}}, {"type": "string", "name": "profiles", "type_v3": {"item": "string", "'
                             'type_name": "optional"}}, {"type": "uint64", "name": "random_seed", "type_v3": {'
                             '"item": "uint64", "type_name": "optional"}}, {"type": "string", "name": "recomme'
                             'nder_parameters", "type_v3": {"item": "string", "type_name": "optional"}}, {"typ'
                             'e": "string", "name": "request_id", "type_v3": {"item": "string", "type_name": "'
                             'optional"}}, {"type": "uint32", "name": "request_position", "type_v3": {"item": '
                             '"uint32", "type_name": "optional"}}, {"type": "string", "name": "source", "type_'
                             'v3": {"item": "string", "type_name": "optional"}}, {"type": "uint64", "name": "t'
                             'imestamp", "type_v3": {"item": "uint64", "type_name": "optional"}}, {"type": "st'
                             'ring", "name": "to_id", "type_v3": {"item": "string", "type_name": "optional"}},'
                             ' {"type": "string", "name": "to_namespace", "type_v3": {"item": "string", "type_'
                             'name": "optional"}}, {"type": "uint32", "name": "to_type", "type_v3": {"item": "'
                             'uint32", "type_name": "optional"}}, {"type": "uint32", "name": "type", "type_v3"'
                             ': {"item": "uint32", "type_name": "optional"}}, {"type": "any", "name": "unique_'
                             'id", "type_v3": {"item": "int32", "type_name": "list"}}, {"type": "string", "nam'
                             'e": "user_interface", "type_v3": {"item": "string", "type_name": "optional"}}, {'
                             '"type": "double", "name": "value", "type_v3": {"item": "double", "type_name": "o'
                             'ptional"}}, {"type": "double", "name": "weight", "type_v3": {"item": "double", "'
                             'type_name": "optional"}}, {"type": "string", "name": "track", "type_v3": {"item"'
                             ': "string", "type_name": "optional"}}, {"type": "string", "name": "id", "type_v3'
                             '": {"item": "string", "type_name": "optional"}}, {"type": "double", "name": "tar'
                             'get", "type_v3": {"item": "double", "type_name": "optional"}}, {"type": "double"'
                             ', "name": "__random__", "type_v3": "double"}, {"type": "double", "name": "group_'
                             'id", "type_v3": {"item": "double", "type_name": "optional"}}]',
    "_field_types": '{'
                    '"weight": "realvalue",'
                    '"like.length": "realvalue",'
                    '"own_play$track": "categorical",'
                    '"track.length": "realvalue",'
                    '"track": "categorical",'
                    '"__row_index_4join__": "categorical",'
                    '"own_play.length": "realvalue",'
                    '"like$track": "categorical",'
                    '"group_id": "realvalue",'
                    '"target": "realvalue",'
                    '"like$track.length": "realvalue",'
                    '"own_play$track.length": "realvalue",'
                    '"unique_id": "categorical"'
                    '}'
}


@pytest.fixture(scope="module")
def data_directory(local_yt):
    print("Creating data directory...")
    client = local_yt.get_client()
    path = YT_ROOT

    # Create train table
    yt.create('map_node', path, client=client, recursive=True)
    with open("offline_train/train.json", "rb") as f:
        data = f.read()

    schema = [{"type_v3": {"type_name": "optional", "item": "uint64"},
               "sort_order": "ascending",
               "type": "uint64", "required": False,
               "type_v2": {"metatype": "optional", "element": "uint64"}, "name": "__row_index_4reduce__"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"},
               "name": "__row_index_4join___pytorch_cat_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"},
               "name": "__row_index_4join___pytorch_cat_data_len"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "group_id_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"},
               "name": "like$track.length_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "like$track_pytorch_cat_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "like$track_pytorch_cat_data_len"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "like.length_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"},
               "name": "own_play$track.length_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "own_play$track_pytorch_cat_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "own_play$track_pytorch_cat_data_len"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "own_play.length_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "target_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "track.length_pytorch_dense_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "track_pytorch_cat_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "track_pytorch_cat_data_len"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "unique_id_pytorch_cat_data"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "unique_id_pytorch_cat_data_len"},
              {"type_v3": {"type_name": "optional", "item": "string"}, "type": "string", "required": False,
               "type_v2": {"metatype": "optional", "element": "string"}, "name": "weight_pytorch_dense_data"}]
    yt.create(
        "table",
        yt.TablePath(yt.ypath_join(path, 'train'), attributes=dict(schema=schema, compression_codec="none")),
        recursive=True,
        client=client,
        attributes=dict(schema=schema)
    )

    yt.write_table(
        yt.TablePath(yt.ypath_join(path, 'train'), attributes=dict(schema=schema, compression_codec="none")),
        data,
        format=yt.JsonFormat(),
        raw=True,
        client=client,
        force_create=True,
    )

    yt.run_sort(
        yt.ypath_join(path, 'train'),
        yt.TablePath(yt.ypath_join(path, 'train'), attributes=dict(compression_codec="none")),
        sort_by=["__row_index_4reduce__"],
        client=client
    )

    for key, value in TRAIN_ATTRIBUTES.items():
        yt.set(
            os.path.join(yt.ypath_join(path, 'train'), "@" + key),
            value,
            client=client
        )

    # Create test table
    with open("offline_test/test.json", "rb") as f:
        data = f.read()

    yt.create(
        "table",
        yt.TablePath(yt.ypath_join(path, 'test'), attributes=dict(schema=schema, compression_codec="none")),
        recursive=True,
        client=client,
        attributes=dict(schema=schema)
    )

    yt.write_table(
        yt.TablePath(yt.ypath_join(path, 'test'), attributes=dict(schema=schema, compression_codec="none")),
        data,
        format=yt.JsonFormat(),
        raw=True,
        client=client,
        force_create=True,
    )

    yt.run_sort(
        yt.ypath_join(path, 'test'),
        yt.TablePath(yt.ypath_join(path, 'test'), attributes=dict(compression_codec="none")),
        sort_by=["__row_index_4reduce__"],
        client=client
    )

    for key, value in TRAIN_ATTRIBUTES.items():
        yt.set(
            os.path.join(yt.ypath_join(path, 'test'), "@" + key),
            value,
            client=client
        )

    yield path


@pytest.fixture(scope="module")
def prepared_env(local_yt):
    print("Preparing env...")
    client = local_yt.get_client()  # yt.YtClient
    proxy = client.config["proxy"]["url"]
    # Pass proxy to catch it in stdout for easier debugging
    with prepare_environment(packages_list=TO_INSTALL, proxy=proxy) as env:
        # Setup local yt environment
        env["YT_PROXY"] = proxy
        env["YT_TOKEN"] = "rfgecnf"  # local yt accepts any token
        env["CUDA_VISIBLE_DEVICES"] = "0"
        yield env


def run_train(train_table, test_table, model_directory, env, input_model_directory=None):
    yatest.common.execute(
        [DSSM_RUNNER, 'train', '--script', SCRIPT_PATH, '--model-config', CONFIG_PATH,
         '--model-directory', model_directory, '--gpu-concurrency', '1', '--cpp-lazy-context',
         '--snapshot-frequency', '3600', '--epoch-count', '2',
         '--train-table', train_table, '--test-table', test_table] +
        (['--input-model-directory', input_model_directory] if input_model_directory else []) +
        [arg for feature in FEATURES for arg in ['--feature', feature]] +
        [arg for target in TARGETS for arg in ['--target', target]] +
        ['--save-convertible-model', '--additional-file', 'factors_config.json', FACTORS_CONFIG_PATH],
        env=env,
        stdout=sys.stdout,
        stderr=sys.stderr
    )


def run_apply(apply_table, dst_table, model_directory, env):
    yatest.common.execute(
        [DSSM_RUNNER, 'apply', '--src-table', apply_table, '--dst-table', dst_table, '--model-directory',
         model_directory, '--gpu-concurrency', '1', '--cpp-lazy-context',
         '--feature-prefix', 'DSSM', '--output-column-description', 'cd.tsv', '--bytes-factor', 'unique_id'],
        env=env,
        stdout=sys.stdout,
        stderr=sys.stderr
    )


def run_export(model_directory, env):
    yatest.common.execute(
        [DSSM_RUNNER, 'export', '--model-directory', model_directory, '--export-archive', 'export_archive.tar',
         '--validate-type', 'compare', '--validate-eps', '0.0001', '--export-prefix', 'pytorch_dssm',
         '--factors-json', FACTORS_PATH],
        env=env,
        stdout=sys.stdout,
        stderr=sys.stderr
    )


def test_train(data_directory, prepared_env):
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "model"),
              env=prepared_env
              )


# TODO test behavior is deprecated
def _test_continue(data_directory, prepared_env):
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "model"),
              input_model_directory=yt.ypath_join(data_directory, "model"),
              env=prepared_env)


def test_apply(data_directory, prepared_env):
    run_apply(apply_table=yt.ypath_join(data_directory, "test"),
              dst_table=yt.ypath_join(data_directory, "applied_0"),
              model_directory=yt.ypath_join(data_directory, "model"),
              env=prepared_env)


def _test_export(env):
    run_export(model_directory=yt.ypath_join(data_directory, "model"), env=env)


# TODO test behavior is deprecated
def _test_train_and_continue(data_directory, prepared_env):
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "model_for_continue"),
              env=prepared_env
              )
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "new_model"),
              input_model_directory=yt.ypath_join(data_directory, "model_for_continue"),
              env=prepared_env
              )


def test_train_and_apply(data_directory, prepared_env):
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "model_for_apply"),
              env=prepared_env
              )
    run_apply(apply_table=yt.ypath_join(data_directory, "test"),
              dst_table=yt.ypath_join(data_directory, "applied_1"),
              model_directory=yt.ypath_join(data_directory, "model_for_apply"),
              env=prepared_env
              )


# Fixme Test doesn't work. https://paste.yandex-team.ru/4550571 Not shippable model here.
# Ask arumyan@ for exportable model.
def _test_train_and_export(data_directory, prepared_env):
    run_train(train_table=yt.ypath_join(data_directory, "train"),
              test_table=yt.ypath_join(data_directory, "test"),
              model_directory=yt.ypath_join(data_directory, "model_for_export"),
              env=prepared_env
              )
    run_export(model_directory=yt.ypath_join(data_directory, "model_for_export"), env=prepared_env)

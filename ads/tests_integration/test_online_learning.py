import subprocess
import pytest
import yt.wrapper as yt
import os
import torch.nn.functional
import json
import contextlib
import shlex
import shutil
import tempfile
import pickle
from ads_pytorch.core.model_serializer import ModelInfo
from ads_pytorch.highlevel_interface import (
    create_synchronous_minibatch_record_iterator,
    get_local_minibatch_stream_reader_options,
    CommonYtOptions
)
from ads_pytorch.nirvana.model_factory import (
    load_ads_pytorch_script_as_module,
    get_ads_pytorch_model_factory,
    ModelFactory
)
from ads_pytorch.yt.table_path import TablePath


@pytest.fixture
def yt_client():
    # for local yt, token is None
    return yt.YtClient(proxy=os.environ["YT_PROXY"], token=os.environ.get("YT_TOKEN", None))


def make_command(wrapper: str, script: str, config: str) -> str:
    return f"python {wrapper} {script} --files model_config={config} --entry_point ads_pytorch.yt.online_learning_entry_point"


def check_call(cmd):
    subprocess.check_call(
        shlex.split(cmd),
        start_new_session=True  # some of processes call SIGKILL on entire process group to ensure tests failure
    )


def check_output(cmd):
    return subprocess.check_output(
        shlex.split(cmd),
        start_new_session=True  # some of processes call SIGKILL on entire process group to ensure tests failure
    ).decode("utf-8")


@contextlib.contextmanager
def local_experiment_dir(fixture_folder, script, config, patch_conf_fn=lambda x: x):
    with tempfile.TemporaryDirectory() as tmp:
        result_script_path = os.path.join(tmp, "script.py")
        result_cfg_path = os.path.join(tmp, "config.json")

        shutil.copy2(
            src=os.path.join(fixture_folder, script),
            dst=result_script_path
        )

        shutil.copy2(
            src=os.path.join(fixture_folder, config),
            dst=result_cfg_path
        )

        with open(result_cfg_path, "rt") as f:
            dct = json.load(f)

        patch_conf_fn(dct)
        with open(result_cfg_path, "wt") as f:
            json.dump(dct, f)

        yield result_script_path, result_cfg_path


def _cmp_local_and_yt(local_path, yt_path, yt_client):
    assert yt.exists(yt_path, client=yt_client)
    script_on_yt = b''.join(list(yt.read_file(yt_path, client=yt_client)))
    with open(local_path, 'rb') as f:
        script_data = f.read()
    assert script_on_yt == script_data


def read_model_info_from_model_yt_dir(model_yt_dir, yt_client) -> ModelInfo:
    path = os.path.join(model_yt_dir, "model", "model_info")
    return pickle.loads(b''.join(list(yt.read_file(path, client=yt_client))))


def _get_bce_loss_from_eval(eval_path, yt_client) -> float:
    rows = list(yt.read_table(yt.TablePath(eval_path, columns=["IsClick", "prediction"]), client=yt_client))
    targets = [float(x["IsClick"]) for x in rows]
    predictions = [float(x["prediction"]) for x in rows]
    loss_value = torch.nn.functional.binary_cross_entropy_with_logits(
        torch.tensor(predictions),
        torch.tensor(targets),
        reduction="mean"
    )
    return loss_value.item()


def test_model_yt_dir(yt_client, fixture_folder, ads_pytorch_script_wrapper):
    model_yt_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/model111"

    def foo(dct):
        dct["model_yt_dir"] = model_yt_dir

    with local_experiment_dir(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json",
        patch_conf_fn=foo
    ) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        check_call(cmd)

        _cmp_local_and_yt(local_path=script, yt_path=os.path.join(model_yt_dir, "script.py"), yt_client=yt_client)
        _cmp_local_and_yt(local_path=config, yt_path=os.path.join(model_yt_dir, "model_config.json"), yt_client=yt_client)

        # evaluation
        eval_path = os.path.join(model_yt_dir, "evaluation/train/shift_1")
        assert yt.exists(eval_path, client=yt_client)
        assert yt.get(os.path.join(eval_path, "@row_count"), client=yt_client) == 41491
        assert _get_bce_loss_from_eval(eval_path=eval_path, yt_client=yt_client) < 0.65

        # final model directory
        final_model_dir = os.path.join(model_yt_dir, "model")
        assert yt.exists(final_model_dir, client=yt_client)

        # check existence of "has_finished" attribute. It's used in all top-level pipelines
        assert yt.exists(os.path.join(final_model_dir, "@_training_finished"), client=yt_client)

        # Now we check that attribute _training_finished is processed by pipeline.
        # let's just check that everything inside final model dir is left untouched
        def _get_modification_times():
            return {
                key: yt.get(os.path.join(final_model_dir, key, "@modification_time"), client=yt_client)
                for key in yt.list(final_model_dir, client=yt_client)
            }

        first_modification_times = _get_modification_times()
        check_call(cmd)
        second_modification_times = _get_modification_times()
        assert second_modification_times == first_modification_times

        # Check that our model has properly processed all batches of data
        model_info = read_model_info_from_model_yt_dir(model_yt_dir=model_yt_dir, yt_client=yt_client)
        assert model_info.hash_embedding_sizes == {
            '_net.embedding_model.embeddings.BannerBMCategoryID': 5028,
            '_net.embedding_model.embeddings.BannerID': 66257,
            '_net.embedding_model.embeddings.BannerTexts': 39490,
            '_net.embedding_model.embeddings.ImpID': 634,
            '_net.embedding_model.embeddings.PageID': 2335,
            '_net.embedding_model.embeddings.PageID,ImpID': 4691,
            '_net.embedding_model.embeddings.UserBestInterests': 4401,
            '_net.embedding_model.embeddings.UserLast10CatalogiaCategories': 6026,
            '_net.embedding_model.embeddings.UserRegionID': 1091
        }


# Train two models and check that second model can be correctly started from second
def test_start_model(yt_client, fixture_folder, ads_pytorch_script_wrapper):
    first_model_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/first_model_dir"
    second_model_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/second_model_dir"

    def first_model_patch(dct):
        dct["model_yt_dir"] = first_model_dir
        dct["evaluation"]["eval_confs"] = []
        dct["train_data"]["end_date"] = "202104160400"

    def second_model_patch(dct):
        dct["model_yt_dir"] = second_model_dir
        dct["start_model"] = os.path.join(first_model_dir, "model")
        dct["train_data"]["start_date"] = "202104160400"

    common_kwargs = dict(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json"
    )

    with local_experiment_dir(**common_kwargs, patch_conf_fn=first_model_patch) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        check_call(cmd)

    model_info = read_model_info_from_model_yt_dir(model_yt_dir=first_model_dir, yt_client=yt_client)
    assert model_info.hash_embedding_sizes == {
        '_net.embedding_model.embeddings.BannerBMCategoryID': 3889,
        '_net.embedding_model.embeddings.BannerID': 26905,
        '_net.embedding_model.embeddings.BannerTexts': 23879,
        '_net.embedding_model.embeddings.ImpID': 537,
        '_net.embedding_model.embeddings.PageID': 1621,
        '_net.embedding_model.embeddings.PageID,ImpID': 3154,
        '_net.embedding_model.embeddings.UserBestInterests': 3573,
        '_net.embedding_model.embeddings.UserLast10CatalogiaCategories': 4918,
        '_net.embedding_model.embeddings.UserRegionID': 907
    }

    with local_experiment_dir(**common_kwargs, patch_conf_fn=second_model_patch) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        check_call(cmd)

    # If model has not been read, we will obtain less numbers here
    # because we've increased start date on second launch
    model_info = read_model_info_from_model_yt_dir(model_yt_dir=second_model_dir, yt_client=yt_client)
    assert model_info.hash_embedding_sizes == {
        '_net.embedding_model.embeddings.BannerBMCategoryID': 5028,
        '_net.embedding_model.embeddings.BannerID': 66257,
        '_net.embedding_model.embeddings.BannerTexts': 39490,
        '_net.embedding_model.embeddings.ImpID': 634,
        '_net.embedding_model.embeddings.PageID': 2335,
        '_net.embedding_model.embeddings.PageID,ImpID': 4691,
        '_net.embedding_model.embeddings.UserBestInterests': 4401,
        '_net.embedding_model.embeddings.UserLast10CatalogiaCategories': 6026,
        '_net.embedding_model.embeddings.UserRegionID': 1091
    }


def test_offline_eval(yt_client, fixture_folder, ads_pytorch_script_wrapper):
    model_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/offline_eval_model_dir"

    def model_patch(dct):
        dct["model_yt_dir"] = model_dir
        dct["evaluation"]["eval_confs"] = []
        dct["offline_evaluation"] = {
            "eval_confs": [
                {
                    "table": dct["train_data"]["table"],
                    "exact_index": 0,
                    "uri_frequency": 1
                }
            ]
        }

    common_kwargs = dict(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json"
    )

    with local_experiment_dir(**common_kwargs, patch_conf_fn=model_patch) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        check_call(cmd)


# Bad to test two at once, and lazy to split
def test_snapshot_and_user_api_for_callbacks(yt_client, fixture_folder, ads_pytorch_script_wrapper):
    model_yt_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/snapshot_dir"

    def model_patch(dct):
        dct["model_yt_dir"] = model_yt_dir
        dct["snapshotter"]["frequency"] = 1  # will ensure snapshot save on every iteration

    with local_experiment_dir(
        fixture_folder=fixture_folder,
        script="light_dssm_with_fail_callback.py",
        config="light_dssm_config.json",
        patch_conf_fn=model_patch
    ) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        # Fail at the first time
        with pytest.raises(Exception):
            check_call(cmd)

        # Check we've trained at certain point
        assert yt.exists(os.path.join(model_yt_dir, "snapshot"), client=yt_client)
        model_info = pickle.loads(b''.join(list(yt.read_file(os.path.join(model_yt_dir, "snapshot", "model_info"), client=yt_client))))
        assert model_info.hash_embedding_sizes == {
            '_net.embedding_model.embeddings.BannerBMCategoryID': 4730,
            '_net.embedding_model.embeddings.BannerID': 52682,
            '_net.embedding_model.embeddings.BannerTexts': 34606,
            '_net.embedding_model.embeddings.ImpID': 619,
            '_net.embedding_model.embeddings.PageID': 2188,
            '_net.embedding_model.embeddings.PageID,ImpID': 4410,
            '_net.embedding_model.embeddings.UserBestInterests': 4166,
            '_net.embedding_model.embeddings.UserLast10CatalogiaCategories': 5694,
            '_net.embedding_model.embeddings.UserRegionID': 1052
        }

        # Run training to finish

        # We can't only check final model because it could be started from scratch.
        # We look at stdout logs and see which URI's have been processed.
        stdout = check_output(cmd)
        process_lines = [x for x in stdout.split("\n") if "Processing URI" in x]
        assert set(process_lines) == {
            """Processing URI <"ranges"=[{"lower_limit"={"key"=["202104160800";];};"upper_limit"={"key"=["202104160900";];};};];>//home/ads/tzar/ci/test_data/raw_logs_concatenated_torch_mb1024""",
            """Processing URI <"ranges"=[{"lower_limit"={"key"=["202104160900";];};"upper_limit"={"key"=["202104161000";];};};];>//home/ads/tzar/ci/test_data/raw_logs_concatenated_torch_mb1024""",
        }

        # Check final model has same size as trained in usual way
        model_info = read_model_info_from_model_yt_dir(model_yt_dir=model_yt_dir, yt_client=yt_client)
        assert model_info.hash_embedding_sizes == {
            '_net.embedding_model.embeddings.BannerBMCategoryID': 5028,
            '_net.embedding_model.embeddings.BannerID': 66257,
            '_net.embedding_model.embeddings.BannerTexts': 39490,
            '_net.embedding_model.embeddings.ImpID': 634,
            '_net.embedding_model.embeddings.PageID': 2335,
            '_net.embedding_model.embeddings.PageID,ImpID': 4691,
            '_net.embedding_model.embeddings.UserBestInterests': 4401,
            '_net.embedding_model.embeddings.UserLast10CatalogiaCategories': 6026,
            '_net.embedding_model.embeddings.UserRegionID': 1091
        }

        eval_path = os.path.join(model_yt_dir, "evaluation/train/shift_1")
        assert yt.exists(eval_path, client=yt_client)
        assert yt.get(os.path.join(eval_path, "@row_count"), client=yt_client) == 41491
        assert _get_bce_loss_from_eval(eval_path=eval_path, yt_client=yt_client) < 0.65


def test_ideployable_model_serialization(yt_client, fixture_folder, ads_pytorch_script_wrapper):
    model_yt_dir = "//home/ads/tzar/ci/test_data/model_yt_dirs/ideployable_model_dir"

    def foo(dct):
        dct["model_yt_dir"] = model_yt_dir
        dct["deployable_model"] = {"final_save": True}

    with local_experiment_dir(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json",
        patch_conf_fn=foo
    ) as (script, config):
        cmd = make_command(wrapper=ads_pytorch_script_wrapper, script=script, config=config)
        check_call(cmd)

    # non-arcadia side ends here, however,
    # we will read this directory in arcadia-side test (which launches this one)
    # and run arcadia code for model conversion to make fully integrational deployability check
    assert yt.exists(os.path.join(model_yt_dir, "ideployable_model"), client=yt_client)


# Test sync data loader here because it's in some sense part of all pipelines
# We should be able to initialize sync


def test_synchronous_data_loader(fixture_folder):
    table = TablePath(
        "//home/ads/tzar/ci/test_data/raw_logs_concatenated_torch_mb1024",
        exact_index=0
    )

    with local_experiment_dir(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json",
        patch_conf_fn=lambda x: None
    ) as (script, config):
        factory_cls = get_ads_pytorch_model_factory(
            script_as_module=load_ads_pytorch_script_as_module(script=script)
        )
        factory: ModelFactory = factory_cls(config)
        model = factory.create_model()

        # This is user-side scenario for

        data_loader = create_synchronous_minibatch_record_iterator(
            table=table,
            features=factory.get_required_features_list(),
            targets=["IsClick"],
            stream_reader_options=get_local_minibatch_stream_reader_options(),
            yt_options=CommonYtOptions(
                token="rfgecnf",
                cluster=os.environ["YT_PROXY"]
            ),
        )
        for record in data_loader:
            model(record.inputs)

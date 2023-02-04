import subprocess
import pytest
import yt.wrapper as yt
import os
import json
import contextlib
import shlex
import shutil
import tempfile


@pytest.fixture
def yt_client():
    # for local yt, token is None
    return yt.YtClient(proxy=os.environ["YT_PROXY"], token=os.environ.get("YT_TOKEN", None))


def make_command(wrapper: str, script: str, config: str) -> str:
    return f"python {wrapper} {script} --files model_config={config} --entry_point ads_pytorch.yt.online_learning_entry_point"


def make_save_snapshot_command(wrapper: str, script: str, config: str, folder: str) -> str:
    return f"python {wrapper} {script} --files model_config={config} --entry_point ads_pytorch.yt.empty_snapshot.entry_point --folder {folder}"


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


@pytest.fixture(scope="module")
def save_empty_snapshot(fixture_folder, ads_pytorch_script_wrapper):
    saved_snapshot_path = "//home/ads/tzar/ci/test_data/model_yt_dirs/empty_snapshot"

    def foo(dct):
        dct["model_yt_dir"] = None  # empty snapshot shall not use this!

    with local_experiment_dir(
        fixture_folder=fixture_folder,
        script="light_dssm.py",
        config="light_dssm_config.json",
        patch_conf_fn=foo
    ) as (script, config):
        cmd = make_save_snapshot_command(
            wrapper=ads_pytorch_script_wrapper,
            script=script,
            config=config,
            folder=saved_snapshot_path
        )
        subprocess.check_call(shlex.split(cmd))

    return saved_snapshot_path


def test_make_empty_snapshot(save_empty_snapshot, yt_client):
    assert yt.exists(save_empty_snapshot, client=yt_client)

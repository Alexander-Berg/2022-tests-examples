import pytest
import torch
import os
import psutil
import uvloop
import tempfile
uvloop.install()


@pytest.fixture(autouse=True)
def cleanup_cuda():
    yield

    # perform cleanup after test run to clean memory from GPU in xdist running
    current_process = psutil.Process(pid=os.getpid())
    for child in current_process.children(recursive=True):
        child.kill()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()


@pytest.fixture(scope="session")
def fixture_folder():
    return os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "fixture"
    )


@pytest.fixture(scope="session", autouse=True)
def ads_pytorch_build_dir():
    # reuse build dir from environment in case of locally launched tests
    # to speed up testing (skip 1-2 minutes to compile C++)
    env_name = "ADS_PYTORCH_BUILD_DIR"
    if env_name in os.environ:
        yield os.environ[env_name]
    else:
        with tempfile.TemporaryDirectory() as build_dir:
            os.environ[env_name] = build_dir
            try:
                yield build_dir
            finally:
                os.environ.pop(env_name)


@pytest.fixture(scope="session")
def ads_pytorch_script_wrapper():
    if "ADS_PYTORCH_SCRIPT_WRAPPER_PATH_FOR_TESTS" in os.environ:
        yield os.environ["ADS_PYTORCH_SCRIPT_WRAPPER_PATH_FOR_TESTS"]
    else:
        inferred_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.realpath(__file__))),
            "ads_pytorch", "nirvana", "wrapper.py"
        )
        if os.path.exists(inferred_path):
            yield inferred_path
        else:
            raise RuntimeError("Could not determine script_wrapper's path")

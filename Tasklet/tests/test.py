from __future__ import unicode_literals

import json
import pytest
import re

import six

import yatest.common as yac


BINARIES_PATHS = ["tasklet/tests/bin/bin", "tasklet/tests/bin3/bin3"]


def sanitize_stderr(text):
    text = six.ensure_text(text)
    log_re = re.compile(r"^\d{4}-\d{2}-\d{2} ")
    line_re = re.compile(r", line \d+,")

    lines = [line_re.sub(", <NUM>,", line) + "\n" for line in text.splitlines() if not log_re.match(line)]
    return "".join(lines)


@pytest.mark.parametrize(
    ("impl", "bin_path"),
    (
        (tasklet_name, bin_path)
        for tasklet_name in ("AwaitTask", "SumTask", "ProductTask", "CompareTask")
        for bin_path in BINARIES_PATHS
    ),
)
def test__run(impl, bin_path):
    bin_path = yac.binary_path(bin_path)
    return yac.canonical_execute(bin_path, save_locally=True, args=_run_args(impl), env=_cli_run_env())


@pytest.mark.parametrize(
    "bin_path",
    BINARIES_PATHS,
)
def test__schema_binary(bin_path):
    bin_path = yac.binary_path(bin_path)
    return yac.canonical_execute(bin_path, save_locally=True, args=["schema", "AwaitTask", "--binary"], env=_cli_run_env())


def test__schema():
    bin = yac.binary_path("tasklet/tests/bin/bin")
    return yac.canonical_execute(bin, save_locally=True, args=["schema", "SumTask"], env=_cli_run_env())


@pytest.mark.parametrize(
    "impl",
    ("AwaitTask", "SumTask")
)
def test__broken(impl):

    bin = yac.binary_path("tasklet/tests/bin_broken/bin_broken")
    res = yac.process.execute(command=[bin] + _run_args(impl), check_exit_code=False, env=_cli_run_env())
    assert res.exit_code != 0
    return sanitize_stderr(res.std_err)


@pytest.mark.parametrize(
    "bin_path",
    BINARIES_PATHS,
)
def test__id(bin_path):
    bin_path = yac.binary_path(bin_path)
    cmd = [bin_path, "run", "--test", "CheckIdTaskPy", "--input", json.dumps(dict(is_child=False))]
    yac.process.execute(command=cmd, check_exit_code=True, env=_cli_run_env())


@pytest.mark.parametrize(
    "bin_path",
    BINARIES_PATHS,
)
def test__ignore_unknown_fields(bin_path):
    bin_path = yac.binary_path(bin_path)
    cmd = [bin_path, "run", "--test", "CheckIdTaskPy", "--input", json.dumps(dict(is_child=False, unknown_field="value"))]
    yac.process.execute(command=cmd, check_exit_code=True, env=_cli_run_env())


def _cli_run_env():
    return {"LOGS_DIR": yac.test_output_path()}


def _run_args(impl):
    return ["run", "--test", impl, "--input", json.dumps(dict(a=1, b=2))]

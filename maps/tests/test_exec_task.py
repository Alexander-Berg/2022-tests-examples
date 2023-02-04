from io import StringIO
from contextlib import contextmanager
import unittest
import sys
import os

from maps.pylibs.utils.lib.process import ExecutionFailed
from maps.pylibs.utils.lib.filesystem import temporary_directory

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FileResource

from maps.garden.sdk.extensions.exec_task import DeprecatedExecTask, run_subprocess


class OverriddenStreams:
    def __init__(self):
        self._out = StringIO()
        self._err = StringIO()

    def __enter__(self):
        sys.stdout = self._out
        sys.stderr = self._err
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        sys.stdout = sys.__stdout__
        sys.stderr = sys.__stderr__

    def out(self):
        return self._out.getvalue()

    def err(self):
        return self._err.getvalue()


@contextmanager
def construct_settings():
    with temporary_directory() as path:
        persistent_path = os.path.join(path, "persistent_data")

        settings = {
            "filesystem": {
                "persistent_path": persistent_path,
            },
        }
        yield settings


def construct_resource(name, settings):
    resource = FileResource(name, name)
    resource.version = Version()
    resource.load_environment_settings(settings)
    resource.exists = False
    return resource


class ExecTaskTest(unittest.TestCase):
    def test_plain_success(self):
        TEST_DATA = "Success"
        cmd = 'printf "{0}"'.format(TEST_DATA)
        task = DeprecatedExecTask(cmd, shell=True)
        with OverriddenStreams() as streams:
            out = task()
            self.assertEqual(out, TEST_DATA)
            self.assertEqual(out, streams.out())

    def test_success_with_error_output(self):
        TEST_DATA = "ErrorOutput"
        cmd = '1>&2 printf "{0}"'.format(TEST_DATA)
        task = DeprecatedExecTask(cmd, shell=True)
        with OverriddenStreams() as streams:
            out = task()
            self.assertEqual(out, "")
            self.assertEqual(out, streams.out())
            self.assertEqual(streams.err(), TEST_DATA)

    def test_failure(self):
        TEST_DATA = "Failure"
        cmd = 'printf "{0}" && 1>&2 printf "{0}" && /bin/false'.format(TEST_DATA)
        task = DeprecatedExecTask(cmd, shell=True)
        with OverriddenStreams():
            self.assertRaises(ExecutionFailed, task)

        with OverriddenStreams():
            error = None
            try:
                task()
            except ExecutionFailed as ex:
                error = ex
            self.assertTrue(error is not None)
            self.assertEqual(error.out, TEST_DATA)
            self.assertEqual(error.err, TEST_DATA)
            self.assertEqual(error.cmd, cmd)
            self.assertNotEqual(error.returncode, 0)

    def test_overridden_input(self):
        TEST_DATA = "Hello"
        cmd = 'read answer; printf "$answer";'
        task = DeprecatedExecTask(cmd, shell=True)

        with construct_settings() as settings:
            res_in = construct_resource("temp_in", settings)
            with res_in.open("w") as f:
                f.write(TEST_DATA)

            with OverriddenStreams():
                out = task(stdin=res_in)
                self.assertEqual(out, TEST_DATA)

    def test_communicate(self):
        TEST_DATA = "Communicate test"
        cmd = "cat"
        with OverriddenStreams() as streams:
            out = run_subprocess(cmd, communicate=TEST_DATA, forward_stdout=False)
            self.assertEqual(out, TEST_DATA)
            self.assertEqual(streams.out(), "")

    def test_forwarding(self):
        TEST_DATA = "Test forwarding"
        cmd = "cat"
        with OverriddenStreams() as streams:
            out = run_subprocess(cmd, communicate=TEST_DATA)
            self.assertEqual(out, TEST_DATA)
            self.assertEqual(streams.out(), TEST_DATA)

    def test_overridden_output(self):
        TEST_DATA_OUT = "Hello from stdout"
        TEST_DATA_ERR = "Hello from stderr"
        cmd = 'printf "{0}" && 1>&2 printf "{1}"'.format(TEST_DATA_OUT, TEST_DATA_ERR)
        task = DeprecatedExecTask(cmd, shell=True)

        with construct_settings() as settings:
            res_out = construct_resource("temp_out", settings)
            with OverriddenStreams() as streams:
                out = task(stdout=res_out)
                # if output is overridden then task must return empty string
                self.assertFalse(out)
                self.assertFalse(streams.out())
                with res_out.open("r") as fout:
                    out_data = fout.readlines()
                    self.assertEqual(out_data, [TEST_DATA_OUT])
                self.assertEqual(streams.err(), TEST_DATA_ERR)

    def test_overridden_errors(self):
        TEST_DATA_OUT = "Hello from stdout"
        TEST_DATA_ERR = "Hello from stderr"
        cmd = 'printf "{0}" && 1>&2 printf "{1}"'.format(TEST_DATA_OUT, TEST_DATA_ERR)
        task = DeprecatedExecTask(cmd, shell=True)

        with construct_settings() as settings:
            res_err = construct_resource("temp_err", settings)
            with OverriddenStreams() as streams:
                out = task(stderr=res_err)
                self.assertFalse(streams.err())
                self.assertEqual(out, TEST_DATA_OUT)
                self.assertEqual(out, streams.out())
                with res_err.open("r") as ferr:
                    err_data = ferr.readlines()
                    self.assertEqual(err_data, [TEST_DATA_ERR])

    def test_non_shell_task(self):
        TEST_DATA = "Test data"
        cmd = ['/usr/bin/printf', TEST_DATA]
        task = DeprecatedExecTask(cmd, shell=False)

        with OverriddenStreams():
            out = task()
            self.assertEqual(out, TEST_DATA)

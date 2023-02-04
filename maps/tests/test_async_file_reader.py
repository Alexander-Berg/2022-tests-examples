import unittest
import subprocess

from maps.garden.sdk.extensions.exec_task import AsyncFileReader


SCRIPT = r"""
import sys

sys.stderr.write('stderr1\n')
sys.stdout.write('stdout1\n')
sys.stderr.write('stderr2')
sys.stdout.write('stdout2')
"""

SCRIPT_FILL_BUFFER = r"""
import sys

for i in range(1000000):
    sys.stdout.write("stdout\n")

for i in range(1000000):
    sys.stderr.write("stderr\n")
"""


class LinesCapture:
    def __init__(self):
        self.lines = []

    def __call__(self, line):
        self.lines.append(line)


class LinesCaptureRaise:
    def __init__(self):
        self.lines = []

    def __call__(self, line):
        self.lines.append(line)
        raise Exception()


def _kwargs():
    # Popen stdout, stderr methods are binary streams by default.
    # But AsyncFileReader requires text streams.
    return {"text": True}


class AsyncFileReaderTest(unittest.TestCase):
    def test_async_file_reader(self):
        process = subprocess.Popen(
            ["python", "-c", SCRIPT],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, **_kwargs())

        std_out_lines = LinesCapture()
        std_out_reader = AsyncFileReader(process.stdout, std_out_lines)
        std_out_reader.start()

        code = process.wait()
        self.assertEqual(code, 0)
        std_out_reader.join()

        self.assertEqual(["stdout1\n", "stdout2"], std_out_lines.lines)

    def test_async_file_reader_raise(self):
        process = subprocess.Popen(
            ["python", "-c", SCRIPT_FILL_BUFFER],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, **_kwargs())

        std_out_lines = LinesCaptureRaise()
        std_out_reader = AsyncFileReader(process.stdout, std_out_lines)
        std_out_reader.start()

        code = process.wait()
        self.assertEqual(code, 1)
        std_out_reader.join()

        self.assertEqual(["stdout\n"], std_out_lines.lines)

import pytest
import yatest
import subprocess
import os.path
import sys
import json

import logging
log = logging.getLogger(__name__)


@pytest.fixture
def split_sessions_binary():
    path = yatest.common.binary_path("maps/automotive/parking/tools/qa/split_sessions/split_sessions")
    assert os.path.exists(path)
    return path


@pytest.fixture
def split_sessions_proc(split_sessions_binary):
    return subprocess.Popen(split_sessions_binary)


def parse_output(output):
    lines = output.decode("utf-8").split("\n")

    assert lines[-1] == ""

    return [json.loads(line) for line in lines[:-1]]


def last_line(stderr_output):
    '''
        Helper function to get exception name from stderr. Example output:

        Traceback (most recent call last):
            File "contrib/tools/python3/src/Lib/runpy.py", line 193, in _run_module_as_main
                "__main__", mod_spec)
            File "contrib/tools/python3/src/Lib/runpy.py", line 85, in _run_code
                exec(code, run_globals)
            File "maps/automotive/parking/tools/qa/split_sessions/__main__.py", line 127, in <module>
                main()
            File "maps/automotive/parking/tools/qa/split_sessions/__main__.py", line 111, in main
                for (record, session_start_timestamp, is_new_session) in sessions_records(args.split_timestamp_gap):
            File "maps/automotive/parking/tools/qa/split_sessions/__main__.py", line 68, in sessions_records
                for (record, is_new_device) in device_records():
            File "maps/automotive/parking/tools/qa/split_sessions/__main__.py", line 56, in device_records
                raise OutOfOrderDeviceError(current_device_id)
        OutOfOrderDeviceError: d0
    '''

    return stderr_output.split("\n")[-2]


@pytest.fixture
def run_split_sessions(split_sessions_binary):
    '''
        Run split_sessions binary and validates return code

        returns parsed output if result code is zero
        returns stderr if result code is not zero
    '''
    def _runner(input, expected_result_code=0):
        if type(input) is list:
            input = "\n".join([json.dumps(row) for row in input])

        result = subprocess.run(split_sessions_binary,
                                input=input.encode('utf-8'),
                                timeout=3,
                                capture_output=True)

        if result.returncode != expected_result_code:
            sys.stderr.write(result.stderr.decode('utf-8'))

        assert result.returncode == expected_result_code

        if expected_result_code == 0:
            return parse_output(result.stdout)
        else:
            return result.stderr.decode('utf-8')

    return _runner


def row(device_id, request_timestamp, session_start_timestamp=None, **kwargs):
    row_data = dict(device_id=device_id, request_timestamp=request_timestamp)

    if session_start_timestamp is not None:
        row_data["session_start_timestamp"] = session_start_timestamp

    row_data.update(kwargs)

    return row_data


class TestYtOperationIO:
    def test_zero_input(self, run_split_sessions):
        assert run_split_sessions("") == []

    def test_one_row(self, run_split_sessions):
        the_row = dict(device_id="d1", request_timestamp="42")

        proc_input = json.dumps(the_row)
        proc_input += "\n"

        result = run_split_sessions(proc_input)

        assert len(result) == 1

        expected_output_row = the_row
        expected_output_row["session_start_timestamp"] = 42

        assert result[0] == expected_output_row

    def test_one_device_multi_row(self, run_split_sessions):
        input_data = [row("d0", 1, record_data="42"),
                      row("d0", 2, record_data="43"),
                      row("d0", 3, record_data="44"),
                      row("d0", 4, record_data="45"),
                      row("d0", 5, record_data="46"),
                      row("d0", 6, record_data="47"),
                      ]

        result = run_split_sessions(input_data)

        assert result == [row("d0", 1, 1, record_data="42"),
                          row("d0", 2, 1, record_data="43"),
                          row("d0", 3, 1, record_data="44"),
                          row("d0", 4, 1, record_data="45"),
                          row("d0", 5, 1, record_data="46"),
                          row("d0", 6, 1, record_data="47"),
                          ]

    def test_multiple_devices(self, run_split_sessions):
        # two devices - d0 and device-1
        # 3 records per each device

        input_data = [row("d0", 1),
                      row("d0", 3),
                      row("d0", 5),
                      row("d1", 2),
                      row("d1", 3),
                      row("d1", 6),
                      ]

        result = run_split_sessions(input_data)

        assert result == [row("d0", 1, 1),
                          row("d0", 3, 1),
                          row("d0", 5, 1),
                          row("d1", 2, 2),
                          row("d1", 3, 2),
                          row("d1", 6, 2),
                          ]

    def test_split_by_gap(self, run_split_sessions):
        input_data = [row("d0", 1),
                      row("d0", 2),
                      row("d0", 3),
                      row("d0", 1004),
                      row("d0", 1005),
                      row("d0", 1006),
                      ]

        result = run_split_sessions(input_data)

        assert result == [row("d0", 1, 1),
                          row("d0", 2, 1),
                          row("d0", 3, 1),
                          row("d0", 1004, 1004),
                          row("d0", 1005, 1004),
                          row("d0", 1006, 1004),
                          ]

    def test_unsorded_timestamps(self, run_split_sessions):
        input_data = [row("d0", 3),
                      row("d0", 2),
                      row("d0", 1),
                      ]

        stderr = run_split_sessions(input_data, expected_result_code=255)

        assert last_line(stderr) == "OutOfOrderTimestampError: ('d0', 3, 2)"

    def test_unsorded_device_id(self, run_split_sessions):
        input_data = [row("d0", 1),
                      row("d1", 2),
                      row("d0", 3),
                      ]

        stderr = run_split_sessions(input_data, expected_result_code=255)

        sys.stderr.write(stderr)

        assert last_line(stderr) == "OutOfOrderDeviceError: d0"

    def test_table_switch(self, run_split_sessions):
        input_data = [{"$attributes": {"table_index": 0}, "$value": None},
                      row("d0", 1),
                      row("d0", 2),
                      row("d0", 3),
                      {"$attributes": {"table_index": 1}, "$value": None},
                      row("d1", 11),
                      row("d1", 12),
                      row("d1", 13),
                      ]

        result = run_split_sessions(input_data)

        assert result == [row("d0", 1, 1),
                          row("d0", 2, 1),
                          row("d0", 3, 1),
                          row("d1", 11, 11),
                          row("d1", 12, 11),
                          row("d1", 13, 11),
                          ]

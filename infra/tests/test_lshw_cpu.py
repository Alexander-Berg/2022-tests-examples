import json

import os
import mock
import subprocess
from infra.rtc.nodeinfo.lib.modules import lshw_cpu


TEST_OUTPUT = \
'''
[
  {
    "id" : "cpu:0",
    "class" : "processor",
    "claimed" : true,
    "handle" : "DMI:00AC",
    "description" : "CPU",
    "product" : "ARMv8 (Q80-30)",
    "vendor" : "Ampere(R)",
    "physid" : "ac",
    "businfo" : "cpu@0",
    "version" : "Ampere(R) Altra(R) Processor",
    "serial" : "000000000000000081940609033842C8",
    "slot" : "CPU 0",
    "units" : "Hz",
    "size" : 3000000000,
    "capacity" : 3000000000,
    "clock" : 1650000000,
    "configuration" : {
      "cores" : "80",
      "enabledcores" : "80",
      "threads" : "80"
    },
    "capabilities" : {
      "lm" : "64-bit capable"
    }
  },
  {
    "id" : "cpu:1",
    "class" : "processor",
    "claimed" : true,
    "handle" : "DMI:00B1",
    "description" : "CPU",
    "product" : "ARMv8 (Q80-30)",
    "vendor" : "Ampere(R)",
    "physid" : "b1",
    "businfo" : "cpu@1",
    "version" : "Ampere(R) Altra(R) Processor",
    "serial" : "000000000000000081900906033842C8",
    "slot" : "CPU 1",
    "units" : "Hz",
    "size" : 3000000000,
    "capacity" : 3000000000,
    "clock" : 1650000000,
    "configuration" : {
      "cores" : "80",
      "enabledcores" : "80",
      "threads" : "80"
    },
    "capabilities" : {
      "lm" : "64-bit capable"
    }
  }
]
'''


def test_lshw_output_no_lshw(monkeypatch):
    exists = mock.Mock(return_value=False)
    monkeypatch.setattr(os.path, 'exists', exists)
    _, err = lshw_cpu.lshw_output()
    assert err == 'lshw is not installed at /usr/bin/lshw'
    exists.assert_called_once_with('/usr/bin/lshw')


def test_lshw_output_ok(monkeypatch):
    exists = mock.Mock(return_value=True)
    monkeypatch.setattr(os.path, 'exists', exists)
    output = mock.Mock(return_value='{"mock": "mock"}')
    monkeypatch.setattr(subprocess, 'check_output', output)

    j, err = lshw_cpu.lshw_output()
    assert err is None
    output.assert_called_once_with(['/usr/bin/lshw', '-json', '-notime', '-quiet', '-c', 'cpu'])
    assert j == {'mock': 'mock'}

    output.reset_mock()
    output.return_value="malformed{}"
    j, err = lshw_cpu.lshw_output()
    assert j is None
    assert err is not None


def test_get_cpu_info():
    lshw = mock.Mock(return_value=(json.loads(TEST_OUTPUT), None,))
    ci, err = lshw_cpu.get_cpu_info(lshw)
    assert err is None
    assert ci == [
        lshw_cpu.LSHWCPUInfo("ARMv8 (Q80-30)", "Ampere(R)", 80, 80),
        lshw_cpu.LSHWCPUInfo("ARMv8 (Q80-30)", "Ampere(R)", 80, 80),
    ]

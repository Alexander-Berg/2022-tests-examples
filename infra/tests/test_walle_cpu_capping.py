from functools import partial
from itertools import izip_longest
from collections import OrderedDict

import pytest
from mock import Mock, mock_open

from checks import walle_cpu_capping
from .tools import mocked_timestamp

from juggler.bundles import Status, CheckResult

WALLE_CPU_CAPPING = "walle_cpu_capping"


def expected_result(expected_status, expected_description):
    return CheckResult([walle_cpu_capping.make_event(expected_status, expected_description)]).to_dict(service=WALLE_CPU_CAPPING)


@pytest.fixture
def patch(monkeypatch):
    return partial(monkeypatch.setattr, walle_cpu_capping)


@pytest.fixture
def mock_timestamp(monkeypatch):
    monkeypatch.setattr(walle_cpu_capping, "timestamp", lambda: mocked_timestamp())


@pytest.fixture
def mock_unsupported_proc_cpuinfo():
    def mock_fn(path, *args):
        files = {
            '/proc/cpuinfo': mock_open(read_data=
"""
processor	: 0
vendor_id	: GenuineIntel
cpu family	: 6
model		: 44
model name	: Intel(R) Xeon(R) CPU           E5645  @ 2.40GHz
stepping	: 2
microcode	: 0x14
cpu MHz		: 2400.157
cache size	: 12288 KB
physical id	: 1
siblings	: 12
core id		: 10
cpu cores	: 6
apicid		: 53
initial apicid	: 53
fpu		: yes
fpu_exception	: yes
cpuid level	: 11
wp		: yes
flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc arch_perfmon pebs bts rep_good nopl xtopology nonstop_tsc aperfmperf pni pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2 ssse3 cx16 xtpr pdcm pcid dca sse4_1 sse4_2 popcnt aes lahf_lm ida arat epb dtherm tpr_shadow vnmi flexpriority ept vpid
bugs		:
bogomips	: 4800.18
clflush size	: 64
cache_alignment	: 64
address sizes	: 40 bits physical, 48 bits virtual
power management:

""")
        }
        return files[path](path, *args)

    return mock_fn


def test_get_cpu_max_freq_and_limit(patch):
    required_calls = {
        '/sys/devices/system/cpu/cpu2/cpufreq/cpuinfo_max_freq',
        '/sys/devices/system/cpu/cpu2/cpufreq/bios_limit'
    }

    mock = Mock(return_value=100500)
    patch("read_content", mock)

    mx, lim = walle_cpu_capping.get_cpu_max_freq_and_limit(2)

    calls = set()
    for c in mock.mock_calls:
        calls.add(c[1][0])  # mock_call[args][agrN]

    assert calls == required_calls
    assert mx == 100500 and lim == 100500


def test_get_cpu_max_freq_and_limit_no_files(patch):
    mock = Mock(side_effect=IOError(2, "No such file or directory", "/proc/somefile"))
    patch("read_content", mock)

    with pytest.raises(walle_cpu_capping.UnsupportedKernel) as e:
        walle_cpu_capping.get_cpu_max_freq_and_limit(122222)
    assert str(e).endswith("File not found: /proc/somefile")


class TestAssertKernelIsSupported(object):
    def mock_kernel_version(self, patch, ver_parts):
        part_names = ["major", "minor", "patch", "prerelease", "build"]
        ret_val = OrderedDict([(name, val) for name, val in izip_longest(part_names, ver_parts)])
        patch("get_kernel_version", lambda: ret_val)

    @pytest.mark.parametrize("version", [
        (2, 6, 20),
        (3, 3, 3)
    ])
    def test_kernels_older_4_are_not_supported(self, patch, version):
        self.mock_kernel_version(patch, version)
        with pytest.raises(walle_cpu_capping.UnsupportedKernel):
            walle_cpu_capping.assert_kernel_is_supported()

    @pytest.mark.parametrize("version", [
        (4, 0),
        (4, 15, 0, 54, "generic"),
        (5, 4)
    ])
    def test_kernels_ge_4_are_supported(self, patch, version):
        self.mock_kernel_version(patch, version)
        walle_cpu_capping.assert_kernel_is_supported()


def test_capping_detect_capping_supported_cpu(patch, mock_xeon_e5_2660_proc_cpuinfo):
    cpuinfo_mock = Mock(return_value=(3200000, 1200000))
    cpucount_mock = Mock(return_value=2)

    patch("get_cpu_max_freq_and_limit", cpuinfo_mock)
    patch("cpu_count", cpucount_mock)

    status, description = walle_cpu_capping.capping_detect(mock_xeon_e5_2660_proc_cpuinfo)

    assert status == Status.CRIT
    assert description.startswith("Capping detected on 2 CPUs")


def test_capping_detect_capping_unsupported_cpu(patch, mock_unsupported_proc_cpuinfo):
    cpuinfo_mock = Mock(return_value=(3200000, 1200000))
    cpucount_mock = Mock(return_value=2)

    patch("get_cpu_max_freq_and_limit", cpuinfo_mock)
    patch("cpu_count", cpucount_mock)

    status, description = walle_cpu_capping.capping_detect(mock_unsupported_proc_cpuinfo)

    assert status == Status.OK
    assert description == "Ok"


def test_capping_detect_no_capping(patch, mock_xeon_e5_2660_proc_cpuinfo):
    cpuinfo_mock = Mock(return_value=(3200000, 3200000))
    cpucount_mock = Mock(return_value=2)

    patch("get_cpu_max_freq_and_limit", cpuinfo_mock)
    patch("cpu_count", cpucount_mock)

    status, description = walle_cpu_capping.capping_detect(mock_xeon_e5_2660_proc_cpuinfo)

    assert status == Status.OK
    assert description.startswith("Ok")


@pytest.mark.usefixtures("mock_timestamp")
def test_ok_if_kernel_is_not_supported(patch, manifest):
    reason = "Unsupported because we can"
    def raiser():
        raise walle_cpu_capping.UnsupportedKernel(reason)
    patch("assert_kernel_is_supported", raiser)

    expected_metadata = {
        "timestamp": mocked_timestamp(),
        "reason": "This kernel doesn\'t support capping detection: {}".format(reason),
    }
    event = manifest.execute(WALLE_CPU_CAPPING)
    assert event == expected_result(Status.OK, expected_metadata)


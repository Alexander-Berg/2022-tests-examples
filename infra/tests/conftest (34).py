# -*- coding: utf-8 -*-
# from __future__ import absolute_import

import pytest
import mock
from checks.entry_point import create_manifest


@pytest.fixture
def manifest():
    return create_manifest()


@pytest.fixture
def mock_xeon_e5_2660_proc_cpuinfo():
    def mock_fn(path, *args):
        files = {
            '/proc/cpuinfo': mock.mock_open(read_data=
"""
processor       : 0
vendor_id       : GenuineIntel
cpu family      : 6
model           : 45
model name      : Intel(R) Xeon(R) CPU E5-2660 0 @ 2.20GHz
stepping        : 7
microcode       : 0x710
cpu MHz         : 2689.299
cache size      : 20480 KB
physical id     : 0
siblings        : 16
core id         : 0
cpu cores       : 8
apicid          : 0
initial apicid  : 0
fpu             : yes
fpu_exception   : yes
cpuid level     : 13
wp              : yes
flags           : fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc arch_perfmon pebs bts rep_good nopl xtopology nonstop_tsc cpuid aperfmperf pni pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2 ssse3 cx16 xtpr pdcm pcid dca sse4_1 sse4_2 x2apic popcnt tsc_deadline_timer aes xsave avx lahf_lm epb tpr_shadow vnmi flexpriority ept vpid xsaveopt dtherm ida arat pln pts
bugs            : cpu_meltdown spectre_v1 spectre_v2 spec_store_bypass l1tf mds
bogomips        : 4400.08
clflush size    : 64
cache_alignment : 64
address sizes   : 46 bits physical, 48 bits virtual
power management:

""")
        }
        return files[path](path, *args)

    return mock_fn

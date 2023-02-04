from infra.rtc.nodeinfo.lib.modules import mem

TEST_1 = """MemTotal:       131988864 kB
MemFree:         9781300 kB
MemAvailable:   82058336 kB
MemKernel:       3045676 kB
Buffers:          431136 kB
Cached:         99673540 kB
SwapCached:            0 kB
Active:         70291108 kB
Inactive:       19327224 kB
Active(anon):   17335144 kB
Inactive(anon):   267752 kB
Active(file):   52955964 kB
Inactive(file): 19059472 kB
Unevictable:    29543064 kB
Mlocked:        29543064 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Dirty:            155388 kB
Writeback:             0 kB
AnonPages:      19057212 kB
Mapped:         71465404 kB
Shmem:            509500 kB
Slab:            1705092 kB
SReclaimable:    1316084 kB
SUnreclaim:       389008 kB
KernelStack:       40384 kB
PageTables:       526320 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    65994432 kB
Committed_AS:   147619416 kB
VmallocTotal:   34359738367 kB
VmallocUsed:           0 kB
VmallocChunk:          0 kB
Percpu:           153088 kB
HardwareCorrupted:     0 kB
AnonHugePages:         0 kB
ShmemHugePages:        0 kB
ShmemPmdMapped:        0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
Hugetlb:               0 kB
DirectMap4k:     3215264 kB
DirectMap2M:    97411072 kB
DirectMap1G:    35651584 kB
"""

TEST_2 = """MemTotal:       528269412 kB
MemFree:        457871708 kB
MemAvailable:   458467780 kB
Buffers:          503316 kB
Cached:          3154368 kB
SwapCached:            0 kB
Active:         67308356 kB
Inactive:         551576 kB
Active(anon):   64420044 kB
Inactive(anon):   144076 kB
Active(file):    2888312 kB
Inactive(file):   407500 kB
Unevictable:       76708 kB
Mlocked:           76708 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Dirty:               716 kB
Writeback:             0 kB
AnonPages:      64279620 kB
Mapped:           270272 kB
Shmem:            330860 kB
Slab:             455148 kB
SReclaimable:     209928 kB
SUnreclaim:       245220 kB
KernelStack:       19200 kB
PageTables:       447492 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    264134704 kB
Committed_AS:    8103884 kB
VmallocTotal:   34359738367 kB
VmallocUsed:           0 kB
VmallocChunk:          0 kB
HardwareCorrupted:     0 kB
AnonHugePages:         0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
DirectMap4k:     3997652 kB
DirectMap2M:    319889408 kB
DirectMap1G:    214958080 kB
"""


def test_mem_info():
    # Test read failed
    mi, err = mem.mem_info(path='/should_not_exists')
    assert err is not None
    # Test parse failed
    with open('test-mem-info', 'w') as f:
        f.write('GARBAGE')
    mi, err = mem.mem_info(path='test-mem-info')
    assert err is None
    assert mi.total_bytes == 0


def test_mem_info_from_buf():
    mi, err = mem.mem_info_from_buf(TEST_1)
    assert err is None
    assert mi.total_bytes == 135156596736
    mi, err = mem.mem_info_from_buf(TEST_2)
    assert err is None
    assert mi.total_bytes == 540947877888

import pytest
import os
import kern

KB = 2**10
MB = 2**20


def link_memory_cgroup_writeback(memcg, blkcg):
    if kern.subsys_bound('memory', 'blkio'):
        return
    if 'writeback_blkio' not in memcg:
        pytest.xfail('no memory.writeback_blkio')
        return
    assert memcg.read_attr('memory.writeback_blkio') == "/\n"
    blkcg_fd = os.open(blkcg.path, os.O_RDONLY)
    try:
        memcg['writeback_blkio'] = blkcg_fd
    finally:
        os.close(blkcg_fd)
    assert memcg.read_attr('memory.writeback_blkio') == blkcg.name + "\n"


@pytest.fixture(params=['nullb', 'dm'])
def target_disk(request):
    backend = None
    if request.param == 'nullb':
        disk = kern.NullBlkDisk(size_MiB=256, memory_backed=1, discard=1)
    elif request.param == 'dm':
        if 'memory_backed' in kern.NullBlkDisk.features():
            backend = kern.NullBlkDisk(size_MiB=256, memory_backed=1, discard=1)
        else:
            backend = kern.RamDisk(size_MiB=256)
        disk = kern.DmLinearDisk(backend=backend)

    disk.write_attr('queue/max_sectors_kb', 64)

    yield disk

    disk.destroy()
    if backend is not None:
        backend.destroy()


@pytest.mark.parametrize('bps_limit,fio_bs,fio_rw,fio_args', [
    pytest.param(1*MB, '64k', 'randread',  {'fadvise_hint': 1}, id="1MBps-64k-randread"),
    pytest.param(1*MB, '64k', 'randwrite', {'fdatasync': 1}, id="1MBps-64k-randwrite-fdatasync"),
    pytest.param(1*MB, '64k', 'randread',  {'direct': 1},    id="1MBps-64k-randread-direct"),
    pytest.param(1*MB, '64k', 'randwrite', {'direct': 1},    id="1MBps-64k-randwrite-direct"),

    # disable readahead
    pytest.param(1*MB, '64k', 'read',  {'fadvise_hint': 'random'},  id="1MBps-64k-read"),

    pytest.param(1*MB, '64k', 'write', {'fdatasync': 1},     id="1MBps-64k-write-fdatasync"),
    pytest.param(1*MB, '64k', 'read',  {'direct': 1},        id="1MBps-64k-read-direct"),
    pytest.param(1*MB, '64k', 'write', {'direct': 1},        id="1MBps-64k-write-direct"),

    pytest.param(1*MB, '4m', 'read',  {'fadvise_hint': 0},  id="1MBps-4m-read"),
    pytest.param(1*MB, '4m', 'write', {'fdatasync': 1},     id="1MBps-4m-write-fdatasync"),
    pytest.param(1*MB, '4m', 'read',  {'direct': 1},        id="1MBps-4m-read-direct"),
    pytest.param(1*MB, '4m', 'write', {'direct': 1},        id="1MBps-4m-write-direct"),

    pytest.param(1*MB, '64k', 'randwrite', {'sync_file_range': 'write,wait_after:1'},    id="1MBps-64k-randwrite-sfr"),
    pytest.param(1*MB, '64k', 'write', {'sync_file_range': 'write,wait_after:1'},        id="1MBps-64k-write-sfr"),
    pytest.param(1*MB, '4m', 'write', {'sync_file_range': 'write,wait_after:1'},        id="1MBps-4m-write-sfr"),
])
@pytest.mark.xfail(not kern.kernel_in('4.9'), reason='broken', run=False)
def test_cgroup_blkio_throttler(sysctl, make_cgroup, target_disk, make_fs, make_fio, bps_limit, fio_bs, fio_rw, fio_args):
    majmin = target_disk.majmin
    fs = make_fs(target_disk)

    blkcg = make_cgroup('blkio')
    cgroups = [blkcg]

    if fio_rw == 'read' or fio_rw == 'randread':
        blkcg['throttle.read_bps_device', majmin] = bps_limit
        blkio_stat = 'Read'
        fio_stat = 'read'
    else:
        blkcg['throttle.write_bps_device', majmin] = bps_limit
        blkio_stat = 'Write'
        fio_stat = 'write'
        if 'fdatasync' in fio_args:
            memcg = make_cgroup('memory')
            link_memory_cgroup_writeback(memcg, blkcg)
            cgroups.append(memcg)
            # new inode for new memcg wbc
            fio_args['filename'] = 'test.0.1'
            fio_args['unlink'] = 1
        elif 'sync_file_range' in fio_args:
            if not 'fs.sync-file-range-blkio' in sysctl:
                pytest.skip("Missing 'fs.sync-file-range-blkio' sysctl")
            sysctl["fs.sync-file-range-blkio"] = 1

    blkcg.delta('throttle.io_service_bytes', majmin, blkio_stat)

    fio = make_fio(cgroups=cgroups, directory=fs, size='100M', rw=fio_rw, bs=fio_bs, runtime=10, time_based=1, **fio_args)

    ret = fio.run()

    stat = blkcg.delta('throttle.io_service_bytes', majmin, blkio_stat)

    bps = ret[fio_stat]['bw'] * KB
    assert bps == pytest.approx(bps_limit, rel=0.1)

    # https://github.com/axboe/fio/issues/378
    bytes = ret[fio_stat]['io_bytes'] * (KB if 'io_kbytes' not in ret[fio_stat] else 1)
    assert bytes == pytest.approx(stat, rel=0.1)


@pytest.mark.parametrize('iops_limit,fio_bs,fio_rw,fio_args', [
    pytest.param(50, '4k', 'randread',  {'fadvise_hint': 1}, id="50iops-6k-randread"),
    pytest.param(50, '4k', 'randwrite', {'fdatasync': 1}, id="50iops-4k-randwrite-fdatasync"),
    pytest.param(50, '4k', 'randread',  {'direct': 1},    id="50iops-4k-randread-direct"),
    pytest.param(50, '4k', 'randwrite', {'direct': 1},    id="50iops-4k-randwrite-direct"),

    pytest.param(50, '4k', 'read',  {'fadvise_hint': 'random'},  id="50iops-4k-read"),
    pytest.param(50, '4k', 'write', {'fdatasync': 1},     id="50iops-4k-write-fdatasync"),
    pytest.param(50, '4k', 'read',  {'direct': 1},        id="50iops-4k-read-direct"),
    pytest.param(50, '4k', 'write', {'direct': 1},        id="50iops-4k-write-direct"),

    pytest.param(50, '4k', 'write', {'sync_file_range': 'write,wait_after:1'},     id="50iops-4k-write-sfr"),
    pytest.param(50, '4k', 'randwrite', {'sync_file_range': 'write,wait_after:1'}, id="50iops-4k-randwrite-sfr"),

    # Broken
    # pytest.param(10, '4k', 'read',  {'direct':1},        id="10iops-4k-read-direct"),
    # pytest.param(10, '4k', 'write', {'direct':1},        id="10iops-4k-write-direct"),
])
@pytest.mark.xfail(not kern.kernel_in('4.9'), reason='broken', run=False)
def test_cgroup_iops_throttler(sysctl, make_cgroup, target_disk, make_fs, make_fio, iops_limit, fio_bs, fio_rw, fio_args):
    majmin = target_disk.majmin
    fs = make_fs(target_disk)

    blkcg = make_cgroup('blkio')
    cgroups = [blkcg]

    if fio_rw == 'read' or fio_rw == 'randread':
        blkcg['throttle.read_iops_device', majmin] = iops_limit
        blkio_stat = 'Read'
        fio_stat = 'read'
    else:
        blkcg['throttle.write_iops_device', majmin] = iops_limit
        blkio_stat = 'Write'
        fio_stat = 'write'
        if 'fdatasync' in fio_args:
            memcg = make_cgroup('memory')
            link_memory_cgroup_writeback(memcg, blkcg)
            cgroups.append(memcg)
            # new inode for new memcg wbc
            fio_args['filename'] = 'test.0.1'
            fio_args['unlink'] = 1
        elif 'sync_file_range' in fio_args:
            if not 'fs.sync-file-range-blkio' in sysctl:
                pytest.skip("Missing 'fs.sync-file-range-blkio' sysctl")
            sysctl["fs.sync-file-range-blkio"] = 1

    blkcg.delta('throttle.io_serviced', majmin, blkio_stat)

    fio = make_fio(cgroups=cgroups, directory=fs, size='100M', rw=fio_rw, bs=fio_bs, runtime=10, time_based=1, **fio_args)

    ret = fio.run()

    stat = blkcg.delta('throttle.io_serviced', majmin, blkio_stat)

    iops = ret[fio_stat]['iops']
    ios = ret[fio_stat]['total_ios']

    # print('iops', iops, 'of', iops_limit, 'ios', ios, 'stat', stat)

    assert iops == pytest.approx(iops_limit, rel=0.15)
    assert ios == pytest.approx(stat, rel=0.1)


@pytest.mark.parametrize('iops_limit,thinktime,throttled_time', [
    pytest.param(10, '200ms', 0),
    pytest.param(10, '110ms', 0),
    pytest.param(10, '100ms', 0),
    pytest.param(10, '90ms', 0.01),
    pytest.param(10, '50ms', 0.05),
    pytest.param(10, '10ms', 0.09),
])
@pytest.mark.xfail(not kern.kernel_in('4.9'), reason='broken', run=False)
def test_throttled_time(make_cgroup, make_temp_file, make_fio, iops_limit, thinktime, throttled_time):
    blkcg = make_cgroup('blkio')
    if not blkcg.has('blkio.throttle.io_throttled_time'):
        pytest.xfail('no blkio.throttle.io_throttled_time')

    tmp = make_temp_file()
    disk = kern.Disk(path=tmp.name)

    blkcg['throttle.read_iops_device', disk.majmin] = iops_limit

    fio = make_fio(filename=tmp.name, filesize='40k', bs='4k', direct=1, rw='read', thinktime=thinktime, number_ios=10, cgroups=[blkcg])

    blkcg.delta('blkio.throttle.io_throttled_time', disk.majmin, 'Total')
    fio.run()
    ns = blkcg.delta('blkio.throttle.io_throttled_time', disk.majmin, 'Total') / 1e9 / 10

    assert ns == pytest.approx(throttled_time, rel=0.1, abs=0.01)

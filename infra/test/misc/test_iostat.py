import pytest
import os
import fcntl
import math
import logging
import kern

KB = 1 << 10
MB = 1 << 20
NS = 10 ** 9


@pytest.fixture(scope='module', params=['raw', 'ext4'])
def target_fstype(request):
    return request.param


@pytest.fixture(scope='module')
def target_disk(request, target_fstype):
    disk = kern.make_temp_disk(kind='sd', size='256MiB', lbpu=1, delay=0)
    disk.write_attr('queue/max_sectors_kb', 128)

    # block udev readpart https://systemd-devel.freedesktop.narkive.com/ayBK4RgP/how-to-stop-systemd-udevd-reading-a-device-after-dd
    disk_fd = open(disk.dev_path)
    fcntl.flock(disk_fd, fcntl.LOCK_EX)

    yield disk

    disk_fd.close()

    disk.destroy()


@pytest.fixture(scope='module')
def target_file(target_disk, target_fstype):
    if target_fstype == 'raw':
        yield target_disk.dev_path
    else:
        target_disk.mkfs(fs_type=target_fstype, mkfs_opts=['-E', 'lazy_itable_init=0,lazy_journal_init=0'])
        target_disk.mount(fs_opts=['barrier'])
        path = target_disk.fs_path + '/file'

        try:
            with open(path, 'wb') as f:
                for i in range(1024):
                    f.write(b'*' * (32 * KB))
                f.flush()
                os.fsync(f.fileno())
        except:
            target_disk.umount()
            raise

        yield path
        target_disk.umount()


def disk_ios(disk, rw, bs):
    if rw == 'trim':
        max_bs = disk.int_attr('queue/discard_max_bytes')
    else:
        max_bs = disk.int_attr('queue/max_sectors_kb') * KB

    if max_bs == 0:
        return 0

    return (bs + max_bs - 1) // max_bs


@pytest.mark.parametrize('bs', [1, 512, 4 * KB, 64 * KB, MB, 32 * MB],
                         ids=['1b', '512b', '4k', '64k', '1M', '32M'])
@pytest.mark.parametrize('mode', ['cached', 'direct'])
def test_disk_stat_read(make_fio, logger,
                        target_disk, target_fstype, target_file,
                        mode, bs):
    fio = make_fio(filename=target_file,
                   filesize=bs,
                   bs=bs,
                   number_ios=1,
                   direct=1 if mode == 'direct' else 0,
                   rw='read',
                   fadvise_hint=0 if mode == 'direct' else 'random')

    vmstat_before = kern.proc_vmstat()
    stat = target_disk.stat()
    fio.run()
    stat = target_disk.stat() - stat
    vmstat_after = kern.proc_vmstat()
    vmstat_diff = {key: vmstat_after[key] - vmstat_before[key] for key in vmstat_before}

    hw_sector_size = target_disk.int_attr('queue/hw_sector_size')
    max_request_size = target_disk.int_attr('queue/max_sectors_kb') * 1024

    read_ios = math.ceil(bs / max_request_size)
    read_ios_max = read_ios

    read_bytes = bs
    read_bytes_max = math.ceil(read_bytes / hw_sector_size) * hw_sector_size

    if mode == 'direct':
        if bs < 4096:
            read_bytes = read_ios = 0
    else:
        if bs < 4096:
            read_bytes = read_ios = 0  # invalidation may fail

        if target_fstype == 'raw':
            read_bytes = (read_bytes + 4095) // 4096 * 4096
            read_bytes_max = (read_bytes_max + 4095) // 4096 * 4096

    if target_fstype != 'raw':
        # read metadata
        read_bytes_max += KB
        read_ios_max += 1

    assert read_bytes <= stat.read_bytes <= read_bytes_max
    assert read_ios <= stat.read_ios <= read_ios_max

    assert stat.write_bytes == 0
    assert stat.write_ios == 0

    assert stat.discard_bytes == 0
    assert stat.discard_ios == 0

    assert stat.flush_ios == 0

    assert vmstat_diff['pgpgin'] >= read_bytes // 1024


@pytest.mark.parametrize('bs', [1, 512, 4 * KB, 64 * KB, MB, 32 * MB],
                         ids=['1b', '512b', '4k', '64k', '1M', '32M'])
@pytest.mark.parametrize('mode', ['sync', 'end_fsync', 'direct'])
def test_disk_stat_write(make_fio, logger,
                         target_disk, target_fstype, target_file,
                         mode, bs):
    hw_sector_size = target_disk.int_attr('queue/hw_sector_size')
    max_request_size = target_disk.int_attr('queue/max_sectors_kb') * 1024

    read_ios_max = read_ios = 0
    read_bytes_max = read_bytes = 0

    write_ios = math.ceil(bs / max_request_size)
    write_bytes = bs

    flush_ios = 0

    metadata_updates = 0

    if mode == 'direct':
        if bs % hw_sector_size != 0:
            pytest.skip('Unalligned O_DIRECT')
    else:
        if target_fstype == 'raw':
            if bs % 4096 != 0:
                read_ios_max += 1
                read_bytes_max += 4096

            write_bytes = math.ceil(write_bytes / 4096) * 4096

            if target_disk.kind == 'dm':
                write_ios = write_bytes / (4 * KB)  # no writepages?

        elif target_fstype == 'ext4':
            if bs < 4096:
                write_bytes = 1024  # FIXME

            metadata_updates += 10

        flush_ios += 1
        write_ios += 1

    if target_fstype != 'raw':
        # read metadata
        read_bytes_max += KB
        read_ios_max += 1
    else:
        read_ios_max += read_ios // 10

    fio = make_fio(filename=target_file,
                   filesize=bs,
                   bs=bs,
                   number_ios=1,
                   rw='randwrite',
                   **{mode: 1})

    vmstat_before = kern.proc_vmstat()
    stat = target_disk.stat()
    fio.run()
    stat = target_disk.stat() - stat
    vmstat_after = kern.proc_vmstat()
    vmstat_diff = {key: vmstat_after[key] - vmstat_before[key] for key in vmstat_before}

    logging.info("%s", stat.dump_json())

    assert read_bytes <= stat.read_bytes <= read_bytes_max
    assert read_ios <= stat.read_ios <= read_ios_max

    assert write_bytes <= stat.write_bytes <= write_bytes + metadata_updates * 512
    assert write_ios <= stat.write_ios <= write_ios + metadata_updates

    assert stat.discard_bytes == 0
    assert stat.discard_ios == 0

    if stat.has_flush:
        assert stat.flush_ios == flush_ios

    assert vmstat_diff['pgpgout'] >= write_bytes // 1024


@pytest.mark.parametrize('bs', [1, 512, 4 * KB, 64 * KB, MB, 32 * MB],
                         ids=['1b', '512b', '4k', '64k', '1M', '32M'])
def test_disk_stat_trim(make_fio, logger,
                        target_disk, target_fstype, target_file, bs):
    if target_fstype != 'raw':
        pytest.skip('Discard only for raw disk devices')

    discard_max_bytes = target_disk.int_attr('queue/discard_max_bytes')
    discard_granularity = target_disk.int_attr('queue/discard_granularity')

    if discard_max_bytes != 0:
        discard_bytes = bs - (bs % discard_granularity)
        discard_ios = math.ceil(discard_bytes / discard_max_bytes)
    else:
        discard_ios = 0
        discard_bytes = 0

    fio = make_fio(filename=target_file,
                   filesize=bs,
                   bs=bs,
                   number_ios=1,
                   rw='trim')

    stat = target_disk.stat()
    fio.run()
    stat = target_disk.stat() - stat

    assert stat.read_bytes == 0
    assert stat.read_ios == 0

    assert stat.write_bytes == 0
    assert stat.write_ios == 0

    assert stat.discard_bytes == discard_bytes
    assert stat.discard_ios == discard_ios

    assert stat.flush_ios == 0


@pytest.mark.parametrize('bs', [1, 512, 4 * KB, 64 * KB, MB, 32 * MB],
                         ids=['1b', '512b', '4k', '64k', '1M', '32M'])
@pytest.mark.parametrize('rw', ['read', 'write', 'trim'])
@pytest.mark.parametrize('direct', [0, 1], ids=['cached', 'direct'])
def test_blkio_throttler_stat(make_fio, make_cgroup,
                              target_disk, target_file, target_fstype,
                              bs, rw, direct):
    ios = disk_ios(target_disk, rw, bs)

    # https://st.yandex-team.ru/KERNEL-223
    if not kern.kernel_in('4.9') and ios > 1:
        pytest.xfail("iostat broken in in 4.4")

    if target_disk.kind == 'ram':
        pytest.xfail("ram iostat broken")

    if rw == 'trim' and (target_fstype != 'raw' or not direct):
        pytest.skip('trim have nothing to do with fs or cache')

    if direct and bs % target_disk.int_attr('queue/hw_sector_size') != 0:
        pytest.skip('direct-io not alligned to hw_sector_size')

    read_ios = ios if rw == 'read' else 0
    read_ios_max = read_ios

    read_bytes = bs if rw == 'read' else 0
    read_bytes_max = read_bytes

    write_ios = ios if rw == 'write' else 0
    write_bytes = bs if rw == 'write' else 0

    if not direct:
        if rw == 'read' and bs < 4096:
            read_bytes = read_ios = 0  # invalidation may fail

        if target_fstype == 'raw':
            if rw == 'write' and bs % 4096 != 0:
                read_ios_max += 1
                read_bytes_max += 4096

            if rw == 'read':
                read_bytes = (read_bytes + 4095) // 4096 * 4096
                read_bytes_max = (read_bytes_max + 4095) // 4096 * 4096

            if rw == 'write':
                write_bytes = (write_bytes + 4095) // 4096 * 4096

            if target_disk.kind == 'dm' and rw == 'write':
                write_ios = write_bytes / (4 * KB)  # no writepages?

        elif target_fstype == 'ext4':
            if rw == 'write' and bs < 4096:
                write_bytes = 1024  # FIXME

            # FIXME
            if rw == 'read':
                read_bytes_max = (read_bytes_max + 4095) // 4096 * 4096

        if rw == 'write':
            write_bytes = 0
            write_ios = 0
            ios = 1

    if target_fstype != 'raw':
        # read metadata
        read_bytes_max += KB
        read_ios_max += 1

    discard_ios = pytest.approx(ios, rel=0.1) if rw == 'trim' else 0
    discard_bytes = bs if (rw == 'trim' and target_disk.discard) else 0

    # discard were accounted as writes
    if rw == 'trim' and not kern.kernel_in('4.9'):
        write_ios, discard_ios = discard_ios, write_ios
        write_bytes, discard_bytes = discard_bytes, write_bytes

    majmin = target_disk.majmin

    blkcg = make_cgroup('blkio')
    fio = make_fio(cgroups=[blkcg],
                   filename=target_file,
                   direct=direct,
                   rw=rw,
                   bs=bs,
                   number_ios=1,
                   fdatasync=1 if not direct and rw == 'write' else 0,
                   fadvise_hint='random' if not direct and rw == 'read' else 0)

    blkcg.delta('blkio.throttle.io_serviced', majmin)
    blkcg.delta('blkio.throttle.io_service_bytes', majmin)
    fio.run()
    stat_ios = blkcg.delta('blkio.throttle.io_serviced', majmin)
    stat_bytes = blkcg.delta('blkio.throttle.io_service_bytes', majmin)

    assert read_bytes <= stat_bytes.get('Read', 0) <= read_bytes_max
    assert read_ios <= stat_ios.get('Read', 0) <= read_ios_max

    assert stat_bytes.get('Write', 0) == write_bytes
    assert stat_ios.get('Write', 0) == write_ios

    assert stat_bytes.get('Discard', 0) == discard_bytes
    assert stat_ios.get('Discard', 0) == discard_ios

    # assert stat_ios.get('Sync', 0) == ios
    # assert stat_ios.get('Async', 0) == 0
    # assert stat_ios.get('Total', 0) == ios


@pytest.mark.parametrize('size', [4 * KB, 64 * KB, MB, 32 * MB])
def test_memory_stat_written(make_temp_file, make_cgroup, make_fio, size):
    memcg = make_cgroup('memory')
    if not memcg.has('memory.stat', 'written'):
        pytest.xfail('no memory.stat written')

    f = make_temp_file()

    fio = make_fio(filename=f.name, size=size, io_size=size, rw='write', bs='4k', end_fsync=1, cgroups=[memcg])

    memcg.delta('memory.stat')
    fio.run()
    stat = memcg.delta('memory.stat')

    assert stat['dirtied'] == pytest.approx(size, abs=32 * 4 * KB)
    assert stat['written'] == pytest.approx(size, abs=32 * 4 * KB)

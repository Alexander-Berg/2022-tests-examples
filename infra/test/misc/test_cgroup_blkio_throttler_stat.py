import pytest
import kern

KB = 2**10
MB = 2**20


@pytest.fixture(params=['dm_striped'])
def target_disk(request, make_loopdev):
    backends = None
    if request.param == 'dm_striped':
        backends = [make_loopdev(size=256 * MB), make_loopdev(size=256 * MB)]
        disk = kern.DmStripedDisk(backends=backends)

    yield disk

    disk.destroy()


@pytest.mark.parametrize('bps_limit,fio_bs,fio_rw,fio_args', [
    pytest.param(1000*MB, '1m', 'read',  {},            id="1000MBps-1m-read"),
    pytest.param(1000*MB, '1m', 'read',  {'direct': 1}, id="1000MBps-1m-read-direct"),
    pytest.param(1000*MB, '1m', 'write', {'direct': 1}, id="1000MBps-1m-write-direct"),
])
@pytest.mark.xfail(not kern.kernel_in('5.4.197'), reason='broken')
def test_cgroup_blkio_throttler(sysctl, make_cgroup, target_disk, make_fs, make_fio, bps_limit, fio_bs, fio_rw, fio_args):
    file_size = '100M'

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

    blkcg.delta('throttle.io_service_bytes', majmin, blkio_stat)

    fio = make_fio(cgroups=cgroups, directory=fs, size=file_size, rw=fio_rw, bs=fio_bs, **fio_args)
    ret = fio.run()

    stat = blkcg.delta('throttle.io_service_bytes', majmin, blkio_stat)
    # https://github.com/axboe/fio/issues/378
    fio_bytes = ret[fio_stat]['io_bytes'] * (KB if 'io_kbytes' not in ret[fio_stat] else 1)

    assert fio_bytes == pytest.approx(stat, rel=0.1)

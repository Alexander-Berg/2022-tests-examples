import kern
import pytest


def run_fio(make_fio, iodepth, filename, disk, logger):
    fio = make_fio(filename=filename, ioengine='libaio', direct=1, rw='rw',
                   bs='4k', filesize='4k', size='4k', invalidate=0,
                   iodepth=iodepth,
                   iodepth_batch_submit=iodepth,
                   iodepth_batch_complete_min=0,
                   iodepth_batch_complete_max=iodepth,
                   time_based=1, runtime=10)

    nr_reqs_max = 0

    logger.debug('run_fio begins, iodepth={} filename={}'.format(iodepth, filename))

    fio.start()
    while not fio.done():
        iostats = disk.read_attr('stat').split()
        # According to "Documentation/iostats.txt" it is a number of requests currently in progress
        nr_reqs_max = max(nr_reqs_max, int(iostats[8]))

    logger.debug('run_fio done, iodepth={} filename={} nr_reqs_max={}'.format(iodepth, filename, nr_reqs_max))

    return nr_reqs_max


@pytest.mark.xfail(not kern.kernel_in("4.19.196-46", "5.4.129-17"), reason="aio over overlayfs broken")
def test_overlayfs_aio(make_sd_disk, make_fs, make_overlayfs, make_fio, logger):
    # Create disk with delay, so it possible to observe request queue with pending requests
    disk = make_sd_disk(size='128MiB', lbpu=1, delay=5)
    fs = make_fs(disk)
    ovl = make_overlayfs(disk, fs)
    fname = ovl + '/dummy.tmp'

    disk_iodepth_max = int(disk.read_attr('queue/nr_requests'))

    nr_reqs_iodepth_one = run_fio(make_fio, 1, fname, disk, logger)
    nr_reqs_iodepth_max = run_fio(make_fio, disk_iodepth_max, fname, disk, logger)

    # On broken overlayfs the nr_reqs_iodepth_one and nr_reqs_iodepth_max are about the same value.
    assert nr_reqs_iodepth_max > nr_reqs_iodepth_one * 2

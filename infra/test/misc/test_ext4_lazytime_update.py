import pytest
import kern


@pytest.mark.xfail(not kern.kernel_in("4.19.100-23", "5.4.38-2"), reason="not fixed")
def test_ext4_remount_nolazytime(make_ram_disk, make_fs, run):
    """
    https://st.yandex-team.ru/KERNEL-274
    """
    disk = make_ram_disk(size_GiB=1)
    fs = make_fs(disk, fs_type='ext4')

    assert 'lazytime' not in kern.Disk(path=fs).fs_opts
    run(['mount', '-o', 'remount,lazytime', fs])
    assert 'lazytime' in kern.Disk(path=fs).fs_opts

    run(['mount', '-o', 'remount,nolazytime', fs])
    assert 'lazytime' not in kern.Disk(path=fs).fs_opts


@pytest.mark.xfail(not kern.kernel_in("4.19.100-23", "5.4.38-2"), reason="not fixed")
def test_ext4_lazytime_update_contention(make_ram_disk, make_fs, make_fio, run):
    """
    https://st.yandex-team.ru/KERNEL-274
    """
    disk = make_ram_disk(size_GiB=1)
    fs = make_fs(disk, fs_type='ext4', mkfs_opts=['-O', '^has_journal'])
    fio = make_fio(ioengine='ftruncate', rw='randwrite', direct=1, bs='4k', filesize='16m',
                   time_based=1, runtime=10, numjobs=32, directory=fs)

    assert 'lazytime' not in kern.Disk(path=fs).fs_opts
    iops_nolazytime = fio.run()['write']['iops']

    run(['mount', '-o', 'remount,lazytime', fs])
    assert 'lazytime' in kern.Disk(path=fs).fs_opts
    iops_lazytime = fio.run()['write']['iops']

    print('iops_lazytime', iops_lazytime)
    print('iops_nolazytime', iops_nolazytime)
    assert iops_lazytime == pytest.approx(iops_nolazytime, rel=0.5)

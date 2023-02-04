import pytest
import os


@pytest.mark.parametrize('limit_in_bytes,disk_size,fuzz_only_mount,fsstress_opts', [
    pytest.param(0, '1024MiB', False, ['-p', '1', '-n', '10000'], id='1024mb'),
    pytest.param(0, '1024MiB', True, ['-p', '1', '-n', '10000'], id='1024mb-onlymount'),
    pytest.param(0, '256MiB', False, ['-p', '1', '-n', '10000'], id='256mb'),
    pytest.param(0, '256MiB', True, ['-p', '1', '-n', '10000'], id='256mb-onlymount'),
    pytest.param(100 << 20, '1024MiB', True, ['-p', '1', '-n', '10000'], id='1024mb-onlymount-memcg'),
])
def test_arc_fuse_fsstress(make_container, make_arc_repo, make_sd_disk, make_fs, find_bin, make_cgroup,
                           limit_in_bytes, disk_size, fuzz_only_mount, fsstress_opts, logger):
    arc_bin = find_bin('arc')
    fsstress_bin = find_bin('fsstress')

    disk = make_sd_disk(size=disk_size, lbpu=1, delay=0)
    fs = make_fs(disk, fs_type='ext4', mkfs_opts=['-q', '-I', '256', '-b', '1024'])

    mount = fs + '/mount'
    store = fs + '/store'

    os.mkdir(mount)
    os.mkdir(store)

    make_arc_repo(mount=mount, store=store)

    cgroups = []
    if limit_in_bytes > 0:
        cg = make_cgroup('memory')
        cg['limit_in_bytes'] = limit_in_bytes
        cgroups.append(cg)

    task = make_container(cgroups=cgroups)

    task.check_call([arc_bin, 'mount', '-m', mount, '-S', store])
    assert os.path.exists(mount + '/README')

    task.check_call([fsstress_bin, '-d', mount] + fsstress_opts)
    if not fuzz_only_mount:
        task.check_call([fsstress_bin, '-d', store] + fsstress_opts)

    task.check_call([arc_bin, 'unmount', mount])
    assert not os.path.exists(mount + '/README')

import kern
import os
import pytest


# KERNEL-538: test checks case when some directories with project quota contain,
# among other files, those that were opened and then unlinked from fs (i.e. such
# files exist until the process that opened them closes them and they aren't
# visible on fs, but takes up space) and we issue 'project_quota check' on some
# other directory - due to a bug in 'project_quota' tool, inode and space usages
# of all such directories get overwritten with usages that only account inodes
# and space from opened and unlinked files.

# FIXME: provide correct 4.19 release with fix (when it'll be released)
@pytest.mark.xfail(not kern.kernel_in("4.19.172-41", "5.4.90-7"), reason="broken")
def test_project_quota_unlinked(make_prj_quota, make_sd_disk, make_fs, logger):
    disk = make_sd_disk(size='256MiB', lbpu=1, delay=0)
    fs = make_fs(disk, fs_type='ext4', mkfs_opts=['-q', '-I', '256', '-b', '1024'])

    dir1_path = fs + "/dir1"
    dir2_path = fs + "/dir2"

    os.mkdir(dir1_path)
    os.mkdir(dir2_path)

    pq1_space_usage = 1024
    pq1_inodes_usage = 1

    pq1 = make_prj_quota(dir1_path)
    pq2 = make_prj_quota(dir2_path)

    file11_path = dir1_path + "/file11"
    file12_path = dir1_path + "/file12"

    file11_size = 5 * 1024 * 1024
    file12_size = 10 * 1024 * 1024

    with open(file11_path, "wb") as f:
        f.write(b'0' * file11_size)

    with open(file12_path, "wb") as f:
        f.write(b'0' * file12_size)

    pq1_space_usage += file11_size + file12_size
    pq1_inodes_usage += 2

    assert pq1.get_space_usage() == pq1_space_usage
    assert pq1.get_inodes_usage() == pq1_inodes_usage

    with open(file11_path, "r") as f:
        os.remove(file11_path)
        assert pq1.get_space_usage() == pq1_space_usage
        assert pq1.get_inodes_usage() == pq1_inodes_usage

        output = pq1.check()
        logger.info('pq1 check: %s', output)
        assert pq1.get_inodes_usage() == pq1_inodes_usage
        assert pq1.get_space_usage() == pq1_space_usage

        output = pq2.check()
        logger.info('pq2 check: %s', output)
        assert pq1.get_space_usage() == pq1_space_usage
        assert pq1.get_inodes_usage() == pq1_inodes_usage

import pytest
import kern
import os
from errno import EINVAL,EPERM  # noqa


@pytest.mark.xfail(not kern.kernel_in("4.19.84-18", "5.4.134-19.1", "5.4.148-24"), reason="not implemented")
@pytest.mark.parametrize('param,expected_param,expected_errno,expected_content', [
    pytest.param(None, "1", None, "0000000 4141 4141 4141 4141 4141 4141 4141 4141\n*\n0100000\n"),
    pytest.param("0", "0", None, "0000000 4141 4141 4141 4141 4141 4141 4141 4141\n*\n0100000\n"),
    pytest.param("1", "1", None, "0000000 4141 4141 4141 4141 4141 4141 4141 4141\n*\n0100000\n"),
    pytest.param("-1", "-1", EPERM, ""),
])
def test_ext4_falloc_hide_no_stale_secure(make_ram_disk, make_fs, make_file, sysfs, param, expected_param, expected_errno, expected_content):
    """
    https://st.yandex-team.ru/KERNEL-191
    Check security level control to FALLOC_FL_NO_HIDE_STALE
    """
    disk = make_ram_disk(size_GiB=1)
    # Initialize disk content with known content
    data = "A" * 1024 * 1024
    with open(disk.dev_path, 'w') as f:
        for i in range(1024):
            f.write(data)
            f.flush()
        os.fsync(f.fileno())

    fs = make_fs(disk, fs_type='ext4')
    fname = fs + "/test"
    f = make_file(fname)

    if param is not None:
        sysfs["/sys/module/ext4/parameters/secure_fallocate_no_hide_stale"] = param

    assert sysfs["/sys/module/ext4/parameters/secure_fallocate_no_hide_stale"] == expected_param

    try:
        kern.fallocate(f, kern.FALLOC_FL_NO_HIDE_STALE, 0, 1024 ** 2)
    except OSError as err:
        assert err.errno == expected_errno

    content = kern.run_output(["hexdump", fname])
    print(content)
    assert expected_content == content


@pytest.mark.xfail(not kern.kernel_in("4.19.84-18", "5.4.14-1"), reason="not implemented")
def test_ext4_falloc_hide_no_stale_secure2(make_ram_disk, make_fs, make_file, sysfs):
    """
    https://st.yandex-team.ru/KERNEL-191
    Check security level control to FALLOC_FL_NO_HIDE_STALE
    Same as test_ext4_falloc_hide_no_stale_secure but w/o sysfs options manipulation
    """
    disk = make_ram_disk(size_GiB=1)
    # Initialize disk content with known content
    data = "A" * 1024 * 1024
    with open(disk.dev_path, 'w') as f:
        for i in range(1024):
            f.write(data)
            f.flush()
        os.fsync(f.fileno())

    fs = make_fs(disk, fs_type='ext4')
    fname = fs + "/test"
    f = make_file(fname)
    kern.fallocate(f, kern.FALLOC_FL_NO_HIDE_STALE, 0, 1024 ** 2)

    content = kern.run_output(["hexdump", fname])
    assert "0000000 4141 4141 4141 4141 4141 4141 4141 4141\n*\n0100000\n" == content


@pytest.mark.skipif(not kern.kernel_in("4.19.17-2", "5.4.134-19.1", "5.4.148-24"), reason='not implemented')
@pytest.mark.parametrize('mode,start,length,expected_errno,expected_content', [
    pytest.param(0,  0, 1024 ** 2, None,
                 "0000000 0000 0000 0000 0000 0000 0000 0000 0000\n*\n0100000\n",
                 id="falloc_0_0_1M"),
    pytest.param(kern.FALLOC_FL_KEEP_SIZE, 0, 1024 ** 2, None,
                 "",
                 id="falloc_keepsz_0_1M"),
    pytest.param(kern.FALLOC_FL_KEEP_SIZE | kern.FALLOC_FL_NO_HIDE_STALE,
                 0, 1024 ** 2, EINVAL,
                 "",
                 id="falloc_keepsznohide_0_1M"),
    pytest.param(kern.FALLOC_FL_NO_HIDE_STALE, 0, 1024 ** 2, None,
                 "0000000 4141 4141 4141 4141 4141 4141 4141 4141\n*\n0100000\n",
                 id="falloc_nohide_0_1M"),
    pytest.param(kern.FALLOC_FL_NO_HIDE_STALE, 12345, 1024 ** 2, None,
                 """0000000 0000 0000 0000 0000 0000 0000 0000 0000
*
0003000 4141 4141 4141 4141 4141 4141 4141 4141
*
0103039
""",
                 id="falloc_nohide_12345_1M"),
])
def test_ext4_falloc_hide_no_stale_mode(make_ram_disk, make_fs, make_file, mode, start, length, expected_errno, expected_content):
    """
    https://st.yandex-team.ru/KERNEL-191
    Check falloc mode handling correctness
    """
    disk = make_ram_disk(size_GiB=1)
    # Initialize disk content with known content
    data = "A" * 1024 * 1024
    with open(disk.dev_path, 'w') as f:
        for i in range(1024):
            f.write(data)
            f.flush()
        os.fsync(f.fileno())

    fs = make_fs(disk, fs_type='ext4')
    fname = fs + "/test"
    f = make_file(fname)

    try:
        kern.fallocate(f, mode, start, length)
    except OSError as err:
        assert err.errno == expected_errno

    content = kern.run_output(["hexdump", fname])
    print(content)
    assert expected_content == content


@pytest.mark.skipif(not kern.kernel_in("4.19.17-2", "5.4.134-19.1", "5.4.148-24"), reason='not implemented')
def test_ext4_falloc_hide_no_stale_convert(make_ram_disk, make_fs, make_file):
    """
    https://st.yandex-team.ru/KERNEL-191
    Check FALLOC_FL_NO_HIDE_STALE convert
    """

    expected_content1 = "0000000 0000 0000 0000 0000 0000 0000 0000 0000\n*\n0100000\n"
    expected_content2 = """0000000 0000 0000 0000 0000 0000 0000 0000 0000
*
0003000 4141 4141 4141 4141 4141 4141 4141 4141
*
00fd000 0000 0000 0000 0000 0000 0000 0000 0000
*
0100000
"""

    disk = make_ram_disk(size_GiB=1)
    # Initialize disk content with known content
    data = "A" * 1024 * 1024
    with open(disk.dev_path, 'w') as f:
        for i in range(1024):
            f.write(data)
            f.flush()
        os.fsync(f.fileno())

    fs = make_fs(disk, fs_type='ext4')
    fname = fs + "/test"
    f = make_file(fname)

    kern.fallocate(f, 0 , 0, 1024 ** 2)
    content = kern.run_output(["hexdump", fname])
    print(content)
    assert expected_content1 == content

    kern.fallocate(f, kern.FALLOC_FL_NO_HIDE_STALE , 12345, 1024 ** 2 - 12345 *2)

    # Explicitly drop cache in order to clean inode's pagecache
    kern.set_sysctl("vm/drop_caches", 3)

    content = kern.run_output(["hexdump", fname])
    print(content)
    assert expected_content2 == content

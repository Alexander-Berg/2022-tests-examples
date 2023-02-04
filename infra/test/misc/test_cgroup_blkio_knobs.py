import pytest
from errno import EINVAL, ERANGE, E2BIG  # noqa
import kern

TEST_DEVICE = '7:0'   # loop0


@pytest.mark.parametrize('rate,expected_rate,expected_errno', [
    (0,         None,       0),
    (" 0 ",     None,       0),
    (1,         1,          0),
    (" 1 ",     1,          0),
    ("01",      1,          0),
    (123456789, 123456789,  0),
    (2**32-2,   2**32-2,    0),
    (2**32-1,   2**32-1,    0),
    (2**32,     2**32,      0),
    (2**32+1,   2**32+1,    0),
    (2**63-1,   2**63-1,    0),
    (2**63,     2**63,      0),
    (2**63+1,   2**63+1,    0),
    (2**64-2,   2**64-2,    0),
    (2**64-1,   None,       0),
    # (2**64,     None,       ERANGE),
    # (2**64+1,   None,       ERANGE),
    # (2**65,     None,       ERANGE),
    ("foo",     None,       EINVAL),
    ("max",     None,       EINVAL),
    (-1,        None,       EINVAL),
    (-2,        None,       EINVAL),
    (-(2**31),  None,       EINVAL),
    (-(2**63),  None,       EINVAL),
    # ("1M",      None,       EINVAL),
    # ("1.2",     None,       EINVAL),
    # ("1,2",     None,       EINVAL),
    # ("1 2",     None,       EINVAL),
    # ("1\t2",    None,       EINVAL),
    # ("1\n2",    None,       EINVAL),
    # ("0x1",     None,       EINVAL),
    # ("0xdeadbeef",     None,       EINVAL),
    pytest.param("a" * 1024,    None,   EINVAL,     id="a * 1024-EINVAL"),
    pytest.param("a" * 4096,    None,   E2BIG,      id="a * 4096-E2BIG"),
    pytest.param("a" * 65536,   None,   E2BIG,      id="a * 65536-E2BIG"),
    pytest.param("0" * 1024,    None,   0,          id="0 * 1024"),
    pytest.param("0" * 4096,    None,   E2BIG,      id="0 * 4096-E2BIG"),
    pytest.param("0" * 65536,   None,   E2BIG,      id="0 * 65536-E2BIG"),
])
@pytest.mark.parametrize('attr', [
    'blkio.throttle.read_bps_device',
    'blkio.throttle.write_bps_device'
])
@pytest.mark.xfail(not kern.kernel_in('4.9'), reason='broken', run=False)
def test_throttler_bps(make_cgroup, attr, rate, expected_rate, expected_errno):
    cg = make_cgroup('blkio')
    val = '{} {}'.format(TEST_DEVICE, rate)
    expected_val = '' if expected_rate is None else '{} {}\n'.format(TEST_DEVICE, expected_rate)
    try:
        cg.write_attr(attr, val)
        if expected_errno is None:
            assert cg.read_attr(attr) == ""
        else:
            assert cg.read_attr(attr) == expected_val
            assert 0 == expected_errno
    except OSError as err:
        assert err.errno == expected_errno
    assert cg.read_attr(attr) == expected_val


@pytest.mark.parametrize('rate,expected_rate,expected_errno', [
    (0,         None,       0),
    (" 0 ",     None,       0),
    (1,         1,          0),
    (" 1 ",     1,          0),
    ("01",      1,          0),
    (123456789, 123456789, 0),
    (2**32-2,   2**32-2,    0),
    (2**32-1,   None,       0),
    # (2**32,     None,       0),
    # (2**32+1,   None,       0),
    # (2**63-1,   None,       0),
    # (2**63,     None,       0),
    # (2**63+1,   None,       0),
    # (2**64-2,   None,       0),
    # (2**64-1,   None,       0),
    # (2**64,     None,       ERANGE),
    # (2**64+1,   None,       ERANGE),
    # (2**65,     None,       ERANGE),
    ("foo",     None,       EINVAL),
    ("max",     None,       EINVAL),
    (-1,        None,       EINVAL),
    (-2,        None,       EINVAL),
    (-(2**31),  None,       EINVAL),
    (-(2**63),  None,       EINVAL),
    # ("1M",      None,       EINVAL),
    # ("1.2",     None,       EINVAL),
    # ("1,2",     None,       EINVAL),
    # ("1 2",     None,       EINVAL),
    # ("1\t2",    None,       EINVAL),
    # ("1\n2",    None,       EINVAL),
    # ("0x1",     None,       EINVAL),
    # ("0xdeadbeef",     None,       EINVAL),
    pytest.param("a" * 1024,    None,   EINVAL,     id="a * 1024-EINVAL"),
    pytest.param("a" * 4096,    None,   E2BIG,      id="a * 4096-E2BIG"),
    pytest.param("a" * 65536,   None,   E2BIG,      id="a * 65536-E2BIG"),
    pytest.param("0" * 1024,    None,   0,          id="0 * 1024"),
    pytest.param("0" * 4096,    None,   E2BIG,      id="0 * 4096-E2BIG"),
    pytest.param("0" * 65536,   None,   E2BIG,      id="0 * 65536-E2BIG"),
])
@pytest.mark.parametrize('attr', [
    'blkio.throttle.read_iops_device',
    'blkio.throttle.write_iops_device'
])
@pytest.mark.xfail(not kern.kernel_in('4.9'), reason='broken', run=False)
def test_throttler_iops(make_cgroup, attr, rate, expected_rate, expected_errno):
    cg = make_cgroup('blkio')
    val = '{} {}'.format(TEST_DEVICE, rate)
    expected_val = '' if expected_rate is None else '{} {}\n'.format(TEST_DEVICE, expected_rate)
    try:
        cg.write_attr(attr, val)
        if expected_errno is None:
            assert cg.read_attr(attr) == ""
        else:
            assert cg.read_attr(attr) == expected_val
            assert 0 == expected_errno
    except OSError as err:
        assert err.errno == expected_errno
    assert cg.read_attr(attr) == expected_val

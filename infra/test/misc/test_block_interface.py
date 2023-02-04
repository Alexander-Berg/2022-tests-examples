import pytest
import kern


@pytest.fixture(params=['null', 'sd', 'sd-mq', 'dm'])
def target_disk(request):
    backend = None
    if request.param == 'null':
        disk = kern.NullBlkDisk(size="256MiB", memory_backed=1, discard=1)
    elif request.param == 'sd':
        disk = kern.ScsiDebugDisk(size='256MiB', lbpu=1, delay=0)
    elif request.param == 'sd-mq':
        disk = kern.ScsiDebugDisk(size='256MiB', lbpu=1, delay=0, submit_queues=8)
    elif request.param == 'dm':
        if 'memory_backed' in kern.NullBlkDisk.features():
            backend = kern.NullBlkDisk(size_MiB=256, memory_backed=1, discard=1)
        else:
            backend = kern.RamDisk(size="256MiB")
        disk = kern.DmLinearDisk(backend=backend)
    else:
        raise Exception("Unknown disk kind: " + request.param)

    disk.write_attr('queue/max_sectors_kb', 64)

    yield disk

    disk.destroy()
    if backend is not None:
        backend.destroy()


def expected_keys(keys, present):
    expected = {}
    for key, value, flags, releases in keys:
        if 'skip' in flags:
            if key in present:
                expected[key] = None
            continue

        assert isinstance(releases, tuple)
        if releases and not kern.kernel_in(*releases):
            continue
        expected[key] = value

    return expected


bdi_common = [
    ('max_ratio', "100", (), ()),
    ('min_ratio', "0", (), ()),
    ('read_ahead_kb', "128", (), ()),
    ('stable_pages_required', "0", (), ()),
    ('uevent', None, (), ()),
]


bdi_yandex = [
    ('io_request_kb', "64", (), ('4.19.107-25', '5.4')),
    ('write_behind_kb', None, (), ('4.19.44-5', '5.4')),  # https://st.yandex-team.ru/KERNEL-104
    ('async_write_behind_kb', None, (), ('4.4.94-43',)),  # commit 5f383e1 ("mm: implement write-behind policy for sequential file writes")
    ('min_write_behind_kb', None, (), ('4.4.94-43',)),  # commit 5f383e1 ("mm: implement write-behind policy for sequential file writes")
]


bdi_attrs = [bdi_common + bdi_yandex]


@pytest.mark.parametrize('attrs', bdi_attrs)
def test_bdi_attrs(attrs, target_disk, logger):
    bdi = target_disk.bdi
    present = bdi.attrs()

    expected = expected_keys(attrs, present)

    logger.debug("bdi: {}, attrs: {}".format(bdi.sysfs_path, present))
    for attr in expected:
        logger.debug("check {}".format(attr))
        if attr not in present:
            pytest.fail('bdi {} expected attribute {} not found'.format(bdi.sysfs_path, attr))

    for attr in present:
        if attr not in expected:
            pytest.fail('bdi {} unexpected attribute {} found'.format(bdi.sysfs_path, attr))
        if expected[attr] is not None:
            got = bdi.read_attr(attr).strip()
            if expected[attr] != got:
                pytest.fail('bdi {}/{} unexpected value found, want: {}, got: {}'.format(bdi.sysfs_path, attr, expected[attr], got))


@pytest.mark.xfail(not kern.kernel_in("4.19.17-2", "5.4.14-1"), reason="feature disabled")
def test_bfq_is_default_scheduler(target_disk, logger):
    """
    See commit 632ae43 block: for single queue devices use bfq by default
    """

    logger.debug("check kind:{}, scheduler: {}".format(target_disk.kind, target_disk.scheduler))

    if target_disk.kind in ["loop", "nvme", "nullb", "ram", "dm"] or target_disk.nr_hw_queues > 1:
        assert target_disk.scheduler == "none"
    else:
        # single queue devices should use bfq by default
        assert target_disk.scheduler == "bfq"


@pytest.mark.xfail(not kern.kernel_in("4.19.17-2"), reason="feature disabled")
def test_loopdev_is_norotational(make_loopdev, logger):
    """
    See commit f4a2789f6 ("block/loop: mark as non-rotational by default")
    """
    lo = make_loopdev(size=1 << 20)

    assert not lo.rotational

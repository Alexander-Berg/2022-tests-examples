import kern
import logging
import mmap
import os
import pytest
import subprocess

MB = 1024 ** 2
GB = 1024 ** 3


class MemoryOffliner():
    def __init__(self, size, logger=None):
        self.size = size
        self.logger = logger or logging.getLogger()

        self.data = None
        self.addr = None
        self.offlined_pfns = None

    def offline(self):
        self.map = mmap.mmap(-1, self.size, mmap.MAP_PRIVATE | mmap.MAP_ANONYMOUS, mmap.PROT_READ | mmap.PROT_WRITE)
        self.addr = kern.buffer_address(self.map)

        ret = kern.madvise(self.addr, self.size, kern.MADV_POPULATE)
        assert ret == 0

        self.offlined_pfns = [page.pfn for page in kern.PageMap.iter_vm_pages(os.getpid(), self.addr, self.size)]

        self.logger.info('soft_offline addr: {} size: {}'.format(self.addr, self.size))
        ret = kern.madvise(self.addr, self.size, kern.MADV_SOFT_OFFLINE)
        assert ret == 0

    def online(self):
        self.logger.info("unpoison offlined pages")
        kern.load_module('hwpoison_inject')

        with open('/sys/kernel/debug/hwpoison/unpoison-pfn', 'w') as fu:
            for pfn in self.offlined_pfns:
                fu.write(str(pfn))
                fu.flush()

        kern.remove_module('hwpoison_inject')


@pytest.fixture
def make_memory_offliner():
    offliners = []

    def _make_memory_offliner(size):
        offliner = MemoryOffliner(size)
        offliners.append(offliner)
        return offliner

    yield _make_memory_offliner

    for offliner in offliners:
        offliner.online()


@pytest.mark.xfail(not kern.kernel_in('4.19.119-30.2', '4.19.131-33', '5.4.14-1'), reason='not fixed')
def test_memory_compaction_poisoning(logger, make_memory_offliner, dmesg, check_kernel_taints):
    """
    https://st.yandex-team.ru/KERNEL-408
    Regression test for hwpoison vs compaction race
    """

    size = 256 * MB
    offliner = make_memory_offliner(size)

    offliner.offline()

    logger.info("trigger memory compaction")
    kern.set_sysctl('vm/compact_memory', 1)

    match = dmesg.match("sysdev: .* step onto hwpoisoned page", timeout=15)
    logger.info("dmesg matched kern WARN successfully: {}".format(match))

    try:
        match = dmesg.match("Killing .* due to hardware memory corruption fault at", timeout=10)
    except subprocess.TimeoutExpired:
        logger.info("all is good - no one was killed")
    else:
        raise Exception("unexpected kill due to hardware memory corruption fault, dmesg: {}".format(match))

    new_taints = kern.KernelTaints()
    diff = new_taints.diff(check_kernel_taints)
    # There are three warnings than can be triggered during this test, require at lease one.
    assert len(diff) == 1
    assert diff['W'] in [1, 2, 3]
    check_kernel_taints['W'] = new_taints['W']

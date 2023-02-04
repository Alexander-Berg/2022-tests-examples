import pytest
import os
import subprocess
import tempfile
import logging
import shutil
import fcntl
import distutils.dir_util

import sys

try:
    import infra.kernel.test.misc.kern as kern
    import infra.kernel.test.misc.util as util
except ImportError:
    from . import kern
    from . import util


class KernImporter(object):
    def find_module(self, fullname, path=None):
        return self if fullname == 'kern' else None

    def load_module(self, fullname):
        if fullname == 'kern':
            sys.modules['kern'] = kern
            return kern
        raise ImportError(fullname)


sys.meta_path.append(KernImporter())


def pytest_report_header(config):
    return ["kernel_release: {}".format(kern.kernel_release())]


def pytest_configure(config):
    config.addinivalue_line(
        "markers", "unprivileged: could be run as normal non-root user"
    )


def pytest_runtest_setup(item):
    #  FIXME
    #  if item.get_closest_marker('unprivileged') is None and os.getuid != 0:
    #      pytest.skip("privileged, requires root")
    pass


def pytest_addoption(parser):
    parser.addoption(
        "--tmpdir-chroot", action="store", default="", help="set tmpfile chroot"
    )


@pytest.fixture
def cmdopt_tmpdir_chroot(request):
    return request.config.getoption("--tmpdir-chroot")


@pytest.fixture(scope='session')
def logger(request):
    return logging.getLogger()


@pytest.fixture(scope='session')
def run(request, logger):
    def _run(args, check=True, stdin=subprocess.DEVNULL, **kwargs):
        logger.info("run '%s'", "' '".join(args))
        ret = subprocess.run(args, check=check, stdin=stdin, **kwargs)
        logger.info("run ".ljust(40, '-'))
        return ret
    return _run


@pytest.fixture(autouse=True)
def lock_perf(request):
    fd = os.open('/run/lock/perf.lock', os.O_RDWR | os.O_CREAT, mode=0o666)
    fcntl.lockf(fd, fcntl.LOCK_SH)

    def _lock_perf():
        fcntl.lockf(fd, fcntl.LOCK_UN)
        fcntl.lockf(fd, fcntl.LOCK_EX)

    yield _lock_perf

    fcntl.lockf(fd, fcntl.LOCK_UN)
    os.close(fd)


@pytest.fixture(scope='session')
def find_bin(request, logger):
    arc_path = {
        'arc': 'arc/local/bin/arc',
        'bpftool': 'infra/kernel/tools/bpftool/release/bpftool/bpftool',
        'fio': 'infra/kernel/tools/fio/release/fio/bin/fio',
        'fsstress': 'infra/kernel/tools/fsstress/fsstress',
        'perf': 'infra/kernel/tools/perf/release/perf',
        'stress-ng': 'infra/kernel/tools/stress-ng/release/stress-ng/stress-ng',
        'schbench': 'infra/kernel/test/schbench/schbench',
    }

    cache = {}

    def _find_bin(name):
        res = cache.get(name)
        if res:
            return res

        path = arc_path.get(name)
        if path:
            res = util.arcadia_build_path(path)
        if not res:
            res = shutil.which(name)
        if res:
            cache[name] = res
        logger.info('find_bin %s -> %s', name, res)
        return res

    return _find_bin


@pytest.fixture(scope='session')
def kernel_release(request):
    return kern.kernel_release()


@pytest.fixture(scope='session')
def lsb_codename(request):
    return subprocess.check_output(['lsb_release', '-sc']).decode('utf8').strip()


@pytest.fixture(autouse=True)
def check_kernel_taints():
    old = kern.KernelTaints()
    yield old
    assert kern.KernelTaints() == old


@pytest.fixture
def dmesg():
    with kern.Dmesg() as res:
        res.drain()
        yield res


@pytest.fixture(scope='session')
def kallsyms():
    return kern.Kallsyms()


@pytest.fixture
def disable_update_initramfs():
    conf = '/etc/initramfs-tools/update-initramfs.conf'
    conf_orig = conf + '.orig'
    with open(conf, 'r') as f:
        text = f.read()
    assert not os.path.exists(conf_orig)
    os.rename(conf, conf_orig)
    text += 'update_initramfs=no'
    with open(conf, 'w') as f:
        f.write(text)
    yield None
    os.rename(conf_orig, conf)


@pytest.fixture(scope="session")
def apt():
    yield kern.Apt()


@pytest.fixture(scope='session')
def kernel_extra_modules(apt, kernel_release):
    if not kern.module_exists('soundcore'):
        apt.install('linux-image-extra-' + kernel_release)


@pytest.fixture
def make_fio(request, find_bin):
    fios = []

    def _make_fio(*args, **kwargs):
        fio = kern.fio.FioJob(*args, fio_bin=find_bin('fio'), **kwargs)
        fios.append(fio)
        return fio

    yield _make_fio

    for fio in fios:
        fio.kill()


@pytest.fixture
def sysctl(request, logger):
    undo = []

    class Sysctl(object):
        def __contains__(self, key):
            ret = kern.has_sysctl(key)
            logger.info('sysctl {} -> {}found'.format(key, "" if ret else "not "))
            return ret

        def __getitem__(self, key):
            val = kern.get_sysctl(key)
            logger.info('sysctl {} -> {}'.format(key, val))
            return val

        def __setitem__(self, key, val):
            old = kern.get_sysctl(key)
            undo.insert(0, (key, old))
            logger.info('sysctl {} <- {} (was {})'.format(key, val, old))
            kern.set_sysctl(key, val)

    yield Sysctl()

    for key, val in undo:
        logger.info('sysctl {} <- {}'.format(key, val))
        kern.set_sysctl(key, val)


@pytest.fixture
def sysfs(request, logger):
    undo = []

    class Sysfs(object):
        def __contains__(self, path):
            ret = os.path.exists(path)
            logger.info('sysfs {} -> {}found'.format(path, "" if ret else "not "))
            return ret

        def __getitem__(self, path):
            val = kern.read_str(path)
            logger.info('sysfs {} -> {}'.format(path, val))
            return val

        def __setitem__(self, path, val):
            old = kern.read_str(path).rstrip()
            undo.insert(0, (path, old))
            logger.info('sysfs {} <- {} (was {})'.format(path, val, old))
            kern.write_str(path, val)

    yield Sysfs()

    for path, val in undo:
        if os.path.exists(path):
            logger.info('sysfs {} <- {}'.format(path, val))
            kern.write_str(path, val)


@pytest.fixture
def make_disk(request):
    disks = []

    def _make_disk(cls, **kwargs):
        disk = cls(**kwargs)
        disks.append(disk)
        return disk

    yield _make_disk

    for disk in reversed(disks):
        disk.destroy()


@pytest.fixture
def make_ram_disk(make_disk):
    def _make_ram_disk(**kwargs):
        return make_disk(kern.disk.RamDisk, **kwargs)
    return _make_ram_disk


@pytest.fixture
def make_null_blk(make_disk):
    def _make_null_blk(**kwargs):
        return make_disk(kern.disk.NullBlkDisk, **kwargs)
    return _make_null_blk


@pytest.fixture
def make_dm_dev(make_disk):
    def _make_dm_dev(**kwargs):
        backend = make_disk(kern.disk.RamDisk, **kwargs)
        return make_disk(kern.disk.DmLinearDisk, backend)
    yield _make_dm_dev


@pytest.fixture
def make_loopdev(make_temp_file, make_disk):
    def _make_loopdev(**kwargs):
        backend = make_temp_file(**kwargs)
        return make_disk(kern.disk.LoopdevDisk, backend=backend.name)
    yield _make_loopdev


@pytest.fixture
def make_sd_disk(make_disk):
    def _make_sd_disk(**kwargs):
        return make_disk(kern.disk.ScsiDebugDisk, **kwargs)
    return _make_sd_disk


@pytest.fixture
def make_fs(request):
    disks = []

    def _make_fs(disk, fs_type='ext4', mkfs_opts=[]):
        disk.mkfs(fs_type=fs_type, mkfs_opts=mkfs_opts)
        disk.mount()
        disks.append(disk)
        return disk.fs_path

    yield _make_fs

    for disk in disks:
        disk.umount()


@pytest.fixture
def make_overlayfs(request):
    def _make_overlayfs(disk, fs):
        upperdir = fs + '/ovl_upper'
        lowerdir = fs + '/ovl_lower'
        workdir = fs + '/ovl_work'
        path = fs + '/ovl_root'

        os.mkdir(upperdir)
        os.mkdir(lowerdir)
        os.mkdir(workdir)
        os.mkdir(path)

        opts = ['lowerdir={},upperdir={},workdir={}'.format(lowerdir, upperdir, workdir)]
        disk.mount_overlay(path=path, fs_opts=opts)
        return path

    return _make_overlayfs


@pytest.fixture
def make_file(request):
    files = []

    def _make_file(name, mode='x', text=None):
        f = open(name, mode=mode)
        if text is not None:
            f.write(text)
            f.flush()
        files.append(name)
        return f

    yield _make_file

    for name in files:
        os.unlink(name)


@pytest.fixture
def make_temp_file(cmdopt_tmpdir_chroot):
    def _make_temp_file(prefix='test-', dir='.', size=0, **kwargs):
        dir = os.path.join(cmdopt_tmpdir_chroot, dir)
        f = tempfile.NamedTemporaryFile(prefix=prefix, dir=dir, **kwargs)
        if size:
            f.truncate(size)
        return f
    return _make_temp_file


@pytest.fixture
def make_temp_dir(cmdopt_tmpdir_chroot):
    def _make_temp_dir(prefix='testdir-', dir='.', **kwargs):
        dir = os.path.join(cmdopt_tmpdir_chroot, dir)
        d = tempfile.TemporaryDirectory(prefix=prefix, dir=dir, **kwargs)
        return d
    return _make_temp_dir


@pytest.fixture()
def make_cgsubsys_check():
    checks = []

    def _make_cgsubsys_check(subsys=["blkio", "cpu", "cpuacct", "cpuset",
                                     "devices", "freezer", "hugetlb", "memory",
                                     "net_cls", "net_prio", "perf_event", "pids",
                                     "unified", "unified_dying"],
                                     delta=100):
        subsys_map = {}

        if "unified" in subsys and "unified_dying" not in subsys:
            subsys.append("unified_dying")

        for name in subsys:
            if kern.cgroup.subsys_enabled(name):
                subsys_map[name] = kern.cgroup.subsys_count(name)

        checks.append((subsys_map, delta))
        return subsys_map

    yield _make_cgsubsys_check

    for check in checks:
        for subsys, old_count in check[0].items():
            new_count = kern.cgroup.subsys_count(subsys)
            delta = check[1]

            if old_count + delta < new_count:
                pytest.fail("CGLEAK_CHECK: cgroups leakage detected: subsys name: '{}', count change: {} -> {} (diff = {}, allowed delta = {})".format(subsys, old_count, new_count,
                                                                                                                                                       new_count - old_count, delta))


@pytest.fixture
def make_cgroup():
    cgroups = []

    def _make_cgroup(subsys, parent=None, **cgroup_params):
        if parent is None:
            parent = kern.cgroup.root_cgroup(subsys)
        cg = parent.temp_child('test-')
        cg.update(cgroup_params)
        cgroups.append(cg)
        return cg

    yield _make_cgroup

    for cg in cgroups:
        cg.remove()


@pytest.fixture
def make_task():
    tasks = []

    def _make_task(task_cls=kern.task.Task, *args, cgroups=[], namespaces=[], **kwargs):
        task = task_cls(*args, **kwargs)
        tasks.append(task)
        cur_namespaces = []
        for ns in namespaces:
            cur_namespaces.append(kern.namespace.Namespace(ns.name))
            ns.enter()
        task.start()
        for ns in reversed(cur_namespaces):
            ns.enter()
        for cg in cgroups:
            cg.attach(task.pid)
        return task

    yield _make_task

    for task in reversed(tasks):
        task.stop()


@pytest.fixture
def make_container(make_cgroup, make_task):

    def _make_container(task_cls=kern.task.Task, cgroups=[], namespaces=[], **params):
        task_params = {}

        for key, val in params.items():
            if key.startswith('cg_'):
                cg_type = key[3:]
                if isinstance(val, (kern.cgroup.Cgroup, type(None))):
                    cg = val
                elif isinstance(val, dict):
                    cg = make_cgroup(cg_type, **val)
                if cg is not None:
                    cgroups.append(cg)
            elif key.startswith('ns_'):
                ns_type = key[3:]
                if isinstance(val, (kern.namespace.Namespace, type(None))):
                    ns = val
                else:
                    ns = kern.namespace.Namespace(ns_type, unshare=True)
                if ns is not None:
                    namespaces.append(ns)
            else:
                task_params[key] = val

        task = make_task(task_cls, cgroups=cgroups, namespaces=namespaces, **task_params)
        task.cgroups = cgroups
        task.namespaces = namespaces
        for cg in cgroups:
            setattr(task, 'cg_' + cg.subsys, cg)
        for ns in namespaces:
            setattr(task, 'ns_' + ns.name, ns)
        return task

    return _make_container


@pytest.fixture
def bpffs():
    with kern.Bpffs() as res:
        yield res


@pytest.fixture
def make_prj_quota():
    pqs = []

    def _make_prj_quota(dir_path, **kwargs):
        pq = kern.quota.PrjQuota(dir_path, **kwargs)
        pqs.append(pq)
        return pq

    yield _make_prj_quota

    for pq in pqs:
        try:
            pq.destroy()
        except Exception:
            # dirs with quota may be destroyed earlier than quota
            pass


@pytest.fixture
def make_arc_repo(make_temp_dir):
    tmpdirs = []

    def _make_arc_repo(mount=None, store=None, **kwargs):
        repo = {}

        if mount is None:
            tmpdirs.append(make_temp_dir(**kwargs))
            repo['mount'] = os.path.normpath(tmpdirs[-1].name)
        else:
            repo['mount'] = mount

        if store is None:
            tmpdirs.append(make_temp_dir(**kwargs))
            repo['store'] = os.path.normpath(tmpdirs[-1].name)
        else:
            repo['store'] = store

        distutils.dir_util.copy_tree(
            str(util.arcadia_source_path("arc/ci/tests/store")),
            repo['store'] + "/.arc",
            preserve_mode=0,
            preserve_times=0,
        )

        return repo

    yield _make_arc_repo

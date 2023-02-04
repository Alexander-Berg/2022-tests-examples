import tempfile
import os
import logging
import time


class Cgroup(object):
    def __init__(self, subsys, name, logger=None):
        assert os.path.isabs(name)
        assert os.path.normpath(name) == name

        if subsys == '':
            subsys = 'unified'

        self.subsys = subsys
        self.name = name
        self.root = subsys_path(subsys)
        self.path = self.root + ('' if name == "/" else self.name)
        self.legacy = subsys != 'unified'
        self.attr_prev = {}
        self.attr_cache = {}
        self.force_cleanup = False
        self.logger = logger or logging.getLogger(str(self))

    def removed(self):
        with open(os.path.join('/sys/kernel/debug/cgroup', self.subsys)) as f:
            return not any(self.name in line for line in f)

    def __getstate__(self):
        return (self.subsys, self.name)

    def __setstate__(self, state):
        subsys, name = state
        self.__init__(subsys, name)

    def __repr__(self):
        return '<Cgroup: {} {}>'.format(self.subsys, self.name)

    def __eq__(self, other):
        return self.subsys == other.subsys and self.name == other.name

    def __div__(self, name):
        return self.child(name)

    def parse_attr(self, attr, *keys):
        if isinstance(attr, tuple):
            attr, _keys = attr[0], attr[1:]
        elif ' ' in attr:
            attr = attr.split(' ')
            attr, _keys = attr[0], attr[1:]
        else:
            _keys = ()
        keys = tuple(_keys) + tuple(keys)
        if self.legacy and not attr.startswith(self.subsys + '.'):
            attr = self.subsys + '.' + attr
        return attr, keys

    def __getitem__(self, attr):
        attr, keys = self.parse_attr(attr)
        return self.get(attr, *keys)

    def __contains__(self, attr):
        attr, keys = self.parse_attr(attr)
        return self.has(attr, *keys)

    def __setitem__(self, attr, val):
        attr, keys = self.parse_attr(attr)
        if keys:
            val = ' '.join(keys) + ' ' + str(val)
        return self.set_attr(attr, val)

    def iteritems(self):
        for attr in self.attrs():
            try:
                val = self.get_attr(attr)
            except:
                val = None
            try:
                val = int(val)
            except:
                pass
            yield attr, val

    def update(self, config):
        for key, val in config.items():
            self[key] = val

    def is_root(self):
        return self.name == "/"

    def is_bound(self, other):
        return self.name == other.name and subsys_bound(self.subsys, other.subsys)

    def is_empty(self):
        return self.get_attr('cgroup.procs') == ''

    def depth(self):
        return 0 if self.is_root() else self.name.count('/')

    def child(self, name):
        assert not os.path.isabs(name)
        assert os.path.normpath(name) == name
        return Cgroup(self.subsys, os.path.join(self.name, name))

    def temp_child(self, prefix='temp-'):
        assert '/' not in prefix
        path = tempfile.mkdtemp(dir=self.path, prefix=prefix)
        self.logger.debug('temp_child -> %s', os.path.basename(path))
        return path_cgroup(self.subsys, path)

    def childs(self, prefix=''):
        for name in os.listdir(self.path):
            path = os.path.join(self.path, name)
            if name.startswith(prefix) and os.path.isdir(path):
                yield self.child(name)

    def walk(self):
        for path, _, _ in os.walk(self.path):
            assert path.startswith(self.path)
            yield Cgroup(self.subsys, path[len(self.root):])

    def exists(self):
        ret = os.path.isdir(self.path)
        self.logger.debug('exists -> %s', ret)
        return ret

    def get_inode(self):
        return os.stat(self.path).st_ino

    def create(self, mode=0o775):
        self.logger.debug('create')
        os.mkdir(self.path, mode)

    def remove(self):
        if not self.exists():
            return

        for child in self.childs():
            child.remove()

        if self.force_cleanup and self.processes():
            self.kill_all(wait=True)

        self.logger.debug('remove')
        os.rmdir(self.path)

    def rename(self, basename):
        assert not self.is_root()
        assert '/' not in basename
        name = os.path.join(os.path.dirname(self.name), basename)
        self.logger.debug('rename <- %s', basename)
        os.rename(self.path, self.root + name)
        self.name = name
        self.path = self.root + name

    def attrs(self, prefix=''):
        names = []
        for name in os.listdir(self.path):
            attr_path = os.path.join(self.path, name)
            if name.startswith(prefix) and os.path.isfile(attr_path):
                names.append(name)
        return names

    def attr_path(self, attr):
        return os.path.join(self.path, attr)

    def has_attr(self, attr):
        ret = os.path.isfile(self.attr_path(attr))
        self.logger.debug('has_attr %s -> %s', attr, ret)
        return ret

    def read_attr(self, attr):
        val = open(self.attr_path(attr), 'rb').read().decode('utf-8')
        self.logger.debug('read %s -> %s', attr, val.rstrip())
        return val

    def write_attr(self, attr, val):
        self.logger.debug('write %s <- %s', attr, val)
        open(self.attr_path(attr), 'wb', 0).write(val.encode('utf-8'))

    def enable_cache(self, attr=None):
        if attr is None:
            for attr in self.attrs():
                self.attr_cache[attr] = None
        elif attr not in self.attr_cache:
            self.attr_cache[attr] = None

    def disable_cache(self, attr=None):
        if attr is None:
            self.attr_cache.clear()
        else:
            del self.attr_cache[attr]

    def flush_cache(self, attr=None):
        if attr is None:
            for attr in self.attr_cache:
                self.attr_cache[attr] = None
        elif attr in self.attr_cache:
            self.attr_cache[attr] = None

    def update_cache(self, attr=None):
        if attr is None:
            for attr in self.attr_cache:
                self.attr_cache[attr] = self.read_attr(attr)
        elif attr in self.attr_cache:
            self.attr_cache[attr] = self.read_attr(attr)

    def get_attr(self, attr):
        if attr in self.attr_cache:
            val = self.attr_cache[attr]
            if val is None:
                val = self.read_attr(attr)
                self.attr_cache[attr] = val
            return val
        return self.read_attr(attr)

    def set_attr(self, attr, val):
        self.write_attr(attr, str(val))

    def get_lines(self, attr):
        return self.get_attr(attr).splitlines()

    def get_line_items(self, attr):
        return self.get_attr(attr).split()

    def get_int(self, attr):
        return int(self.get_attr(attr))

    def get_pids(self, attr):
        return [int(l) for l in self.get_lines(attr)]

    def get_percpus(self, attr):
        return [int(l) for l in self.get_line_items(attr)]

    def get_stat(self, attr, key):
        for line in self.get_lines(attr):
            k, v = line.split(None, 1)
            if k == key:
                return int(v)
        return None

    def get_stats(self, attr):
        stat = {}
        for line in self.get_lines(attr):
            k, v = line.split(None, 1)
            stat[k] = int(v)
        return stat

    def get_blkio_stats(self, attr, dev=None, op=None):
        stat = {}
        for line in self.get_lines(attr):
            w = line.split()
            if w[0] == 'Total':
                continue
            if dev is None:
                if len(w) == 2:
                    stat[w[0]] = int(w[-1])
                elif op is None:
                    stat[w[0], w[1]] = int(w[-1])
                elif op == w[1]:
                    stat[w[0]] = int(w[-1])
            elif dev == w[0]:
                if op is None:
                    stat[w[1]] = int(w[-1])
                elif op == w[1]:
                    return int(w[-1])
        return stat

    def has(self, attr, *keys):
        if not keys:
            return self.has_attr(attr)
        if self.subsys == 'blkio':
            return bool(self.get_blkio_stats(attr, *keys))
        return self.get_stat(attr, *keys) is not None

    def get(self, attr, *keys):
        if self.subsys == 'blkio':
            return self.get_blkio_stats(attr, *keys)
        if not keys:
            if attr == self.subsys + '.stat':
                return self.get_stats(attr)
            return self.get_int(attr)
        return self.get_stat(attr, *keys)

    def delta(self, attr, *keys):
        attr, keys = self.parse_attr(attr, *keys)
        curr = self.get(attr, *keys)
        prev = self.attr_prev.get((attr, keys))
        self.attr_prev[attr, keys] = curr
        if prev is None:
            return curr
        if isinstance(curr, dict):
            return {k: curr[k] - prev.get(k, 0) for k in curr}
        return curr - prev

    def processes(self):
        return self.get_pids('cgroup.procs')

    def attach(self, pid=0):
        self.set_attr('cgroup.procs', pid)

    def threads(self):
        return self.get_pids('tasks' if self.legacy else 'cgroup.threads')

    def attach_thread(self, pid=0):
        self.set_attr('tasks' if self.legacy else 'cgroup.threads', pid)

    def has_task(self, pid):
        return task_cgroup(pid, self.subsys) == self

    def kill_all(self, sig=9, wait=False):
        self.logger.debug('kill_all %s', sig)
        for pid in self.processes():
            os.kill(pid, sig)
        while wait and self.processes():
            time.sleep(0.1)

    def attach_all(self, source):
        self.logger.debug('attach_all %s', source)
        for pid in source.processes():
            self.attach(pid)


def subsys_path(subsys):
    if subsys == '':
        subsys = 'unified'
    if subsys.startswith('name='):
        subsys = subsys[5:]
    return os.path.join('/sys/fs/cgroup/', subsys)


def subsys_mounted(subsys):
    return os.path.ismount(os.path.realpath(subsys_path(subsys)))


def proc_cgroups():
    for line in open("/proc/cgroups"):
        if line[0] == '#':
            continue
        yield line.split(None, 3)

    unified_enabled = False
    unified_nr = 0
    if subsys_enabled("unified"):
        unified_enabled = True
        unified_nr = subsys_count("unified")

    yield "unified", 0, unified_nr, unified_enabled


def subsys_names():
    return [n for n, h, c, e in proc_cgroups()]


def subsys_bound(subsys, other):
    n2h = {n: h for n, h, c, e in proc_cgroups()}
    return n2h.get(subsys) == n2h.get(other)


def subsys_count(subsys):
    if subsys == "unified" or subsys == "unified_dying":
        if subsys_enabled(subsys):
            return root_cgroup("unified").get_stat("cgroup.stat", "nr_descendants" if subsys == "unified" else "nr_dying_descendants")
        return None

    for n, h, c, e in proc_cgroups():
        if n == subsys:
            return int(c)
    return None


def subsys_enabled(subsys):
    if subsys == "unified" or subsys == "unified_dying":
        return subsys_mounted("unified")

    for n, h, c, e in proc_cgroups():
        if n == subsys:
            return int(e) == 1
    return False


def root_cgroup(subsys):
    return Cgroup(subsys, "/")


def task_cgroups(pid):
    for line in open("/proc/{}/cgroup".format(pid), 'r'):
        _, ss, name = line.split(':', 2)
        for subsys in ss.split(','):
            yield Cgroup(subsys, name[:-1])


def task_cgroup(pid, subsys):
    if subsys == 'unified':
        subsys = ''
    for line in open("/proc/{}/cgroup".format(pid), 'r'):
        _, ss, name = line.split(':', 2)
        if ss == subsys or subsys in ss.split(','):
            return Cgroup(subsys, name[:-1])
    return None


def path_cgroup(subsys, path):
    assert os.path.isabs(path)
    assert os.path.normpath(path) == path
    root = subsys_path(subsys)
    assert path.startswith(root)
    if path == root:
        return Cgroup(subsys, "/")
    return Cgroup(subsys, path[len(root):])


def all_cgroups(subsys, prefix=''):
    root = subsys_path(subsys)
    prefix = root + prefix
    for path, _, _ in os.walk(root):
        if path.startswith(prefix):
            yield path_cgroup(subsys, path)

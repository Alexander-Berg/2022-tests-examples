#!/usr/bin/env python3

import tempfile
import subprocess
import os
import select
import shutil
import logging
import time
import json
import re
from .misc import run, run_output, read_int, read_int_list, read_str
from distutils.version import LooseVersion


def kernel_release():
    return read_str('/proc/sys/kernel/osrelease')


def kernel_in(*args, release=None):
    """
    kernel_in("x.y")   -> "x.y" <= major
    kernel_in("x.y.z")   -> "x.y" == major and "x.y.z" <= stable
    kernel_in("x.y.z-y") -> "x.y" == major and "x.y.z-y" <= release
    kernel_in((l, r))  -> l <= release < r
    kernel_in(a, b)    -> release in a or release in b
    """
    if release is None:
        release = kernel_release()
    v = LooseVersion(release)

    for arg in args:
        if isinstance(arg, tuple):
            l = LooseVersion(arg[0])
            r = LooseVersion(arg[1])
            if v >= r:
                continue
        else:
            l = LooseVersion(arg)
        if v < l:
            continue
        if len(l.version) > 2 and l.version[:2] != v.version[:2]:
            continue
        if len(l.version) > 3 and len(l.version) > len(v.version):
            continue
        return True

    return False


class KernelTaints(object):
    bits = "PFSRMBUDAWCIOELKXT" + '?' * 32
    info = "100000100010110101" + '1' * 32

    def __init__(self, strict=False):
        self.taints = {}

        nr_taints = read_int_list("/proc/sys/kernel/nr_taints")
        for i in range(len(nr_taints)):
            if not strict and KernelTaints.info[i] == '1':
                self.taints[KernelTaints.bits[i]] = 0
            else:
                self.taints[KernelTaints.bits[i]] = nr_taints[i]

    def __repr__(self):
        return '<KernelTaints: {}>'.format(self.taints)

    def __setitem__(self, key, value):
        assert key in self.taints
        self.taints[key] = value

    def __getitem__(self, key):
        return self.taints[key]

    def __eq__(self, other):
        return self.taints == other.taints

    def diff(self, other):
        d = {}
        for k in self.taints:
            if self.taints[k] != other.taints[k]:
                d[k] = self.taints[k] - other.taints[k]
        return d


def kernel_taints(strict=False):
    tainted = read_int('/proc/sys/kernel/tainted')
    ret = ''
    for i in range(len(KernelTaints.bits)):
        if not strict and KernelTaints.info[i] == '1':
            continue
        if tainted & (1 << i):
            ret += KernelTaints.bits[i]
    return ret


def module_exists(module):
    return subprocess.call(['/sbin/modinfo', module], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) == 0


def list_module_params(module):
    return [p.split(':')[0] for p in subprocess.check_output(['/sbin/modinfo', '-p', module]).splitlines()]


def module_loaded(module):
    return os.path.isdir('/sys/module/' + module)


def get_module_param(module, param):
    with open('/sys/module/' + module + '/parameters/' + param) as f:
        return f.read().rstrip()


def get_module_params(module):
    ret = {}
    base = '/sys/module/' + module + '/parameters'
    for p in os.listdir(base):
        with open(base + '/' + p) as f:
            ret[p] = f.read().rstrip()
    return ret


def set_module_param(module, param, value):
    with open('/sys/module/' + module + '/parameters/' + param, 'w') as f:
        logging.info('set module %s param %s=%s', module, param, value)
        f.write(str(value))


def loaded_modules():
    return os.listdir('/sys/module/')


def load_module(module, **kwargs):
    logging.info('modprobe %s %s', module, ' '.join(['{}={}'.format(k, v) for k, v in kwargs.items()]))
    subprocess.check_call(['/sbin/modprobe', module] + ['{}={}'.format(k, v) for k, v in kwargs.items()])


class ModuleLoader(object):
    def __init__(self, module, **kwargs):
        self.module = module
        self.args = ['{}={}'.format(k, v) for k, v in kwargs.items()]

    def __enter__(self):
        logging.info('modprobe %s %s', self.module, ' '.join(self.args))
        self.proc = subprocess.Popen(['/sbin/modprobe', self.module] + self.args)

    def __exit__(self, exc, value, tb):
        self.proc.kill()
        self.proc.wait()


def remove_module(module, timeout=5):
    logging.info('modprobe -r %s', module)
    deadline = time.time() + timeout
    while True:
        subprocess.run(['/sbin/modprobe', '-r', module], check=time.time() >= deadline, timeout=timeout)
        if not module_loaded(module):
            break
        time.sleep(1)


def build_tarball(build_dir=None, output_dir=None, arch=None, config=None, localversion=None, install_headers=False):
    make_args = ['make', '-s', '-j' + str(os.sysconf('SC_NPROCESSORS_ONLN'))]

    if build_dir is not None:
        make_args.append('-C')
        make_args.append(build_dir)
    else:
        build_dir = os.getcwd()

    if output_dir is not None:
        make_args.append('O=' + output_dir)

    if config is not None:
        make_args.append('KCONFIG_CONFIG=' + config)

    if arch is not None:
        make_args.append('ARCH=' + arch)

    if localversion is not None:
        make_args.append('LOCALVERSION=' + localversion)

    subprocess.check_call(make_args + ['oldconfig'])
    subprocess.check_call(make_args)

    install_dir = tempfile.mkdtemp(prefix='bin-', dir=output_dir or build_dir)

    subprocess.check_call(make_args + ['INSTALL_MOD_STRIP=1',
                                       'INSTALL_MOD_PATH=' + install_dir,
                                       'INSTALL_PATH=' + install_dir + '/boot',
                                       'modules_install', 'install'])

    shutil.copy(build_dir + '/vmlinux', install_dir + '/vmlinux')


class Dmesg(object):
    def __init__(self):
        self.proc = subprocess.Popen(['dmesg', '-rw'], stdout=subprocess.PIPE)
        self.backlog = b''
        os.set_blocking(self.proc.stdout.fileno(), False)

    def close(self):
        if self.proc is not None:
            self.proc.kill()
            self.proc.wait()
            self.proc = None

    def __enter__(self):
        return self

    def __exit__(self, exc, value, tb):
        self.close()

    def drain(self, timeout=0.1):
        self.backlog = b''
        try:
            while True:
                self.readline(timeout=timeout)
        except subprocess.TimeoutExpired:
            pass

    def readline(self, timeout=None):
        if timeout is not None:
            deadline = time.monotonic() + timeout
        while True:
            sep = self.backlog.find(b'\n')
            if sep != -1:
                ret = self.backlog[0:sep].decode('latin1')
                self.backlog = self.backlog[sep+1:]
                return ret
            out = self.proc.stdout.readline()
            if out:
                self.backlog += out
                continue
            timeout_left = deadline - time.monotonic() if timeout is not None else None
            if not select.select([self.proc.stdout], [], [], timeout_left)[0]:
                raise subprocess.TimeoutExpired('dmesg', timeout)

    def match(self, regex, timeout=None):
        pattern = re.compile(r'^<(?P<level>[0-9])>\[ *(?P<time>[0-9]+\.[0-9]+)\] ' + regex + '$')
        if timeout is not None:
            deadline = time.monotonic() + timeout
        while True:
            if timeout is not None:
                timeout = deadline - time.monotonic()
            line = self.readline(timeout=timeout)
            ret = pattern.match(line)
            if ret is not None:
                logging.debug("dmesg match %s", line)
                return ret
            logging.debug("dmesg skip %s", line)


class Kallsyms(object):
    def __init__(self, path='/proc/kallsyms'):
        self.addrs = {}
        for line in open(path):
            w = line.split()
            self.addrs[w[2]] = '0x' + w[0]

    def __getitem__(self, name):
        return self.addrs[name]


def Trace(object):
    def __init__(self):
        self.base = '/sys/kernel/tracing'
        if not os.path.ismount(self.base):
            subprocess.check_call(['/bin/mount', '-t', 'tracefs', 'tracefs', self.base])
        self.pipe = open(self.base + '/trace_pipe')
        os.set_blocking(self.proc.pipe, False)
        self.backlog = b''

    def readline(self, timeout=None):
        if timeout is not None:
            deadline = time.monotonic() + timeout
        while True:
            sep = self.backlog.find(b'\n')
            if sep != -1:
                ret = self.backlog[0:sep].decode('latin1')
                self.backlog = self.backlog[sep+1:]
                return ret
            out = self.pipe.readline()
            if out:
                self.backlog += out
                continue
            timeout_left = deadline - time.monotonic() if timeout is not None else None
            if not select.select([self.pipe], [], [], timeout_left)[0]:
                return None

    def reset(self):
        self.set('current_tracer', 'nop')
        self.set('enabled_functions', '')
        self.set('set_event', '')
        self.set('trace', '')

    def set(self, name, val):
        with open(self.base + '/' + name, 'w') as f:
            f.write(str(val))

    def add(self, name, val):
        with open(self.base + '/' + name, 'a') as f:
            f.write(str(val))

    def get(self, name):
        with open(self.base + '/' + name) as f:
            return f.read()

    def get_int(self, name):
        return int(self.get(name))

    def get_lines(self, name):
        return self.get(name).splitlines()


def perf_list(args=[], perf_bin='perf'):
    return run_output([perf_bin, 'list'] + args)


def perf_stat(args, cmd, perf_bin='perf', timeout=None):
    result = {}

    with tempfile.NamedTemporaryFile(mode='w+') as report:
        run([perf_bin, 'stat', '-x', ';', '-o', report.name] + args + ['--'] + cmd, check=True, timeout=timeout)

        for line in report.readlines():
            line = line.strip()
            if not line or line.startswith('#'):
                continue

            c = line.split(';')

            logging.info('perf stat: %s', ' '.join(c))

            if c[0] == '<not counted>':
                continue

            result[c[2]] = float(c[0]) if '.' in c[0] else int(c[0])
            if c[5]:
                result[c[6]] = float(c[5])

    return result


def run_bpftool(args=[], bpftool_bin='bpftool'):
    return json.loads(run_output([bpftool_bin, '-j'] + args))


class Bpffs(object):
    def __init__(self):
        self.base = '/sys/fs/bpf'
        if not os.path.ismount(self.base):
            subprocess.check_call(['/bin/mount', '-t', 'bpf', 'bpf', self.base])
        self.pinned_progs = []

    def __enter__(self):
        return self

    def unpin_progs(self):
        for prog in self.pinned_progs:
            os.remove(prog)

    def __exit__(self, exc, value, tb):
        self.unpin_progs()

    def load_prog(self, bpftool_bin, arc_obj):
        import yatest.common

        obj_path = yatest.common.source_path(arc_obj)
        pin_path = self.base + '/' + os.path.basename(os.path.splitext(arc_obj)[0])
        if not os.path.exists(pin_path):
            run_bpftool(['prog', 'load', obj_path, pin_path], bpftool_bin=bpftool_bin)
            self.pinned_progs.append(pin_path)
        return pin_path

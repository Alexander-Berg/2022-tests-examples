import os
import ctypes
from .misc import read_str, write_str, parse_bitmap_list


_libnuma = None
_libc = None
_MPOL_BIND = 1 << 1
_MPOL_F_NUMA_BALANCING = 1 << 13


def proc_meminfo():
    res = {}
    for line in open('/proc/meminfo'):
        w = line.split()
        if len(w) < 2:
            continue
        key = w[0][:-1]
        val = int(w[1])
        if w[-1] == 'kB':
            val *= 1024
        res[key] = val
    return res


def proc_vmstat():
    res = {}
    for line in open('/proc/vmstat'):
        w = line.split()
        if len(w) == 2:
            res[w[0]] = int(w[1])
    return res


def proc_stat():
    res = {}
    for line in open('/proc/stat'):
        w = line.split()
        res[w[0]] = [int(v) for v in w[1:]]
    return res


def task_mountinfo(pid='self'):
    res = []
    for line in open('/proc/{}/mountinfo'.format(pid)):
        m_id, p_id, fs_majmin, m_bind, m_path, tail = line.split(None, 5)
        m_opts, tail = tail.split(None, 1)
        m_tags, tail = tail.split(' - ', 1)
        fs_type, fs_dev, fs_opts = tail.split(None, 2)

        mnt_tags = {}
        for t in m_tags.split():
            if ':' in t:
                k, v = t.split(':', 1)
                mnt_tags[k] = int(v)
            else:
                mnt_tags[t] = True

        opts = {}
        for o in fs_opts.strip().split(','):
            if '=' in o:
                k, v = o.split('=', 1)
                opts[k] = v
            else:
                opts[o] = True

        res.append({
            'mnt_path': m_path,
            'mnt_id': int(m_id),
            'parent_id': int(p_id),
            'mnt_opts': m_opts.split(','),
            'mnt_tags': mnt_tags,
            'mnt_bind': m_bind,
            'type': fs_type,
            'dev':  fs_dev,
            'majmin': fs_majmin,
            'opts': opts
        })

    return res


def task_threads(pid='self'):
    return [int(p) for p in os.listdir('/proc/{}/task'.format(pid))]


def task_children(pid='self'):
    res = []
    for t in task_threads(pid):
        try:
            res.extend(map(int, open('/proc/{}/task/{}/children'.format(pid, t)).read().split()))
        except:
            pass
    return res


def all_tasks():
    return [int(p) for p in os.listdir('/proc') if p.isdigit()]


def all_threads():
    res = []
    for pid in all_tasks():
        try:
            res.extend(task_threads(pid))
        except:
            pass
    return res


def task_status(pid='self'):
    res = {}
    for line in open('/proc/{}/status'.format(pid)):
        if ':' not in line:
            continue
        k, v = line.split(':', 1)
        if v.endswith('kB\n'):
            res[k] = int(v[:-3]) * 1024
        elif k == 'Name':
            res[k] = v[1:-1]
        elif k == 'State':
            res[k] = v[1]
        elif k in ['Pid', 'PPid', 'Tgid', 'Ngid', 'TracerPid', 'Threads', 'FDSize', 'CoreDumping', 'NoNewPrivs', 'Seccomp', 'voluntary_ctxt_switches', 'nonvoluntary_ctxt_switches']:
            res[k] = int(v)
        elif k in ['Groups', 'NStgid', 'NSpid', 'NSpgid', 'NSsid']:
            res[k] = [int(i) for i in v.split()]
        elif k in ['Uid', 'Gid']:
            ids = [int(i) for i in v.split()]
            res['Real' + k] = ids[0]
            res['Effective' + k] = ids[1]
            res['Saved' + k] = ids[2]
            res['Fs' + k] = ids[3]
        elif k in ['SigPnd', 'ShdPnd', 'SigBlk', 'SigIgn', 'SigCgt']:
            mask = int(v, 16) * 2
            res[k] = [i for i in range(64) if mask & (1 << i) != 0]
        elif k in ['CapInh', 'CapPrm', 'CapEff', 'CapBnd', 'CapAmb']:
            mask = int(v, 16)
            res[k] = [i for i in range(64) if mask & (1 << i) != 0]
        elif k in ['Cpus_allowed_list', 'Mems_allowed_list']:
            res[k] = parse_bitmap_list(v)
        else:
            res[k] = v[1:-1]
    return res


def task_maps(pid='self', name='maps'):
    maps = []
    for line in open('/proc/{}/{}'.format(pid, name)):
        a, b = line.split(None, 1)
        if a[-1] != ':':
            a1, a2 = a.split('-')
            begin, end = int(a1, 16), int(a2, 16)
            c = b.split(None, 4)
            perms = c[0]
            offset = int(c[1], 16)
            d = c[2].split(':', 1)
            dev = os.makedev(int(d[0], 16), int(d[1], 16))
            inode = int(c[3])
            pathname = c[4][:-1] if len(c) > 4 else None
            maps.append({
                'begin': begin,
                'end': end,
                'perms': perms,
                'offset': offset,
                'dev': dev,
                'inode': inode,
                'pathname': pathname,
                'read': perms[0] == 'r',
                'write': perms[1] == 'w',
                'exec': perms[2] == 'x',
                'private': perms[3] == 'p',
                'shared': perms[3] == 's',
            })
        elif b.endswith(' kB\n'):
            maps[-1][a[:-1]] = int(b[:-4]) * 1024
        elif a == 'VmFlags:':
            maps[-1]['VmFlags'] = b.split()
    return maps


def task_smaps(pid='self'):
    return task_maps(pid, 'smaps')


def task_smaps_rollup(pid='self'):
    return task_maps(pid, 'smaps_rollup')


def task_read_attr(pid, attr):
    path = '/proc/{}/{}'.format(pid, attr)
    return read_str(path)


def task_write_attr(pid, attr, val):
    path = '/proc/{}/{}'.format(pid, attr)
    return write_str(path, val)


def list_cpus():
    return parse_bitmap_list(read_str('/sys/devices/system/cpu/online'))


def nr_cpus():
    return len(list_cpus())


def virtual_machine():
    vendor = read_str('/sys/class/dmi/id/sys_vendor')
    if vendor in ['QEMU', 'KVM']:
        return vendor
    return None


def list_numa_nodes():
    return parse_bitmap_list(read_str('/sys/devices/system/node/online'))


def nr_numa_nodes():
    return len(list_numa_nodes())


def numa_cpus(node):
    return parse_bitmap_list(read_str('/sys/devices/system/node/node{}/cpulist'.format(node)))


def membind(nodes):
    global _libnuma, _libc

    _libnuma = _libnuma or ctypes.CDLL('libnuma.so')
    mask = 0
    for n in nodes:
        mask |= 1 << n

    ret = _libnuma.set_mempolicy(
        ctypes.c_int(_MPOL_BIND | _MPOL_F_NUMA_BALANCING),
        ctypes.pointer(ctypes.c_ulong(mask)),
        ctypes.c_ulong(max(nodes) + 2))
    if ret != 0:
        _libc = _libc or ctypes.CDLL("libc.so.6")
        get_errno_loc = _libc.__errno_location
        get_errno_loc.restype = ctypes.POINTER(ctypes.c_int)
        e = get_errno_loc()[0]
        raise OSError("set_mempolicy failed: errno={}, mask={:b}, nodes={}".format(e, mask, nodes))


def has_sysctl(key):
    path = '/proc/sys/' + key.replace('.', '/')
    return os.path.exists(path)


def get_sysctl(key):
    path = '/proc/sys/' + key.replace('.', '/')
    return read_str(path)


def set_sysctl(key, val):
    path = '/proc/sys/' + key.replace('.', '/')
    return write_str(path, val)

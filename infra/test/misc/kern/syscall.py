import ctypes
from fcntl import fcntl  # noqa
import os


libc = ctypes.CDLL(None, use_errno=True)
c_off_t = ctypes.c_int64


def syscall(name, argtypes, check=False):
    func = getattr(libc, name)
    func.argtypes = argtypes

    def wrapper(*args):
        ret = func(*args)
        if ret == -1:
            err = ctypes.get_errno()
            if check:
                raise OSError(err, name + ': ' + os.strerror(err))
            return -err
        return ret
    return wrapper


sys_prctl = libc.prctl
sys_prctl.argtypes = [ctypes.c_int, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong]


def prctl(opt, arg2=0, arg3=0, arg4=0, arg5=0):
    ret = sys_prctl(opt, arg2, arg3, arg4, arg5)
    if ret == -1:
        ret = -ctypes.get_errno()
    return ret


PR_GET_DUMPABLE = 3
PR_SET_DUMPABLE = 4
PR_TRANSLATE_PID = 0x59410001
PR_SET_DUMPABLE_INIT_NS = 0x59410002

ptrace = syscall('ptrace', [ctypes.c_int, ctypes.c_int, ctypes.c_ulong, ctypes.c_ulong])

PTRACE_ATTACH = 16
PTRACE_DETACH = 17

CLONE_NEWNS = 0x00020000
CLONE_NEWCGROUP = 0x02000000
CLONE_NEWUTS = 0x04000000
CLONE_NEWIPC = 0x08000000
CLONE_NEWUSER = 0x10000000
CLONE_NEWPID = 0x20000000
CLONE_NEWNET = 0x40000000

unshare = syscall('unshare', [ctypes.c_int], check=True)
setns = syscall('setns', [ctypes.c_int, ctypes.c_int], check=True)

sys_fadvise = libc.posix_fadvise
sys_fadvise.argtypes = [ctypes.c_int, ctypes.c_int64, ctypes.c_int64, ctypes.c_int]


def fadvise(fd, offset, length, advice):
    ret = sys_fadvise(fd, offset, length, advice)
    if ret:
        err = ctypes.get_errno()
        raise OSError(ret, 'fadvise {}: {}'.format(advice, os.strerror(err)))
    return ret

FADV_NORMAL = 0
FADV_RANDOM = 1
FADV_SEQUENTIAL = 2
FADV_WILLNEED = 3
FADV_DONTNEED = 4
FADV_NOREUSE = 5

MAP_LOCKED = 0x2000
MAP_POPULATE = 0x08000
MAP_NORESERVE = 0x04000

MLOCK_ONFAULT = 1

mlock = syscall('mlock', [ctypes.c_void_p, ctypes.c_size_t], check=True)
# mlock2 = syscall('mlock2', [ctypes.c_void_p, ctypes.c_size_t, ctypes.c_int], check=True)
munlock = syscall('munlock', [ctypes.c_void_p, ctypes.c_size_t], check=True)

MCL_CURRENT = 1
MCL_FUTURE = 2
MCL_ONFAULT = 4

mlockall = syscall('mlockall', [ctypes.c_int], check=True)
munlockall = syscall('munlockall', [], check=True)

F_GET_CACHED_PAGES = 0x59410003


sys_fallocate = libc.fallocate
sys_fallocate.restype = ctypes.c_int
sys_fallocate.argtypes = [ctypes.c_int, ctypes.c_int, c_off_t, c_off_t]


def fallocate(fd, mode, offset, length):
    ret = sys_fallocate(fd.fileno(), mode, offset, length)
    if ret:
        err = ctypes.get_errno()
        raise OSError(err, 'fallocate {} {} {} : {}'.format(mode, offset, length, os.strerror(err)))
    return ret

FALLOC_FL_KEEP_SIZE = 0x01
FALLOC_FL_PUNCH_HOLE = 0x02
FALLOC_FL_NO_HIDE_STALE = 0x04
FALLOC_FL_COLLAPSE_RANGE = 0x08
FALLOC_FL_ZERO_RANGE = 0x10
FALLOC_FL_INSERT_RANGE = 0x20
FALLOC_FL_UNSHARE_RANGE = 0x40


eventfd = syscall('eventfd', [ctypes.c_uint, ctypes.c_int])


sys_fsetxattr_args = [ctypes.c_int, ctypes.c_char_p, ctypes.c_void_p, ctypes.c_size_t, ctypes.c_int]
sys_fsetxattr = syscall('fsetxattr', sys_fsetxattr_args, check=True)

XATTR_CREATE = 0x1
XATTR_REPLACE = 0x2


def fsetxattr(fd, name, value, flags):
    if not isinstance(name, str):
        raise TypeError('name type is {} but must be instance of str'.format(type(name)))
    if not isinstance(value, bytes):
        raise TypeError('value type is {} but must be instance of bytes'.format(type(value)))
    return sys_fsetxattr(fd, name.encode('utf-8'), value, len(value), flags)

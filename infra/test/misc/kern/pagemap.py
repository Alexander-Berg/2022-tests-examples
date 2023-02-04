import ctypes
import mmap
import os
import io

mincore = ctypes.CDLL(None).mincore
mincore.argtypes = [ctypes.c_size_t, ctypes.c_size_t, ctypes.c_size_t]

MINCORE_PRESENT = 1

madvise = ctypes.CDLL(None, use_errno=True).madvise
madvise.argtypes = [ctypes.c_size_t, ctypes.c_size_t, ctypes.c_int]

MADV_RANDOM = 1
MADV_SEQUENTIAL = 2
MADV_DONTNEED = 4
MADV_HWPOISON = 100
MADV_SOFT_OFFLINE = 101
MADV_POPULATE = 0x59410003
MADV_STOCKPILE = 0x59410004


def buffer_address(buf):
    return ctypes.addressof(ctypes.c_void_p.from_buffer(buf))

# /proc/pid/pagemap

PTE_PFN_BITS = 55
PTE_PFN_MASK = (1 << PTE_PFN_BITS) - 1
PTE_SWAP_BITS = 55
PTE_SWAP_MASK = (1 << PTE_SWAP_BITS) - 1
PTE_SWAP_TYPE_BITS = 5
PTE_SWAP_TYPE_MASK = (1 << PTE_SWAP_TYPE_BITS) - 1
PTE_SOFTDIRTY = 1 << 55
PTE_MMAP_EXCLUSIVE = 1 << 56
PTE_FILE = 1 << 61
PTE_SWAP = 1 << 62
PTE_PRESENT = 1 << 63

# /proc/kpageflags
# include/uapi/linux/kernel-page-flags.h
# include/linux/kernel-page-flags.h

PageFlags = ['Locked', 'Error', 'Referenced', 'Uptodate', 'Dirty',
              'LRU', 'Active', 'Slab', 'Writeback', 'Reclaim',
              'Buddy', 'MMap', 'Anon', 'SwapCache', 'SwapBacked',
              'Head', 'Tail', 'Huge', 'Unevictable', 'HWPoison',
              'NoPage', 'KSM', 'THP', 'Offline', 'ZeroPage',
              'Idle', 'PageTable', 'Flag27', 'Flag28', 'Flag29',
              'Flag30', 'Flag31', 'Reserved', 'MLocked', 'MappedToDisk',
              'Private', 'Private2', 'OwnerPrivate', 'Arch', 'Uncached',
              'SoftDirty', 'Flag41', 'Flag42', 'Flag43', 'Flag44',
              'Flag45', 'Flag46', 'Flag47', 'Flag48', 'Flag49',
              'Flag50', 'Flag51', 'Flag52', 'Flag53', 'Flag54',
              'Flag55', 'Flag56', 'Flag57', 'Flag58', 'Flag59',
              'Flag60', 'Flag61', 'Flag62', 'Flag63']


for bit, name in enumerate(PageFlags):
    globals()['KPF_' + name.upper()] = 1 << bit


def format_page_flags(flags):
    ret = ""
    for bit, name in enumerate(PageFlags):
        if flags & (1 << bit):
            ret += name + ","
    return ret[:-1]


class Page(object):
    __slots__ = ('offset', 'pfn', 'swap', 'flags', 'mapcount', 'cgroup')

    def __init__(self, offset, pfn, swap=None, flags=0, mapcount=0, cgroup=0):
        self.offset = offset
        self.pfn = pfn
        self.swap = swap
        self.flags = flags
        self.mapcount = mapcount
        self.cgroup = cgroup

    def __repr__(self):
        if self.pfn is not None:
            return '<Page: {}>'.format(self.pfn)
        if self.swap is not None:
            return '<SwapPage: {}>'.format(self.swap)
        return '<NoPage>'

    def format_flags(self):
        return format_page_flags(self.flags)


class PageMap(object):
    def __init__(self, path, bufsize=512, signed=False):
        self.file = io.FileIO(path)
        self.buf = bytearray(bufsize * 8)
        self.vec = ((ctypes.c_int64 if signed else ctypes.c_uint64) * bufsize).from_buffer(self.buf)

    def read(self, offset):
        self.file.seek(offset * 8)
        return self.file.readinto(self.buf) // 8

    def __getitem__(self, offset):
        self.read(offset)
        return self.vec[0]

    @staticmethod
    def iter_all_pages(bufsize=512):
        flags = PageMap('/proc/kpageflags', bufsize)
        mapcount = PageMap('/proc/kpagecount', bufsize, signed=True)
        cgroup = PageMap('/proc/kpagecgroup', bufsize)
        pfn = 0
        while True:
            count = flags.read(pfn)
            if count == 0:
                break
            mapcount.read(pfn)
            cgroup.read(pfn)
            for i in range(count):
                yield Page((pfn + i) * 4096, pfn + i, None, flags.vec[i], mapcount.vec[i], cgroup.vec[i])
            pfn += count

    @staticmethod
    def iter_vm_pages(pid, addr, size, bufsize=512):
        pagemap = PageMap('/proc/{}/pagemap'.format(pid), bufsize)
        flags = PageMap('/proc/kpageflags', 1)
        mapcount = PageMap('/proc/kpagecount', 1, signed=True)
        cgroup = PageMap('/proc/kpagecgroup', 1)
        nr_pages = (size + 4095) // 4096
        for offset in range(addr // 4096, (addr + size + 4095) // 4096, bufsize):
            count = min(pagemap.read(offset), nr_pages)
            nr_pages -= count
            for i in range(count):
                pte = pagemap.vec[i]
                off = (offset + i) * 4096
                if pte & PTE_PRESENT:
                    pfn = pte & PTE_PFN_MASK
                    yield Page(off, pte & PTE_PFN_MASK, None, flags[pfn], mapcount[pfn], cgroup[pfn])
                elif pte & PTE_SWAP:
                    yield Page(off, None, pte & PTE_SWAP_MASK)
                else:
                    yield Page(off, None, None, 0)

    @staticmethod
    def iter_file_pages(fd, bufsize=512):
        mincore_vec = bytearray(bufsize)
        mincore_addr = buffer_address(mincore_vec)

        pagemap = PageMap('/proc/self/pagemap', bufsize)
        flags = PageMap('/proc/kpageflags', 1)
        mapcount = PageMap('/proc/kpagecount', 1, signed=True)
        cgroup = PageMap('/proc/kpagecgroup', 1)

        size = os.fstat(fd).st_size
        nr_pages = (size + 4095) // 4096

        data = mmap.mmap(fd, size, mmap.MAP_PRIVATE, mmap.PROT_READ | mmap.PROT_WRITE)
        data_addr = buffer_address(data)

        for offset in range(0, nr_pages, bufsize):
            nr = min(bufsize, nr_pages - offset)
            start_addr = data_addr + offset * 4096
            mincore(start_addr, nr * 4096, mincore_addr)

            # disable readahead
            madvise(data_addr, size, MADV_RANDOM)

            # touch
            for i in range(nr):
                if mincore_vec[i] & MINCORE_PRESENT:
                    data.seek((offset + i) * 4096)
                    data.read_byte()

            # disable harvesting reference biits
            madvise(data_addr, size, MADV_SEQUENTIAL)

            # read pfns
            pagemap.read(start_addr // 4096)

            # unmap pages
            madvise(data_addr, size, MADV_DONTNEED)

            for i in range(nr):
                pte = pagemap.vec[i]
                off = (offset + i) * 4096
                if pte & PTE_PRESENT:
                    pfn = pte & PTE_PFN_MASK
                    yield Page(off, pfn, None, flags[pfn], mapcount[pfn], cgroup[pfn])
                else:
                    yield Page(off, None, None, 0)

    @staticmethod
    def count_page_flags(pages):
        res = {}
        for page in pages:
            res[page.flags] = res.get(page.flags, 0) + 1
        return res

    @staticmethod
    def count_pages(pages, mask, val):
        res = 0
        for page in pages:
            if page.flags & mask == val:
                res += 1
        return res

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        f = open(sys.argv[1])
        for flags, count in PageMap.count_page_flags(PageMap.iter_file_pages(f.fileno())).items():
            print(format_page_flags(flags), count)
    else:
        for flags, count in PageMap.count_page_flags(PageMap.iter_all_pages()).items():
            print(format_page_flags(flags), count)

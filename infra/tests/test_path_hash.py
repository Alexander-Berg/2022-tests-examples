from __future__ import absolute_import

import os
from cStringIO import StringIO

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util.sys import TempDir
from skycore.framework.utils import Path
from skycore.tartools import TarFile


class TestPathHash(TestCase):
    def test_hashing(self):
        io = StringIO()

        with TempDir() as tempdir:
            p = Path(tempdir)
            p.join('file1').write('abcd')
            p.join('dir1').ensure(dir=1)
            p.join('dir2').mksymlinkto(p.join('dir1'), absolute=False)
            p.join('dir1', 'file2').mksymlinkto(p.join('file1'), absolute=False)
            p.join('dir1', 'file3').mklinkto(p.join('file1'))

            original_hash = p.hash_contents(['dir1',
                                             'dir1/file2',
                                             'dir1/file3',
                                             'dir2',
                                             'file1',
                                             ])

            with TarFile.open(fileobj=io, mode='w:gz') as t:
                t.add(p.join('file1').strpath, 'file1')
                t.add(p.join('dir1').strpath, 'dir1')
                t.add(p.join('dir2').strpath, 'dir2')

        io.reset()

        with TempDir() as tempdir, TarFile.open(fileobj=io) as t:
            self.assertEqual(original_hash, t.md5())
            members = [os.path.relpath(member.name, '.') for member in sorted(t, key=lambda m: m.name)]

            p = Path(tempdir)
            t.extractall(p.strpath)
            self.assertEqual(original_hash, p.hash_contents(members))


if __name__ == '__main__':
    main()

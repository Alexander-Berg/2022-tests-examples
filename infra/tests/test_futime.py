import os
import time
from unittest import TestCase, main

from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.services.portoshell import filesystem
from ya.skynet.util.sys import TempDir


class TestFilesystem(TestCase):
    def test_futime(self):
        with TempDir() as td:
            name = os.path.join(td, 'xxx')
            with open(name, 'w') as f:
                st = os.fstat(f.fileno())
                atime, mtime = st.st_atime, st.st_mtime
                time.sleep(0.1)

                filesystem.futime(f, None)
                st = os.fstat(f.fileno())
                self.assertNotEqual(st.st_atime, atime)
                self.assertNotEqual(st.st_mtime, mtime)
                atime, mtime = st.st_atime, st.st_mtime

                time.sleep(0.1)
                filesystem.futime(f.fileno(), None)
                st = os.fstat(f.fileno())
                self.assertNotEqual(st.st_atime, atime)
                self.assertNotEqual(st.st_mtime, mtime)
                atime, mtime = st.st_atime, st.st_mtime

                time.sleep(0.1)
                filesystem.futime(f.fileno(), (10, 20))
                st = os.fstat(f.fileno())
                self.assertEqual(st.st_atime, 10)
                self.assertEqual(st.st_mtime, 20)

            with self.assertRaises(OSError):
                filesystem.futime(-1, None)


if __name__ == '__main__':
    main()

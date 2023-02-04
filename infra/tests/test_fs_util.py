import errno
import os
import unittest

from sepelib.util.fs.util import close_ignore


class TestFsUtils(unittest.TestCase):
    def test_close_ignore(self):
        self.assertIsNone(close_ignore(-1))
        fd = os.open(os.devnull, os.O_RDONLY)
        # close it with our func
        self.assertIsNone(close_ignore(fd))
        # check that it is invalid now
        with self.assertRaises(EnvironmentError) as cm:
            os.close(fd)
        self.assertEqual(cm.exception.errno, errno.EBADF)

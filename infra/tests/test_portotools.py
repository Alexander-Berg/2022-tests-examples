import os
import sys
import random
import tarfile
from unittest import TestCase, main

import mock

from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.util.sys import TempDir
from ya.skynet.services.portoshell import portotools


def make_name():
    return 'ut-%s-%s' % (os.getpid(), random.randint(0, sys.maxint))


class TestPortotools(TestCase):
    def test_home(self):
        with TempDir() as td:
            conn = portotools.get_portoconn()
            c = conn.CreateWeakContainer(make_name())
            c.SetProperty('root', td)
            path, in_container_path = portotools.make_home_volume(c.name, {'abc': 'def'})
            self.assertTrue(os.path.exists(path))
            self.assertTrue(os.path.isdir(path))

            f = os.path.join(path, 'abc')
            self.assertTrue(os.path.exists(f))
            self.assertTrue(os.path.isfile(f))
            self.assertEqual(open(f).read(), 'def')

            self.assertEqual(in_container_path, '/' + os.path.basename(path))

    def test_find_in_container(self):
        with TempDir() as td:
            with open(os.path.join(td, 'abc'), 'w'):
                pass
            with open(os.path.join(td, 'def'), 'w'):
                pass
            conn = portotools.get_portoconn()
            c = conn.CreateWeakContainer(make_name())
            c.SetProperty('root', td)
            self.assertIsNone(portotools.find_in_container(c.name, ('a', 'b'), None))
            self.assertEquals(portotools.find_in_container(c.name, ('/a', '/b'), 'pepe'), 'pepe')
            self.assertEquals(portotools.find_in_container(c.name, ('/abc', '/b'), 'pepe'), '/abc')
            self.assertEquals(portotools.find_in_container(c.name, ('/b', '/abc'), 'pepe'), '/abc')
            self.assertEquals(portotools.find_in_container(c.name, ('/abc', '/def'), 'pepe'), '/abc')
            self.assertEquals(portotools.find_in_container(c.name, ('/def', '/abc'), 'pepe'), '/def')

    def test_make_utils_container(self):
        conn = portotools.get_portoconn()
        layer_name = make_name()
        try:
            with TempDir() as td, mock.patch('ya.skynet.services.portoshell.portotools.LAYER', new=layer_name):
                with open(os.path.join(td, 'abc'), 'w') as f:
                    f.write("def")
                with tarfile.open(os.path.join(td, "abc.tar.gz"), "w|gz") as tar:
                    tar.add(os.path.join(td, "abc"), "abc")
                with open(os.path.join(td, 'abc'), 'w') as f:
                    f.write("pqr")
                with tarfile.open(os.path.join(td, "pqr.tar.gz"), "w|gz") as tar:
                    tar.add(os.path.join(td, "abc"), "abc")

                with TempDir() as td2, TempDir() as td3:
                    c = conn.CreateWeakContainer(make_name())
                    c.SetProperty('root', td2)
                    try:
                        portotools.make_utils_volume(c.name, os.path.join(td, 'abc.tar.gz'))

                        volume = conn.FindVolume(os.path.join(td2, portotools.MOUNTPOINT))
                        cnts = volume.GetContainers()
                        self.assertEqual(len(cnts), 1)
                        self.assertEqual(cnts[0].name, c.name)

                        with open(os.path.join(volume.path, 'abc')) as f:
                            self.assertEqual(f.read(), 'def')

                        # duplicate call is ok
                        portotools.make_utils_volume(c.name, os.path.join(td, 'abc.tar.gz'))

                        volume = conn.FindVolume(os.path.join(td2, portotools.MOUNTPOINT))
                        cnts = volume.GetContainers()
                        self.assertEqual(len(cnts), 1)
                        self.assertEqual(cnts[0].name, c.name)

                        with open(os.path.join(volume.path, 'abc')) as f:
                            self.assertEqual(f.read(), 'def')

                        # remount new over already mounted changes nothing
                        portotools.make_utils_volume(c.name, os.path.join(td, 'pqr.tar.gz'))

                        volume = conn.FindVolume(os.path.join(td2, portotools.MOUNTPOINT))
                        cnts = volume.GetContainers()
                        self.assertEqual(len(cnts), 1)
                        self.assertEqual(cnts[0].name, c.name)

                        with open(os.path.join(volume.path, 'abc')) as f:
                            self.assertEqual(f.read(), 'def')

                        # mount over new container while busy changes nothing
                        c2 = conn.CreateWeakContainer(make_name())
                        c2.SetProperty('root', td3)
                        try:
                            portotools.make_utils_volume(c2.name, os.path.join(td, 'pqr.tar.gz'))

                            volume = conn.FindVolume(os.path.join(td3, portotools.MOUNTPOINT))
                            cnts = volume.GetContainers()
                            self.assertEqual(len(cnts), 1)
                            self.assertEqual(cnts[0].name, c2.name)

                            with open(os.path.join(volume.path, 'abc')) as f:
                                self.assertEqual(f.read(), 'def')
                        finally:
                            c2.Destroy()
                    finally:
                        c.Destroy()

                    # mount new from scratch changes data
                    c = conn.CreateWeakContainer(make_name())
                    c.SetProperty('root', td2)
                    try:
                        portotools.make_utils_volume(c.name, os.path.join(td, 'pqr.tar.gz'))

                        volume = conn.FindVolume(os.path.join(td2, portotools.MOUNTPOINT))
                        cnts = volume.GetContainers()
                        self.assertEqual(len(cnts), 1)
                        self.assertEqual(cnts[0].name, c.name)

                        with open(os.path.join(volume.path, 'abc')) as f:
                            self.assertEqual(f.read(), 'pqr')
                    finally:
                        c.Destroy()
        finally:
            conn.RemoveLayer(layer_name)


if __name__ == '__main__':
    main()

import os
import six
import sys
import types
import shutil
import traceback
from zipfile import PyZipFile, ZipFile

from ya.skynet.util.unittest import TestCase, main, skip, skipIf
from ya.skynet.util.sys import TempDir

from ya.skynet.services.cqudp import eggs, importer


class TestEggs(TestCase):
    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_this_module(self):
        self.assertTrue(eggs.collect_modules([__name__]))

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    @skip('namespaces are not supported yet')
    def test_namespace_creation(self):
        import kernel

        with TempDir() as tempdir1:
            filename1 = os.path.join(tempdir1, 'testegg.egg')
            zipFile1 = PyZipFile(filename1, 'w')
            zipFile1.writepy(os.path.dirname(kernel.__file__), basename='__ns1__/__ns2__/testegg1')
            zipFile1.close()

            sys.path.insert(0, filename1)
            try:
                self.assertTrue(eggs.collect_modules(['__ns1__.__ns2__.testegg1']))
            finally:
                sys.path.pop(0)

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_banned_eggs(self):
        self.assertFalse(eggs.collect_modules(['kernel', 'kernel.util', 'ya.skynet.util', 'ya.skynet.services.cqudp']))

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_builtin(self):
        self.assertFalse(eggs.collect_modules(['os', 'sys', 'zipfile']))

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_main_module(self):
        self.assertTrue(eggs.collect_modules(['__main__']))

        modules = [name for path, name, module, filename in eggs.collect_modules()]
        self.assertTrue('__new__main__' in modules or '__main__' in modules, 'main not found in: %s' % (modules,))

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_no_stub_main(self):
        self.assertFalse(eggs.check_main("print([1,2,3])\n"))
        self.assertTrue(eggs.check_main("if __name__ == '__main__':\n  print([1,2,3])\n"))

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_no_binary_modules(self):
        import termios
        with TempDir() as tempdir:
            shutil.copy(termios.__file__, os.path.join(tempdir, os.path.basename(termios.__file__)))
            sys.path.insert(0, tempdir)
            del sys.modules['termios']
            try:
                self.assertFalse(eggs.collect_modules(['termios']))
            finally:
                sys.path.pop(0)
                del sys.modules['termios']

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_readonly(self):
        with TempDir() as tempdir:
            with TempDir() as tempdir2:
                filename = os.path.join(tempdir, 'module1.py')
                zipf = os.path.join(tempdir2, 'egg.egg')
                shutil.copy(__file__, filename)
                os.chmod(filename, 0o444)
                os.chmod(tempdir, 0o555)

                zipfile = ZipFile(zipf, 'w')
                eggs.write_module(zipfile, filename, sys.modules[__name__], 'module1.py')
                zipfile.close()

                zipfile = ZipFile(zipf, 'r')
                self.assertIn('module1.py', zipfile.namelist())
                self.assertIn('module1.pyc', zipfile.namelist())

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_custom_importer(self):
        import kernel

        with TempDir() as tempdir:
            filename = os.path.join(tempdir, 'testegg.egg')
            zipFile1 = PyZipFile(filename, 'w')
            eggs.write_package(zipFile1, '__ns1__.kernel', kernel, os.path.dirname(kernel.__file__))
            zipFile1.writestr('__ns1__/__init__.py', '__import__("pkg_resources").declare_namespace(__name__)\n')
            zipFile1.close()

            imprt = importer.InMemoryImporter(open(filename, 'rb').read())
            sys.meta_path.insert(0, imprt)
            try:
                egg = eggs.create_egg(['__ns1__', '__ns1__.kernel'])
                if six.PY2:
                    io = six.moves.cStringIO(egg)
                else:
                    io = six.BytesIO(egg)
                with ZipFile(io) as zegg:
                    files = zegg.namelist()
                for req in (
                    '__ns1__/__init__',
                    '__ns1__/kernel/__init__',
                    '__ns1__/kernel/util/__init__',
                ):
                    # self.assertIn(req + '.py', files)
                    self.assertIn(req + '.pyc', files)
            finally:
                sys.meta_path.pop(0)

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_main_without_extension(self):
        with TempDir() as tempdir:
            mod = types.ModuleType('__main__')
            path = os.path.join(tempdir, 'noextfile')
            mod.__file__ = path

            def x():
                return 42

            mod.x = x

            with open(path, 'w') as f:
                f.write('def x():\n    return 42\nif __name__ == "__main__":\n    pass\n')

            egg = eggs.package_egg([(path, '__main__', mod, '__new__main__.py')])
            imprt = importer.InMemoryImporter(egg)
            self.assertIn('__new__main__', imprt.modnames)

    @skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
    def test_meaningful_filenames(self):
        sys.modules.pop('prettymod.pmod', None)  # just to be sure
        sys.modules.pop('prettymod', None)  # just to be sure
        with TempDir() as tempdir:
            try:
                sys.path.insert(0, tempdir)
                os.makedirs(os.path.join(tempdir, 'prettymod', 'pmod', 'sub1'))
                with open(os.path.join(tempdir, 'prettymod', '__init__.py'), 'w') as f:
                    f.write('__import__("pkg_resources").declare_namespace(__name__)\n')
                with open(os.path.join(tempdir, 'prettymod', 'pmod', '__init__.py'), 'w') as f:
                    f.write('def x():\n    raise RuntimeError("oops")\n')
                with open(os.path.join(tempdir, 'prettymod', 'pmod', 'sub1', '__init__.py'), 'w') as f:
                    f.write('def y():\n    from .. import x\n    x()\n')
                with open(os.path.join(tempdir, 'prettymod', 'pmod', 'sub1', 'sub2.py'), 'w') as f:
                    f.write('def z():\n    from . import y\n    y()\n')

                egg = eggs.create_egg(['prettymod', 'prettymod.pmod'])
                sys.path.remove(tempdir)
                sys.modules.pop('prettymod.pmod', None)
                sys.modules.pop('prettymod', None)

                imprt = importer.InMemoryImporter(egg)
                sys.meta_path.insert(0, imprt)
                try:
                    try:
                        import prettymod.pmod.sub1.sub2 as pss
                        pss.z()
                    except RuntimeError:
                        tb = sys.exc_info()[2]
                        files = [fn for fn, _, _, _ in traceback.extract_tb(tb)]
                        self.assertTrue('prettymod/pmod/sub1/sub2.' in files[-3], files[-3])
                        self.assertTrue('prettymod/pmod/sub1/__init__.' in files[-2], files[-2])
                        self.assertTrue('prettymod/pmod/__init__.' in files[-1], files[-1])
                    else:
                        raise AssertionError("Something strange occurred")

                finally:
                    sys.meta_path.pop(0)

            finally:
                if tempdir in sys.path:
                    sys.path.remove(tempdir)
                sys.modules.pop('prettymod.sub1.sub2', None)  # just to be sure
                sys.modules.pop('prettymod.sub1', None)  # just to be sure
                sys.modules.pop('prettymod', None)  # just to be sure


if __name__ == '__main__':
    raise SystemExit(main())

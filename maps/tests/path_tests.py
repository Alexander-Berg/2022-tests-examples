import os
import shutil
import unittest

from maps.carparks.renderer.config.ecstatic_hooks.lib.path import Path


TEST_DIR = 'genfiles/tests'


class PathTests(unittest.TestCase):
    def setUp(self):
        super(PathTests, self).setUp()
        shutil.rmtree(TEST_DIR, ignore_errors=True)
        os.makedirs(TEST_DIR)

    def test_construction(self):
        path = Path('a', 'bcd', 'e')
        self.assertEquals('a/bcd/e', path.path)
        self.assertEquals('e', path.basename())
        self.assertEquals('a/bcd', path.parent().path)

    def test_concatenation(self):
        path = Path('abc')
        self.assertEquals('abc/def', str(path / 'def'))
        self.assertEquals('abc/de/f', str(path / 'de' / 'f'))
        self.assertEquals('abc', path.path)

    def test_extension(self):
        self.assertEquals('abcdef', str(Path('abc') + 'def'))

    def test_listdir(self):
        dir_path = Path(TEST_DIR) / 'test-dir'
        (dir_path / 'some-file').mkdir()
        (dir_path / 'some-dir').mkdir()

        self.assertEquals(['some-dir', 'some-file'], sorted(dir_path.listdir()))

    def test_glob(self):
        dir_path = Path(TEST_DIR) / 'test-dir'
        (dir_path / 'some-file').mkdir()
        (dir_path / 'some-dir').mkdir()
        (dir_path / 'some-dir2').mkdir()

        self.assertEquals(['some-dir', 'some-dir2', 'some-file'],
                          sorted(dir_path.glob('*')))
        self.assertEquals(['some-dir', 'some-dir2'],
                          sorted(dir_path.glob('*dir*')))
        self.assertEquals(['some-dir'], dir_path.glob('*dir'))
        self.assertEquals(['some-dir'], dir_path.glob('some-di?'))
        self.assertEquals(['some-file'], dir_path.glob('*file'))

    def test_read(self):
        with open(TEST_DIR + '/test-file', 'w') as f:
            f.write('Some contents')

        path = Path(TEST_DIR, 'test-file')
        self.assertTrue(path.exists())
        self.assertTrue(path.is_file())
        self.assertFalse(path.is_dir())
        self.assertEquals(['test-file'], path.parent().listdir())
        self.assertEquals('Some contents', path.read())

    def test_write(self):
        path = Path(TEST_DIR, 'test-file')
        path.write('Test string')
        self.assertTrue(path.exists())
        self.assertTrue(path.is_file())
        self.assertFalse(path.is_dir())
        self.assertEquals(['test-file'], path.parent().listdir())

        self.assertTrue(os.path.isfile(TEST_DIR + '/test-file'))
        with open(TEST_DIR + '/test-file') as f:
            self.assertEquals('Test string', f.read())

    def test_mkdir(self):
        dir_path = TEST_DIR + '/test-dir/inner-dir'

        Path(dir_path).mkdir()

        self.assertEquals(['inner-dir'], Path(TEST_DIR, 'test-dir').listdir())
        self.assertTrue(os.path.isdir(dir_path))

    def test_remove_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'test-dir/inner-dir').mkdir()
        (test_path / 'test-dir/test-file').write('Sample')

        (test_path / 'test-dir').remove()

        self.assertEquals([], test_path.listdir())

    def test_remove_file(self):
        test_path = Path(TEST_DIR)
        (test_path / 'test-file').write('File contents')

        (test_path / 'test-file').remove()

        self.assertEquals([], test_path.listdir())

    def test_copy_puts_src_dir_into_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src').mkdir()
        (test_path / 'src/test-file').write('Message')

        (test_path / 'src').copy_to(test_path / 'dst')

        self.assertEquals(['dst', 'src'], sorted(test_path.listdir()))
        self.assertEquals(['test-file'], (test_path / 'src').listdir())
        self.assertEquals(['src'], (test_path / 'dst').listdir())
        self.assertEquals(['test-file'], (test_path / 'dst/src').listdir())
        self.assertTrue('Message', (test_path / 'dst/src/test-file').read())

    def test_copy_puts_src_dir_as_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src').mkdir()
        (test_path / 'src/test-file').write('Message')

        (test_path / 'src').copy_to(test_path / 'dst/renamed-src')

        self.assertEquals(['dst', 'src'], sorted(test_path.listdir()))
        self.assertEquals(['test-file'], (test_path / 'src').listdir())
        self.assertEquals(['renamed-src'], (test_path / 'dst').listdir())
        self.assertEquals(['test-file'], (test_path / 'dst/renamed-src').listdir())
        self.assertTrue('Message', (test_path / 'dst/renamed-src/test-file').read())

    def test_copy_puts_src_file_into_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src-file').write('Message')

        (test_path / 'src-file').copy_to(test_path / 'dst')

        self.assertEquals(['dst', 'src-file'], sorted(test_path.listdir()))
        self.assertEquals(['src-file'], (test_path / 'dst').listdir())
        self.assertTrue('Message', (test_path / 'dst/src-file').read())

    def test_copy_puts_src_file_as_dst_file(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst-file').write('Old-message')
        (test_path / 'src-file').write('Message')

        (test_path / 'src-file').copy_to(test_path / 'dst-file')

        self.assertEquals(['dst-file', 'src-file'], sorted(test_path.listdir()))
        self.assertTrue('Message', (test_path / 'dst-file').read())

    def test_move_puts_src_dir_into_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src').mkdir()
        (test_path / 'src/test-file').write('Message')

        (test_path / 'src').move_to(test_path / 'dst')

        self.assertEquals(['dst'], test_path.listdir())
        self.assertEquals(['src'], (test_path / 'dst').listdir())
        self.assertEquals(['test-file'], (test_path / 'dst/src').listdir())
        self.assertTrue('Message', (test_path / 'dst/src/test-file').read())

    def test_move_puts_src_dir_as_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src').mkdir()
        (test_path / 'src/test-file').write('Message')

        (test_path / 'src').move_to(test_path / 'dst/renamed-src')

        self.assertEquals(['dst'], test_path.listdir())
        self.assertEquals(['renamed-src'], (test_path / 'dst').listdir())
        self.assertEquals(['test-file'], (test_path / 'dst/renamed-src').listdir())
        self.assertTrue('Message', (test_path / 'dst/renamed-src/test-file').read())

    def test_move_puts_src_file_into_dst_dir(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst').mkdir()
        (test_path / 'src-file').write('Message')

        (test_path / 'src-file').move_to(test_path / 'dst')

        self.assertEquals(['dst'], test_path.listdir())
        self.assertEquals(['src-file'], (test_path / 'dst').listdir())
        self.assertTrue('Message', (test_path / 'dst/src-file').read())

    def test_move_puts_src_file_as_dst_file(self):
        test_path = Path(TEST_DIR)
        (test_path / 'dst-file').write('Old-message')
        (test_path / 'src-file').write('Message')

        (test_path / 'src-file').move_to(test_path / 'dst-file')

        self.assertEquals(['dst-file'], test_path.listdir())
        self.assertTrue('Message', (test_path / 'dst-file').read())
